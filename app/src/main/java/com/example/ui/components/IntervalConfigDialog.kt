package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.SeriesConfig
import com.example.ui.SeriesModeType
import com.example.ui.StartMode
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.TextWhite

private fun getAvailableSplitOptions(distance: Int): List<Int> = when (distance) {
    25 -> listOf(25)
    50 -> listOf(25, 50)
    100 -> listOf(25, 50, 100)
    200 -> listOf(25, 50, 100, 200)
    400 -> listOf(25, 50, 100, 200, 400)
    800 -> listOf(25, 50, 100, 200, 400, 800)
    1500 -> listOf(25, 50, 100, 200, 500, 1500)
    else -> listOf(25, 50, 100)
}

@Composable
fun IntervalConfigDialog(
    currentStartMode: StartMode,
    currentIntervalSec: Int,
    currentPoolLength: Int,
    currentDistance: Int,
    currentSplitDistance: Int = 50,
    currentStroke: String,
    currentSeriesConfig: SeriesConfig = SeriesConfig(),
    isRelayActive: Boolean = false,
    currentRelayStarterCount: Int = 4,
    onDismiss: () -> Unit,
    onSaveWithRelayCount: ((
        mode: StartMode,
        intervalSec: Int,
        poolLength: Int,
        distance: Int,
        splitDistance: Int,
        stroke: String,
        seriesConfig: SeriesConfig,
        isRelayMode: Boolean,
        relayStarterCount: Int,
    ) -> Unit)? = null,
    onSaveWithRelay: ((
        mode: StartMode,
        intervalSec: Int,
        poolLength: Int,
        distance: Int,
        splitDistance: Int,
        stroke: String,
        seriesConfig: SeriesConfig,
        isRelayMode: Boolean,
    ) -> Unit)? = null,
    onSave: (
        mode: StartMode,
        intervalSec: Int,
        poolLength: Int,
        distance: Int,
        splitDistance: Int,
        stroke: String,
        seriesConfig: SeriesConfig,
    ) -> Unit = { _, _, _, _, _, _, _ -> },
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isRelaySelected by remember { mutableStateOf(isRelayActive) }
    var relayStarterCount by remember { mutableIntStateOf(currentRelayStarterCount.coerceIn(2, 8)) }
    var mode by remember { mutableStateOf(currentStartMode) }
    var intervalSec by remember { mutableIntStateOf(currentIntervalSec) }
    var poolLength by remember { mutableIntStateOf(currentPoolLength) }
    var stroke by remember { mutableStateOf(currentStroke) }
    var distance by remember { mutableIntStateOf(currentDistance) }
    var splitDistance by remember { mutableIntStateOf(currentSplitDistance) }

    // Series Config state
    var isSeriesEnabled by remember { mutableStateOf(currentSeriesConfig.isEnabled && !isRelayActive) }
    var seriesMode by remember { mutableStateOf(currentSeriesConfig.mode) }
    var restDurationSec by remember { mutableIntStateOf(currentSeriesConfig.restDurationSeconds) }
    var sendOffDurationSec by remember { mutableIntStateOf(currentSeriesConfig.sendOffDurationSeconds) }
    var sendOffCustomText by remember { mutableStateOf(currentSeriesConfig.sendOffDurationSeconds.toString()) }
    var maxSeriesCountText by remember { mutableStateOf(currentSeriesConfig.maxSeriesCount?.toString() ?: "") }

    val restPresets = listOf(10, 15, 20, 30, 45, 60)
    val sendOffPresets = listOf(
        45 to "0:45",
        60 to "1:00",
        75 to "1:15",
        90 to "1:30",
        120 to "2:00",
    )

    val intervalOptions = listOf(5, 10, 15, 20)
    val poolOptions = listOf(25, 50)
    val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")

    val distanceOptions = if (isRelaySelected) {
        listOf(25, 50, 100).map { it * relayStarterCount }
    } else if (stroke == "Lagen") {
        listOf(100, 200, 400)
    } else {
        listOf(25, 50, 100, 200, 400, 800, 1500)
    }

    val availableSplitOptions = remember(distance, isRelaySelected, relayStarterCount) {
        if (isRelaySelected) listOf((distance / relayStarterCount.coerceAtLeast(1)).coerceAtLeast(25)) else getAvailableSplitOptions(distance)
    }
    val effectiveSplitDistance = if (splitDistance in availableSplitOptions) splitDistance else availableSplitOptions.first()

    val cardColor = Color(0xFF162536)
    val cardBorder = BorderStroke(1.dp, Color(0xFF22384F))

    Dialog(
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            onDismiss()
        }
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF101B2B),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("interval_config_dialog")
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                },
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
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

                // CARD 1: Start-Verfahren & Becken
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardColor,
                    border = cardBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Start-Verfahren & Beckenlänge",
                            fontWeight = FontWeight.Bold,
                            color = AquaPrimary,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Start-Verfahren:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = mode == StartMode.MASS,
                                    onClick = { focusManager.clearFocus(); mode = StartMode.MASS },
                                    label = { Text("Gemeinsam", maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AquaPrimary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                                FilterChip(
                                    selected = mode == StartMode.INTERVAL,
                                    onClick = { focusManager.clearFocus(); mode = StartMode.INTERVAL },
                                    label = { Text("Intervall-Start", maxLines = 1) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AquaPrimary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        if (mode == StartMode.INTERVAL) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Start-Offset pro Bahn:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    intervalOptions.forEach { sec ->
                                        FilterChip(
                                            selected = intervalSec == sec,
                                            onClick = { focusManager.clearFocus(); intervalSec = sec },
                                            label = { Text("${sec}s", maxLines = 1) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AquaPrimary,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Beckenlänge:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                poolOptions.forEach { length ->
                                    FilterChip(
                                        selected = poolLength == length,
                                        onClick = {
                                            focusManager.clearFocus()
                                            poolLength = length
                                            if (length == 50) {
                                                if (distance == 25) distance = 50
                                                if ((stroke == "Lagen" || isRelaySelected) && distance == 100) {
                                                    distance = 200
                                                    splitDistance = 50
                                                } else if (splitDistance == 25) {
                                                    splitDistance = 50
                                                }
                                            }
                                        },
                                        label = { Text("${length}m Bahn", maxLines = 1) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AquaPrimary,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // CARD 2: Disziplin & Strecke
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardColor,
                    border = cardBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isRelaySelected) "Staffel-Disziplin & Strecke" else "Disziplin & Strecke",
                            fontWeight = FontWeight.Bold,
                            color = AquaPrimary,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Schwimmart:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                strokeOptions.forEach { s ->
                                    FilterChip(
                                        selected = stroke == s,
                                        onClick = {
                                            focusManager.clearFocus()
                                            stroke = s
                                            var newDist = distance
                                            if (s == "Lagen" || isRelaySelected) {
                                                if (newDist < 100) newDist = 100
                                                if (newDist > 400) newDist = 400
                                            }
                                            distance = newDist
                                            if (s == "Lagen" || isRelaySelected) {
                                                splitDistance = (newDist / 4).coerceAtLeast(25)
                                                if (splitDistance == 25) poolLength = 25
                                            } else {
                                                val newSplits = getAvailableSplitOptions(newDist)
                                                if (splitDistance !in newSplits) {
                                                    splitDistance = newSplits.first()
                                                }
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
                        }

                        if (stroke == "Lagen") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isRelaySelected) {
                                    "1. Rücken • 2. Brust • 3. Schmett. • 4. Freistil"
                                } else {
                                    "1. Schmett. • 2. Rücken • 3. Brust • 4. Freistil"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color(0xFFFFD600)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (isRelaySelected) "Staffel-Gesamtstrecke (${relayStarterCount}×):" else "Zielstrecke:",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                distanceOptions.forEach { d ->
                                    FilterChip(
                                        selected = distance == d,
                                        onClick = {
                                            focusManager.clearFocus()
                                            distance = d
                                            if (d == 25) {
                                                poolLength = 25
                                                splitDistance = 25
                                            } else if (isRelaySelected) {
                                                val legDist = (d / relayStarterCount.coerceAtLeast(1)).coerceAtLeast(25)
                                                splitDistance = legDist
                                                if (legDist == 25) poolLength = 25
                                            } else if (stroke == "Lagen") {
                                                val legDist = (d / 4).coerceAtLeast(25)
                                                splitDistance = legDist
                                                if (legDist == 25) poolLength = 25
                                            } else {
                                                val newSplits = getAvailableSplitOptions(d)
                                                if (splitDistance !in newSplits) {
                                                    splitDistance = newSplits.first()
                                                }
                                            }
                                        },
                                        label = { Text("${d}m", maxLines = 1) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AquaPrimary,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }

                        if (!isRelaySelected) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Zwischenzeiten (Splits alle):",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    availableSplitOptions.forEach { sDist ->
                                        val labelText = if (sDist == distance) "Nur Ziel (${sDist}m)" else "Alle ${sDist}m"
                                        FilterChip(
                                            selected = effectiveSplitDistance == sDist,
                                            onClick = {
                                                focusManager.clearFocus()
                                                splitDistance = sDist
                                                if (sDist == 25) {
                                                    poolLength = 25
                                                }
                                            },
                                            label = { Text(labelText, maxLines = 1) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AquaPrimary,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // CARD 3: Serien-Modus
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardColor,
                    border = cardBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Serien-Modus",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD600),
                                fontSize = 15.sp
                            )
                            Switch(
                                checked = isSeriesEnabled,
                                onCheckedChange = { enabled ->
                                    focusManager.clearFocus()
                                    isSeriesEnabled = enabled
                                    if (enabled) {
                                        isRelaySelected = false
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = AquaPrimary,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF1A2B3C)
                                ),
                                modifier = Modifier.testTag("config_series_switch")
                            )
                        }

                        if (isSeriesEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Modus-Auswahl:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = seriesMode == SeriesModeType.MANUAL,
                                        onClick = { focusManager.clearFocus(); seriesMode = SeriesModeType.MANUAL },
                                        label = { Text("Manuell", maxLines = 1) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AquaPrimary,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                    FilterChip(
                                        selected = seriesMode == SeriesModeType.FIXED_REST,
                                        onClick = { focusManager.clearFocus(); seriesMode = SeriesModeType.FIXED_REST },
                                        label = { Text("Feste Pause", maxLines = 1) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AquaPrimary,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                    FilterChip(
                                        selected = seriesMode == SeriesModeType.SEND_OFF,
                                        onClick = { focusManager.clearFocus(); seriesMode = SeriesModeType.SEND_OFF },
                                        label = { Text("Abgangszeit", maxLines = 1) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AquaPrimary,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }

                            if (seriesMode == SeriesModeType.FIXED_REST) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Pausenzeit:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.LightGray,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        restPresets.forEach { sec ->
                                            FilterChip(
                                                selected = restDurationSec == sec,
                                                onClick = { focusManager.clearFocus(); restDurationSec = sec },
                                                label = { Text("${sec}s", maxLines = 1) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = AquaPrimary,
                                                    selectedLabelColor = Color.Black
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            if (seriesMode == SeriesModeType.SEND_OFF) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Abgangszeit (Zyklus):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.LightGray,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        sendOffPresets.forEach { (sec, label) ->
                                            FilterChip(
                                                selected = sendOffDurationSec == sec,
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    sendOffDurationSec = sec
                                                    sendOffCustomText = sec.toString()
                                                },
                                                label = { Text(label, maxLines = 1) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = AquaPrimary,
                                                    selectedLabelColor = Color.Black
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = sendOffCustomText,
                                    onValueChange = { newValue ->
                                        val digits = newValue.filter { it.isDigit() }
                                        sendOffCustomText = digits
                                        digits.toIntOrNull()?.let {
                                            if (it > 0) sendOffDurationSec = it
                                        }
                                    },
                                    label = { Text("Eigene Abgangszeit (Sekunden)", color = Color.LightGray, fontSize = 12.sp) },
                                    placeholder = { Text("z. B. 100 für 1:40", color = Color.Gray) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            sendOffCustomText.toIntOrNull()?.let {
                                                if (it > 0) sendOffDurationSec = it
                                            }
                                            keyboardController?.hide()
                                            focusManager.clearFocus(force = true)
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AquaPrimary,
                                        unfocusedBorderColor = Color(0xFF22384F),
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Maximale Serien-Anzahl:",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = maxSeriesCountText,
                                onValueChange = { newValue ->
                                    maxSeriesCountText = newValue.filter { it.isDigit() }
                                },
                                label = { Text("Serien-Anzahl (leer = unbegrenzt)", color = Color.LightGray, fontSize = 12.sp) },
                                placeholder = { Text("z. B. 5", color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus(force = true)
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AquaPrimary,
                                    unfocusedBorderColor = Color(0xFF22384F),
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // CARD 4: Staffel-Modus (genau wie Serien-Modus mit Switch)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardColor,
                    border = cardBorder,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Staffel-Modus",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD600),
                                fontSize = 15.sp
                            )
                            Switch(
                                checked = isRelaySelected,
                                onCheckedChange = { enabled ->
                                    focusManager.clearFocus()
                                    isRelaySelected = enabled
                                    if (enabled) {
                                        isSeriesEnabled = false
                                        mode = StartMode.MASS
                                        val legDist = if (splitDistance in listOf(25, 50, 100)) splitDistance else 50
                                        splitDistance = legDist
                                        distance = legDist * relayStarterCount
                                        if (legDist == 25) {
                                            poolLength = 25
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = AquaPrimary,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF1A2B3C)
                                ),
                                modifier = Modifier.testTag("config_mode_relay")
                            )
                        }

                        if (isRelaySelected) {
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Starter pro Staffel (Anzahl):",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    (2..8).forEach { count ->
                                        FilterChip(
                                            selected = relayStarterCount == count,
                                            onClick = {
                                                focusManager.clearFocus()
                                                val currentLegDist = (distance / relayStarterCount.coerceAtLeast(1)).takeIf { it in listOf(25, 50, 100) }
                                                    ?: splitDistance.takeIf { it in listOf(25, 50, 100) }
                                                    ?: 50
                                                relayStarterCount = count
                                                splitDistance = currentLegDist
                                                distance = currentLegDist * count
                                            },
                                            label = { Text("${count}×", maxLines = 1) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AquaPrimary,
                                                selectedLabelColor = Color.Black
                                            ),
                                            modifier = Modifier.testTag("config_relay_count_$count")
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Teilstrecke pro Starter (${relayStarterCount}× Starter):",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val relayLegOptions = listOf(25, 50, 100)
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    relayLegOptions.forEach { legDist ->
                                        val totalRelayDist = legDist * relayStarterCount
                                        FilterChip(
                                            selected = effectiveSplitDistance == legDist,
                                            onClick = {
                                                focusManager.clearFocus()
                                                distance = totalRelayDist
                                                splitDistance = legDist
                                                if (legDist == 25) {
                                                    poolLength = 25
                                                }
                                            },
                                            label = { Text("${relayStarterCount}×${legDist}m", maxLines = 1) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AquaPrimary,
                                                selectedLabelColor = Color.Black
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Abbrechen", color = Color.LightGray, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            sendOffCustomText.toIntOrNull()?.let {
                                if (it > 0) sendOffDurationSec = it
                            }
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            val parsedMaxCount = maxSeriesCountText.toIntOrNull()?.takeIf { it > 0 }
                            val newConfig = SeriesConfig(
                                isEnabled = isSeriesEnabled && !isRelaySelected,
                                mode = seriesMode,
                                restDurationSeconds = restDurationSec,
                                sendOffDurationSeconds = sendOffDurationSec,
                                maxSeriesCount = parsedMaxCount
                            )
                            when {
                                onSaveWithRelayCount != null -> {
                                    onSaveWithRelayCount(
                                        mode,
                                        intervalSec,
                                        poolLength,
                                        distance,
                                        effectiveSplitDistance,
                                        stroke,
                                        newConfig,
                                        isRelaySelected,
                                        relayStarterCount
                                    )
                                }
                                onSaveWithRelay != null -> {
                                    onSaveWithRelay(
                                        mode,
                                        intervalSec,
                                        poolLength,
                                        distance,
                                        effectiveSplitDistance,
                                        stroke,
                                        newConfig,
                                        isRelaySelected
                                    )
                                }
                                else -> {
                                    onSave(mode, intervalSec, poolLength, distance, effectiveSplitDistance, stroke, newConfig)
                                }
                            }
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
