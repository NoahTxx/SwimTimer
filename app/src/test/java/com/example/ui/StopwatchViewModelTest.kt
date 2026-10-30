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

    @Test
    fun performUndo_afterPauseAndResume_restoresLaneState() = runTest(testDispatcher) {
        viewModel.onStartSingleLane(laneNumber = 1)
        fakeClock.currentMillis = 125_000L
        viewModel.onPauseLane(laneNumber = 1)

        assertEquals(LaneStatus.PAUSED, viewModel.uiState.value.lanes.find { it.laneNumber == 1 }!!.status)

        // Undo pause -> should be RUNNING again
        fakeClock.currentMillis = 130_000L
        viewModel.performUndo()
        val resumedByUndo = viewModel.uiState.value.lanes.find { it.laneNumber == 1 }!!
        assertEquals(LaneStatus.RUNNING, resumedByUndo.status)
        assertEquals(25_000L, resumedByUndo.currentElapsedMillis)

        // Pause again, then resume, then undo resume -> should be PAUSED again
        viewModel.onPauseLane(laneNumber = 1)
        fakeClock.currentMillis = 140_000L
        viewModel.onResumeLane(laneNumber = 1)
        assertEquals(LaneStatus.RUNNING, viewModel.uiState.value.lanes.find { it.laneNumber == 1 }!!.status)

        viewModel.performUndo()
        val pausedByUndo = viewModel.uiState.value.lanes.find { it.laneNumber == 1 }!!
        assertEquals(LaneStatus.PAUSED, pausedByUndo.status)
        assertEquals(25_000L, pausedByUndo.currentElapsedMillis)

        viewModel.onResetAll()
    }

    @Test
    fun deleteSession_preservesManualPbsWhileClearingDeletedSessionPb() = runTest(testDispatcher) {
        // Swimmer has a manual 50m Brust PB (34.00s) and no 100m Freistil PB yet
        val manualCustomPbs = SwimmerEntity.serializeCustomPbsMap(mapOf(("Brust" to 50) to 34_000L))
        val swimmer = SwimmerEntity(
            id = 77L,
            name = "Manual PB Swimmer",
            favoriteStroke = "Freistil",
            pb50mMillis = 26_500L, // manual 50m Freistil PB
            customPbsJson = manualCustomPbs
        )
        fakeRepository.insertSwimmer(swimmer)
        viewModel.assignSwimmerToLane(laneNumber = 1, swimmer = swimmer)

        // Swim a 100m Freistil session in 58.00s (achieves a new 100m Freistil PB)
        viewModel.onStartSingleLane(laneNumber = 1)
        fakeClock.currentMillis = 128_000L
        viewModel.onLapLane(laneNumber = 1)
        fakeClock.currentMillis = 158_000L
        viewModel.onFinishLane(laneNumber = 1)

        viewModel.saveCurrentSession()
        testScheduler.advanceTimeBy(100)

        val afterSave = fakeRepository.getSwimmerById(77L)!!
        assertEquals(58_000L, afterSave.pb100mMillis)
        assertEquals(26_500L, afterSave.pb50mMillis)
        assertEquals(34_000L, afterSave.parseCustomPbsMap()["Brust" to 50])

        // Delete the 100m Freistil session -> 100m Freistil PB should be cleared,
        // while manual 50m Freistil (26.5s) and manual 50m Brust (34.0s) MUST remain intact!
        val sessionId = fakeRepository.allSessionsWithLapsSync().first().session.id
        viewModel.deleteSession(sessionId)
        testScheduler.advanceTimeBy(100)

        val afterDelete = fakeRepository.getSwimmerById(77L)!!
        assertEquals(null, afterDelete.pb100mMillis)
        assertEquals(26_500L, afterDelete.pb50mMillis)
        assertEquals(34_000L, afterDelete.parseCustomPbsMap()["Brust" to 50])

        viewModel.onResetAll()
    }

    @Test
    fun setTargetDistance_clampsIncompatibleSplitDistance() = runTest(testDispatcher) {
        viewModel.setSessionMeta("400m Freistil", "Freistil", 50, 400, 100)
        assertEquals(100, viewModel.uiState.value.splitDistanceMeters)

        // Switching target distance to 50m should automatically clamp splitDistanceMeters from 100m to 50m
        viewModel.setTargetDistance(50)
        assertEquals(50, viewModel.uiState.value.splitDistanceMeters)
    }

    @Test
    fun applyRelayTeamsToStopwatch_configuresLanesAndDistancesCorrectly() = runTest(testDispatcher) {
        val teams = listOf(
            com.example.domain.RelayTeamPlan(
                teamIndex = 0,
                teamTitle = "1. Mannschaft (A-Staffel)",
                legs = listOf(
                    com.example.domain.RelayLegAssignment(1, 1L, "Lukas", "Rücken", 50, 30_000L),
                    com.example.domain.RelayLegAssignment(2, 2L, "Sophie", "Brust", 50, 34_000L),
                    com.example.domain.RelayLegAssignment(3, 3L, "Leon", "Schmetterling", 50, 28_000L),
                    com.example.domain.RelayLegAssignment(4, 4L, "Emma", "Freistil", 50, 26_000L)
                ),
                totalTimeMillis = 118_000L,
                averageLegMillis = 29_500L,
                deltaToFastestMillis = 0L,
                hasEstimatedTimes = false
            )
        )

        viewModel.applyRelayTeamsToStopwatch(
            teams = teams,
            strokeType = "Lagen",
            legDistanceMeters = 50
        )

        val state = viewModel.uiState.value
        assertEquals(200, state.targetDistanceMeters)
        assertEquals(50, state.splitDistanceMeters)
        assertEquals("Lagen", state.strokeType)
        assertEquals(1, state.lanes.size)
        assertEquals("1. Mannschaft (A-Staffel)", state.lanes[0].swimmerName)
        assertEquals(4, state.lanes[0].relayMembers.size)
        assertEquals("Lukas", state.lanes[0].relayMembers[0].swimmerName)
        assertEquals("Emma", state.lanes[0].relayMembers[3].swimmerName)
    }

    @Test
    fun applyRelayTeamsToStopwatch_4x25m_setsPoolLengthAndSplitTo25mAndSavesIndividualSwimmerTimes() = runTest(testDispatcher) {
        val s1 = SwimmerEntity(id = 1L, name = "Lukas", favoriteStroke = "Freistil")
        val s2 = SwimmerEntity(id = 2L, name = "Sophie", favoriteStroke = "Freistil")
        val s3 = SwimmerEntity(id = 3L, name = "Leon", favoriteStroke = "Freistil")
        val s4 = SwimmerEntity(id = 4L, name = "Emma", favoriteStroke = "Freistil")
        fakeRepository.insertSwimmer(s1)
        fakeRepository.insertSwimmer(s2)
        fakeRepository.insertSwimmer(s3)
        fakeRepository.insertSwimmer(s4)

        val teams = listOf(
            com.example.domain.RelayTeamPlan(
                teamIndex = 0,
                teamTitle = "A-Staffel",
                legs = listOf(
                    com.example.domain.RelayLegAssignment(1, 1L, "Lukas", "Freistil", 25, 13_000L),
                    com.example.domain.RelayLegAssignment(2, 2L, "Sophie", "Freistil", 25, 14_000L),
                    com.example.domain.RelayLegAssignment(3, 3L, "Leon", "Freistil", 25, 12_500L),
                    com.example.domain.RelayLegAssignment(4, 4L, "Emma", "Freistil", 25, 13_500L)
                ),
                totalTimeMillis = 53_000L,
                averageLegMillis = 13_250L,
                deltaToFastestMillis = 0L,
                hasEstimatedTimes = false
            )
        )

        // Start with 50m pool length, then apply a 4x25m (100m) Freistil relay
        viewModel.setSessionMeta("Test", "Freistil", 50, 100, 50)
        viewModel.applyRelayTeamsToStopwatch(
            teams = teams,
            strokeType = "Freistil",
            legDistanceMeters = 25
        )

        val state = viewModel.uiState.value
        assertEquals(100, state.targetDistanceMeters)
        assertEquals(25, state.splitDistanceMeters)
        assertEquals(25, state.poolLengthMeters)

        // Swim the 4x25m relay:
        // Leg 1 (Lukas): 12.80s
        // Leg 2 (Sophie): 13.70s (split 26.50s)
        // Leg 3 (Leon): 12.20s (split 38.70s)
        // Leg 4 (Emma): 13.10s (finish 51.80s)
        viewModel.onStartSingleLane(laneNumber = 1)
        fakeClock.currentMillis = 112_800L
        viewModel.onLapLane(laneNumber = 1, exactRealtime = 112_800L)
        fakeClock.currentMillis = 126_500L
        viewModel.onLapLane(laneNumber = 1, exactRealtime = 126_500L)
        fakeClock.currentMillis = 138_700L
        viewModel.onLapLane(laneNumber = 1, exactRealtime = 138_700L)
        fakeClock.currentMillis = 151_800L
        viewModel.onLapLane(laneNumber = 1, exactRealtime = 151_800L)

        val finishedLane = viewModel.uiState.value.lanes[0]
        assertEquals(LaneStatus.FINISHED, finishedLane.status)
        assertEquals(51_800L, finishedLane.finishTimeMillis)

        viewModel.saveCurrentSession()
        testScheduler.advanceTimeBy(200)

        // Verify each lap was saved with the respective swimmer's ID and individual leg time
        val savedSession = fakeRepository.allSessionsWithLapsSync().first()
        assertEquals(4, savedSession.laps.size)
        assertEquals(1L, savedSession.laps[0].swimmerId)
        assertEquals(12_800L, savedSession.laps[0].lapTimeMillis)
        assertEquals(2L, savedSession.laps[1].swimmerId)
        assertEquals(13_700L, savedSession.laps[1].lapTimeMillis)
        assertEquals(3L, savedSession.laps[2].swimmerId)
        assertEquals(12_200L, savedSession.laps[2].lapTimeMillis)
        assertEquals(4L, savedSession.laps[3].swimmerId)
        assertEquals(13_100L, savedSession.laps[3].lapTimeMillis)

        // Verify each swimmer got their own individual 25m PB saved!
        assertEquals(12_800L, fakeRepository.getSwimmerById(1L)?.pb25mMillis)
        assertEquals(13_700L, fakeRepository.getSwimmerById(2L)?.pb25mMillis)
        assertEquals(12_200L, fakeRepository.getSwimmerById(3L)?.pb25mMillis)
        assertEquals(13_100L, fakeRepository.getSwimmerById(4L)?.pb25mMillis)

        viewModel.onResetAll()
    }

    @Test
    fun `applyRelayTeamsToStopwatch automatically disables previously active seriesConfig and sets StartMode MASS`() = runTest(testDispatcher) {
        // First enable Series mode and Interval mode from a previous workout
        viewModel.setSeriesConfig(
            SeriesConfig(isEnabled = true, mode = SeriesModeType.SEND_OFF, sendOffDurationSeconds = 90, maxSeriesCount = 8)
        )
        viewModel.setStartMode(StartMode.INTERVAL)
        assertEquals(true, viewModel.uiState.value.seriesConfig.isEnabled)
        assertEquals(StartMode.INTERVAL, viewModel.uiState.value.startMode)

        val teams = listOf(
            com.example.domain.RelayTeamPlan(
                teamIndex = 0,
                teamTitle = "1. Staffel",
                legs = listOf(
                    com.example.domain.RelayLegAssignment(1, 1L, "Lukas", "Rücken", 50, 30_000L, false, "PB"),
                    com.example.domain.RelayLegAssignment(2, 2L, "Sophie", "Brust", 50, 35_000L, false, "PB"),
                    com.example.domain.RelayLegAssignment(3, 3L, "Leon", "Schmetterling", 50, 29_000L, false, "PB"),
                    com.example.domain.RelayLegAssignment(4, 4L, "Emma", "Freistil", 50, 27_000L, false, "PB")
                ),
                totalTimeMillis = 121_000L,
                averageLegMillis = 30_250L,
                deltaToFastestMillis = 0L,
                hasEstimatedTimes = false
            )
        )

        viewModel.applyRelayTeamsToStopwatch(
            teams = teams,
            strokeType = "Lagen",
            legDistanceMeters = 50
        )

        val state = viewModel.uiState.value
        assertEquals(false, state.seriesConfig.isEnabled)
        assertEquals(StartMode.MASS, state.startMode)
        assertEquals(200, state.targetDistanceMeters)
        assertEquals(50, state.splitDistanceMeters)
        assertEquals(50, state.poolLengthMeters)
    }

    @Test
    fun `manual relay mode in configureStopwatchSettings and assignRelayTeamToLane saves individual swimmer times`() = runTest(testDispatcher) {
        fakeRepository.insertSwimmer(SwimmerEntity(id = 10L, name = "Max"))
        fakeRepository.insertSwimmer(SwimmerEntity(id = 11L, name = "Ben"))
        fakeRepository.insertSwimmer(SwimmerEntity(id = 12L, name = "Clara"))
        fakeRepository.insertSwimmer(SwimmerEntity(id = 13L, name = "Nina"))
        testScheduler.advanceTimeBy(100)

        // Enable Series and Interval first to verify manual relay mode turns them off
        viewModel.setSeriesConfig(
            SeriesConfig(isEnabled = true, mode = SeriesModeType.FIXED_REST, restDurationSeconds = 30)
        )
        viewModel.setStartMode(StartMode.INTERVAL)

        // Manually switch to Relay Mode (4x50m Lagen) in settings
        viewModel.configureStopwatchSettings(
            mode = StartMode.INTERVAL,
            intervalSec = 5,
            poolLength = 50,
            distance = 200,
            splitDistance = 50,
            stroke = "Lagen",
            seriesConfig = SeriesConfig(isEnabled = true, mode = SeriesModeType.FIXED_REST, restDurationSeconds = 30),
            isRelayMode = true
        )

        val stateAfterConfig = viewModel.uiState.value
        assertEquals(false, stateAfterConfig.seriesConfig.isEnabled)
        assertEquals(StartMode.MASS, stateAfterConfig.startMode)
        assertEquals(4, stateAfterConfig.lanes[0].relayMembers.size)
        assertEquals("Rücken", stateAfterConfig.lanes[0].relayMembers[0].stroke)
        assertEquals("Brust", stateAfterConfig.lanes[0].relayMembers[1].stroke)
        assertEquals("Schmetterling", stateAfterConfig.lanes[0].relayMembers[2].stroke)
        assertEquals("Freistil", stateAfterConfig.lanes[0].relayMembers[3].stroke)

        // Manually assign the 4 starters to Lane 1
        viewModel.assignRelayTeamToLane(
            laneNumber = 1,
            teamName = "Vereinsstaffel 1",
            members = listOf(
                com.example.domain.RelayLegAssignment(legPosition = 1, swimmerId = 10L, swimmerName = "Max", stroke = "Rücken", distanceMeters = 50, timeMillis = null),
                com.example.domain.RelayLegAssignment(legPosition = 2, swimmerId = 11L, swimmerName = "Ben", stroke = "Brust", distanceMeters = 50, timeMillis = null),
                com.example.domain.RelayLegAssignment(legPosition = 3, swimmerId = 12L, swimmerName = "Clara", stroke = "Schmetterling", distanceMeters = 50, timeMillis = null),
                com.example.domain.RelayLegAssignment(legPosition = 4, swimmerId = 13L, swimmerName = "Nina", stroke = "Freistil", distanceMeters = 50, timeMillis = null)
            )
        )

        // Swim the 4x50m Lagen relay
        viewModel.onStartSingleLane(laneNumber = 1)
        fakeClock.currentMillis = 131_000L
        viewModel.onLapLane(laneNumber = 1, exactRealtime = 131_000L) // Max (Rücken): 31.0s
        fakeClock.currentMillis = 167_000L
        viewModel.onLapLane(laneNumber = 1, exactRealtime = 167_000L) // Ben (Brust): 36.0s
        fakeClock.currentMillis = 197_000L
        viewModel.onLapLane(laneNumber = 1, exactRealtime = 197_000L) // Clara (Schmetterling): 30.0s
        fakeClock.currentMillis = 224_500L
        viewModel.onLapLane(laneNumber = 1, exactRealtime = 224_500L) // Nina (Freistil): 27.5s

        viewModel.saveCurrentSession()
        testScheduler.advanceTimeBy(200)

        // Verify stroke-specific 50m PBs were saved for each individual swimmer
        assertEquals(31_000L, fakeRepository.getSwimmerById(10L)?.parseCustomPbsMap()?.get("Rücken" to 50))
        assertEquals(36_000L, fakeRepository.getSwimmerById(11L)?.parseCustomPbsMap()?.get("Brust" to 50))
        assertEquals(30_000L, fakeRepository.getSwimmerById(12L)?.parseCustomPbsMap()?.get("Schmetterling" to 50))
        assertEquals(27_500L, fakeRepository.getSwimmerById(13L)?.pb50mMillis)

        viewModel.onResetAll()
    }
}
