package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SwimmerEntity
import com.example.domain.RelayLegAssignment
import com.example.domain.RelayOptimizationMode
import com.example.domain.RelayOptimizer
import com.example.domain.RelayTeamPlan
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.TextWhite
import com.example.util.TimeFormatter

private val TEAM_COLORS = listOf(
    Color(0xFF00E5FF),
    Color(0xFFFFD600),
    Color(0xFF00E676),
    Color(0xFFFF5252),
    Color(0xFFFF4081),
    Color(0xFF7C4DFF),
)

private val swimmerIdsSaver = Saver<Set<Long>?, List<Long>>(
    save = { set -> set?.toList() ?: emptyList() },
    restore = { list -> list.toSet() }
)

private val relayTeamsSaver = Saver<List<RelayTeamPlan>, String>(
    save = { teams ->
        val arr = org.json.JSONArray()
        teams.forEach { t ->
            val obj = org.json.JSONObject().apply {
                put("teamIndex", t.teamIndex)
                put("teamTitle", t.teamTitle)
                t.totalTimeMillis?.let { put("totalTimeMillis", it) }
                t.averageLegMillis?.let { put("averageLegMillis", it) }
                t.deltaToFastestMillis?.let { put("deltaToFastestMillis", it) }
                put("hasEstimatedTimes", t.hasEstimatedTimes)
                val legsArr = org.json.JSONArray()
                t.legs.forEach { leg ->
                    legsArr.put(
                        org.json.JSONObject().apply {
                            put("legPosition", leg.legPosition)
                            leg.swimmerId?.let { put("swimmerId", it) }
                            put("swimmerName", leg.swimmerName)
                            put("stroke", leg.stroke)
                            put("distanceMeters", leg.distanceMeters)
                            leg.timeMillis?.let { put("timeMillis", it) }
                            put("isEstimated", leg.isEstimated)
                            put("sourceLabel", leg.sourceLabel)
                        }
                    )
                }
                put("legs", legsArr)
            }
            arr.put(obj)
        }
        arr.toString()
    },
    restore = { raw ->
        if (raw.isBlank()) emptyList()
        else try {
            val arr = org.json.JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val legsArr = obj.optJSONArray("legs")
                    val legs = buildList {
                        if (legsArr != null) {
                            for (j in 0 until legsArr.length()) {
                                val l = legsArr.getJSONObject(j)
                                add(
                                    RelayLegAssignment(
                                        legPosition = l.optInt("legPosition", j + 1),
                                        swimmerId = if (l.has("swimmerId")) l.optLong("swimmerId") else null,
                                        swimmerName = l.optString("swimmerName", ""),
                                        stroke = l.optString("stroke", "Freistil"),
                                        distanceMeters = l.optInt("distanceMeters", 50),
                                        timeMillis = if (l.has("timeMillis")) l.optLong("timeMillis") else null,
                                        isEstimated = l.optBoolean("isEstimated", false),
                                        sourceLabel = l.optString("sourceLabel", "")
                                    )
                                )
                            }
                        }
                    }
                    add(
                        RelayTeamPlan(
                            teamIndex = obj.optInt("teamIndex", i),
                            teamTitle = obj.optString("teamTitle", "Staffel ${i + 1}"),
                            legs = legs,
                            totalTimeMillis = if (obj.has("totalTimeMillis")) obj.optLong("totalTimeMillis") else null,
                            averageLegMillis = if (obj.has("averageLegMillis")) obj.optLong("averageLegMillis") else null,
                            deltaToFastestMillis = if (obj.has("deltaToFastestMillis")) obj.optLong("deltaToFastestMillis") else null,
                            hasEstimatedTimes = obj.optBoolean("hasEstimatedTimes", false)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
)

@Composable
fun RelayRandomizerComponent(
    dbSwimmers: List<SwimmerEntity>,
    availableGroups: List<String> = emptyList(),
    onApplyTeamsToStopwatch: ((teams: List<RelayTeamPlan>, strokeType: String, legDistanceMeters: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val groupsToDisplay = remember(availableGroups, dbSwimmers) {
        if (availableGroups.isNotEmpty()) {
            availableGroups
        } else {
            dbSwimmers.asSequence()
                .mapNotNull { it.groupName?.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
                .toList()
        }
    }

    val groupCounts = remember(dbSwimmers) {
        dbSwimmers.mapNotNull { it.groupName?.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it.lowercase() }
            .eachCount()
    }

    var selectedMode by rememberSaveable { mutableStateOf(RelayOptimizationMode.MEDLEY_OPTIMAL) }
    var selectedLegDistance by rememberSaveable { mutableIntStateOf(50) }
    var selectedStroke by rememberSaveable { mutableStateOf("Freistil") }
    var selectedGroup by rememberSaveable { mutableStateOf<String?>(null) }

    val groupSwimmers = remember(dbSwimmers, selectedGroup) {
        if (selectedGroup == null) dbSwimmers
        else dbSwimmers.filter { it.groupName.equals(selectedGroup, ignoreCase = true) }
    }

    var savedSwimmerIds by rememberSaveable(stateSaver = swimmerIdsSaver) {
        mutableStateOf<Set<Long>?>(null)
    }

    val selectedSwimmerIds = remember(savedSwimmerIds, groupSwimmers) {
        savedSwimmerIds ?: groupSwimmers.map { it.id }.toSet()
    }

    var manualInputText by rememberSaveable { mutableStateOf("") }
    var teamCount by rememberSaveable { mutableIntStateOf(2) }
    var generatedTeams by rememberSaveable(stateSaver = relayTeamsSaver) { mutableStateOf<List<RelayTeamPlan>>(emptyList()) }
    var statusHintMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val presentSwimmers = remember(dbSwimmers, selectedSwimmerIds) {
        dbSwimmers.filter { selectedSwimmerIds.contains(it.id) }
    }

    fun calculateRelays() {
        focusManager.clearFocus()
        statusHintMessage = null

        val guestNames = if (manualInputText.isNotBlank()) {
            manualInputText.split(",", "\n", ";").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        when (selectedMode) {
            RelayOptimizationMode.MEDLEY_OPTIMAL -> {
                if (presentSwimmers.size < 4) {
                    statusHintMessage = "Für eine 4× Lagenstaffel bitte mindestens 4 Athleten auswählen (aktuell: ${presentSwimmers.size})."
                    generatedTeams = emptyList()
                    return
                }
                generatedTeams = RelayOptimizer.optimizeMedleyRelays(
                    swimmers = presentSwimmers,
                    legDistanceMeters = selectedLegDistance,
                    maxTeams = 4
                )
            }
            RelayOptimizationMode.FASTEST_SINGLE -> {
                if (presentSwimmers.size < 4) {
                    statusHintMessage = "Für eine 4× $selectedStroke-Staffel bitte mindestens 4 Athleten auswählen (aktuell: ${presentSwimmers.size})."
                    generatedTeams = emptyList()
                    return
                }
                generatedTeams = RelayOptimizer.optimizeFastestSingleStrokeRelays(
                    swimmers = presentSwimmers,
                    stroke = selectedStroke,
                    legDistanceMeters = selectedLegDistance,
                    maxTeams = 4
                )
            }
            RelayOptimizationMode.BALANCED -> {
                val totalCount = presentSwimmers.size + guestNames.size
                if (totalCount < 2) {
                    statusHintMessage = "Bitte mindestens 2 Teilnehmer für Fair-Match auswählen."
                    generatedTeams = emptyList()
                    return
                }
                generatedTeams = RelayOptimizer.buildBalancedTeams(
                    swimmers = presentSwimmers,
                    guestNames = guestNames,
                    teamCount = teamCount,
                    stroke = selectedStroke,
                    legDistanceMeters = selectedLegDistance
                )
            }
            RelayOptimizationMode.RANDOM -> {
                val totalCount = presentSwimmers.size + guestNames.size
                if (totalCount < 2) {
                    statusHintMessage = "Bitte mindestens 2 Teilnehmer für die Auslosung auswählen."
                    generatedTeams = emptyList()
                    return
                }
                generatedTeams = RelayOptimizer.buildRandomTeams(
                    swimmers = presentSwimmers,
                    guestNames = guestNames,
                    teamCount = teamCount,
                    stroke = selectedStroke,
                    legDistanceMeters = selectedLegDistance
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- 1. MODUS-AUSWAHL ---
        item(key = "mode_selector") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Staffel-Modus:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val modePairs = remember { RelayOptimizationMode.entries.chunked(2) }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    modePairs.forEach { rowModes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowModes.forEach { mode ->
                                val isSelected = selectedMode == mode
                                val icon = when (mode) {
                                    RelayOptimizationMode.MEDLEY_OPTIMAL -> Icons.Default.AutoAwesome
                                    RelayOptimizationMode.FASTEST_SINGLE -> Icons.Default.EmojiEvents
                                    RelayOptimizationMode.BALANCED -> Icons.Default.Balance
                                    RelayOptimizationMode.RANDOM -> Icons.Default.Shuffle
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedMode = mode
                                        statusHintMessage = null
                                    },
                                    label = {
                                        Text(
                                            text = mode.shortLabel,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AquaPrimary,
                                        selectedLabelColor = Color.Black,
                                        selectedLeadingIconColor = Color.Black,
                                        containerColor = OceanSurface,
                                        labelColor = Color.LightGray,
                                        iconColor = AquaPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("relay_mode_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. STRECKE & LAGE KONFIGURATION ---
        item(key = "distance_and_stroke_config") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = OceanSurface,
                border = BorderStroke(1.dp, Color(0xFF1B2E46)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Teilstrecke:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RelayOptimizer.ALLOWED_LEG_DISTANCES.forEach { dist ->
                                val isSelected = selectedLegDistance == dist
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedLegDistance = dist },
                                    label = {
                                        Text(
                                            text = "4×${dist}m",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AquaPrimary,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF0E1B2A),
                                        labelColor = Color.LightGray
                                    )
                                )
                            }
                        }
                    }

                    if (selectedMode == RelayOptimizationMode.MEDLEY_OPTIMAL) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Offizielle Lagenstaffel-Folge: 1. Rücken • 2. Brust • 3. Schmett • 4. Freistil",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFD600)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            RelayOptimizer.SINGLE_STROKES.forEach { stroke ->
                                val isSelected = selectedStroke == stroke
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedStroke = stroke },
                                    label = {
                                        Text(
                                            text = stroke,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AquaPrimary,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color(0xFF0E1B2A),
                                        labelColor = Color.LightGray
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. TRAININGSGRUPPE & ANWESENDE ATHLETEN ---
        item(key = "swimmer_selection") {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (groupsToDisplay.isNotEmpty()) {
                    Text(
                        text = "1. Trainingsgruppe wählen:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AquaPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        item(key = "all_chip") {
                            FilterChip(
                                selected = selectedGroup == null,
                                onClick = {
                                    selectedGroup = null
                                    savedSwimmerIds = dbSwimmers.map { it.id }.toSet()
                                },
                                label = {
                                    Text(
                                        "Alle (${dbSwimmers.size})",
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedGroup == null) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AquaPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = OceanSurface,
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                        items(groupsToDisplay, key = { it }) { group ->
                            val isSelected = selectedGroup == group
                            val groupCount = groupCounts[group.lowercase()] ?: 0
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedGroup = group
                                    val newSwimmers = dbSwimmers.filter { it.groupName.equals(group, ignoreCase = true) }
                                    savedSwimmerIds = newSwimmers.map { it.id }.toSet()
                                },
                                label = {
                                    Text(
                                        "$group ($groupCount)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AquaPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = OceanSurface,
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }
                }

                if (groupSwimmers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Anwesende Athleten (${selectedSwimmerIds.size} / ${groupSwimmers.size}):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AquaPrimary
                        )

                        Text(
                            text = if (selectedSwimmerIds.size == groupSwimmers.size) "Alle abwählen" else "Alle auswählen",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .noRippleClickable {
                                    savedSwimmerIds = if (selectedSwimmerIds.size == groupSwimmers.size) {
                                        emptySet()
                                    } else {
                                        groupSwimmers.map { it.id }.toSet()
                                    }
                                }
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        groupSwimmers.forEach { swimmer ->
                            val isSelected = selectedSwimmerIds.contains(swimmer.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    savedSwimmerIds = if (isSelected) {
                                        selectedSwimmerIds - swimmer.id
                                    } else {
                                        selectedSwimmerIds + swimmer.id
                                    }
                                },
                                label = { Text(swimmer.name, fontSize = 12.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AquaPrimary,
                                    selectedLabelColor = Color.Black,
                                    containerColor = OceanSurface,
                                    labelColor = Color.Gray
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- 4. GÄSTE & TEAM-ANZAHL (FÜR FAIR-MATCH & ZUFALL) + BERECHNEN BUTTON ---
        item(key = "controls_and_action") {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (selectedMode == RelayOptimizationMode.BALANCED || selectedMode == RelayOptimizationMode.RANDOM) {
                    OutlinedTextField(
                        value = manualInputText,
                        onValueChange = { manualInputText = it },
                        placeholder = { Text("Gäste / Zusätzliche Namen (Komma-getrennt)...", color = Color.Gray, fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OceanSurface,
                            unfocusedContainerColor = OceanSurface,
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color(0xFF1B2E46),
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = AquaPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Anzahl Teams:", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (teamCount > 2) teamCount-- },
                                enabled = teamCount > 2
                            ) {
                                Text("-", style = MaterialTheme.typography.titleLarge, color = if (teamCount > 2) AquaPrimary else Color.Gray)
                            }
                            Text(
                                text = "$teamCount Teams",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AquaPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { if (teamCount < 6) teamCount++ },
                                enabled = teamCount < 6
                            ) {
                                Text("+", style = MaterialTheme.typography.titleLarge, color = if (teamCount < 6) AquaPrimary else Color.Gray)
                            }
                        }
                    }
                }

                val actionButtonLabel = when (selectedMode) {
                    RelayOptimizationMode.MEDLEY_OPTIMAL -> "OPTIMALE 4× LAGEN-STAFFEL BERECHNEN"
                    RelayOptimizationMode.FASTEST_SINGLE -> "4× ${selectedStroke.uppercase()}-STAFFEL BERECHNEN"
                    RelayOptimizationMode.BALANCED -> "FAIRE TEAMS NACH PB BERECHNEN"
                    RelayOptimizationMode.RANDOM -> "TEAMS ZUFÄLLIG AUSLOSEN"
                }

                Button(
                    onClick = { calculateRelays() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("calculate_relay_button")
                ) {
                    Icon(
                        imageVector = when (selectedMode) {
                            RelayOptimizationMode.MEDLEY_OPTIMAL -> Icons.Default.AutoAwesome
                            RelayOptimizationMode.FASTEST_SINGLE -> Icons.Default.EmojiEvents
                            RelayOptimizationMode.BALANCED -> Icons.Default.Balance
                            RelayOptimizationMode.RANDOM -> Icons.Default.Shuffle
                        },
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = actionButtonLabel,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                statusHintMessage?.let { hint ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFB74D)
                    )
                }

                if (generatedTeams.isNotEmpty() && onApplyTeamsToStopwatch != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val targetStroke = if (selectedMode == RelayOptimizationMode.MEDLEY_OPTIMAL) "Lagen" else selectedStroke
                    FilledTonalButton(
                        onClick = {
                            onApplyTeamsToStopwatch(generatedTeams, targetStroke, selectedLegDistance)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF142B3E),
                            contentColor = AquaPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_relay_to_stopwatch_button")
                    ) {
                        Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Direkt auf Stoppuhr übernehmen (${generatedTeams.size} Bahnen)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }
        }

        // --- 5. ERGEBNIS-KARTEN ---
        itemsIndexed(
            items = generatedTeams,
            key = { index, team -> "${team.teamTitle}_$index" },
            contentType = { _, _ -> "RelayTeamCard" }
        ) { index, teamPlan ->
            val teamColor = TEAM_COLORS.getOrElse(index % TEAM_COLORS.size) { AquaPrimary }
            RelayTeamResultCard(
                teamPlan = teamPlan,
                teamColor = teamColor,
                isMedleyMode = selectedMode == RelayOptimizationMode.MEDLEY_OPTIMAL,
                showAnchorBadge = selectedMode != RelayOptimizationMode.RANDOM && teamPlan.legs.size >= 2
            )
        }
    }
}

@Composable
private fun RelayTeamResultCard(
    teamPlan: RelayTeamPlan,
    teamColor: Color,
    isMedleyMode: Boolean,
    showAnchorBadge: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OceanSurface),
        border = BorderStroke(1.5.dp, teamColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = teamColor,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (teamPlan.teamIndex + 1).toString(),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = teamPlan.teamTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            color = teamColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${teamPlan.legs.size} Schwimmer" + if (teamPlan.hasEstimatedTimes) " • inkl. Richtzeit (~)" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                if (teamPlan.totalTimeMillis != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        val prefix = if (teamPlan.hasEstimatedTimes) "~" else ""
                        Text(
                            text = "$prefix${TimeFormatter.formatMillis(teamPlan.totalTimeMillis)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = TextWhite
                        )
                        val delta = teamPlan.deltaToFastestMillis
                        if (delta != null) {
                            val deltaLabel = if (delta == 0L) "Schnellstes Team" else "+${TimeFormatter.formatMillis(delta, shortFormat = true)}s"
                            val deltaColor = if (delta == 0L) PaceGreen else Color(0xFFFFB74D)
                            Text(
                                text = deltaLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp
                                ),
                                color = deltaColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF1B2E46), modifier = Modifier.padding(bottom = 8.dp))

            teamPlan.legs.forEachIndexed { idx, leg ->
                RelayLegRow(
                    leg = leg,
                    isMedleyMode = isMedleyMode,
                    isAnchor = showAnchorBadge && idx == teamPlan.legs.lastIndex && !isMedleyMode
                )
            }
        }
    }
}

@Composable
private fun RelayLegRow(
    leg: RelayLegAssignment,
    isMedleyMode: Boolean,
    isAnchor: Boolean
) {
    val strokeColor = when {
        leg.stroke.equals("Rücken", ignoreCase = true) -> Color(0xFFFFD600)
        leg.stroke.equals("Brust", ignoreCase = true) -> Color(0xFF00E676)
        leg.stroke.equals("Schmetterling", ignoreCase = true) -> Color(0xFFFF5252)
        else -> Color(0xFF00E5FF)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Text(
                text = "${leg.legPosition}.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = AquaPrimary,
                modifier = Modifier.width(22.dp)
            )

            if (isMedleyMode) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = strokeColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, strokeColor.copy(alpha = 0.55f)),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    val shortStroke = if (leg.stroke == "Schmetterling") "Schmett" else leg.stroke
                    Text(
                        text = shortStroke,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = strokeColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = leg.swimmerName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (isAnchor) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "★ Schluss",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFD600)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leg.timeMillis != null) {
                if (leg.sourceLabel.isNotBlank() && leg.sourceLabel != "PB") {
                    Text(
                        text = leg.sourceLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = if (leg.isEstimated) Color(0xFFFFB74D) else AquaPrimary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Text(
                    text = TimeFormatter.formatMillis(leg.timeMillis, shortFormat = leg.distanceMeters <= 50),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (leg.isEstimated) Color(0xFFFFB74D) else AquaPrimary
                )
            } else {
                Text(
                    text = "--:--",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0A1128)
@Composable
fun RelayRandomizerComponentPreview() {
    com.example.ui.theme.SwimTimerTheme {
        Box(modifier = Modifier.background(Color(0xFF0A1128)).padding(12.dp)) {
            RelayRandomizerComponent(
                dbSwimmers = listOf(
                    SwimmerEntity(id = 1, name = "Lukas Weber", favoriteStroke = "Rücken", pb50mMillis = 29_500L, groupName = "Wettkampf 1"),
                    SwimmerEntity(id = 2, name = "Sophie Müller", favoriteStroke = "Brust", pb50mMillis = 34_200L, groupName = "Wettkampf 1"),
                    SwimmerEntity(id = 3, name = "Leon Schmidt", favoriteStroke = "Schmetterling", pb50mMillis = 27_800L, groupName = "Wettkampf 2"),
                    SwimmerEntity(id = 4, name = "Emma Fischer", favoriteStroke = "Freistil", pb50mMillis = 26_400L, groupName = "Wettkampf 1")
                ),
                availableGroups = listOf("Wettkampf 1", "Wettkampf 2")
            )
        }
    }
}
