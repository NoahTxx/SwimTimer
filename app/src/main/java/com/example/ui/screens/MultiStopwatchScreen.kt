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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EdgesensorHigh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.Undo
import com.example.util.ExportHelper
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.AssignSwimmerDialog
import com.example.ui.components.IntervalConfigDialog
import com.example.ui.components.LaneTimerCard
import com.example.util.HapticUtil
import com.example.util.TimeFormatter
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.DeepOceanBackground
import com.example.ui.theme.OceanBorder
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.OceanSurfaceVariant
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
            val lastSaved = uiState.lastSavedSessionWithLaps
            viewModel.clearSavedSuccess()
            if (lastSaved != null) {
                val actionResult = snackbarHostState.showSnackbar(
                    message = msg,
                    actionLabel = "Bericht teilen",
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
                if (actionResult == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    ExportHelper.shareTrainingReport(context, lastSaved)
                }
            } else {
                snackbarHostState.showSnackbar(msg)
            }
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
                    border = BorderStroke(1.dp, Color(0xFF1B2E46)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            enabled = !uiState.hasOpenRun,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isConfigDialogOpen = true }
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
                            val isRelayActive = remember(uiState.lanes) { uiState.lanes.any { it.relayMembers.isNotEmpty() } }
                            val relayCounts = remember(uiState.lanes) {
                                uiState.lanes.filter { it.relayMembers.isNotEmpty() }.map { it.relayMembers.size }.distinct()
                            }
                            val relayHeaderText = remember(isRelayActive, relayCounts, uiState.splitDistanceMeters, uiState.strokeType, uiState.targetDistanceMeters) {
                                if (isRelayActive) {
                                    when {
                                        relayCounts.size == 1 -> {
                                            val c = relayCounts.first()
                                            "${c}×${uiState.splitDistanceMeters}m ${uiState.strokeType} (${c * uiState.splitDistanceMeters}m)"
                                        }
                                        relayCounts.size > 1 -> {
                                            val minC = relayCounts.min()
                                            val maxC = relayCounts.max()
                                            "${minC}–${maxC}×${uiState.splitDistanceMeters}m ${uiState.strokeType} (${minC * uiState.splitDistanceMeters}–${maxC * uiState.splitDistanceMeters}m)"
                                        }
                                        else -> "4×${uiState.splitDistanceMeters}m ${uiState.strokeType} (${uiState.targetDistanceMeters}m)"
                                    }
                                } else {
                                    "${uiState.targetDistanceMeters}m ${uiState.strokeType}"
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = relayHeaderText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    ),
                                    color = AquaPrimary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
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
                            val seriesLabel = if (uiState.seriesConfig.isEnabled && !isRelayActive) {
                                " • " + (when (uiState.seriesConfig.mode) {
                                    SeriesModeType.MANUAL -> "Serie Manuell"
                                    SeriesModeType.FIXED_REST -> "Pause ${uiState.seriesConfig.restDurationSeconds}s"
                                    SeriesModeType.SEND_OFF -> "Abgang ${TimeFormatter.formatSeconds(uiState.seriesConfig.sendOffDurationSeconds.toLong())}"
                                } + if (uiState.seriesConfig.maxSeriesCount != null) " (${uiState.seriesConfig.maxSeriesCount}x)" else "")
                            } else ""

                            val startLabel = if (uiState.startMode == StartMode.INTERVAL) "Intervall (+${uiState.intervalOffsetSeconds}s)" else "Massenstart"

                            Text(
                                text = when {
                                    uiState.strokeType == "Lagen" && isRelayActive ->
                                        "Staffel-Wechsel alle ${uiState.splitDistanceMeters}m • 1. Rücken • 2. Brust • 3. Schmett. • 4. Freistil"
                                    uiState.strokeType == "Lagen" ->
                                        "Splits alle ${uiState.splitDistanceMeters}m$seriesLabel • 1. Schmett. • 2. Rücken • 3. Brust • 4. Freistil"
                                    isRelayActive ->
                                        "Staffel-Modus • Wechsel alle ${uiState.splitDistanceMeters}m • $startLabel"
                                    else ->
                                        "Splits alle ${uiState.splitDistanceMeters}m • $startLabel$seriesLabel"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                                color = if (uiState.strokeType == "Lagen" || isRelayActive) Color(0xFFFFD600) else Color.LightGray,
                                maxLines = 2,
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

                Spacer(modifier = Modifier.width(8.dp))

                // Square Toggle Button for Hardware Volume Keys Mode
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (uiState.isHardwareKeysEnabled) AquaPrimary else OceanSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (uiState.isHardwareKeysEnabled) AquaPrimary else Color(0xFF1B2E46)
                    ),
                    modifier = Modifier
                        .size(54.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            HapticUtil.vibrateClick(context)
                            viewModel.toggleHardwareKeys()
                        }
                        .testTag("hardware_keys_toggle_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.EdgesensorHigh,
                            contentDescription = if (uiState.isHardwareKeysEnabled) "Hardware-Tasten aktiv" else "Hardware-Tasten inaktiv",
                            tint = if (uiState.isHardwareKeysEnabled) Color.Black else Color.LightGray,
                            modifier = Modifier.size(24.dp)
                        )
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
                    val onAssignRelayLegClick = remember(laneNum) { { legPos: Int -> viewModel.openAssignSwimmerDialog(laneNum, legPos) } }
                    val onRemoveLaneClick = remember(laneNum, context) { { HapticUtil.vibrateClick(context); viewModel.removeLane(laneNum) } }
                    val onStartSingleLaneClick = remember(laneNum, context) { { HapticUtil.vibrateHeavy(context); viewModel.onStartSingleLane(laneNum) } }
                    val onPauseClick = remember(laneNum, context) { { HapticUtil.vibrateClick(context); viewModel.onPauseLane(laneNum) } }
                    val onResumeClick = remember(laneNum, context) { { HapticUtil.vibrateClick(context); viewModel.onResumeLane(laneNum) } }

                    LaneTimerCard(
                        lane = lane,
                        onLapClick = onLapClick,
                        onFinishClick = onFinishClick,
                        onAssignSwimmerClick = onAssignSwimmerClick,
                        onAssignRelayLegClick = onAssignRelayLegClick,
                        isSeriesEnabled = uiState.seriesConfig.isEnabled,
                        maxSeriesCount = uiState.seriesConfig.maxSeriesCount,
                        seriesMode = uiState.seriesConfig.mode,
                        sendOffDurationSec = uiState.seriesConfig.sendOffDurationSeconds,
                        onRemoveLaneClick = onRemoveLaneClick,
                        onStartSingleLaneClick = onStartSingleLaneClick,
                        onPauseClick = onPauseClick,
                        onResumeClick = onResumeClick
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
                        (it.currentElapsedMillis > 0L) || (it.allLaps.isNotEmpty()) || (it.status == LaneStatus.FINISHED)
                    }
                }
            }
            val anyLapsRecorded by remember(uiState.lanes) {
                androidx.compose.runtime.derivedStateOf {
                    uiState.lanes.any { it.allLaps.isNotEmpty() }
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
                        CenteredConfirmDialog(
                            title = "Stoppuhr zurücksetzen?",
                            message = "Die erfassten Zeiten wurden noch nicht gespeichert. Möchtest du wirklich alle Bahnen zurücksetzen?",
                            confirmButtonText = "Zurücksetzen",
                            confirmButtonColor = Color(0xFFFF5252),
                            icon = Icons.Default.Refresh,
                            iconTint = Color(0xFFFF5252),
                            onConfirm = {
                                HapticUtil.vibrateHeavy(context)
                                viewModel.onResetAll()
                                isResetConfirmOpen = false
                            },
                            onDismiss = { isResetConfirmOpen = false }
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

                var isNewRunConfirmOpen by remember { mutableStateOf(value = false) }

                if (isNewRunConfirmOpen) {
                    CenteredConfirmDialog(
                        title = "Neuen Lauf starten?",
                        message = "Die erfassten Zeiten des vorherigen Laufs wurden noch nicht gespeichert. Möchtest du wirklich einen neuen Lauf starten und die alten Zeiten verwerfen?",
                        confirmButtonText = "Neu starten",
                        confirmButtonColor = Color(0xFFFF5252),
                        icon = Icons.Default.PlayArrow,
                        iconTint = Color(0xFFFF5252),
                        onConfirm = {
                            HapticUtil.vibrateHeavy(context)
                            viewModel.onStartAll()
                            isNewRunConfirmOpen = false
                        },
                        onDismiss = { isNewRunConfirmOpen = false }
                    )
                }

                // Center: LARGER START / STOP BUTTON IN THE MIDDLE
                if (!anyRunning) {
                    Button(
                        onClick = {
                            if (anyLapsRecorded && !uiState.isCurrentRunSaved && !uiState.seriesConfig.isEnabled) {
                                isNewRunConfirmOpen = true
                            } else {
                                HapticUtil.vibrateHeavy(context)
                                viewModel.onStartAll()
                            }
                        },
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
            val isRelayActive = remember(uiState.lanes) { uiState.lanes.any { it.relayMembers.isNotEmpty() } }
            val currentRelayCount = remember(uiState.lanes) {
                uiState.lanes.firstOrNull { it.relayMembers.isNotEmpty() }?.relayMembers?.size ?: 4
            }
            IntervalConfigDialog(
                currentStartMode = uiState.startMode,
                currentIntervalSec = uiState.intervalOffsetSeconds,
                currentPoolLength = uiState.poolLengthMeters,
                currentDistance = uiState.targetDistanceMeters,
                currentSplitDistance = uiState.splitDistanceMeters,
                currentStroke = uiState.strokeType,
                currentSeriesConfig = uiState.seriesConfig,
                isRelayActive = isRelayActive,
                currentRelayStarterCount = currentRelayCount,
                onDismiss = { isConfigDialogOpen = false },
                onSaveWithRelayCount = { mode, interval, pool, dist, splitDist, stroke, seriesConfig, relaySelected, starterCount ->
                    viewModel.configureStopwatchSettings(
                        mode = mode,
                        intervalSec = interval,
                        poolLength = pool,
                        distance = dist,
                        splitDistance = splitDist,
                        stroke = stroke,
                        seriesConfig = seriesConfig,
                        isRelayMode = relaySelected,
                        relayStarterCount = starterCount
                    )
                }
            )
        }

        if (uiState.isAssignSwimmerDialogVisible && (uiState.assignSwimmerLane != null)) {
            val laneNum = uiState.assignSwimmerLane!!
            val targetLane = remember(uiState.lanes, laneNum) {
                uiState.lanes.find { it.laneNumber == laneNum }
            }
            val assignedSwimmers = remember(uiState.lanes) {
                buildMap {
                    uiState.lanes.forEach { lane ->
                        lane.swimmerId?.let { sId -> put(sId, lane.laneNumber) }
                        lane.relayMembers.forEach { member ->
                            member.swimmerId?.let { sId -> put(sId, lane.laneNumber) }
                        }
                    }
                }
            }
            AssignSwimmerDialog(
                laneNumber = laneNum,
                swimmers = swimmers,
                assignedSwimmers = assignedSwimmers,
                availableGroups = availableGroups,
                currentLane = targetLane,
                currentStrokeType = uiState.strokeType,
                currentSplitDistanceMeters = uiState.splitDistanceMeters,
                initialRelayLegPosition = uiState.assignSwimmerInitialRelayLeg,
                onDismiss = { viewModel.closeAssignSwimmerDialog() },
                onSelectRelayTeam = { teamName, members ->
                    viewModel.assignRelayTeamToLane(laneNum, teamName, members)
                }
            ) { swimmer -> viewModel.assignSwimmerToLane(laneNum, swimmer) }
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

@Composable
private fun CenteredConfirmDialog(
    title: String,
    message: String,
    confirmButtonText: String,
    confirmButtonColor: Color = Color(0xFFFF5252),
    icon: ImageVector,
    iconTint: Color = Color(0xFFFF5252),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = OceanSurface,
            border = BorderStroke(1.dp, OceanBorder),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered Icon Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(iconTint.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Centered Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Centered Description
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    color = Color(0xFFB0BEC5),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Centered, balanced action buttons with equal weight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = OceanSurfaceVariant,
                            contentColor = Color(0xFFCFD8DC)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Abbrechen",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = confirmButtonColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = confirmButtonText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}