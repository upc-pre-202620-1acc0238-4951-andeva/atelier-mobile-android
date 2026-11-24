package com.andeva.atelier.mobile.core.designsystem.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Oscuro: planos calibrados («Precisión Automotriz») desde el pozo hundido (#1C1D20) al plano flotante (#33363C),
// preservando el suelo (#262626) y la tarjeta base (#272727) con contraste WCAG 2.2 AA en toda la escala.
private val DarkColorScheme = darkColorScheme(
    primary = AtelierBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1D3A5F),
    onPrimaryContainer = AtelierBlueSoft,
    secondary = AtelierBlueSoft,
    onSecondary = AtelierBlueDark,
    secondaryContainer = Color(0xFF1D3A5F),
    onSecondaryContainer = AtelierBlueSoft,
    tertiary = AtelierAccent,
    onTertiary = AtelierInk,
    background = AtelierSurfaceDark,
    onBackground = Color.White,
    surface = AtelierCardDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2F3136),
    onSurfaceVariant = AtelierBorderLight,
    surfaceContainerLowest = Color(0xFF1C1D20),
    surfaceContainerLow = AtelierCardDark,
    surfaceContainer = Color(0xFF2F3136),
    surfaceContainerHigh = Color(0xFF33363C),
    surfaceContainerHighest = Color(0xFF3B3E45),
    outline = AtelierBorderDark.copy(alpha = 0.40f),
    outlineVariant = AtelierBorderDark.copy(alpha = 0.12f),
    error = Color(0xFFFF6B76),
    onError = AtelierInk,
    errorContainer = Color(0xFF4A1A20),
    onErrorContainer = Color.White,
    scrim = Color(0x99000000)
)

private val LightColorScheme = lightColorScheme(
    primary = AtelierBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E8FF),
    onPrimaryContainer = AtelierBlueDark,
    secondary = Color(0xFF005DC2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E8FF),
    onSecondaryContainer = AtelierBlueDark,
    tertiary = Color(0xFF9A5300),
    onTertiary = Color.White,
    background = AtelierSurfaceLight,
    onBackground = AtelierInk,
    surface = AtelierCardLight,
    onSurface = AtelierInk,
    surfaceVariant = Color(0xFFECEFF4),
    onSurfaceVariant = Color(0xFF454B55),
    surfaceContainerLowest = AtelierCardLight,
    surfaceContainerLow = AtelierSurfaceLight,
    surfaceContainer = Color(0xFFF2F4F8),
    surfaceContainerHigh = Color(0xFFECEFF4),
    surfaceContainerHighest = Color(0xFFE4E8EF),
    outline = AtelierBorderLight,
    outlineVariant = Color(0xFFE5E8EE),
    error = Color(0xFFB3101D),
    onError = Color.White,
    errorContainer = Color(0xFFFDE3E5),
    onErrorContainer = AtelierInk,
    scrim = Color(0x73141416)
)

private val LocalAtelierColors = staticCompositionLocalOf { DarkAtelierColors }

/** Acceso a los roles propios: `AtelierTheme.colors.ok`, `AtelierTheme.colors.muted`… */
object AtelierTheme {
    val colors: AtelierColors
        @Composable @ReadOnlyComposable get() = LocalAtelierColors.current
}

// Cuántos paneles o barras azules tocan la barra de estado ahora mismo. Un contador y no un booleano:
// al navegar, la pantalla nueva entra antes de que la anterior salga.
private var bluePanelsOnScreen by mutableIntStateOf(0)

/** Mientras está en composición, los íconos de la barra de estado van en claro (hay azul debajo). */
@Composable
fun PanelStatusBarEffect() {
    DisposableEffect(Unit) {
        bluePanelsOnScreen++
        onDispose { bluePanelsOnScreen-- }
    }
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
    val view = LocalView.current
    val lightStatusIcons = !darkTheme && bluePanelsOnScreen == 0
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightStatusIcons
            }
        }
    }
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
