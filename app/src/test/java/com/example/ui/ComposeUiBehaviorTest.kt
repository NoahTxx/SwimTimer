package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.SwimmerEntity
import com.example.ui.screens.MultiStopwatchScreen
import com.example.ui.theme.SwimTimerTheme
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
}


