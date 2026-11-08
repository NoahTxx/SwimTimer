package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.style.TextOverflow
import com.example.domain.PersonalBestEvaluator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SessionWithLaps
import com.example.data.model.SwimmerEntity
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
    val swimmers by viewModel.allSwimmers.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { selectedUri ->
            viewModel.importSessionsFromUri(context.contentResolver, selectedUri) { count ->
                val msg = if (count > 0) "$count Trainingseinheit(en) importiert" else "Keine gültigen Einheiten gefunden"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOceanBackground)
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                var sessionToEdit by remember { mutableStateOf<SessionWithLaps?>(null) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = sessions,
                        key = { (session) -> session.id },
                        contentType = { "SessionHistoryCard" }
                    ) { item ->
                        val onShareReport = remember(context, item) {
                            { ExportHelper.shareTrainingReport(context, item) }
                        }
                        val onShareCsv = remember(context, item, coroutineScope) {
                            {
                                coroutineScope.launch { ExportHelper.shareSession(context, item) }
                                Unit
                            }
                        }
                        val onEditRequest = remember(item) { { sessionToEdit = item } }
                        val onDeleteRequest = remember(item) { { sessionToDelete = item } }

                        SessionHistoryCard(
                            sessionWithLaps = item,
                            onShareReport = onShareReport,
                            onShareCsv = onShareCsv,
                            onEdit = onEditRequest,
                            onDelete = onDeleteRequest
                        )
                    }
                }

                if (sessionToEdit != null) {
                    val targetSession = sessionToEdit!!
                    EditSessionDialog(
                        sessionWithLaps = targetSession,
                        swimmers = swimmers,
                        onDismiss = { sessionToEdit = null },
                        onSave = { newTitle, laneAssignments, relayLegAssignments ->
                            viewModel.updateSessionLaneSwimmers(
                                sessionId = targetSession.session.id,
                                newTitle = newTitle,
                                laneSwimmerMap = laneAssignments,
                                relayLegSwimmerMap = relayLegAssignments,
                                onComplete = {
                                    android.widget.Toast.makeText(context, "Trainingseinheit aktualisiert", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
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
    onShareReport: () -> Unit,
    onShareCsv: () -> Unit,
    onEdit: () -> Unit,
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = onShareReport,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AquaPrimary.copy(alpha = 0.15f),
                            contentColor = AquaPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp).testTag("share_report_btn_${s.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Bericht",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(onClick = onShareCsv, modifier = Modifier.size(32.dp).testTag("share_csv_btn_${s.id}")) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "CSV Export",
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp).testTag("edit_btn_${s.id}")) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Bearbeiten",
                            tint = AquaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).testTag("delete_btn_${s.id}")) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Löschen",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
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
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { isExpanded = !isExpanded }
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
                        val laneLaps = (groupedByLane[laneNum] ?: emptyList()).sortedBy { it.lapIndex }
                        val seriesItems = com.example.domain.PersonalBestEvaluator.splitSavedLapsIntoSeries(
                            laps = laneLaps,
                            targetDistanceMeters = s.targetDistanceMeters,
                            splitDistanceMeters = s.splitDistanceMeters
                        )
                        val hasMultipleSeries = seriesItems.any { it.seriesIndex > 1 }
                        val rawSwimmer = laneLaps.firstOrNull()?.swimmerName ?: "Bahn $laneNum"
                        val laneDisplayTitle = rawSwimmer.substringBefore(" • ").trim().ifEmpty { rawSwimmer }
                        val finishTime = laneLaps.lastOrNull()?.splitTimeMillis ?: 0L

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bahn $laneNum: $laneDisplayTitle",
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
                            Text("Lap / Starter", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1.8f), maxLines = 1)
                            Text("Gesamt", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1.6f), maxLines = 1)
                            Text("Runde", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1.5f), maxLines = 1)
                            Text("Pace", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1.7f), maxLines = 1)
                        }

                        val isLagen = s.strokeType.equals("Lagen", ignoreCase = true)
                        val isRelaySession = com.example.domain.PersonalBestEvaluator.isRelaySession(s.title, laps)
                        val activeLagenShortNames = if (isRelaySession) ExportHelper.LAGEN_STAFFEL_SHORT_NAMES else ExportHelper.LAGEN_SHORT_NAMES
                        val lagenLegLength = (s.targetDistanceMeters / 4).coerceAtLeast(1)

                        laneLaps.zip(seriesItems).forEachIndexed { idx, (lap, lapItem) ->
                            val relayStarterName = if (lap.swimmerName.contains(" • ")) {
                                lap.swimmerName.substringAfter(" • ").trim()
                            } else null
                            val lapLagenStroke = if (isLagen) {
                                val legIdx = if (isRelaySession) {
                                    val parsedNum = relayStarterName?.substringBefore(".")?.trim()?.toIntOrNull()
                                    ((parsedNum ?: lapItem.lapIndex) - 1).coerceAtLeast(0) % activeLagenShortNames.size
                                } else {
                                    (((lapItem.lapIndex - 1) * s.splitDistanceMeters) / lagenLegLength).coerceIn(0, 3)
                                }
                                activeLagenShortNames[legIdx]
                            } else null

                            val lapsInThisSeries = seriesItems.filter { it.seriesIndex == lapItem.seriesIndex }
                            val isOnlyLapInSeries = lapsInThisSeries.size == 1

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1.8f).padding(end = 4.dp)
                                ) {
                                    if (isRelaySession && !relayStarterName.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (hasMultipleSeries) {
                                                Text(
                                                    text = "S${lapItem.seriesIndex} ",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                    color = AquaPrimary,
                                                    maxLines = 1
                                                )
                                            }
                                            Text(
                                                text = relayStarterName,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = TextWhite,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            if (lapLagenStroke != null) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = lapLagenStroke,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFFFD600),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val labelText = if (isOnlyLapInSeries) {
                                                if (hasMultipleSeries) "S${lapItem.seriesIndex} Ziel" else "Ziel"
                                            } else {
                                                if (hasMultipleSeries) "S${lapItem.seriesIndex} L${lapItem.lapIndex}" else "L${lapItem.lapIndex}"
                                            }
                                            Text(
                                                text = labelText,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = AquaPrimary,
                                                maxLines = 1
                                            )
                                            if (lapLagenStroke != null) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = lapLagenStroke,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFFFD600),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = TimeFormatter.formatMillis(lap.splitTimeMillis),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = TextWhite,
                                    modifier = Modifier.weight(1.6f),
                                    maxLines = 1
                                )
                                Text(
                                    text = TimeFormatter.formatMillis(lap.lapTimeMillis, shortFormat = true),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color.LightGray,
                                    modifier = Modifier.weight(1.5f),
                                    maxLines = 1
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1.7f)
                                ) {
                                    if (lap.deltaTargetMillis != null) {
                                        val isFaster = lap.deltaTargetMillis <= 0
                                        Text(
                                            text = TimeFormatter.formatDelta(lap.deltaTargetMillis),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isFaster) PaceGreen else PaceRed,
                                            maxLines = 1
                                        )
                                    } else {
                                        val lapDistance = if (isRelaySession) {
                                            s.splitDistanceMeters.coerceAtLeast(25)
                                        } else {
                                            val prevLap = if (idx > 0) seriesItems[idx - 1].takeIf { it.seriesIndex == lapItem.seriesIndex } else null
                                            val prevCumDist = if (prevLap != null) {
                                                val isPrevLast = (prevLap == lapsInThisSeries.lastOrNull())
                                                if (isPrevLast) s.targetDistanceMeters else (prevLap.lapIndex * s.splitDistanceMeters).coerceAtMost(s.targetDistanceMeters)
                                            } else 0
                                            val isLastLapInSeries = (lapItem == lapsInThisSeries.lastOrNull())
                                            val currentCumDist = if (isLastLapInSeries) {
                                                s.targetDistanceMeters
                                            } else {
                                                (lapItem.lapIndex * s.splitDistanceMeters).coerceAtMost(s.targetDistanceMeters)
                                            }
                                            (currentCumDist - prevCumDist).coerceAtLeast(s.splitDistanceMeters)
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

@Composable
fun EditSessionDialog(
    sessionWithLaps: SessionWithLaps,
    swimmers: List<SwimmerEntity>,
    onDismiss: () -> Unit,
    onSave: (
        newTitle: String,
        laneAssignments: Map<Int, Pair<Long?, String>>,
        relayLegAssignments: Map<Pair<Int, Int>, Pair<Long?, String>>
    ) -> Unit,
) {
    val s = sessionWithLaps.session
    val laps = sessionWithLaps.laps
    var title by remember { mutableStateOf(s.title) }

    val groupedByLane = remember(laps) { laps.groupBy { it.laneNumber } }
    val sortedLanes = remember(groupedByLane) { groupedByLane.keys.sorted() }

    val isRelaySession = remember(s.title, laps) {
        PersonalBestEvaluator.isRelaySession(s.title, laps) || laps.any { it.swimmerName.contains(" • ") }
    }

    val laneTeamNameMap = remember(laps) {
        val map = mutableStateMapOf<Int, String>()
        sortedLanes.forEach { laneNum ->
            val firstLap = groupedByLane[laneNum]?.firstOrNull()
            val rawName = firstLap?.swimmerName ?: "Bahn $laneNum"
            val teamName = if (rawName.contains(" • ")) rawName.substringBefore(" • ").trim() else rawName
            map[laneNum] = teamName
        }
        map
    }

    val singleLaneMap = remember(laps) {
        val map = mutableStateMapOf<Int, Pair<Long?, String>>()
        sortedLanes.forEach { laneNum ->
            val firstLap = groupedByLane[laneNum]?.firstOrNull()
            val rawName = firstLap?.swimmerName ?: "Bahn $laneNum"
            val cleanName = if (rawName.contains(" • ")) rawName.substringBefore(" • ").trim() else rawName
            map[laneNum] = Pair(firstLap?.swimmerId, cleanName)
        }
        map
    }

    // Map: Pair(laneNumber, legPosition) -> Pair(swimmerId, swimmerName)
    val relayLegMap = remember(laps) {
        val map = mutableStateMapOf<Pair<Int, Int>, Pair<Long?, String>>()
        sortedLanes.forEach { laneNum ->
            val laneLaps = groupedByLane[laneNum] ?: emptyList()
            laneLaps.forEach { lap ->
                if (lap.swimmerName.contains(" • ")) {
                    val legPart = lap.swimmerName.substringAfter(" • ").trim()
                    val legPos = legPart.substringBefore(".").trim().toIntOrNull()
                    val legSwimmer = if (legPart.contains(".")) legPart.substringAfter(".").trim() else legPart
                    if (legPos != null) {
                        map[Pair(laneNum, legPos)] = Pair(lap.swimmerId, legSwimmer)
                    }
                }
            }
        }
        map
    }

    // Helper data structure for relay legs per lane
    // LaneNumber -> List of Triple(legPosition, stroke, distance)
    val relayLegsInfo = remember(laps, s.targetDistanceMeters, s.splitDistanceMeters, s.strokeType) {
        val result = mutableMapOf<Int, List<RelayLegInfo>>()
        sortedLanes.forEach { laneNum ->
            val laneLaps = groupedByLane[laneNum]?.sortedBy { it.lapIndex } ?: emptyList()
            val distinctLegNumbers = laneLaps.mapNotNull { lap ->
                if (lap.swimmerName.contains(" • ")) {
                    val legPart = lap.swimmerName.substringAfter(" • ").trim()
                    legPart.substringBefore(".").trim().toIntOrNull()
                } else null
            }.distinct().sorted()

            val legCount = if (distinctLegNumbers.isNotEmpty()) distinctLegNumbers.size else 4
            val legDist = (s.targetDistanceMeters / legCount.coerceAtLeast(1)).coerceAtLeast(s.splitDistanceMeters)
            val isLagen = PersonalBestEvaluator.isSameStroke(s.strokeType, "Lagen")

            val list = (1..legCount).map { pos ->
                val stroke = if (isLagen) {
                    PersonalBestEvaluator.LAGEN_STAFFEL_STROKES.getOrElse((pos - 1) % 4) { "Freistil" }
                } else {
                    s.strokeType
                }
                RelayLegInfo(position = pos, stroke = stroke, distanceMeters = legDist)
            }
            result[laneNum] = list
        }
        result
    }

    // Picker state: if targetLeg is null -> picking for single lane; if targetLeg != null -> picking for relay leg
    var activePickerTarget by remember { mutableStateOf<PickerTarget?>(null) }
    var swimmerSearchQuery by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF101B2B),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("edit_session_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Trainingseinheit bearbeiten",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = TextWhite
                )

                Text(
                    text = "${TimeFormatter.formatDate(s.timestamp)} • ${s.targetDistanceMeters}m ${s.strokeType}" +
                            if (isRelaySession) " (Staffel)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                // Title Input
                Text(
                    text = "Titel der Einheit:",
                    style = MaterialTheme.typography.labelMedium,
                    color = AquaPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AquaPrimary,
                        unfocusedBorderColor = Color(0xFF22384F),
                        focusedContainerColor = Color(0xFF0C1623),
                        unfocusedContainerColor = Color(0xFF0C1623)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_session_title_field")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isRelaySession) "Staffeln & Starter zuweisen:" else "Bahnen & Athleten zuweisen:",
                    style = MaterialTheme.typography.labelMedium,
                    color = AquaPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                sortedLanes.forEach { laneNum ->
                    val laneLaps = groupedByLane[laneNum] ?: emptyList()
                    val isLaneRelay = isRelaySession || laneLaps.any { it.swimmerName.contains(" • ") }

                    if (!isLaneRelay) {
                        // EINZELBAHN CARD
                        val currentAssignment = singleLaneMap[laneNum] ?: Pair(null, "Bahn $laneNum")
                        val currentName = currentAssignment.second
                        val isAssigned = currentAssignment.first != null || (currentName != "Bahn $laneNum" && currentName.isNotBlank())

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF0C1623),
                            border = BorderStroke(1.dp, if (isAssigned) AquaPrimary.copy(alpha = 0.5f) else Color(0xFF1E2D40)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AquaPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(height = 36.dp, width = 68.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = "Bahn $laneNum",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = AquaPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isAssigned) Color(0xFF162536) else Color(0xFF121F2D),
                                    border = BorderStroke(1.dp, if (isAssigned) AquaPrimary.copy(alpha = 0.4f) else Color(0xFF1F3347)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clickable {
                                            swimmerSearchQuery = ""
                                            activePickerTarget = PickerTarget.SingleLane(laneNum)
                                        }
                                        .testTag("pick_swimmer_btn_${laneNum}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isAssigned) AquaPrimary else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isAssigned) currentName else "Athlet wählen...",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isAssigned) FontWeight.SemiBold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            ),
                                            color = if (isAssigned) TextWhite else Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (isAssigned) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            singleLaneMap[laneNum] = Pair(null, "Bahn $laneNum")
                                        },
                                        modifier = Modifier.size(36.dp).testTag("clear_lane_btn_${laneNum}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Zurücksetzen",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // STAFFELBAHN CARD
                        val currentTeamName = laneTeamNameMap[laneNum] ?: "Staffel Bahn $laneNum"
                        val legs = relayLegsInfo[laneNum] ?: emptyList()

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0C1623),
                            border = BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Header: Lane badge + Team name field
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AquaPrimary.copy(alpha = 0.18f),
                                        modifier = Modifier.size(height = 36.dp, width = 68.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text(
                                                text = "Bahn $laneNum",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = AquaPrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedTextField(
                                        value = currentTeamName,
                                        onValueChange = { laneTeamNameMap[laneNum] = it },
                                        singleLine = true,
                                        label = { Text("Staffel-Name", fontSize = 11.sp, color = Color.Gray) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite,
                                            focusedBorderColor = AquaPrimary,
                                            unfocusedBorderColor = Color(0xFF22384F),
                                            focusedContainerColor = Color(0xFF101B2B),
                                            unfocusedContainerColor = Color(0xFF101B2B)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text(
                                    text = "Starter einteilen:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )

                                // Grid / Rows of Relay Legs
                                legs.chunked(2).forEach { legPair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        legPair.forEach { legInfo ->
                                            val legAssignment = relayLegMap[Pair(laneNum, legInfo.position)]
                                            val swimmerId = legAssignment?.first
                                            val swimmerName = legAssignment?.second ?: "Starter ${legInfo.position}"
                                            val hasSwimmer = swimmerId != null && swimmerId > 0

                                            val shortStroke = when {
                                                legInfo.stroke.equals("Schmetterling", ignoreCase = true) -> "Schmett."
                                                else -> legInfo.stroke
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (hasSwimmer) Color(0xFF13273A) else Color(0xFF0E1A29),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (hasSwimmer) PaceGreen.copy(alpha = 0.65f) else Color(0xFF1E344B)
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        swimmerSearchQuery = ""
                                                        activePickerTarget = PickerTarget.RelayLeg(laneNum, legInfo.position, legInfo.stroke)
                                                    }
                                                    .testTag("pick_relay_swimmer_${laneNum}_${legInfo.position}")
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                                                        Text(
                                                            text = "${legInfo.position}. $shortStroke (${legInfo.distanceMeters}m)",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (hasSwimmer) PaceGreen else AquaPrimary,
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            text = if (hasSwimmer) swimmerName else "Starter wählen…",
                                                            fontSize = 12.5.sp,
                                                            fontWeight = if (hasSwimmer) FontWeight.SemiBold else FontWeight.Normal,
                                                            color = if (hasSwimmer) TextWhite else Color.Gray,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    if (hasSwimmer) {
                                                        IconButton(
                                                            onClick = {
                                                                relayLegMap[Pair(laneNum, legInfo.position)] = Pair(null, "Starter ${legInfo.position}")
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Starter entfernen",
                                                                tint = Color.Gray,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = null,
                                                            tint = AquaPrimary.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (legPair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Abbrechen", color = Color.LightGray)
                    }

                    Button(
                        onClick = {
                            val combinedLaneMap = sortedLanes.associateWith { laneNum ->
                                if (isRelaySession) {
                                    val teamName = laneTeamNameMap[laneNum] ?: "Staffel Bahn $laneNum"
                                    Pair(null, teamName)
                                } else {
                                    singleLaneMap[laneNum] ?: Pair(null, "Bahn $laneNum")
                                }
                            }
                            onSave(title, combinedLaneMap, relayLegMap.toMap())
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("edit_session_save_btn")
                    ) {
                        Text("Speichern", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal Swimmer Picker Dialog when activePickerTarget is not null
    if (activePickerTarget != null) {
        val target = activePickerTarget!!
        val filteredSwimmers = remember(swimmers, swimmerSearchQuery) {
            swimmers.asSequence()
                .filter { swimmer ->
                    swimmerSearchQuery.isBlank() || swimmer.name.contains(swimmerSearchQuery, ignoreCase = true)
                }
                .sortedBy { it.name.lowercase() }
                .toList()
        }

        // Swimmer IDs assigned elsewhere in this session
        val alreadyAssignedSwimmers = remember(singleLaneMap, relayLegMap, target) {
            buildMap<Long, String> {
                when (target) {
                    is PickerTarget.SingleLane -> {
                        singleLaneMap.forEach { (laneNum, assignment) ->
                            if (laneNum != target.laneNumber && assignment.first != null) {
                                put(assignment.first!!, "Bahn $laneNum")
                            }
                        }
                    }
                    is PickerTarget.RelayLeg -> {
                        relayLegMap.forEach { (key, assignment) ->
                            val (laneNum, legPos) = key
                            if ((laneNum != target.laneNumber || legPos != target.legPosition) && assignment.first != null) {
                                put(assignment.first!!, "Bahn $laneNum • ${legPos}. Starter")
                            }
                        }
                    }
                }
            }
        }

        val dialogTitle = when (target) {
            is PickerTarget.SingleLane -> "Athlet für Bahn ${target.laneNumber} wählen"
            is PickerTarget.RelayLeg -> "Athlet für ${target.legPosition}. Starter (${target.stroke}) wählen"
        }

        Dialog(onDismissRequest = { activePickerTarget = null }) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0E1A29),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = dialogTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = swimmerSearchQuery,
                        onValueChange = { swimmerSearchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Athlet suchen...", color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = AquaPrimary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (swimmerSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { swimmerSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Löschen", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color(0xFF22384F),
                            focusedContainerColor = Color(0xFF0C1623),
                            unfocusedContainerColor = Color(0xFF0C1623)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredSwimmers.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Text(
                                text = if (swimmers.isEmpty()) "Keine Athleten angelegt" else "Kein Athlet für Suche gefunden",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredSwimmers, key = { it.id }) { swimmer ->
                                val assignedPlace = alreadyAssignedSwimmers[swimmer.id]
                                val isAlreadyAssigned = assignedPlace != null

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAlreadyAssigned) Color(0xFF0A131F) else Color(0xFF142436),
                                    border = BorderStroke(1.dp, if (isAlreadyAssigned) Color(0xFF182635) else Color(0xFF22384F)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isAlreadyAssigned) {
                                            when (target) {
                                                is PickerTarget.SingleLane -> {
                                                    singleLaneMap[target.laneNumber] = Pair(swimmer.id, swimmer.name)
                                                }
                                                is PickerTarget.RelayLeg -> {
                                                    relayLegMap[Pair(target.laneNumber, target.legPosition)] = Pair(swimmer.id, swimmer.name)
                                                }
                                            }
                                            activePickerTarget = null
                                        }
                                        .testTag("swimmer_picker_item_${swimmer.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                text = swimmer.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isAlreadyAssigned) FontWeight.Normal else FontWeight.SemiBold
                                                ),
                                                color = if (isAlreadyAssigned) Color.Gray else TextWhite
                                            )
                                            if (isAlreadyAssigned) {
                                                Text(
                                                    text = "Bereits auf $assignedPlace eingeteilt",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                    color = Color(0xFFFFB74D)
                                                )
                                            } else if (!swimmer.groupName.isNullOrBlank()) {
                                                Text(
                                                    text = swimmer.groupName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AquaPrimary
                                                )
                                            }
                                        }

                                        if (isAlreadyAssigned) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF251F14)
                                            ) {
                                                Text(
                                                    text = "BELEGT",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFFD600),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { activePickerTarget = null }) {
                            Text("Schließen", color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

private sealed interface PickerTarget {
    data class SingleLane(val laneNumber: Int) : PickerTarget
    data class RelayLeg(val laneNumber: Int, val legPosition: Int, val stroke: String) : PickerTarget
}

private data class RelayLegInfo(
    val position: Int,
    val stroke: String,
    val distanceMeters: Int
)
