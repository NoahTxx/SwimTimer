package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SessionWithLaps
import com.example.data.model.SwimmerEntity
import com.example.ui.StopwatchViewModel
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.DeepOceanBackground
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.TextWhite
import com.example.util.TimeFormatter

@Composable
fun SwimmerDetailScreen(
    swimmer: SwimmerEntity,
    allSessions: List<SessionWithLaps>,
    modifier: Modifier = Modifier,
    isActiveTab: Boolean = true,
    availableGroups: List<String> = emptyList(),
    onBack: () -> Unit,
    onUpdateSwimmer: (SwimmerEntity) -> Unit,
    onDeleteSwimmer: (SwimmerEntity) -> Unit,
) {
    BackHandler(enabled = isActiveTab) { onBack() }

    var isEditDialogOpen by remember { mutableStateOf(value = false) }
    var isDeleteConfirmOpen by remember { mutableStateOf(value = false) }

    // Calculate Dynamic PBs from both manual entry and session history
    val dynamicPbs = remember(swimmer, allSessions) {
        val pbs = mutableMapOf<Pair<String, Int>, Long>()
        
        // 1. Fill from manual entry
        val fav = swimmer.favoriteStroke
        swimmer.pb25mMillis?.let { pbs[fav to 25] = it }
        swimmer.pb50mMillis?.let { pbs[fav to 50] = it }
        swimmer.pb100mMillis?.let { pbs[fav to 100] = it }
        swimmer.pb100mLagenMillis?.let { pbs["Lagen" to 100] = it }
        swimmer.pb200mLagenMillis?.let { pbs["Lagen" to 200] = it }
        swimmer.pb400mLagenMillis?.let { pbs["Lagen" to 400] = it }
        
        // 2. Override with better times from history
        val swimmerSessionsForPb = allSessions.filter { sessionWithLaps ->
            sessionWithLaps.laps.any { it.swimmerId == swimmer.id }
        }
        swimmerSessionsForPb.forEach { sessionLaps ->
            val s = sessionLaps.session
            val laps = sessionLaps.laps.filter { it.swimmerId == swimmer.id }.sortedBy { it.lapIndex }
            
            // Check if full distance was swum
            val totalSplitsNeeded = (s.targetDistanceMeters / s.splitDistanceMeters).coerceAtLeast(1)
            val completedFullDistance = laps.size >= totalSplitsNeeded

            val finishTime = if (completedFullDistance) laps.lastOrNull()?.splitTimeMillis ?: 0L else 0L
            if (finishTime > 0) {
                val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")
                val matchedStroke = strokeOptions.find { StopwatchViewModel.isSameStroke(it, s.strokeType) } ?: s.strokeType
                val key = matchedStroke to s.targetDistanceMeters
                val currentPb = pbs[key]
                if ((currentPb == null) || (finishTime < currentPb)) {
                    pbs[key] = finishTime
                }
            }
        }
        pbs
    }

    // Filter sessions that have laps for this swimmer
    val swimmerSessions = remember(allSessions, swimmer.id) {
        allSessions.filter { sessionWithLaps ->
            sessionWithLaps.laps.any { it.swimmerId == swimmer.id }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOceanBackground)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("swimmer_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = AquaPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Athletenprofil",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(
                        onClick = { isEditDialogOpen = true },
                        modifier = Modifier.testTag("edit_swimmer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Bearbeiten",
                            tint = AquaPrimary
                        )
                    }
                    IconButton(
                        onClick = { isDeleteConfirmOpen = true },
                        modifier = Modifier.testTag("delete_swimmer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Löschen",
                            tint = Color(0xFFFF5252)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Swimmer Hero Card
                item(key = "hero_card", contentType = "HeroCard") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = OceanSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(swimmer.colorHex),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = swimmer.name.take(1).uppercase(),
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = swimmer.name,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        ),
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF132A38)
                                        ) {
                                            Text(
                                                text = "Hauptlage: ${swimmer.favoriteStroke}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = AquaPrimary,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        if (!swimmer.groupName.isNullOrBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = AquaPrimary.copy(alpha = 0.15f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = swimmer.groupName,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = AquaPrimary,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Best Times (PB) Section with Stroke Selector
                            var selectedPbStroke by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(swimmer.favoriteStroke) }
                            val pbStrokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pbStrokeOptions.forEach { stroke ->
                                    FilterChip(
                                        selected = StopwatchViewModel.isSameStroke(selectedPbStroke, stroke),
                                        onClick = { selectedPbStroke = stroke },
                                        label = { Text(stroke, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AquaPrimary,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val isLagenSelected = StopwatchViewModel.isSameStroke(selectedPbStroke, "Lagen")

                            if (isLagenSelected) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(100, 200, 400).forEach { dist ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF0F1E2E),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("${dist}m Lagen", style = MaterialTheme.typography.labelSmall, color = Color.LightGray, maxLines = 1)
                                                val pbMillis = dynamicPbs["Lagen" to dist]
                                                val t = pbMillis?.let { TimeFormatter.formatMillis(it) } ?: "--:--"
                                                Text(t, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp), color = AquaPrimary, maxLines = 1)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Lagen Splits inside the Lagen Tab
                                var selectedLagenSplitMode by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(200) }

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF0A1828),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B3550)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "LAGEN SPLITS",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.8.sp
                                                    ),
                                                    color = AquaPrimary,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "Zwischenzeiten pro Schwimmart",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = Color.Gray,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf(100, 200, 400).forEach { mode ->
                                                val isSelected = selectedLagenSplitMode == mode
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSelected) AquaPrimary.copy(alpha = 0.2f) else Color(0xFF162B3D),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AquaPrimary else Color.Transparent),
                                                    modifier = Modifier.weight(1f).clickable { selectedLagenSplitMode = mode }
                                                ) {
                                                    Text(
                                                        text = "${mode}m",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isSelected) AquaPrimary else Color.Gray,
                                                        modifier = Modifier.padding(vertical = 6.dp),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // The split items
                                        val splits = when (selectedLagenSplitMode) {
                                            100 -> listOf(
                                                "Schmetterling" to swimmer.lagenFlySplitMillis,
                                                "Rücken" to swimmer.lagenBackSplitMillis,
                                                "Brust" to swimmer.lagenBreastSplitMillis,
                                                "Freistil" to swimmer.lagenFreeSplitMillis
                                            )
                                            200 -> listOf(
                                                "Schmetterling" to swimmer.pb200mLagenFlySplitMillis,
                                                "Rücken" to swimmer.pb200mLagenBackSplitMillis,
                                                "Brust" to swimmer.pb200mLagenBreastSplitMillis,
                                                "Freistil" to swimmer.pb200mLagenFreeSplitMillis
                                            )
                                            else -> listOf(
                                                "Schmetterling" to swimmer.pb400mLagenFlySplitMillis,
                                                "Rücken" to swimmer.pb400mLagenBackSplitMillis,
                                                "Brust" to swimmer.pb400mLagenBreastSplitMillis,
                                                "Freistil" to swimmer.pb400mLagenFreeSplitMillis
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            splits.take(2).forEach { (name, time) ->
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                    Text(time?.let { TimeFormatter.formatMillis(it) } ?: "--:--", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            splits.drop(2).forEach { (name, time) ->
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                    Text(time?.let { TimeFormatter.formatMillis(it) } ?: "--:--", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(25, 50, 100).forEach { dist ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF0F1E2E),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("${dist}m", style = MaterialTheme.typography.labelSmall, color = Color.LightGray, maxLines = 1)
                                                val pbMillis = dynamicPbs[selectedPbStroke to dist]
                                                val t = pbMillis?.let { TimeFormatter.formatMillis(it, shortFormat = dist <= 50) } ?: "--:--"
                                                Text(t, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp), color = AquaPrimary, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // History Title Header
                item(key = "history_header", contentType = "HistoryHeader") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = AquaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Persönliche Historie",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "${swimmerSessions.size} Einheiten",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                    }
                }

                // Sessions List
                if (swimmerSessions.isEmpty()) {
                    item(key = "empty_history", contentType = "EmptyHistory") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Noch keine Einheiten gestoppt",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Weise ${swimmer.name} auf der Stoppuhr einer Bahn zu und speichere eine Einheit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(
                        items = swimmerSessions,
                        key = { it.session.id },
                        contentType = { "SwimmerSessionHistoryCard" }
                    ) { sessionWithLaps ->
                        SwimmerSessionHistoryCard(
                            sessionWithLaps = sessionWithLaps,
                            swimmer = swimmer,
                            dynamicPbs = dynamicPbs
                        )
                    }
                }
            }
        }

        // Edit Dialog
        if (isEditDialogOpen) {
            EditSwimmerDialog(
                swimmer = swimmer,
                availableGroups = availableGroups,
                onDismiss = { isEditDialogOpen = false }
            ) { updated ->
                onUpdateSwimmer(updated)
                isEditDialogOpen = false
            }
        }

        // Delete Confirm Dialog
        if (isDeleteConfirmOpen) {
            AlertDialog(
                onDismissRequest = { isDeleteConfirmOpen = false },
                title = { Text("Schwimmer löschen", color = TextWhite) },
                text = { Text("Möchtest du ${swimmer.name} wirklich aus dem Team entfernen?", color = Color.LightGray) },
                containerColor = Color(0xFF101B2B),
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteSwimmer(swimmer)
                            isDeleteConfirmOpen = false
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Löschen", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { isDeleteConfirmOpen = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Abbrechen", color = Color.LightGray)
                    }
                }
            )
        }
    }
}

@Composable
private fun SwimmerSessionHistoryCard(
    sessionWithLaps: SessionWithLaps,
    swimmer: SwimmerEntity,
    dynamicPbs: Map<Pair<String, Int>, Long>
) {
    var isExpanded by remember { mutableStateOf(value = false) }
    val s = sessionWithLaps.session
    val swimmerLaps = remember(sessionWithLaps, swimmer.id) {
        sessionWithLaps.laps
            .filter { it.swimmerId == swimmer.id }
            .sortedBy { it.lapIndex }
    }

    val finishTime = swimmerLaps.lastOrNull()?.splitTimeMillis ?: 0L

    val isSessionPb = remember(finishTime, s.strokeType, s.targetDistanceMeters, dynamicPbs) {
        if (finishTime <= 0L) false
        else {
            val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")
            val matchedStroke = strokeOptions.find { StopwatchViewModel.isSameStroke(it, s.strokeType) } ?: s.strokeType
            dynamicPbs[matchedStroke to s.targetDistanceMeters] == finishTime
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OceanSurface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSessionPb) Color(0xFFFFD600) else Color(0xFF1B2E46)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = s.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isSessionPb) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFD600).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD600))
                            ) {
                                Text(
                                    text = "★ PB",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                    color = Color(0xFFFFD600),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${TimeFormatter.formatDate(s.timestamp)} • ${s.poolLengthMeters}m Becken",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = TimeFormatter.formatMillis(finishTime),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isSessionPb) Color(0xFFFFD600) else AquaPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.LightGray
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = Color(0xFF1E2D40), modifier = Modifier.padding(bottom = 8.dp))
                    swimmerLaps.forEach { lap ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Split ${lap.lapIndex}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Runde: ${TimeFormatter.formatMillis(lap.lapTimeMillis, shortFormat = true)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = TextWhite
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gesamt: ${TimeFormatter.formatMillis(lap.splitTimeMillis)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = AquaPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSwimmerDialog(
    swimmer: SwimmerEntity,
    availableGroups: List<String>,
    onDismiss: () -> Unit,
    onSave: (SwimmerEntity) -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var name by remember { mutableStateOf(swimmer.name) }
    var groupName by remember { mutableStateOf(swimmer.groupName ?: "") }
    var stroke by remember { mutableStateOf(swimmer.favoriteStroke) }
    var isStrokeDropdownExpanded by remember { mutableStateOf(false) }

    var pb25Text by remember { mutableStateOf(swimmer.pb25mMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb50Text by remember { mutableStateOf(swimmer.pb50mMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb100Text by remember { mutableStateOf(swimmer.pb100mMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb100LagenText by remember { mutableStateOf(swimmer.pb100mLagenMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb200LagenText by remember { mutableStateOf(swimmer.pb200mLagenMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb400LagenText by remember { mutableStateOf(swimmer.pb400mLagenMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }

    var pb200FlyText by remember { mutableStateOf(swimmer.pb200mLagenFlySplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb200BackText by remember { mutableStateOf(swimmer.pb200mLagenBackSplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb200BreastText by remember { mutableStateOf(swimmer.pb200mLagenBreastSplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb200FreeText by remember { mutableStateOf(swimmer.pb200mLagenFreeSplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }

    var pb400FlyText by remember { mutableStateOf(swimmer.pb400mLagenFlySplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb400BackText by remember { mutableStateOf(swimmer.pb400mLagenBackSplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb400BreastText by remember { mutableStateOf(swimmer.pb400mLagenBreastSplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb400FreeText by remember { mutableStateOf(swimmer.pb400mLagenFreeSplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }

    var flySplitText by remember { mutableStateOf(swimmer.lagenFlySplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var backSplitText by remember { mutableStateOf(swimmer.lagenBackSplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var breastSplitText by remember { mutableStateOf(swimmer.lagenBreastSplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var freeSplitText by remember { mutableStateOf(swimmer.lagenFreeSplitMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }

    val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF101B2B),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_swimmer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Athlet bearbeiten",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AquaPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                var isGroupDropdownExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = isGroupDropdownExpanded,
                    onExpandedChange = { isGroupDropdownExpanded = !isGroupDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (groupName.isEmpty()) "Keine Gruppe" else groupName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trainingsgruppe") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupDropdownExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color(0xFF1B2E46)
                        ),
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isGroupDropdownExpanded,
                        onDismissRequest = { isGroupDropdownExpanded = false },
                        containerColor = Color(0xFF101B2B)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Keine Gruppe", color = TextWhite) },
                            onClick = {
                                groupName = ""
                                isGroupDropdownExpanded = false
                            },
                            modifier = Modifier.sizeIn(minHeight = 48.dp)
                        )
                        availableGroups.forEach { grp ->
                            DropdownMenuItem(
                                text = { Text(grp, color = TextWhite) },
                                onClick = {
                                    groupName = grp
                                    isGroupDropdownExpanded = false
                                },
                                modifier = Modifier.sizeIn(minHeight = 48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                ExposedDropdownMenuBox(
                    expanded = isStrokeDropdownExpanded,
                    onExpandedChange = { isStrokeDropdownExpanded = !isStrokeDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = stroke,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hauptschwimmart") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStrokeDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AquaPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = isStrokeDropdownExpanded,
                        onDismissRequest = { isStrokeDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF142436))
                    ) {
                        strokeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = TextWhite) },
                                onClick = {
                                    stroke = option
                                    isStrokeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("PERSÖNLICHE BESTZEITEN (PB)", style = MaterialTheme.typography.labelSmall, color = AquaPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = pb25Text,
                        onValueChange = { pb25Text = it },
                        label = { Text("PB 25m", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("12.50") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb50Text,
                        onValueChange = { pb50Text = it },
                        label = { Text("PB 50m", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("26.80") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb100Text,
                        onValueChange = { pb100Text = it },
                        label = { Text("PB 100m", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("58.20") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("LAGEN WETTKAMPFZEITEN", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD600))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = pb100LagenText,
                        onValueChange = { pb100LagenText = it },
                        label = { Text("100mLagen", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("1:04.50") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb200LagenText,
                        onValueChange = { pb200LagenText = it },
                        label = { Text("200mLagen", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("2:18.50") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb400LagenText,
                        onValueChange = { pb400LagenText = it },
                        label = { Text("400mLagen", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        placeholder = { Text("4:55.00") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Abbrechen", color = Color.LightGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    swimmer.copy(
                                        name = name.trim(),
                                        groupName = groupName.trim().ifEmpty { null },
                                        favoriteStroke = stroke,
                                        pb25mMillis = TimeFormatter.parseTimeInputToMillis(pb25Text),
                                        pb50mMillis = TimeFormatter.parseTimeInputToMillis(pb50Text),
                                        pb100mMillis = TimeFormatter.parseTimeInputToMillis(pb100Text),
                                        pb100mLagenMillis = TimeFormatter.parseTimeInputToMillis(pb100LagenText),
                                        pb200mLagenMillis = TimeFormatter.parseTimeInputToMillis(pb200LagenText),
                                        pb400mLagenMillis = TimeFormatter.parseTimeInputToMillis(pb400LagenText),
                                        lagenFlySplitMillis = TimeFormatter.parseTimeInputToMillis(flySplitText),
                                        lagenBackSplitMillis = TimeFormatter.parseTimeInputToMillis(backSplitText),
                                        lagenBreastSplitMillis = TimeFormatter.parseTimeInputToMillis(breastSplitText),
                                        lagenFreeSplitMillis = TimeFormatter.parseTimeInputToMillis(freeSplitText),
                                        pb200mLagenFlySplitMillis = TimeFormatter.parseTimeInputToMillis(pb200FlyText),
                                        pb200mLagenBackSplitMillis = TimeFormatter.parseTimeInputToMillis(pb200BackText),
                                        pb200mLagenBreastSplitMillis = TimeFormatter.parseTimeInputToMillis(pb200BreastText),
                                        pb200mLagenFreeSplitMillis = TimeFormatter.parseTimeInputToMillis(pb200FreeText),
                                        pb400mLagenFlySplitMillis = TimeFormatter.parseTimeInputToMillis(pb400FlyText),
                                        pb400mLagenBackSplitMillis = TimeFormatter.parseTimeInputToMillis(pb400BackText),
                                        pb400mLagenBreastSplitMillis = TimeFormatter.parseTimeInputToMillis(pb400BreastText),
                                        pb400mLagenFreeSplitMillis = TimeFormatter.parseTimeInputToMillis(pb400FreeText)
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
                    ) {
                        Text("Speichern", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
