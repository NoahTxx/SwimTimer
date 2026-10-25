package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.StandardTechDrills
import com.example.ui.model.TechDrill
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.TextWhite

private val STROKE_FILTERS = listOf("Alle", "Kraul", "Delfin", "Rücken", "Brust", "Allgemein")

@Composable
fun DrillQuickfinderComponent(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStrokeFilter by remember { mutableStateOf("Alle") }

    val drills = remember { StandardTechDrills.DEFAULT_DRILLS }

    val filteredDrills by remember(searchQuery, selectedStrokeFilter) {
        derivedStateOf {
            drills.filter { drill ->
                val matchesStroke = (selectedStrokeFilter == "Alle") || drill.stroke.equals(selectedStrokeFilter, ignoreCase = true)
                val matchesQuery = searchQuery.isEmpty() ||
                        drill.title.contains(searchQuery, ignoreCase = true) ||
                        drill.stroke.contains(searchQuery, ignoreCase = true) ||
                        drill.description.any { it.contains(searchQuery, ignoreCase = true) }
                matchesStroke && matchesQuery
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Übung oder Lage suchen...", color = Color.Gray) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = AquaPrimary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Löschen", tint = Color.LightGray)
                    }
                }
            },
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
                .padding(bottom = 8.dp)
        )

        // Stroke Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            items(STROKE_FILTERS) { filter ->
                val isSelected = selectedStrokeFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedStrokeFilter = filter },
                    label = { Text(filter, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AquaPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = OceanSurface,
                        labelColor = Color.LightGray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Color(0xFF1B2E46),
                        selectedBorderColor = AquaPrimary
                    )
                )
            }
        }

        // Drills List
        if (filteredDrills.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Keine passenden Übungen gefunden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = filteredDrills,
                    key = { it.id }
                ) { drill ->
                    TechDrillCard(drill = drill)
                }
            }
        }
    }
}

@Composable
private fun TechDrillCard(
    drill: TechDrill
) {
    var isExpanded by remember { mutableStateOf(false) }

    val strokeColor = when (drill.stroke) {
        "Kraul" -> Color(0xFF00E5FF)
        "Delfin" -> Color(0xFFFF5252)
        "Rücken" -> Color(0xFFFFD600)
        "Brust" -> Color(0xFF00E676)
        else -> AquaPrimary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OceanSurface),
        border = BorderStroke(1.dp, Color(0xFF1B2E46)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = strokeColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, strokeColor)
                    ) {
                        Text(
                            text = drill.stroke,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = strokeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = drill.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                }

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
                        .padding(top = 10.dp)
                        .background(Color(0xFF0C1623), shape = RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    drill.description.forEach { step ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                color = AquaPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}
