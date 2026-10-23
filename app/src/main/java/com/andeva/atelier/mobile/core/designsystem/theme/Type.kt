<<<<<<<< HEAD:app/src/main/java/tech/andeva/atelier/atelier_workshop/core/data/designsystem/theme/Type.kt
package tech.andeva.atelier.atelier_workshop.core.data.designsystem.theme
========
package com.andeva.atelier.mobile.core.designsystem.theme
>>>>>>>> e73cb05 (chore: setup dependencies and package structure):app/src/main/java/com/andeva/atelier/mobile/core/designsystem/theme/Type.kt

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)