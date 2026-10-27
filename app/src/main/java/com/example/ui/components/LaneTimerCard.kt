package com.example.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.util.TimeFormatter
import java.util.Locale

@Composable
fun LaneTimerCard(
    lane: LaneState,
    onLapClick: (Long) -> Unit,
    onFinishClick: (Long) -> Unit,
    onAssignSwimmerClick: () -> Unit,
    onTargetPaceClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveLaneClick: (() -> Unit)? = null,
    onStartSingleLaneClick: (() -> Unit)? = null,
    onPauseClick: (() -> Unit)? = null,
    onResumeClick: (() -> Unit)? = null,
    onStrokeRateTap: (() -> Unit)? = null,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val laneAccentColor = remember(lane.laneNumber) {
        LaneColors.getOrElse((lane.laneNumber - 1) % LaneColors.size) { AquaPrimary }
    }

    val cardBorderColor = remember(lane.status, laneAccentColor) {
        when (lane.status) {
            LaneStatus.RUNNING -> laneAccentColor
            LaneStatus.PAUSED -> Color(0xFFF57C00)
            LaneStatus.COUNTDOWN -> Color(0xFFFFD600)
            LaneStatus.FINISHED -> Color(0xFF78909C)
            LaneStatus.IDLE -> OceanBorder
        }
    }

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
                seriesCount = lane.seriesCount,
                status = lane.status,
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
                finishRealtimeMillis = lane.finishRealtimeMillis
            )

            LaneProgressAndLaps(
                targetDistanceMeters = lane.targetDistanceMeters,
                splitDistanceMeters = lane.splitDistanceMeters,
                strokeType = lane.strokeType,
                currentRunLaps = lane.laps,
                allLaps = lane.allLaps,
                seriesCount = lane.seriesCount,
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LaneControls(
                status = lane.status,
                onLapClick = onLapClick,
                onFinishClick = onFinishClick,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onStartSingleLaneClick = onStartSingleLaneClick,
                onStrokeRateTap = onStrokeRateTap,
                currentSpm = lane.currentSpm
            )

            LaneLapsHistory(
                isExpanded = isExpanded,
                allLaps = lane.allLaps,
                seriesCount = lane.seriesCount,
                splitDistanceMeters = lane.splitDistanceMeters
            )
        }
    }
}

@Composable
private fun LaneHeader(
    laneNumber: Int,
    swimmerName: String,
    seriesCount: Int,
    status: LaneStatus,
    targetLapMillis: Long?,
    laneAccentColor: Color,
    onAssignSwimmerClick: () -> Unit,
    onTargetPaceClick: () -> Unit,
    onRemoveLaneClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = laneAccentColor,
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "B$laneNumber",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onAssignSwimmerClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Schwimmer zuweisen",
                    tint = TextWhite.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = swimmerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                color = Color(0xFF1B2E46),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "Serie $seriesCount",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SuggestionChip(
                onClick = onTargetPaceClick,
                label = {
                    Text(
                        text = if (targetLapMillis != null && targetLapMillis > 0) {
                            "Ziel: ${TimeFormatter.formatForInput(targetLapMillis)}"
                        } else {
                            "Zielpace"
                        },
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = OceanSurface,
                    labelColor = AquaPrimary
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = AquaPrimary.copy(alpha = 0.5f)
                )
            )

            if (onRemoveLaneClick != null && (status == LaneStatus.IDLE || status == LaneStatus.FINISHED)) {
                IconButton(
                    onClick = onRemoveLaneClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Bahn entfernen",
                        tint = TextWhite.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitalTimerDisplay(
    status: LaneStatus,
    countdownRemainingSeconds: Int,
    pausedElapsedMillis: Long,
    laneStartTimeRealtime: Long?,
    finishTimeMillis: Long?,
    finishRealtimeMillis: Long? = null
) {
    var displayMillis by remember { mutableLongStateOf(0L) }
    var restElapsedMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(status, laneStartTimeRealtime, pausedElapsedMillis, finishTimeMillis, finishRealtimeMillis) {
        if (status == LaneStatus.RUNNING && laneStartTimeRealtime != null) {
            while (true) {
                withFrameMillis {
                    displayMillis = (SystemClock.elapsedRealtime() - laneStartTimeRealtime).coerceAtLeast(0L)
                }
            }
        } else if (status == LaneStatus.FINISHED && finishRealtimeMillis != null) {
            displayMillis = finishTimeMillis ?: pausedElapsedMillis
            while (true) {
                withFrameMillis {
                    restElapsedMillis = (SystemClock.elapsedRealtime() - finishRealtimeMillis).coerceAtLeast(0L)
                }
            }
        } else {
            displayMillis = when (status) {
                LaneStatus.PAUSED -> pausedElapsedMillis
                LaneStatus.FINISHED -> finishTimeMillis ?: pausedElapsedMillis
                LaneStatus.COUNTDOWN -> -(countdownRemainingSeconds * 1000L)
                else -> 0L
            }
            restElapsedMillis = 0L
        }
    }

    val displayColor = when (status) {
        LaneStatus.RUNNING -> TextWhite
        LaneStatus.PAUSED -> Color(0xFFFFB74D)
        LaneStatus.COUNTDOWN -> Color(0xFFFFD600)
        LaneStatus.FINISHED -> AquaPrimary
        LaneStatus.IDLE -> TextWhite.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A141D), shape = RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (status == LaneStatus.COUNTDOWN) {
                    "-${TimeFormatter.formatSeconds(countdownRemainingSeconds.toLong())}"
                } else {
                    TimeFormatter.formatMillis(displayMillis)
                },
                fontFamily = FontFamily.Monospace,
                fontSize = if (status == LaneStatus.FINISHED && finishRealtimeMillis != null) 34.sp else 38.sp,
                fontWeight = FontWeight.Bold,
                color = displayColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (status == LaneStatus.FINISHED && finishRealtimeMillis != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pausentimer",
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pause: ${TimeFormatter.formatMillis(restElapsedMillis)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LaneProgressAndLaps(
    targetDistanceMeters: Int,
    splitDistanceMeters: Int,
    strokeType: String,
    currentRunLaps: List<LapItem>,
    allLaps: List<LapItem>,
    seriesCount: Int,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    val completedDistance = currentRunLaps.size * splitDistanceMeters

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = OceanSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OceanBorder)
            ) {
                Text(
                    text = "${completedDistance}m / ${targetDistanceMeters}m",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AquaPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = OceanSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OceanBorder)
            ) {
                Text(
                    text = strokeType,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextWhite.copy(alpha = 0.8f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (allLaps.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onExpandClick() }
            ) {
                Text(
                    text = if (seriesCount > 1) "${allLaps.size} Laps ($seriesCount S.)" else "${allLaps.size} Laps",
                    fontSize = 12.sp,
                    color = TextWhite.copy(alpha = 0.7f),
                    maxLines = 1
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Einklappen" else "Ausklappen",
                    tint = TextWhite.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun LaneControls(
    status: LaneStatus,
    onLapClick: (Long) -> Unit,
    onFinishClick: (Long) -> Unit,
    onPauseClick: (() -> Unit)?,
    onResumeClick: (() -> Unit)?,
    onStartSingleLaneClick: (() -> Unit)?,
    onStrokeRateTap: (() -> Unit)?,
    currentSpm: Double?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (status) {
            LaneStatus.IDLE -> {
                if (onStartSingleLaneClick != null) {
                    FilledTonalButton(
                        onClick = onStartSingleLaneClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = AquaPrimary, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Einzelstart",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            LaneStatus.RUNNING -> {
                FilledTonalButton(
                    onClick = { onLapClick(SystemClock.elapsedRealtime()) },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = AquaPrimary, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Split",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                FilledTonalButton(
                    onClick = { onFinishClick(SystemClock.elapsedRealtime()) },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ziel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (onPauseClick != null) {
                    IconButton(
                        onClick = onPauseClick,
                        modifier = Modifier
                            .background(OceanBorder, CircleShape)
                            .size(52.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = TextWhite, modifier = Modifier.size(24.dp))
                    }
                }
            }
            LaneStatus.PAUSED -> {
                if (onResumeClick != null) {
                    FilledTonalButton(
                        onClick = onResumeClick,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFA726), contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Weiter",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { onFinishClick(SystemClock.elapsedRealtime()) },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(52.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ziel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            LaneStatus.COUNTDOWN -> {
                Text(
                    text = "Startet in Kürze...",
                    color = Color(0xFFFFD600),
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
            LaneStatus.FINISHED -> {
                if (onStartSingleLaneClick != null) {
                    FilledTonalButton(
                        onClick = onStartSingleLaneClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = OceanBorder, contentColor = TextWhite),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Neustart",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Text(
                        text = "Ziel erreicht",
                        color = AquaPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun LaneLapsHistory(
    isExpanded: Boolean,
    allLaps: List<LapItem>,
    seriesCount: Int,
    splitDistanceMeters: Int
) {
    AnimatedVisibility(visible = isExpanded && allLaps.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                color = OceanBorder
            )

            allLaps.forEach { lap ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (seriesCount > 1) {
                            Surface(
                                color = Color(0xFF1B2E46),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "S${lap.seriesIndex}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AquaPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Lap ${lap.lapIndex} (${lap.lapIndex * splitDistanceMeters}m)",
                            fontSize = 12.sp,
                            color = TextWhite.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (lap.strokeRateSpm != null && lap.strokeRateSpm > 0) {
                            Text(
                                text = String.format(Locale.US, "%.0f SPM", lap.strokeRateSpm),
                                fontSize = 11.sp,
                                color = AquaPrimary,
                                maxLines = 1
                            )
                        }

                        if (lap.deltaTargetMillis != null) {
                            val deltaColor = if (lap.deltaTargetMillis <= 0) PaceGreen else PaceRed
                            Text(
                                text = TimeFormatter.formatDelta(lap.deltaTargetMillis),
                                fontSize = 11.sp,
                                color = deltaColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Runde: ${TimeFormatter.formatMillis(lap.lapTimeMillis)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            Text(
                                text = "Gesamt: ${TimeFormatter.formatMillis(lap.splitTimeMillis)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = AquaPrimary,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
