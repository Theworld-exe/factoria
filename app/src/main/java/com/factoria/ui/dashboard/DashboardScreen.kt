package com.factoria.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factoria.R
import com.factoria.core.database.SearchResultItem
import com.factoria.core.designsystem.*
import com.factoria.navigation.Screen
import com.factoria.ui.paywall.UserPreferencesManager

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        FactoriaCard(borderColor = FactorioOrange) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "The Factory Must Grow Logo",
                        modifier = Modifier
                            .size(56.dp)
                            .padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "FACTORIA",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = FactorioOrange
                            )
                        )
                        Text(
                            text = "Factorio 2.0 & Space Age Companion",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    }
                }
                OfflineBadge()
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SlateBorder)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Factoria is an unofficial, fan-made companion app. It is not affiliated with, endorsed, or sponsored by Wube Software.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontWeight = FontWeight.SemiBold)
            )
        }

        // Pinned Favorites Section
        val context = LocalContext.current
        val userPrefsManager = remember { UserPreferencesManager.getInstance(context) }
        val favoriteIds by userPrefsManager.favoriteItemIds.collectAsState()

        if (favoriteIds.isNotEmpty()) {
            FactoriaCard(borderColor = IndustrialYellow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = IndustrialYellow)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PINNED FAVORITES (${favoriteIds.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = IndustrialYellow,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    favoriteIds.forEach { itemId ->
                        Surface(
                            modifier = Modifier.bounceClick { userPrefsManager.toggleFavorite(itemId) },
                            shape = RoundedCornerShape(6.dp),
                            color = SlateSurfaceVariant,
                            border = BorderStroke(1.dp, IndustrialYellow.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FactoriaItemIcon(itemId = itemId, itemType = "item", size = 24.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = itemId.replace("-", " ").replaceFirstChar { it.uppercase() },
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Clear, contentDescription = "Unpin", tint = TextSecondary, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // Instant FTS Search Bar Widget
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search items, recipes, fluids, entities & tech...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = FactorioOrange) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FactorioOrange,
                unfocusedBorderColor = SlateBorder,
                focusedContainerColor = SlateSurface,
                unfocusedContainerColor = SlateSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        // Instant Search Results
        if (searchResults.isNotEmpty()) {
            FactoriaCard(borderColor = FactorioOrange.copy(alpha = 0.5f)) {
                Text(
                    text = "SEARCH RESULTS (${searchResults.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = IndustrialYellow,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    searchResults.take(10).forEach { item ->
                        SearchResultRow(item = item) {
                            onNavigate(Screen.Codex.route)
                        }
                    }
                }
            }
        }

        // Quick Stats
        Text(
            text = "DATABASE STATUS",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = IndustrialYellow,
                letterSpacing = 1.2.sp
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard("Factorio", "2.0+", "Version", FactorioOrange, Modifier.weight(1f))
            MetricCard("Expansion", "Space Age", "Active", CircuitGreen, Modifier.weight(1f))
        }

        Text(
            text = "CORE TOOLS",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = IndustrialYellow,
                letterSpacing = 1.2.sp
            )
        )

        FeatureNavCard(
            title = "Production & Ratio Calculator",
            subtitle = "Simplex LP solver for complex networks, cycles, byproducts & quality",
            icon = Icons.Default.Calculate,
            accentColor = FactorioOrange,
            onClick = { onNavigate(Screen.Calculator.route) }
        )

        FeatureNavCard(
            title = "Factorio Codex",
            subtitle = "Offline searchable database of items, fluids, recipes, entities & tech",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            accentColor = CircuitBlue,
            onClick = { onNavigate(Screen.Codex.route) }
        )

        FeatureNavCard(
            title = "Blueprint Inspector & BOM",
            subtitle = "Decode 2.0 blueprint strings, view entity layouts & extract bills of materials",
            icon = Icons.Default.Description,
            accentColor = CircuitGreen,
            onClick = { onNavigate(Screen.Blueprints.route) }
        )

        FeatureNavCard(
            title = "Logistics, Power & Trains",
            subtitle = "Throughput for belts, inserters, 2.0 pipes, nuclear, solar & train loads",
            icon = Icons.Default.ElectricBolt,
            accentColor = IndustrialYellow,
            onClick = { onNavigate(Screen.Logistics.route) }
        )

        FeatureNavCard(
            title = "Interactive Technology Tree",
            subtitle = "Inspect dependencies, recipe unlocks, and total cumulative science costs",
            icon = Icons.Default.AccountTree,
            accentColor = QualityEpicColor,
            onClick = { onNavigate(Screen.TechTree.route) }
        )

        FeatureNavCard(
            title = "All In-Game Recipes Catalog",
            subtitle = "Browse, search, and inspect all 2.0 & Space Age crafting, smelting, chemical & recycling recipes",
            icon = Icons.Default.PrecisionManufacturing,
            accentColor = IndustrialYellow,
            onClick = { onNavigate(Screen.Recipes.route) }
        )

        FeatureNavCard(
            title = "Enemies & Threat Bestiary",
            subtitle = "Biters, Spitters, Worms, Demolishers & Pentapods: Stats, evolution, resistances & tactics",
            icon = Icons.Default.BugReport,
            accentColor = CircuitRed,
            onClick = { onNavigate(Screen.Enemies.route) }
        )

        FeatureNavCard(
            title = "Space Age Planets Compendium",
            subtitle = "Nauvis, Vulcanus, Gleba, Fulgora, Aquilo & Platforms: Solar scaling, hazards & tech",
            icon = Icons.Default.Public,
            accentColor = CircuitBlue,
            onClick = { onNavigate(Screen.Planets.route) }
        )

        FeatureNavCard(
            title = "Factory Todo & Outpost Checklist",
            subtitle = "Offline task list manager for factory goals, supply lists & planet outposts",
            icon = Icons.Default.Checklist,
            accentColor = CircuitGreen,
            onClick = { onNavigate(Screen.Checklist.route) }
        )

        FeatureNavCard(
            title = "Data Pack & Assumptions",
            subtitle = "View dataset metadata, schema information, and mechanics formulas",
            icon = Icons.Default.Info,
            accentColor = TextSecondary,
            onClick = { onNavigate(Screen.DataPack.route) }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SearchResultRow(
    item: SearchResultItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        color = SlateBackground,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.localizedName.ifBlank { item.name },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                val cat = item.category
                if (!cat.isNullOrBlank()) {
                    Text(
                        text = cat.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                    )
                }
            }
            Surface(
                color = when (item.type.lowercase()) {
                    "item" -> FactorioOrange.copy(alpha = 0.2f)
                    "fluid" -> CircuitBlue.copy(alpha = 0.2f)
                    "recipe" -> CircuitGreen.copy(alpha = 0.2f)
                    else -> IndustrialYellow.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = item.type.uppercase(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun FeatureNavCard(
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
                modifier = Modifier.size(48.dp)
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
