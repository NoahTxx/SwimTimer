package com.example.ui

import com.example.data.model.SwimmerEntity
import com.example.data.repository.FakeSwimRepository
import com.example.util.FakeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StopwatchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeSwimRepository()
    private val fakeClock = FakeClock(currentMillis = 100_000L)

    private lateinit var viewModel: StopwatchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = StopwatchViewModel(
            repository = fakeRepository,
            preferencesRepository = null,
            clock = fakeClock,
            soundPlayer = com.example.util.NoOpSoundPlayer()
        )
    }

    @After
    fun tearDown() {
        viewModel.onResetAll()
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasDefaultLanesIdle() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertTrue("Default lanes should not be empty", state.lanes.isNotEmpty())
        assertTrue("All lanes should initially be IDLE", state.lanes.all { it.status == LaneStatus.IDLE })
        assertEquals("Master start time should initially be null", null, state.masterStartTimeRealtime)
    }

    @Test
    fun onStartAll_setsLanesToRunning() = runTest(testDispatcher) {
        viewModel.onStartAll()

        val state = viewModel.uiState.value
        assertEquals(100_000L, state.masterStartTimeRealtime)
        assertEquals(LaneStatus.RUNNING, state.lanes[0].status)
        assertEquals(100_000L, state.lanes[0].laneStartTimeRealtime)
        assertFalse(state.isCurrentRunSaved)

        viewModel.onResetAll()
    }

    @Test
    fun onStartSingleLane_onlyStartsTargetLane() = runTest(testDispatcher) {
        viewModel.onStartSingleLane(laneNumber = 2)

        val state = viewModel.uiState.value
        val lane1 = state.lanes.find { it.laneNumber == 1 }!!
        val lane2 = state.lanes.find { it.laneNumber == 2 }!!

        assertEquals(LaneStatus.IDLE, lane1.status)
        assertEquals(LaneStatus.RUNNING, lane2.status)
        assertEquals(100_000L, lane2.laneStartTimeRealtime)

        viewModel.onResetAll()
    }

    @Test
    fun onLapLane_recordsLapItem() = runTest(testDispatcher) {
        viewModel.onStartSingleLane(laneNumber = 1)

        fakeClock.currentMillis = 130_000L
        viewModel.onLapLane(laneNumber = 1)

        val state = viewModel.uiState.value
        val lane1 = state.lanes.find { it.laneNumber == 1 }!!

        assertEquals(1, lane1.laps.size)
        assertEquals(30_000L, lane1.laps[0].splitTimeMillis)
        assertEquals(30_000L, lane1.laps[0].lapTimeMillis)

        fakeClock.currentMillis = 162_000L
        viewModel.onLapLane(laneNumber = 1)

        val lane1AfterLap2 = viewModel.uiState.value.lanes.find { it.laneNumber == 1 }!!
        assertEquals(2, lane1AfterLap2.laps.size)
        assertEquals(62_000L, lane1AfterLap2.laps[1].splitTimeMillis)
        assertEquals(32_000L, lane1AfterLap2.laps[1].lapTimeMillis)

        viewModel.onResetAll()
    }

    @Test
    fun onFinishLane_setsFinishedStatusAndFinishTime() = runTest(testDispatcher) {
        viewModel.onStartSingleLane(laneNumber = 1)

        fakeClock.currentMillis = 145_000L
        viewModel.onFinishLane(laneNumber = 1)

        val lane1 = viewModel.uiState.value.lanes.find { it.laneNumber == 1 }!!
        assertEquals(LaneStatus.FINISHED, lane1.status)
        assertEquals(45_000L, lane1.finishTimeMillis)

        viewModel.onResetAll()
    }

    @Test
    fun onResetAll_resetsLanesToIdle() = runTest(testDispatcher) {
        viewModel.onStartAll()
        fakeClock.currentMillis = 120_000L
        viewModel.onResetAll()

        val state = viewModel.uiState.value
        assertTrue("All lanes should be IDLE after reset", state.lanes.all { it.status == LaneStatus.IDLE })
        assertEquals(null, state.masterStartTimeRealtime)
    }

    @Test
    fun assignSwimmerToLane_assignsCorrectly() = runTest(testDispatcher) {
        val swimmer = SwimmerEntity(id = 42L, name = "Michael Phelps", groupName = "Elite")
        viewModel.assignSwimmerToLane(laneNumber = 1, swimmer = swimmer)

        val lane1 = viewModel.uiState.value.lanes.find { it.laneNumber == 1 }!!
        assertEquals(42L, lane1.swimmerId)
        assertEquals("Michael Phelps", lane1.swimmerName)
    }

    @Test
    fun addLane_increasesLaneCount() = runTest(testDispatcher) {
        val initialCount = viewModel.uiState.value.lanes.size
        viewModel.addLane()

        val newCount = viewModel.uiState.value.lanes.size
        assertEquals(initialCount + 1, newCount)
    }

    @Test
    fun saveCurrentSession_persistsSessionAndLapsToRepository() = runTest(testDispatcher) {
        val swimmer = SwimmerEntity(id = 10L, name = "Katie Ledecky", groupName = "Distance")
        fakeRepository.insertSwimmer(swimmer)
        viewModel.assignSwimmerToLane(laneNumber = 1, swimmer = swimmer)

        viewModel.onStartSingleLane(laneNumber = 1)
        fakeClock.currentMillis = 130_000L
        viewModel.onLapLane(laneNumber = 1)
        fakeClock.currentMillis = 160_000L
        viewModel.onFinishLane(laneNumber = 1)

        viewModel.saveCurrentSession()
        testScheduler.advanceTimeBy(100)

        val sessions = fakeRepository.allSessionsWithLapsSync()
        assertEquals(1, sessions.size)
        val savedSession = sessions[0]
        assertEquals("100m Freistil", savedSession.session.title)
        assertEquals(2, savedSession.laps.size)
        assertEquals("Katie Ledecky", savedSession.laps[0].swimmerName)
        assertEquals(30_000L, savedSession.laps[0].splitTimeMillis)
        assertEquals(60_000L, savedSession.laps[1].splitTimeMillis)
        assertTrue(viewModel.uiState.value.isCurrentRunSaved)

        viewModel.onResetAll()
    }

    @Test
    fun onFinishLane_whenPaused_finishesWithPausedElapsedTime() = runTest(testDispatcher) {
        viewModel.onStartSingleLane(laneNumber = 1)
        fakeClock.currentMillis = 128_450L
        viewModel.onPauseLane(laneNumber = 1)

        val pausedLane = viewModel.uiState.value.lanes.find { it.laneNumber == 1 }!!
        assertEquals(LaneStatus.PAUSED, pausedLane.status)
        assertEquals(28_450L, pausedLane.currentElapsedMillis)

        // Advance clock while paused — finish time must stay at paused elapsed time (28.45s)
        fakeClock.currentMillis = 190_000L
        viewModel.onFinishLane(laneNumber = 1, exactRealtime = 190_000L)

        val finishedLane = viewModel.uiState.value.lanes.find { it.laneNumber == 1 }!!
        assertEquals(LaneStatus.FINISHED, finishedLane.status)
        assertEquals(28_450L, finishedLane.finishTimeMillis)

        viewModel.onResetAll()
    }

    @Test
    fun performUndo_afterSendOffOverrunSeriesTransition_restoresPreviousSeriesState() = runTest(testDispatcher) {
        viewModel.setSeriesConfig(
            SeriesConfig(
                isEnabled = true,
                mode = SeriesModeType.SEND_OFF,
                sendOffDurationSeconds = 60
            )
        )
        viewModel.onStartSingleLane(laneNumber = 1)

        // Lap 1 at 32s
        fakeClock.currentMillis = 132_000L
        viewModel.onLapLane(laneNumber = 1, exactRealtime = 132_000L)

        // Lap 2 (final lap of Series 1) at 65s -> triggers SEND_OFF overrun transition to Series 2
        fakeClock.currentMillis = 165_000L
        viewModel.onLapLane(laneNumber = 1, exactRealtime = 165_000L)

        val laneAfterOverrun = viewModel.uiState.value.lanes.find { it.laneNumber == 1 }!!
        assertEquals(2, laneAfterOverrun.seriesCount)
        assertTrue(laneAfterOverrun.laps.isEmpty())
        assertEquals(2, laneAfterOverrun.completedSeriesLaps.size)

        // Undo should restore Series 1 with 1 lap and empty completedSeriesLaps
        viewModel.performUndo()

        val laneAfterUndo = viewModel.uiState.value.lanes.find { it.laneNumber == 1 }!!
        assertEquals(1, laneAfterUndo.seriesCount)
        assertEquals(1, laneAfterUndo.laps.size)
        assertTrue(laneAfterUndo.completedSeriesLaps.isEmpty())

        viewModel.onResetAll()
    }
}
