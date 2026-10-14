package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.StartMode
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.TextWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntervalConfigDialog(
    currentStartMode: StartMode,
    currentIntervalSec: Int,
    currentPoolLength: Int,
    currentDistance: Int,
    currentSplitDistance: Int = 50,
    currentStroke: String,
    onDismiss: () -> Unit,
    onSave: (mode: StartMode, intervalSec: Int, poolLength: Int, distance: Int, splitDistance: Int, stroke: String) -> Unit
) {
    var mode by remember { mutableStateOf(currentStartMode) }
    var intervalSec by remember { mutableIntStateOf(currentIntervalSec) }
    var poolLength by remember { mutableIntStateOf(currentPoolLength) }
    var stroke by remember { mutableStateOf(currentStroke) }
    var distance by remember { mutableIntStateOf(currentDistance) }
    var splitDistance by remember { mutableIntStateOf(currentSplitDistance) }

    val intervalOptions = listOf(5, 10, 15, 20)
    val poolOptions = listOf(25, 50)
    val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")

    val distanceOptions = if (stroke == "Lagen") {
        listOf(100, 200, 400)
    } else {
        listOf(25, 50, 100, 200, 400, 800, 1500)
    }

    // 25m splits available for ALL distances!
    val availableSplitOptions = when (distance) {
        25 -> listOf(25)
        50 -> listOf(25, 50)
        100 -> listOf(25, 50, 100)
        200 -> listOf(25, 50, 100, 200)
        400 -> listOf(25, 50, 100, 200, 400)
        800 -> listOf(25, 50, 100, 200, 400, 800)
        1500 -> listOf(25, 50, 100, 200, 500, 1500)
        else -> listOf(25, 50, 100)
    }

    if (!availableSplitOptions.contains(splitDistance)) {
        splitDistance = 25
    }

    val expectedLaps = (distance / splitDistance).coerceAtLeast(1)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF101B2B),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("interval_config_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Trainings- & Zeiteinstellungen",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Start mode selection
                Text(
                    text = "Start-Verfahren:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = mode == StartMode.MASS,
                        onClick = { mode = StartMode.MASS },
                        label = { Text("Gemeinsam", maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AquaPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = mode == StartMode.INTERVAL,
                        onClick = { mode = StartMode.INTERVAL },
                        label = { Text("Intervall-Start", maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AquaPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                if (mode == StartMode.INTERVAL) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Staffel-Offset pro Bahn:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervalOptions.forEach { sec ->
                            FilterChip(
                                selected = intervalSec == sec,
                                onClick = { intervalSec = sec },
                                label = { Text("${sec}s", maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AquaPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pool length
                Text(
                    text = "Beckenlänge:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    poolOptions.forEach { length ->
                        FilterChip(
                            selected = poolLength == length,
                            onClick = { poolLength = length },
                            label = { Text("${length}m Bahn", maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stroke
                Text(
                    text = "Schwimmart:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    strokeOptions.forEach { s ->
                        FilterChip(
                            selected = stroke == s,
                            onClick = {
                                stroke = s
                                if (s == "Lagen" && distance < 100) {
                                    distance = 100
                                }
                            },
                            label = { Text(s, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                if (stroke == "Lagen") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF132738),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Offizielle Lagen-Reihenfolge (Einzelstart, min. 100m):\n1. Schmetterling • 2. Rücken • 3. Brust • 4. Freistil",
                            style = MaterialTheme.typography.labelSmall,
                            color = AquaPrimary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Distance
                Text(
                    text = "Zielstrecke:",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    distanceOptions.forEach { d ->
                        FilterChip(
                            selected = distance == d,
                            onClick = {
                                distance = d
                            },
                            label = { Text("${d}m", maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Split / Lap interval (Requested feature: 25er Zeiten bei ALLEN Distanzen)
                Text(
                    text = "Zwischenzeiten (Splits alle):",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.LightGray,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableSplitOptions.forEach { sDist ->
                        val labelText = if (sDist == distance) "Nur Ziel (${sDist}m)" else "Alle ${sDist}m"
                        FilterChip(
                            selected = splitDistance == sDist,
                            onClick = { splitDistance = sDist },
                            label = { Text(labelText, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
                val splitInfoText = when {
                    stroke == "Lagen" && splitDistance == 25 -> "25m Splits aktiv: Erfasst 1. Schmett, 2. Rücken, 3. Brust, 4. Freistil separat als Bestzeiten!"
                    splitDistance < distance -> "Splits alle ${splitDistance}m • $expectedLaps Laps bis Ziel (${distance}m)"
                    else -> "Nur Endzeit wird gestoppt (1 Lap)"
                }
                Text(
                    text = splitInfoText,
                    style = MaterialTheme.typography.labelSmall,
                    color = AquaPrimary,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Abbrechen", color = Color.LightGray, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(mode, intervalSec, poolLength, distance, splitDistance, stroke)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
                    ) {
                        Text("Übernehmen", color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}
