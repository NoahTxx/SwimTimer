package com.example.ui

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.LapRecordEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SessionWithLaps
import com.example.data.model.SwimmerEntity
import com.example.data.repository.SwimRepository
import com.example.util.HapticUtil
import com.example.util.SoundUtil
import com.example.util.TimeFormatter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.LinkedList

enum class LaneStatus {
    IDLE,
    COUNTDOWN,
    RUNNING,
    FINISHED
}

enum class StartMode {
    MASS,       // Alle starten zeitgleich
    INTERVAL    // Staffelung (z. B. alle 5s, 10s)
}

data class LapItem(
    val lapIndex: Int,
    val splitTimeMillis: Long,
    val lapTimeMillis: Long,
    val targetLapMillis: Long? = null,
    val deltaTargetMillis: Long? = null,
    val strokeRateSpm: Double? = null
)

data class LaneState(
    val laneNumber: Int,
    val swimmerId: Long? = null,
    val swimmerName: String = "Bahn $laneNumber",
    val status: LaneStatus = LaneStatus.IDLE,
    val laneStartTimeRealtime: Long? = null,
    val finishTimeMillis: Long? = null,
    val currentElapsedMillis: Long = 0L,
    val laps: List<LapItem> = emptyList(),
    val targetLapMillis: Long? = null,
    val intervalOffsetMillis: Long = 0L,
    val countdownRemainingSeconds: Int = 0,
    val targetDistanceMeters: Int = 100,
    val splitDistanceMeters: Int = 50,
    val strokeType: String = "Freistil"
)

sealed interface UndoAction {
    data class LapAdded(val laneNumber: Int, val removedLap: LapItem) : UndoAction
    data class LaneFinished(val laneNumber: Int, val previousStatus: LaneStatus, val removedFinishLap: LapItem?) : UndoAction
    data class BulkStart(val previousLanes: List<LaneState>, val previousMasterStart: Long?) : UndoAction
    data class BulkStop(val previousLanes: List<LaneState>) : UndoAction
    data class LaneRemoved(val lane: LaneState, val index: Int) : UndoAction
}

data class StopwatchUiState(
    val laneCount: Int = 4, // 1 to 15
    val lanes: List<LaneState> = emptyList(),
    val isAnyRunning: Boolean = false,
    val masterStartTimeRealtime: Long? = null,
    val startMode: StartMode = StartMode.MASS,
    val intervalOffsetSeconds: Int = 5,
    val isWaterLockActive: Boolean = false,
    val selectedFocusedLane: Int = 1,
    val sessionTitle: String = "Trainingseinheit",
    val poolLengthMeters: Int = 50,
    val targetDistanceMeters: Int = 100,
    val splitDistanceMeters: Int = 50,
    val strokeType: String = "Freistil",
    val canUndo: Boolean = false,
    val lastUndoMessage: String? = null,
    // Stroke Rate Calculator
    val isStrokeRateDialogVisible: Boolean = false,
    val strokeRateTargetLane: Int? = null,
    // Target Pace Dialog
    val isTargetPaceDialogVisible: Boolean = false,
    val targetPaceLane: Int? = null,
    // Assign Swimmer Dialog
    val isAssignSwimmerDialogVisible: Boolean = false,
    val assignSwimmerLane: Int? = null,
    // Save confirmation
    val isSessionSavedSuccess: Boolean = false,
    val saveNotificationMessage: String? = null,
    // Swim Pace & Split Calculator
    val isPaceCalculatorVisible: Boolean = false
)

class StopwatchViewModel(
    private val repository: SwimRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    val allSwimmers: StateFlow<List<SwimmerEntity>> = repository.allSwimmers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<SessionWithLaps>> = repository.allSessionsWithLaps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val undoStack = LinkedList<UndoAction>()
    private var tickerJob: Job? = null

    init {
        initializeLanes(4)
    }

    fun initializeLanes(count: Int) {
        val clamped = count.coerceIn(1, 15)
        val currentDist = _uiState.value.targetDistanceMeters
        val currentSplit = _uiState.value.splitDistanceMeters
        val currentStroke = _uiState.value.strokeType
        val initialLanes = (1..clamped).map { laneNum ->
            LaneState(
                laneNumber = laneNum,
                swimmerName = "Bahn $laneNum",
                targetDistanceMeters = currentDist,
                splitDistanceMeters = currentSplit,
                strokeType = currentStroke
            )
        }
        _uiState.value = _uiState.value.copy(
            laneCount = clamped,
            lanes = initialLanes
        )
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (isActive) {
                updateTick()
                delay(33) // ~30 FPS is silky smooth and battery friendly
            }
        }
    }

    private fun stopTickerIfNoneRunning() {
        val anyRunningOrCountdown = _uiState.value.lanes.any {
            it.status == LaneStatus.RUNNING || it.status == LaneStatus.COUNTDOWN
        }
        if (!anyRunningOrCountdown) {
            tickerJob?.cancel()
            tickerJob = null
        }
    }

    private fun updateTick() {
        val now = SystemClock.elapsedRealtime()
        val masterStart = _uiState.value.masterStartTimeRealtime ?: return

        var anyRunning = false
        val updatedLanes = _uiState.value.lanes.map { lane ->
            when (lane.status) {
                LaneStatus.COUNTDOWN -> {
                    val laneStartTarget = masterStart + lane.intervalOffsetMillis
                    if (now >= laneStartTarget) {
                        anyRunning = true
                        SoundUtil.playStartSignal()
                        lane.copy(
                            status = LaneStatus.RUNNING,
                            laneStartTimeRealtime = laneStartTarget,
                            currentElapsedMillis = now - laneStartTarget,
                            countdownRemainingSeconds = 0
                        )
                    } else {
                        val remainingSec = (((laneStartTarget - now) / 1000) + 1).toInt()
                        if (remainingSec != lane.countdownRemainingSeconds && remainingSec <= 3) {
                            SoundUtil.playCountdownBeep()
                        }
                        lane.copy(countdownRemainingSeconds = remainingSec)
                    }
                }
                LaneStatus.RUNNING -> {
                    anyRunning = true
                    val start = lane.laneStartTimeRealtime ?: masterStart
                    lane.copy(currentElapsedMillis = (now - start).coerceAtLeast(0L))
                }
                LaneStatus.FINISHED -> {
                    val start = lane.laneStartTimeRealtime ?: masterStart
                    lane.copy(currentElapsedMillis = (now - start).coerceAtLeast(0L))
                }
                LaneStatus.IDLE -> lane
            }
        }

        _uiState.value = _uiState.value.copy(
            lanes = updatedLanes,
            isAnyRunning = anyRunning
        )
    }

    // --- Time Control Actions ---

    fun onStartAll(context: Context? = null) {
        context?.let { HapticUtil.vibrateHeavy(it) }
        SoundUtil.playStartSignal()
        val now = SystemClock.elapsedRealtime()
        val currentLanes = _uiState.value.lanes
        val mode = _uiState.value.startMode
        val intervalSec = _uiState.value.intervalOffsetSeconds

        pushUndo(UndoAction.BulkStart(currentLanes, _uiState.value.masterStartTimeRealtime))

        val newLanes = currentLanes.mapIndexed { index, lane ->
            if (mode == StartMode.INTERVAL && index > 0) {
                val offsetMillis = index * intervalSec * 1000L
                lane.copy(
                    status = LaneStatus.COUNTDOWN,
                    intervalOffsetMillis = offsetMillis,
                    countdownRemainingSeconds = index * intervalSec,
                    currentElapsedMillis = 0L,
                    laps = emptyList()
                )
            } else {
                lane.copy(
                    status = LaneStatus.RUNNING,
                    laneStartTimeRealtime = now,
                    intervalOffsetMillis = 0L,
                    currentElapsedMillis = 0L,
                    laps = emptyList(),
                    countdownRemainingSeconds = 0
                )
            }
        }

        _uiState.value = _uiState.value.copy(
            masterStartTimeRealtime = now,
            lanes = newLanes,
            isAnyRunning = true
        )
        startTicker()
    }

    fun onStopAll(context: Context? = null) {
        context?.let { HapticUtil.vibrateHeavy(it) }
        val currentLanes = _uiState.value.lanes
        pushUndo(UndoAction.BulkStop(currentLanes))

        val newLanes = currentLanes.map { lane ->
            if (lane.status == LaneStatus.RUNNING || lane.status == LaneStatus.COUNTDOWN) {
                val finalTime = lane.currentElapsedMillis
                val previousSplit = lane.laps.lastOrNull()?.splitTimeMillis ?: 0L
                val lapTime = (finalTime - previousSplit).coerceAtLeast(0L)
                val deltaTarget = lane.targetLapMillis?.let { lapTime - it }
                val finishLap = if (finalTime > 0 && finalTime > previousSplit) {
                    LapItem(
                        lapIndex = lane.laps.size + 1,
                        splitTimeMillis = finalTime,
                        lapTimeMillis = lapTime,
                        targetLapMillis = lane.targetLapMillis,
                        deltaTargetMillis = deltaTarget
                    )
                } else null
                lane.copy(
                    status = LaneStatus.FINISHED,
                    finishTimeMillis = finalTime,
                    laps = if (finishLap != null) lane.laps + finishLap else lane.laps
                )
            } else lane
        }

        _uiState.value = _uiState.value.copy(
            lanes = newLanes,
            isAnyRunning = false
        )
        stopTickerIfNoneRunning()
    }

    fun onLapLane(laneNumber: Int, context: Context? = null) {
        context?.let { HapticUtil.vibrateClick(it) }
        val lanes = _uiState.value.lanes.toMutableList()
        val index = lanes.indexOfFirst { it.laneNumber == laneNumber }
        if (index == -1) return
        val lane = lanes[index]
        if (lane.status != LaneStatus.RUNNING) return

        val splitTime = lane.currentElapsedMillis
        val previousSplit = lane.laps.lastOrNull()?.splitTimeMillis ?: 0L
        val lapTime = (splitTime - previousSplit).coerceAtLeast(0L)
        val deltaTarget = lane.targetLapMillis?.let { lapTime - it }

        val newLap = LapItem(
            lapIndex = lane.laps.size + 1,
            splitTimeMillis = splitTime,
            lapTimeMillis = lapTime,
            targetLapMillis = lane.targetLapMillis,
            deltaTargetMillis = deltaTarget
        )

        pushUndo(UndoAction.LapAdded(laneNumber, newLap))

        lanes[index] = lane.copy(laps = lane.laps + newLap)
        _uiState.value = _uiState.value.copy(lanes = lanes)
    }

    fun onFinishLane(laneNumber: Int, context: Context? = null) {
        context?.let { HapticUtil.vibrateHeavy(it) }
        val lanes = _uiState.value.lanes.toMutableList()
        val index = lanes.indexOfFirst { it.laneNumber == laneNumber }
        if (index == -1) return
        val lane = lanes[index]
        if (lane.status != LaneStatus.RUNNING && lane.status != LaneStatus.COUNTDOWN) return

        val finalTime = lane.currentElapsedMillis
        val previousSplit = lane.laps.lastOrNull()?.splitTimeMillis ?: 0L
        val lapTime = (finalTime - previousSplit).coerceAtLeast(0L)
        val deltaTarget = lane.targetLapMillis?.let { lapTime - it }

        val finishLap = if (finalTime > 0) {
            LapItem(
                lapIndex = lane.laps.size + 1,
                splitTimeMillis = finalTime,
                lapTimeMillis = lapTime,
                targetLapMillis = lane.targetLapMillis,
                deltaTargetMillis = deltaTarget
            )
        } else null

        pushUndo(UndoAction.LaneFinished(laneNumber, lane.status, finishLap))

        lanes[index] = lane.copy(
            status = LaneStatus.FINISHED,
            finishTimeMillis = finalTime,
            laps = if (finishLap != null) lane.laps + finishLap else lane.laps
        )

        val anyRunning = lanes.any { it.status == LaneStatus.RUNNING || it.status == LaneStatus.COUNTDOWN }
        _uiState.value = _uiState.value.copy(
            lanes = lanes,
            isAnyRunning = anyRunning
        )
        if (!anyRunning) stopTickerIfNoneRunning()
    }

    fun onResetAll(context: Context? = null) {
        context?.let { HapticUtil.vibrateHeavy(it) }
        tickerJob?.cancel()
        tickerJob = null
        undoStack.clear()

        val resetLanes = _uiState.value.lanes.map {
            it.copy(
                status = LaneStatus.IDLE,
                laneStartTimeRealtime = null,
                finishTimeMillis = null,
                currentElapsedMillis = 0L,
                laps = emptyList(),
                countdownRemainingSeconds = 0
            )
        }

        _uiState.value = _uiState.value.copy(
            masterStartTimeRealtime = null,
            lanes = resetLanes,
            isAnyRunning = false,
            canUndo = false,
            lastUndoMessage = null
        )
    }

    // --- Undo Logic ---

    private fun pushUndo(action: UndoAction) {
        undoStack.push(action)
        if (undoStack.size > 20) undoStack.removeLast()
        _uiState.value = _uiState.value.copy(canUndo = true)
    }

    fun performUndo(context: Context? = null) {
        if (undoStack.isEmpty()) return
        context?.let { HapticUtil.vibrateClick(it) }

        when (val action = undoStack.pop()) {
            is UndoAction.LapAdded -> {
                val lanes = _uiState.value.lanes.toMutableList()
                val idx = lanes.indexOfFirst { it.laneNumber == action.laneNumber }
                if (idx != -1) {
                    val currentLaps = lanes[idx].laps.toMutableList()
                    if (currentLaps.isNotEmpty()) {
                        currentLaps.removeAt(currentLaps.lastIndex)
                        lanes[idx] = lanes[idx].copy(laps = currentLaps)
                        _uiState.value = _uiState.value.copy(
                            lanes = lanes,
                            lastUndoMessage = "Lap ${action.removedLap.lapIndex} auf Bahn ${action.laneNumber} rückgängig gemacht"
                        )
                    }
                }
            }
            is UndoAction.LaneFinished -> {
                val lanes = _uiState.value.lanes.toMutableList()
                val idx = lanes.indexOfFirst { it.laneNumber == action.laneNumber }
                if (idx != -1) {
                    val restoredLaps = if (action.removedFinishLap != null && lanes[idx].laps.isNotEmpty()) {
                        lanes[idx].laps.dropLast(1)
                    } else lanes[idx].laps
                    lanes[idx] = lanes[idx].copy(
                        status = action.previousStatus,
                        finishTimeMillis = null,
                        laps = restoredLaps
                    )
                    _uiState.value = _uiState.value.copy(
                        lanes = lanes,
                        isAnyRunning = true,
                        lastUndoMessage = "Zielstopp für Bahn ${action.laneNumber} rückgängig gemacht"
                    )
                    startTicker()
                }
            }
            is UndoAction.BulkStart -> {
                _uiState.value = _uiState.value.copy(
                    lanes = action.previousLanes,
                    masterStartTimeRealtime = action.previousMasterStart,
                    isAnyRunning = action.previousLanes.any { it.status == LaneStatus.RUNNING },
                    lastUndoMessage = "Start rückgängig gemacht"
                )
                stopTickerIfNoneRunning()
            }
            is UndoAction.BulkStop -> {
                _uiState.value = _uiState.value.copy(
                    lanes = action.previousLanes,
                    isAnyRunning = true,
                    lastUndoMessage = "Stopp rückgängig gemacht"
                )
                startTicker()
            }
            is UndoAction.LaneRemoved -> {
                val lanes = _uiState.value.lanes.toMutableList()
                val insertIdx = action.index.coerceIn(0, lanes.size)
                lanes.add(insertIdx, action.lane)
                _uiState.value = _uiState.value.copy(
                    lanes = lanes,
                    laneCount = lanes.size,
                    lastUndoMessage = "Bahn ${action.lane.laneNumber} wiederhergestellt"
                )
            }
        }

        _uiState.value = _uiState.value.copy(canUndo = undoStack.isNotEmpty())
    }

    // --- Hardware Volume Keys Integration ---

    fun onVolumeKeyPressed(isVolumeUp: Boolean, context: Context? = null): Boolean {
        // If Water lock is active or screen is untouched:
        // Volume Up: Lap for focused lane, or all running lanes
        // Volume Down: Start / Stop toggle
        return if (isVolumeUp) {
            val runningLanes = _uiState.value.lanes.filter { it.status == LaneStatus.RUNNING }
            if (runningLanes.isNotEmpty()) {
                val targetLane = runningLanes.firstOrNull { it.laneNumber == _uiState.value.selectedFocusedLane }
                    ?: runningLanes.first()
                onLapLane(targetLane.laneNumber, context)
                true
            } else false
        } else {
            if (_uiState.value.isAnyRunning) {
                onStopAll(context)
            } else {
                onStartAll(context)
            }
            true
        }
    }

    // --- Water Lock ---

    fun setWaterLock(active: Boolean, context: Context? = null) {
        context?.let { HapticUtil.vibrateHeavy(it) }
        _uiState.value = _uiState.value.copy(isWaterLockActive = active)
    }

    // --- Configuration & Settings ---

    fun setLaneCount(count: Int) {
        initializeLanes(count)
    }

    fun addLane(context: Context? = null) {
        val currentLanes = _uiState.value.lanes
        if (currentLanes.size >= 15) {
            _uiState.value = _uiState.value.copy(
                lastUndoMessage = "Limit: Maximal 15 Bahnen möglich"
            )
            return
        }
        context?.let { HapticUtil.vibrateClick(it) }
        val nextLaneNumber = (currentLanes.maxOfOrNull { it.laneNumber } ?: 0) + 1
        val newLane = LaneState(
            laneNumber = nextLaneNumber,
            swimmerName = "Bahn $nextLaneNumber",
            targetDistanceMeters = _uiState.value.targetDistanceMeters,
            splitDistanceMeters = _uiState.value.splitDistanceMeters,
            strokeType = _uiState.value.strokeType
        )
        val updated = currentLanes + newLane
        _uiState.value = _uiState.value.copy(
            lanes = updated,
            laneCount = updated.size,
            lastUndoMessage = "Bahn $nextLaneNumber hinzugefügt"
        )
    }

    fun removeLane(laneNumber: Int, context: Context? = null) {
        val currentLanes = _uiState.value.lanes
        if (currentLanes.size <= 1) {
            _uiState.value = _uiState.value.copy(
                lastUndoMessage = "Mindestens 1 Bahn muss aktiv bleiben"
            )
            return
        }
        context?.let { HapticUtil.vibrateClick(it) }
        val index = currentLanes.indexOfFirst { it.laneNumber == laneNumber }
        if (index == -1) return
        val removedLane = currentLanes[index]

        pushUndo(UndoAction.LaneRemoved(removedLane, index))

        val updated = currentLanes.filter { it.laneNumber != laneNumber }
        _uiState.value = _uiState.value.copy(
            lanes = updated,
            laneCount = updated.size,
            lastUndoMessage = "Bahn $laneNumber entfernt"
        )
    }

    fun removeLastLane(context: Context? = null) {
        val currentLanes = _uiState.value.lanes
        if (currentLanes.size <= 1) {
            _uiState.value = _uiState.value.copy(
                lastUndoMessage = "Mindestens 1 Bahn muss aktiv bleiben!"
            )
            return
        }
        removeLane(currentLanes.last().laneNumber, context)
    }

    fun setStartMode(mode: StartMode) {
        _uiState.value = _uiState.value.copy(startMode = mode)
    }

    fun setIntervalOffsetSeconds(seconds: Int) {
        _uiState.value = _uiState.value.copy(intervalOffsetSeconds = seconds.coerceIn(1, 60))
    }

    fun setSelectedFocusedLane(laneNumber: Int) {
        _uiState.value = _uiState.value.copy(selectedFocusedLane = laneNumber)
    }

    fun setSplitDistance(splitMeters: Int) {
        val updatedLanes = _uiState.value.lanes.map {
            it.copy(splitDistanceMeters = splitMeters)
        }
        _uiState.value = _uiState.value.copy(
            splitDistanceMeters = splitMeters,
            lanes = updatedLanes
        )
    }

    fun setTargetDistance(distance: Int) {
        val enforcedDistance = if (_uiState.value.strokeType == "Lagen" && distance < 100) 100 else distance
        val currentSplit = _uiState.value.splitDistanceMeters
        val enforcedSplit = if (currentSplit > enforcedDistance) 25 else currentSplit
        val updatedLanes = _uiState.value.lanes.map {
            it.copy(targetDistanceMeters = enforcedDistance, splitDistanceMeters = enforcedSplit)
        }
        _uiState.value = _uiState.value.copy(
            targetDistanceMeters = enforcedDistance,
            splitDistanceMeters = enforcedSplit,
            lanes = updatedLanes
        )
    }

    fun setStrokeType(stroke: String) {
        val currentDist = _uiState.value.targetDistanceMeters
        val enforcedDist = if (stroke == "Lagen" && currentDist < 100) 100 else currentDist
        val currentSplit = _uiState.value.splitDistanceMeters
        val enforcedSplit = if (stroke == "Lagen" && enforcedDist == 100) 25 else currentSplit
        val updatedLanes = _uiState.value.lanes.map {
            it.copy(strokeType = stroke, targetDistanceMeters = enforcedDist, splitDistanceMeters = enforcedSplit)
        }
        _uiState.value = _uiState.value.copy(
            strokeType = stroke,
            targetDistanceMeters = enforcedDist,
            splitDistanceMeters = enforcedSplit,
            lanes = updatedLanes
        )
    }

    fun setSessionMeta(title: String, stroke: String, poolLength: Int, distance: Int, splitDistance: Int = 50) {
        val enforcedDistance = if (stroke == "Lagen" && distance < 100) 100 else distance
        val updatedLanes = _uiState.value.lanes.map {
            it.copy(
                targetDistanceMeters = enforcedDistance,
                splitDistanceMeters = splitDistance,
                strokeType = stroke
            )
        }
        _uiState.value = _uiState.value.copy(
            sessionTitle = title,
            strokeType = stroke,
            poolLengthMeters = poolLength,
            targetDistanceMeters = enforcedDistance,
            splitDistanceMeters = splitDistance,
            lanes = updatedLanes
        )
    }

    fun setTargetPaceForLane(laneNumber: Int, paceMillis: Long?) {
        val lanes = _uiState.value.lanes.map {
            if (it.laneNumber == laneNumber) it.copy(targetLapMillis = paceMillis) else it
        }
        _uiState.value = _uiState.value.copy(
            lanes = lanes,
            isTargetPaceDialogVisible = false
        )
    }

    fun setTargetPaceForAllLanes(paceMillis: Long?) {
        val lanes = _uiState.value.lanes.map {
            it.copy(targetLapMillis = paceMillis)
        }
        _uiState.value = _uiState.value.copy(
            lanes = lanes,
            isPaceCalculatorVisible = false
        )
    }

    fun openPaceCalculator() {
        _uiState.value = _uiState.value.copy(isPaceCalculatorVisible = true)
    }

    fun closePaceCalculator() {
        _uiState.value = _uiState.value.copy(isPaceCalculatorVisible = false)
    }

    fun openTargetPaceDialog(laneNumber: Int) {
        _uiState.value = _uiState.value.copy(
            isTargetPaceDialogVisible = true,
            targetPaceLane = laneNumber
        )
    }

    fun closeTargetPaceDialog() {
        _uiState.value = _uiState.value.copy(isTargetPaceDialogVisible = false)
    }

    fun openAssignSwimmerDialog(laneNumber: Int) {
        _uiState.value = _uiState.value.copy(
            isAssignSwimmerDialogVisible = true,
            assignSwimmerLane = laneNumber
        )
    }

    fun closeAssignSwimmerDialog() {
        _uiState.value = _uiState.value.copy(isAssignSwimmerDialogVisible = false)
    }

    fun assignSwimmerToLane(laneNumber: Int, swimmer: SwimmerEntity?) {
        val lanes = _uiState.value.lanes.map {
            if (it.laneNumber == laneNumber) {
                it.copy(
                    swimmerId = swimmer?.id,
                    swimmerName = swimmer?.name ?: "Bahn $laneNumber"
                )
            } else it
        }
        _uiState.value = _uiState.value.copy(
            lanes = lanes,
            isAssignSwimmerDialogVisible = false
        )
    }

    // --- Stroke Rate Calculator ---

    fun openStrokeRateDialog(targetLane: Int? = null) {
        _uiState.value = _uiState.value.copy(
            isStrokeRateDialogVisible = true,
            strokeRateTargetLane = targetLane
        )
    }

    fun closeStrokeRateDialog() {
        _uiState.value = _uiState.value.copy(isStrokeRateDialogVisible = false)
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
                _uiState.value = _uiState.value.copy(lanes = lanes)
            }
        }
    }

    // --- Save Session to Room Database & Automatic PB Update ---

    fun saveCurrentSession(context: Context? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            val session = SessionEntity(
                title = state.sessionTitle.ifBlank { "Training ${TimeFormatter.formatDateShort(System.currentTimeMillis())}" },
                poolLengthMeters = state.poolLengthMeters,
                targetDistanceMeters = state.targetDistanceMeters,
                strokeType = state.strokeType,
                startMode = state.startMode.name,
                intervalOffsetSeconds = state.intervalOffsetSeconds,
                laneCount = state.laneCount,
                splitDistanceMeters = state.splitDistanceMeters,
                notes = "Erfasst mit SwimTimer Pro am Beckenrand"
            )

            val lapEntities = mutableListOf<LapRecordEntity>()
            state.lanes.forEach { lane ->
                lane.laps.forEach { lap ->
                    lapEntities.add(
                        LapRecordEntity(
                            sessionId = 0, // Assigned by repository
                            swimmerId = lane.swimmerId,
                            swimmerName = lane.swimmerName,
                            laneNumber = lane.laneNumber,
                            lapIndex = lap.lapIndex,
                            splitTimeMillis = lap.splitTimeMillis,
                            lapTimeMillis = lap.lapTimeMillis,
                            targetLapMillis = lap.targetLapMillis,
                            deltaTargetMillis = lap.deltaTargetMillis,
                            strokeRateSpm = lap.strokeRateSpm
                        )
                    )
                }
            }

            repository.saveSessionWithLaps(session, lapEntities)

            // Automatic PB update check for assigned athletes:
            // "wenn man die session saved, die pb aktualisiert wird wenn sie besser war"
            val pbAchievements = mutableListOf<String>()
            state.lanes.forEach { lane ->
                val sId = lane.swimmerId
                if (sId != null && (lane.laps.isNotEmpty() || lane.finishTimeMillis != null)) {
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
                        var hasImproved = false

                        val finishTime = lane.finishTimeMillis ?: lane.laps.lastOrNull()?.splitTimeMillis

                        // Target 25m Sprint: Only update 25m PB if stroke matches swimmer's Hauptlage
                        if (state.targetDistanceMeters == 25 && finishTime != null && finishTime > 0 && state.strokeType != "Lagen") {
                            if (isSameStroke(state.strokeType, swimmer.favoriteStroke)) {
                                if (updatedPb25 == null || finishTime < updatedPb25) {
                                    updatedPb25 = finishTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (25m ${swimmer.favoriteStroke} PB: ${TimeFormatter.formatMillis(finishTime, shortFormat = true)})")
                                }
                            }
                        }

                        // Target 50m: Only update 50m PB if stroke matches swimmer's Hauptlage
                        if (state.targetDistanceMeters == 50 && finishTime != null && finishTime > 0 && state.strokeType != "Lagen") {
                            if (isSameStroke(state.strokeType, swimmer.favoriteStroke)) {
                                if (updatedPb50 == null || finishTime < updatedPb50) {
                                    updatedPb50 = finishTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (50m ${swimmer.favoriteStroke} PB: ${TimeFormatter.formatMillis(finishTime, shortFormat = true)})")
                                }
                            }
                        }

                        // Target 100m: Only update 100m PB if stroke matches swimmer's Hauptlage
                        if (state.targetDistanceMeters == 100 && finishTime != null && finishTime > 0 && state.strokeType != "Lagen") {
                            if (isSameStroke(state.strokeType, swimmer.favoriteStroke)) {
                                if (updatedPb100 == null || finishTime < updatedPb100) {
                                    updatedPb100 = finishTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (100m ${swimmer.favoriteStroke} PB: ${TimeFormatter.formatMillis(finishTime)})")
                                }
                            }
                        }

                        // Also: If split interval is 25m or 50m, check splits for non-Lagen matching Hauptlage
                        if (state.strokeType != "Lagen" && lane.laps.isNotEmpty() && isSameStroke(state.strokeType, swimmer.favoriteStroke)) {
                            if (state.splitDistanceMeters == 25) {
                                val split1 = lane.laps[0].splitTimeMillis
                                if (split1 > 0 && (updatedPb25 == null || split1 < updatedPb25)) {
                                    updatedPb25 = split1
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (25m ${swimmer.favoriteStroke} Split: ${TimeFormatter.formatMillis(split1, shortFormat = true)})")
                                }
                            }
                            if (state.splitDistanceMeters == 50) {
                                val split1 = lane.laps[0].splitTimeMillis
                                if (split1 > 0 && (updatedPb50 == null || split1 < updatedPb50)) {
                                    updatedPb50 = split1
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (50m ${swimmer.favoriteStroke} Split: ${TimeFormatter.formatMillis(split1, shortFormat = true)})")
                                }
                            }
                        }

                        // Lagen (Official Einzel-Reihenfolge: 1. Schmett, 2. Rücken, 3. Brust, 4. Freistil)
                        if (state.strokeType == "Lagen") {
                            // Total Lagen PBs
                            if (state.targetDistanceMeters == 100 && finishTime != null && finishTime > 0) {
                                if (updatedPb100Lagen == null || finishTime < updatedPb100Lagen) {
                                    updatedPb100Lagen = finishTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (100m Lagen: ${TimeFormatter.formatMillis(finishTime)})")
                                }
                            } else if (state.targetDistanceMeters == 200 && finishTime != null && finishTime > 0) {
                                if (updatedPb200Lagen == null || finishTime < updatedPb200Lagen) {
                                    updatedPb200Lagen = finishTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (200m Lagen: ${TimeFormatter.formatMillis(finishTime)})")
                                }
                            } else if (state.targetDistanceMeters == 400 && finishTime != null && finishTime > 0) {
                                if (updatedPb400Lagen == null || finishTime < updatedPb400Lagen) {
                                    updatedPb400Lagen = finishTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (400m Lagen: ${TimeFormatter.formatMillis(finishTime)})")
                                }
                            }

                            // 4 official Lagen legs in exact sequence:
                            // 1. Schmetterling, 2. Rücken, 3. Brust, 4. Freistil
                            val legDist = (state.targetDistanceMeters / 4).coerceAtLeast(1)
                            val splitsPerLeg = (legDist / state.splitDistanceMeters).coerceAtLeast(1)

                            fun getLegTime(legIndex: Int): Long? {
                                val startIdx = legIndex * splitsPerLeg
                                val endIdx = startIdx + splitsPerLeg
                                if (lane.laps.size >= endIdx) {
                                    var sum = 0L
                                    for (i in startIdx until endIdx) {
                                        sum += lane.laps[i].lapTimeMillis
                                    }
                                    return if (sum > 0) sum else null
                                }
                                return null
                            }

                            val flyLegTime = getLegTime(0)
                            val backLegTime = getLegTime(1)
                            val breastLegTime = getLegTime(2)
                            val freeLegTime = getLegTime(3)

                            // 200m Lagen: 4x 50m Teilstrecken!
                            if (state.targetDistanceMeters == 200) {
                                // 1. Schmett (50m)
                                if (flyLegTime != null && (updatedPb200Fly == null || flyLegTime < updatedPb200Fly)) {
                                    updatedPb200Fly = flyLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (200L 1. Schmett (50m): ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                                }
                                if (flyLegTime != null && isSameStroke(swimmer.favoriteStroke, "Schmetterling") && (updatedPb50 == null || flyLegTime < updatedPb50)) {
                                    updatedPb50 = flyLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (50m Schmett PB neu via 200L: ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                                }

                                // 2. Rücken (50m)
                                if (backLegTime != null && (updatedPb200Back == null || backLegTime < updatedPb200Back)) {
                                    updatedPb200Back = backLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (200L 2. Rücken (50m): ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                                }
                                if (backLegTime != null && isSameStroke(swimmer.favoriteStroke, "Rücken") && (updatedPb50 == null || backLegTime < updatedPb50)) {
                                    updatedPb50 = backLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (50m Rücken PB neu via 200L: ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                                }

                                // 3. Brust (50m)
                                if (breastLegTime != null && (updatedPb200Breast == null || breastLegTime < updatedPb200Breast)) {
                                    updatedPb200Breast = breastLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (200L 3. Brust (50m): ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                                }
                                if (breastLegTime != null && isSameStroke(swimmer.favoriteStroke, "Brust") && (updatedPb50 == null || breastLegTime < updatedPb50)) {
                                    updatedPb50 = breastLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (50m Brust PB neu via 200L: ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                                }

                                // 4. Freistil / Kraul (50m)
                                if (freeLegTime != null) {
                                    if (updatedPb200Free == null || freeLegTime < updatedPb200Free) {
                                        updatedPb200Free = freeLegTime
                                        hasImproved = true
                                        pbAchievements.add("${swimmer.name} (200L 4. Kraul (50m): ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                                    }
                                    // ONLY update 50m PB if swimmer's favorite stroke is Freistil/Kraul!
                                    if (isSameStroke(swimmer.favoriteStroke, "Freistil")) {
                                        if (updatedPb50 == null || freeLegTime < updatedPb50) {
                                            updatedPb50 = freeLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (50m Freistil PB neu via 200L: ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                                        }
                                    }
                                }
                            } else if (state.targetDistanceMeters == 100) {
                                // 100m Lagen: 4x 25m Teilstrecken!
                                if (flyLegTime != null && (updatedFly == null || flyLegTime < updatedFly)) {
                                    updatedFly = flyLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (100L 1. Schmett (25m): ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                                }
                                if (flyLegTime != null && isSameStroke(swimmer.favoriteStroke, "Schmetterling") && (updatedPb25 == null || flyLegTime < updatedPb25)) {
                                    updatedPb25 = flyLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (25m Schmett PB neu via 100L: ${TimeFormatter.formatMillis(flyLegTime, shortFormat = true)})")
                                }

                                if (backLegTime != null && (updatedBack == null || backLegTime < updatedBack)) {
                                    updatedBack = backLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (100L 2. Rücken (25m): ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                                }
                                if (backLegTime != null && isSameStroke(swimmer.favoriteStroke, "Rücken") && (updatedPb25 == null || backLegTime < updatedPb25)) {
                                    updatedPb25 = backLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (25m Rücken PB neu via 100L: ${TimeFormatter.formatMillis(backLegTime, shortFormat = true)})")
                                }

                                if (breastLegTime != null && (updatedBreast == null || breastLegTime < updatedBreast)) {
                                    updatedBreast = breastLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (100L 3. Brust (25m): ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                                }
                                if (breastLegTime != null && isSameStroke(swimmer.favoriteStroke, "Brust") && (updatedPb25 == null || breastLegTime < updatedPb25)) {
                                    updatedPb25 = breastLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (25m Brust PB neu via 100L: ${TimeFormatter.formatMillis(breastLegTime, shortFormat = true)})")
                                }

                                if (freeLegTime != null) {
                                    if (updatedFree == null || freeLegTime < updatedFree) {
                                        updatedFree = freeLegTime
                                        hasImproved = true
                                        pbAchievements.add("${swimmer.name} (100L 4. Freistil (25m): ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                                    }
                                    // ONLY update 25m PB if swimmer's favorite stroke is Freistil/Kraul!
                                    if (isSameStroke(swimmer.favoriteStroke, "Freistil")) {
                                        if (updatedPb25 == null || freeLegTime < updatedPb25) {
                                            updatedPb25 = freeLegTime
                                            hasImproved = true
                                            pbAchievements.add("${swimmer.name} (25m Freistil PB neu via 100L: ${TimeFormatter.formatMillis(freeLegTime, shortFormat = true)})")
                                        }
                                    }
                                }
                            } else if (state.targetDistanceMeters == 400) {
                                // 400m Lagen: 4x 100m Teilstrecken!
                                // 1. Schmetterling (100m)
                                if (flyLegTime != null && (updatedPb400Fly == null || flyLegTime < updatedPb400Fly)) {
                                    updatedPb400Fly = flyLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (400L 1. Schmett (100m): ${TimeFormatter.formatMillis(flyLegTime)})")
                                }
                                if (flyLegTime != null && isSameStroke(swimmer.favoriteStroke, "Schmetterling") && (updatedPb100 == null || flyLegTime < updatedPb100)) {
                                    updatedPb100 = flyLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (100m Schmett PB neu via 400L: ${TimeFormatter.formatMillis(flyLegTime)})")
                                }

                                // 2. Rücken (100m)
                                if (backLegTime != null && (updatedPb400Back == null || backLegTime < updatedPb400Back)) {
                                    updatedPb400Back = backLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (400L 2. Rücken (100m): ${TimeFormatter.formatMillis(backLegTime)})")
                                }
                                if (backLegTime != null && isSameStroke(swimmer.favoriteStroke, "Rücken") && (updatedPb100 == null || backLegTime < updatedPb100)) {
                                    updatedPb100 = backLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (100m Rücken PB neu via 400L: ${TimeFormatter.formatMillis(backLegTime)})")
                                }

                                // 3. Brust (100m)
                                if (breastLegTime != null && (updatedPb400Breast == null || breastLegTime < updatedPb400Breast)) {
                                    updatedPb400Breast = breastLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (400L 3. Brust (100m): ${TimeFormatter.formatMillis(breastLegTime)})")
                                }
                                if (breastLegTime != null && isSameStroke(swimmer.favoriteStroke, "Brust") && (updatedPb100 == null || breastLegTime < updatedPb100)) {
                                    updatedPb100 = breastLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (100m Brust PB neu via 400L: ${TimeFormatter.formatMillis(breastLegTime)})")
                                }

                                // 4. Freistil (100m)
                                if (freeLegTime != null && (updatedPb400Free == null || freeLegTime < updatedPb400Free)) {
                                    updatedPb400Free = freeLegTime
                                    hasImproved = true
                                    pbAchievements.add("${swimmer.name} (400L 4. Freistil (100m): ${TimeFormatter.formatMillis(freeLegTime)})")
                                }
                                // ONLY update 100m PB if swimmer's favorite stroke is Freistil/Kraul!
                                if (isSameStroke(swimmer.favoriteStroke, "Freistil")) {
                                    if (updatedPb100 == null || freeLegTime < updatedPb100) {
                                        updatedPb100 = freeLegTime
                                        hasImproved = true
                                        pbAchievements.add("${swimmer.name} (100m Freistil PB neu via 400L: ${TimeFormatter.formatMillis(freeLegTime)})")
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
                                    pb400mLagenFreeSplitMillis = updatedPb400Free
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

            context?.let { HapticUtil.vibrateHeavy(it) }
            _uiState.value = _uiState.value.copy(
                isSessionSavedSuccess = true,
                saveNotificationMessage = successMsg
            )
        }
    }

    fun clearSavedSuccess() {
        _uiState.value = _uiState.value.copy(
            isSessionSavedSuccess = false,
            saveNotificationMessage = null
        )
    }

    // --- Swimmer CRUD ---

    fun addSwimmer(
        name: String,
        stroke: String,
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
        pb400Free: Long? = null
    ) {
        viewModelScope.launch {
            val swimmer = SwimmerEntity(
                name = name,
                ageGroup = "Offen",
                favoriteStroke = stroke,
                pb25mMillis = pb25m,
                pb50mMillis = pb50m,
                pb100mMillis = pb100m,
                pb100mLagenMillis = pb100mLagen,
                pb200mLagenMillis = pb200mLagen,
                pb400mLagenMillis = pb400mLagen,
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
                pb400mLagenFreeSplitMillis = pb400Free
            )
            repository.insertSwimmer(swimmer)
        }
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
            if (isLagenA && isLagenB) return true
            return false
        }
    }

    fun updateSwimmer(swimmer: SwimmerEntity) {
        viewModelScope.launch {
            repository.updateSwimmer(swimmer)
        }
    }

    fun deleteSwimmer(swimmer: SwimmerEntity) {
        viewModelScope.launch {
            repository.deleteSwimmer(swimmer)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSessionById(sessionId)
        }
    }
}

class StopwatchViewModelFactory(
    private val repository: SwimRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return StopwatchViewModel(repository) as T
    }
}
