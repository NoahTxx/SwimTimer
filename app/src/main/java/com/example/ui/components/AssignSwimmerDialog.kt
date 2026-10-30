package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SwimmerEntity
import com.example.domain.PersonalBestEvaluator
import com.example.domain.RelayLegAssignment
import com.example.domain.RelayOptimizer
import com.example.ui.LaneState
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.OceanSurface
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.TextWhite
import com.example.util.TimeFormatter

private fun formatShortStroke(stroke: String): String = when {
    stroke.equals("Schmetterling", ignoreCase = true) -> "Schmett."
    else -> stroke
}

@Composable
fun AssignSwimmerDialog(
    laneNumber: Int,
    swimmers: List<SwimmerEntity>,
    assignedSwimmers: Map<Long, Int> = emptyMap(),
    availableGroups: List<String> = emptyList(),
    currentLane: LaneState? = null,
    currentStrokeType: String = "Freistil",
    currentSplitDistanceMeters: Int = 50,
    initialRelayLegPosition: Int? = null,
    onDismiss: () -> Unit,
    onSelectRelayTeam: ((teamName: String, members: List<RelayLegAssignment>) -> Unit)? = null,
    onSelectSwimmer: (SwimmerEntity?) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var selectedGroupFilter by remember { mutableStateOf<String?>(null) }

    val isInitiallyRelay = initialRelayLegPosition != null || currentLane?.relayMembers?.isNotEmpty() == true
    var isRelayMode by remember { mutableStateOf(isInitiallyRelay && onSelectRelayTeam != null) }

    val effectiveLegDist = remember(currentSplitDistanceMeters) {
        if (currentSplitDistanceMeters in listOf(25, 50, 100)) currentSplitDistanceMeters else 50
    }

    var relayTeamName by remember {
        val defaultName = if (currentLane?.relayMembers?.isNotEmpty() == true) {
            currentLane.swimmerName
        } else {
            "Staffel Bahn $laneNumber"
        }
        mutableStateOf(defaultName)
    }

    var relayLegs by remember {
        val initialLegs = if (currentLane != null && currentLane.relayMembers.isNotEmpty()) {
            currentLane.relayMembers
        } else {
            (0 until 4).map { idx ->
                val legStroke = if (PersonalBestEvaluator.isSameStroke(currentStrokeType, "Lagen")) {
                    PersonalBestEvaluator.LAGEN_STAFFEL_STROKES.getOrElse(idx % 4) { "Freistil" }
                } else {
                    currentStrokeType
                }
                if (idx == 0 && currentLane?.swimmerId != null) {
                    val swimmerObj = swimmers.find { it.id == currentLane.swimmerId }
                    val est = swimmerObj?.let { RelayOptimizer.resolveSwimmerTime(it, legStroke, effectiveLegDist) }
                    RelayLegAssignment(
                        legPosition = 1,
                        swimmerId = currentLane.swimmerId,
                        swimmerName = currentLane.swimmerName,
                        stroke = legStroke,
                        distanceMeters = effectiveLegDist,
                        timeMillis = est?.millis,
                        isEstimated = est?.isEstimated ?: false,
                        sourceLabel = est?.sourceLabel ?: ""
                    )
                } else {
                    RelayLegAssignment(
                        legPosition = idx + 1,
                        swimmerId = null,
                        swimmerName = "Starter ${idx + 1}",
                        stroke = legStroke,
                        distanceMeters = effectiveLegDist,
                        timeMillis = null
                    )
                }
            }
        }
        mutableStateOf(initialLegs)
    }

    var activeLegIdx by remember {
        mutableIntStateOf(((initialRelayLegPosition ?: 1) - 1).coerceIn(0, (relayLegs.size - 1).coerceAtLeast(0)))
    }

    fun resizeRelayLegs(newCount: Int) {
        val clamped = newCount.coerceIn(2, 8)
        if (clamped == relayLegs.size) return
        val updated = (0 until clamped).map { idx ->
            val legStroke = if (PersonalBestEvaluator.isSameStroke(currentStrokeType, "Lagen")) {
                PersonalBestEvaluator.LAGEN_STAFFEL_STROKES.getOrElse(idx % 4) { "Freistil" }
            } else {
                currentStrokeType
            }
            val existing = relayLegs.getOrNull(idx)
            existing?.copy(legPosition = idx + 1, stroke = legStroke) ?: RelayLegAssignment(
                legPosition = idx + 1,
                swimmerId = null,
                swimmerName = "Starter ${idx + 1}",
                stroke = legStroke,
                distanceMeters = effectiveLegDist,
                timeMillis = null
            )
        }
        relayLegs = updated
        if (activeLegIdx >= updated.size) {
            activeLegIdx = updated.lastIndex
        }
    }

    val groupsToDisplay = remember(availableGroups, swimmers) {
        if (availableGroups.isNotEmpty()) {
            availableGroups
        } else {
            swimmers.mapNotNull { it.groupName?.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
        }
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag("assign_swimmer_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Schwimmer für Bahn $laneNumber",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen", tint = Color.LightGray)
                    }
                }

                // Mode Switcher: Einzelschwimmer vs. Staffel (4 Starter)
                if (onSelectRelayTeam != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A141F),
                        border = BorderStroke(1.dp, Color(0xFF1E344B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (!isRelayMode) AquaPrimary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isRelayMode = false }
                                    .testTag("assign_mode_single")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (!isRelayMode) Color.Black else Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Einzelschwimmer",
                                        fontSize = 12.sp,
                                        fontWeight = if (!isRelayMode) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isRelayMode) Color.Black else Color.LightGray,
                                        maxLines = 1
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (isRelayMode) AquaPrimary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isRelayMode = true }
                                    .testTag("assign_mode_relay")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = if (isRelayMode) Color.Black else Color.LightGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Staffel (${relayLegs.size} Starter)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isRelayMode) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isRelayMode) Color.Black else Color.LightGray,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!isRelayMode) {
                    // --- EINZELSCHWIMMER MODUS ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 46.dp)
                            .clickable { onSelectSwimmer(null) }
                            .padding(vertical = 8.dp),
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
                } else {
                    // --- STAFFEL MODUS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = relayTeamName,
                            onValueChange = { relayTeamName = it },
                            label = { Text("Staffel-Name", fontSize = 11.sp, color = Color.LightGray) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AquaPrimary,
                                unfocusedBorderColor = Color(0xFF22384F),
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Starter count stepper (2..8)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0A141F),
                            border = BorderStroke(1.dp, Color(0xFF22384F)),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (relayLegs.size > 2) AquaPrimary.copy(alpha = 0.18f) else Color.Transparent,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable(enabled = relayLegs.size > 2) { resizeRelayLegs(relayLegs.size - 1) }
                                        .testTag("assign_relay_dec_starters")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "−",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = if (relayLegs.size > 2) AquaPrimary else Color.DarkGray
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = "${relayLegs.size}×",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = AquaPrimary
                                    )
                                    Text(
                                        text = "${relayLegs.size * effectiveLegDist}m",
                                        fontSize = 9.sp,
                                        color = Color.LightGray
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = if (relayLegs.size < 8) AquaPrimary.copy(alpha = 0.18f) else Color.Transparent,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable(enabled = relayLegs.size < 8) { resizeRelayLegs(relayLegs.size + 1) }
                                        .testTag("assign_relay_inc_starters")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "+",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = if (relayLegs.size < 8) AquaPrimary else Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tippe auf eine Startposition (1–${relayLegs.size}) und wähle unten den Schwimmer:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Grid of the relay starter positions
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        relayLegs.chunked(2).forEachIndexed { rowIdx, pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pair.forEachIndexed { colIdx, leg ->
                                    val idx = rowIdx * 2 + colIdx
                                    val isSelectedSlot = activeLegIdx == idx
                                    val hasSwimmer = leg.swimmerId != null
                                    val isLagen = PersonalBestEvaluator.isSameStroke(currentStrokeType, "Lagen")
                                    val isAnchorSlot = !isLagen && leg.legPosition == relayLegs.size && relayLegs.size >= 2
                                    val slotHeader = if (isLagen) {
                                        "${leg.legPosition}. ${formatShortStroke(leg.stroke)}"
                                    } else if (isAnchorSlot) {
                                        "${leg.legPosition}. Schluss"
                                    } else {
                                        "${leg.legPosition}. Starter"
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = when {
                                            isSelectedSlot -> AquaPrimary.copy(alpha = 0.18f)
                                            hasSwimmer -> Color(0xFF13273A)
                                            else -> Color(0xFF0A141F)
                                        },
                                        border = BorderStroke(
                                            width = if (isSelectedSlot) 1.5.dp else 1.dp,
                                            color = when {
                                                isSelectedSlot -> AquaPrimary
                                                hasSwimmer -> PaceGreen.copy(alpha = 0.6f)
                                                else -> Color(0xFF1E344B)
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable { activeLegIdx = idx }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                val headerColor = if (isSelectedSlot) AquaPrimary else if (hasSwimmer) PaceGreen else if (isAnchorSlot) Color(0xFFFFD600) else Color.Gray
                                                if (isAnchorSlot) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "${leg.legPosition}. ",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = headerColor,
                                                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                                            maxLines = 1
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = Color(0xFFFFD600),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = "Schluss",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = headerColor,
                                                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                                            maxLines = 1
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = slotHeader,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = headerColor,
                                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                                        maxLines = 1
                                                    )
                                                }
                                                Text(
                                                    text = if (hasSwimmer) leg.swimmerName else "Frei (wählen)",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = if (hasSwimmer) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (hasSwimmer) TextWhite else Color.Gray,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (hasSwimmer) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Starter entfernen",
                                                    tint = Color.LightGray,
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clickable {
                                                            val updated = relayLegs.toMutableList()
                                                            updated[idx] = updated[idx].copy(
                                                                swimmerId = null,
                                                                swimmerName = "Starter ${idx + 1}",
                                                                timeMillis = null
                                                            )
                                                            relayLegs = updated
                                                            activeLegIdx = idx
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E2D40), modifier = Modifier.padding(vertical = 8.dp))
                }

                Text(
                    text = if (isRelayMode) {
                        val activeLeg = relayLegs.getOrNull(activeLegIdx)
                        "Athlet für ${activeLegIdx + 1}. Starter (${activeLeg?.stroke ?: currentStrokeType}) wählen:"
                    } else {
                        "Aus Team auswählen:"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = AquaPrimary,
                    fontWeight = FontWeight.Bold
                )

                if (groupsToDisplay.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item(key = "all_chip") {
                            FilterChip(
                                selected = selectedGroupFilter == null,
                                onClick = { selectedGroupFilter = null },
                                label = { Text("Alle", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AquaPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                        items(groupsToDisplay, key = { it }) { group ->
                            val isSelected = selectedGroupFilter == group
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedGroupFilter = group },
                                label = { Text(group, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AquaPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (filteredSwimmers.isEmpty()) {
                    Text(
                        text = "Noch keine Schwimmer angelegt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(if (isRelayMode) 200.dp else 250.dp)
                    ) {
                        items(
                            items = filteredSwimmers,
                            key = { swimmer -> swimmer.id },
                            contentType = { "SwimmerDialogItem" }
                        ) { swimmer ->
                            val existingRelayPos = relayLegs.indexOfFirst { it.swimmerId == swimmer.id }
                            val currentAssignedLane = assignedSwimmers[swimmer.id]
                            val isAssignedToOtherLane = currentAssignedLane != null && currentAssignedLane != laneNumber
                            val isAssignedToThisLane = currentAssignedLane == laneNumber

                            val targetStrokeForPreview = if (isRelayMode) {
                                relayLegs.getOrNull(activeLegIdx)?.stroke ?: currentStrokeType
                            } else {
                                swimmer.favoriteStroke
                            }
                            val resolvedLegTime = remember(swimmer, targetStrokeForPreview, effectiveLegDist) {
                                RelayOptimizer.resolveSwimmerTime(swimmer, targetStrokeForPreview, effectiveLegDist)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 46.dp)
                                    .clickable {
                                        if (!isRelayMode) {
                                            onSelectSwimmer(swimmer)
                                        } else {
                                            val updated = relayLegs.toMutableList()
                                            // If swimmer was already on another leg in this relay, clear that old leg
                                            val oldIdx = updated.indexOfFirst { it.swimmerId == swimmer.id }
                                            if (oldIdx != -1 && oldIdx != activeLegIdx) {
                                                updated[oldIdx] = updated[oldIdx].copy(
                                                    swimmerId = null,
                                                    swimmerName = "Starter ${oldIdx + 1}",
                                                    timeMillis = null
                                                )
                                            }
                                            val legStroke = updated[activeLegIdx].stroke
                                            val est = RelayOptimizer.resolveSwimmerTime(swimmer, legStroke, effectiveLegDist)
                                            updated[activeLegIdx] = updated[activeLegIdx].copy(
                                                swimmerId = swimmer.id,
                                                swimmerName = swimmer.name,
                                                timeMillis = est?.millis,
                                                isEstimated = est?.isEstimated ?: false,
                                                sourceLabel = est?.sourceLabel ?: ""
                                            )
                                            relayLegs = updated
                                            // Advance to next unassigned slot or next index
                                            val nextEmpty = updated.indexOfFirst { it.swimmerId == null }
                                            activeLegIdx = if (nextEmpty != -1) nextEmpty else (activeLegIdx + 1).coerceAtMost(updated.lastIndex)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(swimmer.colorHex),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = swimmer.name.take(1).uppercase(),
                                                color = Color.Black,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = swimmer.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                            color = TextWhite,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val subtitle = when {
                                            isRelayMode && existingRelayPos != -1 -> "In dieser Staffel: ${existingRelayPos + 1}. Starter"
                                            !isRelayMode && isAssignedToThisLane -> "Auf dieser Bahn eingeteilt"
                                            isAssignedToOtherLane -> "Bereits auf Bahn $currentAssignedLane"
                                            else -> "Hauptlage: ${swimmer.favoriteStroke}"
                                        }
                                        val subtitleColor = when {
                                            isRelayMode && existingRelayPos != -1 -> PaceGreen
                                            !isRelayMode && isAssignedToThisLane -> AquaPrimary
                                            isAssignedToOtherLane -> Color(0xFFFFB74D)
                                            else -> AquaPrimary
                                        }
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = subtitleColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                val pbToDisplay = resolvedLegTime?.millis
                                    ?: swimmer.pb50mMillis
                                    ?: swimmer.parseCustomPbsMap()[swimmer.favoriteStroke to 50]
                                if (pbToDisplay != null) {
                                    val isEst = resolvedLegTime?.isEstimated == true
                                    val estPrefix = if (isEst) "~" else "PB: "
                                    Text(
                                        text = "$estPrefix${TimeFormatter.formatMillis(pbToDisplay, shortFormat = true)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = if (isEst) Color(0xFFFFB74D) else AquaPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                if (isRelayMode && onSelectRelayTeam != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onSelectRelayTeam(relayTeamName, relayLegs)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AquaPrimary,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_relay_lane_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Staffel für Bahn $laneNumber übernehmen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
