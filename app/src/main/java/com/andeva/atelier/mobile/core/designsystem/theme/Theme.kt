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

// Oscuro: el suelo (#262626) y la tarjeta (#272727) casi no se separan a propósito; separa el divisor.
// Los pasos intermedios (barra inferior, hoja) suben de a poco para no depender de sombras.
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
    background = Color(0xFF262626),
    onBackground = Color.White,
    surface = Color(0xFF272727),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFC6C6C6),
    surfaceContainerLowest = Color(0xFF222222),
    surfaceContainerLow = Color(0xFF2B2B2B),
    surfaceContainer = Color(0xFF2E2E2E),
    surfaceContainerHigh = Color(0xFF333333),
    surfaceContainerHighest = Color(0xFF3A3A3A),
    outline = Color(0x66EBEBEB),
    outlineVariant = Color(0x1FEBEBEB),
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
    background = Color(0xFFF8F8FA),
    onBackground = AtelierInk,
    surface = Color.White,
    onSurface = AtelierInk,
    surfaceVariant = Color(0xFFEEF0F4),
    onSurfaceVariant = Color(0xFF454B55),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEF0F4),
    surfaceContainerHighest = Color(0xFFE5E8EE),
    outline = Color(0xFFC6C6C6),
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
