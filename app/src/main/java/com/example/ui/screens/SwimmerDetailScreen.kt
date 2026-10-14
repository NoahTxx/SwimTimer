package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
                            var selectedLagenSplitMode by remember { mutableStateOf(200) } // 200m (4x50m) or 100m (4x25m)

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

                                    // Lagen Teilstrecken Selector: 200m (4x50m) vs 100m (4x25m)
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

                                        // Distance toggle chips
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (selectedLagenSplitMode == 200) AquaPrimary else Color(0xFF132333),
                                                modifier = Modifier.clickable { selectedLagenSplitMode = 200 }
                                            ) {
                                                Text(
                                                    text = "200m (4× 50m)",
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
                                                    text = "100m (4× 25m)",
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
                                                    text = "400m (4× 100m)",
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
                                        // 200m Lagen Teilstrecken (je 50m)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // 1. 50m Schmett
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

                                            // 2. 50m Rücken
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

                                            // 3. 50m Brust
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

                                            // 4. 50m Freistil / Kraul
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0C2025),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.8f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("4. Kraul", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = Color(0xFF80D8FF), maxLines = 1)
                                                    }
                                                    Text("50m", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color.Gray, maxLines = 1)
                                                    val t = swimmer.pb200mLagenFreeSplitMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) }
                                                        ?: if (swimmer.favoriteStroke.equals("Freistil", ignoreCase = true) || swimmer.favoriteStroke.equals("Kraul", ignoreCase = true)) swimmer.pb50mMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } else null
                                                        ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = AquaPrimary, maxLines = 1)
                                                }
                                            }
                                        }

                                        // Theoretical Best 200m Medley Sum
                                        val splits200 = listOfNotNull(
                                            swimmer.pb200mLagenFlySplitMillis,
                                            swimmer.pb200mLagenBackSplitMillis,
                                            swimmer.pb200mLagenBreastSplitMillis,
                                            swimmer.pb200mLagenFreeSplitMillis ?: if (swimmer.favoriteStroke.equals("Freistil", ignoreCase = true) || swimmer.favoriteStroke.equals("Kraul", ignoreCase = true)) swimmer.pb50mMillis else null
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
                                                        Text("Theoretische 200m Ideal-Lagen (4x50m):", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
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
                                        // 400m Lagen Teilstrecken (je 100m)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // 1. 100m Schmett
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

                                            // 2. 100m Rücken
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

                                            // 3. 100m Brust
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

                                            // 4. 100m Freistil / Kraul (mit 100m PB Auto-Sync)
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
                                                        ?: if (swimmer.favoriteStroke.equals("Freistil", ignoreCase = true) || swimmer.favoriteStroke.equals("Kraul", ignoreCase = true)) swimmer.pb100mMillis?.let { TimeFormatter.formatMillis(it) } else null
                                                        ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = AquaPrimary, maxLines = 1)
                                                }
                                            }
                                        }

                                        // Theoretical Best 400m Medley Sum
                                        val splits400 = listOfNotNull(
                                            swimmer.pb400mLagenFlySplitMillis,
                                            swimmer.pb400mLagenBackSplitMillis,
                                             swimmer.pb400mLagenBreastSplitMillis,
                                            swimmer.pb400mLagenFreeSplitMillis ?: if (swimmer.favoriteStroke.equals("Freistil", ignoreCase = true) || swimmer.favoriteStroke.equals("Kraul", ignoreCase = true)) swimmer.pb100mMillis else null
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
                                                        Text("Theoretische 400m Ideal-Lagen (4x100m):", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
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
                                        // 100m Lagen Teilstrecken (je 25m)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // 1. 25m Schmett
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

                                            // 2. 25m Rücken
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

                                            // 3. 25m Brust
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

                                            // 4. 25m Freistil / Kraul (mit 25m Sprint Auto-Sync)
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
                                                        ?: if (swimmer.favoriteStroke.equals("Freistil", ignoreCase = true) || swimmer.favoriteStroke.equals("Kraul", ignoreCase = true)) swimmer.pb25mMillis?.let { TimeFormatter.formatMillis(it, shortFormat = true) } else null
                                                        ?: "--:--"
                                                    Text(t, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = AquaPrimary, maxLines = 1)
                                                }
                                            }
                                        }

                                        // Theoretical Best 100m Medley Sum
                                        val splits100 = listOfNotNull(
                                            swimmer.lagenFlySplitMillis,
                                            swimmer.lagenBackSplitMillis,
                                            swimmer.lagenBreastSplitMillis,
                                            swimmer.lagenFreeSplitMillis ?: if (swimmer.favoriteStroke.equals("Freistil", ignoreCase = true) || swimmer.favoriteStroke.equals("Kraul", ignoreCase = true)) swimmer.pb25mMillis else null
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
                                                        Text("Theoretische 100m Ideal-Lagen (4x25m):", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
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

    val finishTime = swimmerLaps.lastOrNull()?.splitTimeMillis ?: 0L
    val isPB = when {
        s.strokeType == "Lagen" && s.targetDistanceMeters == 100 -> swimmer.pb100mLagenMillis != null && swimmer.pb100mLagenMillis == finishTime
        s.strokeType == "Lagen" && s.targetDistanceMeters == 200 -> swimmer.pb200mLagenMillis != null && swimmer.pb200mLagenMillis == finishTime
        s.strokeType == "Lagen" && s.targetDistanceMeters == 400 -> swimmer.pb400mLagenMillis != null && swimmer.pb400mLagenMillis == finishTime
        s.targetDistanceMeters == 25 -> swimmer.pb25mMillis != null && swimmer.pb25mMillis == finishTime
        s.targetDistanceMeters == 50 -> swimmer.pb50mMillis != null && swimmer.pb50mMillis == finishTime
        s.targetDistanceMeters == 100 -> swimmer.pb100mMillis != null && swimmer.pb100mMillis == finishTime
        else -> false
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OceanSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pool,
                        contentDescription = null,
                        tint = AquaPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${s.targetDistanceMeters}m ${s.strokeType}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${TimeFormatter.formatDate(s.timestamp)} • ${s.poolLengthMeters}m Becken",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = TimeFormatter.formatMillis(finishTime),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        ),
                        color = AquaPrimary,
                        maxLines = 1
                    )
                    if (isPB) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF2C2405)
                        ) {
                            Text(
                                text = "PB",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFD600),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expand toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Splits einklappen" else "${swimmerLaps.size} Zwischenzeiten (Splits) ansehen",
                    style = MaterialTheme.typography.labelSmall,
                    color = AquaPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = AquaPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Expanded Splits Details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0C1623))
                        .padding(10.dp)
                ) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lap", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(36.dp), maxLines = 1)
                        Text("Split", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(70.dp), maxLines = 1)
                        Text("Runde", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.width(60.dp), maxLines = 1)
                        Text("Pace/SPM", style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
                    }

                    HorizontalDivider(color = Color(0xFF1E2D40), modifier = Modifier.padding(bottom = 6.dp))

                    val isLagen = s.strokeType.equals("Lagen", ignoreCase = true)
                    val lagenLegLength = (s.targetDistanceMeters / 4).coerceAtLeast(1)

                    swimmerLaps.forEach { lap ->
                        val lapLagenStroke = if (isLagen) {
                            val legIdx = (((lap.lapIndex - 1) * s.splitDistanceMeters) / lagenLegLength).coerceIn(0, 3)
                            val shortNames = listOf("Schmett", "Rücken", "Brust", "Freistil")
                            shortNames[legIdx]
                        } else null

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.width(76.dp)
                            ) {
                                Text(
                                    text = "L${lap.lapIndex}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = AquaPrimary,
                                    maxLines = 1
                                )
                                if (lapLagenStroke != null) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = lapLagenStroke,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                        color = Color(0xFFFFD600),
                                        maxLines = 1
                                    )
                                }
                            }
                            Text(
                                text = TimeFormatter.formatMillis(lap.splitTimeMillis),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = TextWhite,
                                modifier = Modifier.width(68.dp),
                                maxLines = 1
                            )
                            Text(
                                text = TimeFormatter.formatMillis(lap.lapTimeMillis, shortFormat = true),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.LightGray,
                                modifier = Modifier.width(60.dp),
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (lap.deltaTargetMillis != null) {
                                    val isFaster = lap.deltaTargetMillis <= 0
                                    Text(
                                        text = TimeFormatter.formatDelta(lap.deltaTargetMillis),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isFaster) PaceGreen else PaceRed,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                if (lap.strokeRateSpm != null) {
                                    Text(
                                        text = "%.0f SPM".format(lap.strokeRateSpm),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFD600),
                                        maxLines = 1
                                    )
                                } else if (lap.deltaTargetMillis == null) {
                                    // Calculated 100m pace guarantees the rightmost column is never empty
                                    Text(
                                        text = TimeFormatter.formatPacePer100m(lap.lapTimeMillis, s.splitDistanceMeters),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = AquaPrimary,
                                        maxLines = 1
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

@Composable
private fun EditSwimmerDialog(
    swimmer: SwimmerEntity,
    onDismiss: () -> Unit,
    onSave: (SwimmerEntity) -> Unit
) {
    var name by remember { mutableStateOf(swimmer.name) }
    var stroke by remember { mutableStateOf(swimmer.favoriteStroke) }
    var pb25Text by remember {
        mutableStateOf(swimmer.pb25mMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var pb50Text by remember {
        mutableStateOf(swimmer.pb50mMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var pb100Text by remember {
        mutableStateOf(swimmer.pb100mMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var pb100LagenText by remember {
        mutableStateOf(swimmer.pb100mLagenMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var pb200LagenText by remember {
        mutableStateOf(swimmer.pb200mLagenMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var pb400LagenText by remember {
        mutableStateOf(swimmer.pb400mLagenMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    // 200m Lagen Splits (4x 50m)
    var pb200FlyText by remember {
        mutableStateOf(swimmer.pb200mLagenFlySplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var pb200BackText by remember {
        mutableStateOf(swimmer.pb200mLagenBackSplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var pb200BreastText by remember {
        mutableStateOf(swimmer.pb200mLagenBreastSplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var pb200FreeText by remember {
        mutableStateOf(swimmer.pb200mLagenFreeSplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    // 400m Lagen Splits (4x 100m)
    var pb400FlyText by remember {
        mutableStateOf(swimmer.pb400mLagenFlySplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var pb400BackText by remember {
        mutableStateOf(swimmer.pb400mLagenBackSplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var pb400BreastText by remember {
        mutableStateOf(swimmer.pb400mLagenBreastSplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var pb400FreeText by remember {
        mutableStateOf(swimmer.pb400mLagenFreeSplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    // 100m Lagen Splits (4x 25m)
    var flySplitText by remember {
        mutableStateOf(swimmer.lagenFlySplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var backSplitText by remember {
        mutableStateOf(swimmer.lagenBackSplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var breastSplitText by remember {
        mutableStateOf(swimmer.lagenBreastSplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }
    var freeSplitText by remember {
        mutableStateOf(swimmer.lagenFreeSplitMillis?.let { String.format(java.util.Locale.US, "%.2f", it / 1000.0) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schwimmer bearbeiten", color = TextWhite, fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF101B2B),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AquaPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pb25Text,
                        onValueChange = { pb25Text = it },
                        label = { Text("25m Sprint PB (s)") },
                        placeholder = { Text("12.50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = AquaPrimary),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb50Text,
                        onValueChange = { pb50Text = it },
                        label = { Text("50m PB (s)") },
                        placeholder = { Text("24.80") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = AquaPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pb100Text,
                        onValueChange = { pb100Text = it },
                        label = { Text("100m PB (s)") },
                        placeholder = { Text("54.20") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = AquaPrimary),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb100LagenText,
                        onValueChange = { pb100LagenText = it },
                        label = { Text("100m Lagen PB (s)") },
                        placeholder = { Text("63.50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = AquaPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pb200LagenText,
                        onValueChange = { pb200LagenText = it },
                        label = { Text("200m Lagen PB (s)") },
                        placeholder = { Text("138.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = AquaPrimary),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb400LagenText,
                        onValueChange = { pb400LagenText = it },
                        label = { Text("400m Lagen PB (s)") },
                        placeholder = { Text("295.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = AquaPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 200m Lagen Splits (4x 50m)
                Text(
                    text = "200m Lagen Splits (4× 50m):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pb200FlyText,
                        onValueChange = { pb200FlyText = it },
                        label = { Text("1. Schmett (50m)") },
                        placeholder = { Text("29.50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb200BackText,
                        onValueChange = { pb200BackText = it },
                        label = { Text("2. Rücken (50m)") },
                        placeholder = { Text("35.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFFD600)),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pb200BreastText,
                        onValueChange = { pb200BreastText = it },
                        label = { Text("3. Brust (50m)") },
                        placeholder = { Text("39.80") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E676)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb200FreeText,
                        onValueChange = { pb200FreeText = it },
                        label = { Text("4. Kraul (50m)") },
                        placeholder = { Text("28.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF)),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 400m Lagen Splits (4x 100m)
                Text(
                    text = "400m Lagen Splits (4× 100m):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF80D8FF)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pb400FlyText,
                        onValueChange = { pb400FlyText = it },
                        label = { Text("1. Schmett (100m)") },
                        placeholder = { Text("64.50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb400BackText,
                        onValueChange = { pb400BackText = it },
                        label = { Text("2. Rücken (100m)") },
                        placeholder = { Text("74.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFFD600)),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pb400BreastText,
                        onValueChange = { pb400BreastText = it },
                        label = { Text("3. Brust (100m)") },
                        placeholder = { Text("84.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E676)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb400FreeText,
                        onValueChange = { pb400FreeText = it },
                        label = { Text("4. Kraul (100m)") },
                        placeholder = { Text("61.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF)),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 100m Lagen Splits (4x 25m)
                Text(
                    text = "100m Lagen Splits (4× 25m):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFD600)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = flySplitText,
                        onValueChange = { flySplitText = it },
                        label = { Text("1. Schmett (25m)") },
                        placeholder = { Text("13.80") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = AquaPrimary),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = backSplitText,
                        onValueChange = { backSplitText = it },
                        label = { Text("2. Rücken (25m)") },
                        placeholder = { Text("15.50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = AquaPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = breastSplitText,
                        onValueChange = { breastSplitText = it },
                        label = { Text("3. Brust (25m)") },
                        placeholder = { Text("18.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = AquaPrimary),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = freeSplitText,
                        onValueChange = { freeSplitText = it },
                        label = { Text("4. Kraul (25m)") },
                        placeholder = { Text("12.90") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = AquaPrimary),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        fun parseSec(t: String) = t.replace(',', '.').toDoubleOrNull()?.let { (it * 1000).toLong() }
                        val inputPb50 = parseSec(pb50Text)
                        val inputPb100 = parseSec(pb100Text)
                        val inputPb25 = parseSec(pb25Text)

                        onSave(
                            swimmer.copy(
                                name = name,
                                favoriteStroke = stroke,
                                pb25mMillis = inputPb25,
                                pb50mMillis = inputPb50,
                                pb100mMillis = inputPb100,
                                pb100mLagenMillis = parseSec(pb100LagenText),
                                pb200mLagenMillis = parseSec(pb200LagenText),
                                pb400mLagenMillis = parseSec(pb400LagenText),
                                pb200mLagenFlySplitMillis = parseSec(pb200FlyText),
                                pb200mLagenBackSplitMillis = parseSec(pb200BackText),
                                pb200mLagenBreastSplitMillis = parseSec(pb200BreastText),
                                pb200mLagenFreeSplitMillis = parseSec(pb200FreeText),
                                pb400mLagenFlySplitMillis = parseSec(pb400FlyText),
                                pb400mLagenBackSplitMillis = parseSec(pb400BackText),
                                pb400mLagenBreastSplitMillis = parseSec(pb400BreastText),
                                pb400mLagenFreeSplitMillis = parseSec(pb400FreeText),
                                lagenFlySplitMillis = parseSec(flySplitText),
                                lagenBackSplitMillis = parseSec(backSplitText),
                                lagenBreastSplitMillis = parseSec(breastSplitText),
                                lagenFreeSplitMillis = parseSec(freeSplitText)
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
            ) {
                Text("Speichern", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("Abbrechen", color = Color.LightGray)
            }
        }
    )
}
