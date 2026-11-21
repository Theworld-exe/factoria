package com.factoria.ui.techtree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Search
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
import com.factoria.core.model.Technology

@Composable
fun TechTreeScreen(
    viewModel: TechTreeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
    ) {
        // Search
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Filter technologies...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FactorioOrange) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FactorioOrange,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Tech Details & Cumulative Cost Card
        uiState.selectedTech?.let { tech ->
            FactoriaCard(borderColor = QualityEpicColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = tech.localizedName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            "Formula: ${tech.unitCount} cycles × ${tech.unitTimeSeconds.toInt()}s",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = QualityEpicColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Prerequisites
                if (tech.prerequisiteIds.isNotEmpty()) {
                    Text("PREREQUISITES", fontWeight = FontWeight.Bold, color = IndustrialYellow, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tech.prerequisiteIds.forEach { preId ->
                            Surface(
                                color = SlateSurfaceVariant,
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = preId.replace("-", " "),
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Cumulative Science Packs needed
                Text(
                    "TOTAL CUMULATIVE SCIENCE (From Start)",
                    fontWeight = FontWeight.Bold,
                    color = FactorioOrange,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                uiState.cumulativePacks.forEach { (packId, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = packId.replace("-", " ").replaceFirstChar { it.uppercase() },
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$count",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CircuitBlue,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // List of all technologies
        Text(
            text = "ALL TECHNOLOGIES",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = IndustrialYellow,
                letterSpacing = 1.2.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        val filteredTechs = uiState.technologies.filter {
            uiState.searchQuery.isBlank() || it.name.contains(uiState.searchQuery, ignoreCase = true) || it.localizedName.contains(uiState.searchQuery, ignoreCase = true)
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredTechs) { tech ->
                val isSelected = tech.id == uiState.selectedTech?.id
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectTechnology(tech) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) SlateSurfaceVariant else SlateSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) FactorioOrange else SlateBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = tech.localizedName,
                                color = if (isSelected) FactorioOrange else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${tech.sciencePacks.size} pack types • ${tech.unitCount} units",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (isSelected) {
                            Text("SELECTED", color = FactorioOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
