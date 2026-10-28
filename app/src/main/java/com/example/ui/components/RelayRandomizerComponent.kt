package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

private val TEAM_COLORS = listOf(
    Color(0xFF00E5FF),
    Color(0xFFFF5252),
    Color(0xFFFFD600),
    Color(0xFF00E676),
    Color(0xFFFF4081),
    Color(0xFF7C4DFF),
)

private val teamsSaver = Saver<List<List<String>>, String>(
    save = { teams -> teams.joinToString("\u001E") { team -> team.joinToString("\u001F") } },
    restore = { str ->
        if (str.isEmpty()) emptyList()
        else str.split("\u001E").map { teamStr -> if (teamStr.isEmpty()) emptyList() else teamStr.split("\u001F") }
    }
)

private val swimmerIdsSaver = Saver<Set<Long>?, List<Long>>(
    save = { set -> set?.toList() ?: emptyList() },
    restore = { list -> list.toSet() }
)

@Composable
fun RelayRandomizerComponent(
    dbSwimmers: List<SwimmerEntity>,
    availableGroups: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    val groupsToDisplay = remember(availableGroups, dbSwimmers) {
        if (availableGroups.isNotEmpty()) {
            availableGroups
        } else {
            dbSwimmers.asSequence().mapNotNull { it.groupName?.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.sortedWith(String.CASE_INSENSITIVE_ORDER).toList()
        }
    }

    val groupCounts = remember(dbSwimmers) {
        dbSwimmers.mapNotNull { it.groupName?.trim() }.filter { it.isNotBlank() }.groupingBy { it.lowercase() }.eachCount()
    }

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
    var generatedTeams by rememberSaveable(stateSaver = teamsSaver) { mutableStateOf<List<List<String>>>(emptyList()) }

    fun drawTeams() {
        focusManager.clearFocus()
        val allNames = mutableListOf<String>()
        val presentSwimmers = dbSwimmers.filter { selectedSwimmerIds.contains(it.id) }
        allNames.addAll(presentSwimmers.map { it.name.trim() }.filter { it.isNotEmpty() })

        if (manualInputText.isNotBlank()) {
            val customNames = manualInputText.split(",", "\n", ";").map { it.trim() }.filter { it.isNotEmpty() }
            allNames.addAll(customNames)
        }

        val uniqueNames = allNames.distinct()
        if (uniqueNames.size < 2) return

        val effectiveTeamCount = teamCount.coerceIn(2, uniqueNames.size.coerceAtMost(6))
        val shuffled = uniqueNames.shuffled()
        val result = List(effectiveTeamCount) { mutableListOf<String>() }

        shuffled.forEachIndexed { idx, name ->
            result[idx % effectiveTeamCount].add(name)
        }
        generatedTeams = result
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 12.dp)
    ) {
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
                        label = { Text("Alle (${dbSwimmers.size})", fontSize = 12.sp, fontWeight = if (selectedGroup == null) FontWeight.Bold else FontWeight.Normal) },
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
                                savedSwimmerIds = if (selectedSwimmerIds.size == groupSwimmers.size) {
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

        Button(
            onClick = { drawTeams() },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary, contentColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("TEAMS AUSLOSEN", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (generatedTeams.isNotEmpty()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = generatedTeams,
                    key = { index, _ -> index },
                    contentType = { _, _ -> "TeamCard" }
                ) { index, teamMembers ->
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
