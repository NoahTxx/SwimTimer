package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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

@Composable
fun SwimmersManagementScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier,
    isActiveTab: Boolean = true,
) {
    val context = LocalContext.current
    val allSwimmers by viewModel.allSwimmers.collectAsStateWithLifecycle()
    val swimmers by viewModel.swimmerUiList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.swimmersSearchQuery.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()

    var selectedSwimmerId by remember { mutableStateOf<Long?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(value = false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val json = stream.bufferedReader().readText()
                    val imported = ExportHelper.importSwimmersFromJson(json)
                    if (imported.isNotEmpty()) {
                        viewModel.importSwimmers(imported)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val activeSwimmer = allSwimmers.firstOrNull { swimmer -> swimmer.id == selectedSwimmerId }

    // If an athlete is selected, show their personal history / detail screen
    if (activeSwimmer != null) {
        SwimmerDetailScreen(
            swimmer = activeSwimmer,
            allSessions = allSessions,
            isActiveTab = isActiveTab,
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
            .padding(horizontal = 16.dp, vertical = 16.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Schwimmteam & Kader",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextWhite,
                        maxLines = 1
                    )
                    Text(
                        text = "Tippe auf einen Athleten für die persönliche Historie",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Export Button
                    FilledTonalIconButton(
                        onClick = {
                            if (allSwimmers.isNotEmpty()) {
                                ExportHelper.shareSwimmersExport(context, allSwimmers)
                            }
                        },
                        enabled = allSwimmers.isNotEmpty(),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = OceanSurface,
                            contentColor = AquaPrimary
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Athleten exportieren",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Import Button
                    FilledTonalIconButton(
                        onClick = {
                            importLauncher.launch("*/*")
                        },
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = OceanSurface,
                            contentColor = AquaPrimary
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
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
                    .padding(bottom = 12.dp),
                placeholder = { Text("Schwimmer suchen...") },
                singleLine = true,
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

        if (isAddDialogOpen) {
            AddSwimmerDialog(
                onDismiss = { isAddDialogOpen = false },
                onAdd = { name, stroke, pb25, pb50, pb100, pb100Lagen, pb200Lagen, pb400Lagen, fly, back, breast, free, pb200Fly, pb200Back, pb200Breast, pb200Free, pb400Fly, pb400Back, pb400Breast, pb400Free ->
                    viewModel.addSwimmer(
                        name = name,
                        stroke = stroke,
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
                        pb400Free = pb400Free
                    )
                    isAddDialogOpen = false
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
                            text = swimmer.name.take(1).uppercase(),
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = swimmer.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                val pb50Formatted = swimmer.pb50mText.ifEmpty { "--:--" }
                Text(
                    text = pb50Formatted,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSwimmerDialog(
    onDismiss: () -> Unit,
    onAdd: (
        name: String,
        stroke: String,
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
        pb400Free: Long?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var stroke by remember { mutableStateOf("Freistil") }
    var isStrokeDropdownExpanded by remember { mutableStateOf(value = false) }

    var pb25Text by remember { mutableStateOf("") }
    var pb50Text by remember { mutableStateOf("") }
    var pb100Text by remember { mutableStateOf("") }
    var pb100LagenText by remember { mutableStateOf("") }
    var pb200LagenText by remember { mutableStateOf("") }
    var pb400LagenText by remember { mutableStateOf("") }

    // 200m Lagen Splits (4x 50m)
    var pb200FlyText by remember { mutableStateOf("") }
    var pb200BackText by remember { mutableStateOf("") }
    var pb200BreastText by remember { mutableStateOf("") }
    var pb200FreeText by remember { mutableStateOf("") }

    // 400m Lagen Splits (4x 100m)
    var pb400FlyText by remember { mutableStateOf("") }
    var pb400BackText by remember { mutableStateOf("") }
    var pb400BreastText by remember { mutableStateOf("") }
    var pb400FreeText by remember { mutableStateOf("") }

    // 100m Lagen Splits (4x 25m)
    var flySplitText by remember { mutableStateOf("") }
    var backSplitText by remember { mutableStateOf("") }
    var breastSplitText by remember { mutableStateOf("") }
    var freeSplitText by remember { mutableStateOf("") }

    val strokeOptions = listOf("Freistil", "Brust", "Rücken", "Schmetterling", "Lagen")

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

                Spacer(modifier = Modifier.height(14.dp))

                // Name field
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_swimmer_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Hauptschwimmart Dropdown Menu
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
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStrokeDropdownExpanded)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AquaPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = AquaPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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

                Spacer(modifier = Modifier.height(10.dp))

                // Standard PB fields (Optional)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = pb25Text,
                        onValueChange = { pb25Text = it },
                        label = { Text("PB 25m", maxLines = 1) },
                        placeholder = { Text("12.50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AquaPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb50Text,
                        onValueChange = { pb50Text = it },
                        label = { Text("PB 50m", maxLines = 1) },
                        placeholder = { Text("24.80") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AquaPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb100Text,
                        onValueChange = { pb100Text = it },
                        label = { Text("PB 100m", maxLines = 1) },
                        placeholder = { Text("54.20") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = AquaPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Lagen PBs (100m, 200m, 400m)
                Text(
                    text = "Lagen Wettkampfzeiten (optional, z.B. 1:03.50):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFD600)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = pb100LagenText,
                        onValueChange = { pb100LagenText = it },
                        label = { Text("100m Lagen", maxLines = 1) },
                        placeholder = { Text("1:03.50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = Color(0xFFFFD600)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb200LagenText,
                        onValueChange = { pb200LagenText = it },
                        label = { Text("200m Lagen", maxLines = 1) },
                        placeholder = { Text("2:18.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = Color(0xFFFFD600)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb400LagenText,
                        onValueChange = { pb400LagenText = it },
                        label = { Text("400m Lagen", maxLines = 1) },
                        placeholder = { Text("4:55.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = Color(0xFFFFD600)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 200m Lagen Teilstrecken (4x 50m)
                Text(
                    text = "200m Lagen Teilstrecken (4 × 50m):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AquaPrimary
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = pb200FlyText,
                        onValueChange = { pb200FlyText = it },
                        label = { Text("1. Schmett (50m)", maxLines = 1) },
                        placeholder = { Text("29.50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb200BackText,
                        onValueChange = { pb200BackText = it },
                        label = { Text("2. Rücken (50m)", maxLines = 1) },
                        placeholder = { Text("35.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFFD600)),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = pb200BreastText,
                        onValueChange = { pb200BreastText = it },
                        label = { Text("3. Brust (50m)", maxLines = 1) },
                        placeholder = { Text("39.80") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E676)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb200FreeText,
                        onValueChange = { pb200FreeText = it },
                        label = { Text("4. Kraul (50m)", maxLines = 1) },
                        placeholder = { Text("28.00") },
                        singleLine = true,
                        supportingText = { Text("Sync 50m PB", fontSize = 8.sp, color = PaceGreen) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF)),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 400m Lagen Teilstrecken (4x 100m)
                Text(
                    text = "400m Lagen Teilstrecken (4 × 100m):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF80D8FF)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = pb400FlyText,
                        onValueChange = { pb400FlyText = it },
                        label = { Text("1. Schmett (100m)", maxLines = 1) },
                        placeholder = { Text("1:04.50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb400BackText,
                        onValueChange = { pb400BackText = it },
                        label = { Text("2. Rücken (100m)", maxLines = 1) },
                        placeholder = { Text("1:14.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFFD600)),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = pb400BreastText,
                        onValueChange = { pb400BreastText = it },
                        label = { Text("3. Brust (100m)", maxLines = 1) },
                        placeholder = { Text("1:24.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E676)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pb400FreeText,
                        onValueChange = { pb400FreeText = it },
                        label = { Text("4. Kraul (100m)", maxLines = 1) },
                        placeholder = { Text("1:01.00") },
                        singleLine = true,
                        supportingText = { Text("Sync 100m PB", fontSize = 8.sp, color = PaceGreen) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF)),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 100m Lagen Teilstrecken (4x 25m)
                Text(
                    text = "100m Lagen Teilstrecken (4 × 25m):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFFD600)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = flySplitText,
                        onValueChange = { flySplitText = it },
                        label = { Text("1. Schmett (25m)", maxLines = 1) },
                        placeholder = { Text("13.80") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = backSplitText,
                        onValueChange = { backSplitText = it },
                        label = { Text("2. Rücken (25m)", maxLines = 1) },
                        placeholder = { Text("15.50") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFFFFD600)),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = breastSplitText,
                        onValueChange = { breastSplitText = it },
                        label = { Text("3. Brust (25m)", maxLines = 1) },
                        placeholder = { Text("18.00") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E676)),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = freeSplitText,
                        onValueChange = { freeSplitText = it },
                        label = { Text("4. Kraul (25m)", maxLines = 1) },
                        placeholder = { Text("12.90") },
                        singleLine = true,
                        supportingText = { Text("Sync 25m PB", fontSize = 8.sp, color = PaceGreen) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = Color(0xFF00E5FF)),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

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
                                onAdd(
                                    name.trim(),
                                    stroke,
                                    TimeFormatter.parseTimeInputToMillis(pb25Text),
                                    TimeFormatter.parseTimeInputToMillis(pb50Text),
                                    TimeFormatter.parseTimeInputToMillis(pb100Text),
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
                                    TimeFormatter.parseTimeInputToMillis(pb400FreeText)
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