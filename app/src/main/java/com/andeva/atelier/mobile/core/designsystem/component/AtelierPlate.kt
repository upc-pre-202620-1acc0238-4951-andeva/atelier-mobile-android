package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierBlueDark
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierInk
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.PlateShape

/** [L] en fichas y paneles de decisión; [M] en listas densas. */
enum class PlateSize { M, L }

/** `BKL482` → `BKL-482`. Solo el formato de auto (3 + 3, empieza en letra); el resto se muestra tal cual. */
internal fun formatPlate(raw: String): String {
    val plate = raw.trim().uppercase()
    return if (plate.length == 6 && plate.all { it.isLetterOrDigit() } && plate.first().isLetter()) {
        plate.take(3) + "-" + plate.drop(3)
    } else {
        plate
    }
}

/**
 * Insignia automotriz mecanizada: blanca metálica con franja superior peruana `#031A6B`, bisel sutil
 * y texto Satoshi Black `tnum` en `#262626` en ambos temas.
 * TalkBack la deletrea ("placa B K L 4 8 2") en vez de leerla como palabra.
 */
@Composable
fun AtelierPlate(
    plate: String,
    modifier: Modifier = Modifier,
    size: PlateSize = PlateSize.M
) {
    val colors = AtelierTheme.colors
    val text = formatPlate(plate)
    val spoken = stringResource(R.string.plate_a11y, text.filter { it.isLetterOrDigit() }.toList().joinToString(" "))
    val plateGradient = remember(colors.plate, colors.plateBase) {
        Brush.verticalGradient(listOf(colors.plate, colors.plateBase))
    }

    Box(
        modifier = modifier
            .clip(PlateShape)
            .background(plateGradient)
            .drawWithContent {
                drawContent()
                val stripeHeight = 2.5.dp.toPx()
                drawRect(
                    color = AtelierBlueDark,
                    topLeft = Offset.Zero,
                    size = Size(this.size.width, stripeHeight)
                )
            }
            .border(1.dp, AtelierInk.copy(alpha = 0.22f), PlateShape)
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 2.dp)
            .clearAndSetSemantics { contentDescription = spoken },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (size == PlateSize.L) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
            },
            color = AtelierInk,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Preview(name = "Placa · claro", showBackground = true)
@Composable
private fun PlatePreviewLight() = AtelierWorkshopTheme(darkTheme = false) { AtelierPlate("BKL482", Modifier.padding(16.dp)) }

@Preview(name = "Placa · oscuro", showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun PlatePreviewDark() =
    AtelierWorkshopTheme(darkTheme = true) { AtelierPlate("F4T-119", Modifier.padding(16.dp), PlateSize.L) }
