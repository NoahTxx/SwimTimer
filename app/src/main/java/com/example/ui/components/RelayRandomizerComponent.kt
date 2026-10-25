package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun RelayRandomizerComponent(
    dbSwimmers: List<SwimmerEntity>,
    modifier: Modifier = Modifier
) {
    var manualInputText by remember { mutableStateOf("") }
    var selectedSwimmerNames by remember { mutableStateOf(dbSwimmers.map { swimmer -> swimmer.name }.toSet()) }
    var teamCount by remember { mutableIntStateOf(2) }
    var generatedTeams by remember { mutableStateOf<List<List<String>>>(emptyList()) }

    fun drawTeams() {
        val allNames = mutableListOf<String>()
        allNames.addAll(selectedSwimmerNames)
        if (manualInputText.isNotBlank()) {
            val customNames = manualInputText.split(",", "\n", ";").map { it.trim() }.filter { it.isNotEmpty() }
            allNames.addAll(customNames)
        }
        val uniqueNames = allNames.distinct()
        if (uniqueNames.isEmpty()) return

        val shuffled = uniqueNames.shuffled()
        val numTeams = teamCount.coerceIn(2, 6)
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
        // Quick DB Swimmers Selector
        if (dbSwimmers.isNotEmpty()) {
            Text(
                text = "Athleten aus der Datenbank auswählen:",
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                items(dbSwimmers) { swimmer ->
                    val isSelected = selectedSwimmerNames.contains(swimmer.name)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedSwimmerNames = if (isSelected) {
                                selectedSwimmerNames - swimmer.name
                            } else {
                                selectedSwimmerNames + swimmer.name
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
                            labelColor = Color.LightGray
                        )
                    )
                }
            }
        }

        // Manual Name Input
        OutlinedTextField(
            value = manualInputText,
            onValueChange = { manualInputText = it },
            placeholder = { Text("Zusätzliche Namen eingeben (Komma-getrennt)...", color = Color.Gray, fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = AquaPrimary)
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

        // Action Draw Teams Button
        Button(
            onClick = { drawTeams() },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary, contentColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Icon(imageVector = Icons.Default.Shuffle, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("TEAMS AUSLOSEN", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Generated Teams Output
        if (generatedTeams.isNotEmpty()) {
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
