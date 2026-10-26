package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SwimmerEntity
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.TextWhite
import com.example.util.TimeFormatter

@Composable
fun AssignSwimmerDialog(
    laneNumber: Int,
    swimmers: List<SwimmerEntity>,
    onDismiss: () -> Unit,
    onSelectSwimmer: (SwimmerEntity?) -> Unit,
) {
    var selectedGroupFilter by remember { mutableStateOf<String?>(null) }
    
    val availableGroups = remember(swimmers) {
        swimmers.mapNotNull { it.groupName?.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }
    
    val filteredSwimmers = remember(swimmers, selectedGroupFilter) {
        if (selectedGroupFilter == null) {
            swimmers
        } else {
            swimmers.filter { it.groupName.equals(selectedGroupFilter, ignoreCase = true) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF101B2B),
            modifier = Modifier.fillMaxWidth().testTag("assign_swimmer_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Schwimmer für Bahn $laneNumber",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Default "Bahn X" option to clear
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSwimmer(null) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF263238),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(laneNumber.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Standard: Bahn $laneNumber",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.LightGray
                    )
                }

                HorizontalDivider(color = Color(0xFF1E2D40), modifier = Modifier.padding(vertical = 6.dp))

                Text(
                    text = "Aus Team auswählen:",
                    style = MaterialTheme.typography.labelMedium,
                    color = AquaPrimary
                )
                
                if (availableGroups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            androidx.compose.material3.FilterChip(
                                selected = selectedGroupFilter == null,
                                onClick = { selectedGroupFilter = null },
                                label = { Text("Alle", fontSize = 11.sp) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AquaPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                        items(availableGroups) { group ->
                            val isSelected = selectedGroupFilter == group
                            androidx.compose.material3.FilterChip(
                                selected = isSelected,
                                onClick = { selectedGroupFilter = group },
                                label = { Text(group, fontSize = 11.sp) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AquaPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredSwimmers.isEmpty()) {
                    Text(
                        text = "Noch keine Schwimmer angelegt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(filteredSwimmers, key = { swimmer -> swimmer.id }) { swimmer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSwimmer(swimmer) }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(swimmer.colorHex),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = swimmer.name.take(1).uppercase(),
                                                color = Color.Black,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = swimmer.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextWhite,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Hauptlage: ${swimmer.favoriteStroke}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AquaPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }

                                swimmer.pb50mMillis?.let { pb50m ->
                                    Text(
                                        text = "PB: ${TimeFormatter.formatMillis(pb50m, shortFormat = true)}",
                                        style = MaterialTheme.typography.labelSmall,
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
