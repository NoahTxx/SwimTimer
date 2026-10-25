package com.example.ui

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.LapRecordEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SessionWithLaps
import com.example.data.model.SwimmerEntity
import com.example.data.repository.SwimRepository
import com.example.util.ExportHelper
import com.example.util.HapticUtil
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
    FINISHED
}

enum class StartMode {
    MASS,
    INTERVAL
}

@Immutable
data class LapItem(
    val lapIndex: Int,
    val splitTimeMillis: Long,
    val lapTimeMillis: Long,
    val targetLapMillis: Long? = null,
    val deltaTargetMillis: Long? = null,
    val strokeRateSpm: Double? = null,
)

@Immutable
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
    val strokeType: String = "Freistil",
    val spmTapStartRealtime: Long? = null,
    val currentSpm: Double? = null,
)

sealed interface UndoAction {
    data class LapAdded(
        val laneNumber: Int,
        val previousStatus: LaneStatus,
        val previousFinishTime: Long?,
        val removedLap: LapItem,
    ) : UndoAction
    data class LaneFinished(
        val laneNumber: Int,
        val previousStatus: LaneStatus,
        val removedFinishLap: LapItem?,
    ) : UndoAction
    data class BulkStart(val previousLanes: List<LaneState>, val previousMasterStart: Long?) : UndoAction
    data class BulkStop(val previousLanes: List<LaneState>) : UndoAction
    data class LaneRemoved(val lane: LaneState, val index: Int) : UndoAction
}

@Immutable
data class StopwatchUiState(
    val laneCount: Int = 4,
    val lanes: List<LaneState> = emptyList(),
    val masterStartTimeRealtime: Long? = null,
    val startMode: StartMode = StartMode.MASS,
    val intervalOffsetSeconds: Int = 5,
    val selectedFocusedLane: Int = 1,
    val sessionTitle: String = "Trainingseinheit",
    val poolLengthMeters: Int = 50,
    val targetDistanceMeters: Int = 100,
    val splitDistanceMeters: Int = 50,
    val strokeType: String = "Freistil",
    val canUndo: Boolean = false,
    val lastUndoMessage: String? = null,
    val isCurrentRunSaved: Boolean = false,
    val isStrokeRateDialogVisible: Boolean = false,
    val strokeRateTargetLane: Int? = null,
    val isTargetPaceDialogVisible: Boolean = false,
    val targetPaceLane: Int? = null,
    val isAssignSwimmerDialogVisible: Boolean = false,
    val assignSwimmerLane: Int? = null,
    val isSessionSavedSuccess: Boolean = false,
    val saveNotificationMessage: String? = null,
    val isPaceCalculatorVisible: Boolean = false,
) {
    val hasTickingLanes: Boolean
        get() = lanes.any { lane -> lane.status == LaneStatus.RUNNING || lane.status == LaneStatus.COUNTDOWN }

    val hasOpenRun: Boolean
        get() = lanes.any { (it.status == LaneStatus.RUNNING) || (it.status == LaneStatus.COUNTDOWN) || (it.status == LaneStatus.PAUSED) }
}

@Immutable
data class SwimmerCardUiModel(
    val id: Long,
    val name: String,
    val groupOrClub: String,
    val pb25mText: String,
    val pb50mText: String,
    val pb100mText: String,
    val pb100mImText: String,
    val groupName: String? = null,
    val originalEntity: SwimmerEntity,
)

class StopwatchViewModel(
    private val repository: SwimRepository,
    context: Context? = null,
    private val clock: MonotonicClock = AndroidMonotonicClock(),
) : ViewModel() {

    private val prefs = context?.applicationContext?.getSharedPreferences("swim_timer_preferences", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = _uiState.asStateFlow()

    val allSwimmers: StateFlow<List<SwimmerEntity>> = repository.allSwimmers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val swimmersSearchQuery = MutableStateFlow("")

    val selectedGroupFilter = MutableStateFlow<String?>(null)

    val swimmerUiList: StateFlow<List<SwimmerCardUiModel>> = combine(
        allSwimmers,
        swimmersSearchQuery,
        selectedGroupFilter,
    ) { swimmers, query, selectedGroup ->
        var filtered = swimmers
        if (selectedGroup != null) {
            filtered = filtered.filter { it.groupName.equals(selectedGroup, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }
        filtered.map { swimmer ->
            SwimmerCardUiModel(
                id = swimmer.id,
                name = swimmer.name,
                groupOrClub = swimmer.favoriteStroke,
                pb25mText = swimmer.pb25mMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "",
                pb50mText = swimmer.pb50mMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "",
                pb100mText = swimmer.pb100mMillis?.let { TimeFormatter.formatMillis(it) } ?: "",
                pb100mImText = swimmer.pb100mLagenMillis?.let { TimeFormatter.formatMillis(it) } ?: "",
                groupName = swimmer.groupName,
                originalEntity = swimmer,
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<SessionWithLaps>> = repository.allSessionsWithLaps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val undoStack = LinkedList<UndoAction>()
    private var tickerJob: Job? = null

    init {
        loadSavedPreferences()
        if (prefs == null) {
            initializeLanes(4)
        }
    }

    private fun validateDistances(targetMeters: Int, splitMeters: Int) {
        require(targetMeters > 0)
        require(splitMeters > 0)
        require(targetMeters % splitMeters == 0) {
            "Die Zieldistanz muss durch die Splitdistanz teilbar sein."
        }
    }

    private fun sanitizeDistanceConfig(targetMeters: Int, splitMeters: Int): Pair<Int, Int> {
        val safeSplit = splitMeters.coerceIn(25, 1000).takeIf { it > 0 } ?: 50
        val safeTarget = targetMeters.coerceIn(safeSplit, 2000).takeIf { it > 0 } ?: safeSplit
        return if (safeTarget % safeSplit == 0) {
            safeTarget to safeSplit
        } else {
            val alignedTarget = safeSplit * ((safeTarget / safeSplit).coerceAtLeast(1))
            alignedTarget to safeSplit
        }
    }

    private fun loadSavedPreferences() {
        if (prefs == null) return
        val pool = prefs.getInt("poolLengthMeters", 50)
        val dist = prefs.getInt("targetDistanceMeters", 100)
        val split = prefs.getInt("splitDistanceMeters", 50)
        val stroke = prefs.getString("strokeType", "Freistil") ?: "Freistil"
        val startModeStr = prefs.getString("startMode", "MASS") ?: "MASS"
        val interval = prefs.getInt("intervalOffsetSeconds", 5)
        val count = prefs.getInt("laneCount", 4)

        val startModeVal = try {
            StartMode.valueOf(startModeStr)
        } catch (_: Exception) {
            StartMode.MASS
        }

        val (safeTargetMeters, safeSplitMeters) = try {
            validateDistances(dist, split)
            dist to split
        } catch (_: IllegalArgumentException) {
            sanitizeDistanceConfig(dist, split)
        }

        _uiState.update { currentState ->
            currentState.copy(
                poolLengthMeters = pool.coerceIn(25, 2000),
                targetDistanceMeters = safeTargetMeters,
                splitDistanceMeters = safeSplitMeters,
                strokeType = stroke.ifBlank { "Freistil" },
                startMode = startModeVal,
                intervalOffsetSeconds = interval.coerceIn(1, 30),
                laneCount = count.coerceIn(1, 15),
                sessionTitle = "${safeTargetMeters}m ${stroke.ifBlank { "Freistil" }}",
            )
        }
        initializeLanes(count.coerceIn(1, 15))
    }

    private fun savePreferences() {
        prefs?.edit()?.apply {
            putInt("poolLengthMeters", _uiState.value.poolLengthMeters)
            putInt("targetDistanceMeters", _uiState.value.targetDistanceMeters)
            putInt("splitDistanceMeters", _uiState.value.splitDistanceMeters)
            putString("strokeType", _uiState.value.strokeType)
            putString("startMode", _uiState.value.startMode.name)
            putInt("intervalOffsetSeconds", _uiState.value.intervalOffsetSeconds)
            putInt("laneCount", _uiState.value.laneCount)
            apply()
        }
    }

    fun initializeLanes(count: Int) {
        val clamped = count.coerceIn(1, 15)
        _uiState.update { currentState ->
            val currentDist = currentState.targetDistanceMeters
            val currentSplit = currentState.splitDistanceMeters
            val currentStroke = currentState.strokeType

            val safeDist = runCatching { validateDistances(currentDist, currentSplit); currentDist }
                .getOrElse { sanitizeDistanceConfig(currentDist, currentSplit).first }

            val initialLanes = (1..clamped).map { laneNum ->
                LaneState(
                    laneNumber = laneNum,
                    swimmerName = "Bahn $laneNum",
                    targetDistanceMeters = safeDist,
                    splitDistanceMeters = currentSplit,
                    strokeType = currentStroke,
                )
            }
            currentState.copy(
                laneCount = clamped,
                lanes = initialLanes,
                isCurrentRunSaved = false,
            )
        }
        savePreferences()
    }

    // Remaining file unchanged below this point.
}

