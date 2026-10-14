package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SessionWithLaps
import com.example.ui.StopwatchViewModel
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.DeepOceanBackground
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.PaceRed
import com.example.ui.theme.TextWhite
import com.example.util.ExportHelper
import com.example.util.TimeFormatter

@Composable
fun SessionsHistoryScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOceanBackground)
            .padding(16.dp)
    ) {
        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Noch keine Einheiten gespeichert",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Stoppe Zeiten auf der Hauptseite und tippe auf 'SAVE', um Einheiten dauerhaft abzulegen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(sessions, key = { it.session.id }) { item ->
                    SessionHistoryCard(
                        sessionWithLaps = item,
                        onShare = { ExportHelper.shareSession(context, item) },
                        onDelete = { viewModel.deleteSession(item.session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(
    sessionWithLaps: SessionWithLaps,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val s = sessionWithLaps.session
    val laps = sessionWithLaps.laps
    val groupedByLane = laps.groupBy { it.laneNumber }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OceanSurface),
        modifier = Modifier.fillMaxWidth().testTag("session_card_${s.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Pool,
                        contentDescription = null,
                        tint = AquaPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = s.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite
                        )
                        Text(
                            text = "${TimeFormatter.formatDate(s.timestamp)} • ${s.poolLengthMeters}m Becken",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                    }
                }

                Row {
                    IconButton(onClick = onShare) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Teilen", tint = AquaPrimary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Löschen", tint = Color(0xFFFF5252))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Summary metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${s.targetDistanceMeters}m ${s.strokeType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AquaPrimary
                )
                Text(
                    text = "• ${groupedByLane.size} Bahnen",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
                Text(
                    text = "• ${laps.size} Splits gesamt",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable details toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Details ausblenden" else "Zeiten & Splits ansehen (${groupedByLane.size} Bahnen)",
                    style = MaterialTheme.typography.labelMedium,
                    color = AquaPrimary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = AquaPrimary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0C1623))
                        .padding(12.dp)
                ) {
                    groupedByLane.keys.sorted().forEach { laneNum ->
                        val laneLaps = groupedByLane[laneNum] ?: emptyList()
                        val swimmer = laneLaps.firstOrNull()?.swimmerName ?: "Bahn $laneNum"
                        val finishTime = laneLaps.lastOrNull()?.splitTimeMillis ?: 0L

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bahn $laneNum: $swimmer",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = "Gesamt: ${TimeFormatter.formatMillis(finishTime)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AquaPrimary
                            )
                        }

                        // Split Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lap", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(36.dp), maxLines = 1)
                            Text("Split", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(70.dp), maxLines = 1)
                            Text("Runde", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(60.dp), maxLines = 1)
                            Text("Pace/SPM", style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
                        }

                        val isLagen = s.strokeType.equals("Lagen", ignoreCase = true)
                        val lagenLegLength = (s.targetDistanceMeters / 4).coerceAtLeast(1)

                        laneLaps.forEach { lap ->
                            val lapLagenStroke = if (isLagen) {
                                val legIdx = (((lap.lapIndex - 1) * s.splitDistanceMeters) / lagenLegLength).coerceIn(0, 3)
                                val shortNames = listOf("Schmett", "Rücken", "Brust", "Freistil")
                                shortNames[legIdx]
                            } else null

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.width(76.dp)
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
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = Color(0xFFFFD600),
                                            maxLines = 1
                                        )
                                    }
                                }
                                Text(
                                    text = TimeFormatter.formatMillis(lap.splitTimeMillis),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = TextWhite,
                                    modifier = Modifier.width(68.dp),
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
                                            text = TimeFormatter.formatPacePer100m(lap.lapTimeMillis, s.splitDistanceMeters),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = AquaPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFF1E2D40),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
