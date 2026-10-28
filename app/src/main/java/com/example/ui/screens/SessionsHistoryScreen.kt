package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SessionsHistoryScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { selectedUri ->
            viewModel.importSessionsFromUri(context.contentResolver, selectedUri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOceanBackground)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Top Action Bar for Export/Import
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historie (${sessions.size})",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = TextWhite,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Export Button
                FilledTonalIconButton(
                    onClick = {
                        if (sessions.isNotEmpty()) {
                            coroutineScope.launch {
                                ExportHelper.shareSessionsExport(context, sessions)
                            }
                        }
                    },
                    enabled = sessions.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = OceanSurface,
                        contentColor = AquaPrimary
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Historie exportieren",
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Import Button
                FilledTonalIconButton(
                    onClick = { importLauncher.launch("*/*") },
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = OceanSurface,
                        contentColor = AquaPrimary
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Historie importieren",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
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
                var sessionToDelete by remember { mutableStateOf<SessionWithLaps?>(null) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = sessions,
                        key = { sessionWithLaps -> sessionWithLaps.session.id },
                        contentType = { "SessionHistoryCard" }
                    ) { item ->
                        val onShare = remember(context, item, coroutineScope) {
                            {
                                coroutineScope.launch { ExportHelper.shareSession(context, item) }
                                Unit
                            }
                        }
                        val onDeleteRequest = remember(item) { { sessionToDelete = item } }

                        SessionHistoryCard(
                            sessionWithLaps = item,
                            onShare = onShare,
                            onDelete = onDeleteRequest
                        )
                    }
                }

                if (sessionToDelete != null) {
                    val targetSession = sessionToDelete!!
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { sessionToDelete = null },
                        title = { Text("Trainingseinheit löschen?", color = TextWhite, fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                text = "Möchtest du '${targetSession.session.title}' vom ${TimeFormatter.formatDate(targetSession.session.timestamp)} wirklich unwiderruflich löschen?",
                                color = Color.LightGray
                            )
                        },
                        containerColor = Color(0xFF101B2B),
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteSession(targetSession.session.id)
                                    sessionToDelete = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                            ) {
                                Text("Löschen", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            Button(
                                onClick = { sessionToDelete = null },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Text("Abbrechen", color = Color.LightGray)
                            }
                        }
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
    var isExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(value = false) }
    val s = sessionWithLaps.session
    val laps = sessionWithLaps.laps
    val groupedByLane = remember(laps) { laps.groupBy { it.laneNumber } }
    val sortedLanes = remember(groupedByLane) { groupedByLane.keys.sorted() }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
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
                            color = TextWhite,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${TimeFormatter.formatDate(s.timestamp)} • ${s.poolLengthMeters}m Becken",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            maxLines = 1
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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    sortedLanes.forEach { laneNum ->
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
                                color = TextWhite,
                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                            Text("Lap", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1.2f), maxLines = 1)
                            Text("Gesamt", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1.8f), maxLines = 1)
                            Text("Runde", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1.6f), maxLines = 1)
                            Text("Pace/SPM", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(2.2f), maxLines = 1)
                        }

                        val isLagen = s.strokeType.equals("Lagen", ignoreCase = true)
                        val lagenLegLength = (s.targetDistanceMeters / 4).coerceAtLeast(1)

                        laneLaps.forEach { lap ->
                            val lapLagenStroke = if (isLagen) {
                                val legIdx = (((lap.lapIndex - 1) * s.splitDistanceMeters) / lagenLegLength).coerceIn(0, 3)
                                ExportHelper.LAGEN_SHORT_NAMES[legIdx]
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
                                    modifier = Modifier.weight(1.2f)
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
                                    modifier = Modifier.weight(1.8f),
                                    maxLines = 1
                                )
                                Text(
                                    text = TimeFormatter.formatMillis(lap.lapTimeMillis, shortFormat = true),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color.LightGray,
                                    modifier = Modifier.weight(1.6f),
                                    maxLines = 1
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(2.2f)
                                ) {
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
                                        val totalLapsForLane = laneLaps.size
                                        val isLastLap = (lap == laneLaps.last())
                                        val lapDistance = if (totalLapsForLane == 1 && isLastLap) {
                                            s.targetDistanceMeters
                                        } else if (isLastLap && s.targetDistanceMeters > (totalLapsForLane - 1) * s.splitDistanceMeters) {
                                            s.targetDistanceMeters - ((totalLapsForLane - 1) * s.splitDistanceMeters)
                                        } else {
                                            s.splitDistanceMeters
                                        }
                                        Text(
                                            text = TimeFormatter.formatPacePer100m(lap.lapTimeMillis, lapDistance),
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
