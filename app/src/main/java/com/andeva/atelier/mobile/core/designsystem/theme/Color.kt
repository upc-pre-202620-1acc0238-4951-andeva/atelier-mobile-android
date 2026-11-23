package com.andeva.atelier.mobile.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Marca (docs/branding/pallette.txt). Cada color dice una sola cosa: DESIGN.md → La Regla del Código Vial. */
val AtelierBlue = Color(0xFF0071EB)
val AtelierBlueDark = Color(0xFF031A6B)
val AtelierBlueSoft = Color(0xFF69B1FF)
val AtelierAccent = Color(0xFFF68B01)
val AtelierAmberDark = Color(0xFFD87900)
val AtelierAmberSoft = Color(0xFFFFCD69)
val AtelierSuccess = Color(0xFF3AC530)
val AtelierError = Color(0xFFFB2C36)
val AtelierWarning = Color(0xFFF0B100)
val AtelierInk = Color(0xFF262626)

/**
 * Roles que Material 3 no tiene. Los estados traen su tinta (texto, pasa contraste) y su fondo tenue;
 * el tono de marca va solo en marcas y rellenos (`*Mark`).
 *
 * [panel], [decision] y [plate] valen lo mismo en claro y en oscuro: una señal no cambia de color de noche.
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
    val idleContainer: Color,
    val warn: Color,
    val warnContainer: Color,
    val fab: Color,
    val onFab: Color,
    val bone: Color,
    /** Panel de cabecera: Azul Señal con tinta blanca. */
    val panel: Color = AtelierBlue,
    val onPanel: Color = Color.White,
    val boneOnPanel: Color = Color(0x3DFFFFFF),
    /** Panel de decisión: ámbar con Tinta. Solo para lo que espera a una persona. */
    val decision: Color = AtelierAccent,
    val decisionPressed: Color = AtelierAmberDark,
    val decisionSoft: Color = AtelierAmberSoft,
    val onDecision: Color = AtelierInk,
    /** La placa: objeto físico, blanca con tinta oscura en ambos temas. */
    val plate: Color = Color.White,
    val onPlate: Color = AtelierInk,
    val okMark: Color = AtelierSuccess,
    val badMark: Color = AtelierError,
    val warnMark: Color = AtelierWarning
)

val DarkAtelierColors = AtelierColors(
    card = Color(0xFF272727),
    cardHigh = Color(0xFF333333),
    outlineSoft = Color(0x1FEBEBEB),
    inkSecondary = Color(0xFFC6C6C6),
    muted = Color(0xFFA3A3A3),
    primaryText = AtelierBlueSoft,
    ok = AtelierSuccess,
    okContainer = Color(0x293AC530),
    wait = AtelierAmberSoft,
    waitContainer = Color(0x33F68B01),
    bad = Color(0xFFFF6B76),
    badContainer = Color(0x38FB2C36),
    info = AtelierBlueSoft,
    infoContainer = Color(0x3D0071EB),
    idle = Color(0xFFC6C6C6),
    idleContainer = Color(0x14FFFFFF),
    warn = AtelierWarning,
    warnContainer = Color(0x29F0B100),
    fab = AtelierBlueSoft,
    onFab = AtelierBlueDark,
    bone = Color(0x1FFFFFFF)
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
    idleContainer = Color(0xFFECEEF2),
    warn = Color(0xFF9A5300),
    warnContainer = Color(0xFFFFF4CC),
    fab = AtelierBlueDark,
    onFab = Color.White,
    bone = Color(0x14262626)
)
