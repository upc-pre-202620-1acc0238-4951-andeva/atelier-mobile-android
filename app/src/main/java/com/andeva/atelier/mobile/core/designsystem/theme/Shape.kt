package com.andeva.atelier.mobile.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Tres radios y nada más (DESIGN.md → Shapes). Sin píldoras, salvo la excepción de [FloatingBarShape]. */
val PlateShape = RoundedCornerShape(6.dp)
val ControlShape = RoundedCornerShape(12.dp)
val PanelShape = RoundedCornerShape(16.dp)

/** Única excepción a "sin píldoras": la barra inferior flotante y su botón de acción (DESIGN.md → Barra flotante). */
val FloatingBarShape = RoundedCornerShape(percent = 50)

val CardShape = ControlShape
val FieldShape = ControlShape

/** Marca de estado: a 28 dp de alto, 12 dp de radio se leería como píldora. */
val ChipShape = PlateShape

// extraLarge queda en el valor de Material 3: lo usan la hoja inferior y los diálogos.
val AtelierShapes = Shapes(
    extraSmall = PlateShape,
    small = ControlShape,
    medium = ControlShape,
    large = PanelShape
)

/** Módulo de 8 dp; 4 solo dentro de un componente. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}
