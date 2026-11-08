package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    selectedGroupFilter: String? = null,
    onSelectGroupFilter: ((String?) -> Unit)? = null,
    currentLane: LaneState? = null,
    currentStrokeType: String = "Freistil",
    currentSplitDistanceMeters: Int = 50,
    initialRelayLegPosition: Int? = null,
    onDismiss: () -> Unit,
    onSelectRelayTeam: ((teamName: String, members: List<RelayLegAssignment>) -> Unit)? = null,
    onSelectSwimmer: (SwimmerEntity?) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var localGroupFilter by rememberSaveable { mutableStateOf(selectedGroupFilter) }
    val activeGroupFilter = onSelectGroupFilter?.let { selectedGroupFilter } ?: localGroupFilter
    val updateGroupFilter: (String?) -> Unit = { newGroup ->
        if (onSelectGroupFilter != null) {
            onSelectGroupFilter(newGroup)
        } else {
            localGroupFilter = newGroup
        }
    }

    val isInitiallyRelay = initialRelayLegPosition != null || currentLane?.relayMembers?.isNotEmpty() == true
    var isRelayMode by rememberSaveable { mutableStateOf(isInitiallyRelay && onSelectRelayTeam != null) }

    val effectiveLegDist = remember(currentSplitDistanceMeters) {
        if (currentSplitDistanceMeters in listOf(25, 50, 100)) currentSplitDistanceMeters else 50
    }

    var relayTeamName by rememberSaveable {
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
        if (isRelayMode && onSelectRelayTeam != null) {
            onSelectRelayTeam(relayTeamName, updated)
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

    val filteredSwimmers = remember(swimmers, activeGroupFilter) {
        if (activeGroupFilter == null) {
            swimmers
        } else {
            swimmers.filter { it.groupName.equals(activeGroupFilter, ignoreCase = true) }
        }
    }

    val dismissAndCommit: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (isRelayMode && onSelectRelayTeam != null) {
            onSelectRelayTeam(relayTeamName, relayLegs)
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = dismissAndCommit,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF101B2B),
            border = BorderStroke(1.dp, Color(0xFF1E344B)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp)
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                }
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
                        onClick = dismissAndCommit,
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
                                    .clickable(
                                        indication = null,
                                        interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
                                    ) { isRelayMode = false }
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
                                    .clickable(
                                        indication = null,
                                        interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
                                    ) { isRelayMode = true }
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
                            .clickable(
                                indication = null,
                                interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
                            ) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                onSelectSwimmer(null)
                            }
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
                        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                        OutlinedTextField(
                            value = relayTeamName,
                            onValueChange = {
                                relayTeamName = it
                                if (isRelayMode && onSelectRelayTeam != null) {
                                    onSelectRelayTeam(it, relayLegs)
                                }
                            },
                            label = { Text("Staffel-Name", fontSize = 11.sp, color = Color.LightGray) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AquaPrimary,
                                unfocusedBorderColor = Color(0xFF22384F),
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        // Starter count stepper (2..8) matching OutlinedTextField height
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0C1623),
                            border = BorderStroke(1.dp, Color(0xFF22384F)),
                            modifier = Modifier.height(56.dp)
                        ) {
                            val canDec = relayLegs.size > 2
                            val canInc = relayLegs.size < 8

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (canDec) AquaPrimary.copy(alpha = 0.18f) else Color(0xFF131F2D),
                                    border = BorderStroke(1.dp, if (canDec) AquaPrimary.copy(alpha = 0.4f) else Color(0xFF1D2F42)),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            enabled = canDec,
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) { resizeRelayLegs(relayLegs.size - 1) }
                                        .testTag("assign_relay_dec_starters")
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Starter reduzieren",
                                            tint = if (canDec) AquaPrimary else Color.DarkGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "${relayLegs.size} Starter",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${relayLegs.size * effectiveLegDist}m gesamt",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AquaPrimary,
                                        maxLines = 1
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (canInc) AquaPrimary.copy(alpha = 0.18f) else Color(0xFF131F2D),
                                    border = BorderStroke(1.dp, if (canInc) AquaPrimary.copy(alpha = 0.4f) else Color(0xFF1D2F42)),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable(
                                            enabled = canInc,
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) { resizeRelayLegs(relayLegs.size + 1) }
                                        .testTag("assign_relay_inc_starters")
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Starter erhöhen",
                                            tint = if (canInc) AquaPrimary else Color.DarkGray,
                                            modifier = Modifier.size(18.dp)
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
                                    val shortStroke = formatShortStroke(leg.stroke)

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = when {
                                            isSelectedSlot -> AquaPrimary.copy(alpha = 0.16f)
                                            hasSwimmer -> Color(0xFF102332)
                                            else -> Color(0xFF0C1724)
                                        },
                                        border = BorderStroke(
                                            width = if (isSelectedSlot) 1.5.dp else 1.dp,
                                            color = when {
                                                isSelectedSlot -> AquaPrimary
                                                hasSwimmer -> PaceGreen.copy(alpha = 0.6f)
                                                else -> Color(0xFF1B2F42)
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clickable(
                                                indication = null,
                                                interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
                                            ) { activeLegIdx = idx }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 9.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Top Row: Position + Name Pill in top-left, stroke/badge & delete-X in top-right
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                val displayName = if (hasSwimmer) leg.swimmerName else if (isAnchorSlot) "Schluss (${leg.stroke})" else "Starter"
                                                Surface(
                                                    shape = RoundedCornerShape(5.dp),
                                                    color = when {
                                                        isSelectedSlot -> AquaPrimary.copy(alpha = 0.25f)
                                                        hasSwimmer -> PaceGreen.copy(alpha = 0.22f)
                                                        isAnchorSlot -> Color(0xFFFFD600).copy(alpha = 0.22f)
                                                        else -> Color(0xFF16283C)
                                                    },
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "${leg.legPosition}. $displayName",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when {
                                                                isSelectedSlot -> AquaPrimary
                                                                hasSwimmer -> PaceGreen
                                                                isAnchorSlot -> Color(0xFFFFD600)
                                                                else -> Color(0xFF90A4AE)
                                                            },
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                                        )
                                                        if (isAnchorSlot) {
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.Star,
                                                                contentDescription = null,
                                                                tint = Color(0xFFFFD600),
                                                                modifier = Modifier.size(9.5.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(4.dp))

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${leg.distanceMeters}m $shortStroke",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF90A4AE),
                                                        maxLines = 1
                                                    )

                                                    if (hasSwimmer) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = Color(0xFF1F354D),
                                                            modifier = Modifier
                                                                .size(18.dp)
                                                                .clickable(
                                                                    indication = null,
                                                                    interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
                                                                ) {
                                                                    val updated = relayLegs.toMutableList()
                                                                    updated[idx] = updated[idx].copy(
                                                                        swimmerId = null,
                                                                        swimmerName = "Starter ${idx + 1}",
                                                                        timeMillis = null,
                                                                        isEstimated = false,
                                                                        sourceLabel = ""
                                                                    )
                                                                    relayLegs = updated
                                                                    activeLegIdx = idx
                                                                    if (isRelayMode && onSelectRelayTeam != null) {
                                                                        onSelectRelayTeam(relayTeamName, updated)
                                                                    }
                                                                }
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Starter entfernen",
                                                                    tint = Color.LightGray,
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Center: Clean time / status presentation
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (hasSwimmer) {
                                                    if (leg.timeMillis != null) {
                                                        val estPrefix = if (leg.isEstimated) "~" else "PB "
                                                        Text(
                                                            text = "$estPrefix${TimeFormatter.formatMillis(leg.timeMillis, shortFormat = true)}",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (leg.isEstimated) Color(0xFFFFB74D) else PaceGreen,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    } else {
                                                        Text(
                                                            text = leg.swimmerName,
                                                            fontSize = 14.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextWhite,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = if (isSelectedSlot) "← Athlet wählen" else "Frei",
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelectedSlot) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (isSelectedSlot) AquaPrimary else Color(0xFF546E7A),
                                                        maxLines = 1
                                                    )
                                                }
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
                                selected = activeGroupFilter == null,
                                onClick = { updateGroupFilter(null) },
                                label = { Text("Alle", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AquaPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                        items(groupsToDisplay, key = { it }) { group ->
                            val isSelected = activeGroupFilter == group
                            FilterChip(
                                selected = isSelected,
                                onClick = { updateGroupFilter(if (isSelected) null else group) },
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
                    val assignListState = androidx.compose.foundation.lazy.rememberLazyListState()
                    androidx.compose.runtime.LaunchedEffect(assignListState.isScrollInProgress) {
                        if (assignListState.isScrollInProgress) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    }

                    LazyColumn(
                        state = assignListState,
                        modifier = Modifier.height(if (isRelayMode) 250.dp else 320.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
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
                            val isAssignedToCurrentActiveLeg = isRelayMode && relayLegs.getOrNull(activeLegIdx)?.swimmerId == swimmer.id
                            val isAssignedToOtherLegInThisRelay = isRelayMode && existingRelayPos != -1 && existingRelayPos != activeLegIdx

                            val previewDistance = if (isRelayMode) effectiveLegDist else (currentLane?.targetDistanceMeters ?: effectiveLegDist)
                            val targetStrokeForPreview = if (isRelayMode) {
                                relayLegs.getOrNull(activeLegIdx)?.stroke ?: currentStrokeType
                            } else {
                                currentStrokeType
                            }
                            val resolvedLegTime = remember(swimmer, targetStrokeForPreview, previewDistance) {
                                RelayOptimizer.resolveSwimmerTime(swimmer, targetStrokeForPreview, previewDistance)
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isAssignedToCurrentActiveLeg -> AquaPrimary.copy(alpha = 0.16f)
                                    isAssignedToOtherLegInThisRelay -> Color(0xFF0F2537)
                                    isAssignedToThisLane -> AquaPrimary.copy(alpha = 0.12f)
                                    else -> Color(0xFF0E1A29)
                                },
                                border = BorderStroke(
                                    width = if (isAssignedToCurrentActiveLeg) 1.5.dp else 1.dp,
                                    color = when {
                                        isAssignedToCurrentActiveLeg -> AquaPrimary
                                        isAssignedToOtherLegInThisRelay -> PaceGreen.copy(alpha = 0.5f)
                                        isAssignedToThisLane -> AquaPrimary.copy(alpha = 0.5f)
                                        else -> Color(0xFF1B2D40)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        indication = null,
                                        interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
                                    ) {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
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
                                                    timeMillis = null,
                                                    isEstimated = false,
                                                    sourceLabel = ""
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
                                            if (onSelectRelayTeam != null) {
                                                onSelectRelayTeam(relayTeamName, updated)
                                            }
                                            // Advance to next unassigned slot or next index
                                            val nextEmpty = updated.indexOfFirst { it.swimmerId == null }
                                            activeLegIdx = if (nextEmpty != -1) nextEmpty else (activeLegIdx + 1).coerceAtMost(updated.lastIndex)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
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
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = swimmer.name.take(1).uppercase(),
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(9.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = swimmer.name,
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                                                    color = TextWhite,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isAssignedToCurrentActiveLeg) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = AquaPrimary
                                                    ) {
                                                        Text(
                                                            text = "HIER",
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.Black,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            val subtitle = when {
                                                isRelayMode && isAssignedToCurrentActiveLeg ->
                                                    "Ausgewählt für ${activeLegIdx + 1}. Starter"
                                                isRelayMode && existingRelayPos != -1 ->
                                                    "In dieser Staffel: ${existingRelayPos + 1}. Starter"
                                                !isRelayMode && isAssignedToThisLane ->
                                                    "Auf dieser Bahn eingeteilt"
                                                isAssignedToOtherLane ->
                                                    "Bereits auf Bahn $currentAssignedLane"
                                                swimmer.groupName != null ->
                                                    "${swimmer.groupName} • Hauptlage: ${swimmer.favoriteStroke}"
                                                else ->
                                                    "Hauptlage: ${swimmer.favoriteStroke}"
                                            }
                                            val subtitleColor = when {
                                                isRelayMode && isAssignedToCurrentActiveLeg -> AquaPrimary
                                                isRelayMode && existingRelayPos != -1 -> PaceGreen
                                                !isRelayMode && isAssignedToThisLane -> AquaPrimary
                                                isAssignedToOtherLane -> Color(0xFFFFB74D)
                                                else -> Color(0xFF90A4AE)
                                            }
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                color = subtitleColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Time Badge on the right: only display PB or estimated time for the actual configured stroke & distance
                                    val pbToDisplay = resolvedLegTime?.millis
                                    if (pbToDisplay != null) {
                                        val isEst = resolvedLegTime.isEstimated
                                        Surface(
                                            shape = RoundedCornerShape(7.dp),
                                            color = if (isEst) Color(0xFFFFB74D).copy(alpha = 0.15f) else AquaPrimary.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, if (isEst) Color(0xFFFFB74D).copy(alpha = 0.45f) else AquaPrimary.copy(alpha = 0.45f))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                            ) {
                                                val strokeShort = formatShortStroke(targetStrokeForPreview)
                                                Text(
                                                    text = "${previewDistance}m $strokeShort",
                                                    fontSize = 10.sp,
                                                    color = Color.LightGray,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                val estPrefix = if (isEst) "~" else ""
                                                Text(
                                                    text = "$estPrefix${TimeFormatter.formatMillis(pbToDisplay, shortFormat = previewDistance <= 50)}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isEst) Color(0xFFFFB74D) else AquaPrimary
                                                )
                                            }
                                        }
                                    }
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
                            keyboardController?.hide()
                            onSelectRelayTeam(relayTeamName, relayLegs)
                            onDismiss()
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
