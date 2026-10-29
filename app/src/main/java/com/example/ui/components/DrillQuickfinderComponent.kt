package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.StandardTechDrills
import com.example.ui.model.TechDrill
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.TextWhite

private val STROKE_FILTERS = listOf("Alle", "Kraul", "Rücken", "Brust", "Delfin", "Wenden / Starts")
private val FOCUS_AREA_FILTERS = listOf("Alle", "Arme (Zug)", "Beine (Kick)", "Wasserlage & Rumpf", "Atmung & Koordination")
private val ALL_DRILLS = StandardTechDrills.DEFAULT_DRILLS

@Composable
fun DrillQuickfinderComponent(
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStrokeFilter by rememberSaveable { mutableStateOf("Alle") }
    var selectedFocusAreaFilter by rememberSaveable { mutableStateOf("Alle") }
    var selectedSpecificFocusFilter by rememberSaveable { mutableStateOf("Alle") }

    // Dynamic specific focus list based on selected stroke / focus area
    val availableSpecificFocuses = remember(selectedStrokeFilter, selectedFocusAreaFilter) {
        val filtered = ALL_DRILLS.filter { drill ->
            val matchesStroke = (selectedStrokeFilter == "Alle") || drill.stroke.equals(selectedStrokeFilter, ignoreCase = true)
            val matchesFocusArea = (selectedFocusAreaFilter == "Alle") || drill.focusArea.equals(selectedFocusAreaFilter, ignoreCase = true)
            matchesStroke && matchesFocusArea
        }
        listOf("Alle") + filtered.asSequence().mapNotNull { it.specificFocus }.distinct().sorted().toList()
    }

    LaunchedEffect(selectedStrokeFilter, selectedFocusAreaFilter, availableSpecificFocuses) {
        if (selectedSpecificFocusFilter != "Alle" && selectedSpecificFocusFilter !in availableSpecificFocuses) {
            selectedSpecificFocusFilter = "Alle"
        }
    }

    val filteredDrills = remember(searchQuery, selectedStrokeFilter, selectedFocusAreaFilter, selectedSpecificFocusFilter) {
        ALL_DRILLS.filter { drill ->
            val matchesStroke = (selectedStrokeFilter == "Alle") || drill.stroke.equals(selectedStrokeFilter, ignoreCase = true)
            val matchesFocusArea = (selectedFocusAreaFilter == "Alle") || drill.focusArea.equals(selectedFocusAreaFilter, ignoreCase = true)
            val matchesSpecificFocus = (selectedSpecificFocusFilter == "Alle") || drill.specificFocus.equals(selectedSpecificFocusFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isEmpty() ||
                    drill.title.contains(searchQuery, ignoreCase = true) ||
                    drill.stroke.contains(searchQuery, ignoreCase = true) ||
                    drill.focusArea.contains(searchQuery, ignoreCase = true) ||
                    (drill.specificFocus?.contains(searchQuery, ignoreCase = true) == true) ||
                    drill.tips.any { it.contains(searchQuery, ignoreCase = true) } ||
                    drill.goal.contains(searchQuery, ignoreCase = true)

            matchesStroke && matchesFocusArea && matchesSpecificFocus && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "search_and_filters") {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Übung, Lage oder Stichwort suchen...", color = Color.Gray, fontSize = 13.sp) },
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
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
                        .padding(bottom = 6.dp)
                )

        // FILTER LEVEL 1: Hauptkategorie (Schwimmart)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            items(STROKE_FILTERS, key = { it }) { filter ->
                val isSelected = selectedStrokeFilter == filter
                val catColor = when (filter) {
                    "Kraul" -> Color(0xFF00E5FF)
                    "Rücken" -> Color(0xFFFFD600)
                    "Brust" -> Color(0xFF00E676)
                    "Delfin" -> Color(0xFFFF5252)
                    "Wenden / Starts" -> Color(0xFFFF9100)
                    else -> AquaPrimary
                }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedStrokeFilter = filter
                        selectedFocusAreaFilter = "Alle"
                        selectedSpecificFocusFilter = "Alle"
                    },
                    shape = RoundedCornerShape(10.dp),
                    label = {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = catColor,
                        selectedLabelColor = Color.Black,
                        containerColor = if (filter == "Alle") OceanSurface else catColor.copy(alpha = 0.15f),
                        labelColor = if (filter == "Alle") TextWhite.copy(alpha = 0.85f) else catColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (filter == "Alle") Color(0xFF1B2E46) else catColor.copy(alpha = 0.5f),
                        selectedBorderColor = catColor
                    )
                )
            }
        }

        // FILTER LEVEL 2: Fokus-Bereich (Unterkategorie 1)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            items(FOCUS_AREA_FILTERS, key = { it }) { focus ->
                val isSelected = selectedFocusAreaFilter == focus
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedFocusAreaFilter = focus
                        selectedSpecificFocusFilter = "Alle"
                    },
                    shape = RoundedCornerShape(10.dp),
                    label = { Text(focus, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AquaPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = OceanSurface,
                        labelColor = TextWhite.copy(alpha = 0.85f)
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

        // FILTER LEVEL 3: Spezifischer Schwerpunkt (Unterkategorie 2 - dynamic)
        if (availableSpecificFocuses.size > 2) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(availableSpecificFocuses, key = { it }) { specific ->
                    val isSelected = selectedSpecificFocusFilter == specific
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSpecificFocusFilter = specific },
                        shape = RoundedCornerShape(10.dp),
                        label = { Text(specific, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AquaPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = OceanSurface,
                            labelColor = TextWhite.copy(alpha = 0.85f)
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
        }

                // Results Summary Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (filteredDrills.size == ALL_DRILLS.size) "${ALL_DRILLS.size} Übungen" else "${filteredDrills.size} von ${ALL_DRILLS.size} Übungen",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = TextWhite.copy(alpha = 0.7f)
                    )

                    if (selectedStrokeFilter != "Alle" || selectedFocusAreaFilter != "Alle" || selectedSpecificFocusFilter != "Alle" || searchQuery.isNotEmpty()) {
                        Text(
                            text = "Filter zurücksetzen",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AquaPrimary,
                            modifier = Modifier.clickable {
                                selectedStrokeFilter = "Alle"
                                selectedFocusAreaFilter = "Alle"
                                selectedSpecificFocusFilter = "Alle"
                                searchQuery = ""
                            }
                        )
                    }
                }
            }
        }

        // Drills List
        if (filteredDrills.isEmpty()) {
            item(key = "empty_state") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Keine passenden Übungen für diese Filterkombination",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(
                items = filteredDrills,
                key = { it.id },
                contentType = { "drill_card" }
            ) { drill ->
                TechDrillCard(drill = drill)
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
        "Wenden / Starts" -> Color(0xFFFF9100)
        else -> Color(0xFF80D8FF)
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = strokeColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, strokeColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = drill.stroke,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = strokeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF12283A)
                        ) {
                            Text(
                                text = drill.focusArea,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = Color.LightGray,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = drill.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = TextWhite
                    )

                    if (!drill.specificFocus.isNullOrBlank()) {
                        Text(
                            text = "Fokus: ${drill.specificFocus}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AquaPrimary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = AquaPrimary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Expanded / Always scannable details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color(0xFF0A1420), shape = RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    // Training Goal Box
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD600),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "TRAININGSZIEL / WARUM?",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = Color(0xFFFFD600)
                            )
                            Text(
                                text = drill.goal,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextWhite
                            )
                        }
                    }

                    // Execution Tips ("Worauf achten?")
                    Text(
                        text = "AUSFÜHRUNG & WORAUF ACHTEN:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                        color = AquaPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    drill.tips.forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                color = AquaPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0A1128)
@Composable
fun DrillQuickfinderComponentPreview() {
    com.example.ui.theme.SwimTimerTheme {
        DrillQuickfinderComponent()
    }
}

