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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

    var selectedSwimmerId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<Long?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(value = false) }
    var isAddGroupDialogOpen by remember { mutableStateOf(value = false) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val availableGroups by viewModel.availableGroups.collectAsStateWithLifecycle()
    val groupCounts = remember(allSwimmers) {
        allSwimmers.asSequence().mapNotNull { it.groupName?.trim() }.filter { it.isNotBlank() }.groupingBy { it.lowercase() }.eachCount()
    }
    val selectedGroup by viewModel.selectedGroupFilter.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { selectedUri ->
            viewModel.importSwimmersFromUri(context.contentResolver, selectedUri) { count ->
                val msg = if (count > 0) "$count Athlet(en) importiert" else "Keine neuen Athleten gefunden"
                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            }
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
            modifier = modifier,
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
                            newGroupName = ""
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
                            onClick = {
                                onCreateGroup(newGroupName.trim())
                                newGroupName = ""
                            },
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
    SwimmerFormDialog(
        initialSwimmer = null,
        availableGroups = availableGroups,
        defaultGroup = defaultGroup,
        dialogTitle = "Neuen Schwimmer anlegen",
        confirmButtonText = "Hinzufügen",
        dialogTestTag = "add_swimmer_dialog",
        onDismiss = onDismiss,
        onConfirm = { created ->
            onAdd(
                created.name,
                created.favoriteStroke,
                created.groupName,
                created.pb25mMillis,
                created.pb50mMillis,
                created.pb100mMillis,
                created.pb100mLagenMillis,
                created.pb200mLagenMillis,
                created.pb400mLagenMillis,
                created.lagenFlySplitMillis,
                created.lagenBackSplitMillis,
                created.lagenBreastSplitMillis,
                created.lagenFreeSplitMillis,
                created.pb200mLagenFlySplitMillis,
                created.pb200mLagenBackSplitMillis,
                created.pb200mLagenBreastSplitMillis,
                created.pb200mLagenFreeSplitMillis,
                created.pb400mLagenFlySplitMillis,
                created.pb400mLagenBackSplitMillis,
                created.pb400mLagenBreastSplitMillis,
                created.pb400mLagenFreeSplitMillis,
                created.parseCustomPbsMap()
            )
        }
    )
}