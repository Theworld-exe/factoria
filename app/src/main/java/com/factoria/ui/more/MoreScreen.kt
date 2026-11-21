package com.factoria.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factoria.core.designsystem.*
import com.factoria.navigation.Screen

@Composable
fun MoreScreen(
    onNavigate: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SECONDARY TOOLS & GUIDES",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = IndustrialYellow,
                letterSpacing = 1.2.sp
            )
        )

        MoreToolCard(
            title = "Blueprint Inspector & BOM",
            subtitle = "Decode Factorio 2.0 blueprint strings, view entity layouts & extract bills of materials",
            icon = Icons.Default.Description,
            accentColor = CircuitGreen,
            onClick = { onNavigate(Screen.Blueprints.route) }
        )

        MoreToolCard(
            title = "Interactive Technology Tree",
            subtitle = "Inspect technology prerequisites, recipe unlocks & cumulative science pack costs",
            icon = Icons.Default.AccountTree,
            accentColor = QualityEpicColor,
            onClick = { onNavigate(Screen.TechTree.route) }
        )

        MoreToolCard(
            title = "All In-Game Recipes Catalog",
            subtitle = "Browse, search & inspect crafting, smelting, chemical, biochamber & recycling recipes",
            icon = Icons.Default.PrecisionManufacturing,
            accentColor = IndustrialYellow,
            onClick = { onNavigate(Screen.Recipes.route) }
        )

        MoreToolCard(
            title = "Enemies & Threat Bestiary",
            subtitle = "Biters, Spitters, Worms, Demolishers & Pentapods: Stats, evolution thresholds & tactics",
            icon = Icons.Default.BugReport,
            accentColor = CircuitRed,
            onClick = { onNavigate(Screen.Enemies.route) }
        )

        MoreToolCard(
            title = "Space Age Planets Compendium",
            subtitle = "Nauvis, Vulcanus, Gleba, Fulgora, Aquilo & Platforms: Solar scaling, hazards & tech",
            icon = Icons.Default.Public,
            accentColor = CircuitBlue,
            onClick = { onNavigate(Screen.Planets.route) }
        )

        MoreToolCard(
            title = "Factory Todo & Outpost Checklist",
            subtitle = "Offline task list manager for factory goals, supply lists & planet outpost tasks",
            icon = Icons.Default.Checklist,
            accentColor = CircuitGreen,
            onClick = { onNavigate(Screen.Checklist.route) }
        )

        MoreToolCard(
            title = "Data Pack & About App",
            subtitle = "View dataset metadata, schema information, formulas & legal disclaimers",
            icon = Icons.Default.Info,
            accentColor = TextSecondary,
            onClick = { onNavigate(Screen.DataPack.route) }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MoreToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}
