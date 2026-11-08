package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.SwimmerEntity
import com.example.ui.screens.MultiStopwatchScreen
import com.example.ui.theme.SwimTimerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ComposeUiBehaviorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun verifyStateRestoration_preservesComponentState() {
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            var selectedDistance by rememberSaveable { mutableStateOf(100) }
            SwimTimerTheme {
                androidx.compose.material3.Button(onClick = { selectedDistance = 200 }) {
                    androidx.compose.material3.Text("Distance: $selectedDistance m")
                }
            }
        }

        // Verify initial state
        composeTestRule.onNodeWithText("Distance: 100 m").assertIsDisplayed()

        // Modify state
        composeTestRule.onNodeWithText("Distance: 100 m").performClick()
        composeTestRule.onNodeWithText("Distance: 200 m").assertIsDisplayed()

        // Trigger recreation
        restorationTester.emulateSavedInstanceStateRestore()

        // Verify state was restored
        composeTestRule.onNodeWithText("Distance: 200 m").assertIsDisplayed()
    }

    @Test
    fun navigation_tabSwitchingBetweenScreens() {
        val fakeRepo = com.example.data.repository.FakeSwimRepository()
        val viewModel = StopwatchViewModel(fakeRepo, null, com.example.util.FakeClock())

        composeTestRule.setContent {
            SwimTimerTheme {
                com.example.MainAppContent(viewModel = viewModel)
            }
        }

        // Bottom bar items are displayed and can navigate
        composeTestRule.onNodeWithText("Stoppuhr").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tools").performClick()
        composeTestRule.onNodeWithText("Athleten").performClick()
        composeTestRule.onNodeWithText("Historie").performClick()
        composeTestRule.onNodeWithText("Stoppuhr").performClick()
    }

    @Test
    fun intervalConfigDialog_relaySwitchTogglesAndSavesRelayMode() {
        var savedRelayMode = false
        var savedDistance = 0

        composeTestRule.setContent {
            SwimTimerTheme {
                com.example.ui.components.IntervalConfigDialog(
                    currentStartMode = StartMode.MASS,
                    currentIntervalSec = 5,
                    currentPoolLength = 25,
                    currentDistance = 100,
                    currentSplitDistance = 50,
                    currentStroke = "Freistil",
                    isRelayActive = false,
                    onDismiss = {},
                    onSaveWithRelay = { _, _, _, dist, _, _, _, isRelay ->
                        savedRelayMode = isRelay
                        savedDistance = dist
                    }
                )
            }
        }

        composeTestRule.onNode(androidx.compose.ui.test.hasTestTag("config_mode_relay"))
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("4×50m")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("Übernehmen")
            .performScrollTo()
            .performClick()

        assertTrue("Relay mode should be enabled via switch", savedRelayMode)
        org.junit.Assert.assertEquals(200, savedDistance)
    }

    @Test
    fun laneTimerCard_relayModeDisplaysWechselButton() {
        val relayLane = LaneState(
            laneNumber = 1,
            status = LaneStatus.RUNNING,
            swimmerName = "Staffel Bahn 1",
            targetDistanceMeters = 200,
            splitDistanceMeters = 50,
            laneStartTimeRealtime = null,
            currentElapsedMillis = 12340L,
            relayMembers = listOf(
                com.example.domain.RelayLegAssignment(1, 1L, "Starter 1", "Freistil", 50, null),
                com.example.domain.RelayLegAssignment(2, 2L, "Starter 2", "Freistil", 50, null),
                com.example.domain.RelayLegAssignment(3, 3L, "Starter 3", "Freistil", 50, null),
                com.example.domain.RelayLegAssignment(4, 4L, "Starter 4", "Freistil", 50, null),
            )
        )

        composeTestRule.setContent {
            SwimTimerTheme {
                com.example.ui.components.LaneTimerCard(
                    lane = relayLane,
                    onLapClick = {},
                    onFinishClick = {},
                    onAssignSwimmerClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Wechsel 1→2").assertExists()
    }

    @Test
    fun intervalConfigDialog_hidesSplitSelectionInRelayMode() {
        composeTestRule.setContent {
            SwimTimerTheme {
                com.example.ui.components.IntervalConfigDialog(
                    currentStartMode = com.example.ui.StartMode.MASS,
                    currentIntervalSec = 5,
                    currentPoolLength = 50,
                    currentDistance = 200,
                    currentSplitDistance = 50,
                    currentStroke = "Freistil",
                    currentSeriesConfig = com.example.ui.SeriesConfig(),
                    isRelayActive = true,
                    currentRelayStarterCount = 4,
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // When relay mode is enabled, the redundant split section should not exist
        composeTestRule.onNodeWithText("Zwischenzeiten (Splits alle):").assertDoesNotExist()
        composeTestRule.onNodeWithText("Staffel-Wechsel (pro Starter):").assertDoesNotExist()
    }

    @Test
    fun assignSwimmerDialog_relayModeRendersStarterBubblesAndSwimmerList() {
        val testSwimmers = listOf(
            SwimmerEntity(id = 1, name = "Lukas Weber", favoriteStroke = "Freistil", pb50mMillis = 26400L),
            SwimmerEntity(id = 2, name = "Sophie Müller", favoriteStroke = "Brust", pb50mMillis = 33200L)
        )

        composeTestRule.setContent {
            SwimTimerTheme {
                com.example.ui.components.AssignSwimmerDialog(
                    laneNumber = 1,
                    currentLane = LaneState(laneNumber = 1, swimmerName = "Bahn 1"),
                    swimmers = testSwimmers,
                    currentStrokeType = "Freistil",
                    currentSplitDistanceMeters = 50,
                    initialRelayLegPosition = 1,
                    onDismiss = {},
                    onSelectRelayTeam = { _, _ -> },
                    onSelectSwimmer = {}
                )
            }
        }

        // Check that relay starter slot tags are rendered
        composeTestRule.onNodeWithText("1. Starter").assertExists()
        composeTestRule.onNodeWithText("4. Schluss ", substring = true).assertExists()
        // Check that swimmer item is rendered in the list
        composeTestRule.onNodeWithText("Lukas Weber").assertExists()

        // Check stepper displays 4 Starter and 200m gesamt
        composeTestRule.onNodeWithText("4 Starter").assertExists()
        composeTestRule.onNodeWithText("200m gesamt").assertExists()

        // Increment to 5 starters
        composeTestRule.onNodeWithTag("assign_relay_inc_starters").performClick()
        composeTestRule.onNodeWithText("5 Starter").assertExists()
        composeTestRule.onNodeWithText("250m gesamt").assertExists()

        // Decrement down to 3 starters
        composeTestRule.onNodeWithTag("assign_relay_dec_starters").performClick()
        composeTestRule.onNodeWithTag("assign_relay_dec_starters").performClick()
        composeTestRule.onNodeWithText("3 Starter").assertExists()
        composeTestRule.onNodeWithText("150m gesamt").assertExists()
    }

    @Test
    fun multiStopwatchScreen_hardwareKeysToggleButton_togglesState() {
        val fakeRepo = com.example.data.repository.FakeSwimRepository()
        val viewModel = StopwatchViewModel(
            repository = fakeRepo,
            soundPlayer = com.example.util.NoOpSoundPlayer()
        )

        composeTestRule.setContent {
            SwimTimerTheme {
                MultiStopwatchScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("hardware_keys_toggle_button").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Hardware-Tasten inaktiv").assertExists()

        composeTestRule.onNodeWithTag("hardware_keys_toggle_button").performClick()

        assertTrue(viewModel.uiState.value.isHardwareKeysEnabled)
        composeTestRule.onNodeWithContentDescription("Hardware-Tasten aktiv").assertExists()
    }

    @Test
    fun exportHelper_generateTrainingReport_formatsReportCorrectly() {
        val session = com.example.data.model.SessionEntity(
            id = 1L,
            timestamp = 1730372400000L,
            title = "100m Freistil Test",
            poolLengthMeters = 50,
            targetDistanceMeters = 100,
            strokeType = "Freistil",
            startMode = "MASS",
            intervalOffsetSeconds = 5,
            laneCount = 2,
            splitDistanceMeters = 50
        )
        val laps = listOf(
            com.example.data.model.LapRecordEntity(
                sessionId = 1L,
                swimmerName = "Max Mustermann",
                laneNumber = 1,
                lapIndex = 1,
                splitTimeMillis = 28000L,
                lapTimeMillis = 28000L
            ),
            com.example.data.model.LapRecordEntity(
                sessionId = 1L,
                swimmerName = "Max Mustermann",
                laneNumber = 1,
                lapIndex = 2,
                splitTimeMillis = 58500L,
                lapTimeMillis = 30500L
            )
        )
        val report = com.example.util.ExportHelper.generateTrainingReport(
            com.example.data.model.SessionWithLaps(session = session, laps = laps)
        )

        assertTrue(report.contains("SWIMTIMER – TRAININGSBERICHT"))
        assertTrue(report.contains("Max Mustermann"))
        assertTrue(report.contains("0:58.50"))
    }

    @Test
    fun intervalConfigDialog_adjustIntervalWithPlusAndMinusButtons() {
        var savedIntervalSec = 0

        composeTestRule.setContent {
            SwimTimerTheme {
                com.example.ui.components.IntervalConfigDialog(
                    currentStartMode = StartMode.INTERVAL,
                    currentIntervalSec = 5,
                    currentPoolLength = 50,
                    currentDistance = 100,
                    currentSplitDistance = 50,
                    currentStroke = "Freistil",
                    onDismiss = {},
                    onSave = { _, intervalSec, _, _, _, _, _ ->
                        savedIntervalSec = intervalSec
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("interval_sec_display", useUnmergedTree = true).assertExists()

        // Click plus button twice: 5 -> 6 -> 7
        composeTestRule.onNodeWithTag("interval_sec_inc", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithTag("interval_sec_inc", useUnmergedTree = true).performClick()

        // Click minus button once: 7 -> 6
        composeTestRule.onNodeWithTag("interval_sec_dec", useUnmergedTree = true).performClick()

        // Click preset chip 15s -> 15s
        composeTestRule.onNodeWithText("15s").performClick()

        // Plus button once: 15 -> 16
        composeTestRule.onNodeWithTag("interval_sec_inc", useUnmergedTree = true).performClick()

        // Save
        composeTestRule.onNodeWithText("Übernehmen")
            .performScrollTo()
            .performClick()

        assertEquals(16, savedIntervalSec)
    }

    @Test
    fun editSessionDialog_touchSelectionAndDuplicatePrevention() {
        var savedTitle = ""
        var savedAssignments: Map<Int, Pair<Long?, String>> = emptyMap()

        val session = com.example.data.model.SessionEntity(
            id = 1L,
            title = "100m Brust",
            timestamp = 1730000000000L,
            targetDistanceMeters = 100,
            strokeType = "Brust"
        )
        val laps = listOf(
            com.example.data.model.LapRecordEntity(
                id = 10L,
                sessionId = 1L,
                swimmerName = "Bahn 1",
                laneNumber = 1,
                lapIndex = 1,
                splitTimeMillis = 35000L,
                lapTimeMillis = 35000L
            ),
            com.example.data.model.LapRecordEntity(
                id = 11L,
                sessionId = 1L,
                swimmerName = "Bahn 2",
                laneNumber = 2,
                lapIndex = 1,
                splitTimeMillis = 37000L,
                lapTimeMillis = 37000L
            )
        )
        val sessionWithLaps = com.example.data.model.SessionWithLaps(session, laps)
        val swimmers = listOf(
            SwimmerEntity(id = 1L, name = "Sophie", groupName = "Wettkampf 1"),
            SwimmerEntity(id = 2L, name = "Lukas", groupName = "Wettkampf 1"),
            SwimmerEntity(id = 3L, name = "Mia", groupName = "Nachwuchs")
        )

        composeTestRule.setContent {
            SwimTimerTheme {
                com.example.ui.screens.EditSessionDialog(
                    sessionWithLaps = sessionWithLaps,
                    swimmers = swimmers,
                    onDismiss = {},
                    onSave = { newTitle, laneMap, _ ->
                        savedTitle = newTitle
                        savedAssignments = laneMap
                    }
                )
            }
        }

        // Dialog elements exist
        composeTestRule.onNodeWithTag("edit_session_dialog").assertExists()
        composeTestRule.onNodeWithText("Trainingseinheit bearbeiten").assertExists()
        composeTestRule.onNodeWithTag("edit_session_title_field").assertExists()
        composeTestRule.onNodeWithTag("pick_swimmer_btn_1").assertExists()
        composeTestRule.onNodeWithTag("pick_swimmer_btn_2").assertExists()

        // 1. Open picker for Bahn 1 and select Sophie
        composeTestRule.onNodeWithTag("pick_swimmer_btn_1").performClick()
        composeTestRule.onNodeWithTag("swimmer_picker_item_1").performClick()

        // Verify Bahn 1 now displays Sophie
        composeTestRule.onNodeWithTag("pick_swimmer_btn_1").assertExists()

        // 2. Open picker for Bahn 2 - Sophie is already assigned to Bahn 1 and must be disabled/marked as BELEGT
        composeTestRule.onNodeWithTag("pick_swimmer_btn_2").performClick()
        composeTestRule.onNodeWithText("BELEGT").assertExists()
        composeTestRule.onNodeWithText("Bereits auf Bahn 1 eingeteilt").assertExists()

        // Clicking on Sophie must NOT assign her to Bahn 2 (clickable is disabled)
        composeTestRule.onNodeWithTag("swimmer_picker_item_1").performClick()
        // The modal picker should still be open because the click was ignored
        composeTestRule.onNodeWithText("Athlet für Bahn 2 wählen").assertExists()

        // Instead, pick Lukas
        composeTestRule.onNodeWithTag("swimmer_picker_item_2").performClick()

        // Test clear button on Bahn 2
        composeTestRule.onNodeWithTag("clear_lane_btn_2").performClick()

        // Save
        composeTestRule.onNodeWithTag("edit_session_save_btn")
            .performScrollTo()
            .performClick()

        assertEquals("100m Brust", savedTitle)
        assertEquals(Pair(1L, "Sophie"), savedAssignments[1])
        assertEquals(Pair(null, "Bahn 2"), savedAssignments[2])
    }

    @Test
    fun editSessionDialog_relaySession_rendersAllStarterCardsAndAllowsAssigningLegs() {
        var savedTitle = ""
        var savedAssignments = mapOf<Int, Pair<Long?, String>>()
        var savedLegAssignments = mapOf<Pair<Int, Int>, Pair<Long?, String>>()

        val session = com.example.data.model.SessionEntity(
            id = 200L,
            title = "4x50m Freistil Staffel",
            targetDistanceMeters = 200,
            splitDistanceMeters = 50,
            strokeType = "Freistil"
        )
        val laps = listOf(
            com.example.data.model.LapRecordEntity(sessionId = 200L, swimmerId = null, swimmerName = "Staffel Bahn 1 • 1. Starter 1", laneNumber = 1, lapIndex = 1, splitTimeMillis = 30_000L, lapTimeMillis = 30_000L),
            com.example.data.model.LapRecordEntity(sessionId = 200L, swimmerId = null, swimmerName = "Staffel Bahn 1 • 2. Starter 2", laneNumber = 1, lapIndex = 2, splitTimeMillis = 61_000L, lapTimeMillis = 31_000L),
            com.example.data.model.LapRecordEntity(sessionId = 200L, swimmerId = null, swimmerName = "Staffel Bahn 1 • 3. Starter 3", laneNumber = 1, lapIndex = 3, splitTimeMillis = 93_000L, lapTimeMillis = 32_000L),
            com.example.data.model.LapRecordEntity(sessionId = 200L, swimmerId = null, swimmerName = "Staffel Bahn 1 • 4. Starter 4", laneNumber = 1, lapIndex = 4, splitTimeMillis = 126_000L, lapTimeMillis = 33_000L)
        )
        val sessionWithLaps = com.example.data.model.SessionWithLaps(session, laps)
        val swimmers = listOf(
            SwimmerEntity(id = 10L, name = "Lukas", groupName = "Wettkampf 1"),
            SwimmerEntity(id = 20L, name = "Sophie", groupName = "Wettkampf 1")
        )

        composeTestRule.setContent {
            SwimTimerTheme {
                com.example.ui.screens.EditSessionDialog(
                    sessionWithLaps = sessionWithLaps,
                    swimmers = swimmers,
                    onDismiss = {},
                    onSave = { newTitle, laneMap, legMap ->
                        savedTitle = newTitle
                        savedAssignments = laneMap
                        savedLegAssignments = legMap
                    }
                )
            }
        }

        // Verify relay starter picking button exists for Leg 1 and Leg 2
        composeTestRule.onNodeWithTag("pick_relay_swimmer_1_1").performScrollTo().assertExists()
        composeTestRule.onNodeWithTag("pick_relay_swimmer_1_2").performScrollTo().assertExists()

        // Click on Leg 1 to open picker and assign Lukas
        composeTestRule.onNodeWithTag("pick_relay_swimmer_1_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("swimmer_picker_item_10").performClick()
        composeTestRule.waitForIdle()

        // Click on Leg 2 to open picker and assign Sophie
        composeTestRule.onNodeWithTag("pick_relay_swimmer_1_2").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("swimmer_picker_item_20").performClick()
        composeTestRule.waitForIdle()

        // Save
        composeTestRule.onNodeWithTag("edit_session_save_btn")
            .performScrollTo()
            .performClick()

        assertEquals("4x50m Freistil Staffel", savedTitle)
        assertEquals("Staffel Bahn 1", savedAssignments[1]?.second)
        assertEquals(Pair(10L, "Lukas"), savedLegAssignments[Pair(1, 1)])
        assertEquals(Pair(20L, "Sophie"), savedLegAssignments[Pair(1, 2)])
    }

    @Test
    fun assignSwimmerDialog_strokeAndDistanceResolutionAndPersistentGroup() {
        var selectedGroup: String? = null
        var chosenSwimmer: SwimmerEntity? = null

        // Sophie: favorite Freistil 50m: 30.00s, Custom PB Rücken 50m: 35.00s (35000L)
        val sophie = SwimmerEntity(
            id = 1L,
            name = "Sophie",
            favoriteStroke = "Freistil",
            pb50mMillis = 30000L,
            customPbsJson = """{"Rücken_50":35000}""",
            groupName = "Wettkampf"
        )
        val swimmers = listOf(sophie)

        composeTestRule.setContent {
            SwimTimerTheme {
                com.example.ui.components.AssignSwimmerDialog(
                    laneNumber = 1,
                    swimmers = swimmers,
                    currentLane = LaneState(laneNumber = 1, swimmerName = "Bahn 1", targetDistanceMeters = 50),
                    currentStrokeType = "Rücken",
                    currentSplitDistanceMeters = 50,
                    selectedGroupFilter = "Wettkampf",
                    onSelectGroupFilter = { selectedGroup = it },
                    onDismiss = {},
                    onSelectSwimmer = { chosenSwimmer = it }
                )
            }
        }

        // Must display 50m Rücken time (35.00), NEVER 50m Freistil (30.00)
        composeTestRule.onNodeWithText("50m Rücken").assertExists()
        composeTestRule.onNodeWithText("35.00").assertExists()
        composeTestRule.onNodeWithText("30.00").assertDoesNotExist()

        // Click on "Alle" chip to reset group filter
        composeTestRule.onNodeWithText("Alle").performClick()
        assertNull(selectedGroup)

        // Select swimmer
        composeTestRule.onNodeWithText("Sophie").performClick()
        assertEquals(sophie, chosenSwimmer)
    }
}


