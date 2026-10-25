package com.andeva.atelier.mobile.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AtelierBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0B2F66),
    onPrimaryContainer = Color(0xFFD3E6FF),
    secondary = AtelierBlueSoft,
    onSecondary = Color(0xFF031A6B),
    tertiary = AtelierAccent,
    background = Color(0xFF141416),
    onBackground = Color.White,
    surface = Color(0xFF1C1C1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF232326),
    onSurfaceVariant = Color(0xFFC9CED6),
    surfaceContainer = Color(0xFF232326),
    surfaceContainerHigh = Color(0xFF2C2C30),
    outline = Color(0xFF3A3A3F),
    outlineVariant = Color(0xFF2A2A2E),
    error = Color(0xFFFF6B76),
    onError = Color(0xFF141416),
    errorContainer = Color(0xFF4A1A20),
    onErrorContainer = Color.White,
    scrim = Color(0x99000000)
)

private val LightColorScheme = lightColorScheme(
    primary = AtelierBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E8FF),
    onPrimaryContainer = Color(0xFF031A6B),
    secondary = Color(0xFF005DC2),
    onSecondary = Color.White,
    tertiary = Color(0xFF9A5300),
    background = Color(0xFFF8F8FA),
    onBackground = Color(0xFF262626),
    surface = Color.White,
    onSurface = Color(0xFF262626),
    surfaceVariant = Color(0xFFEEF0F4),
    onSurfaceVariant = Color(0xFF454B55),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEF0F4),
    outline = Color(0xFFC6C6C6),
    outlineVariant = Color(0xFFE5E8EE),
    error = Color(0xFFB3101D),
    onError = Color.White,
    errorContainer = Color(0xFFFDE3E5),
    onErrorContainer = Color(0xFF262626),
    scrim = Color(0x73141416)
)

private val LocalAtelierColors = staticCompositionLocalOf { DarkAtelierColors }

/** Acceso a los roles propios: `AtelierTheme.colors.ok`, `AtelierTheme.colors.muted`… */
object AtelierTheme {
    val colors: AtelierColors
        @Composable @ReadOnlyComposable get() = LocalAtelierColors.current
}

/**
 * Tema de Atelier. Sin color dinámico a propósito: la paleta de marca es fija (docs/branding).
 * Oscuro y claro son esquemas completos, no una inversión.
 */
@Composable
fun AtelierWorkshopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAtelierColors provides if (darkTheme) DarkAtelierColors else LightAtelierColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            shapes = AtelierShapes,
            content = content
        )
    }
}
