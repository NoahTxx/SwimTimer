package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.LaneStatus
import com.example.ui.SeriesModeType
import com.example.ui.StartMode
import com.example.ui.StopwatchViewModel
import com.example.ui.components.AssignSwimmerDialog
import com.example.ui.components.IntervalConfigDialog
import com.example.ui.components.LaneTimerCard
import com.example.ui.components.StrokeRateCalculatorDialog
import com.example.ui.components.SwimPaceCalculatorDialog
import com.example.util.HapticUtil
import com.example.util.TimeFormatter
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.DeepOceanBackground
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.PaceRed
import com.example.ui.theme.TextWhite

@Composable
fun MultiStopwatchScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val swimmers by viewModel.allSwimmers.collectAsStateWithLifecycle()
    val availableGroups by viewModel.availableGroups.collectAsStateWithLifecycle()

    var isConfigDialogOpen by remember { mutableStateOf(value = false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.lastUndoMessage) {
        uiState.lastUndoMessage?.let { msg ->
            viewModel.clearUndoMessage()
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(uiState.isSessionSavedSuccess) {
        if (uiState.isSessionSavedSuccess) {
            val msg = uiState.saveNotificationMessage ?: "Trainingseinheit erfolgreich gespeichert!"
            viewModel.clearSavedSuccess()
            snackbarHostState.showSnackbar(msg)
        }
    }

    KeepScreenOnEffect(enabled = uiState.hasOpenRun)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOceanBackground),
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // TOP: Stopwatch Settings Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = OceanSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B2E46)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !uiState.hasOpenRun) { isConfigDialogOpen = true }
                        .testTag("session_info_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "${uiState.targetDistanceMeters}m ${uiState.strokeType}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp
                                    ),
                                    color = AquaPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• ${uiState.poolLengthMeters}m Becken",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextWhite,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            val seriesLabel = if (uiState.seriesConfig.isEnabled) {
                                " • " + (when (uiState.seriesConfig.mode) {
                                    SeriesModeType.MANUAL -> "Serie Manuell"
                                    SeriesModeType.FIXED_REST -> "Pause ${uiState.seriesConfig.restDurationSeconds}s"
                                    SeriesModeType.SEND_OFF -> "Abgang ${TimeFormatter.formatSeconds(uiState.seriesConfig.sendOffDurationSeconds.toLong())}"
                                } + if (uiState.seriesConfig.maxSeriesCount != null) " (${uiState.seriesConfig.maxSeriesCount}x)" else "")
                            } else ""

                            val startLabel = if (uiState.startMode == StartMode.INTERVAL) "Intervall (+${uiState.intervalOffsetSeconds}s)" else "Massenstart"

                            Text(
                                text = if (uiState.strokeType == "Lagen") {
                                    "Splits alle ${uiState.splitDistanceMeters}m$seriesLabel • 1. Schmett • 2. Rücken • 3. Brust • 4. Kraul"
                                } else {
                                    "Splits alle ${uiState.splitDistanceMeters}m • $startLabel$seriesLabel"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.strokeType == "Lagen") Color(0xFFFFD600) else Color.LightGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = CircleShape,
                            color = AquaPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Stoppuhr Einstellungen",
                                    tint = AquaPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // MIDDLE: Lanes List (Dynamic up to 15 lanes with centered + button at the bottom)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = uiState.lanes,
                    key = { lane -> lane.laneNumber },
                    contentType = { "LaneTimerCard" }
                ) { lane ->
                    val laneNum = lane.laneNumber
                    val onLapClick = remember(laneNum, context) { { exactTime: Long -> HapticUtil.vibrateClick(context); viewModel.onLapLane(laneNum, exactTime) } }
                    val onFinishClick = remember(laneNum, context) { { exactTime: Long -> HapticUtil.vibrateHeavy(context); viewModel.onFinishLane(laneNum, exactTime) } }
                    val onAssignSwimmerClick = remember(laneNum) { { viewModel.openAssignSwimmerDialog(laneNum) } }
                    val onRemoveLaneClick = remember(laneNum, context) { { HapticUtil.vibrateClick(context); viewModel.removeLane(laneNum) } }
                    val onStartSingleLaneClick = remember(laneNum, context) { { HapticUtil.vibrateHeavy(context); viewModel.onStartSingleLane(laneNum) } }
                    val onPauseClick = remember(laneNum, context) { { HapticUtil.vibrateClick(context); viewModel.onPauseLane(laneNum) } }
                    val onResumeClick = remember(laneNum, context) { { HapticUtil.vibrateClick(context); viewModel.onResumeLane(laneNum) } }
                    val onStrokeRateTap = remember(laneNum) { { viewModel.onTapStrokeRate(laneNum) } }

                    LaneTimerCard(
                        lane = lane,
                        onLapClick = onLapClick,
                        onFinishClick = onFinishClick,
                        onAssignSwimmerClick = onAssignSwimmerClick,
                        isSeriesEnabled = uiState.seriesConfig.isEnabled,
                        maxSeriesCount = uiState.seriesConfig.maxSeriesCount,
                        seriesMode = uiState.seriesConfig.mode,
                        sendOffDurationSec = uiState.seriesConfig.sendOffDurationSeconds,
                        onRemoveLaneClick = onRemoveLaneClick,
                        onStartSingleLaneClick = onStartSingleLaneClick,
                        onPauseClick = onPauseClick,
                        onResumeClick = onResumeClick,
                        onStrokeRateTap = onStrokeRateTap
                    )
                }

                // BOTTOM OF LIST: Centered '+' Button
                item(
                    key = "add_lane_bottom_control",
                    contentType = "AddLaneButton"
                ) {
                    if (uiState.lanes.size < 15) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            FilledIconButton(
                                onClick = { HapticUtil.vibrateClick(context); viewModel.addLane() },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = AquaPrimary,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier
                                    .size(54.dp)
                                    .testTag("add_lane_card_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Schwimmer hinzufügen",
                                    tint = Color.Black,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }

            // BOTTOM MASTER CONTROLS BAR
            val anyRunning = uiState.hasOpenRun
            val anyHasTimes by remember(uiState.lanes) {
                androidx.compose.runtime.derivedStateOf {
                    uiState.lanes.any {
                        (it.currentElapsedMillis > 0L) || (it.laps.isNotEmpty()) || (it.status == LaneStatus.FINISHED)
                    }
                }
            }
            val anyLapsRecorded by remember(uiState.lanes) {
                androidx.compose.runtime.derivedStateOf {
                    uiState.lanes.any { it.laps.isNotEmpty() }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepOceanBackground)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Reset & Undo
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isResetConfirmOpen by remember { mutableStateOf(value = false) }

                    FilledTonalButton(
                        onClick = {
                            if (anyLapsRecorded && !uiState.isCurrentRunSaved) {
                                isResetConfirmOpen = true
                            } else {
                                HapticUtil.vibrateHeavy(context)
                                viewModel.onResetAll()
                            }
                        },
                        enabled = !anyRunning && anyHasTimes,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = OceanSurface,
                            contentColor = AquaPrimary,
                            disabledContainerColor = OceanSurface.copy(alpha = 0.35f),
                            disabledContentColor = Color.DarkGray
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("master_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(22.dp))
                    }

                    if (isResetConfirmOpen) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { isResetConfirmOpen = false },
                            title = { Text("Stoppuhr zurücksetzen?", color = TextWhite, fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    "Die erfassten Zeiten wurden noch nicht gespeichert. Möchtest du wirklich alle Bahnen zurücksetzen?",
                                    color = Color.LightGray
                                )
                            },
                            containerColor = Color(0xFF101B2B),
                            confirmButton = {
                                Button(
                                    onClick = {
                                        HapticUtil.vibrateHeavy(context)
                                        viewModel.onResetAll()
                                        isResetConfirmOpen = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                                ) {
                                    Text("Zurücksetzen", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { isResetConfirmOpen = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                ) {
                                    Text("Abbrechen", color = Color.LightGray)
                                }
                            }
                        )
                    }

                    if (uiState.canUndo) {
                        FilledTonalButton(
                            onClick = { HapticUtil.vibrateClick(context); viewModel.performUndo() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = OceanSurface,
                                contentColor = Color(0xFFFFD600)
                            ),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .size(46.dp)
                                .testTag("undo_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Rückgängig", modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // Center: LARGER START / STOP BUTTON IN THE MIDDLE
                if (!anyRunning) {
                    Button(
                        onClick = { HapticUtil.vibrateHeavy(context); viewModel.onStartAll() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaceGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("master_start_all_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.startMode == StartMode.INTERVAL) "Intervall-Start" else "START ALLE",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Button(
                        onClick = { HapticUtil.vibrateHeavy(context); viewModel.onStopAll() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PaceRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("master_stop_all_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STOPP ALLE",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1
                        )
                    }
                }

                // Right: Speichern / Save (Protected against accidental double-saving)
                Button(
                    onClick = { HapticUtil.vibrateHeavy(context); viewModel.saveCurrentSession() },
                    enabled = anyLapsRecorded && !uiState.isCurrentRunSaved,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isCurrentRunSaved) PaceGreen else AquaPrimary,
                        contentColor = Color.Black,
                        disabledContainerColor = if (uiState.isCurrentRunSaved) PaceGreen.copy(alpha = 0.4f) else AquaPrimary.copy(alpha = 0.25f),
                        disabledContentColor = Color.DarkGray
                    ),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("master_save_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isCurrentRunSaved) Icons.Default.Check else Icons.Default.Save,
                        contentDescription = "Speichern",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Floating notification pill at the top
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) { snackbarData ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = OceanSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.5f)),
                shadowElevation = 8.dp,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PaceGreen,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = snackbarData.visuals.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        ),
                        color = TextWhite,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Dialogs
        if (isConfigDialogOpen) {
            IntervalConfigDialog(
                currentStartMode = uiState.startMode,
                currentIntervalSec = uiState.intervalOffsetSeconds,
                currentPoolLength = uiState.poolLengthMeters,
                currentDistance = uiState.targetDistanceMeters,
                currentSplitDistance = uiState.splitDistanceMeters,
                currentStroke = uiState.strokeType,
                currentSeriesConfig = uiState.seriesConfig,
                onDismiss = { isConfigDialogOpen = false }
            ) { mode, interval, pool, dist, splitDist, stroke, seriesConfig ->
                viewModel.setStartMode(mode)
                viewModel.setIntervalOffsetSeconds(interval)
                viewModel.setSeriesConfig(seriesConfig)
                viewModel.setSessionMeta("${dist}m $stroke", stroke, pool, dist, splitDist)
            }
        }

        if (uiState.isAssignSwimmerDialogVisible && (uiState.assignSwimmerLane != null)) {
            val laneNum = uiState.assignSwimmerLane!!
            val assignedSwimmers = remember(uiState.lanes) {
                uiState.lanes.mapNotNull { lane ->
                    lane.swimmerId?.let { sId -> sId to lane.laneNumber }
                }.toMap()
            }
            AssignSwimmerDialog(
                laneNumber = laneNum,
                swimmers = swimmers,
                assignedSwimmers = assignedSwimmers,
                availableGroups = availableGroups,
                onDismiss = { viewModel.closeAssignSwimmerDialog() },
            ) { swimmer -> viewModel.assignSwimmerToLane(laneNum, swimmer) }
        }

        if (uiState.isStrokeRateDialogVisible) {
            StrokeRateCalculatorDialog(
                targetLane = uiState.strokeRateTargetLane,
                onDismiss = { viewModel.closeStrokeRateDialog() },
                onAttachToLane = { laneNum, spm ->
                    viewModel.attachStrokeRateToLane(laneNum, spm)
                }
            )
        }

        if (uiState.isPaceCalculatorVisible) {
            SwimPaceCalculatorDialog(
                initialDistance = uiState.targetDistanceMeters,
                initialStroke = uiState.strokeType,
                initialSplitDistance = uiState.splitDistanceMeters,
                onDismiss = { viewModel.closePaceCalculator() }
            )
        }
    }
}

@Composable
private fun KeepScreenOnEffect(enabled: Boolean) {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose {
            view.keepScreenOn = false
        }
    }
}