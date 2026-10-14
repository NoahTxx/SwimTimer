package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SessionWithLaps
import com.example.data.model.SwimmerEntity
import com.example.ui.StopwatchViewModel
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.DeepOceanBackground
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.PaceRed
import com.example.ui.theme.TextWhite
import com.example.util.TimeFormatter

@Composable
fun SwimmerDetailScreen(
    swimmer: SwimmerEntity,
    allSessions: List<SessionWithLaps>,
    onBack: () -> Unit,
    onUpdateSwimmer: (SwimmerEntity) -> Unit,
    onDeleteSwimmer: (SwimmerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var isEditDialogOpen by remember { mutableStateOf(false) }
    var isDeleteConfirmOpen by remember { mutableStateOf(false) }

    // Filter sessions that have laps for this swimmer
    val swimmerSessions = remember(allSessions, swimmer.id) {
        allSessions.filter { sessionWithLaps ->
            sessionWithLaps.laps.any { it.swimmerId == swimmer.id }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOceanBackground)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("swimmer_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                            tint = AquaPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Athletenprofil",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextWhite,
                        maxLines = 1
                    )
                }

                Row {
                    IconButton(
                        onClick = { isEditDialogOpen = true },
                        modifier = Modifier.testTag("edit_swimmer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Bearbeiten",
                            tint = AquaPrimary
                        )
                    }
                    IconButton(
                        onClick = { isDeleteConfirmOpen = true },
                        modifier = Modifier.testTag("delete_swimmer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Löschen",
                            tint = Color(0xFFFF5252)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Swimmer Hero Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = OceanSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(swimmer.colorHex),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = swimmer.name.take(1).uppercase(),
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = swimmer.name,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        ),
                                        color = TextWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF132A38)
                                    ) {
                                        Text(
                                            text = "Hauptlage: ${swimmer.favoriteStroke}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = AquaPrimary,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Best Times (PB) Section: 25m, 50m, 100m
                            Text(
                                text = "PERSÖNLICHE BESTZEITEN (PB)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.LightGray,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 25m Sprint PB Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F1E2E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = "25m PB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.LightGray,
                                            maxLines = 1
                                        )
                                        val pb25Text = swimmer.pb25mMillis?.let {
                                            TimeFormatter.formatMillis(it, shortFormat = true)
                                        } ?: "--:--"
                                        Text(
                                            text = pb25Text,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = AquaPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 50m PB Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F1E2E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = "50m PB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.LightGray,
                                            maxLines = 1
                                        )
                                        val pb50Text = swimmer.pb50mMillis?.let {
                                            TimeFormatter.formatMillis(it, shortFormat = true)
                                        } ?: "--:--"
                                        Text(
                                            text = pb50Text,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = AquaPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 100m PB Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F1E2E),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = "100m PB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.LightGray,
                                            maxLines = 1
                                        )
                                        val pb100Text = swimmer.pb100mMillis?.let {
                                            TimeFormatter.formatMillis(it)
                                        } ?: "--:--"
                                        Text(
                                            text = pb100Text,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            ),
                                            color = Color(0xFFFFD600),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Official Lagen (Individual Medley: Schmett -> Rücken -> Brust -> Kraul)
                            var selectedLagenSplitMode by remember { mutableIntStateOf(200) } // 200m (4x50m), 100m (4x25m), or 400m (4x100m)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0A1828),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B3550)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "LAGEN WETTKAMPF-ZEITEN",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.8.sp
                                                ),
                                                color = AquaPrimary,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Offizielle Gesamtdistanzen",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color.Gray,
                                                maxLines = 1
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF162B3D)
                                        ) {
                                            Text(
                                                text = "PB & Splits",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                color = AquaPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Total Lagen Best Times (100m, 200m, 400m)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 100m Lagen Total PB
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF102236),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("100m Lagen", style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
                                                val t = swimmer.pb100mLagenMillis?.let { TimeFormatter.formatMillis(it) } ?: "--:--"
                                                Text(
                                                    text = t,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFFFFD600),
                                                    maxLines = 1
                                                )
                                                Text("Kurzbahn", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.DarkGray, maxLines = 1)
                                            }
                                        }

                                        // 200m Lagen Total PB
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF102236),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("200m Lagen", style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
                                                val t = swimmer.pb200mLagenMillis?.let { TimeFormatter.formatMillis(it) } ?: "--:--"
                                                Text(
                                                    text = t,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = AquaPrimary,
                                                    maxLines = 1
                                                )
                                                Text("Olympisch", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.DarkGray, maxLines = 1)
                                            }
                                        }

                                        // 400m Lagen Total PB
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF102236),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text("400m Lagen", style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
                                                val t = swimmer.pb400mLagenMillis?.let { TimeFormatter.formatMillis(it) } ?: "--:--"
                                                Text(
                                                    text = t,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = Color(0xFF80D8FF),
                                                    maxLines = 1
                                                )
                                                Text("Langdistanz", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.DarkGray, maxLines = 1)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Lagen Teilstrecken Selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Splits:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextWhite,
                                            maxLines = 1
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (selectedLagenSplitMode == 200) AquaPrimary else Color(0xFF132333),
                                                modifier = Modifier.clickable { selectedLagenSplitMode = 200 }
                                            ) {
                                                Text(
                                                    text = "200m (4×50m)",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (selectedLagenSplitMode == 200) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = if (selectedLagenSplitMode == 200) Color.Black else Color.LightGray,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    maxLines = 1
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (selectedLagenSplitMode == 100) AquaPrimary else Color(0xFF132333),
                                                modifier = Modifier.clickable { selectedLagenSplitMode = 100 }
                                            ) {
                                                Text(
                                                    text = "100m (4×25m)",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (selectedLagenSplitMode == 100) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = if (selectedLagenSplitMode == 100) Color.Black else Color.LightGray,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    maxLines = 1
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (selectedLagenSplitMode == 400) AquaPrimary else Color(0xFF132333),
                                                modifier = Modifier.clickable { selectedLagenSplitMode = 400 }
                                            ) {
                                                Text(
                                                    text = "400m (4×100m)",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (selectedLagenSplitMode == 400) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = if (selectedLagenSplitMode == 400) Color.Black else Color.LightGray,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (selectedLagenSplitMode == 200) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF201318),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("1. Schmett", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFFF8A80), maxLines = 1)
                                                    Text("50m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.pb200mLagenFlySplitMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite, maxLines = 1)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF201E10),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFFD600).copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("2. Rücken", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFFFE082), maxLines = 1)
                                                    Text("50m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.pb200mLagenBackSplitMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite, maxLines = 1)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0E2018),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00E676).copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("3. Brust", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFA7FFEB), maxLines = 1)
                                                    Text("50m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.pb200mLagenBreastSplitMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite, maxLines = 1)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0C2025),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.8f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("4. Kraul", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color(0xFF80D8FF), maxLines = 1)
                                                    Text("50m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.pb200mLagenFreeSplitMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) }
                                                        ?: if (StopwatchViewModel.isSameStroke(swimmer.favoriteStroke, "Freistil")) swimmer.pb50mMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } else null
                                                        ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = AquaPrimary, maxLines = 1)
                                                }
                                            }
                                        }

                                        val splits200 = listOfNotNull(
                                            swimmer.pb200mLagenFlySplitMillis,
                                            swimmer.pb200mLagenBackSplitMillis,
                                            swimmer.pb200mLagenBreastSplitMillis,
                                            swimmer.pb200mLagenFreeSplitMillis ?: if (StopwatchViewModel.isSameStroke(swimmer.favoriteStroke, "Freistil")) swimmer.pb50mMillis else null
                                        )
                                        if (splits200.size == 4) {
                                            val theoreticalSum = splits200.sum()
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0D2535),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Theoretische 200m Ideal-Lagen (4×50m):", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                                        Text(TimeFormatter.formatMillis(theoreticalSum), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = AquaPrimary)
                                                    }
                                                    val actual200m = swimmer.pb200mLagenMillis
                                                    if (actual200m != null) {
                                                        val diff = actual200m - theoreticalSum
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("Potenzial vs. 200m PB:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                            Text(
                                                                text = if (diff > 0) "-${TimeFormatter.formatMillis(diff, shortFormat = true)} möglich" else "Optimale Renneinteilung!",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = if (diff > 0) PaceGreen else Color(0xFFFFD600)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (selectedLagenSplitMode == 400) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF201318),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("1. Schmett", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFFF8A80), maxLines = 1)
                                                    Text("100m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.pb400mLagenFlySplitMillis?.let { TimeFormatter.formatMillis(it) } ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite, maxLines = 1)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF201E10),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFFD600).copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("2. Rücken", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFFFE082), maxLines = 1)
                                                    Text("100m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.pb400mLagenBackSplitMillis?.let { TimeFormatter.formatMillis(it) } ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite, maxLines = 1)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0E2018),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00E676).copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("3. Brust", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFA7FFEB), maxLines = 1)
                                                    Text("100m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.pb400mLagenBreastSplitMillis?.let { TimeFormatter.formatMillis(it) } ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite, maxLines = 1)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0C2025),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.8f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("4. Kraul", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color(0xFF80D8FF), maxLines = 1)
                                                    Text("100m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.pb400mLagenFreeSplitMillis?.let { TimeFormatter.formatMillis(it) }
                                                        ?: if (StopwatchViewModel.isSameStroke(swimmer.favoriteStroke, "Freistil")) swimmer.pb100mMillis?.let { TimeFormatter.formatMillis(it) } else null
                                                        ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = AquaPrimary, maxLines = 1)
                                                }
                                            }
                                        }

                                        val splits400 = listOfNotNull(
                                            swimmer.pb400mLagenFlySplitMillis,
                                            swimmer.pb400mLagenBackSplitMillis,
                                            swimmer.pb400mLagenBreastSplitMillis,
                                            swimmer.pb400mLagenFreeSplitMillis ?: if (StopwatchViewModel.isSameStroke(swimmer.favoriteStroke, "Freistil")) swimmer.pb100mMillis else null
                                        )
                                        if (splits400.size == 4) {
                                            val theoreticalSum = splits400.sum()
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0D2535),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Theoretische 400m Ideal-Lagen (4×100m):", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                                        Text(TimeFormatter.formatMillis(theoreticalSum), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = AquaPrimary)
                                                    }
                                                    val actual400m = swimmer.pb400mLagenMillis
                                                    if (actual400m != null) {
                                                        val diff = actual400m - theoreticalSum
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("Potenzial vs. 400m PB:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                            Text(
                                                                text = if (diff > 0) "-${TimeFormatter.formatMillis(diff)} möglich" else "Optimale Renneinteilung!",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = if (diff > 0) PaceGreen else Color(0xFFFFD600)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF201318),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFF5252).copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("1. Schmett", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFFF8A80), maxLines = 1)
                                                    Text("25m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.lagenFlySplitMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite, maxLines = 1)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF201E10),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFFD600).copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("2. Rücken", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFFFE082), maxLines = 1)
                                                    Text("25m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.lagenBackSplitMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite, maxLines = 1)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0E2018),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF00E676).copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("3. Brust", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFFA7FFEB), maxLines = 1)
                                                    Text("25m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.lagenBreastSplitMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = TextWhite, maxLines = 1)
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0C2025),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.8f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Text("4. Kraul", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color(0xFF80D8FF), maxLines = 1)
                                                    Text("25m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.lagenFreeSplitMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) }
                                                        ?: if (StopwatchViewModel.isSameStroke(swimmer.favoriteStroke, "Freistil")) swimmer.pb25mMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } else null
                                                        ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = AquaPrimary, maxLines = 1)
                                                }
                                            }
                                        }

                                        val splits100 = listOfNotNull(
                                            swimmer.lagenFlySplitMillis,
                                            swimmer.lagenBackSplitMillis,
                                            swimmer.lagenBreastSplitMillis,
                                            swimmer.lagenFreeSplitMillis ?: if (StopwatchViewModel.isSameStroke(swimmer.favoriteStroke, "Freistil")) swimmer.pb25mMillis else null
                                        )
                                        if (splits100.size == 4) {
                                            val theoreticalSum = splits100.sum()
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0D2535),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Theoretische 100m Ideal-Lagen (4×25m):", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                                                        Text(TimeFormatter.formatMillis(theoreticalSum), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = AquaPrimary)
                                                    }
                                                    val actual100m = swimmer.pb100mLagenMillis
                                                    if (actual100m != null) {
                                                        val diff = actual100m - theoreticalSum
                                                        Spacer(modifier = Modifier.height(3.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("Potenzial vs. 100m PB:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                            Text(
                                                                text = if (diff > 0) "-${TimeFormatter.formatMillis(diff, shortFormat = true)} möglich" else "Optimale Renneinteilung!",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = if (diff > 0) PaceGreen else Color(0xFFFFD600)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // History Title Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = AquaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Persönliche Historie",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextWhite,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "${swimmerSessions.size} Einheiten",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                    }
                }

                // Sessions List
                if (swimmerSessions.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Noch keine Einheiten gestoppt",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.LightGray,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Weise ${swimmer.name} auf der Stoppuhr einer Bahn zu und speichere eine Einheit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(swimmerSessions, key = { it.session.id }) { sessionWithLaps ->
                        SwimmerSessionHistoryCard(
                            sessionWithLaps = sessionWithLaps,
                            swimmer = swimmer
                        )
                    }
                }
            }
        }

        // Edit Dialog
        if (isEditDialogOpen) {
            EditSwimmerDialog(
                swimmer = swimmer,
                onDismiss = { isEditDialogOpen = false },
                onSave = { updated ->
                    onUpdateSwimmer(updated)
                    isEditDialogOpen = false
                }
            )
        }

        // Delete Confirm Dialog
        if (isDeleteConfirmOpen) {
            AlertDialog(
                onDismissRequest = { isDeleteConfirmOpen = false },
                title = { Text("Schwimmer löschen", color = TextWhite) },
                text = { Text("Möchtest du ${swimmer.name} wirklich aus dem Team entfernen?", color = Color.LightGray) },
                containerColor = Color(0xFF101B2B),
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteSwimmer(swimmer)
                            isDeleteConfirmOpen = false
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                    ) {
                        Text("Löschen", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { isDeleteConfirmOpen = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Abbrechen", color = Color.LightGray)
                    }
                }
            )
        }
    }
}

@Composable
private fun SwimmerSessionHistoryCard(
    sessionWithLaps: SessionWithLaps,
    swimmer: SwimmerEntity
) {
    var isExpanded by remember { mutableStateOf(false) }
    val s = sessionWithLaps.session
    val swimmerLaps = sessionWithLaps.laps
        .filter { it.swimmerId == swimmer.id }
        .sortedBy { it.lapIndex }

    val finishTime = swimmerLaps.lastOrNull()?.splitTimeMillis ?: