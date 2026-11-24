package com.andeva.atelier.mobile.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.andeva.atelier.mobile.R

/** Toda la interfaz. Satoshi trae 400, 500, 700 y 900: no hay 600 ni 800 (DESIGN.md → Typography). */
val Satoshi = FontFamily(
    Font(R.font.satoshi_regular, FontWeight.Normal),
    Font(R.font.satoshi_medium, FontWeight.Medium),
    Font(R.font.satoshi_bold, FontWeight.Bold),
    Font(R.font.satoshi_black, FontWeight.Black)
)

/** Solo para el texto del imagotipo (DESIGN.md → Typography). */
val AlbertSans = FontFamily(Font(R.font.albert_sans_extrabold, FontWeight.ExtraBold))

// Cifras tabulares en todo: placas, montos y cronómetros se alinean sin fuente mono.
private fun satoshi(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Satoshi,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = "tnum"
)

/**
 * Escala de DESIGN.md sobre los roles de Material 3. `displaySmall` es la leyenda de señal: una por pantalla.
 * Nada baja de 12sp.
 */
val Typography = Typography(
    displayLarge = satoshi(FontWeight.Black, 48, 48, -0.02),
    displayMedium = satoshi(FontWeight.Black, 48, 48, -0.02),
    displaySmall = satoshi(FontWeight.Black, 48, 48, -0.02),
    headlineLarge = satoshi(FontWeight.Black, 32, 40, -0.01),
    headlineMedium = satoshi(FontWeight.Black, 28, 32, -0.01),
    headlineSmall = satoshi(FontWeight.Black, 24, 32, -0.01),
    titleLarge = satoshi(FontWeight.Black, 22, 28),
    titleMedium = satoshi(FontWeight.Bold, 16, 24),
    titleSmall = satoshi(FontWeight.Bold, 14, 20),
    bodyLarge = satoshi(FontWeight.Normal, 16, 24),
    bodyMedium = satoshi(FontWeight.Normal, 14, 20),
    bodySmall = satoshi(FontWeight.Normal, 12, 16),
    labelLarge = satoshi(FontWeight.Bold, 14, 20),
    labelMedium = satoshi(FontWeight.Medium, 12, 16, 0.01),
    labelSmall = satoshi(FontWeight.Medium, 12, 16, 0.01)
)
