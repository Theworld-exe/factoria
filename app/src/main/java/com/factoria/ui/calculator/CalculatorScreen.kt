package com.factoria.ui.calculator

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factoria.core.designsystem.*
import com.factoria.core.model.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val savedProfiles by viewModel.savedProfiles.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showTargetPicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveProfileName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Saved Profiles Card (if any saved profiles exist)
        if (savedProfiles.isNotEmpty()) {
            FactoriaCard(borderColor = CircuitGreen) {
                Text(
                    text = "SAVED FACTORY PROFILES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CircuitGreen,
                        letterSpacing = 1.2.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedProfiles) { profile ->
                        Surface(
                            color = SlateSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, SlateBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.clickable { viewModel.loadProfile(profile) }
                                ) {
                                    Text(profile.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${profile.target_rate} ${profile.target_id}", color = TextSecondary, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { viewModel.deleteSavedProfile(profile.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CircuitRed, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Target Selection Card
        FactoriaCard(borderColor = FactorioOrange) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRODUCTION TARGET",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = FactorioOrange,
                        letterSpacing = 1.2.sp
                    )
                )
                TextButton(onClick = { showSaveDialog = true }) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = CircuitGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SAVE PROFILE", color = CircuitGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = { showTargetPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SlateSurfaceVariant,
                    contentColor = TextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.selectedTargetId.replace("-", " ").replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold,
                        color = IndustrialYellow
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select target", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.targetRateText,
                    onValueChange = { viewModel.setTargetRate(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Target Rate", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FactorioOrange,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                var unitMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { unitMenuExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(uiState.selectedUnit.label, color = TextPrimary)
                    }
                    DropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false },
                        modifier = Modifier.background(SlateSurface)
                    ) {
                        RateUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.label, color = TextPrimary) },
                                onClick = {
                                    viewModel.setUnit(unit)
                                    unitMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        FactoriaCard {
            Text(
                text = "QUALITY TIER (SPACE AGE 2.0)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = IndustrialYellow,
                    letterSpacing = 1.2.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QualityTier.entries) { quality ->
                    FilterChip(
                        selected = uiState.selectedQuality == quality,
                        onClick = { viewModel.setQuality(quality) },
                        label = { Text(quality.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FactorioOrange,
                            selectedLabelColor = Color.Black,
                            containerColor = SlateSurfaceVariant,
                            labelColor = TextPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "SOLVER OBJECTIVE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = IndustrialYellow,
                    letterSpacing = 1.2.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SolverObjective.entries) { obj ->
                    FilterChip(
                        selected = uiState.selectedObjective == obj,
                        onClick = { viewModel.setObjective(obj) },
                        label = { Text(obj.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CircuitGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = SlateSurfaceVariant,
                            labelColor = TextPrimary
                        )
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.solve() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FactorioOrange),
            shape = RoundedCornerShape(8.dp),
            enabled = !uiState.isSolving
        ) {
            if (uiState.isSolving) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CALCULATE RATIOS (SOLVE)", color = Color.Black, fontWeight = FontWeight.ExtraBold)
            }
        }

        uiState.errorMessage?.let { msg ->
            Surface(
                color = CircuitRed.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CircuitRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    text = msg,
                    color = CircuitRed,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        uiState.solution?.let { sol ->
            if (sol.status == SolverStatus.OPTIMAL) {
                Text(
                    text = "OPTIMIZATION RESULTS (SOLVED IN ${sol.solvingTimeMs}ms)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CircuitGreen,
                        letterSpacing = 1.2.sp
                    )
                )

                val totalMachines = sol.recipeRates.sumOf { it.machinesRequired }
                val roundedTotal = sol.recipeRates.sumOf { it.roundedMachines }
                val powerString = if (sol.totalPowerWatts >= 1_000_000_000) {
                    String.format(Locale.US, "%.2f GW", sol.totalPowerWatts / 1_000_000_000.0)
                } else if (sol.totalPowerWatts >= 1_000_000) {
                    String.format(Locale.US, "%.2f MW", sol.totalPowerWatts / 1_000_000.0)
                } else {
                    String.format(Locale.US, "%.2f kW", sol.totalPowerWatts / 1_000.0)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard("Total Machines", "$roundedTotal", "(${String.format(Locale.US, "%.1f", totalMachines)})", FactorioOrange, Modifier.weight(1f))
                    MetricCard("Power", powerString, "", IndustrialYellow, Modifier.weight(1f))
                    MetricCard("Pollution", String.format(Locale.US, "%.1f", sol.totalPollutionPerMinute), "/min", CircuitRed, Modifier.weight(1f))
                }

                if (sol.catalystStartupBuffers.isNotEmpty()) {
                    FactoriaCard(borderColor = IndustrialYellow) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = IndustrialYellow)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Catalyst Startup Buffers Required:", fontWeight = FontWeight.Bold, color = IndustrialYellow)
                        }
                        sol.catalystStartupBuffers.forEach { (res, count) ->
                            Text("• ${count.toInt()}x $res required before process starts", color = TextPrimary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                FactoriaCard {
                    Text("REQUIRED FACTORY PRODUCTION", fontWeight = FontWeight.Bold, color = IndustrialYellow)
                    Spacer(modifier = Modifier.height(8.dp))

                    sol.recipeRates.forEach { rr ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(rr.recipeName, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    QualityBadge(rr.machineQuality)
                                }
                                Text(
                                    "${rr.roundedMachines}x (${String.format(Locale.US, "%.2f", rr.machinesRequired)})",
                                    color = FactorioOrange,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Rate: ${String.format(Locale.US, "%.2f", rr.craftsPerSecond)} crafts/s",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Machine: ${rr.machineEntityId}",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                FactoriaCard {
                    Text("RAW RESOURCE INPUTS", fontWeight = FontWeight.Bold, color = IndustrialYellow)
                    Spacer(modifier = Modifier.height(8.dp))
                    sol.rawResources.forEach { (res, rate) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(res.replace("-", " ").replaceFirstChar { it.uppercase() }, color = TextPrimary)
                            Text(
                                "${String.format(Locale.US, "%.2f", rate)} /s",
                                color = CircuitBlue,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        val shareText = buildString {
                            appendLine("=== Factoria Production Plan ===")
                            appendLine("Target: ${uiState.selectedTargetId} @ ${uiState.targetRateText} ${uiState.selectedUnit.label}")
                            appendLine("Machines: $roundedTotal (Total Power: $powerString)")
                            appendLine("\nRecipes:")
                            sol.recipeRates.forEach { rr ->
                                appendLine("• ${rr.recipeName}: ${rr.roundedMachines}x ${rr.machineEntityId} (${String.format(Locale.US, "%.2f", rr.craftsPerSecond)}/s)")
                            }
                            appendLine("\nRaw Resources:")
                            sol.rawResources.forEach { (res, rate) ->
                                appendLine("• $res: ${String.format(Locale.US, "%.2f", rate)}/s")
                            }
                            appendLine("\nGenerated completely offline by Factoria")
                        }
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Production Plan"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FactorioOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FactorioOrange)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SHARE / EXPORT PLAN")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showTargetPicker) {
        AlertDialog(
            onDismissRequest = { showTargetPicker = false },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTargetPicker = false }) { Text("CANCEL", color = TextSecondary) }
            },
            title = { Text("Select Production Target", color = FactorioOrange) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                    items(uiState.availableItems, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setTargetId(item.id)
                                    showTargetPicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FactoriaItemIcon(
                                itemId = item.id,
                                itemType = "item",
                                size = 28.dp,
                                modifier = Modifier.semantics {
                                    contentDescription = "Icon for ${item.localizedName}"
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.localizedName,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            containerColor = SlateSurface
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentProfile(saveProfileName)
                        saveProfileName = ""
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CircuitGreen)
                ) {
                    Text("SAVE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("CANCEL", color = TextSecondary) }
            },
            title = { Text("Save Factory Profile", color = CircuitGreen) },
            text = {
                OutlinedTextField(
                    value = saveProfileName,
                    onValueChange = { saveProfileName = it },
                    label = { Text("Profile Name", color = TextSecondary) },
                    placeholder = { Text("${uiState.selectedTargetId.replace("-", " ").replaceFirstChar { it.uppercase() }} Plan", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CircuitGreen,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            containerColor = SlateSurface
        )
    }
}
