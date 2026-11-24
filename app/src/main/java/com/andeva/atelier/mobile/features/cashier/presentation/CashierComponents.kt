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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.presentation.DocumentIntents
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateTimeLima = DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.forLanguageTag("es-PE"))
    .withZone(BusinessTime.ZONE)

/** `16/11 14:35`, hora de Lima. */
internal fun Instant.formatLimaDateTime(): String = DateTimeLima.format(this)

/** `B001-00000045` con correlativo a 8 dígitos. */
internal fun formatVoucherCode(serie: String, number: Int): String =
    "$serie-${number.toString().padStart(8, '0')}"

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

/** Abre o comparte el PDF descargado en `cacheDir/pdfs` mediante [DocumentIntents]. */
internal fun shareOrOpenPdf(context: Context, pdfFile: File, chooserTitle: String): Boolean =
    DocumentIntents.openPdf(context, pdfFile, chooserTitle)

/** Contenido reutilizable de la hoja de anulación de comprobante. */
@Composable
internal fun VoidSheetContent(
    form: VoidForm,
    onReasonChange: (String) -> Unit,
    error: String?
) {
    val reasonTooLong = form.reason.trim().length > 250
    Text(
        text = stringResource(R.string.cashier_void_body),
        style = MaterialTheme.typography.bodyLarge,
        color = AtelierTheme.colors.inkSecondary
    )
    AtelierTextField(
        label = stringResource(R.string.cashier_void_reason_label),
        value = form.reason,
        onValueChange = onReasonChange,
        placeholder = stringResource(R.string.cashier_void_reason_placeholder),
        isError = form.reason.isNotEmpty() && !form.reasonValid || reasonTooLong,
        errorText = stringResource(R.string.cashier_void_reason_error).takeIf {
            (form.reason.isNotEmpty() && !form.reasonValid) || reasonTooLong
        }
    )
    error?.let { AtelierBanner(message = it, icon = Icons.Outlined.ErrorOutline) }
}

/** Contenido reutilizable de la hoja de emisión de nota de crédito. */
@Composable
internal fun CreditNoteSheetContent(
    form: CreditNoteForm,
    onReasonChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    error: String?,
    onSerieChange: (String) -> Unit = {}
) {
    val amountInvalid = form.amount.isNotBlank() && form.parsedAmount == null
    Text(
        text = stringResource(R.string.cashier_nc_reference, form.referenceLabel),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    if (form.availableSeries.size > 1) {
        Text(
            text = stringResource(R.string.cashier_nc_serie_label),
            style = MaterialTheme.typography.titleSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        ChoiceGrid(
            options = form.availableSeries.map { it.serie },
            selected = form.selectedSerie.ifBlank { form.availableSeries.first().serie },
            label = { it },
            onSelect = onSerieChange,
            columns = 3
        )
    }
    AtelierTextField(
        label = stringResource(R.string.cashier_nc_reason_label),
        value = form.reason,
        onValueChange = onReasonChange,
        placeholder = stringResource(R.string.cashier_nc_reason_placeholder),
        isError = form.reason.isNotEmpty() && !form.reasonValid,
        errorText = stringResource(R.string.cashier_nc_reason_error).takeIf { form.reason.isNotEmpty() && !form.reasonValid }
    )
    AtelierTextField(
        label = stringResource(R.string.cashier_nc_amount_label),
        value = form.amount,
        onValueChange = onAmountChange,
        isError = amountInvalid,
        errorText = stringResource(R.string.cashier_nc_amount_error).takeIf { amountInvalid },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
    Text(
        text = stringResource(R.string.cashier_nc_irreversible),
        style = MaterialTheme.typography.bodyMedium,
        color = AtelierTheme.colors.inkSecondary
    )
    error?.let { AtelierBanner(message = it, icon = Icons.Outlined.ErrorOutline) }
}
