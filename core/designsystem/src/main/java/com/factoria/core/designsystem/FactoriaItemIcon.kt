package com.factoria.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun FactoriaItemIcon(
    itemId: String,
    itemType: String = "item",
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val id = itemId.lowercase(Locale.ROOT)

    // Palette mapping per item category
    val (bgColor, accentColor, badgeText) = when {
        id.contains("circuit") || id == "processing-unit" -> when {
            id.contains("advanced") -> Triple(Color(0xFF2C1010), Color(0xFFE53935), "IC")
            id.contains("processing") -> Triple(Color(0xFF0F1E36), Color(0xFF1E88E5), "PU")
            else -> Triple(Color(0xFF102A16), Color(0xFF4CAF50), "EC")
        }
        id.contains("science") -> when {
            id.contains("automation") -> Triple(Color(0xFF330E0E), Color(0xFFE53935), "SP")
            id.contains("logistic") -> Triple(Color(0xFF0E3314), Color(0xFF4CAF50), "SP")
            id.contains("chemical") -> Triple(Color(0xFF0E2733), Color(0xFF00ACC1), "SP")
            id.contains("military") -> Triple(Color(0xFF261833), Color(0xFF8E24AA), "SP")
            id.contains("production") -> Triple(Color(0xFF330E29), Color(0xFFD81B60), "SP")
            id.contains("utility") -> Triple(Color(0xFF332B0E), Color(0xFFFFB300), "SP")
            id.contains("space") -> Triple(Color(0xFF333333), Color(0xFFE0E0E0), "SP")
            else -> Triple(Color(0xFF1A1A2E), Color(0xFF805AD5), "SP")
        }
        id.contains("plate") -> when {
            id.contains("iron") -> Triple(Color(0xFF20262E), Color(0xFF90A4AE), "FE")
            id.contains("copper") -> Triple(Color(0xFF331C0E), Color(0xFFFB8C00), "CU")
            id.contains("steel") -> Triple(Color(0xFF1C2024), Color(0xFFCFD8DC), "ST")
            else -> Triple(Color(0xFF26231D), Color(0xFFFFB74D), "PL")
        }
        id.contains("ore") || id == "coal" || id == "stone" -> when {
            id.contains("iron") -> Triple(Color(0xFF1C222A), Color(0xFF64B5F6), "ORE")
            id.contains("copper") -> Triple(Color(0xFF2E1C12), Color(0xFFFF9800), "ORE")
            id.contains("coal") -> Triple(Color(0xFF121212), Color(0xFF757575), "COAL")
            id.contains("uranium") -> Triple(Color(0xFF0B2E13), Color(0xFF00E676), "U")
            else -> Triple(Color(0xFF232323), Color(0xFFB0BEC5), "ORE")
        }
        itemType == "fluid" || id.contains("oil") || id.contains("water") || id.contains("acid") || id == "steam" || id == "lava" -> when {
            id.contains("water") -> Triple(Color(0xFF0B2336), Color(0xFF29B6F6), "H2O")
            id.contains("steam") -> Triple(Color(0xFF262E33), Color(0xFFECEFF1), "STM")
            id.contains("crude") -> Triple(Color(0xFF121212), Color(0xFF424242), "OIL")
            id.contains("petroleum") -> Triple(Color(0xFF33240E), Color(0xFFFFA726), "GAS")
            id.contains("acid") -> Triple(Color(0xFF1C330E), Color(0xFF76FF03), "ACID")
            id.contains("lava") -> Triple(Color(0xFF3B0F00), Color(0xFFFF3D00), "MAG")
            else -> Triple(Color(0xFF0D2830), Color(0xFF26C6DA), "FLD")
        }
        id.contains("machine") || id.contains("assembler") -> Triple(Color(0xFF261D12), FactorioOrange, "ASM")
        id.contains("furnace") -> Triple(Color(0xFF261D12), IndustrialYellow, "FURN")
        id.contains("chemical") || id.contains("plant") -> Triple(Color(0xFF0E2733), CircuitBlue, "CHEM")
        id.contains("foundry") -> Triple(Color(0xFF3B0F00), FactorioOrange, "FOUND")
        id == "biochamber" -> Triple(Color(0xFF0E3314), CircuitGreen, "BIO")
        id == "recycler" -> Triple(Color(0xFF20262E), QualityEpicColor, "RECY")
        itemType == "technology" -> Triple(Color(0xFF2B1033), QualityEpicColor, "TECH")
        else -> Triple(SlateSurfaceVariant, FactorioOrange, id.take(3).uppercase())
    }

    Surface(
        modifier = modifier
            .size(size)
            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height

                when {
                    id.contains("circuit") -> {
                        drawRect(color = accentColor.copy(alpha = 0.3f), topLeft = Offset(w * 0.2f, h * 0.2f), size = Size(w * 0.6f, h * 0.6f))
                        drawRect(color = accentColor, topLeft = Offset(w * 0.35f, h * 0.35f), size = Size(w * 0.3f, h * 0.3f))
                    }
                    id.contains("science") -> {
                        drawCircle(color = accentColor, radius = w * 0.25f, center = Offset(w * 0.5f, h * 0.6f))
                        drawRect(color = accentColor, topLeft = Offset(w * 0.42f, h * 0.2f), size = Size(w * 0.16f, h * 0.25f))
                    }
                    id.contains("plate") -> {
                        drawRect(color = accentColor, topLeft = Offset(w * 0.2f, h * 0.35f), size = Size(w * 0.6f, h * 0.3f))
                    }
                    itemType == "fluid" || id.contains("oil") || id.contains("water") -> {
                        drawCircle(color = accentColor, radius = w * 0.3f, center = Offset(w * 0.5f, h * 0.5f))
                    }
                    else -> {
                        drawCircle(color = accentColor.copy(alpha = 0.2f), radius = w * 0.35f, center = Offset(w * 0.5f, h * 0.5f))
                    }
                }
            }

            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (size < 36.dp) 8.sp else 10.sp,
                    color = Color.White
                )
            )
        }
    }
}
