package com.andeva.atelier.mobile.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Una sola familia de formas: botones píldora, tarjetas y campos 16 dp, chips 8 dp. */
val CardShape = RoundedCornerShape(16.dp)
val FieldShape = RoundedCornerShape(16.dp)
val ChipShape = RoundedCornerShape(8.dp)
val PillShape = CircleShape

val AtelierShapes = Shapes(
    extraSmall = ChipShape,
    small = ChipShape,
    medium = CardShape,
    large = CardShape,
    extraLarge = CardShape
)
