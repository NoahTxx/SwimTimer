package com.example.ui.components

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.TextWhite
import com.example.util.HapticUtil
import java.util.Locale

@Composable
fun StrokeRateCalculatorDialog(
    targetLane: Int?,
    onDismiss: () -> Unit,
    onAttachToLane: ((laneNumber: Int, spm: Double) -> Unit)? = null,
) {
    val context = LocalContext.current
    var baseCycles by remember { mutableIntStateOf(3) } // Standard is 3 strokes
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
            if (currentTapCount >= (baseCycles + 1)) { // 0 -> 1 -> 2 -> 3 (e.g. 3 cycles)
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F1A28),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("stroke_rate_dialog"),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = AquaPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Schlagfrequenz",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Schließen",
                            tint = Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Base cycles selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Basis:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp)
                    )
                    listOf(1, 2, 3).forEach { cycles ->
                        FilterChip(
                            selected = baseCycles == cycles,
                            onClick = {
                                baseCycles = cycles
                                reset()
                            },
                            label = { Text("$cycles Züge") },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AquaPrimary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Result Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF162538)),
                    contentAlignment = Alignment.Center
                ) {
                    if (calculatedSpm != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.US, "%.1f", calculatedSpm),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = PaceGreen
                            )
                            Text(
                                text = String.format(Locale.US, "Züge / min (%.2f s für %d Züge)", elapsedSeconds ?: 0.0, baseCycles),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    } else if (currentTapCount > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Zug $currentTapCount von $baseCycles",
                                style = MaterialTheme.typography.headlineMedium,
                                color = AquaPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Im Rhythmus der Handeintauchung weiter tippen!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "--.- SPM",
                                style = MaterialTheme.typography.displayMedium,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "Tippen starten beim ersten Armzug",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Massive Tap Area (Coach friendly!)
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(if (currentTapCount > 0) AquaPrimary else Color(0xFF00838F))
                        .clickable { handleStrokeTap() }
                        .testTag("stroke_rate_tap_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = if (currentTapCount == 0) "START" else "ZUG $currentTapCount",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp
                            ),
                            color = if (currentTapCount > 0) Color.Black else Color.White
                        )
                        Text(
                            text = "HIER TIPPEN",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentTapCount > 0) Color.DarkGray else Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { reset() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Zurücksetzen",
                            tint = Color.LightGray
                        )
                    }

                    if ((targetLane != null) && (calculatedSpm != null) && (onAttachToLane != null)) {
                        Button(
                            onClick = {
                                onAttachToLane(targetLane, calculatedSpm!!)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PaceGreen)
                        ) {
                            Text("An Bahn $targetLane anhängen", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0A1128)
@Composable
fun StrokeRateCalculatorDialogPreview() {
    com.example.ui.theme.SwimTimerTheme {
        StrokeRateCalculatorDialog(
            targetLane = 1,
            onDismiss = {},
            onAttachToLane = { _, _ -> }
        )
    }
}
