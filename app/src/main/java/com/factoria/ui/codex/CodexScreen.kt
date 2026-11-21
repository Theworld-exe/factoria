package com.factoria.ui.codex

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factoria.core.database.SearchResultItem
import com.factoria.core.designsystem.*
import com.factoria.core.model.*
import com.factoria.ui.paywall.UserPreferencesManager

@Composable
fun CodexScreen(
    viewModel: CodexViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("All", "Items", "Fluids", "Recipes", "Entities", "Technologies")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search 2.0 items, recipes, entities...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FactorioOrange) },
            trailingIcon = {
                if (uiState.searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FactorioOrange,
                unfocusedBorderColor = SlateBorder,
                focusedContainerColor = SlateSurface,
                unfocusedContainerColor = SlateSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val selected = uiState.selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FactorioOrange,
                        selectedLabelColor = Color.Black,
                        containerColor = SlateSurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Results or Categorized List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.searchQuery.isNotBlank()) {
                items(uiState.searchResults) { result ->
                    SearchResultCard(result = result, onClick = {
                        when (result.type) {
                            "item" -> uiState.items.find { it.id == result.id }?.let { viewModel.selectItem(it) }
                            "recipe" -> uiState.recipes.find { it.id == result.id }?.let { viewModel.selectRecipe(it) }
                            "entity" -> uiState.entities.find { it.id == result.id }?.let { viewModel.selectEntity(it) }
                            "technology" -> uiState.technologies.find { it.id == result.id }?.let { viewModel.selectTechnology(it) }
                        }
                    })
                }
            } else {
                when (uiState.selectedCategory) {
                    "All", "Items" -> {
                        if (uiState.selectedCategory == "All") {
                            item { SectionHeader("ITEMS (${uiState.items.size})") }
                        }
                        items(uiState.items) { item ->
                            ItemRow(item = item, onClick = { viewModel.selectItem(item) })
                        }
                    }
                }
                when (uiState.selectedCategory) {
                    "All", "Fluids" -> {
                        if (uiState.selectedCategory == "All") {
                            item { SectionHeader("FLUIDS (${uiState.fluids.size})") }
                        }
                        items(uiState.fluids) { fluid ->
                            FluidRow(fluid = fluid)
                        }
                    }
                }
                when (uiState.selectedCategory) {
                    "All", "Recipes" -> {
                        if (uiState.selectedCategory == "All") {
                            item { SectionHeader("RECIPES (${uiState.recipes.size})") }
                        }
                        items(uiState.recipes) { recipe ->
                            RecipeRow(recipe = recipe, onClick = { viewModel.selectRecipe(recipe) })
                        }
                    }
                }
                when (uiState.selectedCategory) {
                    "All", "Entities" -> {
                        if (uiState.selectedCategory == "All") {
                            item { SectionHeader("ENTITIES & MACHINES (${uiState.entities.size})") }
                        }
                        items(uiState.entities) { entity ->
                            EntityRow(entity = entity, onClick = { viewModel.selectEntity(entity) })
                        }
                    }
                }
                when (uiState.selectedCategory) {
                    "All", "Technologies" -> {
                        if (uiState.selectedCategory == "All") {
                            item { SectionHeader("TECHNOLOGIES (${uiState.technologies.size})") }
                        }
                        items(uiState.technologies) { tech ->
                            TechRow(tech = tech, onClick = { viewModel.selectTechnology(tech) })
                        }
                    }
                }
            }
        }
    }

    // Detail Dialogs
    uiState.selectedItem?.let { item ->
        ItemDetailDialog(
            item = item,
            usedIn = uiState.relatedUsedIn,
            producedBy = uiState.relatedProducedBy,
            onDismiss = { viewModel.dismissDetail() }
        )
    }

    uiState.selectedRecipe?.let { recipe ->
        RecipeDetailDialog(
            recipe = recipe,
            onDismiss = { viewModel.dismissDetail() }
        )
    }

    uiState.selectedEntity?.let { entity ->
        EntityDetailDialog(
            entity = entity,
            onDismiss = { viewModel.dismissDetail() }
        )
    }

    uiState.selectedTechnology?.let { tech ->
        TechDetailDialog(
            tech = tech,
            onDismiss = { viewModel.dismissDetail() }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            color = IndustrialYellow,
            letterSpacing = 1.2.sp
        ),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun SearchResultCard(result: SearchResultItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FactoriaItemIcon(itemId = result.id, itemType = result.type, size = 36.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(result.localizedName, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                result.category?.let { cat ->
                    Text(cat.uppercase(), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Surface(
                color = when (result.type) {
                    "item" -> FactorioOrange.copy(alpha = 0.2f)
                    "fluid" -> CircuitBlue.copy(alpha = 0.2f)
                    "recipe" -> IndustrialYellow.copy(alpha = 0.2f)
                    "entity" -> CircuitGreen.copy(alpha = 0.2f)
                    else -> QualityEpicColor.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = result.type.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ItemRow(item: Item, onClick: () -> Unit) {
    val context = LocalContext.current
    val userPrefsManager = remember { UserPreferencesManager.getInstance(context) }
    val favoriteIds by userPrefsManager.favoriteItemIds.collectAsState()
    val isFav = favoriteIds.contains(item.id)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FactoriaItemIcon(itemId = item.id, itemType = "item", size = 38.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.localizedName, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("Group: ${item.itemGroup} | Stack: ${item.stackSize}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            if (item.hasSpoilage) {
                Surface(color = CircuitRed.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        "Spoils: ${item.spoilageTimeSeconds.toInt()}s",
                        color = CircuitRed,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = { userPrefsManager.toggleFavorite(item.id) }) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Pin Favorite",
                    tint = if (isFav) IndustrialYellow else TextSecondary
                )
            }
        }
    }
}

@Composable
fun FluidRow(fluid: Fluid) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(6.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FactoriaItemIcon(itemId = fluid.id, itemType = "fluid", size = 38.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fluid.localizedName, color = CircuitBlue, fontWeight = FontWeight.Bold)
                Text("Default Temp: ${fluid.defaultTemp}°C", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            if (fluid.fuelValueJoules > 0) {
                Text("${(fluid.fuelValueJoules / 1_000_000.0)} MJ", color = IndustrialYellow, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun RecipeRow(recipe: Recipe, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FactoriaItemIcon(itemId = recipe.id, itemType = "recipe", size = 38.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(recipe.localizedName, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("${recipe.craftingTime}s", color = FactorioOrange, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("Category: ${recipe.category}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun EntityRow(entity: Entity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FactoriaItemIcon(itemId = entity.id, itemType = "entity", size = 38.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entity.localizedName, color = IndustrialYellow, fontWeight = FontWeight.Bold)
                Text("Speed: ${entity.craftingSpeed}x | Modules: ${entity.moduleSlots}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun TechRow(tech: Technology, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FactoriaItemIcon(itemId = tech.id, itemType = "technology", size = 38.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tech.localizedName, color = QualityEpicColor, fontWeight = FontWeight.Bold)
                Text("Science Units: ${tech.unitCount} x ${tech.unitTimeSeconds}s", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ItemDetailDialog(
    item: Item,
    usedIn: List<Recipe>,
    producedBy: List<Recipe>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE", color = FactorioOrange) }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FactoriaItemIcon(itemId = item.id, itemType = "item", size = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(item.localizedName, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Group: ${item.itemGroup}", color = TextSecondary)
                Text("Subgroup: ${item.subgroup}", color = TextSecondary)
                Text("Stack Size: ${item.stackSize}", color = TextSecondary)
                if (item.fuelValueJoules > 0) {
                    Text("Fuel Value: ${(item.fuelValueJoules / 1_000_000.0)} MJ", color = IndustrialYellow)
                }
                if (usedIn.isNotEmpty()) {
                    Text("Used In Recipes (${usedIn.size}):", fontWeight = FontWeight.Bold, color = IndustrialYellow)
                    usedIn.take(5).forEach { r -> Text("• ${r.localizedName}", color = TextPrimary, fontSize = 12.sp) }
                }
                if (producedBy.isNotEmpty()) {
                    Text("Produced By Recipes (${producedBy.size}):", fontWeight = FontWeight.Bold, color = IndustrialYellow)
                    producedBy.take(5).forEach { r -> Text("• ${r.localizedName}", color = TextPrimary, fontSize = 12.sp) }
                }
            }
        },
        containerColor = SlateSurface
    )
}

@Composable
fun RecipeDetailDialog(recipe: Recipe, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = FactorioOrange) } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FactoriaItemIcon(itemId = recipe.id, itemType = "recipe", size = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(recipe.localizedName, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Category: ${recipe.category}", color = TextSecondary)
                Text("Crafting Time: ${recipe.craftingTime}s", color = FactorioOrange)
                Text("Ingredients:", fontWeight = FontWeight.Bold, color = IndustrialYellow)
                recipe.ingredients.forEach { ing -> Text("• ${ing.amount}x ${ing.targetId}", color = TextPrimary, fontSize = 12.sp) }
                Text("Products:", fontWeight = FontWeight.Bold, color = IndustrialYellow)
                recipe.products.forEach { prod -> Text("• ${prod.amount}x ${prod.targetId}", color = TextPrimary, fontSize = 12.sp) }
            }
        },
        containerColor = SlateSurface
    )
}

@Composable
fun EntityDetailDialog(entity: Entity, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = FactorioOrange) } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FactoriaItemIcon(itemId = entity.id, itemType = "entity", size = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(entity.localizedName, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Type: ${entity.type.name}", color = TextSecondary)
                Text("Crafting Speed: ${entity.craftingSpeed}x", color = FactorioOrange)
                Text("Module Slots: ${entity.moduleSlots}", color = IndustrialYellow)
                Text("Energy Usage: ${(entity.energyUsageWatts / 1000.0)} kW", color = CircuitGreen)
                Text("Health: ${entity.health}", color = TextPrimary)
            }
        },
        containerColor = SlateSurface
    )
}

@Composable
fun TechDetailDialog(tech: Technology, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = FactorioOrange) } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FactoriaItemIcon(itemId = tech.id, itemType = "technology", size = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(tech.localizedName, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Science Packs Needed:", fontWeight = FontWeight.Bold, color = IndustrialYellow)
                tech.sciencePacks.forEach { sp -> Text("• ${sp.amount}x ${sp.sciencePackId}", color = TextPrimary, fontSize = 12.sp) }
                if (tech.unlockedRecipeIds.isNotEmpty()) {
                    Text("Unlocks Recipes (${tech.unlockedRecipeIds.size}):", fontWeight = FontWeight.Bold, color = IndustrialYellow)
                    tech.unlockedRecipeIds.forEach { rec -> Text("• $rec", color = TextPrimary, fontSize = 12.sp) }
                }
            }
        },
        containerColor = SlateSurface
    )
}
