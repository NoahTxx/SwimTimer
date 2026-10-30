package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.PlatformTextStyle
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

    // Pre-filter sessions that have laps for this swimmer
    val swimmerSessions = remember(allSessions, swimmer.id) {
        allSessions.filter { sessionWithLaps ->
            sessionWithLaps.laps.any { it.swimmerId == swimmer.id }
        }
    }

    // Calculate Dynamic PBs from both manual entry and session history
    val dynamicPbs = remember(swimmer, swimmerSessions) {
        val pbs = mutableMapOf<Pair<String, Int>, Long>()

        // 1. Fill from custom PBs map if present
        val customMap = swimmer.parseCustomPbsMap()
        pbs.putAll(customMap)

        // 2. Fill from legacy manual entry fields
        val fav = if (swimmer.favoriteStroke == "Lagen") "Freistil" else swimmer.favoriteStroke
        swimmer.pb25mMillis?.let { if (pbs[fav to 25] == null || it < pbs[fav to 25]!!) pbs[fav to 25] = it }
        swimmer.pb50mMillis?.let { if (pbs[fav to 50] == null || it < pbs[fav to 50]!!) pbs[fav to 50] = it }
        swimmer.pb100mMillis?.let { if (pbs[fav to 100] == null || it < pbs[fav to 100]!!) pbs[fav to 100] = it }
        swimmer.pb100mLagenMillis?.let { if (pbs["Lagen" to 100] == null || it < pbs["Lagen" to 100]!!) pbs["Lagen" to 100] = it }
        swimmer.pb200mLagenMillis?.let { if (pbs["Lagen" to 200] == null || it < pbs["Lagen" to 200]!!) pbs["Lagen" to 200] = it }
        swimmer.pb400mLagenMillis?.let { if (pbs["Lagen" to 400] == null || it < pbs["Lagen" to 400]!!) pbs["Lagen" to 400] = it }
        
        // 3. Override with better times from history (including multi-series runs and relay legs!)
        val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")
        swimmerSessions.forEach { sessionLaps ->
            val performances = com.example.domain.PersonalBestEvaluator.extractSwimmerSessionPerformances(
                sessionWithLaps = sessionLaps,
                swimmerId = swimmer.id
            )
            performances.forEach { perf ->
                val matchedStroke = strokeOptions.find { StopwatchViewModel.isSameStroke(it, perf.strokeType) } ?: perf.strokeType
                val key = matchedStroke to perf.distanceMeters
                if (perf.bestTimeMillis > 0L) {
                    val currentPb = pbs[key]
                    if (currentPb == null || perf.bestTimeMillis < currentPb) {
                        pbs[key] = perf.bestTimeMillis
                    }
                }
            }
        }
        pbs
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

                            // Best Times (PB) Section with Single-Glance Stroke Selector (5 Tabs in 1 Row)
                            var selectedPbStroke by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(if (swimmer.favoriteStroke == "Lagen") "Freistil" else swimmer.favoriteStroke) }
                            val pbStrokeOptions = listOf(
                                "Freistil" to "Freistil",
                                "Brust" to "Brust",
                                "Rücken" to "Rücken",
                                "Schmetterling" to "Schmett.",
                                "Lagen" to "Lagen"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                pbStrokeOptions.forEach { (strokeKey, strokeLabel) ->
                                    val isSelected = StopwatchViewModel.isSameStroke(selectedPbStroke, strokeKey)
                                    val strokeCatColor = when (strokeKey) {
                                        "Freistil" -> Color(0xFF00E5FF)
                                        "Brust" -> Color(0xFF00E676)
                                        "Rücken" -> Color(0xFFFFD600)
                                        "Schmetterling" -> Color(0xFFFF5252)
                                        else -> Color(0xFFAB47BC)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) strokeCatColor else Color(0xFF122334),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) strokeCatColor else Color(0xFF1B2E46)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedPbStroke = strokeKey }
                                    ) {
                                        Text(
                                            text = strokeLabel,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.sp
                                            ),
                                            color = if (isSelected) Color.Black else TextWhite.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
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
                                                Text("${dist}m", style = MaterialTheme.typography.labelSmall, color = Color.LightGray, maxLines = 1)
                                                val pbMillis = dynamicPbs["Lagen" to dist]
                                                val t = pbMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "--:--"
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
                                                    Text(time?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "--:--", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            splits.drop(2).forEach { (name, time) ->
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(name, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                    Text(time?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "--:--", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(25, 50, 100, 200).forEach { dist ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF0F1E2E),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("${dist}m", style = MaterialTheme.typography.labelSmall, color = Color.LightGray, maxLines = 1)
                                                val pbMillis = dynamicPbs[selectedPbStroke to dist]
                                                val t = pbMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "--:--"
                                                Text(t, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp), color = AquaPrimary, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Langstrecke Card (Accordion)
                item(key = "langstrecke_card", contentType = "LangstreckeCard") {
                    var isLangstreckeExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = OceanSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isLangstreckeExpanded = !isLangstreckeExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = AquaPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Langstrecke (400m, 800m, 1500m)",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                                        color = TextWhite
                                    )
                                }
                                Icon(
                                    imageVector = if (isLangstreckeExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = AquaPrimary
                                )
                            }

                            AnimatedVisibility(visible = isLangstreckeExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    HorizontalDivider(color = Color(0xFF1B2E46), modifier = Modifier.padding(bottom = 12.dp))

                                    val longDistanceItems = listOf(
                                        "400m" to ("Freistil" to 400),
                                        "800m" to ("Freistil" to 800),
                                        "1500m" to ("Freistil" to 1500)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        longDistanceItems.forEach { (label, key) ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFF0F1E2E),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(label, style = MaterialTheme.typography.labelSmall, color = Color.LightGray, maxLines = 1)
                                                    val pbMillis = dynamicPbs[key]
                                                    val t = pbMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp), color = AquaPrimary, maxLines = 1)
                                                }
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
    var isExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(value = false) }
    val s = sessionWithLaps.session
    val performances = remember(sessionWithLaps, swimmer.id) {
        com.example.domain.PersonalBestEvaluator.extractSwimmerSessionPerformances(
            sessionWithLaps = sessionWithLaps,
            swimmerId = swimmer.id
        )
    }
    val primaryPerf = performances.minByOrNull { if (it.bestTimeMillis > 0L) it.bestTimeMillis else Long.MAX_VALUE }
    val finishTime = primaryPerf?.bestTimeMillis ?: 0L
    val perfStroke = primaryPerf?.strokeType ?: s.strokeType
    val perfDistance = primaryPerf?.distanceMeters ?: s.targetDistanceMeters
    val isRelayLeg = primaryPerf?.isRelayLeg == true
    val relayLegLabel = primaryPerf?.relayLegLabel

    val displayLaps = remember(performances) {
        performances.flatMap { it.laps }
    }
    val hasMultipleSeries = remember(displayLaps) { displayLaps.any { it.seriesIndex > 1 } }

    val isSessionPb = remember(finishTime, perfStroke, perfDistance, dynamicPbs) {
        if (finishTime <= 0L) false
        else {
            val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")
            val matchedStroke = strokeOptions.find { StopwatchViewModel.isSameStroke(it, perfStroke) } ?: perfStroke
            dynamicPbs[matchedStroke to perfDistance] == finishTime
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD600),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "PB",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        ),
                                        color = Color(0xFFFFD600)
                                    )
                                }
                            }
                        }
                    }
                    if (!relayLegLabel.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = relayLegLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AquaPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                        text = TimeFormatter.formatMillis(finishTime, shortFormat = isRelayLeg && perfDistance <= 50),
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
                    val effectiveTargetDist = if (isRelayLeg) perfDistance else s.targetDistanceMeters
                    val effectiveSplitDist = if (isRelayLeg) {
                        (perfDistance / displayLaps.size.coerceAtLeast(1)).coerceAtLeast(s.splitDistanceMeters)
                    } else s.splitDistanceMeters

                    displayLaps.forEachIndexed { lapIdx, lapItem ->
                        val isLastLapInSeries = (lapItem == displayLaps.lastOrNull { it.seriesIndex == lapItem.seriesIndex })
                        val prevLapItem = if (lapIdx > 0) displayLaps[lapIdx - 1].takeIf { it.seriesIndex == lapItem.seriesIndex } else null
                        val prevCumDist = if (prevLapItem != null) {
                            val isPrevLast = (prevLapItem == displayLaps.lastOrNull { it.seriesIndex == prevLapItem.seriesIndex })
                            if (isPrevLast) effectiveTargetDist else (prevLapItem.lapIndex * effectiveSplitDist).coerceAtMost(effectiveTargetDist)
                        } else 0
                        val currentCumDist = if (isLastLapInSeries) {
                            effectiveTargetDist
                        } else {
                            (lapItem.lapIndex * effectiveSplitDist).coerceAtMost(effectiveTargetDist)
                        }
                        val lapDist = (currentCumDist - prevCumDist).coerceAtLeast(effectiveSplitDist)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRelayLeg) {
                                    "Staffel-Teilstrecke (${lapDist}m $perfStroke)"
                                } else if (displayLaps.size == 1) {
                                    if (hasMultipleSeries) "S${lapItem.seriesIndex} • Ziel (${lapDist}m)" else "Ziel (${lapDist}m)"
                                } else if (hasMultipleSeries) {
                                    "S${lapItem.seriesIndex} • Split ${lapItem.lapIndex} (${lapDist}m)"
                                } else {
                                    "Split ${lapItem.lapIndex} (${lapDist}m)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Einzel: ${TimeFormatter.formatMillis(lapItem.lapTimeMillis, shortFormat = true)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = TextWhite
                                )
                                if (!isRelayLeg || displayLaps.size > 1) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Gesamt: ${TimeFormatter.formatMillis(lapItem.splitTimeMillis)}",
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
}

@Composable
private fun EditSwimmerDialog(
    swimmer: SwimmerEntity,
    availableGroups: List<String>,
    onDismiss: () -> Unit,
    onSave: (SwimmerEntity) -> Unit
) {
    SwimmerFormDialog(
        initialSwimmer = swimmer,
        availableGroups = availableGroups,
        defaultGroup = swimmer.groupName,
        dialogTitle = "Athlet bearbeiten",
        confirmButtonText = "Speichern",
        dialogTestTag = "edit_swimmer_dialog",
        onDismiss = onDismiss,
        onConfirm = onSave
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwimmerFormDialog(
    initialSwimmer: SwimmerEntity? = null,
    availableGroups: List<String>,
    defaultGroup: String? = null,
    dialogTitle: String,
    confirmButtonText: String,
    dialogTestTag: String,
    onDismiss: () -> Unit,
    onConfirm: (SwimmerEntity) -> Unit
) {
    val swimmer = initialSwimmer ?: SwimmerEntity(
        id = 0,
        name = "",
        ageGroup = "Offen",
        favoriteStroke = "Freistil",
        groupName = defaultGroup
    )
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var name by remember { mutableStateOf(swimmer.name) }
    var groupName by remember { mutableStateOf(swimmer.groupName ?: "") }
    var stroke by remember { mutableStateOf(if (swimmer.favoriteStroke == "Lagen") "Freistil" else swimmer.favoriteStroke) }
    var isStrokeDropdownExpanded by remember { mutableStateOf(false) }

    val existingPbs = remember(swimmer) { swimmer.parseCustomPbsMap() }

    // Multi-Stroke PB State
    var free25Text by remember { mutableStateOf(existingPbs["Freistil" to 25]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Freistil") swimmer.pb25mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var free50Text by remember { mutableStateOf(existingPbs["Freistil" to 50]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Freistil") swimmer.pb50mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var free100Text by remember { mutableStateOf(existingPbs["Freistil" to 100]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Freistil") swimmer.pb100mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var free200Text by remember { mutableStateOf(existingPbs["Freistil" to 200]?.let { TimeFormatter.formatForInput(it) } ?: "") }

    var breast25Text by remember { mutableStateOf(existingPbs["Brust" to 25]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Brust") swimmer.pb25mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var breast50Text by remember { mutableStateOf(existingPbs["Brust" to 50]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Brust") swimmer.pb50mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var breast100Text by remember { mutableStateOf(existingPbs["Brust" to 100]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Brust") swimmer.pb100mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var breast200Text by remember { mutableStateOf(existingPbs["Brust" to 200]?.let { TimeFormatter.formatForInput(it) } ?: "") }

    var back25Text by remember { mutableStateOf(existingPbs["Rücken" to 25]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Rücken") swimmer.pb25mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var back50Text by remember { mutableStateOf(existingPbs["Rücken" to 50]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Rücken") swimmer.pb50mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var back100Text by remember { mutableStateOf(existingPbs["Rücken" to 100]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Rücken") swimmer.pb100mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var back200Text by remember { mutableStateOf(existingPbs["Rücken" to 200]?.let { TimeFormatter.formatForInput(it) } ?: "") }

    var fly25Text by remember { mutableStateOf(existingPbs["Schmetterling" to 25]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Schmetterling") swimmer.pb25mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var fly50Text by remember { mutableStateOf(existingPbs["Schmetterling" to 50]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Schmetterling") swimmer.pb50mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var fly100Text by remember { mutableStateOf(existingPbs["Schmetterling" to 100]?.let { TimeFormatter.formatForInput(it) } ?: (if (swimmer.favoriteStroke == "Schmetterling") swimmer.pb100mMillis?.let { TimeFormatter.formatForInput(it) } ?: "" else "")) }
    var fly200Text by remember { mutableStateOf(existingPbs["Schmetterling" to 200]?.let { TimeFormatter.formatForInput(it) } ?: "") }

    var free400Text by remember { mutableStateOf(existingPbs["Freistil" to 400]?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var free800Text by remember { mutableStateOf(existingPbs["Freistil" to 800]?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var free1500Text by remember { mutableStateOf(existingPbs["Freistil" to 1500]?.let { TimeFormatter.formatForInput(it) } ?: "") }

    var pb100LagenText by remember { mutableStateOf(existingPbs["Lagen" to 100]?.let { TimeFormatter.formatForInput(it) } ?: swimmer.pb100mLagenMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb200LagenText by remember { mutableStateOf(existingPbs["Lagen" to 200]?.let { TimeFormatter.formatForInput(it) } ?: swimmer.pb200mLagenMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }
    var pb400LagenText by remember { mutableStateOf(existingPbs["Lagen" to 400]?.let { TimeFormatter.formatForInput(it) } ?: swimmer.pb400mLagenMillis?.let { TimeFormatter.formatForInput(it) } ?: "") }

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

    var expandedStrokeSection by remember { mutableStateOf<String?>(stroke) }
    var isLagenSplitsExpanded by remember { mutableStateOf(false) }

    val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF101B2B),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(dialogTestTag)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextWhite,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- ABSCHNITT 1: STAMMDATEN ---
                Text(
                    text = "STAMMDATEN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = AquaPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AquaPrimary,
                        unfocusedBorderColor = Color(0xFF1B2E46)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_swimmer_name_input")
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
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupDropdownExpanded) },
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
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color(0xFF1B2E46)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .testTag("stroke_dropdown_field")
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

                Spacer(modifier = Modifier.height(16.dp))

                // --- ABSCHNITT 2: BESTZEITEN JE SCHWIMMART (KLAPPMENÜ) ---
                Text(
                    text = "BESTZEITEN JE SCHWIMMART (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = AquaPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                val timeKeyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                )
                val timeKeyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                )

                val strokeCategories = listOf(
                    Triple("Freistil", "Freistil / Kraul", Color(0xFF00E5FF)),
                    Triple("Brust", "Brust", Color(0xFF00E676)),
                    Triple("Rücken", "Rücken", Color(0xFFFFD600)),
                    Triple("Schmetterling", "Schmetterling / Delfin", Color(0xFFFF5252)),
                    Triple("Lagen", "Lagen (100m, 200m, 400m)", Color(0xFFAB47BC))
                )

                strokeCategories.forEach { (catKey, catLabel, catColor) ->
                    val isExpanded = expandedStrokeSection == catKey
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF132334),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isExpanded) catColor else Color(0xFF1B2E46)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                expandedStrokeSection = if (isExpanded) null else catKey
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(catColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = catLabel,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextWhite
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))

                                if (catKey == "Lagen") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = pb100LagenText,
                                            onValueChange = { pb100LagenText = it },
                                            label = { Text("100m", fontSize = 11.sp, maxLines = 1) },
                                            singleLine = true,
                                            keyboardOptions = timeKeyboardOptions,
                                            keyboardActions = timeKeyboardActions,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = pb200LagenText,
                                            onValueChange = { pb200LagenText = it },
                                            label = { Text("200m", fontSize = 11.sp, maxLines = 1) },
                                            singleLine = true,
                                            keyboardOptions = timeKeyboardOptions,
                                            keyboardActions = timeKeyboardActions,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = pb400LagenText,
                                            onValueChange = { pb400LagenText = it },
                                            label = { Text("400m", fontSize = 11.sp, maxLines = 1) },
                                            singleLine = true,
                                            keyboardOptions = timeKeyboardOptions,
                                            keyboardActions = timeKeyboardActions,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    val (v25, set25, v50, set50, v100, set100, v200, set200) = when (catKey) {
                                        "Brust" -> EditTuple8(breast25Text, { s: String -> breast25Text = s }, breast50Text, { s: String -> breast50Text = s }, breast100Text, { s: String -> breast100Text = s }, breast200Text, { s: String -> breast200Text = s })
                                        "Rücken" -> EditTuple8(back25Text, { s: String -> back25Text = s }, back50Text, { s: String -> back50Text = s }, back100Text, { s: String -> back100Text = s }, back200Text, { s: String -> back200Text = s })
                                        "Schmetterling" -> EditTuple8(fly25Text, { s: String -> fly25Text = s }, fly50Text, { s: String -> fly50Text = s }, fly100Text, { s: String -> fly100Text = s }, fly200Text, { s: String -> fly200Text = s })
                                        else -> EditTuple8(free25Text, { s: String -> free25Text = s }, free50Text, { s: String -> free50Text = s }, free100Text, { s: String -> free100Text = s }, free200Text, { s: String -> free200Text = s })
                                    }

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = v25,
                                                onValueChange = set25,
                                                label = { Text("25m", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                singleLine = true,
                                                keyboardOptions = timeKeyboardOptions,
                                                keyboardActions = timeKeyboardActions,
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = v50,
                                                onValueChange = set50,
                                                label = { Text("50m", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                singleLine = true,
                                                keyboardOptions = timeKeyboardOptions,
                                                keyboardActions = timeKeyboardActions,
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = v100,
                                                onValueChange = set100,
                                                label = { Text("100m", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                singleLine = true,
                                                keyboardOptions = timeKeyboardOptions,
                                                keyboardActions = timeKeyboardActions,
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = v200,
                                                onValueChange = set200,
                                                label = { Text("200m", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                singleLine = true,
                                                keyboardOptions = timeKeyboardOptions,
                                                keyboardActions = timeKeyboardActions,
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- ABSCHNITT 3: LAGEN TEILSTRECKEN (AUFKLAPPBAR) ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF142436),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLagenSplitsExpanded = !isLagenSplitsExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isLagenSplitsExpanded) "Lagen-Teilstrecken verbergen" else "Lagen-Teilstrecken eintragen",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AquaPrimary
                        )
                        Icon(
                            imageVector = if (isLagenSplitsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = AquaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isLagenSplitsExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "100m Lagen Teilstrecken (4 × 25m):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFFD600))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = flySplitText, onValueChange = { flySplitText = it }, label = { Text("1. Schmett") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFF5252), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = backSplitText, onValueChange = { backSplitText = it }, label = { Text("2. Rücken") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFFD600), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = breastSplitText, onValueChange = { breastSplitText = it }, label = { Text("3. Brust") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E676), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = freeSplitText, onValueChange = { freeSplitText = it }, label = { Text("4. Kraul") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "200m Lagen Teilstrecken (4 × 50m):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AquaPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = pb200FlyText, onValueChange = { pb200FlyText = it }, label = { Text("1. Schmett") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFF5252), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pb200BackText, onValueChange = { pb200BackText = it }, label = { Text("2. Rücken") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFFD600), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = pb200BreastText, onValueChange = { pb200BreastText = it }, label = { Text("3. Brust") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E676), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pb200FreeText, onValueChange = { pb200FreeText = it }, label = { Text("4. Kraul") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "400m Lagen Teilstrecken (4 × 100m):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF80D8FF))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = pb400FlyText, onValueChange = { pb400FlyText = it }, label = { Text("1. Schmett") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFF5252), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pb400BackText, onValueChange = { pb400BackText = it }, label = { Text("2. Rücken") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFFD600), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = pb400BreastText, onValueChange = { pb400BreastText = it }, label = { Text("3. Brust") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E676), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pb400FreeText, onValueChange = { pb400FreeText = it }, label = { Text("4. Kraul") }, singleLine = true, keyboardOptions = timeKeyboardOptions, keyboardActions = timeKeyboardActions, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }
                }

                // --- ABSCHNITT 4: LANGSTRECKE (AUFKLAPPBAR) ---
                Spacer(modifier = Modifier.height(12.dp))

                var isLangstreckeEditExpanded by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF142436),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLangstreckeEditExpanded = !isLangstreckeEditExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isLangstreckeEditExpanded) "Langstrecken verbergen" else "Langstrecken eintragen (400m, 800m, 1500m)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AquaPrimary
                        )
                        Icon(
                            imageVector = if (isLangstreckeEditExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = AquaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isLangstreckeEditExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "Freistil Langstrecke:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = free400Text,
                            onValueChange = { free400Text = it },
                            label = { Text("400m", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = timeKeyboardOptions,
                            keyboardActions = timeKeyboardActions,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF1B2E46)),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = free800Text,
                            onValueChange = { free800Text = it },
                            label = { Text("800m", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = timeKeyboardOptions,
                            keyboardActions = timeKeyboardActions,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF1B2E46)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = free1500Text,
                            onValueChange = { free1500Text = it },
                            label = { Text("1500m", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = timeKeyboardOptions,
                            keyboardActions = timeKeyboardActions,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF1B2E46)),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                                // Start with existing custom PBs map so other distances/strokes (e.g. 400m Brust, 300m, etc.) are never wiped out!
                                val customMap = existingPbs.toMutableMap()
                                fun updateMapPb(str: String, dist: Int, txt: String) {
                                    val parsed = TimeFormatter.parseTimeInputToMillis(txt)
                                    if (parsed != null && parsed > 0) {
                                        customMap[str to dist] = parsed
                                    } else {
                                        customMap.remove(str to dist)
                                    }
                                }
                                updateMapPb("Freistil", 25, free25Text)
                                updateMapPb("Freistil", 50, free50Text)
                                updateMapPb("Freistil", 100, free100Text)
                                updateMapPb("Freistil", 200, free200Text)
                                updateMapPb("Freistil", 400, free400Text)
                                updateMapPb("Freistil", 800, free800Text)
                                updateMapPb("Freistil", 1500, free1500Text)

                                updateMapPb("Brust", 25, breast25Text)
                                updateMapPb("Brust", 50, breast50Text)
                                updateMapPb("Brust", 100, breast100Text)
                                updateMapPb("Brust", 200, breast200Text)

                                updateMapPb("Rücken", 25, back25Text)
                                updateMapPb("Rücken", 50, back50Text)
                                updateMapPb("Rücken", 100, back100Text)
                                updateMapPb("Rücken", 200, back200Text)

                                updateMapPb("Schmetterling", 25, fly25Text)
                                updateMapPb("Schmetterling", 50, fly50Text)
                                updateMapPb("Schmetterling", 100, fly100Text)
                                updateMapPb("Schmetterling", 200, fly200Text)

                                updateMapPb("Lagen", 100, pb100LagenText)
                                updateMapPb("Lagen", 200, pb200LagenText)
                                updateMapPb("Lagen", 400, pb400LagenText)

                                val fav25 = customMap[stroke to 25]
                                val fav50 = customMap[stroke to 50]
                                val fav100 = customMap[stroke to 100]

                                onConfirm(
                                    swimmer.copy(
                                        name = name.trim(),
                                        groupName = groupName.trim().ifEmpty { null },
                                        favoriteStroke = stroke,
                                        pb25mMillis = fav25,
                                        pb50mMillis = fav50,
                                        pb100mMillis = fav100,
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
                                        pb400mLagenFreeSplitMillis = TimeFormatter.parseTimeInputToMillis(pb400FreeText),
                                        customPbsJson = SwimmerEntity.serializeCustomPbsMap(customMap)
                                    ).withManualPbsSnapshot()
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary),
                        modifier = Modifier.testTag("submit_add_swimmer_button")
                    ) {
                        Text(confirmButtonText, color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

private data class EditTuple8<A, B, C, D, E, F, G, H>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G, val h: H
)
