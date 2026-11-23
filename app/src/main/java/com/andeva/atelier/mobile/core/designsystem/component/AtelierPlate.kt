package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
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
 * La placa, firma del sistema: un vehículo se muestra siempre así, primero en la fila.
 * Es un objeto físico: blanca con tinta oscura en claro y en oscuro. Solo para placas.
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
    Text(
        text = text,
        style = if (size == PlateSize.L) MaterialTheme.typography.titleLarge
        else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        color = colors.onPlate,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .clip(PlateShape)
            .background(colors.plate)
            .border(2.dp, colors.onPlate, PlateShape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clearAndSetSemantics { contentDescription = spoken }
    )
}

@Preview(name = "Placa · claro", showBackground = true)
@Composable
private fun PlatePreviewLight() = AtelierWorkshopTheme(darkTheme = false) { AtelierPlate("BKL482", Modifier.padding(16.dp)) }

@Preview(name = "Placa · oscuro", showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun PlatePreviewDark() =
    AtelierWorkshopTheme(darkTheme = true) { AtelierPlate("F4T-119", Modifier.padding(16.dp), PlateSize.L) }
