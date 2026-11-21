package com.factoria.ui.recipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factoria.core.designsystem.*
import com.factoria.core.model.Recipe
import com.factoria.ui.paywall.UserPreferencesManager

@Composable
fun RecipesScreen(
    viewModel: RecipesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("All", "smelting", "crafting", "chemistry", "centrifuging", "metallurgy", "casting", "organic", "recycling", "Oil")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search all in-game recipes, ingredients, products...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = IndustrialYellow) },
            trailingIcon = {
                if (uiState.searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = IndustrialYellow,
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

        // Crafting Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val selected = uiState.selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.selectCategory(category) },
                    label = { Text(category.replaceFirstChar { it.uppercase() }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndustrialYellow,
                        selectedLabelColor = Color.Black,
                        containerColor = SlateSurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Loading or Recipe List
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = IndustrialYellow)
            }
        } else if (uiState.filteredRecipes.isEmpty()) {
            FactoriaCard {
                Text(
                    text = "No in-game recipes found for query '${uiState.searchQuery}'.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.filteredRecipes, key = { it.id }) { recipe ->
                    RecipeCardRow(recipe = recipe, onClick = { viewModel.selectRecipe(recipe) })
                }
            }
        }
    }

    // Recipe Detail Modal Dialog
    uiState.selectedRecipe?.let { recipe ->
        RecipeDetailDialog(
            recipe = recipe,
            onDismiss = { viewModel.dismissDetail() }
        )
    }
}

@Composable
fun RecipeCardRow(
    recipe: Recipe,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val userPrefsManager = remember { UserPreferencesManager.getInstance(context) }
    val favoriteIds by userPrefsManager.favoriteItemIds.collectAsState()
    val isFav = favoriteIds.contains(recipe.id)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, IndustrialYellow.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FactoriaItemIcon(itemId = recipe.id, itemType = "recipe", size = 42.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.localizedName.ifBlank { recipe.name },
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${recipe.craftingTime}s",
                        color = FactorioOrange,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = IndustrialYellow.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = recipe.category.uppercase(),
                            color = IndustrialYellow,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${recipe.ingredients.size} In • ${recipe.products.size} Out",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            IconButton(onClick = { userPrefsManager.toggleFavorite(recipe.id) }) {
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
fun RecipeDetailDialog(
    recipe: Recipe,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = FactorioOrange, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FactoriaItemIcon(itemId = recipe.id, itemType = "recipe", size = 32.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(recipe.localizedName.ifBlank { recipe.name }, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Machine Category:", color = TextSecondary)
                    Text(recipe.category, color = IndustrialYellow, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Base Crafting Time:", color = TextSecondary)
                    Text("${recipe.craftingTime} seconds", color = FactorioOrange, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = SlateBorder)

                // Ingredients
                Text("INGREDIENTS (${recipe.ingredients.size}):", fontWeight = FontWeight.Bold, color = IndustrialYellow, fontSize = 12.sp)
                recipe.ingredients.forEach { ing ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FactoriaItemIcon(itemId = ing.targetId, itemType = "item", size = 20.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(ing.targetId.replace("-", " ").replaceFirstChar { it.uppercase() }, color = TextPrimary, fontSize = 12.sp)
                        }
                        Text("${ing.amount}x", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Products
                Text("PRODUCTS (${recipe.products.size}):", fontWeight = FontWeight.Bold, color = CircuitGreen, fontSize = 12.sp)
                recipe.products.forEach { prod ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FactoriaItemIcon(itemId = prod.targetId, itemType = "item", size = 20.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(prod.targetId.replace("-", " ").replaceFirstChar { it.uppercase() }, color = TextPrimary, fontSize = 12.sp)
                        }
                        val probText = if (prod.probability < 1.0) " (${(prod.probability * 100).toInt()}%)" else ""
                        Text("${prod.amount}x$probText", color = CircuitGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        containerColor = SlateSurface
    )
}
