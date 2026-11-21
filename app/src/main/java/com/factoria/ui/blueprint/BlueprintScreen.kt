package com.factoria.ui.blueprint

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.factoria.core.blueprint.SampleBlueprints
import com.factoria.core.designsystem.*
import com.factoria.core.model.*

@Composable
fun BlueprintScreen(
    viewModel: BlueprintViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Import Card
        FactoriaCard(borderColor = CircuitGreen) {
            Text(
                text = "IMPORT BLUEPRINT STRING",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CircuitGreen,
                    letterSpacing = 1.2.sp
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = uiState.blueprintStringInput,
                onValueChange = { viewModel.onInputChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("Paste '0eN...' blueprint string here", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CircuitGreen,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipData = clipboardManager.getText()?.text
                        if (!clipData.isNullOrBlank()) {
                            viewModel.onInputChanged(clipData)
                            viewModel.decodeBlueprint(clipData)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.loadSampleBlueprint() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IndustrialYellow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IndustrialYellow.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sample BP", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.decodeBlueprint() },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CircuitGreen),
                    enabled = !uiState.isDecoding
                ) {
                    if (uiState.isDecoding) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                    } else {
                        Text("DECODE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        uiState.errorMessage?.let { error ->
            Surface(
                color = CircuitRed.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CircuitRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    color = CircuitRed,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Blueprint details & BOM
        uiState.activeBlueprint?.let { bp ->
            FactoriaCard(borderColor = FactorioOrange) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = bp.label ?: "Unnamed Blueprint",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        bp.description?.let { desc ->
                            Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        }
                    }
                    Surface(
                        color = FactorioOrange.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "v${bp.version ?: 2}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = FactorioOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard("Entities", "${bp.entities.size}", "", FactorioOrange, Modifier.weight(1f))
                    MetricCard("Tiles", "${bp.tiles.size}", "", IndustrialYellow, Modifier.weight(1f))
                }
            }

            // Bill of Materials (BOM)
            uiState.bom?.let { bom ->
                FactoriaCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "BILL OF MATERIALS (BOM)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = IndustrialYellow,
                                letterSpacing = 1.2.sp
                            )
                        )
                        IconButton(onClick = {
                            val shareText = buildString {
                                appendLine("=== Blueprint Bill of Materials ===")
                                appendLine("Blueprint: ${bp.label ?: "Unnamed"}")
                                appendLine("\nEntities:")
                                bom.entityCounts.forEach { (name, count) ->
                                    appendLine("• $count x $name")
                                }
                                if (bom.moduleCounts.isNotEmpty()) {
                                    appendLine("\nModules:")
                                    bom.moduleCounts.forEach { (name, count) ->
                                        appendLine("• $count x $name")
                                    }
                                }
                                if (bom.tileCounts.isNotEmpty()) {
                                    appendLine("\nTiles:")
                                    bom.tileCounts.forEach { (name, count) ->
                                        appendLine("• $count x $name")
                                    }
                                }
                                appendLine("\nExported from Factoria (Offline)")
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Bill of Materials"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share BOM", tint = FactorioOrange)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("ENTITIES", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                    bom.entityCounts.forEach { (name, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name.replace("-", " ").replaceFirstChar { it.uppercase() }, color = TextPrimary)
                            Text("$count", fontWeight = FontWeight.Bold, color = FactorioOrange, fontFamily = FontFamily.Monospace)
                        }
                    }

                    if (bom.moduleCounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("MODULES", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                        bom.moduleCounts.forEach { (name, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name.replace("-", " ").replaceFirstChar { it.uppercase() }, color = TextPrimary)
                                Text("$count", fontWeight = FontWeight.Bold, color = CircuitBlue, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    if (bom.tileCounts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("TILES / CONCRETE", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontWeight = FontWeight.Bold))
                        bom.tileCounts.forEach { (name, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(name.replace("-", " ").replaceFirstChar { it.uppercase() }, color = TextPrimary)
                                Text("$count", fontWeight = FontWeight.Bold, color = IndustrialYellow, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    if (bom.unknownEntities.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = CircuitRed.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CircuitRed),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = CircuitRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unrecognized / Modded Entities:", color = CircuitRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                bom.unknownEntities.forEach { u ->
                                    Text("• $u", color = TextPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        PreMadeBlueprintLibrary(
            onInspect = { bpString -> viewModel.inspectPresetBlueprint(bpString) }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PreMadeBlueprintLibrary(
    onInspect: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val sampleBlueprints = SampleBlueprints.library

    FactoriaCard(borderColor = IndustrialYellow) {
        Text(
            text = "PRE-MADE FACTORIO 2.0 BLUEPRINT LIBRARY",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = IndustrialYellow,
                letterSpacing = 1.2.sp
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tap a blueprint to inspect its BOM or copy its compressed 2.0 string to clipboard",
            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
        )
        Spacer(modifier = Modifier.height(12.dp))

        sampleBlueprints.forEach { entry ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        FactoriaSoundManager.playClick()
                        onInspect(entry.blueprintString)
                    }
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.title, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(entry.description, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    }
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Blueprint String", entry.blueprintString)
                            clipboard.setPrimaryClip(clip)
                            FactoriaSoundManager.playClick()
                            Toast.makeText(context, "Copied blueprint string to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FactorioOrange),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("COPY", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }
            }
            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f), modifier = Modifier.padding(top = 6.dp))
        }
    }
}
