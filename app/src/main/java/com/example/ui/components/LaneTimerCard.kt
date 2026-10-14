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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.LaneColors
import com.example.ui.theme.OceanBorder
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.PaceRed
import com.example.ui.theme.TextWhite
import com.example.util.TimeFormatter

@Composable
fun LaneTimerCard(
    lane: LaneState,
    onLapClick: () -> Unit,
    onFinishClick: () -> Unit,
    onAssignSwimmerClick: () -> Unit,
    onTargetPaceClick: () -> Unit,
    onStrokeRateClick: (() -> Unit)? = null,
    onRemoveLaneClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val laneAccentColor = LaneColors.getOrElse((lane.laneNumber - 1) % LaneColors.size) { AquaPrimary }

    val cardBorderColor by animateColorAsState(
        targetValue = when (lane.status) {
            LaneStatus.RUNNING -> laneAccentColor
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
        colors = CardDefaults.cardColors(
            containerColor = OceanSurface
        ),
        border = BorderStroke(1.5.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
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
                                text = "${lane.laneNumber}",
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
                                text = lane.swimmerName,
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
                        val statusLabel = when (lane.status) {
                            LaneStatus.IDLE -> "Bereit"
                            LaneStatus.COUNTDOWN -> "Start in ${lane.countdownRemainingSeconds}s"
                            LaneStatus.RUNNING -> "Aktiv"
                            LaneStatus.FINISHED -> "Ziel erreicht"
                        }
                        val statusColor = when (lane.status) {
                            LaneStatus.IDLE -> Color.LightGray
                            LaneStatus.COUNTDOWN -> Color(0xFFFFD600)
                            LaneStatus.RUNNING -> PaceGreen
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
                                text = if (lane.targetLapMillis != null) {
                                    "Soll: ${TimeFormatter.formatMillis(lane.targetLapMillis, shortFormat = true)}"
                                } else "Soll-Pace",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (lane.targetLapMillis != null) Color(0xFF193248) else Color(0xFF132030),
                            labelColor = if (lane.targetLapMillis != null) AquaPrimary else Color.LightGray
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
                                .testTag("remove_lane_button_${lane.laneNumber}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Bahn ${lane.laneNumber} entfernen",
                                tint = Color(0xFFFF5252).copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main Digital Timer Display Row - Centered
            val displayTime = when (lane.status) {
                LaneStatus.IDLE -> "00:00.00"
                LaneStatus.COUNTDOWN -> "in ${lane.countdownRemainingSeconds}s"
                LaneStatus.RUNNING -> TimeFormatter.formatMillis(lane.currentElapsedMillis)
                LaneStatus.FINISHED -> TimeFormatter.formatMillis(lane.finishTimeMillis ?: lane.currentElapsedMillis)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    textAlign = TextAlign.Center,
                    color = when (lane.status) {
                        LaneStatus.COUNTDOWN -> Color(0xFFFFD600)
                        LaneStatus.FINISHED -> Color(0xFFB0BEC5)
                        else -> TextWhite
                    },
                    maxLines = 1,
                    modifier = Modifier.testTag("timer_text_lane_${lane.laneNumber}")
                )
            }

            // Split Plan & Last Lap Info Row
            val totalSplitsNeeded = (lane.targetDistanceMeters / lane.splitDistanceMeters).coerceAtLeast(1)
            val currentLapNumber = (lane.laps.size + 1).coerceAtMost(totalSplitsNeeded)
            val nextSplitDistance = (currentLapNumber * lane.splitDistanceMeters).coerceAtMost(lane.targetDistanceMeters)
            val isLagen = lane.strokeType.equals("Lagen", ignoreCase = true)
            val lagenLegLength = (lane.targetDistanceMeters / 4).coerceAtLeast(1)
            val currentLagenLegIndex = (((currentLapNumber - 1) * lane.splitDistanceMeters) / lagenLegLength).coerceIn(0, 3)
            val lagenStrokeNames = listOf("1. Schmett", "2. Rücken", "3. Brust", "4. Freistil")
            val lagenStrokeColors = listOf(Color(0xFFFF5252), Color(0xFFFFD600), Color(0xFF00E676), Color(0xFF00E5FF))

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
                    if (isLagen && lane.status == LaneStatus.RUNNING) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = lagenStrokeColors[currentLagenLegIndex].copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, lagenStrokeColors[currentLagenLegIndex]),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = lagenStrokeNames[currentLagenLegIndex],
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = lagenStrokeColors[currentLagenLegIndex],
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }

                    val progressText = when {
                        lane.status == LaneStatus.FINISHED -> {
                            val finish = lane.finishTimeMillis ?: lane.currentElapsedMillis
                            val restMillis = (lane.currentElapsedMillis - finish).coerceAtLeast(0L)
                            if (restMillis >= 1000L) {
                                "Ziel (${lane.targetDistanceMeters}m) • +${TimeFormatter.formatMillis(restMillis, shortFormat = true)}"
                            } else {
                                "Ziel (${lane.targetDistanceMeters}m)"
                            }
                        }
                        lane.status == LaneStatus.RUNNING -> {
                            if (currentLapNumber >= totalSplitsNeeded) {
                                "Ziel ${lane.targetDistanceMeters}m ($currentLapNumber/$totalSplitsNeeded)"
                            } else {
                                "${nextSplitDistance}m ($currentLapNumber/$totalSplitsNeeded)"
                            }
                        }
                        else -> "${lane.targetDistanceMeters}m • alle ${lane.splitDistanceMeters}m"
                    }

                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (lane.status == LaneStatus.RUNNING) AquaPrimary else Color.LightGray,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Last Lap badge if laps exist
                if (lane.laps.isNotEmpty()) {
                    val lastLap = lane.laps.last()
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { isExpanded = !isExpanded }
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

            Spacer(modifier = Modifier.height(12.dp))

            // Massive Poolside Touch Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // LAP / SPLIT Button
                Button(
                    onClick = onLapClick,
                    enabled = lane.status == LaneStatus.RUNNING,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AquaPrimary,
                        disabledContainerColor = Color(0xFF1B2C3F)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.8f)
                        .height(60.dp)
                        .testTag("lap_button_lane_${lane.laneNumber}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (lane.status == LaneStatus.RUNNING) Color.Black else Color.DarkGray,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SPLIT / LAP",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            ),
                            color = if (lane.status == LaneStatus.RUNNING) Color.Black else Color.DarkGray,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // FINISH Button
                FilledTonalButton(
                    onClick = onFinishClick,
                    enabled = lane.status == LaneStatus.RUNNING,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFD32F2F),
                        disabledContainerColor = Color(0xFF1B2C3F)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(60.dp)
                        .testTag("finish_button_lane_${lane.laneNumber}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = if (lane.status == LaneStatus.RUNNING) Color.White else Color.DarkGray,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ZIEL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            ),
                            color = if (lane.status == LaneStatus.RUNNING) Color.White else Color.DarkGray,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Expanded Lap History for this Lane (Clean Structured Table with NO wrapping)
            AnimatedVisibility(visible = isExpanded && lane.laps.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C1623))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Zwischenzeiten (Splits alle ${lane.splitDistanceMeters}m):",
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

                    lane.laps.forEach { lap ->
                        val lapLagenStroke = if (isLagen) {
                            val legIdx = (((lap.lapIndex - 1) * lane.splitDistanceMeters) / lagenLegLength).coerceIn(0, 3)
                            val shortNames = listOf("Schmett", "Rücken", "Brust", "Freistil")
                            shortNames[legIdx]
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
                                        text = TimeFormatter.formatPacePer100m(lap.lapTimeMillis, lane.splitDistanceMeters),
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
    }
}
