package com.andeva.atelier.mobile.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.andeva.atelier.mobile.R

/** Interfaz: Plus Jakarta Sans, la misma de la galería de diseño. */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

/** Solo para datos: códigos, contadores, kilometrajes. */
val JetBrainsMono = FontFamily(Font(R.font.jetbrains_mono_medium, FontWeight.Medium))

/** Solo para el texto del imagotipo (docs/branding/design-system.md §3). */
val AlbertSans = FontFamily(Font(R.font.albert_sans_extrabold, FontWeight.ExtraBold))

/**
 * Escala de Material 3 con los pesos de Atelier. Nombres del diseño → rol:
 * display → headlineLarge, headline → headlineMedium, title-l/m → titleLarge/titleMedium,
 * etiqueta de campo → titleSmall, body-l/m → bodyLarge/bodyMedium, label-l/m → labelLarge/labelMedium.
 */
val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.64).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.48).sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.12.sp
    )
)
