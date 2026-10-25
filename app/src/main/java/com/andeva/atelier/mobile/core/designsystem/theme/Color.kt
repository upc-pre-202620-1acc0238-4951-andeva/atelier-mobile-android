package com.andeva.atelier.mobile.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Marca (docs/branding/pallette.txt). */
val AtelierBlue = Color(0xFF0071EB)
val AtelierBlueDark = Color(0xFF031A6B)
val AtelierBlueSoft = Color(0xFF69B1FF)
val AtelierAccent = Color(0xFFF68B01)

/**
 * Roles que Material 3 no tiene: tarjetas, estados operativos (ok / espera / error / info / inactivo) y
 * texto secundario. Cada estado trae su color de contenido y su fondo tenue.
 */
@Immutable
data class AtelierColors(
    val card: Color,
    val cardHigh: Color,
    val outlineSoft: Color,
    val inkSecondary: Color,
    val muted: Color,
    val primaryText: Color,
    val ok: Color,
    val okContainer: Color,
    val wait: Color,
    val waitContainer: Color,
    val bad: Color,
    val badContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val idle: Color,
    val idleContainer: Color
)

val DarkAtelierColors = AtelierColors(
    card = Color(0xFF232326),
    cardHigh = Color(0xFF2C2C30),
    outlineSoft = Color(0xFF2A2A2E),
    inkSecondary = Color(0xFFC9CED6),
    muted = Color(0xFF9BA3AF),
    primaryText = Color(0xFF69B1FF),
    ok = Color(0xFF00D756),
    okContainer = Color(0x2400D756),
    wait = Color(0xFFF7B32B),
    waitContainer = Color(0x29F68B01),
    bad = Color(0xFFFF6B76),
    badContainer = Color(0x38D81222),
    info = Color(0xFF69B1FF),
    infoContainer = Color(0x330071EB),
    idle = Color(0xFFB4BCC8),
    idleContainer = Color(0x14FFFFFF)
)

val LightAtelierColors = AtelierColors(
    card = Color(0xFFFFFFFF),
    cardHigh = Color(0xFFEEF0F4),
    outlineSoft = Color(0xFFE5E8EE),
    inkSecondary = Color(0xFF454B55),
    muted = Color(0xFF5B6676),
    primaryText = Color(0xFF005DC2),
    ok = Color(0xFF00702B),
    okContainer = Color(0xFFD9F7E4),
    wait = Color(0xFF9A5300),
    waitContainer = Color(0xFFFFF0D9),
    bad = Color(0xFFB3101D),
    badContainer = Color(0xFFFDE3E5),
    info = Color(0xFF005DC2),
    infoContainer = Color(0xFFDCEBFF),
    idle = Color(0xFF4B5361),
    idleContainer = Color(0xFFECEEF2)
)
