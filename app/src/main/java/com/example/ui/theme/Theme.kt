package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AetherisColorScheme = darkColorScheme(
    primary = AetherCyan,
    onPrimary = ObsidianBg,
    primaryContainer = AetherCyanContainer,
    onPrimaryContainer = TextPrimary,
    secondary = SolarGold,
    onSecondary = ObsidianBg,
    secondaryContainer = SolarGoldContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = ArcaneViolet,
    onTertiary = TextPrimary,
    tertiaryContainer = ArcaneVioletContainer,
    onTertiaryContainer = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianOutline,
    error = CrimsonDanger,
    onError = TextPrimary
)

private val AetherisShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AetherisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AetherisColorScheme,
        typography = Typography,
        shapes = AetherisShapes,
        content = content
    )
}
