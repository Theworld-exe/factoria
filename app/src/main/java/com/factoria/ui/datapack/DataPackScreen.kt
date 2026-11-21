package com.factoria.ui.datapack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.factoria.core.database.sync.SyncState
import com.factoria.core.designsystem.*

@Composable
fun DataPackScreen(
    viewModel: DataPackViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val activeMeta by viewModel.activeMetadata.collectAsState()
    val syncState by viewModel.syncState.collectAsState()

    var selectedDataset by remember { mutableStateOf("2.0") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Database Status
        Text(
            text = "ACTIVE DATABASE PROFILE",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = IndustrialYellow,
                letterSpacing = 1.2.sp
            )
        )

        FactoriaCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dataset Target", color = TextSecondary)
                Text(
                    if (activeMeta?.dataset == "spa") "Space Age (2.0)" else "Vanilla Base (2.0)",
                    color = FactorioOrange,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Game Balance Version", color = TextSecondary)
                Text(
                    activeMeta?.sourceVersion ?: "2.0.77",
                    color = CircuitGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Schema Architecture", color = TextSecondary)
                Text(
                    "v${activeMeta?.schemaVersion ?: "1.0.0"} (Normalized SQLite)",
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Entity Breakdown", color = TextSecondary)
                Text(
                    "${activeMeta?.itemsCount ?: 399} items | ${activeMeta?.recipesCount ?: 403} recipes | ${activeMeta?.machinesCount ?: 18} machines",
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (!activeMeta?.contentChecksum.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SHA-256 Checksum", color = TextSecondary)
                    Text(
                        activeMeta?.contentChecksum?.take(12) + "...",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // In-App Synchronizer
        Text(
            text = "DATABASE SYNCHRONIZATION",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = IndustrialYellow,
                letterSpacing = 1.2.sp
            )
        )

        FactoriaCard {
            Text(
                "Sync with Remote Game Data",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Updates recipes, items, quality multipliers, and machine stats without requiring an APK rebuild. Download is verified via SHA-256 and checked for foreign-key integrity before atomic swap.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Dataset Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedDataset == "2.0",
                    onClick = { selectedDataset = "2.0" },
                    label = { Text("Base 2.0") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FactorioOrange,
                        selectedLabelColor = Color.Black
                    )
                )
                FilterChip(
                    selected = selectedDataset == "spa",
                    onClick = { selectedDataset = "spa" },
                    label = { Text("Space Age") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CircuitGreen,
                        selectedLabelColor = Color.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button
            Button(
                onClick = { viewModel.checkForUpdate(selectedDataset) },
                enabled = syncState !is SyncState.Downloading && syncState !is SyncState.Validating && syncState !is SyncState.Applying,
                colors = ButtonDefaults.buttonColors(containerColor = FactorioOrange),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CHECK FOR DATABASE UPDATES", fontWeight = FontWeight.Bold, color = SlateBackground)
            }

            // Sync State Display
            when (val state = syncState) {
                is SyncState.Idle -> {
                    // Nothing to display
                }
                is SyncState.CheckingForUpdates -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = FactorioOrange, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Checking remote manifest for dataset '${state.datasetId}'...", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                is SyncState.UpdateAvailable -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = SlateSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, FactorioOrange),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("New Dataset Version Available!", color = FactorioOrange, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Current: ${state.currentVersion}  ➔  New: ${state.newVersion}", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            if (state.entry.releaseNotes.isNotBlank()) {
                                Text("Notes: ${state.entry.releaseNotes}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.applyUpdate(state.entry) },
                                colors = ButtonDefaults.buttonColors(containerColor = CircuitGreen),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("DOWNLOAD & INSTALL UPDATE", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
                is SyncState.UpToDate -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CircuitGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Database is up to date (version ${state.version}).", color = CircuitGreen, style = MaterialTheme.typography.bodySmall)
                    }
                }
                is SyncState.Downloading -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Downloading dataset...", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            Text("${state.progressPercent}%", color = FactorioOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { state.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = FactorioOrange,
                            trackColor = SlateBorder
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${state.bytesDownloaded / 1024} KB downloaded", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                is SyncState.Validating -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CircuitBlue, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Validating SQLite integrity & foreign key constraints...", color = CircuitBlue, style = MaterialTheme.typography.bodySmall)
                    }
                }
                is SyncState.Applying -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = IndustrialYellow, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Applying atomic database replacement...", color = IndustrialYellow, style = MaterialTheme.typography.bodySmall)
                    }
                }
                is SyncState.Success -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = CircuitGreen.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CircuitGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CircuitGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Update Installed Successfully!", color = CircuitGreen, fontWeight = FontWeight.Bold)
                                Text(state.message, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                is SyncState.Error -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = CircuitRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CircuitRed),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = CircuitRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Update Failed", color = CircuitRed, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(state.message, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                            if (state.rolledBack) {
                                Text("Safeguard active: Database was safely rolled back to previous state.", color = CircuitGreen, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Calculation Assumptions & Formulas
        Text(
            text = "CALCULATION ASSUMPTIONS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = IndustrialYellow,
                letterSpacing = 1.2.sp
            )
        )

        FactoriaCard {
            Text("Linear Programming (Two-Phase Simplex)", fontWeight = FontWeight.Bold, color = FactorioOrange)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Solves exact steady-state material flows using Bland's anti-cycling rule. Handles multi-input/output recipes, cyclic dependencies (Kovarex, oil cracking, recycling loops), and byproduct minimization.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }

        FactoriaCard {
            Text("Quality Tiers (Factorio 2.0)", fontWeight = FontWeight.Bold, color = IndustrialYellow)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Each tier above Normal grants +30% crafting speed to machines and multiplies module bonuses according to official Space Age prototype ratios (Normal 1.0x, Uncommon 1.3x, Rare 1.6x, Epic 1.9x, Legendary 2.2x).",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }

        FactoriaCard {
            Text("Nuclear Power Formulas", fontWeight = FontWeight.Bold, color = CircuitGreen)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Reactor output = 40 MW * (1 + Adjacent Active Reactors). 1 Heat Exchanger = 10 MW (103.09 steam/s at 500°C). 1 Steam Turbine = 5.82 MW (consumes 60 steam/s).",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }

        FactoriaCard {
            Text("Solar / Accumulator Ratio", fontWeight = FontWeight.Bold, color = CircuitBlue)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Calculated based on day-night cycle integration: 0.84 accumulators per solar panel for standard Nauvis surface. Scaled appropriately for Space Age planets (Vulcanus, Fulgora, Gleba, Aquilo).",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }

        // About & Trademark Disclaimer
        Text(
            text = "ABOUT & LEGAL NOTICE",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = IndustrialYellow,
                letterSpacing = 1.2.sp
            )
        )

        FactoriaCard(borderColor = FactorioOrange) {
            Text("TRADEMARK DISCLAIMER", fontWeight = FontWeight.Bold, color = FactorioOrange)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Factoria is an unofficial, fan-made companion app. It is not affiliated with, endorsed, or sponsored by Wube Software.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
