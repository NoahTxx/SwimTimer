package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SwimmerEntity
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.TextWhite

private val TEAM_COLORS = listOf(
    Color(0xFF00E5FF), // Aqua
    Color(0xFFFF5252), // Red
    Color(0xFFFFD600), // Yellow
    Color(0xFF00E676), // Green
    Color(0xFFFF4081), // Pink
    Color(0xFF7C4DFF), // Purple
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelayRandomizerComponent(
    dbSwimmers: List<SwimmerEntity>,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    // Available training groups
    val availableGroups = remember(dbSwimmers) {
        dbSwimmers.mapNotNull { it.groupName?.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    var selectedGroup by remember { mutableStateOf<String?>(null) } // null = Alle

    // Filter swimmers by selected group
    val groupSwimmers = remember(dbSwimmers, selectedGroup) {
        if (selectedGroup == null) dbSwimmers
        else dbSwimmers.filter { it.groupName.equals(selectedGroup, ignoreCase = true) }
    }

    // Set of active/present swimmer IDs in selected group
    var selectedSwimmerIds by remember { mutableStateOf(groupSwimmers.map { it.id }.toSet()) }

    // Sync selected swimmer IDs whenever group changes
    LaunchedEffect(selectedGroup, dbSwimmers) {
        selectedSwimmerIds = groupSwimmers.map { it.id }.toSet()
    }

    var manualInputText by remember { mutableStateOf("") }
    var teamCount by remember { mutableIntStateOf(2) }
    var generatedTeams by remember { mutableStateOf<List<List<String>>>(emptyList()) }

    // Aktive Teilnehmer (DB + Gäste)
    val activeSwimmers = remember(dbSwimmers, selectedSwimmerIds, manualInputText) {
        val presentSwimmers = dbSwimmers.filter { selectedSwimmerIds.contains(it.id) }
        val customNames = if (manualInputText.isNotBlank()) {
            manualInputText.split(",", "\n", ";").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()
        (presentSwimmers.map { it.name } + customNames).distinct()
    }

    val activeCount = activeSwimmers.size
    val effectiveTeamCount = remember(teamCount, activeCount) {
        teamCount.coerceIn(2, activeCount.coerceAtLeast(2))
    }

    fun drawTeams() {
        focusManager.clearFocus()
        if (activeCount < 2) return

        val shuffled = activeSwimmers.shuffled()
        val numTeams = effectiveTeamCount
        val result = List(numTeams) { mutableListOf<String>() }

        shuffled.forEachIndexed { idx, name ->
            result[idx % numTeams].add(name)
        }
        generatedTeams = result
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
        // SCHRITT 1: GRUPPE VORFILTERN
        if (availableGroups.isNotEmpty()) {
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
                item {
                    FilterChip(
                        selected = selectedGroup == null,
                        onClick = { selectedGroup = null },
                        label = { Text("Alle (${dbSwimmers.size})", fontSize = 12.sp, fontWeight = if (selectedGroup == null) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AquaPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = OceanSurface,
                            labelColor = Color.LightGray
                        )
                    )
                }
                items(availableGroups) { group ->
                    val isSelected = selectedGroup == group
                    val groupCount = dbSwimmers.count { it.groupName.equals(group, ignoreCase = true) }
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedGroup = group },
                        label = { Text("$group ($groupCount)", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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

        // SCHRITT 2: ANWESENHEIT PRÜFEN
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

                Row {
                    Text(
                        text = if (selectedSwimmerIds.size == groupSwimmers.size) "Alle abwählen" else "Alle auswählen",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .noRippleClickable {
                                selectedSwimmerIds = if (selectedSwimmerIds.size == groupSwimmers.size) {
                                    emptySet()
                                } else {
                                    groupSwimmers.map { it.id }.toSet()
                                }
                            }
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                groupSwimmers.forEach { swimmer ->
                    val isSelected = selectedSwimmerIds.contains(swimmer.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedSwimmerIds = if (isSelected) {
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

        // Manual Guest Names Input
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
                .padding(bottom = 10.dp)
        )

        // Team Count Selector Controls
        val maxSelectableTeams = activeCount.coerceIn(2, 6)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
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
                    text = "$effectiveTeamCount Teams",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { if (teamCount < maxSelectableTeams) teamCount++ },
                    enabled = teamCount < maxSelectableTeams
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge, color = if (teamCount < maxSelectableTeams) AquaPrimary else Color.Gray)
                }
            }
        }

        // SCHRITT 3: AUSLOSEN BUTTON (Deaktiviert falls < 2 Teilnehmer)
        Button(
            onClick = { drawTeams() },
            enabled = activeCount >= 2,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AquaPrimary,
                contentColor = Color.Black,
                disabledContainerColor = OceanSurface,
                disabledContentColor = Color.Gray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("TEAMS AUSLOSEN", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }

        if (activeCount < 2) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hinweis: Mindestens 2 ausgewählte / anwesende Athleten für eine Auslosung erforderlich.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Generated Teams Output
        if (generatedTeams.isNotEmpty()) {
            val totalSwimmersInTeams = generatedTeams.sumOf { it.size }
            val isUneven = generatedTeams.isNotEmpty() && (totalSwimmersInTeams % generatedTeams.size != 0)

            if (isUneven) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFD600).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFFFFD600).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "Hinweis: Ungleiche Teilnehmerzahl – Teams unterscheiden sich um 1 Person",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFD600),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(generatedTeams) { index, teamMembers ->
                    val teamColor = TEAM_COLORS.getOrElse(index % TEAM_COLORS.size) { AquaPrimary }
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = OceanSurface),
                        border = BorderStroke(1.5.dp, teamColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = teamColor,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (index + 1).toString(),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Black
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Staffel-Team ${index + 1} (${teamMembers.size} Schwimmer)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = teamColor
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            teamMembers.forEachIndexed { swimmerIdx, memberName ->
                                Text(
                                    text = "${swimmerIdx + 1}. $memberName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextWhite,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
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
