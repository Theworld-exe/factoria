package com.factoria.ui.enemies

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factoria.core.designsystem.*

@Composable
fun EnemiesScreen(
    viewModel: EnemiesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = listOf("All", "Biters", "Spitters", "Worms", "Nests", "Space Age")
    val planets = listOf("All", "Nauvis", "Vulcanus", "Gleba")

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
            placeholder = { Text("Search enemies, threats, tactics...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CircuitRed) },
            trailingIcon = {
                if (uiState.searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CircuitRed,
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

        // Category Filter Chips
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
                        selectedContainerColor = CircuitRed,
                        selectedLabelColor = Color.Black,
                        containerColor = SlateSurface,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Planet Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(planets) { planet ->
                val selected = uiState.selectedPlanet == planet
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.selectPlanet(planet) },
                    label = { Text(planet, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IndustrialYellow,
                        selectedLabelColor = Color.Black,
                        containerColor = SlateSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Enemy List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.filteredEnemies) { enemy ->
                EnemyCardRow(enemy = enemy, onClick = { viewModel.selectEnemy(enemy) })
            }
        }
    }

    // Enemy Detail Dialog
    uiState.selectedEnemy?.let { enemy ->
        EnemyDetailDialog(
            enemy = enemy,
            onDismiss = { viewModel.dismissDetail() }
        )
    }
}

@Composable
fun EnemyCardRow(
    enemy: EnemyUnit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, CircuitRed.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = enemy.name,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = CircuitRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = enemy.planet.uppercase(),
                            color = CircuitRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "HP: ${enemy.health.toInt()}",
                        color = IndustrialYellow,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Atk: ${enemy.attackDamage}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Evo: ${(enemy.evolutionThreshold * 100).toInt()}%",
                        color = FactorioOrange,
                        fontSize = 12.sp
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun EnemyDetailDialog(
    enemy: EnemyUnit,
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
                Icon(Icons.Default.Warning, contentDescription = null, tint = CircuitRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(enemy.name, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(enemy.description, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(color = SlateBorder)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Health Points (HP):", color = TextSecondary)
                    Text("${enemy.health.toInt()}", color = IndustrialYellow, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Planet Location:", color = TextSecondary)
                    Text(enemy.planet, color = CircuitGreen, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Evolution Threshold:", color = TextSecondary)
                    Text("${(enemy.evolutionThreshold * 100).toInt()}%", color = FactorioOrange, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Attack Type & Damage:", color = TextSecondary)
                    Text("${enemy.attackType} (${enemy.attackDamage})", color = TextPrimary, fontSize = 12.sp)
                }

                if (enemy.resistances.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("DAMAGE RESISTANCES:", fontWeight = FontWeight.Bold, color = IndustrialYellow, fontSize = 12.sp)
                    enemy.resistances.forEach { r ->
                        val decreaseStr = if (r.decrease > 0) "${r.decrease.toInt()} / " else ""
                        Text("• ${r.type}: ${decreaseStr}${r.percent.toInt()}% reduction", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("RECOMMENDED COUNTERMEASURES:", fontWeight = FontWeight.Bold, color = CircuitGreen, fontSize = 12.sp)
                Text(enemy.recommendedCounter, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
            }
        },
        containerColor = SlateSurface
    )
}
