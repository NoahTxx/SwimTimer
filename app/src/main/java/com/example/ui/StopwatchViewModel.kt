package com.example.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.LapRecordEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SessionWithLaps
import com.example.data.model.SwimmerEntity
import com.example.data.repository.SwimRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.util.ExportHelper
import com.example.util.SoundUtil
import com.example.util.TimeFormatter
import com.example.util.AndroidMonotonicClock
import com.example.util.MonotonicClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import java.util.LinkedList

enum class LaneStatus {
    IDLE,
    COUNTDOWN,
    RUNNING,
    PAUSED,
    FINISHED,
    REST_COUNTDOWN
}

enum class StartMode {
    MASS,       // Alle starten zeitgleich
    INTERVAL    // Staffelung (z. B. alle 5s, 10s)
}

enum class SeriesModeType {
    MANUAL,      // Manuell (Standard)
    FIXED_REST,  // Feste Pause (Rest Interval)
    SEND_OFF     // Feste Abgangszeit (Send-off / Zyklus)
}

@Immutable
data class SeriesConfig(
    val isEnabled: Boolean = false,
    val mode: SeriesModeType = SeriesModeType.MANUAL,
    val restDurationSeconds: Int = 20,
    val sendOffDurationSeconds: Int = 60,
    val maxSeriesCount: Int? = null // null = Unbegrenzt
)

@Immutable
data class LapItem(
    val lapIndex: Int,
    val splitTimeMillis: Long,
    val lapTimeMillis: Long,
    val strokeRateSpm: Double? = null,
    val seriesIndex: Int = 1,
)

@Immutable
data class LaneState(
    val laneNumber: Int,
    val swimmerId: Long? = null,
    val swimmerName: String = "Bahn $laneNumber",
    val status: LaneStatus = LaneStatus.IDLE,
    val laneStartTimeRealtime: Long? = null,
    val finishTimeMillis: Long? = null,
    val finishRealtimeMillis: Long? = null,
    val restTargetRealtime: Long? = null,
    val restRemainingSeconds: Int = 0,
    val currentElapsedMillis: Long = 0L,
    val laps: List<LapItem> = emptyList(),
    val seriesCount: Int = 1,
    val completedSeriesLaps: List<LapItem> = emptyList(),
    val intervalOffsetMillis: Long = 0L,
    val countdownRemainingSeconds: Int = 0,
    val targetDistanceMeters: Int = 100,
    val splitDistanceMeters: Int = 50,
    val strokeType: String = "Freistil",
    val spmTapStartRealtime: Long? = null,
    val currentSpm: Double? = null,
) {
    val allLaps: List<LapItem>
        get() = completedSeriesLaps + laps
}

sealed interface UndoAction {
    data class LapAdded(
        val laneNumber: Int,
        val previousStatus: LaneStatus,
        val previousFinishTime: Long?,
        val previousFinishRealtime: Long? = null,
        val removedLap: LapItem
    ) : UndoAction
    data class LaneFinished(
        val laneNumber: Int,
        val previousStatus: LaneStatus,
        val previousFinishTime: Long? = null,
        val previousFinishRealtime: Long? = null,
        val removedFinishLap: LapItem?
    ) : UndoAction
    data class BulkStart(val previousLanes: List<LaneState>, val previousMasterStart: Long?) : UndoAction
    data class BulkStop(val previousLanes: List<LaneState>) : UndoAction
    data class LaneRemoved(val lane: LaneState, val index: Int) : UndoAction
}

@Immutable
data class StopwatchUiState(
    val laneCount: Int = 4, // 1 to 15
    val lanes: List<LaneState> = emptyList(),
    val masterStartTimeRealtime: Long? = null,
    val startMode: StartMode = StartMode.MASS,
    val intervalOffsetSeconds: Int = 5,
    val seriesConfig: SeriesConfig = SeriesConfig(),
    val selectedFocusedLane: Int = 1,
    val sessionTitle: String = "Trainingseinheit",
    val poolLengthMeters: Int = 50,
    val targetDistanceMeters: Int = 100,
    val splitDistanceMeters: Int = 50,
    val strokeType: String = "Freistil",
    val canUndo: Boolean = false,
    val lastUndoMessage: String? = null,
    // Prevents accidental double-saving of the exact same run
    val isCurrentRunSaved: Boolean = false,
    // Stroke Rate Calculator
    val isStrokeRateDialogVisible: Boolean = false,
    val strokeRateTargetLane: Int? = null,
    // Assign Swimmer Dialog
    val isAssignSwimmerDialogVisible: Boolean = false,
    val assignSwimmerLane: Int? = null,
    // Save confirmation
    val isSessionSavedSuccess: Boolean = false,
    val saveNotificationMessage: String? = null,
    // Swim Pace & Split Calculator
    val isPaceCalculatorVisible: Boolean = false
) {
    @Suppress("unused")
    val hasTickingLanes: Boolean
        get() = lanes.any { lane -> lane.status == LaneStatus.RUNNING || lane.status == LaneStatus.COUNTDOWN || lane.status == LaneStatus.REST_COUNTDOWN }

    val hasOpenRun: Boolean
        get() = lanes.any { (it.status == LaneStatus.RUNNING) || (it.status == LaneStatus.COUNTDOWN) || (it.status == LaneStatus.PAUSED) || (it.status == LaneStatus.REST_COUNTDOWN) }
}

@Immutable
data class SwimmerCardUiModel(
    val id: Long,
    val name: String,
    val initial: String,
    val groupOrClub: String,
    val pb25mText: String,
    val pb50mText: String,
    val pb100mText: String,
    val pb100mImText: String,
    val pb50mFormatted: String,
    val groupName: String? = null,
    val originalEntity: SwimmerEntity
)

class StopwatchViewModel(
    private val repository: SwimRepository,
    private val preferencesRepository: UserPreferencesRepository? = null,
    private val clock: MonotonicClock = AndroidMonotonicClock()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    val allSwimmers: StateFlow<List<SwimmerEntity>> = repository.allSwimmers
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val swimmersSearchQuery = MutableStateFlow("")

    val selectedGroupFilter = MutableStateFlow<String?>(null) // null = Alle

    private val _customGroups = MutableStateFlow<Set<String>>(emptySet())
    val customGroups: StateFlow<Set<String>> = _customGroups.asStateFlow()

    val availableGroups: StateFlow<List<String>> = combine(
        allSwimmers,
        _customGroups
    ) { swimmers, custom ->
        val fromSwimmers = swimmers.mapNotNull { it.groupName?.trim() }.filter { it.isNotBlank() }
        (fromSwimmers + custom)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val swimmerUiList: StateFlow<List<SwimmerCardUiModel>> = combine(
        allSwimmers,
        swimmersSearchQuery,
        selectedGroupFilter
    ) { swimmers, query, selectedGroup ->
        var filtered = swimmers
        if (selectedGroup != null) {
            filtered = filtered.filter { it.groupName.equals(selectedGroup, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }
        filtered.map { swimmer ->
            val pb50mText = swimmer.pb50mMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: ""
            SwimmerCardUiModel(
                id = swimmer.id,
                name = swimmer.name,
                initial = swimmer.name.take(1).uppercase(),
                groupOrClub = swimmer.favoriteStroke,
                pb25mText = swimmer.pb25mMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "",
                pb50mText = pb50mText,
                pb100mText = swimmer.pb100mMillis?.let { TimeFormatter.formatMillis(it) } ?: "",
                pb100mImText = swimmer.pb100mLagenMillis?.let { TimeFormatter.formatMillis(it) } ?: "",
                pb50mFormatted = pb50mText.ifEmpty { "--:--" },
                groupName = swimmer.groupName,
                originalEntity = swimmer
            )
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSessions: StateFlow<List<SessionWithLaps>> = repository.allSessionsWithLaps
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val undoStack = LinkedList<UndoAction>()
    private var tickerJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadSavedPreferences()
            if (preferencesRepository == null) {
                initializeLanes(4)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            allSwimmers.collect { swimmers ->
                val dbGroups = swimmers.mapNotNull { it.groupName?.trim() }.filter { it.isNotBlank() }
                if (dbGroups.isNotEmpty()) {
                    val currentCustom = _customGroups.value
                    val missing = dbGroups.filter { dbG -> currentCustom.none { it.equals(dbG, ignoreCase = true) } }
                    if (missing.isNotEmpty()) {
                        _customGroups.update { old ->
                            (old + missing).filter { it.isNotBlank() }.distinctBy { it.lowercase() }.toSet()
                        }
                        saveGroups()
                    }
                }
                val currentFilter = selectedGroupFilter.value
                if (currentFilter != null) {
                    val groupExists = swimmers.any { it.groupName?.trim()?.equals(currentFilter, ignoreCase = true) == true } ||
                            _customGroups.value.any { it.equals(currentFilter, ignoreCase = true) }
                    if (!groupExists) {
                        selectedGroupFilter.value = null
                    }
                }
            }
        }
    }

    fun renameGroup(oldName: String, newName: String) {
        if (oldName.isBlank() || newName.isBlank()) return
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (selectedGroupFilter.value?.trim()?.equals(trimmedOld, ignoreCase = true) == true) {
            selectedGroupFilter.value = trimmedNew
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.renameGroupForAllSwimmers(trimmedOld, trimmedNew)
            _customGroups.update { old ->
                old.filterNot { it.equals(trimmedOld, ignoreCase = true) }.toSet() + trimmedNew
            }
            saveGroups()
        }
    }

    fun deleteGroup(groupName: String) {
        if (groupName.isBlank()) return
        val trimmed = groupName.trim()
        if (selectedGroupFilter.value?.trim()?.equals(trimmed, ignoreCase = true) == true) {
            selectedGroupFilter.value = null
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearGroupForAllSwimmers(trimmed)
            _customGroups.update { old ->
                old.filterNot { it.equals(trimmed, ignoreCase = true) }.toSet()
            }
            saveGroups()
        }
    }

    fun createGroup(groupName: String) {
        val name = groupName.trim()
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _customGroups.update { old ->
                if (old.none { it.equals(name, ignoreCase = true) }) old + name else old
            }
            saveGroups()
        }
    }

    private fun saveGroups() {
        preferencesRepository?.saveCustomGroups(_customGroups.value)
    }

    private fun validateDistances(targetMeters: Int, splitMeters: Int) {
        require(targetMeters > 0)
        require(splitMeters > 0)
        require(targetMeters % splitMeters == 0) {
            "Die Zieldistanz muss durch die Splitdistanz teilbar sein."
        }
    }

    private fun loadSavedPreferences() {
        SoundUtil.init()
        val prefsRepo = preferencesRepository ?: return
        val poolRaw = prefsRepo.getPoolLengthMeters()
        val pool = poolRaw.coerceIn(10, 100)

        val distRaw = prefsRepo.getTargetDistanceMeters()
        val dist = distRaw.coerceIn(25, 10000)

        val splitRaw = prefsRepo.getSplitDistanceMeters()
        var split = splitRaw.coerceIn(25, dist)
        if (dist % split != 0) {
            split = if (dist % 50 == 0) 50 else dist
        }

        val stroke = prefsRepo.getStrokeType()
        val startModeStr = prefsRepo.getStartMode()
        val intervalRaw = prefsRepo.getIntervalOffsetSeconds()
        val interval = intervalRaw.coerceIn(1, 120)

        val countRaw = prefsRepo.getLaneCount()
        val count = countRaw.coerceIn(1, 15)

        val startModeVal = try {
            StartMode.valueOf(startModeStr)
        } catch (_: Exception) {
            StartMode.MASS
        }

        val isSeriesEnabled = prefsRepo.getSeriesEnabled(false)
        val seriesModeStr = prefsRepo.getSeriesMode()
        val seriesRest = prefsRepo.getSeriesRestSec()
        val seriesSendOff = prefsRepo.getSeriesSendOffSec()
        val seriesMaxRaw = prefsRepo.getSeriesMaxCount()
        val seriesMax = if (seriesMaxRaw > 0) seriesMaxRaw else null

        val seriesModeVal = try {
            SeriesModeType.valueOf(seriesModeStr)
        } catch (_: Exception) {
            SeriesModeType.MANUAL
        }

        val loadedSeriesConfig = SeriesConfig(
            isEnabled = isSeriesEnabled,
            mode = seriesModeVal,
            restDurationSeconds = seriesRest,
            sendOffDurationSeconds = seriesSendOff,
            maxSeriesCount = seriesMax
        )

        _uiState.update { currentState ->
            currentState.copy(
                poolLengthMeters = pool,
                targetDistanceMeters = dist,
                splitDistanceMeters = split,
                strokeType = stroke,
                startMode = startModeVal,
                intervalOffsetSeconds = interval,
                seriesConfig = loadedSeriesConfig,
                laneCount = count,
                sessionTitle = "${dist}m $stroke"
            )
        }
        
        val savedGroups = prefsRepo.getCustomGroups()
        _customGroups.value = savedGroups
        
        initializeLanes(count)
    }

    private fun savePreferences() {
        preferencesRepository?.savePreferences(
            poolLengthMeters = _uiState.value.poolLengthMeters,
            targetDistanceMeters = _uiState.value.targetDistanceMeters,
            splitDistanceMeters = _uiState.value.splitDistanceMeters,
            strokeType = _uiState.value.strokeType,
            startMode = _uiState.value.startMode.name,
            intervalOffsetSeconds = _uiState.value.intervalOffsetSeconds,
            laneCount = _uiState.value.laneCount
        )
        preferencesRepository?.saveSeriesConfig(
            isEnabled = _uiState.value.seriesConfig.isEnabled,
            mode = _uiState.value.seriesConfig.mode.name,
            restSec = _uiState.value.seriesConfig.restDurationSeconds,
            sendOffSec = _uiState.value.seriesConfig.sendOffDurationSeconds,
            maxCount = _uiState.value.seriesConfig.maxSeriesCount ?: -1
        )
    }

    fun setSeriesConfig(config: SeriesConfig) {
        _uiState.update { it.copy(seriesConfig = config) }
        savePreferences()
    }

    fun initializeLanes(count: Int) {
        val clamped = count.coerceIn(1, 15)
        _uiState.update { currentState ->
            val currentDist = currentState.targetDistanceMeters
            val currentSplit = currentState.splitDistanceMeters
            val currentStroke = currentState.strokeType
            
            validateDistances(currentDist, currentSplit)
            
            val initialLanes = (1..clamped).map { laneNum ->
                LaneState(
                    laneNumber = laneNum,
                    swimmerName = "Bahn $laneNum",
                    targetDistanceMeters = currentDist,
                    splitDistanceMeters = currentSplit,
                    strokeType = currentStroke
                )
            }
            currentState.copy(
                laneCount = clamped,
                lanes = initialLanes,
                isCurrentRunSaved = false
            )
        }
        savePreferences()
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (isActive) {
                updateTick()
                val hasCountdown = _uiState.value.lanes.any { it.status == LaneStatus.COUNTDOWN || it.status == LaneStatus.REST_COUNTDOWN }
                val hasRunning = _uiState.value.lanes.any { it.status == LaneStatus.RUNNING }
                if (!hasCountdown && !hasRunning) {
                    break
                }
                if (hasCountdown) {
                    delay(50.milliseconds)
                } else {
                    delay(200.milliseconds)
                }
            }
            tickerJob = null
        }
    }

    private fun stopTickerIfAllIdle() {
        val anyActiveOrFinished = _uiState.value.lanes.any {
            it.status == LaneStatus.RUNNING ||
                it.status == LaneStatus.COUNTDOWN ||
                it.status == LaneStatus.REST_COUNTDOWN ||
                it.status == LaneStatus.FINISHED
        }
        if (!anyActiveOrFinished) {
            tickerJob?.cancel()
            tickerJob = null
        }
    }

    private fun computeExactLaneElapsed(lane: LaneState, now: Long, masterStartTimeRealtime: Long?): Long {
        val masterStart = masterStartTimeRealtime ?: return lane.currentElapsedMillis
        val start = lane.laneStartTimeRealtime ?: (masterStart + lane.intervalOffsetMillis)
        return (now - start).coerceAtLeast(0L)
    }

    private fun updateTick() {
        val now = clock.nowMillis()
        var playStartSignalThisTick = false
        var lowestCountdownBeepThisTick: Int? = null

        _uiState.update { currentState ->
            val masterStart = currentState.masterStartTimeRealtime ?: now

            val updatedLanes = currentState.lanes.map { lane ->
                when (lane.status) {
                    LaneStatus.COUNTDOWN -> {
                        val laneStartTarget = masterStart + lane.intervalOffsetMillis
                        if (now >= laneStartTarget) {
                            playStartSignalThisTick = true
                            lane.copy(
                                status = LaneStatus.RUNNING,
                                laneStartTimeRealtime = laneStartTarget,
                                countdownRemainingSeconds = 0
                            )
                        } else {
                            val remainingMs = (laneStartTarget - now).coerceAtLeast(0L)
                            val remainingSec = ((remainingMs + 999L) / 1000L).toInt()
                            if (remainingSec != lane.countdownRemainingSeconds && remainingSec in 1..3) {
                                lowestCountdownBeepThisTick = minOf(lowestCountdownBeepThisTick ?: remainingSec, remainingSec)
                            }
                            lane.copy(countdownRemainingSeconds = remainingSec)
                        }
                    }
                    LaneStatus.REST_COUNTDOWN -> {
                        val restTarget = lane.restTargetRealtime
                        if (restTarget == null || now >= restTarget) {
                            playStartSignalThisTick = true
                            val (newCompletedLaps, newSeriesCount) = lane.preservePreviousSeriesIfAny()
                            lane.copy(
                                status = LaneStatus.RUNNING,
                                laneStartTimeRealtime = restTarget ?: now,
                                finishTimeMillis = null,
                                finishRealtimeMillis = null,
                                restTargetRealtime = null,
                                restRemainingSeconds = 0,
                                currentElapsedMillis = 0L,
                                laps = emptyList(),
                                completedSeriesLaps = newCompletedLaps,
                                seriesCount = newSeriesCount
                            )
                        } else {
                            val remainingMs = (restTarget - now).coerceAtLeast(0L)
                            val remainingSec = ((remainingMs + 999L) / 1000L).toInt()
                            if (remainingSec != lane.restRemainingSeconds && remainingSec in 1..3) {
                                lowestCountdownBeepThisTick = minOf(lowestCountdownBeepThisTick ?: remainingSec, remainingSec)
                            }
                            lane.copy(restRemainingSeconds = remainingSec)
                        }
                    }
                    LaneStatus.RUNNING -> lane
                    LaneStatus.FINISHED -> lane
                    LaneStatus.PAUSED -> lane
                    LaneStatus.IDLE -> lane
                }
            }

            if (currentState.lanes != updatedLanes) {
                currentState.copy(lanes = updatedLanes)
            } else {
                currentState
            }
        }

        if (playStartSignalThisTick) {
            SoundUtil.playStartSignal()
        } else if (lowestCountdownBeepThisTick != null) {
            SoundUtil.playCountdownBeep(lowestCountdownBeepThisTick!!)
        }
    }

    // --- Time Control Actions ---

    private fun LaneState.preservePreviousSeriesIfAny(): Pair<List<LapItem>, Int> {
        return if (this.laps.isNotEmpty() || this.status == LaneStatus.FINISHED) {
            val taggedLaps = this.laps.map { it.copy(seriesIndex = this.seriesCount) }
            Pair(this.completedSeriesLaps + taggedLaps, this.seriesCount + 1)
        } else {
            Pair(this.completedSeriesLaps, this.seriesCount)
        }
    }

    fun onStartAll() {
        SoundUtil.playStartSignal()
        val now = clock.nowMillis()

        pushUndo(UndoAction.BulkStart(_uiState.value.lanes, _uiState.value.masterStartTimeRealtime))

        _uiState.update { currentState ->
            val currentLanes = currentState.lanes
            val mode = currentState.startMode
            val intervalSec = currentState.intervalOffsetSeconds

            val newLanes = currentLanes.mapIndexed { index, lane ->
                val (newCompletedLaps, newSeriesCount) = lane.preservePreviousSeriesIfAny()
                if (mode == StartMode.INTERVAL && index > 0) {
                    val offsetMillis = index * intervalSec * 1000L
                    lane.copy(
                        status = LaneStatus.COUNTDOWN,
                        laneStartTimeRealtime = null,
                        finishTimeMillis = null,
                        finishRealtimeMillis = null,
                        intervalOffsetMillis = offsetMillis,
                        countdownRemainingSeconds = index * intervalSec,
                        currentElapsedMillis = 0L,
                        laps = emptyList(),
                        completedSeriesLaps = newCompletedLaps,
                        seriesCount = newSeriesCount
                    )
                } else {
                    lane.copy(
                        status = LaneStatus.RUNNING,
                        laneStartTimeRealtime = now,
                        finishTimeMillis = null,
                        finishRealtimeMillis = null,
                        intervalOffsetMillis = 0L,
                        currentElapsedMillis = 0L,
                        laps = emptyList(),
                        completedSeriesLaps = newCompletedLaps,
                        seriesCount = newSeriesCount,
                        countdownRemainingSeconds = 0
                    )
                }
            }

            currentState.copy(
                masterStartTimeRealtime = now,
                lanes = newLanes,
                isCurrentRunSaved = false
            )
        }
        startTicker()
    }

    fun onStartSingleLane(laneNumber: Int) {
        SoundUtil.playStartSignal()
        val now = clock.nowMillis()
        
        pushUndo(UndoAction.BulkStop(_uiState.value.lanes))

        _uiState.update { currentState ->
            val lanes = currentState.lanes.toMutableList()
            val index = lanes.indexOfFirst { it.laneNumber == laneNumber }
            if (index == -1) return@update currentState

            val lane = lanes[index]
            val (newCompletedLaps, newSeriesCount) = lane.preservePreviousSeriesIfAny()
            
            lanes[index] = lane.copy(
                status = LaneStatus.RUNNING,
                laneStartTimeRealtime = now,
                finishTimeMillis = null,
                finishRealtimeMillis = null,
                currentElapsedMillis = 0L,
                laps = emptyList(),
                completedSeriesLaps = newCompletedLaps,
                seriesCount = newSeriesCount,
                intervalOffsetMillis = 0L,
                countdownRemainingSeconds = 0
            )

            currentState.copy(
                lanes = lanes,
                isCurrentRunSaved = false,
                masterStartTimeRealtime = currentState.masterStartTimeRealtime ?: now
            )
        }
        startTicker()
    }

    private var waveAudioJob: Job? = null

    /**
     * Startet alle besetzten (oder alle) Bahnen gestaffelt im festen Abstand
     * (z. B. staggerIntervalMillis = 5_000L für +5s Wellen-Start).
     */
    @Suppress("unused")
    fun startStaggeredWave(staggerIntervalMillis: Long = 5_000L) {
        SoundUtil.playStartSignal()
        val baseStartRealtime = clock.nowMillis()
        var waveIndex = 0

        pushUndo(UndoAction.BulkStart(_uiState.value.lanes, _uiState.value.masterStartTimeRealtime))

        _uiState.update { state ->
            val updatedLanes = state.lanes.map { lane ->
                val scheduledStartRealtime = baseStartRealtime + (waveIndex * staggerIntervalMillis)
                waveIndex++
                val (newCompletedLaps, newSeriesCount) = lane.preservePreviousSeriesIfAny()
                lane.copy(
                    status = LaneStatus.RUNNING,
                    laneStartTimeRealtime = scheduledStartRealtime,
                    finishTimeMillis = null,
                    currentElapsedMillis = 0L,
                    laps = emptyList(),
                    completedSeriesLaps = newCompletedLaps,
                    seriesCount = newSeriesCount,
                    intervalOffsetMillis = 0L,
                    countdownRemainingSeconds = 0,
                    spmTapStartRealtime = null,
                    currentSpm = null
                )
            }
            state.copy(
                masterStartTimeRealtime = baseStartRealtime,
                lanes = updatedLanes,
                isCurrentRunSaved = false
            )
        }
        startTicker()
        scheduleWaveAudioSignals(waveCount = waveIndex, staggerIntervalMillis = staggerIntervalMillis)
    }

    private fun scheduleWaveAudioSignals(waveCount: Int, staggerIntervalMillis: Long) {
        waveAudioJob?.cancel()
        if (waveCount <= 1 || staggerIntervalMillis <= 0L) return

        waveAudioJob = viewModelScope.launch(Dispatchers.Default) {
            val baseTime = clock.nowMillis()
            for (wave in 1 until waveCount) {
                val targetTime = baseTime + wave * staggerIntervalMillis
                val beepTime = targetTime - 1_000L
                val delayToBeep = beepTime - clock.nowMillis()
                if (delayToBeep > 0) {
                    delay(delayToBeep.milliseconds)
                    SoundUtil.playCountdownBeep()
                }
                val delayToStart = targetTime - clock.nowMillis()
                if (delayToStart > 0) {
                    delay(delayToStart.milliseconds)
                    SoundUtil.playStartSignal()
                }
            }
        }
    }

    /**
     * Live-3-Zyklen-SPM-Messung direkt auf der LaneTimerCard während der Laufzeit:
     * 1. Tap = Startet 3-Zyklen-Messung
     * 2. Tap = Berechnet SPM = (3 * 60_000) / deltaMs und speichert den Wert auf der Bahn
     */
    fun onTapStrokeRate(laneNumber: Int) {
        val now = clock.nowMillis()
        _uiState.update { state ->
            val lanes = state.lanes.map { lane ->
                if (lane.laneNumber != laneNumber) return@map lane
                val tapStart = lane.spmTapStartRealtime
                if (tapStart == null) {
                    lane.copy(spmTapStartRealtime = now)
                } else {
                    val deltaMs = (now - tapStart).coerceAtLeast(500L)
                    val calculatedSpm = (3f * 60_000f) / deltaMs.toFloat()
                    val roundedSpm = kotlin.math.round(calculatedSpm * 10f) / 10.0
                    lane.copy(
                        spmTapStartRealtime = null,
                        currentSpm = roundedSpm
                    )
                }
            }
            state.copy(lanes = lanes)
        }
    }

    private fun resolveFinishLaneState(
        lane: LaneState,
        updatedLaps: List<LapItem>,
        exactRealtime: Long,
        seriesConfig: SeriesConfig
    ): LaneState {
        val maxCount = seriesConfig.maxSeriesCount
        val isMaxReached = maxCount != null && lane.seriesCount >= maxCount

        if (!seriesConfig.isEnabled || isMaxReached || seriesConfig.mode == SeriesModeType.MANUAL) {
            val finalTime = updatedLaps.lastOrNull()?.splitTimeMillis ?: 0L
            return lane.copy(
                status = LaneStatus.FINISHED,
                finishTimeMillis = finalTime,
                finishRealtimeMillis = exactRealtime,
                currentElapsedMillis = finalTime,
                laps = updatedLaps,
                restTargetRealtime = null,
                restRemainingSeconds = 0
            )
        }

        if (seriesConfig.mode == SeriesModeType.FIXED_REST) {
            val finalTime = updatedLaps.lastOrNull()?.splitTimeMillis ?: 0L
            val restTarget = exactRealtime + (seriesConfig.restDurationSeconds * 1000L)
            return lane.copy(
                status = LaneStatus.REST_COUNTDOWN,
                finishTimeMillis = finalTime,
                finishRealtimeMillis = exactRealtime,
                currentElapsedMillis = finalTime,
                laps = updatedLaps,
                restTargetRealtime = restTarget,
                restRemainingSeconds = seriesConfig.restDurationSeconds
            )
        }

        // SEND_OFF mode
        val sendOffMs = seriesConfig.sendOffDurationSeconds * 1000L
        val cycleStart = lane.laneStartTimeRealtime ?: exactRealtime
        val cycleTarget = cycleStart + sendOffMs

        if (exactRealtime < cycleTarget) {
            // Normalfall: swimmer finished before send-off cycle expired
            val finalTime = updatedLaps.lastOrNull()?.splitTimeMillis ?: 0L
            val restSec = (((cycleTarget - exactRealtime) + 999L) / 1000L).toInt()
            return lane.copy(
                status = LaneStatus.REST_COUNTDOWN,
                finishTimeMillis = finalTime,
                finishRealtimeMillis = exactRealtime,
                currentElapsedMillis = finalTime,
                laps = updatedLaps,
                restTargetRealtime = cycleTarget,
                restRemainingSeconds = restSec
            )
        } else {
            // OVERRUN FALL: swimmer finished after send-off cycle expired!
            // Clean series transition immediately
            val taggedLaps = updatedLaps.map { it.copy(seriesIndex = lane.seriesCount) }
            val newCompletedLaps = lane.completedSeriesLaps + taggedLaps
            val newSeriesCount = lane.seriesCount + 1
            SoundUtil.playStartSignal()
            return lane.copy(
                status = LaneStatus.RUNNING,
                laneStartTimeRealtime = exactRealtime,
                finishTimeMillis = null,
                finishRealtimeMillis = null,
                restTargetRealtime = null,
                restRemainingSeconds = 0,
                currentElapsedMillis = 0L,
                laps = emptyList(),
                completedSeriesLaps = newCompletedLaps,
                seriesCount = newSeriesCount
            )
        }
    }

    fun onStopAll() {
        val now = clock.nowMillis()
        pushUndo(UndoAction.BulkStop(_uiState.value.lanes))

        _uiState.update { currentState ->
            val currentLanes = currentState.lanes
            val newLanes = currentLanes.map { lane ->
                when (lane.status) {
                    LaneStatus.COUNTDOWN -> {
                        lane.copy(
                            status = LaneStatus.IDLE,
                            laneStartTimeRealtime = null,
                            finishTimeMillis = null,
                            currentElapsedMillis = 0L,
                            countdownRemainingSeconds = 0
                        )
                    }
                    LaneStatus.RUNNING -> {
                        val exactFinalTime = roundToHundredths(computeExactLaneElapsed(lane, now, currentState.masterStartTimeRealtime))
                        val previousSplit = lane.laps.lastOrNull()?.splitTimeMillis ?: 0L
                        val lapTime = (exactFinalTime - previousSplit).coerceAtLeast(0L)
                        val totalSplitsNeeded = (lane.targetDistanceMeters / lane.splitDistanceMeters).coerceAtLeast(1)

                        val finishLap = if (exactFinalTime > 0L && lane.laps.size < totalSplitsNeeded) {
                            LapItem(
                                lapIndex = totalSplitsNeeded,
                                splitTimeMillis = exactFinalTime,
                                lapTimeMillis = lapTime,
                                strokeRateSpm = lane.currentSpm,
                                seriesIndex = lane.seriesCount
                            )
                        } else null

                        val updatedLaps = if (finishLap != null) lane.laps + finishLap else lane.laps

                        lane.copy(
                            status = LaneStatus.FINISHED,
                            finishTimeMillis = exactFinalTime,
                            finishRealtimeMillis = now,
                            currentElapsedMillis = exactFinalTime,
                            laps = updatedLaps
                        )
                    }
                    LaneStatus.PAUSED -> {
                        val finalTime = lane.currentElapsedMillis
                        lane.copy(
                            status = LaneStatus.FINISHED,
                            finishTimeMillis = finalTime,
                            finishRealtimeMillis = now
                        )
                    }
                    LaneStatus.REST_COUNTDOWN -> {
                        val finalTime = lane.finishTimeMillis ?: lane.currentElapsedMillis
                        lane.copy(
                            status = LaneStatus.FINISHED,
                            finishTimeMillis = finalTime,
                            finishRealtimeMillis = now,
                            restTargetRealtime = null,
                            restRemainingSeconds = 0
                        )
                    }
                    else -> lane
                }
            }

            currentState.copy(
                lanes = newLanes,
                isCurrentRunSaved = false
            )
        }
        stopTickerIfAllIdle()
    }

    fun onPauseLane(laneNumber: Int) {
        val now = clock.nowMillis()
        var anyRunning = false
        _uiState.update { currentState ->
            val lanes = currentState.lanes.toMutableList()
            val index = lanes.indexOfFirst { it.laneNumber == laneNumber }
            if (index == -1) return@update currentState

            val lane = lanes[index]
            if (lane.status != LaneStatus.RUNNING) return@update currentState

            val elapsed = computeExactLaneElapsed(lane, now, currentState.masterStartTimeRealtime)

            lanes[index] = lane.copy(
                status = LaneStatus.PAUSED,
                currentElapsedMillis = elapsed
            )

            anyRunning = lanes.any { it.status == LaneStatus.RUNNING || it.status == LaneStatus.COUNTDOWN || it.status == LaneStatus.PAUSED || it.status == LaneStatus.REST_COUNTDOWN }
            currentState.copy(lanes = lanes)
        }
        if (!anyRunning) stopTickerIfAllIdle()
    }

    fun onResumeLane(laneNumber: Int) {
        val now = clock.nowMillis()
        
        _uiState.update { currentState ->
            val lanes = currentState.lanes.toMutableList()
            val index = lanes.indexOfFirst { it.laneNumber == laneNumber }
            if (index == -1) return@update currentState

            val lane = lanes[index]
            if (lane.status != LaneStatus.PAUSED) return@update currentState

            lanes[index] = lane.copy(
                status = LaneStatus.RUNNING,
                laneStartTimeRealtime = now - lane.currentElapsedMillis,
                finishTimeMillis = null,
                finishRealtimeMillis = null
            )

            currentState.copy(lanes = lanes)
        }
        startTicker()
    }

    fun onLapLane(laneNumber: Int, exactRealtime: Long = clock.nowMillis()) {
        var pushedUndo: UndoAction? = null

        _uiState.update { currentState ->
            val lanes = currentState.lanes.toMutableList()
            val index = lanes.indexOfFirst { it.laneNumber == laneNumber }
            if (index == -1) return@update currentState

            val lane = lanes[index]
            if (lane.status != LaneStatus.RUNNING) return@update currentState

            val splitTime = roundToHundredths(computeExactLaneElapsed(lane, exactRealtime, currentState.masterStartTimeRealtime))
            val previousSplit = lane.laps.lastOrNull()?.splitTimeMillis ?: 0L
            val lapTime = (splitTime - previousSplit).coerceAtLeast(0L)

            val newLapIndex = lane.laps.size + 1
            val totalSplitsNeeded = (lane.targetDistanceMeters / lane.splitDistanceMeters).coerceAtLeast(1)
            val isFinalLap = newLapIndex >= totalSplitsNeeded

            val newLap = LapItem(
                lapIndex = newLapIndex,
                splitTimeMillis = splitTime,
                lapTimeMillis = lapTime,
                strokeRateSpm = lane.currentSpm,
                seriesIndex = lane.seriesCount
            )

            pushedUndo = UndoAction.LapAdded(
                laneNumber = laneNumber,
                previousStatus = lane.status,
                previousFinishTime = lane.finishTimeMillis,
                previousFinishRealtime = lane.finishRealtimeMillis,
                removedLap = newLap
            )

            lanes[index] = if (isFinalLap) {
                resolveFinishLaneState(lane, lane.laps + newLap, exactRealtime, currentState.seriesConfig)
            } else {
                lane.copy(
                    currentElapsedMillis = splitTime,
                    laps = lane.laps + newLap
                )
            }

            currentState.copy(
                lanes = lanes,
                selectedFocusedLane = laneNumber,
                isCurrentRunSaved = false
            )
        }

        pushedUndo?.let { pushUndo(it) }
        startTicker()
    }

    fun onFinishLane(laneNumber: Int, exactRealtime: Long = clock.nowMillis()) {
        var pushedUndo: UndoAction? = null

        _uiState.update { currentState ->
            val lanes = currentState.lanes.toMutableList()
            val index = lanes.indexOfFirst { it.laneNumber == laneNumber }
            if (index == -1) return@update currentState

            val lane = lanes[index]
            if (lane.status != LaneStatus.RUNNING) return@update currentState

            val finalTime = roundToHundredths(computeExactLaneElapsed(lane, exactRealtime, currentState.masterStartTimeRealtime))
            val previousSplit = lane.laps.lastOrNull()?.splitTimeMillis ?: 0L
            val lapTime = (finalTime - previousSplit).coerceAtLeast(0L)
            val totalSplitsNeeded = (lane.targetDistanceMeters / lane.splitDistanceMeters).coerceAtLeast(1)

            val finishLap = if (finalTime > 0L && lane.laps.size < totalSplitsNeeded) {
                LapItem(
                    lapIndex = totalSplitsNeeded,
                    splitTimeMillis = finalTime,
                    lapTimeMillis = lapTime,
                    strokeRateSpm = lane.currentSpm,
                    seriesIndex = lane.seriesCount
                )
            } else null

            val updatedLaps = if (finishLap != null) lane.laps + finishLap else lane.laps

            pushedUndo = UndoAction.LaneFinished(
                laneNumber = laneNumber,
                previousStatus = lane.status,
                previousFinishTime = lane.finishTimeMillis,
                previousFinishRealtime = lane.finishRealtimeMillis,
                removedFinishLap = finishLap
            )

            lanes[index] = resolveFinishLaneState(lane, updatedLaps, exactRealtime, currentState.seriesConfig)

            currentState.copy(
                lanes = lanes,
                selectedFocusedLane = laneNumber,
                isCurrentRunSaved = false
            )
        }
        
        pushedUndo?.let { pushUndo(it) }
        startTicker()
    }

    @Suppress("unused")
    fun onLapAllRunning() {
        val now = clock.nowMillis()
        
        pushUndo(UndoAction.BulkStop(_uiState.value.lanes)) // Snapshot before bulk action

        _uiState.update { currentState ->
            val currentLanes = currentState.lanes
            val runningLanes = currentLanes.filter { it.status == LaneStatus.RUNNING }
            if (runningLanes.isEmpty()) return@update currentState

            val newLanes = currentLanes.map { lane ->
                if (lane.status != LaneStatus.RUNNING) return@map lane

                val splitTime = roundToHundredths(computeExactLaneElapsed(lane, now, currentState.masterStartTimeRealtime))
                val previousSplit = lane.laps.lastOrNull()?.splitTimeMillis ?: 0L
                val lapTime = (splitTime - previousSplit).coerceAtLeast(0L)

                val newLapIndex = lane.laps.size + 1
                val totalSplitsNeeded = (lane.targetDistanceMeters / lane.splitDistanceMeters).coerceAtLeast(1)
                val isFinalLap = newLapIndex >= totalSplitsNeeded

                val newLap = LapItem(
                    lapIndex = newLapIndex,
                    splitTimeMillis = splitTime,
                    lapTimeMillis = lapTime,
                    strokeRateSpm = lane.currentSpm,
                    seriesIndex = lane.seriesCount
                )

                if (isFinalLap) {
                    resolveFinishLaneState(lane, lane.laps + newLap, now, currentState.seriesConfig)
                } else {
                    lane.copy(
                        currentElapsedMillis = splitTime,
                        laps = lane.laps + newLap
                    )
                }
            }

            currentState.copy(
                lanes = newLanes,
                isCurrentRunSaved = false
            )
        }
    }

    fun onResetAll() {
        tickerJob?.cancel()
        tickerJob = null
        undoStack.clear()

        _uiState.update { currentState ->
            val resetLanes = currentState.lanes.map {
                it.copy(
                    status = LaneStatus.IDLE,
                    laneStartTimeRealtime = null,
                    finishTimeMillis = null,
                    finishRealtimeMillis = null,
                    restTargetRealtime = null,
                    restRemainingSeconds = 0,
                    currentElapsedMillis = 0L,
                    laps = emptyList(),
                    completedSeriesLaps = emptyList(),
                    seriesCount = 1,
                    spmTapStartRealtime = null,
                    currentSpm = null,
                    countdownRemainingSeconds = 0
                )
            }

            currentState.copy(
                masterStartTimeRealtime = null,
                lanes = resetLanes,
                canUndo = false,
                lastUndoMessage = null,
                isCurrentRunSaved = false
            )
        }
    }

    private fun roundToHundredths(millis: Long): Long = ((millis + 5L) / 10L) * 10L

    // --- Undo Logic ---

    fun clearUndoMessage() {
        _uiState.update { it.copy(lastUndoMessage = null) }
    }

    private fun pushUndo(action: UndoAction) {
        undoStack.push(action)
        if (undoStack.size > 20) undoStack.removeLast()
        _uiState.update { it.copy(canUndo = true) }
    }

    fun performUndo() {
        if (undoStack.isEmpty()) return

        when (val action = undoStack.pop()) {
            is UndoAction.LapAdded -> {
                var anyRunning = false
                _uiState.update { currentState ->
                    val lanes = currentState.lanes.toMutableList()
                    val idx = lanes.indexOfFirst { it.laneNumber == action.laneNumber }
                    if (idx != -1) {
                        val currentLaps = lanes[idx].laps.toMutableList()
                        if (currentLaps.isNotEmpty()) {
                            currentLaps.removeAt(currentLaps.lastIndex)
                            lanes[idx] = lanes[idx].copy(
                                status = action.previousStatus,
                                finishTimeMillis = action.previousFinishTime,
                                finishRealtimeMillis = action.previousFinishRealtime,
                                laps = currentLaps
                            )
                            anyRunning = lanes.any { it.status == LaneStatus.RUNNING || it.status == LaneStatus.COUNTDOWN || it.status == LaneStatus.PAUSED }
                            return@update currentState.copy(
                                lanes = lanes,
                                isCurrentRunSaved = false,
                                lastUndoMessage = "Lap ${action.removedLap.lapIndex} auf Bahn ${action.laneNumber} rückgängig gemacht"
                            )
                        }
                    }
                    currentState
                }
                if (anyRunning) startTicker()
            }
            is UndoAction.LaneFinished -> {
                _uiState.update { currentState ->
                    val lanes = currentState.lanes.toMutableList()
                    val idx = lanes.indexOfFirst { it.laneNumber == action.laneNumber }
                    if (idx != -1) {
                        val restoredLaps = if (action.removedFinishLap != null && lanes[idx].laps.isNotEmpty()) {
                            lanes[idx].laps.dropLast(1)
                        } else lanes[idx].laps
                        lanes[idx] = lanes[idx].copy(
                            status = action.previousStatus,
                            finishTimeMillis = action.previousFinishTime,
                            finishRealtimeMillis = action.previousFinishRealtime,
                            laps = restoredLaps
                        )
                        return@update currentState.copy(
                            lanes = lanes,
                            isCurrentRunSaved = false,
                            lastUndoMessage = "Zielstopp für Bahn ${action.laneNumber} rückgängig gemacht"
                        )
                    }
                    currentState
                }
                startTicker()
            }
            is UndoAction.BulkStart -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        lanes = action.previousLanes,
                        masterStartTimeRealtime = action.previousMasterStart,
                        isCurrentRunSaved = false,
                        lastUndoMessage = "Start rückgängig gemacht"
                    )
                }
                stopTickerIfAllIdle()
            }
            is UndoAction.BulkStop -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        lanes = action.previousLanes,
                        isCurrentRunSaved = false,
                        lastUndoMessage = "Stopp rückgängig gemacht"
                    )
                }
                startTicker()
            }
            is UndoAction.LaneRemoved -> {
                _uiState.update { currentState ->
                    val lanes = currentState.lanes.toMutableList()
                    val insertIdx = action.index.coerceIn(0, lanes.size)
                    lanes.add(insertIdx, action.lane)
                    currentState.copy(
                        lanes = lanes,
                        laneCount = lanes.size,
                        lastUndoMessage = "Bahn ${action.lane.laneNumber} wiederhergestellt"
                    )
                }
            }
        }
        _uiState.update { it.copy(canUndo = undoStack.isNotEmpty()) }
    }

    // --- Configuration & Settings ---

    @Suppress("unused")
    fun setLaneCount(count: Int) {
        initializeLanes(count)
    }

    fun addLane() {
        val currentLanes = _uiState.value.lanes
        if (currentLanes.size >= 15) {
            _uiState.update { it.copy(
                lastUndoMessage = "Limit: Maximal 15 Bahnen möglich"
            ) }
            return
        }
        undoStack.clear()
        _uiState.update { it.copy(canUndo = false) }
        
        val nextLaneNumber = (currentLanes.maxOfOrNull { it.laneNumber } ?: 0) + 1
        val newLane = LaneState(
            laneNumber = nextLaneNumber,
            swimmerName = "Bahn $nextLaneNumber",
            targetDistanceMeters = _uiState.value.targetDistanceMeters,
            splitDistanceMeters = _uiState.value.splitDistanceMeters,
            strokeType = _uiState.value.strokeType
        )
        val updated = currentLanes + newLane
        _uiState.update { it.copy(
            lanes = updated,
            laneCount = updated.size,
            lastUndoMessage = "Bahn $nextLaneNumber hinzugefügt"
        ) }
        savePreferences()
    }

    fun removeLane(laneNumber: Int) {
        val currentLanes = _uiState.value.lanes
        if (currentLanes.size <= 1) {
            _uiState.update { it.copy(
                lastUndoMessage = "Mindestens 1 Bahn muss aktiv bleiben"
            ) }
            return
        }
        val index = currentLanes.indexOfFirst { it.laneNumber == laneNumber }
        if (index == -1) return
        val removedLane = currentLanes[index]
        pushUndo(UndoAction.LaneRemoved(removedLane, index))

        val updated = currentLanes.filter { it.laneNumber != laneNumber }
        _uiState.update { it.copy(
            lanes = updated,
            laneCount = updated.size,
            lastUndoMessage = "Bahn $laneNumber entfernt"
        ) }
        savePreferences()
    }

    @Suppress("unused")
    fun removeLastLane() {
        val currentLanes = _uiState.value.lanes
        if (currentLanes.size <= 1) {
            _uiState.update { it.copy(
                lastUndoMessage = "Mindestens 1 Bahn muss aktiv bleiben!"
            ) }
            return
        }
        removeLane(currentLanes.last().laneNumber)
    }

    fun setStartMode(mode: StartMode) {
        _uiState.update { it.copy(startMode = mode) }
        savePreferences()
    }

    fun setIntervalOffsetSeconds(seconds: Int) {
        _uiState.update { it.copy(intervalOffsetSeconds = seconds.coerceIn(1, 60)) }
        savePreferences()
    }

    @Suppress("unused")
    fun setSelectedFocusedLane(laneNumber: Int) {
        _uiState.update { it.copy(selectedFocusedLane = laneNumber) }
    }

    @Suppress("unused")
    fun setSplitDistance(splitMeters: Int) {
        val currentDist = _uiState.value.targetDistanceMeters
        validateDistances(currentDist, splitMeters)
        val updatedLanes = _uiState.value.lanes.map {
            it.copy(splitDistanceMeters = splitMeters)
        }
        _uiState.update { it.copy(
            splitDistanceMeters = splitMeters,
            lanes = updatedLanes
        ) }
        savePreferences()
    }

    @Suppress("unused")
    fun setTargetDistance(distance: Int) {
        val enforcedDistance = if (_uiState.value.strokeType == "Lagen" && distance < 100) 100 else distance
        val currentSplit = _uiState.value.splitDistanceMeters
        val enforcedSplit = if (currentSplit > enforcedDistance) 25 else currentSplit
        validateDistances(enforcedDistance, enforcedSplit)
        val updatedLanes = _uiState.value.lanes.map {
            it.copy(targetDistanceMeters = enforcedDistance, splitDistanceMeters = enforcedSplit)
        }
        _uiState.update { it.copy(
            targetDistanceMeters = enforcedDistance,
            splitDistanceMeters = enforcedSplit,
            lanes = updatedLanes
        ) }
        savePreferences()
    }

    @Suppress("unused")
    fun setStrokeType(stroke: String) {
        val currentDist = _uiState.value.targetDistanceMeters
        val enforcedDist = if (stroke == "Lagen" && currentDist < 100) 100 else currentDist
        val currentSplit = _uiState.value.splitDistanceMeters
        val enforcedSplit = if (stroke == "Lagen" && enforcedDist == 100) 25 else currentSplit
        validateDistances(enforcedDist, enforcedSplit)
        val updatedLanes = _uiState.value.lanes.map {
            it.copy(strokeType = stroke, targetDistanceMeters = enforcedDist, splitDistanceMeters = enforcedSplit)
        }
        _uiState.update { it.copy(
            strokeType = stroke,
            targetDistanceMeters = enforcedDist,
            splitDistanceMeters = enforcedSplit,
            lanes = updatedLanes
        ) }
        savePreferences()
    }

    fun setSessionMeta(title: String, stroke: String, poolLength: Int, distance: Int, splitDistance: Int = 50) {
        val enforcedDistance = if (stroke == "Lagen" && distance < 100) 100 else distance
        val enforcedSplit = if (splitDistance > enforcedDistance) enforcedDistance else splitDistance
        validateDistances(enforcedDistance, enforcedSplit)
        val cleanTitle = title.ifBlank { "${enforcedDistance}m $stroke" }
        val updatedLanes = _uiState.value.lanes.map {
            it.copy(
                targetDistanceMeters = enforcedDistance,
                splitDistanceMeters = enforcedSplit,
                strokeType = stroke
            )
        }
        _uiState.update { it.copy(
            sessionTitle = cleanTitle,
            strokeType = stroke,
            poolLengthMeters = poolLength,
            targetDistanceMeters = enforcedDistance,
            splitDistanceMeters = enforcedSplit,
            lanes = updatedLanes
        ) }
        savePreferences()
    }

    @Suppress("unused")
    fun openPaceCalculator() {
        _uiState.update { it.copy(isPaceCalculatorVisible = true) }
    }

    fun closePaceCalculator() {
        _uiState.update { it.copy(isPaceCalculatorVisible = false) }
    }

    fun openAssignSwimmerDialog(laneNumber: Int) {
        _uiState.update { it.copy(
            isAssignSwimmerDialogVisible = true,
            assignSwimmerLane = laneNumber
        ) }
    }

    fun closeAssignSwimmerDialog() {
        _uiState.update { it.copy(isAssignSwimmerDialogVisible = false) }
    }

    fun assignSwimmerToLane(laneNumber: Int, swimmer: SwimmerEntity?) {
        _uiState.update { currentState ->
            val updatedLanes = currentState.lanes.map { lane ->
                if (lane.laneNumber == laneNumber) {
                    lane.copy(
                        swimmerId = swimmer?.id,
                        swimmerName = swimmer?.name ?: "Bahn $laneNumber"
                    )
                } else if (swimmer != null && lane.swimmerId == swimmer.id) {
                    // Unassign swimmer from previous lane if already assigned elsewhere
                    lane.copy(
                        swimmerId = null,
                        swimmerName = "Bahn ${lane.laneNumber}"
                    )
                } else {
                    lane
                }
            }
            currentState.copy(
                lanes = updatedLanes,
                isAssignSwimmerDialogVisible = false
            )
        }
    }

    // --- Stroke Rate Calculator ---

    @Suppress("unused")
    fun openStrokeRateDialog(targetLane: Int? = null) {
        _uiState.update { it.copy(
            isStrokeRateDialogVisible = true,
            strokeRateTargetLane = targetLane
        ) }
    }

    fun closeStrokeRateDialog() {
        _uiState.update { it.copy(isStrokeRateDialogVisible = false) }
    }

    fun attachStrokeRateToLane(laneNumber: Int, spm: Double) {
        val lanes = _uiState.value.lanes.toMutableList()
        val idx = lanes.indexOfFirst { it.laneNumber == laneNumber }
        if (idx != -1) {
            val laps = lanes[idx].laps.toMutableList()
            if (laps.isNotEmpty()) {
                val lastLap = laps.last()
                laps[laps.lastIndex] = lastLap.copy(strokeRateSpm = spm)
                lanes[idx] = lanes[idx].copy(laps = laps)
                _uiState.update { it.copy(
                    lanes = lanes,
                    isCurrentRunSaved = false
                ) }
            }
        }
    }

    // --- Save Session to Room Database & Automatic PB Update ---

    fun saveCurrentSession() {
        if (_uiState.value.isCurrentRunSaved) return
        _uiState.update { it.copy(isCurrentRunSaved = true) } // Set early to prevent double tap

        viewModelScope.launch {
            val state = _uiState.value
            val activeLanesWithLaps = state.lanes.filter { it.allLaps.isNotEmpty() }
            if (activeLanesWithLaps.isEmpty()) return@launch

            val defaultTitle = "${state.targetDistanceMeters}m ${state.strokeType}"
            val session = SessionEntity(
                title = if (state.sessionTitle.isBlank() || state.sessionTitle == "Trainingseinheit" || state.sessionTitle == "Training") {
                    defaultTitle
                } else {
                    state.sessionTitle
                },
                poolLengthMeters = state.poolLengthMeters,
                targetDistanceMeters = state.targetDistanceMeters,
                strokeType = state.strokeType,
                startMode = state.startMode.name,
                intervalOffsetSeconds = state.intervalOffsetSeconds,
                laneCount = activeLanesWithLaps.size,
                splitDistanceMeters = state.splitDistanceMeters,
                notes = "Erfasst mit SwimTimer am Beckenrand"
            )

            val lapEntities = mutableListOf<LapRecordEntity>()
            activeLanesWithLaps.forEach { lane ->
                lane.allLaps.forEachIndexed { globalIndex, lap ->
                    lapEntities.add(
                        LapRecordEntity(
                            sessionId = 0, // Assigned by repository
                            swimmerId = lane.swimmerId,
                            swimmerName = lane.swimmerName,
                            laneNumber = lane.laneNumber,
                            lapIndex = globalIndex + 1,
                            splitTimeMillis = lap.splitTimeMillis,
                            lapTimeMillis = lap.lapTimeMillis,
                            targetLapMillis = null,
                            deltaTargetMillis = null,
                            strokeRateSpm = lap.strokeRateSpm
                        )
                    )
                }
            }

            repository.saveSessionWithLaps(session, lapEntities)

            // Automatic PB update check for assigned athletes
            val expectedSplitsForFullDistance = (state.targetDistanceMeters / state.splitDistanceMeters).coerceAtLeast(1)
            val pbAchievements = mutableListOf<String>()

            activeLanesWithLaps.forEach { lane ->
                val sId = lane.swimmerId
                if (sId != null) {
                    val swimmer = repository.getSwimmerById(sId)
                    if (swimmer != null) {
                        var updatedPb25 = swimmer.pb25mMillis
                        var updatedPb50 = swimmer.pb50mMillis
                        var updatedPb100 = swimmer.pb100mMillis
                        var updatedPb100Lagen = swimmer.pb100mLagenMillis
                        var updatedPb200Lagen = swimmer.pb200mLagenMillis
                        var updatedPb400Lagen = swimmer.pb400mLagenMillis

                        // 100m Lagen Splits (4x 25m)
                        var updatedFly = swimmer.lagenFlySplitMillis
                        var updatedBack = swimmer.lagenBackSplitMillis
                        var updatedBreast = swimmer.lagenBreastSplitMillis
                        var updatedFree = swimmer.lagenFreeSplitMillis

                        // 200m Lagen Splits (4x 50m)
                        var updatedPb200Fly = swimmer.pb200mLagenFlySplitMillis
                        var updatedPb200Back = swimmer.pb200mLagenBackSplitMillis
                        var updatedPb200Breast = swimmer.pb200mLagenBreastSplitMillis
                        var updatedPb200Free = swimmer.pb200mLagenFreeSplitMillis

                        // 400m Lagen Splits (4x 100m)
                        var updatedPb400Fly = swimmer.pb400mLagenFlySplitMillis
                        var updatedPb400Back = swimmer.pb400mLagenBackSplitMillis
                        var updatedPb400Breast = swimmer.pb400mLagenBreastSplitMillis
                        var updatedPb400Free = swimmer.pb400mLagenFreeSplitMillis

                        val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")
                        val matchedStroke = strokeOptions.find { isSameStroke(it, state.strokeType) } ?: state.strokeType
                        val customPbsMap = swimmer.parseCustomPbsMap().toMutableMap()

                        var hasImproved = false

                        val runs = lane.allLaps.groupBy { it.seriesIndex }.values
                        runs.forEach { seriesLaps ->
                            val hasTargetLap = seriesLaps.any { it.lapIndex == expectedSplitsForFullDistance }
                            val completedFullDistance = hasTargetLap || (seriesLaps.size >= expectedSplitsForFullDistance) || (seriesLaps.isNotEmpty() && (lane.status == LaneStatus.FINISHED || lane.status == LaneStatus.REST_COUNTDOWN || lane.finishTimeMillis != null))
                            val finishTime = if (completedFullDistance) {
                                seriesLaps.find { it.lapIndex == expectedSplitsForFullDistance }?.splitTimeMillis ?: seriesLaps.lastOrNull()?.splitTimeMillis
                            } else null

                            // Helper to find exact cumulative split time at a specific distance (e.g. 25m, 50m, 100m)
                            fun getCumulativeSplitAtMeters(meters: Int): Long? {
                                if (meters == state.targetDistanceMeters && finishTime != null && finishTime > 0) return finishTime
                                if (meters % state.splitDistanceMeters != 0) return null
                                val expectedLapIndex = meters / state.splitDistanceMeters
                                val lap = seriesLaps.find { it.lapIndex == expectedLapIndex }
                                return lap?.splitTimeMillis?.takeIf { it > 0L }
                            }

                            // Check multi-stroke PBs in customPbsMap for any stroke
                            if (finishTime != null && finishTime > 0) {
                                val currentCustomPb = customPbsMap[matchedStroke to state.targetDistanceMeters]
                                if (currentCustomPb == null || finishTime < currentCustomPb) {
                                    customPbsMap[matchedStroke to state.targetDistanceMeters] = finishTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} ${state.targetDistanceMeters}m $matchedStroke: ${TimeFormatter.formatMillis(finishTime)}")
                                }
                            }

                            // Non-Lagen: Check 25m, 50m, and 100m PBs for favorite stroke
                            if (state.strokeType != "Lagen" && isSameStroke(state.strokeType, swimmer.favoriteStroke)) {
                                val timeAt25m = if (state.targetDistanceMeters == 25) finishTime else getCumulativeSplitAtMeters(25)
                                if (timeAt25m != null && (updatedPb25 == null || timeAt25m < updatedPb25)) {
                                    updatedPb25 = timeAt25m
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} 25m ${swimmer.favoriteStroke}: ${TimeFormatter.formatMillis(timeAt25m, shortFormat = true)}")
                                }

                                val timeAt50m = if (state.targetDistanceMeters == 50) finishTime else getCumulativeSplitAtMeters(50)
                                if (timeAt50m != null && (updatedPb50 == null || timeAt50m < updatedPb50)) {
                                    updatedPb50 = timeAt50m
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} 50m ${swimmer.favoriteStroke}: ${TimeFormatter.formatMillis(timeAt50m, shortFormat = true)}")
                                }

                                val timeAt100m = if (state.targetDistanceMeters == 100) finishTime else getCumulativeSplitAtMeters(100)
                                if (timeAt100m != null && (updatedPb100 == null || timeAt100m < updatedPb100)) {
                                    updatedPb100 = timeAt100m
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} 100m ${swimmer.favoriteStroke}: ${TimeFormatter.formatMillis(timeAt100m)}")
                                }
                            }

                            // Lagen (Official Einzel-Reihenfolge: 1. Schmett, 2. Rücken, 3. Brust, 4. Freistil)
                            if (state.strokeType == "Lagen") {
                                // Total Lagen PBs (only when full distance was completed!)
                                if (state.targetDistanceMeters == 100 && finishTime != null && finishTime > 0) {
                                    if (updatedPb100Lagen == null || finishTime < updatedPb100Lagen) {
                                        updatedPb100Lagen = finishTime
                                        hasImproved = true
                                        pbAchievements.add("${swimmer.name} 100m Lagen: ${TimeFormatter.formatMillis(finishTime)}")
                                    }
                                } else if (state.targetDistanceMeters == 200 && finishTime != null && finishTime > 0) {
                                    if (updatedPb200Lagen == null || finishTime < updatedPb200Lagen) {
                                        updatedPb200Lagen = finishTime
                                        hasImproved = true
                                        pbAchievements.add("${swimmer.name} 200m Lagen: ${TimeFormatter.formatMillis(finishTime)}")
                                    }
                                } else if (state.targetDistanceMeters == 400 && finishTime != null && finishTime > 0) {
                                    if (updatedPb400Lagen == null || finishTime < updatedPb400Lagen) {
                                        updatedPb400Lagen = finishTime
                                        hasImproved = true
                                        pbAchievements.add("${swimmer.name} 400m Lagen: ${TimeFormatter.formatMillis(finishTime)}")
                                    }
                                }

                                val legDist = (state.targetDistanceMeters / 4).coerceAtLeast(1)
                                // Only evaluate individual medley legs if splitDistance cleanly divides legDist
                                if (legDist % state.splitDistanceMeters == 0) {
                                    val splitsPerLeg = (legDist / state.splitDistanceMeters).coerceAtLeast(1)

                                    fun getLegTime(legIndex: Int): Long? {
                                        val startIdx = legIndex * splitsPerLeg
                                        val endIdx = startIdx + splitsPerLeg
                                        if (seriesLaps.size >= endIdx) {
                                            var sum = 0L
                                            for (i in startIdx until endIdx) {
                                                sum += seriesLaps[i].lapTimeMillis
                                            }
                                            return if (sum > 0) sum else null
                                        }
                                        return null
                                    }

                                val flyLegTime = getLegTime(0)
                                val backLegTime = getLegTime(1)
                                val breastLegTime = getLegTime(2)
                                val freeLegTime = getLegTime(3)

                                if (state.targetDistanceMeters == 200) {
                                    // 1. Schmett (50m)
                                    if (flyLegTime != null) {
                                        if (updatedPb200Fly == null || flyLegTime < updatedPb200Fly) {
                                            updatedPb200Fly = flyLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (200L 1. Schmett (50m): ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Schmetterling") && (updatedPb50 == null || flyLegTime < updatedPb50)) {
                                            updatedPb50 = flyLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (50m Schmett PB via 200L: ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                                        }
                                    }
                                    // 2. Rücken (50m)
                                    if (backLegTime != null) {
                                        if (updatedPb200Back == null || backLegTime < updatedPb200Back) {
                                            updatedPb200Back = backLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (200L 2. Rücken (50m): ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Rücken") && (updatedPb50 == null || backLegTime < updatedPb50)) {
                                            updatedPb50 = backLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (50m Rücken PB via 200L: ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                                        }
                                    }
                                    // 3. Brust (50m)
                                    if (breastLegTime != null) {
                                        if (updatedPb200Breast == null || breastLegTime < updatedPb200Breast) {
                                            updatedPb200Breast = breastLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (200L 3. Brust (50m): ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Brust") && (updatedPb50 == null || breastLegTime < updatedPb50)) {
                                            updatedPb50 = breastLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (50m Brust PB via 200L: ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                                        }
                                    }
                                    // 4. Freistil / Kraul (50m)
                                    if (freeLegTime != null) {
                                        if (updatedPb200Free == null || freeLegTime < updatedPb200Free) {
                                            updatedPb200Free = freeLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (200L 4. Kraul (50m): ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Freistil") && (updatedPb50 == null || freeLegTime < updatedPb50)) {
                                            updatedPb50 = freeLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (50m Freistil PB via 200L: ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                                        }
                                    }
                                } else if (state.targetDistanceMeters == 100) {
                                    // 100m Lagen: 4x 25m Teilstrecken
                                    if (flyLegTime != null) {
                                        if (updatedFly == null || flyLegTime < updatedFly) {
                                            updatedFly = flyLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (100L 1. Schmett (25m): ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Schmetterling") && (updatedPb25 == null || flyLegTime < updatedPb25)) {
                                            updatedPb25 = flyLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (25m Schmett PB via 100L: ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                                        }
                                    }
                                    if (backLegTime != null) {
                                        if (updatedBack == null || backLegTime < updatedBack) {
                                            updatedBack = backLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (100L 2. Rücken (25m): ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Rücken") && (updatedPb25 == null || backLegTime < updatedPb25)) {
                                            updatedPb25 = backLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (25m Rücken PB via 100L: ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                                        }
                                    }
                                    if (breastLegTime != null) {
                                        if (updatedBreast == null || breastLegTime < updatedBreast) {
                                            updatedBreast = breastLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (100L 3. Brust (25m): ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Brust") && (updatedPb25 == null || breastLegTime < updatedPb25)) {
                                            updatedPb25 = breastLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (25m Brust PB via 100L: ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                                        }
                                    }
                                    if (freeLegTime != null) {
                                        if (updatedFree == null || freeLegTime < updatedFree) {
                                            updatedFree = freeLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (100L 4. Freistil (25m): ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Freistil") && (updatedPb25 == null || freeLegTime < updatedPb25)) {
                                            updatedPb25 = freeLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (25m Freistil PB via 100L: ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                                        }
                                    }
                                } else if (state.targetDistanceMeters == 400) {
                                    // 400m Lagen: 4x 100m Teilstrecken
                                    if (flyLegTime != null) {
                                        if (updatedPb400Fly == null || flyLegTime < updatedPb400Fly) {
                                            updatedPb400Fly = flyLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (400L 1. Schmett (100m): ${TimeFormatter.formatMillis(flyLegTime)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Schmetterling") && (updatedPb100 == null || flyLegTime < updatedPb100)) {
                                            updatedPb100 = flyLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (100m Schmett PB via 400L: ${TimeFormatter.formatMillis(flyLegTime)})")
                                        }
                                    }
                                    if (backLegTime != null) {
                                        if (updatedPb400Back == null || backLegTime < updatedPb400Back) {
                                            updatedPb400Back = backLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (400L 2. Rücken (100m): ${TimeFormatter.formatMillis(backLegTime)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Rücken") && (updatedPb100 == null || backLegTime < updatedPb100)) {
                                            updatedPb100 = backLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (100m Rücken PB via 400L: ${TimeFormatter.formatMillis(backLegTime)})")
                                        }
                                    }
                                    if (breastLegTime != null) {
                                        if (updatedPb400Breast == null || breastLegTime < updatedPb400Breast) {
                                            updatedPb400Breast = breastLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (400L 3. Brust (100m): ${TimeFormatter.formatMillis(breastLegTime)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Brust") && (updatedPb100 == null || breastLegTime < updatedPb100)) {
                                            updatedPb100 = breastLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (100m Brust PB via 400L: ${TimeFormatter.formatMillis(breastLegTime)})")
                                        }
                                    }
                                    if (freeLegTime != null) {
                                        if (updatedPb400Free == null || freeLegTime < updatedPb400Free) {
                                            updatedPb400Free = freeLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (400L 4. Freistil (100m): ${TimeFormatter.formatMillis(freeLegTime)})")
                                        }
                                        if (isSameStroke(swimmer.favoriteStroke, "Freistil") && (updatedPb100 == null || freeLegTime < updatedPb100)) {
                                            updatedPb100 = freeLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (100m Freistil PB via 400L: ${TimeFormatter.formatMillis(freeLegTime)})")
                                        }
                                    }
                                }
                            }
                        }
                        }

                        if (hasImproved) {
                            repository.updateSwimmer(
                                swimmer.copy(
                                    pb25mMillis = updatedPb25,
                                    pb50mMillis = updatedPb50,
                                    pb100mMillis = updatedPb100,
                                    pb100mLagenMillis = updatedPb100Lagen,
                                    pb200mLagenMillis = updatedPb200Lagen,
                                    pb400mLagenMillis = updatedPb400Lagen,
                                    lagenFlySplitMillis = updatedFly,
                                    lagenBackSplitMillis = updatedBack,
                                    lagenBreastSplitMillis = updatedBreast,
                                    lagenFreeSplitMillis = updatedFree,
                                    pb200mLagenFlySplitMillis = updatedPb200Fly,
                                    pb200mLagenBackSplitMillis = updatedPb200Back,
                                    pb200mLagenBreastSplitMillis = updatedPb200Breast,
                                    pb200mLagenFreeSplitMillis = updatedPb200Free,
                                    pb400mLagenFlySplitMillis = updatedPb400Fly,
                                    pb400mLagenBackSplitMillis = updatedPb400Back,
                                    pb400mLagenBreastSplitMillis = updatedPb400Breast,
                                    pb400mLagenFreeSplitMillis = updatedPb400Free,
                                    customPbsJson = SwimmerEntity.serializeCustomPbsMap(customPbsMap)
                                )
                            )
                        }
                    }
                }
            }

            val successMsg = if (pbAchievements.isNotEmpty()) {
                "Gespeichert! Neue PB: ${pbAchievements.joinToString(", ")}"
            } else {
                "Trainingseinheit erfolgreich gespeichert!"
            }

            _uiState.update { it.copy(
                isSessionSavedSuccess = true,
                isCurrentRunSaved = true,
                saveNotificationMessage = successMsg
            ) }
        }
    }

    fun clearSavedSuccess() {
        _uiState.update { it.copy(
            isSessionSavedSuccess = false,
            saveNotificationMessage = null
        ) }
    }

    // --- Swimmer CRUD ---

    fun addSwimmer(
        name: String,
        stroke: String,
        groupName: String? = null,
        pb25m: Long? = null,
        pb50m: Long? = null,
        pb100m: Long? = null,
        pb100mLagen: Long? = null,
        pb200mLagen: Long? = null,
        pb400mLagen: Long? = null,
        flySplit: Long? = null,
        backSplit: Long? = null,
        breastSplit: Long? = null,
        freeSplit: Long? = null,
        pb200Fly: Long? = null,
        pb200Back: Long? = null,
        pb200Breast: Long? = null,
        pb200Free: Long? = null,
        pb400Fly: Long? = null,
        pb400Back: Long? = null,
        pb400Breast: Long? = null,
        pb400Free: Long? = null,
        customPbsMap: Map<Pair<String, Int>, Long>? = null
    ) {
        viewModelScope.launch {
            val resolvedStroke = if (stroke == "Lagen") "Freistil" else stroke
            val customJson = customPbsMap?.let { SwimmerEntity.serializeCustomPbsMap(it) }
            val swimmer = SwimmerEntity(
                name = name.trim(),
                ageGroup = "Offen",
                favoriteStroke = resolvedStroke,
                groupName = groupName?.trim()?.takeIf { it.isNotBlank() },
                pb25mMillis = pb25m ?: customPbsMap?.get(resolvedStroke to 25),
                pb50mMillis = pb50m ?: customPbsMap?.get(resolvedStroke to 50),
                pb100mMillis = pb100m ?: customPbsMap?.get(resolvedStroke to 100),
                pb100mLagenMillis = pb100mLagen ?: customPbsMap?.get("Lagen" to 100),
                pb200mLagenMillis = pb200mLagen ?: customPbsMap?.get("Lagen" to 200),
                pb400mLagenMillis = pb400mLagen ?: customPbsMap?.get("Lagen" to 400),
                lagenFlySplitMillis = flySplit,
                lagenBackSplitMillis = backSplit,
                lagenBreastSplitMillis = breastSplit,
                lagenFreeSplitMillis = freeSplit,
                pb200mLagenFlySplitMillis = pb200Fly,
                pb200mLagenBackSplitMillis = pb200Back,
                pb200mLagenBreastSplitMillis = pb200Breast,
                pb200mLagenFreeSplitMillis = pb200Free,
                pb400mLagenFlySplitMillis = pb400Fly,
                pb400mLagenBackSplitMillis = pb400Back,
                pb400mLagenBreastSplitMillis = pb400Breast,
                pb400mLagenFreeSplitMillis = pb400Free,
                customPbsJson = customJson
            )
            repository.insertSwimmer(swimmer)
        }
    }

    fun importSwimmers(swimmers: List<SwimmerEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.allSwimmers.firstOrNull() ?: emptyList()
            val existingNames = existing.asSequence().map { it.name }.toSet()
            
            var addedCount = 0
            swimmers.forEach { swimmer ->
                if (!existingNames.contains(swimmer.name)) {
                    repository.insertSwimmer(swimmer.copy(id = 0))
                    addedCount++
                }
            }
        }
    }

    fun importSessionsFromJson(jsonString: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = try {
                val importedSessions = ExportHelper.importSessionsFromJson(jsonString)
                if (importedSessions.isNotEmpty()) {
                    repository.importSessionsWithLaps(importedSessions)
                }
                importedSessions.size
            } catch (_: Exception) {
                0
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete(count)
            }
        }
    }

    fun importSessionsFromUri(contentResolver: android.content.ContentResolver, uri: android.net.Uri, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = try {
                val jsonString = contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                val importedSessions = ExportHelper.importSessionsFromJson(jsonString)
                if (importedSessions.isNotEmpty()) {
                    repository.importSessionsWithLaps(importedSessions)
                }
                importedSessions.size
            } catch (_: Exception) {
                0
            }
            if (onComplete != null) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(count)
                }
            }
        }
    }

    fun importSwimmersFromUri(contentResolver: android.content.ContentResolver, uri: android.net.Uri, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = try {
                val jsonString = contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                val importedSwimmers = ExportHelper.importSwimmersFromJson(jsonString)
                if (importedSwimmers.isNotEmpty()) {
                    importSwimmers(importedSwimmers)
                }
                importedSwimmers.size
            } catch (_: Exception) {
                0
            }
            if (onComplete != null) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(count)
                }
            }
        }
    }

    fun updateSwimmer(swimmer: SwimmerEntity) {
        viewModelScope.launch {
            repository.updateSwimmer(swimmer)
            // Keep assigned lanes in sync if swimmer was renamed
            val updatedLanes = _uiState.value.lanes.map { lane ->
                if (lane.swimmerId == swimmer.id) {
                    lane.copy(swimmerName = swimmer.name)
                } else lane
            }
            _uiState.update { it.copy(lanes = updatedLanes) }
        }
    }

    fun deleteSwimmer(swimmer: SwimmerEntity) {
        viewModelScope.launch {
            repository.deleteSwimmer(swimmer)
            // Unassign deleted swimmer from any active lane
            val updatedLanes = _uiState.value.lanes.map { lane ->
                if (lane.swimmerId == swimmer.id) {
                    lane.copy(swimmerId = null, swimmerName = "Bahn ${lane.laneNumber}")
                } else lane
            }
            _uiState.update { it.copy(lanes = updatedLanes) }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            val sessionWithLaps = repository.getSessionWithLapsSync(sessionId)
            val affectedSwimmerIds = sessionWithLaps?.laps?.asSequence()?.mapNotNull { it.swimmerId }?.distinct()?.toList() ?: emptyList()

            repository.deleteSessionById(sessionId)

            affectedSwimmerIds.forEach { swimmerId ->
                recalculateSwimmerPbs(swimmerId)
            }
        }
    }

    suspend fun recalculateSwimmerPbs(swimmerId: Long) {
        val swimmer = repository.getSwimmerById(swimmerId) ?: return
        val remainingSessions = repository.allSessionsWithLapsSync().filter { session ->
            session.laps.any { it.swimmerId == swimmerId }
        }

        var best25: Long? = null
        var best50: Long? = null
        var best100: Long? = null
        var best100Lagen: Long? = null
        var best200Lagen: Long? = null
        var best400Lagen: Long? = null

        var best100LFly: Long? = null
        var best100LBack: Long? = null
        var best100LBreast: Long? = null
        var best100LFree: Long? = null

        var best200LFly: Long? = null
        var best200LBack: Long? = null
        var best200LBreast: Long? = null
        var best200LFree: Long? = null

        var best400LFly: Long? = null
        var best400LBack: Long? = null
        var best400LBreast: Long? = null
        var best400LFree: Long? = null

        val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")
        val customMap = swimmer.parseCustomPbsMap().toMutableMap()

        remainingSessions.forEach { sessionWithLaps ->
            val s = sessionWithLaps.session
            val swimmerLaps = sessionWithLaps.laps
                .filter { it.swimmerId == swimmerId }
                .sortedBy { it.lapIndex }

            if (swimmerLaps.isEmpty()) return@forEach

            val totalSplitsNeeded = (s.targetDistanceMeters / s.splitDistanceMeters).coerceAtLeast(1)
            val hasTargetLap = swimmerLaps.any { it.lapIndex == totalSplitsNeeded }
            val completedFullDistance = hasTargetLap || swimmerLaps.size >= totalSplitsNeeded || swimmerLaps.isNotEmpty()
            val finishTime = if (completedFullDistance) swimmerLaps.find { it.lapIndex == totalSplitsNeeded }?.splitTimeMillis ?: swimmerLaps.lastOrNull()?.splitTimeMillis else null

            if (finishTime != null && finishTime > 0) {
                val matchedStroke = strokeOptions.find { isSameStroke(it, s.strokeType) } ?: s.strokeType
                val curPb = customMap[matchedStroke to s.targetDistanceMeters]
                if (curPb == null || finishTime < curPb) {
                    customMap[matchedStroke to s.targetDistanceMeters] = finishTime
                }
            }

            fun getCumulativeSplitAt(meters: Int): Long? {
                if (meters == s.targetDistanceMeters && finishTime != null && finishTime > 0) return finishTime
                if (meters % s.splitDistanceMeters != 0) return null
                val targetIdx = meters / s.splitDistanceMeters
                val lap = swimmerLaps.find { it.lapIndex == targetIdx }
                return lap?.splitTimeMillis?.takeIf { it > 0L }
            }

            // Non-Lagen:
            if (s.strokeType != "Lagen" && isSameStroke(s.strokeType, swimmer.favoriteStroke)) {
                getCumulativeSplitAt(25)?.let { t -> best25.let { b -> if (b == null || t < b) best25 = t } }
                getCumulativeSplitAt(50)?.let { t -> best50.let { b -> if (b == null || t < b) best50 = t } }
                getCumulativeSplitAt(100)?.let { t -> best100.let { b -> if (b == null || t < b) best100 = t } }
            }

            // Lagen:
            if (s.strokeType == "Lagen" && finishTime != null && finishTime > 0) {
                when (s.targetDistanceMeters) {
                    100 -> best100Lagen.let { b -> if (b == null || finishTime < b) best100Lagen = finishTime }
                    200 -> best200Lagen.let { b -> if (b == null || finishTime < b) best200Lagen = finishTime }
                    400 -> best400Lagen.let { b -> if (b == null || finishTime < b) best400Lagen = finishTime }
                }

                val legDist = (s.targetDistanceMeters / 4).coerceAtLeast(1)
                if (legDist % s.splitDistanceMeters == 0) {
                    val splitsPerLeg = (legDist / s.splitDistanceMeters).coerceAtLeast(1)
                    fun getLegTime(legIdx: Int): Long? {
                        val start = legIdx * splitsPerLeg
                        val end = start + splitsPerLeg
                        if (swimmerLaps.size >= end) {
                            var sum = 0L
                            for (i in start until end) sum += swimmerLaps[i].lapTimeMillis
                            return if (sum > 0) sum else null
                        }
                        return null
                    }

                    val fly = getLegTime(0)
                    val back = getLegTime(1)
                    val breast = getLegTime(2)
                    val free = getLegTime(3)

                    when (s.targetDistanceMeters) {
                        100 -> {
                            fly?.let { if (best100LFly == null || it < best100LFly!!) best100LFly = it }
                            back?.let { if (best100LBack == null || it < best100LBack!!) best100LBack = it }
                            breast?.let { if (best100LBreast == null || it < best100LBreast!!) best100LBreast = it }
                            free?.let { if (best100LFree == null || it < best100LFree!!) best100LFree = it }
                        }
                        200 -> {
                            fly?.let { if (best200LFly == null || it < best200LFly!!) best200LFly = it }
                            back?.let { if (best200LBack == null || it < best200LBack!!) best200LBack = it }
                            breast?.let { if (best200LBreast == null || it < best200LBreast!!) best200LBreast = it }
                            free?.let { if (best200LFree == null || it < best200LFree!!) best200LFree = it }
                        }
                        400 -> {
                            fly?.let { if (best400LFly == null || it < best400LFly!!) best400LFly = it }
                            back?.let { if (best400LBack == null || it < best400LBack!!) best400LBack = it }
                            breast?.let { if (best400LBreast == null || it < best400LBreast!!) best400LBreast = it }
                            free?.let { if (best400LFree == null || it < best400LFree!!) best400LFree = it }
                        }
                    }
                }
            }
        }

        repository.updateSwimmer(
            swimmer.copy(
                pb25mMillis = best25,
                pb50mMillis = best50,
                pb100mMillis = best100,
                pb100mLagenMillis = best100Lagen,
                pb200mLagenMillis = best200Lagen,
                pb400mLagenMillis = best400Lagen,
                lagenFlySplitMillis = best100LFly,
                lagenBackSplitMillis = best100LBack,
                lagenBreastSplitMillis = best100LBreast,
                lagenFreeSplitMillis = best100LFree,
                pb200mLagenFlySplitMillis = best200LFly,
                pb200mLagenBackSplitMillis = best200LBack,
                pb200mLagenBreastSplitMillis = best200LBreast,
                pb200mLagenFreeSplitMillis = best200LFree,
                pb400mLagenFlySplitMillis = best400LFly,
                pb400mLagenBackSplitMillis = best400LBack,
                pb400mLagenBreastSplitMillis = best400LBreast,
                pb400mLagenFreeSplitMillis = best400LFree,
                customPbsJson = SwimmerEntity.serializeCustomPbsMap(customMap)
            )
        )
    }

    companion object {
        fun isSameStroke(strokeA: String?, strokeB: String?): Boolean {
            if (strokeA == null || strokeB == null) return false
            val a = strokeA.trim().lowercase()
            val b = strokeB.trim().lowercase()
            if (a == b) return true
            val isFreeA = a == "freistil" || a == "kraul" || a == "free"
            val isFreeB = b == "freistil" || b == "kraul" || b == "free"
            if (isFreeA && isFreeB) return true
            val isFlyA = a == "schmetterling" || a == "delfin" || a == "fly"
            val isFlyB = b == "schmetterling" || b == "delfin" || b == "fly"
            if (isFlyA && isFlyB) return true
            val isBackA = a == "rücken" || a == "ruecken" || a == "back"
            val isBackB = b == "rücken" || b == "ruecken" || b == "back"
            if (isBackA && isBackB) return true
            val isBreastA = a == "brust" || a == "breast"
            val isBreastB = b == "brust" || b == "breast"
            if (isBreastA && isBreastB) return true
            val isLagenA = a == "lagen" || a == "medley" || a == "im"
            val isLagenB = b == "lagen" || b == "medley" || b == "im"
            return isLagenA && isLagenB
        }
    }
}

class StopwatchViewModelFactory(
    private val repository: SwimRepository,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StopwatchViewModel(repository, preferencesRepository) as T
    }
}