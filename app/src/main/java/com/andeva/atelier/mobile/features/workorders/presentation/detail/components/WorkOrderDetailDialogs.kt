package com.andeva.atelier.mobile.features.workorders.presentation.detail.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSelectionCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.presentation.detail.AddTaskForm
import com.andeva.atelier.mobile.features.workorders.presentation.detail.CascadeBreakdown
import com.andeva.atelier.mobile.features.workorders.presentation.detail.EditLaborForm
import com.andeva.atelier.mobile.features.workorders.presentation.detail.RejectQuotationForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** US22 E1: agregar una nueva tarea con su servicio y precio de mano de obra sin IGV. */
@Composable
internal fun AddTaskDialog(
    form: AddTaskForm,
    services: List<ServiceOption>,
    mechanics: List<MechanicOption> = emptyList(),
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onSelectService: (ServiceOption) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onSelectMechanic: (String?) -> Unit = {},
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AtelierButton(
                text = stringResource(R.string.order_add_task_confirm),
                onClick = onConfirm,
                enabled = form.serviceId != null && form.description.isNotBlank() && !isSaving,
                loading = isSaving
            )
        },
        dismissButton = {
            AtelierButton(
                text = stringResource(R.string.proposals_cancel),
                onClick = onDismiss,
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        },
        title = { Text(stringResource(R.string.order_add_task_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.order_add_task_service),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                services.forEach { service ->
                    val selected = service.id == form.serviceId
                    AtelierSelectionCard(
                        title = service.name,
                        price = service.basePrice?.format(),
                        selected = selected,
                        enabled = !isSaving,
                        onClick = { onSelectService(service) }
                    )
                }
                if (invalidField == "serviceId") {
                    Text(
                        text = stringResource(R.string.order_field_service),
                        style = MaterialTheme.typography.bodySmall,
                        color = AtelierTheme.colors.bad
                    )
                }
                AtelierTextField(
                    label = stringResource(R.string.order_add_task_description),
                    value = form.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = invalidField == "description",
                    errorText = if (invalidField == "description") stringResource(R.string.order_field_description) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                AtelierTextField(
                    label = stringResource(R.string.order_add_task_price),
                    value = form.price,
                    onValueChange = onPriceChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = invalidField == "price",
                    errorText = if (invalidField == "price") stringResource(R.string.order_field_price) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                )
                if (mechanics.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.order_add_task_mechanic),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AtelierSelectionCard(
                        title = stringResource(R.string.open_order_mechanic_none),
                        selected = form.mechanicId == null,
                        enabled = !isSaving,
                        onClick = { onSelectMechanic(null) }
                    )
                    mechanics.forEach { mechanic ->
                        val selected = mechanic.membershipId == form.mechanicId
                        val subtitle = if (mechanic.name != null && !mechanic.jobTitle.isNullOrBlank()) mechanic.jobTitle else null
                        AtelierSelectionCard(
                            title = mechanic.displayName,
                            subtitle = subtitle,
                            leadingIcon = Icons.Outlined.Person,
                            selected = selected,
                            enabled = !isSaving,
                            onClick = { onSelectMechanic(mechanic.membershipId) }
                        )
                    }
                }
                error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
            }
        }
    )
}

/** US22 E2: actualizar el precio de mano de obra sin IGV de una tarea existente. */
@Composable
internal fun EditLaborDialog(
    form: EditLaborForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onPriceChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AtelierButton(
                text = stringResource(R.string.order_edit_labor_confirm),
                onClick = onConfirm,
                enabled = form.price.isNotBlank() && !isSaving,
                loading = isSaving
            )
        },
        dismissButton = {
            AtelierButton(
                text = stringResource(R.string.proposals_cancel),
                onClick = onDismiss,
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        },
        title = { Text(stringResource(R.string.order_edit_labor_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = form.taskTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AtelierTextField(
                    label = stringResource(R.string.order_add_task_price),
                    value = form.price,
                    onValueChange = onPriceChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = invalidField == "price",
                    errorText = if (invalidField == "price") stringResource(R.string.order_field_price) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                )
                error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
            }
        }
    )
}

/**
 * US25 E2: modal «Cancelar la orden» tras desistimiento del cliente.
 * «Los repuestos reservados volverán al inventario.» Exige motivo obligatorio (`reason`).
 */
@Composable
internal fun RejectQuotationDialog(
    form: RejectQuotationForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AtelierButton(
                text = stringResource(R.string.quotation_reject_confirm),
                onClick = onConfirm,
                enabled = form.reason.isNotBlank() && !isSaving,
                loading = isSaving
            )
        },
        dismissButton = {
            AtelierButton(
                text = stringResource(R.string.quotation_reject_back),
                onClick = onDismiss,
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        },
        title = { Text(stringResource(R.string.quotation_reject_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.quotation_reject_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
                AtelierTextField(
                    label = stringResource(R.string.quotation_reject_reason_label),
                    value = form.reason,
                    onValueChange = onReasonChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    placeholder = stringResource(R.string.quotation_reject_reason_hint),
                    isError = invalidField == "reason",
                    errorText = if (invalidField == "reason") stringResource(R.string.quotation_reject_field_error) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
            }
        }
    )
}

/**
 * US24 (Tarea 4): visor integrado de la proforma digital en PDF con verificación del desglose de mano de obra y repuestos,
 * más botón para compartir el archivo mediante `FileProvider`.
 */
@Composable
internal fun PdfPreviewDialog(
    pdfFile: File,
    breakdown: CascadeBreakdown?,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    var pages by remember(pdfFile.absolutePath) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var renderError by remember(pdfFile.absolutePath) { mutableStateOf(false) }

    LaunchedEffect(pdfFile.absolutePath) {
        withContext(Dispatchers.IO) {
            runCatching {
                if (!pdfFile.exists() || pdfFile.length() == 0L) return@runCatching emptyList()
                val rendered = mutableListOf<Bitmap>()
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val count = minOf(renderer.pageCount, 3)
                        for (index in 0 until count) {
                            renderer.openPage(index).use { page ->
                                val width = (page.width * 2).coerceAtLeast(600)
                                val height = (page.height * 2).coerceAtLeast(800)
                                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                rendered += bitmap
                            }
                        }
                    }
                }
                rendered
            }.onSuccess { bitmaps ->
                pages = bitmaps
            }.onFailure {
                renderError = true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(stringResource(R.string.quotation_pdf_preview_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = pdfFile.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = AtelierTheme.colors.inkSecondary
                )
                breakdown?.let { b ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AtelierTheme.colors.card)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.quotation_labor_subtotal), style = MaterialTheme.typography.bodySmall)
                            Text(b.laborSubtotal.format(), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.quotation_parts_subtotal), style = MaterialTheme.typography.bodySmall)
                            Text(b.partsSubtotal.format(), style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(color = AtelierTheme.colors.outlineSoft)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.quotation_subtotal), style = MaterialTheme.typography.bodySmall)
                            Text(b.subtotal.format(), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.quotation_igv), style = MaterialTheme.typography.bodySmall)
                            Text(b.taxAmount.format(), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.quotation_total), style = MaterialTheme.typography.titleSmall)
                            Text(b.totalAmount.format(), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                when {
                    pages.isNotEmpty() -> pages.forEachIndexed { idx, bitmap ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = stringResource(R.string.quotation_pdf_page_desc, idx + 1),
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                    renderError -> AtelierBanner(
                        message = stringResource(R.string.quotation_pdf_ready),
                        icon = Icons.Outlined.Description,
                        kind = BannerKind.Info
                    )
                    else -> Text(
                        text = stringResource(R.string.search_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                }
                AtelierButton(
                    text = stringResource(R.string.quotation_share_chooser),
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Outlined.Share
                )
                AtelierButton(
                    text = stringResource(R.string.quotation_pdf_close),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined
                )
            }
        }
    )
}

/**
 * US16 / Tarea 5: asignación o cambio de bahía física de trabajo para la orden en intervención técnica.
 */
@Composable
internal fun AssignBayDialog(
    bays: List<WorkBay>,
    currentBayId: String?,
    isSaving: Boolean,
    error: UiText?,
    onSelectBay: (WorkBay) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            AtelierButton(
                text = stringResource(R.string.proposals_cancel),
                onClick = onDismiss,
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        },
        title = { Text(stringResource(R.string.order_assign_bay_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.order_assign_bay_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
                if (bays.isEmpty()) {
                    Text(
                        text = stringResource(R.string.bays_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                } else {
                    bays.forEach { bay ->
                        val isCurrent = bay.id == currentBayId
                        val statusText = when (bay.status) {
                            BayStatus.AVAILABLE -> stringResource(R.string.bay_status_available)
                            BayStatus.OCCUPIED -> stringResource(R.string.bay_status_occupied)
                            BayStatus.MAINTENANCE -> stringResource(R.string.bay_status_maintenance)
                            null -> null
                        }
                        val statusBadgeColor = when (bay.status) {
                            BayStatus.AVAILABLE -> AtelierTheme.colors.ok.copy(alpha = 0.2f)
                            BayStatus.OCCUPIED -> MaterialTheme.colorScheme.primaryContainer
                            BayStatus.MAINTENANCE -> AtelierTheme.colors.wait.copy(alpha = 0.2f)
                            null -> null
                        }
                        AtelierSelectionCard(
                            title = bay.name,
                            subtitle = bay.type?.name,
                            badgeText = if (isCurrent) stringResource(R.string.order_bay_current) else statusText,
                            badgeColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else statusBadgeColor,
                            selected = isCurrent,
                            enabled = bay.canReceive && !isCurrent && !isSaving,
                            onClick = { onSelectBay(bay) }
                        )
                    }
                }
                error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
            }
        }
    )
}

/** Asignar o reasignar el mecánico responsable de una tarea técnica (US12 E2, US22 E2). */
@Composable
internal fun AssignMechanicDialog(
    taskTitle: String,
    currentMechanicId: String?,
    mechanics: List<MechanicOption>,
    isSaving: Boolean,
    error: UiText?,
    onSelectMechanic: (MechanicOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            AtelierButton(
                text = stringResource(R.string.proposals_cancel),
                onClick = onDismiss,
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        },
        title = { Text(stringResource(R.string.order_assign_mechanic_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.order_assign_mechanic_subtitle, taskTitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
                if (mechanics.isEmpty()) {
                    Text(
                        text = stringResource(R.string.order_mechanics_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                } else {
                    mechanics.forEach { mechanic ->
                        val isCurrent = mechanic.membershipId == currentMechanicId
                        val subtitle = if (mechanic.name != null && !mechanic.jobTitle.isNullOrBlank()) mechanic.jobTitle else null
                        AtelierSelectionCard(
                            title = mechanic.displayName,
                            subtitle = subtitle,
                            badgeText = if (isCurrent) stringResource(R.string.order_mechanic_current) else null,
                            badgeColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else null,
                            leadingIcon = Icons.Outlined.Person,
                            selected = isCurrent,
                            enabled = !isSaving,
                            onClick = { onSelectMechanic(mechanic) }
                        )
                    }
                }
                error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
            }
        }
    )
}
