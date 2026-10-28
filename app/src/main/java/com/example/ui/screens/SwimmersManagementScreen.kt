package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.StopwatchViewModel
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.DeepOceanBackground
import com.example.ui.theme.OceanSurface
import com.example.util.ExportHelper
import com.example.ui.theme.PaceGreen
import com.example.ui.theme.TextWhite
import com.example.util.TimeFormatter

import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SwimmersManagementScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier,
    isActiveTab: Boolean = true,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allSwimmers by viewModel.allSwimmers.collectAsStateWithLifecycle()
    val swimmers by viewModel.swimmerUiList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.swimmersSearchQuery.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    var selectedSwimmerId by remember { mutableStateOf<Long?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(value = false) }
    var isAddGroupDialogOpen by remember { mutableStateOf(value = false) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val availableGroups by viewModel.availableGroups.collectAsStateWithLifecycle()
    val groupCounts = remember(allSwimmers) {
        allSwimmers.mapNotNull { it.groupName?.trim() }.filter { it.isNotBlank() }.groupingBy { it.lowercase() }.eachCount()
    }
    val selectedGroup by viewModel.selectedGroupFilter.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { selectedUri ->
            viewModel.importSwimmersFromUri(context.contentResolver, selectedUri)
        }
    }

    val activeSwimmer = allSwimmers.firstOrNull { swimmer -> swimmer.id == selectedSwimmerId }

    // If an athlete is selected, show their personal history / detail screen
    if (activeSwimmer != null) {
        SwimmerDetailScreen(
            swimmer = activeSwimmer,
            allSessions = allSessions,
            isActiveTab = isActiveTab,
            availableGroups = availableGroups,
            onBack = { selectedSwimmerId = null },
            onUpdateSwimmer = { updated -> viewModel.updateSwimmer(updated) },
            onDeleteSwimmer = { toDelete ->
                viewModel.deleteSwimmer(toDelete)
                selectedSwimmerId = null
            },
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOceanBackground)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row with Title, Import/Export Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Schwimmteam & Kader",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Export Button
                    FilledTonalIconButton(
                        onClick = {
                            if (allSwimmers.isNotEmpty()) {
                                coroutineScope.launch {
                                    ExportHelper.shareSwimmersExport(context, allSwimmers)
                                }
                            }
                        },
                        enabled = allSwimmers.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = OceanSurface,
                            contentColor = AquaPrimary
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Athleten exportieren",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Import Button
                    FilledTonalIconButton(
                        onClick = {
                            importLauncher.launch("*/*")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = OceanSurface,
                            contentColor = AquaPrimary
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Athleten importieren",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.swimmersSearchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                placeholder = { Text("Schwimmer suchen...") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AquaPrimary,
                    unfocusedBorderColor = OceanSurface,
                    focusedContainerColor = DeepOceanBackground,
                    unfocusedContainerColor = DeepOceanBackground
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Suchen",
                        tint = Color.LightGray
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.swimmersSearchQuery.value = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Suche löschen",
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            )

            // Group Filter Bar
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                item {
                    androidx.compose.material3.FilterChip(
                        selected = selectedGroup == null,
                        onClick = { viewModel.selectedGroupFilter.value = null },
                        label = { Text("Alle (${allSwimmers.size})", fontSize = 12.sp, fontWeight = if (selectedGroup == null) FontWeight.Bold else FontWeight.Normal) },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AquaPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = OceanSurface,
                            labelColor = Color.LightGray
                        )
                    )
                }
                items(
                    items = availableGroups,
                    key = { it }
                ) { group ->
                    val isSelected = selectedGroup == group
                    val count = groupCounts[group.lowercase()] ?: 0
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectedGroupFilter.value = if (isSelected) null else group
                        },
                        label = { Text("$group ($count)", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AquaPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = OceanSurface,
                            labelColor = Color.LightGray
                        )
                    )
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF132738),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { isAddGroupDialogOpen = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = AquaPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gruppe", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AquaPrimary)
                        }
                    }
                }
            }

            if (swimmers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Noch keine Schwimmer angelegt",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = swimmers,
                        key = { it.id },
                        contentType = { "SwimmerCard" }
                    ) { swimmer ->
                        val swimmerId = swimmer.id
                        val onClick = remember(swimmerId) { { selectedSwimmerId = swimmerId } }
                        SwimmerCard(
                            swimmer = swimmer,
                            onClick = onClick
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { isAddDialogOpen = true },
            containerColor = AquaPrimary,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_swimmer_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Schwimmer hinzufügen")
        }

        if (isAddGroupDialogOpen) {
            ManageGroupsDialog(
                existingGroups = availableGroups,
                onDismiss = { isAddGroupDialogOpen = false },
                onCreateGroup = { newGroup ->
                    viewModel.createGroup(newGroup)
                    viewModel.selectedGroupFilter.value = newGroup
                    isAddGroupDialogOpen = false
                },
                onRenameGroup = { oldName, newName ->
                    viewModel.renameGroup(oldName, newName)
                },
                onDeleteGroup = { group ->
                    viewModel.deleteGroup(group)
                }
            )
        }

        if (isAddDialogOpen) {
            AddSwimmerDialog(
                availableGroups = availableGroups,
                defaultGroup = selectedGroup,
                onDismiss = { isAddDialogOpen = false },
                onAdd = { name, stroke, groupName, pb25, pb50, pb100, pb100Lagen, pb200Lagen, pb400Lagen, fly, back, breast, free, pb200Fly, pb200Back, pb200Breast, pb200Free, pb400Fly, pb400Back, pb400Breast, pb400Free, customPbsMap ->
                    viewModel.addSwimmer(
                        name = name,
                        stroke = stroke,
                        groupName = groupName,
                        pb25m = pb25,
                        pb50m = pb50,
                        pb100m = pb100,
                        pb100mLagen = pb100Lagen,
                        pb200mLagen = pb200Lagen,
                        pb400mLagen = pb400Lagen,
                        flySplit = fly,
                        backSplit = back,
                        breastSplit = breast,
                        freeSplit = free,
                        pb200Fly = pb200Fly,
                        pb200Back = pb200Back,
                        pb200Breast = pb200Breast,
                        pb200Free = pb200Free,
                        pb400Fly = pb400Fly,
                        pb400Back = pb400Back,
                        pb400Breast = pb400Breast,
                        pb400Free = pb400Free,
                        customPbsMap = customPbsMap
                    )
                    isAddDialogOpen = false
                }
            )
        }
    }
}

@Composable
private fun SwimmerCard(
    swimmer: com.example.ui.SwimmerCardUiModel,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OceanSurface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("swimmer_card_${swimmer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(swimmer.originalEntity.colorHex),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = swimmer.initial,
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = swimmer.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!swimmer.groupName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AquaPrimary.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AquaPrimary.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = swimmer.groupName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = AquaPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Text(
                        text = "Hauptlage: ${swimmer.groupOrClub}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AquaPrimary,
                        maxLines = 1
                    )
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (swimmer.pb25mText.isNotEmpty()) {
                            Text(
                                text = "25m: ${swimmer.pb25mText}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = AquaPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        if (swimmer.pb50mText.isNotEmpty()) {
                            Text(
                                text = "50m: ${swimmer.pb50mText}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.LightGray,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        if (swimmer.pb100mText.isNotEmpty()) {
                            Text(
                                text = "100m: ${swimmer.pb100mText}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFFFFD600),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        if (swimmer.pb100mImText.isNotEmpty()) {
                            Text(
                                text = "Lagen: ${swimmer.pb100mImText}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFFFF8A80),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "50m PB",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color.Gray,
                    maxLines = 1
                )
                Text(
                    text = swimmer.pb50mFormatted,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = AquaPrimary,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Profil",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Details ansehen",
                        tint = AquaPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ManageGroupsDialog(
    existingGroups: List<String>,
    onDismiss: () -> Unit,
    onCreateGroup: (String) -> Unit,
    onRenameGroup: (oldName: String, newName: String) -> Unit,
    onDeleteGroup: (groupName: String) -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var newGroupName by remember { mutableStateOf("") }
    var groupToRename by remember { mutableStateOf<String?>(null) }
    var groupToDelete by remember { mutableStateOf<String?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    if (groupToDelete != null) {
        val targetGroup = groupToDelete!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Gruppe '$targetGroup' löschen?", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Möchtest du die Gruppe '$targetGroup' wirklich löschen? Der Gruppenname aller betroffenen Athleten wird sofort auf NULL zurückgesetzt.",
                    color = Color.LightGray
                )
            },
            containerColor = Color(0xFF101B2B),
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(targetGroup)
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Löschen", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { groupToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Abbrechen", color = Color.LightGray)
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF101B2B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Trainingsgruppen verwalten",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                    color = TextWhite
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Neue Gruppe erstellen") },
                    placeholder = { Text("z. B. Dolphins 1") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        if (newGroupName.isNotBlank()) {
                            onCreateGroup(newGroupName.trim())
                        }
                    }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AquaPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingGroups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Vorhandene Gruppen bearbeiten / löschen:", style = MaterialTheme.typography.labelSmall, color = AquaPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        existingGroups.forEach { group ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = OceanSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1B2E46)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (groupToRename == group) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = renameInputText,
                                            onValueChange = { renameInputText = it },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextWhite,
                                                unfocusedTextColor = TextWhite,
                                                focusedBorderColor = AquaPrimary
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(onClick = {
                                            if (renameInputText.isNotBlank() && renameInputText.trim() != group) {
                                                onRenameGroup(group, renameInputText.trim())
                                            }
                                            groupToRename = null
                                        }) {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = "Speichern", tint = PaceGreen)
                                        }
                                        IconButton(onClick = { groupToRename = null }) {
                                            Icon(imageVector = Icons.Default.Close, contentDescription = "Abbrechen", tint = Color.LightGray)
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = group,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = TextWhite,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    groupToRename = group
                                                    renameInputText = group
                                                },
                                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Umbenennen", tint = AquaPrimary, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(
                                                onClick = { groupToDelete = group },
                                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Gruppe löschen", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), modifier = Modifier.sizeIn(minHeight = 48.dp)) {
                        Text("Schließen", color = Color.LightGray)
                    }
                    if (newGroupName.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onCreateGroup(newGroupName.trim()) },
                            colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary),
                            modifier = Modifier.sizeIn(minHeight = 48.dp)
                        ) {
                            Text("Erstellen", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSwimmerDialog(
    availableGroups: List<String> = emptyList(),
    defaultGroup: String? = null,
    onDismiss: () -> Unit,
    onAdd: (
        name: String,
        stroke: String,
        groupName: String?,
        pb25m: Long?,
        pb50m: Long?,
        pb100m: Long?,
        pb100mLagen: Long?,
        pb200mLagen: Long?,
        pb400mLagen: Long?,
        fly: Long?,
        back: Long?,
        breast: Long?,
        free: Long?,
        pb200Fly: Long?,
        pb200Back: Long?,
        pb200Breast: Long?,
        pb200Free: Long?,
        pb400Fly: Long?,
        pb400Back: Long?,
        pb400Breast: Long?,
        pb400Free: Long?,
        customPbsMap: Map<Pair<String, Int>, Long>
    ) -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var name by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf(defaultGroup ?: "") }
    var stroke by remember { mutableStateOf("Freistil") }
    var isStrokeDropdownExpanded by remember { mutableStateOf(false) }

    // Multi-Stroke PB State Variables
    var free25Text by remember { mutableStateOf("") }
    var free50Text by remember { mutableStateOf("") }
    var free100Text by remember { mutableStateOf("") }
    var free200Text by remember { mutableStateOf("") }

    var breast25Text by remember { mutableStateOf("") }
    var breast50Text by remember { mutableStateOf("") }
    var breast100Text by remember { mutableStateOf("") }
    var breast200Text by remember { mutableStateOf("") }

    var back25Text by remember { mutableStateOf("") }
    var back50Text by remember { mutableStateOf("") }
    var back100Text by remember { mutableStateOf("") }
    var back200Text by remember { mutableStateOf("") }

    var fly25Text by remember { mutableStateOf("") }
    var fly50Text by remember { mutableStateOf("") }
    var fly100Text by remember { mutableStateOf("") }
    var fly200Text by remember { mutableStateOf("") }

    var free400Text by remember { mutableStateOf("") }
    var free800Text by remember { mutableStateOf("") }
    var free1500Text by remember { mutableStateOf("") }

    var pb100LagenText by remember { mutableStateOf("") }
    var pb200LagenText by remember { mutableStateOf("") }
    var pb400LagenText by remember { mutableStateOf("") }

    // Lagen Splits
    var flySplitText by remember { mutableStateOf("") }
    var backSplitText by remember { mutableStateOf("") }
    var breastSplitText by remember { mutableStateOf("") }
    var freeSplitText by remember { mutableStateOf("") }

    var pb200FlyText by remember { mutableStateOf("") }
    var pb200BackText by remember { mutableStateOf("") }
    var pb200BreastText by remember { mutableStateOf("") }
    var pb200FreeText by remember { mutableStateOf("") }

    var pb400FlyText by remember { mutableStateOf("") }
    var pb400BackText by remember { mutableStateOf("") }
    var pb400BreastText by remember { mutableStateOf("") }
    var pb400FreeText by remember { mutableStateOf("") }

    // Expandable Klappmenü states
    var expandedStrokeSection by remember { mutableStateOf<String?>("Freistil") }
    var isLagenSplitsExpanded by remember { mutableStateOf(false) }

    val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF101B2B),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_swimmer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Neuen Schwimmer anlegen",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = TextWhite,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- ABSCHNITT 1: STAMMDATEN ---
                Text(
                    text = "STAMMDATEN",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = AquaPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AquaPrimary,
                        unfocusedBorderColor = Color(0xFF1B2E46)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_swimmer_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                var isGroupDropdownExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = isGroupDropdownExpanded,
                    onExpandedChange = { isGroupDropdownExpanded = !isGroupDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = if (groupName.isEmpty()) "Keine Gruppe" else groupName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Trainingsgruppe") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGroupDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color(0xFF1B2E46)
                        ),
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = isGroupDropdownExpanded,
                        onDismissRequest = { isGroupDropdownExpanded = false },
                        containerColor = Color(0xFF101B2B)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Keine Gruppe", color = TextWhite) },
                            onClick = {
                                groupName = ""
                                isGroupDropdownExpanded = false
                            },
                            modifier = Modifier.sizeIn(minHeight = 48.dp)
                        )
                        availableGroups.forEach { grp ->
                            DropdownMenuItem(
                                text = { Text(grp, color = TextWhite) },
                                onClick = {
                                    groupName = grp
                                    isGroupDropdownExpanded = false
                                },
                                modifier = Modifier.sizeIn(minHeight = 48.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hauptschwimmart Dropdown (Lagen is omitted)
                ExposedDropdownMenuBox(
                    expanded = isStrokeDropdownExpanded,
                    onExpandedChange = { isStrokeDropdownExpanded = !isStrokeDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = stroke,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hauptschwimmart") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStrokeDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color(0xFF1B2E46),
                            focusedLabelColor = AquaPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .testTag("stroke_dropdown_field")
                    )

                    ExposedDropdownMenu(
                        expanded = isStrokeDropdownExpanded,
                        onDismissRequest = { isStrokeDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF142436))
                    ) {
                        strokeOptions.forEach { strokeOption ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = strokeOption,
                                        color = if (stroke == strokeOption) AquaPrimary else TextWhite,
                                        fontWeight = if (stroke == strokeOption) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                },
                                onClick = {
                                    stroke = strokeOption
                                    isStrokeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // --- ABSCHNITT 2: BESTZEITEN PRO LAGE (KLAPPMENÜ) ---
                Text(
                    text = "BESTZEITEN JE SCHWIMMART (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = AquaPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val strokeCategories = listOf(
                    Triple("Freistil", "Freistil / Kraul", Color(0xFF00E5FF)),
                    Triple("Brust", "Brust", Color(0xFF00E676)),
                    Triple("Rücken", "Rücken", Color(0xFFFFD600)),
                    Triple("Schmetterling", "Schmetterling / Delfin", Color(0xFFFF5252)),
                    Triple("Lagen", "Lagen (100m, 200m, 400m)", Color(0xFFAB47BC))
                )

                strokeCategories.forEach { (catKey, catLabel, catColor) ->
                    val isExpanded = expandedStrokeSection == catKey
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF132334),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isExpanded) catColor else Color(0xFF1B2E46)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                expandedStrokeSection = if (isExpanded) null else catKey
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(catColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = catLabel,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextWhite
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))

                                if (catKey == "Lagen") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = pb100LagenText,
                                            onValueChange = { pb100LagenText = it },
                                            label = { Text("100m", fontSize = 11.sp, maxLines = 1) },
                                            placeholder = { Text("1:03.50") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = pb200LagenText,
                                            onValueChange = { pb200LagenText = it },
                                            label = { Text("200m", fontSize = 11.sp, maxLines = 1) },
                                            placeholder = { Text("2:18.00") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = pb400LagenText,
                                            onValueChange = { pb400LagenText = it },
                                            label = { Text("400m", fontSize = 11.sp, maxLines = 1) },
                                            placeholder = { Text("4:55.00") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    val (v25, set25, v50, set50, v100, set100, v200, set200) = when (catKey) {
                                        "Brust" -> Tuple8(breast25Text, { s: String -> breast25Text = s }, breast50Text, { s: String -> breast50Text = s }, breast100Text, { s: String -> breast100Text = s }, breast200Text, { s: String -> breast200Text = s })
                                        "Rücken" -> Tuple8(back25Text, { s: String -> back25Text = s }, back50Text, { s: String -> back50Text = s }, back100Text, { s: String -> back100Text = s }, back200Text, { s: String -> back200Text = s })
                                        "Schmetterling" -> Tuple8(fly25Text, { s: String -> fly25Text = s }, fly50Text, { s: String -> fly50Text = s }, fly100Text, { s: String -> fly100Text = s }, fly200Text, { s: String -> fly200Text = s })
                                        else -> Tuple8(free25Text, { s: String -> free25Text = s }, free50Text, { s: String -> free50Text = s }, free100Text, { s: String -> free100Text = s }, free200Text, { s: String -> free200Text = s })
                                    }

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = v25,
                                                onValueChange = set25,
                                                label = { Text("25m", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                placeholder = { Text("12.50") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = v50,
                                                onValueChange = set50,
                                                label = { Text("50m", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                placeholder = { Text("24.80") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = v100,
                                                onValueChange = set100,
                                                label = { Text("100m", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                placeholder = { Text("54.20") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = v200,
                                                onValueChange = set200,
                                                label = { Text("200m", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                placeholder = { Text("2:02.00") },
                                                singleLine = true,
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = catColor, unfocusedBorderColor = Color(0xFF1B2E46)),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- LAGEN TEILSTRECKEN (AUFKLAPPBAR) ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF142436),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLagenSplitsExpanded = !isLagenSplitsExpanded }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isLagenSplitsExpanded) "Lagen-Teilstrecken verbergen" else "Lagen-Teilstrecken eintragen (optional)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AquaPrimary
                        )
                        Icon(
                            imageVector = if (isLagenSplitsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = AquaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isLagenSplitsExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "100m Lagen Teilstrecken (4 × 25m):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFFD600))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = flySplitText, onValueChange = { flySplitText = it }, label = { Text("1. Schmett") }, placeholder = { Text("13.80") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFF5252), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = backSplitText, onValueChange = { backSplitText = it }, label = { Text("2. Rücken") }, placeholder = { Text("15.50") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFFD600), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = breastSplitText, onValueChange = { breastSplitText = it }, label = { Text("3. Brust") }, placeholder = { Text("18.00") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E676), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = freeSplitText, onValueChange = { freeSplitText = it }, label = { Text("4. Kraul") }, placeholder = { Text("12.90") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "200m Lagen Teilstrecken (4 × 50m):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AquaPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = pb200FlyText, onValueChange = { pb200FlyText = it }, label = { Text("1. Schmett") }, placeholder = { Text("29.50") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFF5252), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pb200BackText, onValueChange = { pb200BackText = it }, label = { Text("2. Rücken") }, placeholder = { Text("35.00") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFFD600), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = pb200BreastText, onValueChange = { pb200BreastText = it }, label = { Text("3. Brust") }, placeholder = { Text("39.80") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E676), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pb200FreeText, onValueChange = { pb200FreeText = it }, label = { Text("4. Kraul") }, placeholder = { Text("28.00") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "400m Lagen Teilstrecken (4 × 100m):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF80D8FF))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = pb400FlyText, onValueChange = { pb400FlyText = it }, label = { Text("1. Schmett") }, placeholder = { Text("1:04.50") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFF5252), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pb400BackText, onValueChange = { pb400BackText = it }, label = { Text("2. Rücken") }, placeholder = { Text("1:14.00") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFFD600), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(value = pb400BreastText, onValueChange = { pb400BreastText = it }, label = { Text("3. Brust") }, placeholder = { Text("1:24.00") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E676), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pb400FreeText, onValueChange = { pb400FreeText = it }, label = { Text("4. Kraul") }, placeholder = { Text("1:01.00") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF), unfocusedBorderColor = Color(0xFF1B2E46)), modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Abbrechen", color = Color.LightGray, maxLines = 1)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val customMap = mutableMapOf<Pair<String, Int>, Long>()
                                fun addMapPb(str: String, dist: Int, txt: String) {
                                    TimeFormatter.parseTimeInputToMillis(txt)?.let { m ->
                                        if (m > 0) customMap[str to dist] = m
                                    }
                                }
                                addMapPb("Freistil", 25, free25Text)
                                addMapPb("Freistil", 50, free50Text)
                                addMapPb("Freistil", 100, free100Text)
                                addMapPb("Freistil", 200, free200Text)
                                addMapPb("Freistil", 400, free400Text)
                                addMapPb("Freistil", 800, free800Text)
                                addMapPb("Freistil", 1500, free1500Text)

                                addMapPb("Brust", 25, breast25Text)
                                addMapPb("Brust", 50, breast50Text)
                                addMapPb("Brust", 100, breast100Text)
                                addMapPb("Brust", 200, breast200Text)

                                addMapPb("Rücken", 25, back25Text)
                                addMapPb("Rücken", 50, back50Text)
                                addMapPb("Rücken", 100, back100Text)
                                addMapPb("Rücken", 200, back200Text)

                                addMapPb("Schmetterling", 25, fly25Text)
                                addMapPb("Schmetterling", 50, fly50Text)
                                addMapPb("Schmetterling", 100, fly100Text)
                                addMapPb("Schmetterling", 200, fly200Text)

                                addMapPb("Lagen", 100, pb100LagenText)
                                addMapPb("Lagen", 200, pb200LagenText)
                                addMapPb("Lagen", 400, pb400LagenText)

                                val primaryFav25 = customMap[stroke to 25] ?: TimeFormatter.parseTimeInputToMillis(free25Text)
                                val primaryFav50 = customMap[stroke to 50] ?: TimeFormatter.parseTimeInputToMillis(free50Text)
                                val primaryFav100 = customMap[stroke to 100] ?: TimeFormatter.parseTimeInputToMillis(free100Text)

                                onAdd(
                                    name.trim(),
                                    stroke,
                                    groupName.trim().ifEmpty { null },
                                    primaryFav25,
                                    primaryFav50,
                                    primaryFav100,
                                    TimeFormatter.parseTimeInputToMillis(pb100LagenText),
                                    TimeFormatter.parseTimeInputToMillis(pb200LagenText),
                                    TimeFormatter.parseTimeInputToMillis(pb400LagenText),
                                    TimeFormatter.parseTimeInputToMillis(flySplitText),
                                    TimeFormatter.parseTimeInputToMillis(backSplitText),
                                    TimeFormatter.parseTimeInputToMillis(breastSplitText),
                                    TimeFormatter.parseTimeInputToMillis(freeSplitText),
                                    TimeFormatter.parseTimeInputToMillis(pb200FlyText),
                                    TimeFormatter.parseTimeInputToMillis(pb200BackText),
                                    TimeFormatter.parseTimeInputToMillis(pb200BreastText),
                                    TimeFormatter.parseTimeInputToMillis(pb200FreeText),
                                    TimeFormatter.parseTimeInputToMillis(pb400FlyText),
                                    TimeFormatter.parseTimeInputToMillis(pb400BackText),
                                    TimeFormatter.parseTimeInputToMillis(pb400BreastText),
                                    TimeFormatter.parseTimeInputToMillis(pb400FreeText),
                                    customMap
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary),
                        modifier = Modifier.testTag("submit_add_swimmer_button")
                    ) {
                        Text("Hinzufügen", color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

private data class Tuple8<A, B, C, D, E, F, G, H>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G, val h: H
)