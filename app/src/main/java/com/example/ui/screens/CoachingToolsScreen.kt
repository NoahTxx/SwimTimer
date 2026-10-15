package com.example.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StopwatchViewModel
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.DeepOceanBackground
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.TextWhite
import com.example.util.HapticUtil
import com.example.util.TimeFormatter
import java.util.Locale

enum class CoachingToolSection(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    PACE("Pace-Rechner", Icons.Default.Calculate),
    STROKE_RATE("Takt / SPM", Icons.Default.Speed),
    CONVERTER("25m ↔ 50m", Icons.AutoMirrored.Filled.CompareArrows)
}

@Composable
fun CoachingToolsScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(CoachingToolSection.PACE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOceanBackground)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Top Tool Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = OceanSurface,
                border = BorderStroke(1.dp, Color(0xFF1B2E46))
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoachingToolSection.entries.forEach { section ->
                        val isSelected = selectedSection == section
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AquaPrimary else Color.Transparent,
                            modifier = Modifier
                                .clickable { selectedSection = section }
                                .testTag("tool_tab_${section.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = section.label,
                                    tint = if (isSelected) Color.Black else Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = section.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = if (isSelected) Color.Black else Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Content
        when (selectedSection) {
            CoachingToolSection.PACE -> PaceCalculatorView(
                onApplyToStopwatch = { avgMillis, stroke, distance, splitDistance ->
                    viewModel.setTargetPaceForAllLanes(
                        paceMillis = avgMillis,
                        stroke = stroke,
                        distance = distance,
                        splitDistance = splitDistance
                    )
                }
            )
            CoachingToolSection.STROKE_RATE -> StrokeRateTapView()
            CoachingToolSection.CONVERTER -> PoolLengthConverterView()
        }
    }
}

/**
 * 1. PACE & SPLIT CALCULATOR
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaceCalculatorView(
    onApplyToStopwatch: ((Long, String, Int, Int) -> Unit)? = null
) {
    var stroke by remember { mutableStateOf("Freistil") }
    var distance by remember { mutableIntStateOf(100) }
    var splitDistance by remember { mutableIntStateOf(25) }
    var targetTimeSecondsInput by remember { mutableStateOf("58.00") }
    var selectedStrategy by remember { mutableStateOf("Gleichmäßig") }

    val strokes = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")
    val distances = listOf(25, 50, 100, 200, 400, 800, 1500)

    val targetMillis by remember(targetTimeSecondsInput) {
        derivedStateOf {
            TimeFormatter.parseTimeInputToMillis(targetTimeSecondsInput) ?: 60_000L
        }
    }

    val splitsCount = (distance / splitDistance).coerceAtLeast(1)
    val pacePer100m = (targetMillis.toDouble() / distance.toDouble()) * 100.0

    // Compute splits based on strategy
    val computedSplits by remember(distance, splitDistance, targetMillis, selectedStrategy, stroke) {
        derivedStateOf {
            val count = (distance / splitDistance).coerceAtLeast(1)
            val baseSplit = targetMillis / count
            val lapDurations = LongArray(count) { baseSplit }

            when (selectedStrategy) {
                "Negativer Split" -> {
                    if (count >= 2) {
                        val half = count / 2
                        val deltaPerLap = (1500L / count.coerceAtLeast(2))
                        for (i in 0 until half) {
                            lapDurations[i] += deltaPerLap
                        }
                        for (i in half until count) {
                            lapDurations[i] -= deltaPerLap
                        }
                    }
                }
                "Startsprung-Bonus" -> {
                    if (count >= 2) {
                        val bonus = 1200L.coerceAtMost(baseSplit / 4)
                        lapDurations[0] -= bonus
                        val addedToOthers = bonus / (count - 1)
                        for (i in 1 until count) {
                            lapDurations[i] += addedToOthers
                        }
                    }
                }
                "Lagen-Physiologie" -> {
                    if (stroke == "Lagen" && count >= 4) {
                        val legCount = count / 4
                        val flyPct = 0.24
                        val backPct = 0.26
                        val breastPct = 0.27
                        val freePct = 0.23
                        for (i in 0 until count) {
                            val leg = (i / legCount).coerceIn(0, 3)
                            val legTotal = when (leg) {
                                0 -> (targetMillis * flyPct).toLong()
                                1 -> (targetMillis * backPct).toLong()
                                2 -> (targetMillis * breastPct).toLong()
                                else -> (targetMillis * freePct).toLong()
                            }
                            lapDurations[i] = legTotal / legCount
                        }
                    }
                }
            }

            var cum = 0L
            List(count) { idx ->
                cum += lapDurations[idx]
                val currentMeters = (idx + 1) * splitDistance
                val legStroke = if (stroke == "Lagen") {
                    val legIdx = (((idx) * splitDistance) / (distance / 4).coerceAtLeast(1)).coerceIn(0, 3)
                    when (legIdx) {
                        0 -> "Schmett"
                        1 -> "Rücken"
                        2 -> "Brust"
                        else -> "Freistil"
                    }
                } else stroke
                Triple(idx + 1, lapDurations[idx], cum) to (currentMeters to legStroke)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Universal explanation banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0C1A2A),
            border = BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Zielzeiten- & Rennplan-Rechner",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Berechnet die optimalen Zwischenzeiten (Splits) für jedes Rennen oder Trainingsset. Wähle eine Taktik (z. B. 'Negativer Split' für eine stärkere 2. Rennhälfte) und übernimm die Vorgabe direkt auf deine Stoppuhr-Bahnen!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Configuration Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OceanSurface),
            border = BorderStroke(1.dp, Color(0xFF1B2E46))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RENNDATEN & ZIELZEIT",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary,
                    textAlign = TextAlign.Center
                )

                // Stroke Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    strokes.forEach { s ->
                        FilterChip(
                            selected = stroke == s,
                            onClick = {
                                stroke = s
                                if (s == "Lagen" && distance < 100) distance = 100
                            },
                            label = { Text(s, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // Distance Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    distances.forEach { d ->
                        val isLagenDisabled = stroke == "Lagen" && d < 100
                        FilterChip(
                            selected = distance == d,
                            onClick = {
                                if (!isLagenDisabled) {
                                    distance = d
                                    if (splitDistance > d) splitDistance = 25
                                }
                            },
                            enabled = !isLagenDisabled,
                            label = { Text("${d}m", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // Split Distance Toggle (25m vs 50m)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Split-Intervalle:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(25, 50).forEach { split ->
                            val enabled = split <= distance
                            FilterChip(
                                selected = splitDistance == split,
                                onClick = { if (enabled) splitDistance = split },
                                enabled = enabled,
                                label = { Text("alle ${split}m", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AquaPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                // Target Time Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = targetTimeSecondsInput,
                        onValueChange = { targetTimeSecondsInput = it },
                        label = { Text("Zielzeit (z.B. 58.50 oder 1:02.30)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color(0xFF1B2E46),
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0D253A),
                        border = BorderStroke(1.dp, Color(0xFF1E4366)),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("100m Pace", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = TimeFormatter.formatDetailed(pacePer100m.toLong()),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = PaceGreen
                            )
                        }
                    }
                }

                // Pacing Strategies
                Text(
                    text = "Renntaktik:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val strategies = if (stroke == "Lagen") {
                        listOf("Gleichmäßig", "Lagen-Physiologie", "Negativer Split")
                    } else {
                        listOf("Gleichmäßig", "Startsprung-Bonus", "Negativer Split")
                    }
                    strategies.forEach { strat ->
                        FilterChip(
                            selected = selectedStrategy == strat,
                            onClick = { selectedStrategy = strat },
                            label = { Text(strat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD600),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                val strategyExplanation = when (selectedStrategy) {
                    "Negativer Split" -> "2. Rennhälfte ca. 1.5s schneller – spart Laktat und trainiert Stehvermögen."
                    "Startsprung-Bonus" -> "1. Bahn ca. 1.2s schneller durch den Startsprung vom Block."
                    "Lagen-Physiologie" -> "Splits angepasst an Schmett (24%), Rücken (26%), Brust (27%) und Kraul (23%)."
                    else -> "Gleichmäßiges Tempo auf allen Bahnen (ideal für Grundlagenausdauer)."
                }
                Text(
                    text = strategyExplanation,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )
            }
        }

        // Splits Table Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OceanSurface),
            border = BorderStroke(1.dp, Color(0xFF1B2E46))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BERECHNETE SPLIT-ZEITEN ($splitsCount Teilstrecken)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AquaPrimary
                    )
                    Text(
                        text = "Gesamt: ${TimeFormatter.formatDetailed(targetMillis)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite
                    )
                }

                HorizontalDivider(color = Color(0xFF1B2E46), modifier = Modifier.padding(bottom = 6.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Split", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1.2f))
                    Text("Schwimmart", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1.2f))
                    Text("Teilzeit", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    Text("Kumuliert", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = Color(0xFF142436), modifier = Modifier.padding(vertical = 4.dp))

                computedSplits.forEach { (info, meta) ->
                    val (lapNum, lapDur, cum) = info
                    val (meters, legStroke) = meta
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${meters}m (#$lapNum)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextWhite,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = legStroke,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (stroke == "Lagen") Color(0xFFFFD600) else AquaPrimary,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = TimeFormatter.formatDetailed(lapDur),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = PaceGreen,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = TimeFormatter.formatDetailed(cum),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.LightGray,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.End
                        )
                    }
                }

                if (onApplyToStopwatch != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    var appliedFeedback by remember(distance, splitDistance, targetMillis, stroke) { mutableStateOf(false) }

                    Button(
                        onClick = {
                            val avgSplit = targetMillis / splitsCount
                            onApplyToStopwatch(avgSplit, stroke, distance, splitDistance)
                            appliedFeedback = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (appliedFeedback) PaceGreen else AquaPrimary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("apply_pace_to_stopwatch_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appliedFeedback) "Soll-Pace & Strecke auf alle Bahnen übertragen!" else "Als Soll-Pace auf Stoppuhr übertragen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. STROKE RATE (SPM) TAP TEMPO TOOL
 */
@Composable
private fun StrokeRateTapView() {
    val context = LocalContext.current
    var baseCycles by remember { mutableIntStateOf(3) }
    var currentTapCount by remember { mutableIntStateOf(0) }
    var startTimestamp by remember { mutableLongStateOf(0L) }
    var calculatedSpm by remember { mutableStateOf<Double?>(null) }
    var elapsedSeconds by remember { mutableStateOf<Double?>(null) }

    fun reset() {
        currentTapCount = 0
        startTimestamp = 0L
        calculatedSpm = null
        elapsedSeconds = null
    }

    fun handleStrokeTap() {
        HapticUtil.vibrateClick(context)
        val now = SystemClock.elapsedRealtime()
        if (currentTapCount == 0) {
            startTimestamp = now
            currentTapCount = 1
            calculatedSpm = null
            elapsedSeconds = null
        } else {
            currentTapCount++
            if (currentTapCount >= baseCycles + 1) {
                val totalElapsed = (now - startTimestamp) / 1000.0
                if (totalElapsed > 0.2) {
                    val spm = (baseCycles.toDouble() / totalElapsed) * 60.0
                    calculatedSpm = spm
                    elapsedSeconds = totalElapsed
                    HapticUtil.vibrateHeavy(context)
                }
                currentTapCount = 0
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0C1A2A),
            border = BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Schlagfrequenz-Messung (SPM)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "So geht's: Wähle unten z. B. '3 Züge'. Tippe beim Eintauchen der Hand des Schwimmers auf den großen Kreis und danach im gleichen Takt die nächsten 3 Züge mit. Die App ermittelt automatisch die Züge pro Minute!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OceanSurface),
            border = BorderStroke(1.dp, Color(0xFF1B2E46)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCHLAGFREQUENZ-MESSUNG",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AquaPrimary
                    )
                    Button(
                        onClick = { reset() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2E46)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp)
                    }
                }

                Text(
                    text = "Messung über wie viele Armzüge:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { cycles ->
                        FilterChip(
                            selected = baseCycles == cycles,
                            onClick = {
                                baseCycles = cycles
                                reset()
                            },
                            label = { Text("$cycles Züge", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }
        }

        // BIG TAP BUTTON
        Surface(
            onClick = { handleStrokeTap() },
            shape = CircleShape,
            color = if (currentTapCount > 0) Color(0xFF1A384F) else Color(0xFF0F2436),
            border = BorderStroke(3.dp, if (currentTapCount > 0) AquaPrimary else Color(0xFF1B4066)),
            modifier = Modifier
                .size(190.dp)
                .testTag("spm_tap_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (currentTapCount > 0) PaceGreen else AquaPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (currentTapCount == 0) "TIPPE ZUG" else "ZUG $currentTapCount / $baseCycles",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = TextWhite,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (currentTapCount == 0) "Im Armzug-Rhythmus" else "Nächsten Zug tippen...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Live SPM Result
        if (calculatedSpm != null) {
            val spm = calculatedSpm!!
            val classification = when {
                spm < 34.0 -> "Ruhiger Grundlagenrhythmus (Langstrecke)"
                spm <= 44.0 -> "Optimales Renntempo (Mittelstrecke)"
                else -> "Hohe Sprint-Frequenz (Sprintbereich)"
            }
            val classificationColor = when {
                spm < 34.0 -> AquaPrimary
                spm <= 44.0 -> PaceGreen
                else -> Color(0xFFFFD600)
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2333)),
                border = BorderStroke(1.dp, PaceGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GEMESSENE SCHLAGFREQUENZ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PaceGreen,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f SPM", spm),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 38.sp
                        ),
                        color = TextWhite,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Dauer für $baseCycles Züge: ${String.format(Locale.US, "%.2fs", elapsedSeconds ?: 0.0)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = classificationColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, classificationColor)
                    ) {
                        Text(
                            text = classification,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = classificationColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Stroke Rate Evaluation Benchmark Guide
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OceanSurface),
            border = BorderStroke(1.dp, Color(0xFF1B2E46)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "RICHTWERTE FÜR DAS SCHWIMMTRAINING",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary
                )
                listOf(
                    "Freistil Sprint (50m/100m)" to "45 - 55+ SPM",
                    "Freistil Mittelstrecke (200m/400m)" to "34 - 42 SPM",
                    "Schmetterling" to "42 - 54 SPM",
                    "Brustschwimmen" to "32 - 42 SPM (Sprint bis 48)",
                    "Rückenschwimmen" to "38 - 48 SPM"
                ).forEach { (discipline, range) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(discipline, style = MaterialTheme.typography.bodySmall, color = TextWhite)
                        Text(
                            range,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFD600)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. POOL LENGTH CONVERTER (25m SCM vs 50m LCM)
 */
@Composable
private fun PoolLengthConverterView() {
    var sourcePool by remember { mutableStateOf("25m Kurzbahn") }
    var stroke by remember { mutableStateOf("Freistil") }
    var distance by remember { mutableIntStateOf(100) }
    var timeInput by remember { mutableStateOf("58.00") }

    val strokes = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")
    val distances = listOf(50, 100, 200, 400)

    val inputMillis = TimeFormatter.parseTimeInputToMillis(timeInput) ?: 60_000L

    val deltaPer100m = when (stroke) {
        "Brust" -> 2000L
        "Rücken" -> 1800L
        "Schmetterling" -> 1600L
        "Lagen" -> 1800L
        else -> 1400L // Freistil
    }

    val totalDelta = (deltaPer100m * (distance.toDouble() / 100.0)).toLong()
    val convertedMillis = if (sourcePool == "25m Kurzbahn") {
        inputMillis + totalDelta
    } else {
        (inputMillis - totalDelta).coerceAtLeast(1000L)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0C1A2A),
            border = BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "25m Kurzbahn (SCM) ↔ 50m Langbahn (LCM)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Warum sind Zeiten unterschiedlich? Auf der 25m-Kurzbahn hat man doppelt so viele Wenden und kraftvolle Abstoßphasen unter Wasser. Daher spart man ca. 1.4s bis 2.0s pro 100m im Vergleich zur 50m-Langbahn. Dieser Rechner rechnet nach offiziellen DSV/FINA-Formeln um.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OceanSurface),
            border = BorderStroke(1.dp, Color(0xFF1B2E46))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BAHNLÄNGEN-UMRECHNUNG",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary,
                    textAlign = TextAlign.Center
                )

                // Direction Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("25m Kurzbahn", "50m Langbahn").forEach { pool ->
                        FilterChip(
                            selected = sourcePool == pool,
                            onClick = { sourcePool = pool },
                            label = { Text("Ausgang: $pool", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Stroke Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    strokes.forEach { s ->
                        FilterChip(
                            selected = stroke == s,
                            onClick = {
                                stroke = s
                                if (s == "Lagen" && distance < 100) distance = 100
                            },
                            label = { Text(s, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // Distance Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    distances.forEach { d ->
                        val isLagenDisabled = stroke == "Lagen" && d < 100
                        FilterChip(
                            selected = distance == d,
                            onClick = { if (!isLagenDisabled) distance = d },
                            enabled = !isLagenDisabled,
                            label = { Text("${d}m", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // Time Input
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { timeInput = it },
                    label = { Text("Eingabezeit ($sourcePool)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AquaPrimary,
                        unfocusedBorderColor = Color(0xFF1B2E46),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Converted Result Display Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2636)),
            border = BorderStroke(1.dp, AquaPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val targetPoolName = if (sourcePool == "25m Kurzbahn") "50m Langbahn (LCM)" else "25m Kurzbahn (SCM)"
                Text(
                    text = "UMGERECHNETE ZEIT AUF $targetPoolName",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = TimeFormatter.formatDetailed(convertedMillis),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 36.sp
                    ),
                    color = PaceGreen,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Differenz durch Wenden & Abstoß: ${if (sourcePool == "25m Kurzbahn") "+" else "-"}${String.format(Locale.US, "%.2fs", totalDelta / 1000.0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}