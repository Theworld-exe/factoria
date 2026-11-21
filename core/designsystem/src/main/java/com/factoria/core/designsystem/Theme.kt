package com.factoria.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FactoriaColorScheme = darkColorScheme(
    primary = FactorioOrange,
    onPrimary = Color.Black,
    primaryContainer = FactorioOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = IndustrialYellow,
    onSecondary = Color.Black,
    secondaryContainer = SlateSurfaceVariant,
    onSecondaryContainer = IndustrialYellow,
    tertiary = CircuitGreen,
    onTertiary = Color.Black,
    background = SlateBackground,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SlateBorder,
    error = CircuitRed,
    onError = Color.White
)

@Composable
fun FactoriaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FactoriaColorScheme,
        content = content
    )
}
