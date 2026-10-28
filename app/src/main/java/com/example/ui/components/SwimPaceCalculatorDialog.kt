package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.TextWhite
import com.example.util.TimeFormatter

import androidx.compose.runtime.Immutable

enum class PacingStrategy(val title: String, val subtitle: String) {
    EVEN("Gleichmäßig", "Konstante Pace pro Split"),
    NEGATIVE("Negative Split", "2. Rennhälfte ~1.5s schneller"),
    DIVE_START("Startsprung-Bonus", "1. Split -1.2s durch Startsprung"),
    MEDLEY_DISTRIBUTION("Lagen-Physiologie", "Schmett 24% • Rück 26% • Brust 27% • Kraul 23%")
}

@Immutable
data class CalculatedSplit(
    val lapIndex: Int,
    val splitDistanceMeters: Int,
    val strokeName: String,
    val lapDurationMillis: Long,
    val cumulativeTimeMillis: Long,
)

private fun getPaceCalculatorSplitOptions(distance: Int): List<Int> = when (distance) {
    25 -> listOf(25)
    50 -> listOf(25, 50)
    100 -> listOf(25, 50, 100)
    200 -> listOf(25, 50, 100, 200)
    400 -> listOf(25, 50, 100, 200)
    800 -> listOf(25, 50, 100, 200)
    1500 -> listOf(25, 50, 100, 200, 500)
    else -> listOf(25, 50)
}

@Composable
fun SwimPaceCalculatorDialog(
    initialDistance: Int = 100,
    initialStroke: String = "Freistil",
    initialSplitDistance: Int = 25,
    onDismiss: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onApplyTargetPaceToAllLanes: ((averageLapMillis: Long) -> Unit)? = null,
) {
    var stroke by remember { mutableStateOf(initialStroke) }
    var distance by remember { mutableIntStateOf(if ((initialStroke == "Lagen") && (initialDistance < 100)) 100 else initialDistance) }
    var splitDistance by remember { mutableIntStateOf(initialSplitDistance) }
    var targetTimeInput by remember {
        mutableStateOf(
            when (distance) {
                50 -> "26.50"
                100 -> if (stroke == "Lagen") "64.00" else "58.00"
                200 -> if (stroke == "Lagen") "142.00" else "128.00"
                400 -> "270.00"
                else -> "60.00"
            }
        )
    }

    var strategy by remember {
        mutableStateOf(if (stroke == "Lagen") PacingStrategy.MEDLEY_DISTRIBUTION else PacingStrategy.EVEN)
    }

    val distanceOptions = if (stroke == "Lagen") listOf(100, 200, 400) else listOf(25, 50, 100, 200, 400, 800, 1500)
    val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")

    val splitOptions = remember(distance) { getPaceCalculatorSplitOptions(distance) }
    val effectiveSplitDistance = if (splitDistance in splitOptions) splitDistance else splitOptions.first()

    // Parsing target time directly without redundant derivedStateOf
    val parsedTargetMillis = remember(targetTimeInput) {
        val text = targetTimeInput.trim().replace(',', '.')
        if (text.contains(':')) {
            val parts = text.split(':')
            val mins = parts[0].toLongOrNull() ?: 0L
            val secs = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            (((mins * 60) + secs) * 1000).toLong()
        } else {
            val secs = text.toDoubleOrNull() ?: 0.0
            (secs * 1000).toLong()
        }
    }

    // Calculate splits list directly with remember
    val calculatedSplits = remember(distance, effectiveSplitDistance, stroke, strategy, parsedTargetMillis) {
        val totalMillis = parsedTargetMillis
        if (totalMillis <= 0) emptyList()
        else {
            val numLaps = (distance / effectiveSplitDistance).coerceAtLeast(1)
            val isLagen = stroke == "Lagen"
            val legDist = (distance / 4).coerceAtLeast(1)
            val lagenStrokes = listOf("1. Schmett", "2. Rücken", "3. Brust", "4. Freistil")

            val splits = mutableListOf<CalculatedSplit>()

            if (isLagen && ((strategy == PacingStrategy.MEDLEY_DISTRIBUTION) || (distance >= 100))) {
                // Medley Physiological Ratios:
                // 1. Schmett: 24%, 2. Rücken: 26%, 3. Brust: 27%, 4. Freistil: 23%
                val ratios = listOf(0.24, 0.26, 0.27, 0.23)
                val splitsPerLeg = (numLaps / 4).coerceAtLeast(1)

                var accumulated = 0L
                for (lapIdx in 1..numLaps) {
                    val legIdx = (((lapIdx - 1) * effectiveSplitDistance) / legDist).coerceIn(0, 3)
                    val legRatio = ratios[legIdx]
                    val lapDuration = ((totalMillis * legRatio) / splitsPerLeg).toLong()
                    accumulated += lapDuration
                    splits.add(
                        CalculatedSplit(
                            lapIndex = lapIdx,
                            splitDistanceMeters = (lapIdx * effectiveSplitDistance).coerceAtMost(distance),
                            strokeName = lagenStrokes[legIdx],
                            lapDurationMillis = lapDuration,
                            cumulativeTimeMillis = accumulated
                        )
                    )
                }
            } else {
                // Non-Medley Strategies
                when (strategy) {
                    PacingStrategy.EVEN, PacingStrategy.MEDLEY_DISTRIBUTION -> {
                        val evenLap = totalMillis / numLaps
                        var accumulated = 0L
                        for (lapIdx in 1..numLaps) {
                            accumulated += evenLap
                            splits.add(
                                CalculatedSplit(
                                    lapIndex = lapIdx,
                                    splitDistanceMeters = (lapIdx * effectiveSplitDistance).coerceAtMost(distance),
                                    strokeName = stroke,
                                    lapDurationMillis = evenLap,
                                    cumulativeTimeMillis = accumulated
                                )
                            )
                        }
                    }
                    PacingStrategy.NEGATIVE -> {
                        // 2nd half 3% faster than 1st half
                        val baseLap = totalMillis.toDouble() / numLaps
                        val delta = 1500L / numLaps.coerceAtLeast(2) // ~1.5s delta
                        var accumulated = 0L
                        for (lapIdx in 1..numLaps) {
                            val lapDuration = if (lapIdx <= (numLaps / 2)) {
                                (baseLap + delta).toLong()
                            } else {
                                (baseLap - delta).toLong()
                            }
                            accumulated += lapDuration
                            splits.add(
                                CalculatedSplit(
                                    lapIndex = lapIdx,
                                    splitDistanceMeters = (lapIdx * effectiveSplitDistance).coerceAtMost(distance),
                                    strokeName = stroke,
                                    lapDurationMillis = lapDuration,
                                    cumulativeTimeMillis = accumulated
                                )
                            )
                        }
                    }
                    PacingStrategy.DIVE_START -> {
                        // First split gets 1200ms dive bonus off blocks
                        val diveBonus = 1200L.coerceAtMost(totalMillis / 4)
                        val remainingLaps = (numLaps - 1).coerceAtLeast(1)
                        val lapAfterDive = if (numLaps > 1) {
                            (totalMillis - diveBonus) / numLaps
                        } else totalMillis

                        var accumulated = 0L
                        for (lapIdx in 1..numLaps) {
                            val lapDuration = if (lapIdx == 1) {
                                (lapAfterDive - diveBonus).coerceAtLeast(5000L)
                            } else {
                                (lapAfterDive + (diveBonus / remainingLaps))
                            }
                            accumulated += lapDuration
                            splits.add(
                                CalculatedSplit(
                                    lapIndex = lapIdx,
                                    splitDistanceMeters = (lapIdx * effectiveSplitDistance).coerceAtMost(distance),
                                    strokeName = stroke,
                                    lapDurationMillis = lapDuration,
                                    cumulativeTimeMillis = accumulated
                                )
                            )
                        }
                    }
                }
            }

            // Adjust last cumulative time to match exact target
            if (splits.isNotEmpty()) {
                val diff = totalMillis - splits.last().cumulativeTimeMillis
                if (diff != 0L) {
                    val last = splits.last()
                    splits[splits.lastIndex] = last.copy(
                        lapDurationMillis = last.lapDurationMillis + diff,
                        cumulativeTimeMillis = totalMillis
                    )
                }
            }

            splits
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF101B2B),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("swim_pace_calculator_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AquaPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = AquaPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pace- & Split-Rechner",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                            Text(
                                text = "Wettkampf- & Trainingseinteilung",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Schließen",
                            tint = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stroke Selection
                Text("Schwimmart:", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    strokeOptions.forEach { s ->
                        FilterChip(
                            selected = stroke == s,
                            onClick = {
                                stroke = s
                                var newDist = distance
                                if (s == "Lagen") {
                                    if (newDist < 100) newDist = 100
                                    strategy = PacingStrategy.MEDLEY_DISTRIBUTION
                                }
                                distance = newDist
                                val newSplits = getPaceCalculatorSplitOptions(newDist)
                                if (splitDistance !in newSplits) {
                                    splitDistance = newSplits.first()
                                }
                            },
                            label = { Text(s, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (s == "Lagen") Color(0xFFFFD600) else AquaPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                if (stroke == "Lagen") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lagen min. 100m • Offizielle Reihenfolge: 1. Schmett, 2. Rücken, 3. Brust, 4. Freistil",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color(0xFFFFD600)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Distance Selection
                Text("Distanz:", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    distanceOptions.forEach { d ->
                        FilterChip(
                            selected = distance == d,
                            onClick = {
                                distance = d
                                val newSplits = getPaceCalculatorSplitOptions(d)
                                if (splitDistance !in newSplits) {
                                    splitDistance = newSplits.first()
                                }
                            },
                            label = { Text("${d}m", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Splits Selection (25m across ALL distances!)
                Text("Splits / Zwischenzeiten:", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    splitOptions.forEach { sDist ->
                        FilterChip(
                            selected = effectiveSplitDistance == sDist,
                            onClick = { splitDistance = sDist },
                            label = { Text("Alle ${sDist}m", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Target Time Input
                Text("Ziel-Endzeit (z.B. 58.50 oder 1:04.20):", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                    OutlinedTextField(
                        value = targetTimeInput,
                        onValueChange = { targetTimeInput = it },
                        placeholder = { Text("z.B. 1:02.50") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AquaPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("pace_calculator_time_input")
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF091421),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = TimeFormatter.formatMillis(parsedTargetMillis),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AquaPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pacing Strategy Selection
                Text("Rennstrategie:", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val applicableStrategies = if (stroke == "Lagen") {
                        listOf(PacingStrategy.MEDLEY_DISTRIBUTION, PacingStrategy.EVEN)
                    } else {
                        listOf(PacingStrategy.EVEN, PacingStrategy.NEGATIVE, PacingStrategy.DIVE_START)
                    }

                    applicableStrategies.forEach { strat ->
                        val isSelected = strategy == strat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF132F42) else Color(0xFF0F1E2E),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) AquaPrimary else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(0.5.dp, if (isSelected) AquaPrimary else Color(0xFF1E2E3E), RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strat.title,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) AquaPrimary else TextWhite
                                    )
                                    Text(
                                        text = strat.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray
                                    )
                                }
                                Button(
                                    onClick = { strategy = strat },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) AquaPrimary else Color(0xFF192A3A)
                                    ),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (isSelected) "Aktiv" else "Wählen",
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color.Black else Color.LightGray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Calculated Splits Table
                Text(
                    text = "Berechneter Split- & Paceplan (${calculatedSplits.size} Teilstrecken):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF08121D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Split", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(55.dp))
                            Text("Disziplin", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(75.dp))
                            Text("Lap-Pace", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(60.dp))
                            Text("Gesamt", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }

                        HorizontalDivider(color = Color(0xFF152638), modifier = Modifier.padding(bottom = 6.dp))

                        calculatedSplits.forEach { (_, splitDistanceMeters, strokeName, lapDurationMillis, cumulativeTimeMillis) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${splitDistanceMeters}m",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = AquaPrimary,
                                    modifier = Modifier.width(55.dp)
                                )
                                Text(
                                    text = strokeName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (stroke == "Lagen") Color(0xFFFFD600) else Color.LightGray
                                    ),
                                    modifier = Modifier.width(75.dp),
                                    maxLines = 1
                                )
                                Text(
                                    text = TimeFormatter.formatMillis(lapDurationMillis, shortFormat = true),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = TextWhite,
                                    modifier = Modifier.width(60.dp)
                                )
                                Text(
                                    text = TimeFormatter.formatMillis(cumulativeTimeMillis),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF80D8FF)
                                )
                            }
                        }
                    }
                }

                // Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Schließen", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
