package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.StopwatchViewModel
import com.example.ui.components.DrillQuickfinderComponent
import com.example.ui.components.IntervalSplitLoggerComponent
import com.example.ui.components.RelayRandomizerComponent
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.DeepOceanBackground
import com.example.ui.theme.OceanSurface

enum class CoachingToolSection(val label: String, val icon: ImageVector) {
    DRILLS("Technik-Drills", Icons.AutoMirrored.Filled.MenuBook),
    RELAY("Team-Losung", Icons.Default.Groups),
    LOGGER("Soll vs. Ist", Icons.Default.Timer)
}

@Composable
fun CoachingToolsScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedSection by rememberSaveable { mutableStateOf(CoachingToolSection.DRILLS) }
    val swimmers by viewModel.allSwimmers.collectAsStateWithLifecycle()
    val availableGroups by viewModel.availableGroups.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOceanBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Compact, Flat Top Tool Selector Tabs
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = OceanSurface,
            border = BorderStroke(1.dp, Color(0xFF1B2E46)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoachingToolSection.entries.forEach { section ->
                    val isSelected = selectedSection == section
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) AquaPrimary else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSection = section }
                            .testTag("tool_tab_${section.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = section.label,
                                tint = if (isSelected) Color.Black else Color.LightGray,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = section.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 12.sp
                                ),
                                color = if (isSelected) Color.Black else Color.LightGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Active Tool Section View
        when (selectedSection) {
            CoachingToolSection.DRILLS -> DrillQuickfinderComponent()
            CoachingToolSection.RELAY -> RelayRandomizerComponent(dbSwimmers = swimmers, availableGroups = availableGroups)
            CoachingToolSection.LOGGER -> IntervalSplitLoggerComponent()
        }
    }
}
