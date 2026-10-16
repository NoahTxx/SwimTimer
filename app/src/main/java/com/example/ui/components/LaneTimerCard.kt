package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.withFrameMillis
import android.os.SystemClock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LaneState
import com.example.ui.LaneStatus
import com.example.ui.LapItem
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.LaneColors
import com.example.ui.theme.OceanBorder
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.PaceRed
import com.example.ui.theme.TextWhite
import com.example.ui.theme.PaceNeutral
import com.example.util.TimeFormatter

private val LAGEN_STROKE_NAMES = listOf("1. Schmett", "2. Rücken", "3. Brust", "4. Freistil")
private val LAGEN_SHORT_NAMES = listOf("Schmett", "Rücken", "Brust", "Freistil")
private val LAGEN_STROKE_COLORS = listOf(Color(0xFFFF5252), Color(0xFFFFD600), Color(0xFF00E676), Color(0xFF00E5FF))

@Composable
fun LaneTimerCard(
    lane: LaneState,
    onLapClick: (Long) -> Unit,
    onFinishClick: (Long) -> Unit,
    onAssignSwimmerClick: () -> Unit,
    onTargetPaceClick: () -> Unit,
    onRemoveLaneClick: (() -> Unit)? = null,
    onStartSingleLaneClick: (() -> Unit)? = null,
    onPauseClick: (() -> Unit)? = null,
    onResumeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    val laneAccentColor = remember(lane.laneNumber) {
        LaneColors.getOrElse((lane.laneNumber - 1) % LaneColors.size) { AquaPrimary }
    }

    val cardBorderColor by animateColorAsState(
        targetValue = when (lane.status) {
            LaneStatus.RUNNING -> laneAccentColor
            LaneStatus.PAUSED -> Color(0xFFF57C00)
            LaneStatus.COUNTDOWN -> Color(0xFFFFD600)
            LaneStatus.FINISHED -> Color(0xFF78909C)
            LaneStatus.IDLE -> OceanBorder
        },
        label = "border_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lane_card_${lane.laneNumber}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = OceanSurface),
        border = BorderStroke(1.5.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            LaneHeader(
                laneNumber = lane.laneNumber,
                swimmerName = lane.swimmerName,
                status = lane.status,
                countdownRemainingSeconds = lane.countdownRemainingSeconds,
                targetLapMillis = lane.targetLapMillis,
                laneAccentColor = laneAccentColor,
                onAssignSwimmerClick = onAssignSwimmerClick,
                onTargetPaceClick = onTargetPaceClick,
                onRemoveLaneClick = onRemoveLaneClick
            )

            Spacer(modifier = Modifier.height(6.dp))

            DigitalTimerDisplay(
                status = lane.status,
                countdownRemainingSeconds = lane.countdownRemainingSeconds,
                pausedElapsedMillis = lane.currentElapsedMillis,
                laneStartTimeRealtime = lane.laneStartTimeRealtime,
                finishTimeMillis = lane.finishTimeMillis,
                laneNumber = lane.laneNumber
            )

            LaneProgressAndLaps(
                status = lane.status,
                targetDistanceMeters = lane.targetDistanceMeters,
                splitDistanceMeters = lane.splitDistanceMeters,
                strokeType = lane.strokeType,
                finishTimeMillis = lane.finishTimeMillis,
                laneStartTimeRealtime = lane.laneStartTimeRealtime,
                laps = lane.laps,
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LaneControls(
                status = lane.status,
                laneNumber = lane.laneNumber,
                onLapClick = onLapClick,
                onFinishClick = onFinishClick,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onStartSingleLaneClick = onStartSingleLaneClick
            )

            LaneLapsHistory(
                isExpanded = isExpanded,
                laps = lane.laps,
                splitDistanceMeters = lane.splitDistanceMeters,
                targetDistanceMeters = lane.targetDistanceMeters,
                strokeType = lane.strokeType
            )
        }
    }
}

@Composable
private fun LaneHeader(
    laneNumber: Int,
    swimmerName: String,
    status: LaneStatus,
    countdownRemainingSeconds: Int,
    targetLapMillis: Long?,
    laneAccentColor: Color,
    onAssignSwimmerClick: () -> Unit,
    onTargetPaceClick: () -> Unit,
    onRemoveLaneClick: (() -> Unit)?
) {
    // Header Row: Lane Badge, Swimmer Name, Settings Chips
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable { onAssignSwimmerClick() }
                .padding(vertical = 4.dp)
        ) {
            // Lane Badge
            Surface(
                shape = CircleShape,
                color = laneAccentColor,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$laneNumber",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp
                        ),
                        color = Color.Black,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = swimmerName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Schwimmer zuweisen",
                        tint = Color.Gray,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Status subtitle
                val statusLabel = when (status) {
                    LaneStatus.IDLE -> "Bereit"
                    LaneStatus.COUNTDOWN -> "Start in ${countdownRemainingSeconds}s"
                    LaneStatus.RUNNING -> "Aktiv"
                    LaneStatus.PAUSED -> "Pausiert"
                    LaneStatus.FINISHED -> "Ziel erreicht"
                }
                val statusColor = when (status) {
                    LaneStatus.IDLE -> Color.LightGray
                    LaneStatus.COUNTDOWN -> Color(0xFFFFD600)
                    LaneStatus.RUNNING -> PaceGreen
                    LaneStatus.PAUSED -> Color(0xFFF57C00)
                    LaneStatus.FINISHED -> Color.Gray
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Action chips: Target Pace & Delete Action
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Target Pace Chip
            SuggestionChip(
                onClick = onTargetPaceClick,
                label = {
                    Text(
                        text = if (targetLapMillis != null) {
                            "Soll: ${TimeFormatter.formatMillis(targetLapMillis, shortFormat = true)}"
                        } else "Soll-Pace",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (targetLapMillis != null) Color(0xFF193248) else Color(0xFF132030),
                    labelColor = if (targetLapMillis != null) AquaPrimary else Color.LightGray
                ),
                border = BorderStroke(0.5.dp, Color(0xFF2C4360)),
                modifier = Modifier.height(28.dp)
            )

            // Remove Lane Button (Individual box remove)
            if (onRemoveLaneClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onRemoveLaneClick,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("remove_lane_button_$laneNumber")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Bahn $laneNumber entfernen",
                        tint = Color(0xFFFF5252).copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RunningTime(startRealtime: Long, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val now = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(startRealtime) {
        while (true) withFrameMillis { now.longValue = SystemClock.elapsedRealtime() }
    }
    // feste Größe -> Relayout-Grenze, kein Hochpropagieren
    val box = remember(style) { measurer.measure("00:00.00", style).size }
    val density = LocalDensity.current
    Spacer(
        modifier
            .size(with(density) { DpSize(box.width.toDp(), box.height.toDp()) })
            .drawBehind {
                val text = TimeFormatter.formatMillis(now.longValue - startRealtime)
                drawText(measurer, text, style = style.copy(color = color))
            }
    )
}

@Composable
private fun DigitalTimerDisplay(
    status: LaneStatus,
    countdownRemainingSeconds: Int,
    pausedElapsedMillis: Long,
    laneStartTimeRealtime: Long?,
    finishTimeMillis: Long?,
    laneNumber: Int
) {
    val textStyle = MaterialTheme.typography.displayLarge.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = 38.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("timer_text_lane_$laneNumber"),
        contentAlignment = Alignment.Center
    ) {
        if (status == LaneStatus.RUNNING && laneStartTimeRealtime != null) {
            RunningTime(
                startRealtime = laneStartTimeRealtime,
                style = textStyle,
                color = TextWhite
            )
        } else {
            val displayTime = when (status) {
                LaneStatus.IDLE -> "00:00.00"
                LaneStatus.COUNTDOWN -> "in ${countdownRemainingSeconds}s"
                LaneStatus.PAUSED -> TimeFormatter.formatMillis(pausedElapsedMillis)
                LaneStatus.FINISHED -> TimeFormatter.formatMillis(finishTimeMillis ?: pausedElapsedMillis)
                else -> "00:00.00"
            }
            Text(
                text = displayTime,
                style = textStyle,
                textAlign = TextAlign.Center,
                color = when (status) {
                    LaneStatus.COUNTDOWN -> Color(0xFFFFD600)
                    LaneStatus.FINISHED -> Color(0xFFB0BEC5)
                    LaneStatus.PAUSED -> Color(0xFFFFCC80)
                    else -> TextWhite
                },
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LaneProgressAndLaps(
    status: LaneStatus,
    targetDistanceMeters: Int,
    splitDistanceMeters: Int,
    strokeType: String,
    finishTimeMillis: Long?,
    laneStartTimeRealtime: Long?,
    laps: List<LapItem>,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    var restSecondsState by remember(status, finishTimeMillis, laneStartTimeRealtime) {
        mutableLongStateOf(
            if (status == LaneStatus.FINISHED && finishTimeMillis != null && laneStartTimeRealtime != null) {
                val now = SystemClock.elapsedRealtime()
                ((now - laneStartTimeRealtime).coerceAtLeast(0L) - finishTimeMillis).coerceAtLeast(0L) / 1000L
            } else 0L
        )
    }

    LaunchedEffect(status, finishTimeMillis, laneStartTimeRealtime) {
        if (status == LaneStatus.FINISHED && finishTimeMillis != null && laneStartTimeRealtime != null) {
            while (true) {
                val now = SystemClock.elapsedRealtime()
                val totalElapsed = (now - laneStartTimeRealtime).coerceAtLeast(0L)
                val restMillis = (totalElapsed - finishTimeMillis).coerceAtLeast(0L)
                restSecondsState = restMillis / 1000L
                kotlinx.coroutines.delay(200L)
            }
        }
    }

    val totalSplitsNeeded = (targetDistanceMeters / splitDistanceMeters).coerceAtLeast(1)
    val currentLapNumber = (laps.size + 1).coerceAtMost(totalSplitsNeeded)
    val nextSplitDistance = (currentLapNumber * splitDistanceMeters).coerceAtMost(targetDistanceMeters)
    val isLagen = strokeType.equals("Lagen", ignoreCase = true)
    val lagenLegLength = (targetDistanceMeters / 4).coerceAtLeast(1)
    val currentLagenLegIndex = (((currentLapNumber - 1) * splitDistanceMeters) / lagenLegLength).coerceIn(0, 3)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Split distance progress label + Lagen leg
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (isLagen && status == LaneStatus.RUNNING) {
                val color = LAGEN_STROKE_COLORS[currentLagenLegIndex]
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, color),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = LAGEN_STROKE_NAMES[currentLagenLegIndex],
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }

            val progressText = when {
                status == LaneStatus.FINISHED -> {
                    "Ziel erreicht • Pause: ${TimeFormatter.formatSeconds(restSecondsState)}"
                }
                status == LaneStatus.RUNNING || status == LaneStatus.PAUSED -> {
                    if (currentLapNumber >= totalSplitsNeeded) {
                        "Ziel ${targetDistanceMeters}m ($currentLapNumber/$totalSplitsNeeded)"
                    } else {
                        "${nextSplitDistance}m ($currentLapNumber/$totalSplitsNeeded)"
                    }
                }
                else -> "${targetDistanceMeters}m • alle ${splitDistanceMeters}m"
            }

            Text(
                text = progressText,
                style = MaterialTheme.typography.labelSmall,
                color = if (status == LaneStatus.RUNNING || status == LaneStatus.PAUSED) AquaPrimary else Color.LightGray,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Last Lap badge if laps exist
        if (laps.isNotEmpty()) {
            val lastLap = laps.last()
            Spacer(modifier = Modifier.width(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onExpandClick() }
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF132030))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "L${lastLap.lapIndex}: ${TimeFormatter.formatMillis(lastLap.lapTimeMillis, shortFormat = true)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextWhite,
                    maxLines = 1
                )
                if (lastLap.deltaTargetMillis != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    val isFaster = lastLap.deltaTargetMillis <= 0
                    Text(
                        text = TimeFormatter.formatDelta(lastLap.deltaTargetMillis),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isFaster) PaceGreen else PaceRed,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Details",
                    tint = Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun LaneControls(
    status: LaneStatus,
    laneNumber: Int,
    onLapClick: (Long) -> Unit,
    onFinishClick: (Long) -> Unit,
    onPauseClick: (() -> Unit)?,
    onResumeClick: (() -> Unit)?,
    onStartSingleLaneClick: (() -> Unit)?
) {
    if (status == LaneStatus.RUNNING) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // LAP / SPLIT Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AquaPrimary,
                modifier = Modifier
                    .weight(1.6f)
                    .height(60.dp)
                    .testTag("lap_button_lane_$laneNumber")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val t = SystemClock.elapsedRealtime() - (SystemClock.uptimeMillis() - down.uptimeMillis)
                            onLapClick(t)
                            waitForUpOrCancellation()
                        }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LAP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        ),
                        color = Color.Black,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // PAUSE Button
            if (onPauseClick != null) {
                Surface(
                    onClick = onPauseClick,
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF57C00),
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // FINISH Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFD32F2F),
                modifier = Modifier
                    .weight(1.2f)
                    .height(60.dp)
                    .testTag("finish_button_lane_$laneNumber")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val t = SystemClock.elapsedRealtime() - (SystemClock.uptimeMillis() - down.uptimeMillis)
                            onFinishClick(t)
                            waitForUpOrCancellation()
                        }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ZIEL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    } else if (status == LaneStatus.PAUSED) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // RESUME Button
            if (onResumeClick != null) {
                Surface(
                    onClick = onResumeClick,
                    shape = RoundedCornerShape(14.dp),
                    color = PaceGreen,
                    modifier = Modifier
                        .weight(1.8f)
                        .height(60.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WEITER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            ),
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // FINISH Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFD32F2F),
                modifier = Modifier
                    .weight(1.2f)
                    .height(60.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val t = SystemClock.elapsedRealtime() - (SystemClock.uptimeMillis() - down.uptimeMillis)
                            onFinishClick(t)
                            waitForUpOrCancellation()
                        }
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ZIEL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    } else if (status == LaneStatus.COUNTDOWN) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1B2C3F),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "STARTET GLEICH...",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PaceNeutral
                )
            }
        }
    } else {
        // IDLE or FINISHED State -> Allow Einzelstart / Intervall Re-Start
        if (onStartSingleLaneClick != null) {
            val isFinished = status == LaneStatus.FINISHED
            var enableRestart by remember(isFinished) { mutableStateOf(!isFinished) }
            LaunchedEffect(isFinished) {
                if (isFinished) {
                    kotlinx.coroutines.delay(1500)
                    enableRestart = true
                }
            }
            
            Surface(
                onClick = onStartSingleLaneClick,
                enabled = enableRestart,
                shape = RoundedCornerShape(14.dp),
                color = if (enableRestart) PaceGreen else Color(0xFF1B2C3F),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("restart_lane_button_$laneNumber")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFinished) "RE-START / NEUES INTERVALL" else "EINZELSTART BAHN $laneNumber",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            lineHeight = 15.sp
                        ),
                        color = Color.Black,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1B2C3F),
                    modifier = Modifier
                        .weight(1.8f)
                        .height(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("SPLIT / LAP", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1B2C3F),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("ZIEL", color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LaneLapsHistory(
    isExpanded: Boolean,
    laps: List<LapItem>,
    splitDistanceMeters: Int,
    targetDistanceMeters: Int,
    strokeType: String
) {
    AnimatedVisibility(visible = isExpanded && laps.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0C1623))
                .padding(10.dp)
        ) {
            Text(
                text = "Zwischenzeiten (Splits alle ${splitDistanceMeters}m):",
                style = MaterialTheme.typography.labelSmall,
                color = AquaPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Column Titles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lap", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(36.dp), maxLines = 1)
                Text("Split", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(70.dp), maxLines = 1)
                Text("Runde", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(60.dp), maxLines = 1)
                Text("Pace / Abw.", style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
            }

            HorizontalDivider(color = Color(0xFF1E2D40), modifier = Modifier.padding(bottom = 4.dp))

            val isLagen = strokeType.equals("Lagen", ignoreCase = true)
            val lagenLegLength = (targetDistanceMeters / 4).coerceAtLeast(1)

            laps.forEach { lap ->
                val lapLagenStroke = if (isLagen) {
                    val legIdx = (((lap.lapIndex - 1) * splitDistanceMeters) / lagenLegLength).coerceIn(0, 3)
                    LAGEN_SHORT_NAMES[legIdx]
                } else null

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(62.dp)
                    ) {
                        Text(
                            text = "L${lap.lapIndex}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = AquaPrimary,
                            maxLines = 1
                        )
                        if (lapLagenStroke != null) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = lapLagenStroke,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.LightGray,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        text = TimeFormatter.formatMillis(lap.splitTimeMillis),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = TextWhite,
                        modifier = Modifier.width(70.dp),
                        maxLines = 1
                    )
                    Text(
                        text = TimeFormatter.formatMillis(lap.lapTimeMillis, shortFormat = true),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color.LightGray,
                        modifier = Modifier.width(60.dp),
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (lap.deltaTargetMillis != null) {
                            val isFaster = lap.deltaTargetMillis <= 0
                            Text(
                                text = TimeFormatter.formatDelta(lap.deltaTargetMillis),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isFaster) PaceGreen else PaceRed,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (lap.strokeRateSpm != null) {
                            Text(
                                text = "%.0f SPM".format(lap.strokeRateSpm),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD600),
                                maxLines = 1
                            )
                        } else if (lap.deltaTargetMillis == null) {
                            Text(
                                text = TimeFormatter.formatPacePer100m(lap.lapTimeMillis, splitDistanceMeters),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = AquaPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}