package com.example

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AppDatabase
import com.example.data.repository.SwimRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.StopwatchViewModel
import com.example.ui.StopwatchViewModelFactory
import com.example.ui.screens.CoachingToolsScreen
import com.example.ui.screens.MultiStopwatchScreen
import com.example.ui.screens.SessionsHistoryScreen
import com.example.ui.screens.SwimmersManagementScreen
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.DeepOceanBackground
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.SwimTimerTheme

enum class AppScreen {
    STOPWATCH,
    HISTORY,
    SWIMMERS,
    TOOLS
}

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getInstance(applicationContext) }
    private val repository by lazy { SwimRepository(database) }
    private val preferencesRepository by lazy { UserPreferencesRepository(applicationContext) }
    private val viewModel: StopwatchViewModel by viewModels {
        StopwatchViewModelFactory(repository, preferencesRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep screen on while coaching at the pool
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.keepScreenOn = true

        // Force maximum display refresh rate (120Hz / 144Hz / 90Hz)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val displayModes = display?.supportedModes
                val maxRefreshMode = displayModes?.maxByOrNull { it.refreshRate }
                if (maxRefreshMode != null) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = maxRefreshMode.modeId
                    }
                }
            } catch (_: Exception) {}
        }

        setContent {
            SwimTimerTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: StopwatchViewModel) {
    var currentScreen by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(AppScreen.STOPWATCH) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    androidx.compose.runtime.LaunchedEffect(currentScreen) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    BackHandler(enabled = currentScreen != AppScreen.STOPWATCH) {
        currentScreen = AppScreen.STOPWATCH
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepOceanBackground)
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
            },
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (currentScreen) {
                AppScreen.STOPWATCH -> MultiStopwatchScreen(viewModel = viewModel)
                AppScreen.HISTORY -> SessionsHistoryScreen(viewModel = viewModel)
                AppScreen.SWIMMERS -> SwimmersManagementScreen(viewModel = viewModel, isActiveTab = true)
                AppScreen.TOOLS -> CoachingToolsScreen(
                    viewModel = viewModel,
                    onNavigateToStopwatch = { currentScreen = AppScreen.STOPWATCH }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = OceanSurface,
                border = BorderStroke(1.dp, Color(0xFF1B2E46)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val screens = listOf(
                        Triple(AppScreen.STOPWATCH, "Stoppuhr", Icons.Default.Timer),
                        Triple(AppScreen.HISTORY, "Historie", Icons.Default.History),
                        Triple(AppScreen.SWIMMERS, "Athleten", Icons.Default.People),
                        Triple(AppScreen.TOOLS, "Tools", Icons.Default.Calculate)
                    )

                    screens.forEach { (screen, label, icon) ->
                        val isSelected = currentScreen == screen
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) AquaPrimary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { currentScreen = screen }
                                .testTag("nav_${label.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) Color.Black else Color.LightGray,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (isSelected) Color.Black else Color.LightGray,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}