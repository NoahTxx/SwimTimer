package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SwimmerEntity
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.TextWhite
import com.example.util.TimeFormatter
import java.util.Locale

@Composable
fun TargetPaceDialog(
    laneNumber: Int,
    currentPaceMillis: Long?,
    assignedSwimmer: SwimmerEntity? = null,
    @Suppress("UNUSED_PARAMETER") targetDistanceMeters: Int = 100,
    splitDistanceMeters: Int = 50,
    strokeType: String = "Freistil",
    onDismiss: () -> Unit,
    onSavePace: (Long?) -> Unit,
) {
    var inputSeconds by remember {
        mutableStateOf(
            if (currentPaceMillis != null) {
                String.format(Locale.US, "%.2f", currentPaceMillis / 1000.0)
            } else "",
        )
    }

    // Determine reference PB for the split distance
    val referencePbMillis: Long? = assignedSwimmer?.let { swimmer ->
        when (splitDistanceMeters) {
            25 -> swimmer.pb25mMillis ?: (swimmer.pb50mMillis?.let { it / 2 })
            50 -> swimmer.pb50mMillis ?: (swimmer.pb100mMillis?.let { it / 2 })
            100 -> if (strokeType == "Lagen") swimmer.pb100mLagenMillis ?: swimmer.pb100mMillis else swimmer.pb100mMillis
            200 -> if (strokeType == "Lagen") swimmer.pb200mLagenMillis ?: (swimmer.pb100mMillis?.let { it * 2 }) else (swimmer.pb100mMillis?.let { it * 2 })
            400 -> if (strokeType == "Lagen") swimmer.pb400mLagenMillis else (swimmer.pb100mMillis?.let { it * 4 })
            else -> swimmer.pb100mMillis?.let { it * (splitDistanceMeters / 100) }
        }
    }

    val quickPaces = listOf(12.0, 14.0, 16.0, 26.0, 28.0, 30.0, 32.0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF101B2B),
            modifier = Modifier.fillMaxWidth().testTag("target_pace_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Soll-Pace für Bahn $laneNumber",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Gibt die Richtzeit pro Bahn/Split vor. Die App färbt Zeiten unter Vorgabe grün, darüber rot.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                OutlinedTextField(
                    value = inputSeconds,
                    onValueChange = { inputSeconds = it },
                    label = { Text("Zielzeit in Sekunden (z. B. 31.50)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AquaPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = AquaPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Athlete-specific training zones
                if (referencePbMillis != null) {
                    val pbSec = referencePbMillis / 1000.0
                    val ga1Sec = pbSec * 1.20
                    val ga2Sec = pbSec * 1.10
                    val wsaSec = pbSec * 1.04

                    Text(
                        text = "Trainingszonen (${assignedSwimmer.name}):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFD600),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // GA1 Chip
                        FilterChip(
                            selected = inputSeconds == String.format(Locale.US, "%.2f", ga1Sec),
                            onClick = { inputSeconds = String.format(Locale.US, "%.2f", ga1Sec) },
                            label = { Text(String.format(Locale.GERMANY, "GA1 (%.1fs)", ga1Sec)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E676),
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        // GA2 Chip
                        FilterChip(
                            selected = inputSeconds == String.format(Locale.US, "%.2f", ga2Sec),
                            onClick = { inputSeconds = String.format(Locale.US, "%.2f", ga2Sec) },
                            label = { Text(String.format(Locale.GERMANY, "GA2 (%.1fs)", ga2Sec)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFD600),
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // WSA Chip
                        FilterChip(
                            selected = inputSeconds == String.format(Locale.US, "%.2f", wsaSec),
                            onClick = { inputSeconds = String.format(Locale.US, "%.2f", wsaSec) },
                            label = { Text(String.format(Locale.GERMANY, "WSA (%.1fs)", wsaSec)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF9100),
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        // Sprint Chip
                        FilterChip(
                            selected = inputSeconds == String.format(Locale.US, "%.2f", pbSec),
                            onClick = { inputSeconds = String.format(Locale.US, "%.2f", pbSec) },
                            label = { Text(String.format(Locale.GERMANY, "PB (%.1fs)", pbSec)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Quick selector chips
                Text(
                    text = "Feste Zeiten:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.align(Alignment.Start)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    quickPaces.forEach { pace ->
                        FilterChip(
                            selected = (inputSeconds == String.format(Locale.US, "%.1f", pace)) ||
                                    (inputSeconds == String.format(Locale.US, "%.2f", pace)),
                            onClick = {
                                inputSeconds = String.format(Locale.US, "%.2f", pace)
                            },
                            label = { Text("${pace.toInt()}s") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = { onSavePace(null) }
                    ) {
                        Text("Zurücksetzen", color = Color.LightGray)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            val millis = TimeFormatter.parseTimeInputToMillis(inputSeconds)
                            if (millis != null && millis > 0) {
                                onSavePace(millis)
                            } else {
                                onSavePace(null)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PaceGreen)
                    ) {
                        Text("Speichern", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
