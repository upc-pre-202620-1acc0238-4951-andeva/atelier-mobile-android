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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSelectionCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.bays.presentation.labelRes
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.presentation.detail.ChargeForm
import com.andeva.atelier.mobile.features.tasks.presentation.detail.PartEditForm
import com.andeva.atelier.mobile.features.tasks.presentation.detail.PartEditMode
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.presentation.detail.AddTaskForm
import com.andeva.atelier.mobile.features.workorders.presentation.detail.CancelOrderForm
import com.andeva.atelier.mobile.features.workorders.presentation.detail.CascadeBreakdown
import com.andeva.atelier.mobile.features.workorders.presentation.detail.EditLaborForm
import com.andeva.atelier.mobile.features.workorders.presentation.detail.RejectQuotationForm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val DecimalKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)

@Composable
private fun SheetError(error: UiText?) {
    error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
}

/** Fila de elección dentro de una hoja: 48 dp mínimo, marca de elegido con ícono y no solo con color. */
@Composable
internal fun PickRow(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else AtelierTheme.colors.muted
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AtelierTheme.colors.inkSecondary
                    )
                }
            }
            trailing?.invoke()
            if (selected && trailing == null) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** US22 E1: agregar una nueva tarea con su servicio y precio de mano de obra sin IGV. */
@Composable
internal fun AddTaskSheet(
    form: AddTaskForm,
    services: List<ServiceOption>,
    mechanics: List<MechanicOption>,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onSelectService: (ServiceOption) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onSelectMechanic: (String?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.order_add_task_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.order_add_task_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = form.serviceId != null && form.description.isNotBlank() && !isSaving,
                loading = isSaving
            )
        }
    ) {
        Text(
            text = stringResource(R.string.order_add_task_service),
            style = MaterialTheme.typography.labelLarge,
            color = AtelierTheme.colors.inkSecondary
        )
        Column {
            services.forEach { service ->
                PickRow(
                    title = service.name,
                    subtitle = service.basePrice?.format(),
                    selected = service.id == form.serviceId,
                    enabled = !isSaving,
                    onClick = { onSelectService(service) }
                )
            }
        }
        if (invalidField == "serviceId") {
            Text(
                text = stringResource(R.string.order_field_service),
                style = MaterialTheme.typography.bodyMedium,
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
            keyboardOptions = DecimalKeyboard
        )
        if (mechanics.isNotEmpty()) {
            Text(
                text = stringResource(R.string.order_add_task_mechanic),
                style = MaterialTheme.typography.labelLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            Column {
                PickRow(
                    title = stringResource(R.string.open_order_mechanic_none),
                    selected = form.mechanicId == null,
                    enabled = !isSaving,
                    onClick = { onSelectMechanic(null) }
                )
                mechanics.forEach { mechanic ->
                    PickRow(
                        title = mechanic.displayName,
                        subtitle = mechanic.jobTitle?.takeIf { mechanic.name != null && it.isNotBlank() },
                        selected = mechanic.membershipId == form.mechanicId,
                        enabled = !isSaving,
                        onClick = { onSelectMechanic(mechanic.membershipId) }
                    )
                }
            }
        }
        SheetError(error)
    }
}

/** US22 E2: actualizar el precio de mano de obra sin IGV de una tarea existente. */
@Composable
internal fun EditLaborSheet(
    form: EditLaborForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onPriceChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.order_edit_labor_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.order_edit_labor_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = form.price.isNotBlank() && !isSaving,
                loading = isSaving
            )
        }
    ) {
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
            keyboardOptions = DecimalKeyboard
        )
        SheetError(error)
    }
}

/** Confirmación para eliminar una tarea no completada de la orden de trabajo. */
@Composable
internal fun DeleteTaskSheet(
    taskTitle: String,
    hasProducts: Boolean,
    isSaving: Boolean,
    error: UiText?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.order_delete_task_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.order_delete_task_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.DestructiveFilled,
                enabled = !hasProducts && !isSaving,
                loading = isSaving
            )
            AtelierButton(
                text = stringResource(R.string.quotation_reject_back),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        }
    ) {
        if (hasProducts) {
            AtelierBanner(
                message = stringResource(R.string.order_delete_task_has_parts),
                icon = Icons.Outlined.ErrorOutline,
                kind = BannerKind.Wait
            )
        } else {
            Text(
                text = stringResource(R.string.order_delete_task_body, taskTitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        SheetError(error)
    }
}

/**
 * US25 E2: «Cancelar la orden» tras el desistimiento del cliente. Los repuestos reservados vuelven al inventario;
 * exige el motivo (`reason`).
 */
@Composable
internal fun RejectQuotationSheet(
    form: RejectQuotationForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ReasonSheet(
        title = stringResource(R.string.quotation_reject_title),
        body = stringResource(R.string.quotation_reject_body),
        label = stringResource(R.string.quotation_reject_reason_label),
        hint = stringResource(R.string.quotation_reject_reason_hint),
        fieldError = stringResource(R.string.quotation_reject_field_error),
        confirmText = stringResource(R.string.quotation_reject_confirm),
        value = form.reason,
        isSaving = isSaving,
        invalid = invalidField == "reason",
        error = error,
        onReasonChange = onReasonChange,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/** Cancelar la orden en cualquier momento: siempre se confirma aquí y pide el motivo. */
@Composable
internal fun CancelOrderSheet(
    form: CancelOrderForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ReasonSheet(
        title = stringResource(R.string.orders_cancel_title),
        body = stringResource(R.string.orders_cancel_body),
        label = stringResource(R.string.orders_cancel_reason_label),
        hint = stringResource(R.string.orders_cancel_reason_hint),
        fieldError = stringResource(R.string.orders_cancel_field_error),
        confirmText = stringResource(R.string.orders_cancel_confirm),
        value = form.reason,
        isSaving = isSaving,
        invalid = invalidField == "reason",
        error = error,
        onReasonChange = onReasonChange,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun ReasonSheet(
    title: String,
    body: String,
    label: String,
    hint: String,
    fieldError: String,
    confirmText: String,
    value: String,
    isSaving: Boolean,
    invalid: Boolean,
    error: UiText?,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = title,
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.DestructiveFilled,
                enabled = value.isNotBlank() && !isSaving,
                loading = isSaving
            )
            AtelierButton(
                text = stringResource(R.string.quotation_reject_back),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        }
    ) {
        Text(text = body, style = MaterialTheme.typography.bodyLarge, color = AtelierTheme.colors.inkSecondary)
        AtelierTextField(
            label = label,
            value = value,
            onValueChange = onReasonChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            placeholder = hint,
            isError = invalid,
            errorText = if (invalid) fieldError else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        SheetError(error)
    }
}

/**
 * US25: la proforma espera la respuesta del cliente. Aceptó inicia los trabajos; rechazó pasa al motivo
 * y cancela la orden.
 */
@Composable
internal fun QuotationDecisionSheet(
    isApproving: Boolean,
    isBusy: Boolean,
    isOnline: Boolean,
    error: UiText?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.orders_quotation_decision_title),
        onDismiss = onDismiss,
        actions = {
            if (!isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.quotation_approve_action),
                onClick = onApprove,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                loading = isApproving
            )
            AtelierButton(
                text = stringResource(R.string.quotation_reject_action),
                onClick = onReject,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Destructive,
                enabled = !isBusy
            )
        }
    ) {
        Text(
            text = stringResource(R.string.quotation_customer_question),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        SheetError(error)
    }
}

/**
 * H5: un mecánico pidió suspender su tarea (falta un repuesto). Quien tiene `tasks:hold_validate` aprueba o
 * rechaza aquí, con el motivo a la vista.
 */
@Composable
internal fun SuspensionSheet(
    taskTitle: String,
    reason: String?,
    missingItem: String?,
    canDecide: Boolean,
    isOnline: Boolean,
    busyApprove: Boolean,
    busyReject: Boolean,
    error: UiText?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit
) {
    val busy = busyApprove || busyReject
    AtelierBottomSheet(
        title = stringResource(R.string.orders_suspension_title),
        onDismiss = onDismiss,
        actions = {
            if (!isOnline) {
                AtelierRequiresConnection()
            }
            AtelierButton(
                text = stringResource(R.string.orders_suspension_approve),
                onClick = onApprove,
                modifier = Modifier.fillMaxWidth(),
                enabled = canDecide && isOnline && !busy,
                loading = busyApprove
            )
            AtelierButton(
                text = stringResource(R.string.orders_suspension_reject),
                onClick = onReject,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Destructive,
                enabled = canDecide && isOnline && !busy,
                loading = busyReject
            )
        }
    ) {
        Text(text = taskTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = reason?.takeIf { it.isNotBlank() } ?: stringResource(R.string.orders_suspension_no_reason),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        missingItem?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = stringResource(R.string.orders_suspension_missing_item, it),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        SheetError(error)
    }
}

/**
 * US24: vista de la proforma en PDF con el desglose y el botón de compartir (`FileProvider`).
 */
@Composable
internal fun PdfPreviewSheet(
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

    AtelierBottomSheet(
        title = stringResource(R.string.quotation_pdf_preview_title),
        onDismiss = onDismiss,
        actions = {
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
                style = AtelierButtonStyle.Text
            )
        }
    ) {
        Text(text = pdfFile.name, style = MaterialTheme.typography.bodyMedium, color = AtelierTheme.colors.inkSecondary)
        breakdown?.let { b ->
            SummaryRow(stringResource(R.string.quotation_subtotal), b.subtotal.format())
            SummaryRow(stringResource(R.string.quotation_igv), b.taxAmount.format())
            SummaryRow(stringResource(R.string.quotation_total), b.totalAmount.format(), emphasized = true)
        }
        when {
            pages.isNotEmpty() -> pages.forEachIndexed { idx, bitmap ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ControlShape)
                        .background(AtelierTheme.colors.plate)
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
    }
}

/** US16: asignación o cambio de bahía de trabajo para la orden. */
@Composable
internal fun AssignBaySheet(
    bays: List<WorkBay>,
    currentBayId: String?,
    isSaving: Boolean,
    error: UiText?,
    onSelectBay: (WorkBay) -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.order_assign_bay_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        }
    ) {
        Text(
            text = stringResource(R.string.order_assign_bay_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
        if (bays.isEmpty()) {
            Text(
                text = stringResource(R.string.bays_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
        } else {
            Column {
                bays.forEach { bay ->
                    val isCurrent = bay.id == currentBayId
                    val (statusText, statusKind) = when (bay.status) {
                        BayStatus.AVAILABLE -> stringResource(R.string.bay_status_available) to ChipKind.Ok
                        BayStatus.OCCUPIED -> stringResource(R.string.bay_status_occupied) to ChipKind.Info
                        BayStatus.MAINTENANCE -> stringResource(R.string.bay_status_maintenance) to ChipKind.Wait
                        null -> null to ChipKind.Idle
                    }
                    PickRow(
                        title = bay.name,
                        subtitle = bay.type?.let { stringResource(it.labelRes()) },
                        selected = isCurrent,
                        enabled = bay.canReceive && !isCurrent && !isSaving,
                        onClick = { onSelectBay(bay) },
                        trailing = {
                            if (isCurrent) {
                                AtelierChip(text = stringResource(R.string.order_bay_current), kind = ChipKind.Info)
                            } else if (statusText != null) {
                                AtelierChip(text = statusText, kind = statusKind)
                            }
                        }
                    )
                }
            }
        }
        SheetError(error)
    }
}

/** Asignar o reasignar el mecánico responsable de una tarea (US12 E2, US22 E2). */
@Composable
internal fun AssignMechanicSheet(
    taskTitle: String,
    currentMechanicId: String?,
    mechanics: List<MechanicOption>,
    isSaving: Boolean,
    error: UiText?,
    onSelectMechanic: (MechanicOption) -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.order_assign_mechanic_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        }
    ) {
        Text(
            text = stringResource(R.string.order_assign_mechanic_subtitle, taskTitle),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
        if (mechanics.isEmpty()) {
            Text(
                text = stringResource(R.string.order_mechanics_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
        } else {
            Column {
                mechanics.forEach { mechanic ->
                    val isCurrent = mechanic.membershipId == currentMechanicId
                    PickRow(
                        title = mechanic.displayName,
                        subtitle = mechanic.jobTitle?.takeIf { mechanic.name != null && it.isNotBlank() },
                        selected = isCurrent,
                        enabled = !isSaving,
                        onClick = { onSelectMechanic(mechanic) },
                        leading = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = AtelierTheme.colors.inkSecondary
                            )
                        },
                        trailing = if (isCurrent) {
                            { AtelierChip(text = stringResource(R.string.order_mechanic_current), kind = ChipKind.Info) }
                        } else null
                    )
                }
            }
        }
        SheetError(error)
    }
}

/** US32 E2: buscar el repuesto e imputarlo desde la orden en hoja inferior (acciones al alcance del pulgar). */
@Composable
internal fun OrderChargePartSheet(
    form: ChargeForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onQueryChange: (String) -> Unit,
    onSelect: (PartOption) -> Unit,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.charge_title),
        onDismiss = { if (!isSaving) onDismiss() },
        actions = {
            AtelierButton(
                text = stringResource(R.string.charge_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = form.selected != null && form.quantity.isNotBlank() && !isSaving,
                loading = isSaving
            )
            AtelierButton(
                text = stringResource(R.string.proposals_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        }
    ) {
        if (form.selected == null) {
            AtelierTextField(
                label = stringResource(R.string.catalog_search_label),
                value = form.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                placeholder = stringResource(R.string.catalog_search_hint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            Text(
                text = stringResource(R.string.charge_select_part_prompt),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            when {
                form.isSearching -> Text(
                    text = stringResource(R.string.search_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
                form.results.isEmpty() -> Text(
                    text = stringResource(R.string.catalog_empty_filtered),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
                else -> form.results.take(MAX_PART_RESULTS).forEach { part ->
                    OrderPartOptionRow(part = part, onSelect = onSelect, enabled = !isSaving)
                }
            }
        } else {
            OrderPartOptionRow(part = form.selected, onSelect = {}, enabled = false, selected = true)
            AtelierButton(
                text = stringResource(R.string.charge_change_part_btn),
                onClick = { onQueryChange(form.query) },
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
            AtelierTextField(
                label = stringResource(R.string.charge_quantity),
                value = form.quantity,
                onValueChange = onQuantityChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                isError = invalidField == "quantity",
                errorText = if (invalidField == "quantity") stringResource(R.string.task_field_quantity) else null,
                keyboardOptions = DecimalKeyboard
            )
        }
        SheetError(error)
    }
}

@Composable
private fun OrderPartOptionRow(
    part: PartOption,
    onSelect: (PartOption) -> Unit,
    enabled: Boolean,
    selected: Boolean = false
) {
    val stockText = if (part.isAvailable) {
        stringResource(R.string.part_stock, part.totalStock.formatQuantity())
    } else {
        stringResource(R.string.stock_out)
    }
    val priceFormatted = part.basePrice?.let {
        stringResource(R.string.part_price, Money(it, Money.PEN).format())
    }

    AtelierSelectionCard(
        title = part.name,
        subtitle = stockText,
        price = priceFormatted,
        badgeText = if (!part.isAvailable) stringResource(R.string.stock_out) else null,
        badgeColor = if (!part.isAvailable) AtelierTheme.colors.badContainer else null,
        selected = selected,
        enabled = enabled && part.isAvailable,
        onClick = { onSelect(part) }
    )
}

/** US33: corregir la cantidad de un repuesto imputado o devolverlo al lote de origen en hoja inferior. */
@Composable
internal fun OrderPartEditSheet(
    form: PartEditForm,
    isSaving: Boolean,
    invalidField: String?,
    error: UiText?,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val returning = form.mode == PartEditMode.RETURN
    AtelierBottomSheet(
        title = stringResource(if (returning) R.string.return_title else R.string.edit_part_title),
        onDismiss = { if (!isSaving) onDismiss() },
        actions = {
            AtelierButton(
                text = stringResource(if (returning) R.string.return_confirm else R.string.edit_part_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                style = if (returning) AtelierButtonStyle.DestructiveFilled else AtelierButtonStyle.Filled,
                enabled = (returning || form.quantity.isNotBlank()) && !isSaving,
                loading = isSaving
            )
            AtelierButton(
                text = stringResource(R.string.proposals_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        }
    ) {
        Text(
            text = form.product.productName ?: stringResource(R.string.task_part_unnamed),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(if (returning) R.string.return_body else R.string.edit_part_body),
            style = MaterialTheme.typography.bodyLarge,
            color = AtelierTheme.colors.inkSecondary
        )
        if (!returning) {
            AtelierTextField(
                label = stringResource(R.string.charge_quantity),
                value = form.quantity,
                onValueChange = onQuantityChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                isError = invalidField == "quantity",
                errorText = if (invalidField == "quantity") stringResource(R.string.task_field_quantity) else null,
                keyboardOptions = DecimalKeyboard
            )
        }
        SheetError(error)
    }
}

private const val MAX_PART_RESULTS = 8
