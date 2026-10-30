package com.example.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.LapRecordEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SessionWithLaps
import com.example.data.model.SwimmerEntity
import com.example.data.repository.ISwimRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.domain.PersonalBestEvaluator
import com.example.util.ExportHelper
import com.example.util.SoundPlayer
import com.example.util.DefaultSoundPlayer
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
    val maxSeriesCount: Int? = null, // null = Unbegrenzt
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
    val relayMembers: List<com.example.domain.RelayLegAssignment> = emptyList(),
) {
    val allLaps: List<LapItem>
        get() = completedSeriesLaps + laps

    val effectiveTargetDistanceMeters: Int
        get() = if (relayMembers.isNotEmpty()) {
            relayMembers.size * splitDistanceMeters
        } else {
            targetDistanceMeters
        }

    val totalSplitsNeeded: Int
        get() = if (relayMembers.isNotEmpty()) {
            relayMembers.size.coerceAtLeast(1)
        } else {
            (targetDistanceMeters / splitDistanceMeters).coerceAtLeast(1)
        }
}

sealed interface UndoAction {
    data class LapAdded(
        val laneNumber: Int,
        val previousStatus: LaneStatus,
        val previousFinishTime: Long?,
        val previousFinishRealtime: Long? = null,
        val removedLap: LapItem,
        val previousLaneState: LaneState? = null,
    ) : UndoAction
    data class LaneFinished(
        val laneNumber: Int,
        val previousStatus: LaneStatus,
        val previousFinishTime: Long? = null,
        val previousFinishRealtime: Long? = null,
        val removedFinishLap: LapItem?,
        val previousLaneState: LaneState? = null,
    ) : UndoAction
    data class BulkStart(val previousLanes: List<LaneState>, val previousMasterStart: Long?) : UndoAction
    data class SingleLaneStart(val laneNumber: Int, val previousLanes: List<LaneState>, val previousMasterStart: Long?) : UndoAction
    data class BulkStop(val previousLanes: List<LaneState>) : UndoAction
    data class LaneRemoved(val lane: LaneState, val index: Int) : UndoAction
    data class LanePauseToggled(val laneNumber: Int, val previousLaneState: LaneState) : UndoAction
}

@Immutable
data class StopwatchUiState(
    val laneCount: Int = 4, // 1 to 15
    val lanes: List<LaneState> = (1..4).map { laneNum ->
        LaneState(laneNumber = laneNum, swimmerName = "Bahn $laneNum")
    },
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
    // Assign Swimmer Dialog
    val isAssignSwimmerDialogVisible: Boolean = false,
    val assignSwimmerLane: Int? = null,
    val assignSwimmerInitialRelayLeg: Int? = null,
    // Save confirmation
    val isSessionSavedSuccess: Boolean = false,
    val saveNotificationMessage: String? = null,
    // Swim Pace & Split Calculator
    val isPaceCalculatorVisible: Boolean = false,
) {
    @Suppress("unused")
    val hasTickingLanes: Boolean
        get() = lanes.any { (it.status == LaneStatus.RUNNING) || (it.status == LaneStatus.COUNTDOWN) || (it.status == LaneStatus.REST_COUNTDOWN) }

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
    val originalEntity: SwimmerEntity,
)

class StopwatchViewModel(
    private val repository: ISwimRepository,
    private val preferencesRepository: UserPreferencesRepository? = null,
    private val clock: MonotonicClock = AndroidMonotonicClock(),
    private val soundPlayer: SoundPlayer = DefaultSoundPlayer()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    val allSwimmers: StateFlow<List<SwimmerEntity>> = repository.allSwimmers
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val swimmersSearchQuery = MutableStateFlow("")

    val selectedGroupFilter = MutableStateFlow<String?>(null) // null = Alle

    private val _customGroups = MutableStateFlow<Set<String>>(emptySet())

    val availableGroups: StateFlow<List<String>> = combine(
        allSwimmers,
        _customGroups
    ) { swimmers, custom ->
        val fromSwimmers = swimmers.asSequence().mapNotNull { it.groupName?.trim() }.filter { it.isNotBlank() }
        (fromSwimmers + custom.asSequence())
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .toList()
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
            val customMap = swimmer.parseCustomPbsMap()
            val fav = if (swimmer.favoriteStroke == "Lagen") "Freistil" else swimmer.favoriteStroke
            fun findPbForStroke(st: String, dist: Int): Long? {
                val direct = customMap[st to dist]
                if (direct != null) return direct
                val matched = customMap.entries.firstOrNull { (k, _) ->
                    k.second == dist && PersonalBestEvaluator.isSameStroke(k.first, st)
                }?.value
                return matched
            }
            val pb25Millis = listOfNotNull(swimmer.pb25mMillis, findPbForStroke(fav, 25)).minOrNull()
            val pb50Millis = listOfNotNull(swimmer.pb50mMillis, findPbForStroke(fav, 50)).minOrNull()
            val pb100Millis = listOfNotNull(swimmer.pb100mMillis, findPbForStroke(fav, 100)).minOrNull()
            val pb100ImMillis = listOfNotNull(swimmer.pb100mLagenMillis, findPbForStroke("Lagen", 100)).minOrNull()
            val pb50mText = pb50Millis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: ""
            SwimmerCardUiModel(
                id = swimmer.id,
                name = swimmer.name,
                initial = swimmer.name.take(1).uppercase(),
                groupOrClub = swimmer.favoriteStroke,
                pb25mText = pb25Millis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "",
                pb50mText = pb50mText,
                pb100mText = pb100Millis?.let { TimeFormatter.formatMillis(it) } ?: "",
                pb100mImText = pb100ImMillis?.let { TimeFormatter.formatMillis(it) } ?: "",
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
        if (preferencesRepository == null) {
            initializeLanes(4)
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadSavedPreferences()
        }
        viewModelScope.launch(Dispatchers.IO) {
            allSwimmers.collect { swimmers ->
                val dbGroups = swimmers.asSequence().mapNotNull { it.groupName?.trim() }.filter { it.isNotBlank() }.toList()
                if (dbGroups.isNotEmpty()) {
                    val currentCustom = _customGroups.value
                    val missing = dbGroups.filter { dbG -> currentCustom.none { it.equals(dbG, ignoreCase = true) } }
                    if (missing.isNotEmpty()) {
                        _customGroups.update { old ->
                            (old + missing).asSequence().filter { it.isNotBlank() }.distinctBy { it.lowercase() }.toSet()
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
                old.asSequence().filterNot { it.equals(trimmedOld, ignoreCase = true) }.toSet() + trimmedNew
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
                old.asSequence().filterNot { it.equals(trimmed, ignoreCase = true) }.toSet()
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
        require((targetMeters % splitMeters) == 0) {
            "Die Zieldistanz muss durch die Splitdistanz teilbar sein."
        }
    }

    private fun loadSavedPreferences() {
        soundPlayer.init()
        val prefsRepo = preferencesRepository ?: return
        val poolRaw = prefsRepo.getPoolLengthMeters()
        val pool = poolRaw.coerceIn(10, 100)

        val distRaw = prefsRepo.getTargetDistanceMeters()
        val dist = distRaw.coerceIn(25, 10000)

        val splitRaw = prefsRepo.getSplitDistanceMeters()
        var split = splitRaw.coerceIn(25, dist)
        if ((dist % split) != 0) {
            split = if ((dist % 50) == 0) 50 else dist
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

        val isSeriesEnabled = prefsRepo.getSeriesEnabled(default = false)
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
        val start = lane.laneStartTimeRealtime
            ?: masterStartTimeRealtime?.let { it + lane.intervalOffsetMillis }
            ?: return lane.currentElapsedMillis
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
            soundPlayer.playStartSignal()
        } else {
            lowestCountdownBeepThisTick?.let { soundPlayer.playCountdownBeep(it) }
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
        soundPlayer.playStartSignal()
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
        soundPlayer.playStartSignal()
        val now = clock.nowMillis()
        
        pushUndo(UndoAction.SingleLaneStart(laneNumber, _uiState.value.lanes, _uiState.value.masterStartTimeRealtime))

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
        soundPlayer.playStartSignal()
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
                    countdownRemainingSeconds = 0
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
                    soundPlayer.playCountdownBeep()
                }
                val delayToStart = targetTime - clock.nowMillis()
                if (delayToStart > 0) {
                    delay(delayToStart.milliseconds)
                    soundPlayer.playStartSignal()
                }
            }
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
            soundPlayer.playStartSignal()
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
        waveAudioJob?.cancel()
        waveAudioJob = null
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
                        val totalSplitsNeeded = lane.totalSplitsNeeded

                        val finishLap = if (exactFinalTime > 0L && lane.laps.size < totalSplitsNeeded) {
                            LapItem(
                                lapIndex = totalSplitsNeeded,
                                splitTimeMillis = exactFinalTime,
                                lapTimeMillis = lapTime,
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
                        val exactFinalTime = roundToHundredths(lane.currentElapsedMillis)
                        val previousSplit = lane.laps.lastOrNull()?.splitTimeMillis ?: 0L
                        val lapTime = (exactFinalTime - previousSplit).coerceAtLeast(0L)
                        val totalSplitsNeeded = lane.totalSplitsNeeded
                        val finishLap = if (exactFinalTime > 0L && lane.laps.size < totalSplitsNeeded) {
                            LapItem(
                                lapIndex = totalSplitsNeeded,
                                splitTimeMillis = exactFinalTime,
                                lapTimeMillis = lapTime,
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
        var pushedUndo: UndoAction? = null
        _uiState.update { currentState ->
            val lanes = currentState.lanes.toMutableList()
            val index = lanes.indexOfFirst { it.laneNumber == laneNumber }
            if (index == -1) return@update currentState

            val lane = lanes[index]
            if (lane.status != LaneStatus.RUNNING) return@update currentState

            val elapsed = computeExactLaneElapsed(lane, now, currentState.masterStartTimeRealtime)
            pushedUndo = UndoAction.LanePauseToggled(
                laneNumber = laneNumber,
                previousLaneState = lane.copy(currentElapsedMillis = elapsed)
            )

            lanes[index] = lane.copy(
                status = LaneStatus.PAUSED,
                currentElapsedMillis = elapsed
            )

            anyRunning = lanes.any { it.status == LaneStatus.RUNNING || it.status == LaneStatus.COUNTDOWN || it.status == LaneStatus.PAUSED || it.status == LaneStatus.REST_COUNTDOWN }
            currentState.copy(lanes = lanes)
        }
        pushedUndo?.let { pushUndo(it) }
        if (!anyRunning) stopTickerIfAllIdle()
    }

    fun onResumeLane(laneNumber: Int) {
        val now = clock.nowMillis()
        var pushedUndo: UndoAction? = null

        _uiState.update { currentState ->
            val lanes = currentState.lanes.toMutableList()
            val index = lanes.indexOfFirst { it.laneNumber == laneNumber }
            if (index == -1) return@update currentState

            val lane = lanes[index]
            if (lane.status != LaneStatus.PAUSED) return@update currentState

            pushedUndo = UndoAction.LanePauseToggled(laneNumber = laneNumber, previousLaneState = lane)

            lanes[index] = lane.copy(
                status = LaneStatus.RUNNING,
                laneStartTimeRealtime = now - lane.currentElapsedMillis,
                finishTimeMillis = null,
                finishRealtimeMillis = null
            )

            currentState.copy(lanes = lanes)
        }
        pushedUndo?.let { pushUndo(it) }
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
            val totalSplitsNeeded = lane.totalSplitsNeeded
            val isFinalLap = newLapIndex >= totalSplitsNeeded

            val newLap = LapItem(
                lapIndex = newLapIndex,
                splitTimeMillis = splitTime,
                lapTimeMillis = lapTime,
                seriesIndex = lane.seriesCount
            )

            pushedUndo = UndoAction.LapAdded(
                laneNumber = laneNumber,
                previousStatus = lane.status,
                previousFinishTime = lane.finishTimeMillis,
                previousFinishRealtime = lane.finishRealtimeMillis,
                removedLap = newLap,
                previousLaneState = lane
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
            if (lane.status != LaneStatus.RUNNING && lane.status != LaneStatus.PAUSED) return@update currentState

            val finalTime = if (lane.status == LaneStatus.PAUSED) {
                roundToHundredths(lane.currentElapsedMillis)
            } else {
                roundToHundredths(computeExactLaneElapsed(lane, exactRealtime, currentState.masterStartTimeRealtime))
            }
            val previousSplit = lane.laps.lastOrNull()?.splitTimeMillis ?: 0L
            val lapTime = (finalTime - previousSplit).coerceAtLeast(0L)
            val totalSplitsNeeded = lane.totalSplitsNeeded

            val finishLap = if (finalTime > 0L && lane.laps.size < totalSplitsNeeded) {
                LapItem(
                    lapIndex = totalSplitsNeeded,
                    splitTimeMillis = finalTime,
                    lapTimeMillis = lapTime,
                    seriesIndex = lane.seriesCount
                )
            } else null

            val updatedLaps = if (finishLap != null) lane.laps + finishLap else lane.laps

            pushedUndo = UndoAction.LaneFinished(
                laneNumber = laneNumber,
                previousStatus = lane.status,
                previousFinishTime = lane.finishTimeMillis,
                previousFinishRealtime = lane.finishRealtimeMillis,
                removedFinishLap = finishLap,
                previousLaneState = lane
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
                val totalSplitsNeeded = lane.totalSplitsNeeded
                val isFinalLap = newLapIndex >= totalSplitsNeeded

                val newLap = LapItem(
                    lapIndex = newLapIndex,
                    splitTimeMillis = splitTime,
                    lapTimeMillis = lapTime,
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
        waveAudioJob?.cancel()
        waveAudioJob = null
        undoStack.clear()

        _uiState.update { currentState ->
            val resetLanes = currentState.lanes.map { lane ->
                val resetRelayMembers = if (lane.relayMembers.isNotEmpty()) {
                    lane.relayMembers.mapIndexed { idx, leg ->
                        leg.copy(
                            swimmerId = null,
                            swimmerName = "Starter ${idx + 1}",
                            timeMillis = null,
                            isEstimated = false,
                            sourceLabel = ""
                        )
                    }
                } else {
                    emptyList()
                }

                lane.copy(
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
                    countdownRemainingSeconds = 0,
                    relayMembers = resetRelayMembers
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
                        if (action.previousLaneState != null) {
                            lanes[idx] = action.previousLaneState
                            anyRunning = lanes.any { it.status == LaneStatus.RUNNING || it.status == LaneStatus.COUNTDOWN || it.status == LaneStatus.PAUSED || it.status == LaneStatus.REST_COUNTDOWN }
                            return@update currentState.copy(
                                lanes = lanes,
                                isCurrentRunSaved = false,
                                lastUndoMessage = "Lap ${action.removedLap.lapIndex} auf Bahn ${action.laneNumber} rückgängig gemacht"
                            )
                        }
                        val currentLaps = lanes[idx].laps.toMutableList()
                        if (currentLaps.isNotEmpty()) {
                            currentLaps.removeAt(currentLaps.lastIndex)
                            lanes[idx] = lanes[idx].copy(
                                status = action.previousStatus,
                                finishTimeMillis = action.previousFinishTime,
                                finishRealtimeMillis = action.previousFinishRealtime,
                                laps = currentLaps
                            )
                            anyRunning = lanes.any { it.status == LaneStatus.RUNNING || it.status == LaneStatus.COUNTDOWN || it.status == LaneStatus.PAUSED || it.status == LaneStatus.REST_COUNTDOWN }
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
                        if (action.previousLaneState != null) {
                            lanes[idx] = action.previousLaneState
                        } else {
                            val restoredLaps = if (action.removedFinishLap != null && lanes[idx].laps.isNotEmpty()) {
                                lanes[idx].laps.dropLast(1)
                            } else lanes[idx].laps
                            lanes[idx] = lanes[idx].copy(
                                status = action.previousStatus,
                                finishTimeMillis = action.previousFinishTime,
                                finishRealtimeMillis = action.previousFinishRealtime,
                                laps = restoredLaps
                            )
                        }
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
                waveAudioJob?.cancel()
                waveAudioJob = null
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
            is UndoAction.SingleLaneStart -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        lanes = action.previousLanes,
                        masterStartTimeRealtime = action.previousMasterStart,
                        isCurrentRunSaved = false,
                        lastUndoMessage = "Einzelstart für Bahn ${action.laneNumber} rückgängig gemacht"
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
            is UndoAction.LanePauseToggled -> {
                var anyRunning = false
                _uiState.update { currentState ->
                    val lanes = currentState.lanes.toMutableList()
                    val idx = lanes.indexOfFirst { it.laneNumber == action.laneNumber }
                    if (idx != -1) {
                        val now = clock.nowMillis()
                        val restored = if (action.previousLaneState.status == LaneStatus.RUNNING) {
                            action.previousLaneState.copy(
                                laneStartTimeRealtime = now - action.previousLaneState.currentElapsedMillis
                            )
                        } else {
                            action.previousLaneState
                        }
                        lanes[idx] = restored
                        anyRunning = lanes.any { it.status == LaneStatus.RUNNING || it.status == LaneStatus.COUNTDOWN || it.status == LaneStatus.PAUSED || it.status == LaneStatus.REST_COUNTDOWN }
                        return@update currentState.copy(
                            lanes = lanes,
                            lastUndoMessage = "Pause/Weiter für Bahn ${action.laneNumber} rückgängig gemacht"
                        )
                    }
                    currentState
                }
                if (anyRunning) startTicker() else stopTickerIfAllIdle()
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
        val state = _uiState.value
        val isRelayActive = currentLanes.any { it.relayMembers.isNotEmpty() }
        val newLane = if (isRelayActive) {
            val defaultCount = currentLanes.firstOrNull { it.relayMembers.isNotEmpty() }?.relayMembers?.size ?: 4
            val relayLegs = buildDefaultRelayLegs(
                strokeType = state.strokeType,
                legDistanceMeters = state.splitDistanceMeters,
                existingLane = null,
                targetStarterCount = defaultCount
            )
            LaneState(
                laneNumber = nextLaneNumber,
                swimmerName = "Staffel Bahn $nextLaneNumber",
                targetDistanceMeters = relayLegs.size * state.splitDistanceMeters,
                splitDistanceMeters = state.splitDistanceMeters,
                strokeType = state.strokeType,
                relayMembers = relayLegs
            )
        } else {
            LaneState(
                laneNumber = nextLaneNumber,
                swimmerName = "Bahn $nextLaneNumber",
                targetDistanceMeters = state.targetDistanceMeters,
                splitDistanceMeters = state.splitDistanceMeters,
                strokeType = state.strokeType
            )
        }
        val updated = currentLanes + newLane
        _uiState.update { it.copy(
            lanes = updated,
            laneCount = updated.size,
            lastUndoMessage = "Bahn $nextLaneNumber hinzugefügt"
        ) }
        savePreferences()
    }

    private fun buildDefaultRelayLegs(
        strokeType: String,
        legDistanceMeters: Int,
        existingLane: LaneState? = null,
        targetStarterCount: Int? = null
    ): List<com.example.domain.RelayLegAssignment> {
        val swimmersById = allSwimmers.value.associateBy { it.id }
        val desiredCount = (targetStarterCount ?: existingLane?.relayMembers?.size?.takeIf { it >= 2 } ?: 4).coerceIn(2, 8)
        if (existingLane != null && existingLane.relayMembers.isNotEmpty()) {
            return (0 until desiredCount).map { idx ->
                val legStroke = if (PersonalBestEvaluator.isSameStroke(strokeType, "Lagen")) {
                    PersonalBestEvaluator.LAGEN_STAFFEL_STROKES.getOrElse(idx % 4) { "Freistil" }
                } else {
                    strokeType
                }
                val existingLeg = existingLane.relayMembers.getOrNull(idx)
                if (existingLeg != null) {
                    val swimmerEntity = existingLeg.swimmerId?.let { swimmersById[it] }
                    val est = swimmerEntity?.let {
                        com.example.domain.RelayOptimizer.resolveSwimmerTime(it, legStroke, legDistanceMeters)
                    }
                    existingLeg.copy(
                        legPosition = idx + 1,
                        stroke = legStroke,
                        distanceMeters = legDistanceMeters,
                        timeMillis = est?.millis ?: existingLeg.timeMillis,
                        isEstimated = est?.isEstimated ?: existingLeg.isEstimated,
                        sourceLabel = est?.sourceLabel ?: existingLeg.sourceLabel
                    )
                } else {
                    com.example.domain.RelayLegAssignment(
                        legPosition = idx + 1,
                        swimmerId = null,
                        swimmerName = "Starter ${idx + 1}",
                        stroke = legStroke,
                        distanceMeters = legDistanceMeters,
                        timeMillis = null
                    )
                }
            }
        }
        return (0 until desiredCount).map { idx ->
            val legStroke = if (PersonalBestEvaluator.isSameStroke(strokeType, "Lagen")) {
                PersonalBestEvaluator.LAGEN_STAFFEL_STROKES.getOrElse(idx % 4) { "Freistil" }
            } else {
                strokeType
            }
            if (idx == 0 && existingLane?.swimmerId != null) {
                val swimmerEntity = swimmersById[existingLane.swimmerId]
                val est = swimmerEntity?.let {
                    com.example.domain.RelayOptimizer.resolveSwimmerTime(it, legStroke, legDistanceMeters)
                }
                com.example.domain.RelayLegAssignment(
                    legPosition = 1,
                    swimmerId = existingLane.swimmerId,
                    swimmerName = existingLane.swimmerName,
                    stroke = legStroke,
                    distanceMeters = legDistanceMeters,
                    timeMillis = est?.millis,
                    isEstimated = est?.isEstimated ?: false,
                    sourceLabel = est?.sourceLabel ?: ""
                )
            } else {
                com.example.domain.RelayLegAssignment(
                    legPosition = idx + 1,
                    swimmerId = null,
                    swimmerName = "Starter ${idx + 1}",
                    stroke = legStroke,
                    distanceMeters = legDistanceMeters,
                    timeMillis = null
                )
            }
        }
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

    fun applyRelayTeamsToStopwatch(
        teams: List<com.example.domain.RelayTeamPlan>,
        strokeType: String,
        legDistanceMeters: Int
    ) {
        if (teams.isEmpty()) return
        onResetAll()

        val safeSplit = legDistanceMeters.takeIf { it in listOf(25, 50, 100) } ?: 50
        val distinctCounts = teams.take(15).map { it.legs.size.coerceAtLeast(1) }.distinct()
        val primaryCount = distinctCounts.firstOrNull() ?: 4
        val defaultTargetDistance = primaryCount * safeSplit
        val enforcedSplit = resolveCompatibleSplit(defaultTargetDistance, safeSplit)
        val enforcedPool = if (enforcedSplit == 25) 25 else _uiState.value.poolLengthMeters
        val relayTitle = if (distinctCounts.size == 1) {
            "${primaryCount}x${enforcedSplit}m $strokeType Staffel"
        } else {
            "${distinctCounts.min()}–${distinctCounts.max()}x${enforcedSplit}m $strokeType Staffel"
        }

        val relayLanes = teams.take(15).mapIndexed { index, team ->
            val memberCount = team.legs.size.coerceAtLeast(1)
            val laneTargetDist = memberCount * enforcedSplit
            val normalizedLegs = team.legs.mapIndexed { legIdx, leg ->
                val legStroke = if (PersonalBestEvaluator.isSameStroke(strokeType, "Lagen")) {
                    PersonalBestEvaluator.LAGEN_STAFFEL_STROKES.getOrElse(legIdx % 4) { "Freistil" }
                } else {
                    strokeType
                }
                leg.copy(
                    legPosition = legIdx + 1,
                    stroke = legStroke,
                    distanceMeters = enforcedSplit
                )
            }
            LaneState(
                laneNumber = index + 1,
                swimmerId = null,
                swimmerName = team.teamTitle,
                targetDistanceMeters = laneTargetDist,
                splitDistanceMeters = enforcedSplit,
                strokeType = strokeType,
                relayMembers = normalizedLegs
            )
        }

        _uiState.update { state ->
            state.copy(
                sessionTitle = relayTitle,
                strokeType = strokeType,
                poolLengthMeters = enforcedPool,
                targetDistanceMeters = defaultTargetDistance,
                splitDistanceMeters = enforcedSplit,
                laneCount = relayLanes.size,
                lanes = relayLanes,
                startMode = StartMode.MASS,
                seriesConfig = SeriesConfig(isEnabled = false),
                lastUndoMessage = "${relayLanes.size} Staffel-Teams übernommen ($relayTitle • ${enforcedPool}m Becken)"
            )
        }
        savePreferences()
    }

    fun configureStopwatchSettings(
        mode: StartMode,
        intervalSec: Int,
        poolLength: Int,
        distance: Int,
        splitDistance: Int,
        stroke: String,
        seriesConfig: SeriesConfig,
        isRelayMode: Boolean,
        relayStarterCount: Int? = null
    ) {
        if (isRelayMode) {
            val legDist = when {
                splitDistance in listOf(25, 50, 100) && relayStarterCount != null -> splitDistance
                distance in listOf(100, 200, 400) && relayStarterCount == null -> (distance / 4).coerceAtLeast(25)
                splitDistance in listOf(25, 50, 100) -> splitDistance
                else -> 50
            }
            val starterCount = (relayStarterCount ?: (distance / legDist).takeIf { it in 2..8 } ?: 4).coerceIn(2, 8)
            val relayDist = starterCount * legDist
            val enforcedPool = if (legDist == 25) 25 else poolLength
            val relayTitle = "${starterCount}x${legDist}m $stroke Staffel"
            val updatedLanes = _uiState.value.lanes.map { lane ->
                val relayLegs = buildDefaultRelayLegs(
                    strokeType = stroke,
                    legDistanceMeters = legDist,
                    existingLane = lane,
                    targetStarterCount = starterCount
                )
                val updatedName = if (lane.relayMembers.isNotEmpty()) {
                    lane.swimmerName
                } else {
                    "Staffel Bahn ${lane.laneNumber}"
                }
                lane.copy(
                    swimmerId = null,
                    swimmerName = updatedName,
                    targetDistanceMeters = relayLegs.size * legDist,
                    splitDistanceMeters = legDist,
                    strokeType = stroke,
                    relayMembers = relayLegs
                )
            }
            _uiState.update { state ->
                state.copy(
                    sessionTitle = relayTitle,
                    startMode = StartMode.MASS,
                    intervalOffsetSeconds = intervalSec.coerceIn(1, 60),
                    seriesConfig = SeriesConfig(isEnabled = false),
                    strokeType = stroke,
                    poolLengthMeters = enforcedPool,
                    targetDistanceMeters = relayDist,
                    splitDistanceMeters = legDist,
                    lanes = updatedLanes
                )
            }
            savePreferences()
        } else {
            val wasRelayActive = _uiState.value.lanes.any { it.relayMembers.isNotEmpty() }
            if (wasRelayActive) {
                _uiState.update { state ->
                    state.copy(
                        lanes = state.lanes.map { lane ->
                            if (lane.relayMembers.isNotEmpty()) {
                                lane.copy(
                                    swimmerId = null,
                                    swimmerName = "Bahn ${lane.laneNumber}",
                                    relayMembers = emptyList()
                                )
                            } else lane
                        }
                    )
                }
            }
            setStartMode(mode)
            setIntervalOffsetSeconds(intervalSec)
            setSeriesConfig(seriesConfig)
            setSessionMeta("${distance}m $stroke", stroke, poolLength, distance, splitDistance)
        }
    }

    @Suppress("unused")
    fun setSplitDistance(splitMeters: Int) {
        val currentDist = _uiState.value.targetDistanceMeters
        validateDistances(currentDist, splitMeters)
        val enforcedPool = if (splitMeters == 25) 25 else _uiState.value.poolLengthMeters
        val updatedLanes = _uiState.value.lanes.map {
            it.copy(splitDistanceMeters = splitMeters)
        }
        _uiState.update { it.copy(
            poolLengthMeters = enforcedPool,
            splitDistanceMeters = splitMeters,
            lanes = updatedLanes
        ) }
        savePreferences()
    }

    private fun resolveCompatibleSplit(targetMeters: Int, candidateSplit: Int): Int {
        if (candidateSplit in 1..targetMeters && (targetMeters % candidateSplit) == 0) {
            return candidateSplit
        }
        return when {
            (targetMeters % 50) == 0 -> 50
            (targetMeters % 25) == 0 -> 25
            else -> targetMeters
        }
    }

    @Suppress("unused")
    fun setTargetDistance(distance: Int) {
        val stroke = _uiState.value.strokeType
        val enforcedDistance = if (stroke == "Lagen" && distance < 100) 100 else distance
        val isRelayActive = _uiState.value.lanes.any { it.relayMembers.isNotEmpty() }
        val preferredSplit = when {
            isRelayActive && enforcedDistance in listOf(100, 200, 400) -> enforcedDistance / 4
            stroke == "Lagen" && enforcedDistance == 100 -> 25
            enforcedDistance == 25 -> 25
            else -> _uiState.value.splitDistanceMeters
        }
        val enforcedSplit = resolveCompatibleSplit(enforcedDistance, preferredSplit)
        val enforcedPool = if (enforcedDistance == 25 || enforcedSplit == 25) 25 else _uiState.value.poolLengthMeters
        validateDistances(enforcedDistance, enforcedSplit)
        val updatedLanes = _uiState.value.lanes.map { lane ->
            if (lane.relayMembers.isNotEmpty()) {
                val updatedMembers = buildDefaultRelayLegs(stroke, enforcedSplit, lane)
                lane.copy(
                    targetDistanceMeters = updatedMembers.size * enforcedSplit,
                    splitDistanceMeters = enforcedSplit,
                    relayMembers = updatedMembers
                )
            } else {
                lane.copy(targetDistanceMeters = enforcedDistance, splitDistanceMeters = enforcedSplit)
            }
        }
        _uiState.update { it.copy(
            poolLengthMeters = enforcedPool,
            targetDistanceMeters = enforcedDistance,
            splitDistanceMeters = enforcedSplit,
            lanes = updatedLanes
        ) }
        savePreferences()
    }

    @Suppress("unused")
    fun setStrokeType(stroke: String) {
        val currentDist = _uiState.value.targetDistanceMeters
        val enforcedDist = if (stroke == "Lagen") currentDist.coerceIn(100, 400) else currentDist
        val isRelayActive = _uiState.value.lanes.any { it.relayMembers.isNotEmpty() }
        val currentSplit = _uiState.value.splitDistanceMeters
        val preferredSplit = when {
            isRelayActive && enforcedDist in listOf(100, 200, 400) -> enforcedDist / 4
            stroke == "Lagen" && enforcedDist == 100 -> 25
            else -> currentSplit
        }
        val enforcedSplit = resolveCompatibleSplit(enforcedDist, preferredSplit)
        val enforcedPool = if (enforcedDist == 25 || enforcedSplit == 25) 25 else _uiState.value.poolLengthMeters
        validateDistances(enforcedDist, enforcedSplit)
        val updatedLanes = _uiState.value.lanes.map { lane ->
            if (lane.relayMembers.isNotEmpty()) {
                val updatedMembers = buildDefaultRelayLegs(stroke, enforcedSplit, lane)
                lane.copy(
                    strokeType = stroke,
                    targetDistanceMeters = updatedMembers.size * enforcedSplit,
                    splitDistanceMeters = enforcedSplit,
                    relayMembers = updatedMembers
                )
            } else {
                lane.copy(strokeType = stroke, targetDistanceMeters = enforcedDist, splitDistanceMeters = enforcedSplit)
            }
        }
        _uiState.update { it.copy(
            strokeType = stroke,
            poolLengthMeters = enforcedPool,
            targetDistanceMeters = enforcedDist,
            splitDistanceMeters = enforcedSplit,
            lanes = updatedLanes
        ) }
        savePreferences()
    }

    fun setSessionMeta(title: String, stroke: String, poolLength: Int, distance: Int, splitDistance: Int = 50) {
        val enforcedDistance = if (stroke == "Lagen" && distance < 100) 100 else distance
        val isRelayActive = _uiState.value.lanes.any { it.relayMembers.isNotEmpty() }
        val preferredSplit = when {
            isRelayActive && enforcedDistance in listOf(100, 200, 400) -> enforcedDistance / 4
            stroke == "Lagen" && enforcedDistance == 100 -> 25
            enforcedDistance == 25 -> 25
            else -> splitDistance
        }
        val enforcedSplit = resolveCompatibleSplit(enforcedDistance, preferredSplit)
        val enforcedPool = if (enforcedDistance == 25 || enforcedSplit == 25) 25 else poolLength
        validateDistances(enforcedDistance, enforcedSplit)

        val updatedLanes = _uiState.value.lanes.map { lane ->
            if (lane.relayMembers.isNotEmpty()) {
                val updatedMembers = buildDefaultRelayLegs(stroke, enforcedSplit, lane)
                lane.copy(
                    targetDistanceMeters = updatedMembers.size * enforcedSplit,
                    splitDistanceMeters = enforcedSplit,
                    strokeType = stroke,
                    relayMembers = updatedMembers
                )
            } else {
                lane.copy(
                    targetDistanceMeters = enforcedDistance,
                    splitDistanceMeters = enforcedSplit,
                    strokeType = stroke
                )
            }
        }
        val cleanTitle = if (isRelayActive) {
            val counts = updatedLanes.filter { it.relayMembers.isNotEmpty() }.map { it.relayMembers.size }.distinct()
            val countPrefix = if (counts.size == 1) "${counts.first()}x" else if (counts.size > 1) "${counts.min()}–${counts.max()}x" else "4x"
            "${countPrefix}${enforcedSplit}m $stroke Staffel"
        } else {
            title.ifBlank { "${enforcedDistance}m $stroke" }
        }
        _uiState.update { it.copy(
            sessionTitle = cleanTitle,
            strokeType = stroke,
            poolLengthMeters = enforcedPool,
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

    fun openAssignSwimmerDialog(laneNumber: Int, initialRelayLeg: Int? = null) {
        _uiState.update { it.copy(
            isAssignSwimmerDialogVisible = true,
            assignSwimmerLane = laneNumber,
            assignSwimmerInitialRelayLeg = initialRelayLeg
        ) }
    }

    fun closeAssignSwimmerDialog() {
        _uiState.update { it.copy(
            isAssignSwimmerDialogVisible = false,
            assignSwimmerInitialRelayLeg = null
        ) }
    }

    fun assignSwimmerToLane(laneNumber: Int, swimmer: SwimmerEntity?) {
        _uiState.update { currentState ->
            val updatedLanes = currentState.lanes.map { lane ->
                if (lane.laneNumber == laneNumber) {
                    lane.copy(
                        swimmerId = swimmer?.id,
                        swimmerName = swimmer?.name ?: "Bahn $laneNumber",
                        targetDistanceMeters = currentState.targetDistanceMeters,
                        splitDistanceMeters = currentState.splitDistanceMeters,
                        relayMembers = emptyList()
                    )
                } else if (swimmer != null && lane.swimmerId == swimmer.id) {
                    // Unassign swimmer from previous lane if already assigned elsewhere
                    lane.copy(
                        swimmerId = null,
                        swimmerName = "Bahn ${lane.laneNumber}",
                        relayMembers = emptyList()
                    )
                } else {
                    lane
                }
            }
            currentState.copy(
                lanes = updatedLanes,
                isAssignSwimmerDialogVisible = false,
                assignSwimmerInitialRelayLeg = null
            )
        }
    }

    fun assignRelayTeamToLane(
        laneNumber: Int,
        teamName: String,
        members: List<com.example.domain.RelayLegAssignment>
    ) {
        val swimmersById = allSwimmers.value.associateBy { it.id }
        _uiState.update { currentState ->
            val stroke = currentState.strokeType
            val legDist = currentState.splitDistanceMeters.takeIf { it in listOf(25, 50, 100) }
                ?: ((currentState.targetDistanceMeters / 4).takeIf { it in listOf(25, 50, 100) } ?: 50)
            val enforcedPool = if (legDist == 25) 25 else currentState.poolLengthMeters
            val normalizedMembers = members.mapIndexed { idx, leg ->
                val legStroke = if (PersonalBestEvaluator.isSameStroke(stroke, "Lagen")) {
                    PersonalBestEvaluator.LAGEN_STAFFEL_STROKES.getOrElse(idx % 4) { "Freistil" }
                } else {
                    stroke
                }
                val swimmerEntity = leg.swimmerId?.let { swimmersById[it] }
                val est = swimmerEntity?.let {
                    com.example.domain.RelayOptimizer.resolveSwimmerTime(it, legStroke, legDist)
                }
                leg.copy(
                    legPosition = idx + 1,
                    swimmerName = swimmerEntity?.name ?: leg.swimmerName.ifBlank { "Starter ${idx + 1}" },
                    stroke = legStroke,
                    distanceMeters = legDist,
                    timeMillis = est?.millis,
                    isEstimated = est?.isEstimated ?: false,
                    sourceLabel = est?.sourceLabel ?: ""
                )
            }
            val assignedIdsInThisRelay = normalizedMembers.mapNotNull { it.swimmerId }.toSet()
            val cleanTeamName = teamName.trim().ifBlank { "Staffel Bahn $laneNumber" }
            val laneRelayDist = normalizedMembers.size.coerceAtLeast(1) * legDist

            val updatedLanes = currentState.lanes.map { lane ->
                if (lane.laneNumber == laneNumber) {
                    lane.copy(
                        swimmerId = null,
                        swimmerName = cleanTeamName,
                        targetDistanceMeters = laneRelayDist,
                        splitDistanceMeters = legDist,
                        strokeType = stroke,
                        relayMembers = normalizedMembers
                    )
                } else {
                    // Remove any duplicate swimmer from other lanes
                    val cleanedSingleId = if (lane.swimmerId != null && lane.swimmerId in assignedIdsInThisRelay) null else lane.swimmerId
                    val cleanedSingleName = if (lane.swimmerId != null && lane.swimmerId in assignedIdsInThisRelay) "Bahn ${lane.laneNumber}" else lane.swimmerName
                    val cleanedRelayMembers = if (lane.relayMembers.isNotEmpty()) {
                        lane.relayMembers.map { m ->
                            if (m.swimmerId != null && m.swimmerId in assignedIdsInThisRelay) {
                                m.copy(swimmerId = null, swimmerName = "Starter ${m.legPosition}", timeMillis = null)
                            } else {
                                m.copy(distanceMeters = legDist)
                            }
                        }
                    } else emptyList()
                    lane.copy(
                        swimmerId = cleanedSingleId,
                        swimmerName = cleanedSingleName,
                        targetDistanceMeters = if (cleanedRelayMembers.isNotEmpty()) cleanedRelayMembers.size * legDist else lane.targetDistanceMeters,
                        splitDistanceMeters = legDist,
                        relayMembers = cleanedRelayMembers
                    )
                }
            }

            val relayCounts = updatedLanes.filter { it.relayMembers.isNotEmpty() }.map { it.relayMembers.size }.distinct()
            val primaryCount = relayCounts.firstOrNull() ?: normalizedMembers.size.coerceAtLeast(1)
            val countPrefix = if (relayCounts.size <= 1) "${primaryCount}x" else "${relayCounts.min()}–${relayCounts.max()}x"

            currentState.copy(
                sessionTitle = "${countPrefix}${legDist}m $stroke Staffel",
                poolLengthMeters = enforcedPool,
                targetDistanceMeters = primaryCount * legDist,
                splitDistanceMeters = legDist,
                seriesConfig = SeriesConfig(isEnabled = false),
                lanes = updatedLanes,
                isAssignSwimmerDialogVisible = false,
                assignSwimmerInitialRelayLeg = null
            )
        }
        savePreferences()
    }

    // --- Save Session to Room Database & Automatic PB Update ---

    fun saveCurrentSession() {
        if (_uiState.value.isCurrentRunSaved) return
        val activeLanesWithLaps = _uiState.value.lanes.filter { it.allLaps.isNotEmpty() }
        if (activeLanesWithLaps.isEmpty()) return

        _uiState.update { it.copy(isCurrentRunSaved = true) } // Set early to prevent double tap

        viewModelScope.launch {
            val state = _uiState.value

            val isRelaySession = activeLanesWithLaps.any { it.relayMembers.isNotEmpty() }
            val relayCounts = activeLanesWithLaps.filter { it.relayMembers.isNotEmpty() }.map { it.relayMembers.size }.distinct()
            val relayPrefix = when {
                relayCounts.size == 1 -> "${relayCounts.first()}x"
                relayCounts.size > 1 -> "${relayCounts.min()}–${relayCounts.max()}x"
                else -> "4x"
            }
            val defaultTitle = if (isRelaySession) {
                "${relayPrefix}${state.splitDistanceMeters}m ${state.strokeType} Staffel"
            } else {
                "${state.targetDistanceMeters}m ${state.strokeType}"
            }
            val sessionTargetDist = if (isRelaySession) {
                activeLanesWithLaps.maxOfOrNull { it.effectiveTargetDistanceMeters } ?: state.targetDistanceMeters
            } else {
                state.targetDistanceMeters
            }
            val session = SessionEntity(
                title = if (state.sessionTitle.isBlank() || state.sessionTitle == "Trainingseinheit" || state.sessionTitle == "Training") {
                    defaultTitle
                } else if (isRelaySession && !state.sessionTitle.contains("Staffel", ignoreCase = true)) {
                    defaultTitle
                } else {
                    state.sessionTitle
                },
                poolLengthMeters = state.poolLengthMeters,
                targetDistanceMeters = sessionTargetDist,
                strokeType = state.strokeType,
                startMode = state.startMode.name,
                intervalOffsetSeconds = state.intervalOffsetSeconds,
                laneCount = activeLanesWithLaps.size,
                splitDistanceMeters = state.splitDistanceMeters,
                notes = "Erfasst mit SwimTimer am Beckenrand"
            )

            val lapEntities = mutableListOf<LapRecordEntity>()
            activeLanesWithLaps.forEach { lane ->
                val laneExpectedSplits = lane.totalSplitsNeeded
                if (lane.relayMembers.isNotEmpty()) {
                    val legDist = (lane.effectiveTargetDistanceMeters / lane.relayMembers.size.coerceAtLeast(1)).coerceAtLeast(lane.splitDistanceMeters)
                    val splitsPerLeg = (legDist / lane.splitDistanceMeters).coerceAtLeast(1)
                    lane.allLaps.forEach { lap ->
                        val memberIdx = ((lap.lapIndex - 1).coerceAtLeast(0) / splitsPerLeg).coerceIn(0, lane.relayMembers.lastIndex)
                        val member = lane.relayMembers[memberIdx]
                        val encodedLapIndex = ((lap.seriesIndex - 1).coerceAtLeast(0) * laneExpectedSplits) + lap.lapIndex
                        lapEntities.add(
                            LapRecordEntity(
                                sessionId = 0,
                                swimmerId = member.swimmerId,
                                swimmerName = "${lane.swimmerName} • ${member.legPosition}. ${member.swimmerName}",
                                laneNumber = lane.laneNumber,
                                lapIndex = encodedLapIndex,
                                splitTimeMillis = lap.splitTimeMillis,
                                lapTimeMillis = lap.lapTimeMillis,
                                targetLapMillis = null,
                                deltaTargetMillis = null,
                                strokeRateSpm = lap.strokeRateSpm
                            )
                        )
                    }
                } else {
                    lane.allLaps.forEach { lap ->
                        val encodedLapIndex = ((lap.seriesIndex - 1).coerceAtLeast(0) * laneExpectedSplits) + lap.lapIndex
                        lapEntities.add(
                            LapRecordEntity(
                                sessionId = 0, // Assigned by repository
                                swimmerId = lane.swimmerId,
                                swimmerName = lane.swimmerName,
                                laneNumber = lane.laneNumber,
                                lapIndex = encodedLapIndex,
                                splitTimeMillis = lap.splitTimeMillis,
                                lapTimeMillis = lap.lapTimeMillis,
                                targetLapMillis = null,
                                deltaTargetMillis = null,
                                strokeRateSpm = lap.strokeRateSpm
                            )
                        )
                    }
                }
            }

            repository.saveSessionWithLaps(session, lapEntities)

            // Automatic PB update check for assigned athletes (both single lanes and relay legs!)
            val pbAchievements = mutableListOf<String>()

            activeLanesWithLaps.forEach { lane ->
                if (lane.relayMembers.isNotEmpty()) {
                    val distinctSwimmerIds = lane.relayMembers.mapNotNull { it.swimmerId }.distinct()
                    for (sId in distinctSwimmerIds) {
                        val swimmer = repository.getSwimmerById(sId) ?: continue
                        val evalResult = PersonalBestEvaluator.evaluate(
                            swimmer = swimmer,
                            lane = lane,
                            targetDistanceMeters = lane.effectiveTargetDistanceMeters,
                            splitDistanceMeters = lane.splitDistanceMeters,
                            strokeType = state.strokeType
                        )
                        if (evalResult.hasImproved) {
                            repository.updateSwimmer(evalResult.updatedSwimmer)
                            pbAchievements.addAll(evalResult.achievements)
                        }
                    }
                } else {
                    val sId = lane.swimmerId
                    if (sId != null) {
                        val swimmer = repository.getSwimmerById(sId)
                        if (swimmer != null) {
                            val evalResult = PersonalBestEvaluator.evaluate(
                                swimmer = swimmer,
                                lane = lane,
                                targetDistanceMeters = state.targetDistanceMeters,
                                splitDistanceMeters = state.splitDistanceMeters,
                                strokeType = state.strokeType
                            )
                            if (evalResult.hasImproved) {
                                repository.updateSwimmer(evalResult.updatedSwimmer)
                                pbAchievements.addAll(evalResult.achievements)
                            }
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

    @Suppress("unused")
    fun importSwimmers(swimmers: List<SwimmerEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            importSwimmersInternal(swimmers)
        }
    }

    private suspend fun importSwimmersInternal(swimmers: List<SwimmerEntity>): Int {
        val existing = repository.allSwimmers.firstOrNull() ?: emptyList()
        val existingNamesLower = existing.asSequence().map { it.name.trim().lowercase() }.toMutableSet()
        var insertedCount = 0

        swimmers.forEach { swimmer ->
            val cleanKey = swimmer.name.trim().lowercase()
            if (cleanKey.isNotBlank() && existingNamesLower.add(cleanKey)) {
                repository.insertSwimmer(swimmer.copy(id = 0, name = swimmer.name.trim()))
                insertedCount++
            }
        }
        return insertedCount
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
                    val affectedSwimmerIds = importedSessions.asSequence()
                        .flatMap { it.laps.asSequence() }
                        .mapNotNull { it.swimmerId }
                        .distinct()
                        .toList()
                    affectedSwimmerIds.forEach { swimmerId ->
                        recalculateSwimmerPbs(swimmerId)
                    }
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
                    importSwimmersInternal(importedSwimmers)
                } else {
                    0
                }
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
            // Keep assigned lanes and relay legs in sync if swimmer was renamed
            val updatedLanes = _uiState.value.lanes.map { lane ->
                val updatedMembers = if (lane.relayMembers.any { it.swimmerId == swimmer.id }) {
                    lane.relayMembers.map { leg ->
                        if (leg.swimmerId == swimmer.id) leg.copy(swimmerName = swimmer.name) else leg
                    }
                } else lane.relayMembers
                if (lane.swimmerId == swimmer.id) {
                    lane.copy(swimmerName = swimmer.name, relayMembers = updatedMembers)
                } else {
                    lane.copy(relayMembers = updatedMembers)
                }
            }
            _uiState.update { it.copy(lanes = updatedLanes) }
        }
    }

    fun deleteSwimmer(swimmer: SwimmerEntity) {
        viewModelScope.launch {
            repository.deleteSwimmer(swimmer)
            // Unassign deleted swimmer from any active lane or relay leg
            val updatedLanes = _uiState.value.lanes.map { lane ->
                val updatedMembers = if (lane.relayMembers.any { it.swimmerId == swimmer.id }) {
                    lane.relayMembers.map { leg ->
                        if (leg.swimmerId == swimmer.id) leg.copy(swimmerId = null) else leg
                    }
                } else lane.relayMembers
                if (lane.swimmerId == swimmer.id) {
                    lane.copy(swimmerId = null, swimmerName = "Bahn ${lane.laneNumber}", relayMembers = updatedMembers)
                } else {
                    lane.copy(relayMembers = updatedMembers)
                }
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
                recalculateSwimmerPbs(swimmerId, deletedSession = sessionWithLaps)
            }
        }
    }

    private fun evaluateSessionOnSwimmer(
        baseSwimmer: SwimmerEntity,
        sessionWithLaps: SessionWithLaps,
        swimmerId: Long
    ): SwimmerEntity {
        return PersonalBestEvaluator.evaluateSavedSessionForSwimmer(
            baseSwimmer = baseSwimmer,
            sessionWithLaps = sessionWithLaps,
            swimmerId = swimmerId
        )
    }

    suspend fun recalculateSwimmerPbs(swimmerId: Long, deletedSession: SessionWithLaps? = null) {
        val swimmer = repository.getSwimmerById(swimmerId) ?: return
        val remainingSessions = repository.allSessionsWithLapsSync().filter { session ->
            session.laps.any { it.swimmerId == swimmerId }
        }

        val blankSwimmer = swimmer.copy(
            pb25mMillis = null,
            pb50mMillis = null,
            pb100mMillis = null,
            pb100mLagenMillis = null,
            pb200mLagenMillis = null,
            pb400mLagenMillis = null,
            lagenFlySplitMillis = null,
            lagenBackSplitMillis = null,
            lagenBreastSplitMillis = null,
            lagenFreeSplitMillis = null,
            pb200mLagenFlySplitMillis = null,
            pb200mLagenBackSplitMillis = null,
            pb200mLagenBreastSplitMillis = null,
            pb200mLagenFreeSplitMillis = null,
            pb400mLagenFlySplitMillis = null,
            pb400mLagenBackSplitMillis = null,
            pb400mLagenBreastSplitMillis = null,
            pb400mLagenFreeSplitMillis = null,
            customPbsJson = null
        )

        var recalculated = if (deletedSession != null) {
            val deletedEval = evaluateSessionOnSwimmer(blankSwimmer, deletedSession, swimmerId)
            val deletedCustomMap = deletedEval.parseCustomPbsMap()
            val preservedCustomMap = swimmer.parseCustomPbsMap().filterNot { (k, v) ->
                deletedCustomMap[k] == v
            }
            fun keepIfNotFromDeleted(currentVal: Long?, deletedVal: Long?): Long? =
                if (currentVal != null && currentVal == deletedVal) null else currentVal

            swimmer.copy(
                pb25mMillis = keepIfNotFromDeleted(swimmer.pb25mMillis, deletedEval.pb25mMillis),
                pb50mMillis = keepIfNotFromDeleted(swimmer.pb50mMillis, deletedEval.pb50mMillis),
                pb100mMillis = keepIfNotFromDeleted(swimmer.pb100mMillis, deletedEval.pb100mMillis),
                pb100mLagenMillis = keepIfNotFromDeleted(swimmer.pb100mLagenMillis, deletedEval.pb100mLagenMillis),
                pb200mLagenMillis = keepIfNotFromDeleted(swimmer.pb200mLagenMillis, deletedEval.pb200mLagenMillis),
                pb400mLagenMillis = keepIfNotFromDeleted(swimmer.pb400mLagenMillis, deletedEval.pb400mLagenMillis),
                lagenFlySplitMillis = keepIfNotFromDeleted(swimmer.lagenFlySplitMillis, deletedEval.lagenFlySplitMillis),
                lagenBackSplitMillis = keepIfNotFromDeleted(swimmer.lagenBackSplitMillis, deletedEval.lagenBackSplitMillis),
                lagenBreastSplitMillis = keepIfNotFromDeleted(swimmer.lagenBreastSplitMillis, deletedEval.lagenBreastSplitMillis),
                lagenFreeSplitMillis = keepIfNotFromDeleted(swimmer.lagenFreeSplitMillis, deletedEval.lagenFreeSplitMillis),
                pb200mLagenFlySplitMillis = keepIfNotFromDeleted(swimmer.pb200mLagenFlySplitMillis, deletedEval.pb200mLagenFlySplitMillis),
                pb200mLagenBackSplitMillis = keepIfNotFromDeleted(swimmer.pb200mLagenBackSplitMillis, deletedEval.pb200mLagenBackSplitMillis),
                pb200mLagenBreastSplitMillis = keepIfNotFromDeleted(swimmer.pb200mLagenBreastSplitMillis, deletedEval.pb200mLagenBreastSplitMillis),
                pb200mLagenFreeSplitMillis = keepIfNotFromDeleted(swimmer.pb200mLagenFreeSplitMillis, deletedEval.pb200mLagenFreeSplitMillis),
                pb400mLagenFlySplitMillis = keepIfNotFromDeleted(swimmer.pb400mLagenFlySplitMillis, deletedEval.pb400mLagenFlySplitMillis),
                pb400mLagenBackSplitMillis = keepIfNotFromDeleted(swimmer.pb400mLagenBackSplitMillis, deletedEval.pb400mLagenBackSplitMillis),
                pb400mLagenBreastSplitMillis = keepIfNotFromDeleted(swimmer.pb400mLagenBreastSplitMillis, deletedEval.pb400mLagenBreastSplitMillis),
                pb400mLagenFreeSplitMillis = keepIfNotFromDeleted(swimmer.pb400mLagenFreeSplitMillis, deletedEval.pb400mLagenFreeSplitMillis),
                customPbsJson = SwimmerEntity.serializeCustomPbsMap(preservedCustomMap)
            )
        } else {
            swimmer
        }

        remainingSessions.forEach { sessionWithLaps ->
            recalculated = evaluateSessionOnSwimmer(recalculated, sessionWithLaps, swimmerId)
        }

        repository.updateSwimmer(recalculated)
    }

    companion object {
        fun isSameStroke(strokeA: String?, strokeB: String?): Boolean =
            PersonalBestEvaluator.isSameStroke(strokeA, strokeB)
    }
}

class StopwatchViewModelFactory(
    private val repository: ISwimRepository,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StopwatchViewModel(repository, preferencesRepository) as T
    }
}