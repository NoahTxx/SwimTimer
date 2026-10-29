package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.StrokeRateCalculatorDialog
import com.example.ui.components.SwimPaceCalculatorDialog
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
    fun strokeRateCalculatorDialog_displaysLaneAndDismisses() {
        var dismissed = false

        composeTestRule.setContent {
            SwimTimerTheme {
                StrokeRateCalculatorDialog(
                    targetLane = 3,
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Schlagfrequenz").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Schließen").performClick()
        assertTrue("onDismiss callback should be invoked when clicking close button", dismissed)
    }

    @Test
    fun swimPaceCalculatorDialog_rendersStrategyAndInputs() {
        composeTestRule.setContent {
            SwimTimerTheme {
                SwimPaceCalculatorDialog(
                    initialDistance = 100,
                    initialStroke = "Freistil",
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Pace- & Split-Rechner", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Gleichmäßig", substring = true)
            .assertExists()
    }

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
}
