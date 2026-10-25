package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    RELAY("Staffel-Auslosung", Icons.Default.Groups),
    LOGGER("Soll/Ist-Logger", Icons.Default.Timer)
}

@Composable
fun CoachingToolsScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier,
) {
    var selectedSection by remember { mutableStateOf(CoachingToolSection.DRILLS) }
    val swimmers by viewModel.allSwimmers.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepOceanBackground)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        // Top Tool Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = OceanSurface,
                border = BorderStroke(1.dp, Color(0xFF1B2E46))
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoachingToolSection.entries.forEach { section ->
                        val isSelected = selectedSection == section
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AquaPrimary else Color.Transparent,
                            modifier = Modifier
                                .clickable { selectedSection = section }
                                .testTag("tool_tab_${section.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = section.label,
                                    tint = if (isSelected) Color.Black else Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = section.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = if (isSelected) Color.Black else Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Tool Section View
        when (selectedSection) {
            CoachingToolSection.DRILLS -> DrillQuickfinderComponent()
            CoachingToolSection.RELAY -> RelayRandomizerComponent(dbSwimmers = swimmers)
            CoachingToolSection.LOGGER -> IntervalSplitLoggerComponent()
        }
    }
}
