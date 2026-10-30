package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.RelayLegAssignment
import com.example.ui.LaneState
import com.example.ui.LaneStatus
import com.example.ui.LapItem
import com.example.ui.SeriesModeType
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.LaneColors
import com.example.ui.theme.OceanBorder
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.PaceRed
import com.example.ui.theme.TextWhite
import com.example.util.AndroidMonotonicClock
import com.example.util.MonotonicClock
import com.example.util.TimeFormatter

@Composable
fun LaneTimerCard(
    lane: LaneState,
    onLapClick: (Long) -> Unit,
    onFinishClick: (Long) -> Unit,
    onAssignSwimmerClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAssignRelayLegClick: ((legPosition: Int) -> Unit)? = null,
    isSeriesEnabled: Boolean = false,
    maxSeriesCount: Int? = null,
    seriesMode: SeriesModeType = SeriesModeType.MANUAL,
    sendOffDurationSec: Int = 0,
    onRemoveLaneClick: (() -> Unit)? = null,
    onStartSingleLaneClick: (() -> Unit)? = null,
    onPauseClick: (() -> Unit)? = null,
    onResumeClick: (() -> Unit)? = null,
    clock: MonotonicClock = AndroidMonotonicClock(),
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val laneAccentColor = remember(lane.laneNumber) {
        LaneColors.getOrElse((lane.laneNumber - 1) % LaneColors.size) { AquaPrimary }
    }

    val cardBorderColor = remember(lane.status, laneAccentColor) {
        when (lane.status) {
            LaneStatus.RUNNING -> laneAccentColor
            LaneStatus.PAUSED -> Color(0xFFF57C00)
            LaneStatus.COUNTDOWN, LaneStatus.REST_COUNTDOWN -> Color(0xFFFFD600)
            LaneStatus.FINISHED -> Color(0xFF78909C)
            LaneStatus.IDLE -> OceanBorder
        }
    }

    val isRelay = lane.relayMembers.isNotEmpty()
    val effectiveTargetDist = lane.effectiveTargetDistanceMeters
    val splitsPerLeg = if (isRelay) {
        val legDist = (effectiveTargetDist / lane.relayMembers.size.coerceAtLeast(1)).coerceAtLeast(lane.splitDistanceMeters)
        (legDist / lane.splitDistanceMeters).coerceAtLeast(1)
    } else 1
    val activeRelayLegIdx = if (isRelay) {
        (lane.laps.size / splitsPerLeg).coerceIn(0, lane.relayMembers.lastIndex)
    } else 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lane_card_${lane.laneNumber}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = OceanSurface),
        border = BorderStroke(1.5.dp, cardBorderColor),
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            LaneHeader(
                laneNumber = lane.laneNumber,
                swimmerName = lane.swimmerName,
                isRelay = isRelay,
                relayFormatBadge = if (isRelay) "${lane.relayMembers.size}×${lane.splitDistanceMeters}m" else null,
                seriesCount = lane.seriesCount,
                maxSeriesCount = maxSeriesCount,
                isSeriesEnabled = isSeriesEnabled,
                status = lane.status,
                laneAccentColor = laneAccentColor,
                onAssignSwimmerClick = onAssignSwimmerClick,
                onRemoveLaneClick = onRemoveLaneClick
            )

            Spacer(modifier = Modifier.height(6.dp))

            DigitalTimerDisplay(
                status = lane.status,
                countdownRemainingSeconds = lane.countdownRemainingSeconds,
                restTargetRealtime = lane.restTargetRealtime,
                pausedElapsedMillis = lane.currentElapsedMillis,
                laneStartTimeRealtime = lane.laneStartTimeRealtime,
                finishTimeMillis = lane.finishTimeMillis,
                finishRealtimeMillis = lane.finishRealtimeMillis,
                isSeriesEnabled = isSeriesEnabled,
                seriesMode = seriesMode,
                sendOffDurationSec = sendOffDurationSec,
                clock = clock
            )

            if (isRelay) {
                Spacer(modifier = Modifier.height(8.dp))
                RelayLineupSection(
                    relayMembers = lane.relayMembers,
                    currentRunLaps = lane.laps,
                    status = lane.status,
                    splitsPerLeg = splitsPerLeg,
                    activeLegIdx = activeRelayLegIdx,
                    isLagenRelay = lane.strokeType.equals("Lagen", ignoreCase = true),
                    onLegClick = if (lane.status == LaneStatus.IDLE) {
                        { legPos -> onAssignRelayLegClick?.invoke(legPos) ?: onAssignSwimmerClick() }
                    } else null
                )
            }

            LaneProgressAndLaps(
                status = lane.status,
                targetDistanceMeters = effectiveTargetDist,
                splitDistanceMeters = lane.splitDistanceMeters,
                strokeType = lane.strokeType,
                currentRunLaps = lane.laps,
                allLaps = lane.allLaps,
                seriesCount = lane.seriesCount,
                isSeriesEnabled = isSeriesEnabled,
                isExpanded = isExpanded,
                onExpandClick = { isExpanded = !isExpanded }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LaneControls(
                status = lane.status,
                isSeriesEnabled = isSeriesEnabled,
                isRelay = isRelay,
                activeRelayLegNumber = activeRelayLegIdx + 1,
                totalRelayLegs = lane.relayMembers.size,
                onLapClick = onLapClick,
                onFinishClick = onFinishClick,
                onPauseClick = onPauseClick,
                onResumeClick = onResumeClick,
                onStartSingleLaneClick = onStartSingleLaneClick,
                clock = clock
            )

            LaneLapsHistory(
                status = lane.status,
                targetDistanceMeters = effectiveTargetDist,
                isExpanded = isExpanded,
                allLaps = lane.allLaps,
                seriesCount = lane.seriesCount,
                isSeriesEnabled = isSeriesEnabled,
                splitDistanceMeters = lane.splitDistanceMeters,
                relayMembers = lane.relayMembers,
                splitsPerLeg = splitsPerLeg
            )
        }
    }
}

@Composable
private fun LaneHeader(
    laneNumber: Int,
    swimmerName: String,
    isRelay: Boolean = false,
    relayFormatBadge: String? = null,
    seriesCount: Int,
    maxSeriesCount: Int? = null,
    isSeriesEnabled: Boolean = false,
    status: LaneStatus,
    laneAccentColor: Color,
    onAssignSwimmerClick: () -> Unit,
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
                    imageVector = if (isRelay) Icons.Default.Groups else Icons.Default.Person,
                    contentDescription = "Schwimmer zuweisen",
                    tint = if (isRelay) AquaPrimary else TextWhite.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = swimmerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (relayFormatBadge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = AquaPrimary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = relayFormatBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AquaPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            if (isSeriesEnabled) {
                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = Color(0xFF1B2E46),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.5f))
                ) {
                    val seriesLabel = if (maxSeriesCount != null) "Serie $seriesCount/$maxSeriesCount" else "Serie $seriesCount"
                    Text(
                        text = seriesLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }

        if (onRemoveLaneClick != null && (status == LaneStatus.IDLE || status == LaneStatus.FINISHED)) {
            Spacer(modifier = Modifier.width(10.dp))
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

@Composable
private fun RelayLineupSection(
    relayMembers: List<RelayLegAssignment>,
    currentRunLaps: List<LapItem>,
    status: LaneStatus,
    splitsPerLeg: Int,
    activeLegIdx: Int,
    isLagenRelay: Boolean,
    onLegClick: ((legPosition: Int) -> Unit)? = null
) {
    val isRacing = status == LaneStatus.RUNNING || status == LaneStatus.PAUSED
    val isFinished = status == LaneStatus.FINISHED || status == LaneStatus.REST_COUNTDOWN

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0D1B2A),
        border = BorderStroke(1.dp, Color(0xFF1E344B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            relayMembers.chunked(2).forEachIndexed { rowIdx, pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pair.forEachIndexed { colIdx, member ->
                        val memberIdx = rowIdx * 2 + colIdx
                        val startLapIdx = memberIdx * splitsPerLeg
                        val endLapIdx = startLapIdx + splitsPerLeg
                        val startSplitMs = if (startLapIdx == 0) 0L else currentRunLaps.find { it.lapIndex == startLapIdx }?.splitTimeMillis
                        val endSplitMs = currentRunLaps.find { it.lapIndex == endLapIdx }?.splitTimeMillis
                        val completedLegTimeMs = if (startSplitMs != null && endSplitMs != null && endSplitMs > startSplitMs) {
                            endSplitMs - startSplitMs
                        } else null

                        val isCompleted = completedLegTimeMs != null
                        val isActiveNow = isRacing && !isCompleted && memberIdx == activeLegIdx
                        val isPlaceholder = member.swimmerId == null && member.swimmerName.startsWith("Starter")

                        val cellBg = when {
                            isActiveNow -> AquaPrimary.copy(alpha = 0.18f)
                            isCompleted -> Color(0xFF112638)
                            else -> Color(0xFF0A141F)
                        }
                        val cellBorder = when {
                            isActiveNow -> AquaPrimary
                            isCompleted -> PaceGreen.copy(alpha = 0.5f)
                            else -> Color(0xFF192B3E)
                        }

                        val shortStroke = when {
                            member.stroke.equals("Schmetterling", ignoreCase = true) -> "Schmett."
                            else -> member.stroke
                        }
                        val isAnchorLeg = !isLagenRelay && member.legPosition == relayMembers.size && relayMembers.size >= 2
                        val posLabel = if (isLagenRelay) {
                            "${member.legPosition}. $shortStroke"
                        } else if (isAnchorLeg) {
                            "${member.legPosition}. Schluss"
                        } else {
                            "${member.legPosition}. Starter"
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = cellBg,
                            border = BorderStroke(if (isActiveNow) 1.5.dp else 1.dp, cellBorder),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(
                                    if (onLegClick != null) {
                                        Modifier.clickable { onLegClick(member.legPosition) }
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false).padding(end = 4.dp)
                                    ) {
                                        val headerColor = when {
                                            isActiveNow -> AquaPrimary
                                            isCompleted -> PaceGreen
                                            isAnchorLeg -> Color(0xFFFFD600)
                                            else -> Color.Gray
                                        }
                                        if (isAnchorLeg) {
                                            Text(
                                                text = "${member.legPosition}. ",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = headerColor,
                                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                                maxLines = 1
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD600),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Schluss",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = headerColor,
                                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = posLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = headerColor,
                                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    when {
                                        completedLegTimeMs != null -> {
                                            Text(
                                                text = "✓ ${TimeFormatter.formatMillis(completedLegTimeMs, shortFormat = true)}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PaceGreen,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        isActiveNow -> {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AquaPrimary
                                            ) {
                                                Text(
                                                    text = "AKTIV",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        member.timeMillis != null && !isFinished -> {
                                            val estPrefix = if (member.isEstimated) "~" else ""
                                            Text(
                                                text = "$estPrefix${TimeFormatter.formatMillis(member.timeMillis, shortFormat = true)}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
                                                fontWeight = if (member.isEstimated) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (member.isEstimated) Color(0xFFFFB74D) else AquaPrimary.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = if (isPlaceholder && onLegClick != null) "Schwimmer wählen…" else member.swimmerName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActiveNow) FontWeight.Black else if (isPlaceholder) FontWeight.Normal else FontWeight.SemiBold,
                                    color = if (isPlaceholder) AquaPrimary.copy(alpha = 0.75f) else TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitalTimerDisplay(
    status: LaneStatus,
    countdownRemainingSeconds: Int,
    restTargetRealtime: Long? = null,
    pausedElapsedMillis: Long,
    laneStartTimeRealtime: Long?,
    finishTimeMillis: Long?,
    finishRealtimeMillis: Long? = null,
    isSeriesEnabled: Boolean = false,
    seriesMode: SeriesModeType = SeriesModeType.MANUAL,
    sendOffDurationSec: Int = 0,
    clock: MonotonicClock = AndroidMonotonicClock()
) {
    var displayMillis by remember { mutableLongStateOf(0L) }
    var restElapsedMillis by remember { mutableLongStateOf(0L) }
    var overrunMillis by remember { mutableLongStateOf(0L) }
    var isOverrunning by remember { mutableStateOf(false) }

    val sendOffMs = sendOffDurationSec * 1000L
    val isSendOffMode = isSeriesEnabled && seriesMode == SeriesModeType.SEND_OFF && sendOffMs > 0

    LaunchedEffect(status, laneStartTimeRealtime, pausedElapsedMillis, finishTimeMillis, finishRealtimeMillis, restTargetRealtime, isSeriesEnabled, seriesMode, sendOffDurationSec) {
        if (status == LaneStatus.RUNNING && laneStartTimeRealtime != null) {
            while (true) {
                withFrameMillis {
                    val elapsed = (clock.nowMillis() - laneStartTimeRealtime).coerceAtLeast(0L)
                    if (isSendOffMode) {
                        if (elapsed <= sendOffMs) {
                            displayMillis = sendOffMs - elapsed
                            isOverrunning = false
                            overrunMillis = 0L
                        } else {
                            displayMillis = 0L
                            isOverrunning = true
                            overrunMillis = elapsed - sendOffMs
                        }
                    } else {
                        displayMillis = elapsed
                        isOverrunning = false
                        overrunMillis = 0L
                    }
                }
            }
        } else if (status == LaneStatus.FINISHED && finishRealtimeMillis != null) {
            displayMillis = finishTimeMillis ?: pausedElapsedMillis
            isOverrunning = false
            overrunMillis = 0L
            while (true) {
                withFrameMillis {
                    restElapsedMillis = (clock.nowMillis() - finishRealtimeMillis).coerceAtLeast(0L)
                }
            }
        } else if (status == LaneStatus.REST_COUNTDOWN && restTargetRealtime != null) {
            displayMillis = finishTimeMillis ?: pausedElapsedMillis
            isOverrunning = false
            overrunMillis = 0L
            while (true) {
                withFrameMillis {
                    restElapsedMillis = (restTargetRealtime - clock.nowMillis()).coerceAtLeast(0L)
                }
            }
        } else {
            if (status == LaneStatus.PAUSED) {
                if (isSendOffMode) {
                    if (pausedElapsedMillis <= sendOffMs) {
                        displayMillis = sendOffMs - pausedElapsedMillis
                        isOverrunning = false
                        overrunMillis = 0L
                    } else {
                        displayMillis = 0L
                        isOverrunning = true
                        overrunMillis = pausedElapsedMillis - sendOffMs
                    }
                } else {
                    displayMillis = pausedElapsedMillis
                    isOverrunning = false
                    overrunMillis = 0L
                }
            } else {
                displayMillis = when (status) {
                    LaneStatus.FINISHED, LaneStatus.REST_COUNTDOWN -> finishTimeMillis ?: pausedElapsedMillis
                    LaneStatus.COUNTDOWN -> -(countdownRemainingSeconds * 1000L)
                    else -> 0L
                }
                isOverrunning = false
                overrunMillis = 0L
            }
            restElapsedMillis = 0L
        }
    }

    val displayColor = when {
        isOverrunning -> PaceRed
        status == LaneStatus.RUNNING && isSendOffMode -> AquaPrimary
        status == LaneStatus.PAUSED && isSendOffMode -> AquaPrimary
        status == LaneStatus.RUNNING -> TextWhite
        status == LaneStatus.PAUSED -> Color(0xFFFFB74D)
        status == LaneStatus.COUNTDOWN || status == LaneStatus.REST_COUNTDOWN -> Color(0xFFFFD600)
        status == LaneStatus.FINISHED -> AquaPrimary
        status == LaneStatus.IDLE -> TextWhite.copy(alpha = 0.5f)
        else -> TextWhite
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
                fontSize = if ((status == LaneStatus.FINISHED && finishRealtimeMillis != null) || status == LaneStatus.REST_COUNTDOWN || isOverrunning) 32.sp else 38.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOverrunning) PaceRed else displayColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (status == LaneStatus.REST_COUNTDOWN) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pausentimer",
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pause: ${TimeFormatter.formatMillis(restElapsedMillis)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD600),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (status == LaneStatus.FINISHED && finishRealtimeMillis != null) {
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
            } else if (isOverrunning) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⚠ Overrun +${TimeFormatter.formatMillis(overrunMillis)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PaceRed,
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
    status: LaneStatus,
    targetDistanceMeters: Int,
    splitDistanceMeters: Int,
    strokeType: String,
    currentRunLaps: List<LapItem>,
    allLaps: List<LapItem>,
    seriesCount: Int,
    isSeriesEnabled: Boolean = false,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    val completedDistance = if (status == LaneStatus.FINISHED || status == LaneStatus.REST_COUNTDOWN) {
        targetDistanceMeters
    } else {
        (currentRunLaps.size * splitDistanceMeters).coerceAtMost(targetDistanceMeters)
    }

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
                    text = if (isSeriesEnabled && seriesCount > 1) "${allLaps.size} Splits ($seriesCount S.)" else "${allLaps.size} Splits",
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

@Composable
private fun LaneControls(
    status: LaneStatus,
    isSeriesEnabled: Boolean = false,
    isRelay: Boolean = false,
    activeRelayLegNumber: Int = 1,
    totalRelayLegs: Int = 4,
    onLapClick: (Long) -> Unit,
    onFinishClick: (Long) -> Unit,
    onPauseClick: (() -> Unit)?,
    onResumeClick: (() -> Unit)?,
    onStartSingleLaneClick: (() -> Unit)?,
    clock: MonotonicClock = AndroidMonotonicClock()
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
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = AquaPrimary, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Einzelstart",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            LaneStatus.RUNNING -> {
                val isRelaySplit = isRelay && activeRelayLegNumber < totalRelayLegs
                val splitButtonLabel = if (isRelaySplit) {
                    "Wechsel ${activeRelayLegNumber}→${activeRelayLegNumber + 1}"
                } else {
                    "Split"
                }

                FilledTonalButton(
                    onClick = { onLapClick(clock.nowMillis()) },
                    modifier = Modifier
                        .weight(if (isRelaySplit) 1.65f else 1.4f)
                        .height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = AquaPrimary, contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = if (isRelaySplit) 8.dp else 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(if (isRelaySplit) 18.dp else 22.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isRelaySplit) 4.dp else 6.dp))
                    Text(
                        text = splitButtonLabel,
                        fontSize = if (isRelaySplit) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = { onFinishClick(clock.nowMillis()) },
                    modifier = Modifier
                        .weight(if (isRelaySplit) 1.0f else 1.1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = if (isRelaySplit) 10.dp else 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(if (isRelaySplit) 20.dp else 22.dp)
                    )
                    Spacer(modifier = Modifier.width(if (isRelaySplit) 4.dp else 6.dp))
                    Text(
                        text = "Ziel",
                        fontSize = if (isRelaySplit) 15.sp else 16.sp,
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
                            .size(if (isRelaySplit) 52.dp else 58.dp)
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
                            .weight(1.4f)
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFA726), contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Weiter",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { onFinishClick(clock.nowMillis()) },
                    modifier = Modifier
                        .weight(1.1f)
                        .height(58.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEF5350), contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ziel",
                        fontSize = 16.sp,
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
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
            LaneStatus.FINISHED, LaneStatus.REST_COUNTDOWN -> {
                if (onStartSingleLaneClick != null && (isSeriesEnabled || status == LaneStatus.REST_COUNTDOWN)) {
                    FilledTonalButton(
                        onClick = onStartSingleLaneClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = OceanBorder, contentColor = TextWhite),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Neustart",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Text(
                        text = "Ziel erreicht",
                        color = AquaPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
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
    status: LaneStatus,
    targetDistanceMeters: Int,
    isExpanded: Boolean,
    allLaps: List<LapItem>,
    seriesCount: Int,
    isSeriesEnabled: Boolean = false,
    splitDistanceMeters: Int,
    relayMembers: List<RelayLegAssignment> = emptyList(),
    splitsPerLeg: Int = 1
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
                val isLastLapInRun = lap == allLaps.lastOrNull { it.seriesIndex == lap.seriesIndex }
                val lapCumDist = if ((status == LaneStatus.FINISHED || status == LaneStatus.REST_COUNTDOWN) && isLastLapInRun) {
                    targetDistanceMeters
                } else {
                    (lap.lapIndex * splitDistanceMeters).coerceAtMost(targetDistanceMeters)
                }
                val relayMember = if (relayMembers.isNotEmpty()) {
                    val idx = ((lap.lapIndex - 1).coerceAtLeast(0) / splitsPerLeg).coerceIn(0, relayMembers.lastIndex)
                    relayMembers[idx]
                } else null

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
                        if (isSeriesEnabled && seriesCount > 1) {
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
                        if (relayMember != null) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "${relayMember.legPosition}. ${relayMember.swimmerName}",
                                    fontSize = 12.5.sp,
                                    color = TextWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${relayMember.stroke} • ${lapCumDist}m",
                                    fontSize = 10.5.sp,
                                    color = AquaPrimary.copy(alpha = 0.85f),
                                    maxLines = 1
                                )
                            }
                        } else {
                            val lapLabel = if (allLaps.count { it.seriesIndex == lap.seriesIndex } == 1 && (status == LaneStatus.FINISHED || status == LaneStatus.REST_COUNTDOWN)) {
                                "Ziel (${lapCumDist}m)"
                            } else {
                                "Lap ${lap.lapIndex} (${lapCumDist}m)"
                            }
                            Text(
                                text = lapLabel,
                                fontSize = 12.sp,
                                color = TextWhite.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0A1128)
@Composable
fun LaneTimerCardPreview() {
    com.example.ui.theme.SwimTimerTheme {
        LaneTimerCard(
            lane = LaneState(
                laneNumber = 1,
                status = LaneStatus.RUNNING,
                swimmerName = "Max Mustermann",
                laneStartTimeRealtime = 1000L,
                currentElapsedMillis = 32540L,
                laps = listOf(
                    LapItem(lapIndex = 1, splitTimeMillis = 15200, lapTimeMillis = 15200),
                    LapItem(lapIndex = 2, splitTimeMillis = 32540, lapTimeMillis = 17340)
                )
            ),
            onLapClick = {},
            onFinishClick = {},
            onAssignSwimmerClick = {},
        )
    }
}
