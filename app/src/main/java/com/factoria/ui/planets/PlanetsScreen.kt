package com.factoria.ui.planets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun PlanetsScreen(
    viewModel: PlanetsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search Space Age planets, resources, hazards...", color = TextSecondary) },
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

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.filteredPlanets) { planet ->
                PlanetCardRow(planet = planet, onClick = { viewModel.selectPlanet(planet) })
            }
        }
    }

    uiState.selectedPlanet?.let { planet ->
        PlanetDetailDialog(
            planet = planet,
            onDismiss = { viewModel.dismissDetail() }
        )
    }
}

@Composable
fun PlanetCardRow(
    planet: PlanetInfo,
    onClick: () -> Unit
) {
    val accentColor = when (planet.id) {
        "vulcanus" -> FactorioOrange
        "gleba" -> CircuitGreen
        "fulgora" -> IndustrialYellow
        "aquilo" -> CircuitBlue
        else -> QualityEpicColor
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = planet.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
                Text(planet.subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Solar: ${(planet.solarMultiplier * 100).toInt()}%",
                        color = IndustrialYellow,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Day: ${planet.dayNightCycleSec}s",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun PlanetDetailDialog(
    planet: PlanetInfo,
    onDismiss: () -> Unit
) {
    val accentColor = when (planet.id) {
        "vulcanus" -> FactorioOrange
        "gleba" -> CircuitGreen
        "fulgora" -> IndustrialYellow
        "aquilo" -> CircuitBlue
        else -> QualityEpicColor
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = FactorioOrange, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Public, contentDescription = null, tint = accentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(planet.name, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(planet.description, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(color = SlateBorder)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Solar Power Multiplier:", color = TextSecondary)
                    Text("${(planet.solarMultiplier * 100).toInt()}%", color = IndustrialYellow, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Day/Night Cycle:", color = TextSecondary)
                    Text("${planet.dayNightCycleSec} seconds", color = TextPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("PRIMARY RESOURCES:", fontWeight = FontWeight.Bold, color = IndustrialYellow, fontSize = 12.sp)
                Text(planet.primaryResources.joinToString(" • "), color = TextPrimary, style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(4.dp))
                Text("EXCLUSIVE TECH & BUILDINGS:", fontWeight = FontWeight.Bold, color = CircuitBlue, fontSize = 12.sp)
                Text(planet.exclusiveTechs.joinToString(" • "), color = TextPrimary, style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(4.dp))
                Text("ENVIRONMENTAL HAZARDS:", fontWeight = FontWeight.Bold, color = CircuitRed, fontSize = 12.sp)
                Text(planet.environmentalHazards, color = TextSecondary, style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(4.dp))
                Text("STRATEGY TIPS:", fontWeight = FontWeight.Bold, color = CircuitGreen, fontSize = 12.sp)
                Text(planet.strategyTips, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
            }
        },
        containerColor = SlateSurface
    )
}
