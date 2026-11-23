package com.andeva.atelier.mobile.features.cashier.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.domain.BusinessTime
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateTimeLima = DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.forLanguageTag("es-PE"))
    .withZone(BusinessTime.ZONE)

/** `16/11 14:35`, hora de Lima. */
internal fun Instant.formatLimaDateTime(): String = DateTimeLima.format(this)

/** Fila de cifras: rótulo a la izquierda, monto a la derecha. [emphasized] para el total. */
@Composable
internal fun AmountRow(label: String, value: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (emphasized) MaterialTheme.colorScheme.onBackground else AtelierTheme.colors.inkSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/** Elegir una opción entre pocas, en filas de [columns]: la elegida va en tonal, las demás en contorno. */
@Composable
internal fun <T> ChoiceGrid(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(columns).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    val isSelected = option == selected
                    AtelierButton(
                        text = label(option),
                        onClick = { onSelect(option) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { this.selected = isSelected },
                        style = if (isSelected) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
                        enabled = enabled
                    )
                }
                repeat(columns - row.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** Abre el menú de compartir con el PDF descargado en `cacheDir/pdfs`; false si no hay app que lo reciba. */
internal fun shareOrOpenPdf(context: Context, pdfFile: File, chooserTitle: String): Boolean = try {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: IllegalArgumentException) {
    false
}
