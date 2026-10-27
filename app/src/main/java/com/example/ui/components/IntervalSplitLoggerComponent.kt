package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.PaceRed
import com.example.ui.theme.TextWhite
import com.example.util.TimeFormatter

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList

@Immutable
data class RepetitionSplit(
    val repIndex: Int,
    val targetMillis: Long?,
    val actualMillis: Long? = null,
    val inputText: String = ""
)

private val repSplitsSaver = listSaver<SnapshotStateList<RepetitionSplit>, String>(
    save = { list ->
        list.map { "${it.repIndex}\u001F${it.targetMillis ?: ""}\u001F${it.actualMillis ?: ""}\u001F${it.inputText}" }
    },
    restore = { savedList ->
        val stateList = mutableStateListOf<RepetitionSplit>()
        savedList.forEach { itemStr ->
            val parts = itemStr.split("\u001F", limit = 4)
            if (parts.size == 4) {
                val repIdx = parts[0].toIntOrNull() ?: 1
                val targetMs = parts[1].toLongOrNull()
                val actualMs = parts[2].toLongOrNull()
                val txt = parts[3]
                stateList.add(
                    RepetitionSplit(
                        repIndex = repIdx,
                        targetMillis = targetMs,
                        actualMillis = actualMs,
                        inputText = txt
                    )
                )
            }
        }
        stateList
    }
)

@Composable
fun IntervalSplitLoggerComponent(
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    var repCountText by rememberSaveable { mutableStateOf("8") }
    var distanceMetersText by rememberSaveable { mutableStateOf("50") }
    var targetInputText by rememberSaveable { mutableStateOf("") }
    var restPauseText by rememberSaveable { mutableStateOf("20") }

    val targetMillis = remember(targetInputText) {
        TimeFormatter.parseTimeInputToMillis(targetInputText)
    }

    val restSeconds = remember(restPauseText) {
        restPauseText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    }

    val restMillis = remember(restSeconds) { restSeconds * 1000L }

    val repCount = remember(repCountText) {
        repCountText.toIntOrNull()?.coerceIn(1, 30) ?: 8
    }

    val distanceMeters = remember(distanceMetersText) {
        distanceMetersText.toIntOrNull()?.coerceIn(25, 1500) ?: 50
    }

    val repSplits = rememberSaveable(saver = repSplitsSaver) {
        mutableStateListOf<RepetitionSplit>()
    }

    LaunchedEffect(repCount, targetMillis) {
        for (i in 0 until repSplits.size) {
            val current = repSplits[i]
            if (current.targetMillis != targetMillis) {
                repSplits[i] = current.copy(targetMillis = targetMillis)
            }
        }
        if (repSplits.size < repCount) {
            for (i in (repSplits.size + 1)..repCount) {
                repSplits.add(RepetitionSplit(repIndex = i, targetMillis = targetMillis))
            }
        } else if (repSplits.size > repCount) {
            while (repSplits.size > repCount) {
                repSplits.removeAt(repSplits.lastIndex)
            }
        }
    }

    val recordedSplits by remember(repSplits) {
        derivedStateOf { repSplits.mapNotNull { split -> split.actualMillis } }
    }

    val avgMillis by remember(recordedSplits) {
        derivedStateOf {
            if (recordedSplits.isEmpty()) null else recordedSplits.average().toLong()
        }
    }

    val sendOffMillis = remember(targetMillis, restMillis) {
        val target = targetMillis
        if (target != null && restMillis > 0L) target + restMillis else null
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Setup Card: Target Pace, Distance, Repetitions & Pause
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = OceanSurface),
            border = BorderStroke(1.dp, Color(0xFF1B2E46)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header with Title and info badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Serien-Konfiguration (${repCount}x ${distanceMeters}m)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AquaPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (sendOffMillis != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFD600).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFFFD600)),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "Abgang alle ${TimeFormatter.formatMillis(sendOffMillis, shortFormat = true)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFD600),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    } else if (restSeconds > 0L) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AquaPrimary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, AquaPrimary),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = "${restSeconds}s Pause nach Sprint",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AquaPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Row 1: Reps & Distance
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = repCountText,
                        onValueChange = { repCountText = it },
                        label = { Text("Anzahl (z.B. 8)", fontSize = 11.sp, color = Color.LightGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0C1623),
                            unfocusedContainerColor = Color(0xFF0C1623),
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color(0xFF1B2E46),
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = distanceMetersText,
                        onValueChange = { distanceMetersText = it },
                        label = { Text("Strecke (m)", fontSize = 11.sp, color = Color.LightGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0C1623),
                            unfocusedContainerColor = Color(0xFF0C1623),
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color(0xFF1B2E46),
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Input Row 2: Pause & Optional Soll-Zeit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = restPauseText,
                        onValueChange = { restPauseText = it },
                        label = { Text("Pause (Sek, z.B. 20)", fontSize = 11.sp, color = Color.LightGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0C1623),
                            unfocusedContainerColor = Color(0xFF0C1623),
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color(0xFF1B2E46),
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.weight(1.2f)
                    )

                    OutlinedTextField(
                        value = targetInputText,
                        onValueChange = { targetInputText = it },
                        label = { Text("Soll-Zeit (optional)", fontSize = 11.sp, color = Color.LightGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0C1623),
                            unfocusedContainerColor = Color(0xFF0C1623),
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color(0xFF1B2E46),
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        modifier = Modifier.weight(1.2f)
                    )
                }

                if (avgMillis != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Erfasst: ${recordedSplits.size} / $repCount (${repCount * distanceMeters}m)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                        Text(
                            text = "Ø Ist-Zeit: ${TimeFormatter.formatMillis(avgMillis!!)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = AquaPrimary
                        )
                    }
                }
            }
        }

        // Repetitions Splits Entry List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            itemsIndexed(
                items = repSplits,
                key = { _, item -> item.repIndex },
                contentType = { _, _ -> "RepetitionSplitCard" }
            ) { index, item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = OceanSurface),
                    border = BorderStroke(1.dp, Color(0xFF1B2E46)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rep Number Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AquaPrimary,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = item.repIndex.toString(),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Wdh ${item.repIndex} (${distanceMeters}m)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite
                            )
                        }

                        // Actual Time Entry & Delta Display
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            val actual = item.actualMillis
                            val target = item.targetMillis

                            if (actual != null && target != null) {
                                val delta = actual - target
                                val isFaster = delta <= 0
                                Text(
                                    text = TimeFormatter.formatDelta(delta),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                    color = if (isFaster) PaceGreen else PaceRed,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }

                            OutlinedTextField(
                                value = item.inputText,
                                onValueChange = { input ->
                                    repSplits[index] = item.copy(
                                        inputText = input,
                                        actualMillis = TimeFormatter.parseTimeInputToMillis(input)
                                    )
                                },
                                placeholder = { Text(if (target != null) TimeFormatter.formatMillis(target, shortFormat = true) else "00:28.50", fontSize = 12.sp, color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0C1623),
                                    unfocusedContainerColor = Color(0xFF0C1623),
                                    focusedBorderColor = AquaPrimary,
                                    unfocusedBorderColor = Color(0xFF1B2E46),
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
