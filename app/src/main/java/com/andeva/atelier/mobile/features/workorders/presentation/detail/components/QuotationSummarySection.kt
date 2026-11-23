package com.andeva.atelier.mobile.features.workorders.presentation.detail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusChipKind
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusLabelRes
import com.andeva.atelier.mobile.features.workorders.presentation.detail.CascadeBreakdown
import com.andeva.atelier.mobile.features.workorders.presentation.detail.WorkOrderDetailAction
import com.andeva.atelier.mobile.features.workorders.presentation.detail.WorkOrderDetailUiState
import com.andeva.atelier.mobile.features.workorders.presentation.detail.cascadeTotal
import com.andeva.atelier.mobile.features.workorders.presentation.detail.resolvedTotal
import java.math.BigDecimal

/**
 * US22 + US23: una tarea con su desglose en cascada (mano de obra + repuestos imputados = total de la tarea).
 * Sin tarjeta: la fila de la tarea abre su cronómetro y debajo van mecánico, mano de obra y repuestos, sangrados.
 */
@Composable
internal fun TaskBlock(
    task: TaskDetail,
    state: WorkOrderDetailUiState,
    onOpenTask: () -> Unit,
    onEditLabor: () -> Unit,
    onAddPart: () -> Unit,
    onEditPart: (TaskProduct) -> Unit,
    onReturnPart: (TaskProduct) -> Unit,
    onAssignMechanic: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = state.breakdown?.currency ?: task.price?.currency ?: Money.PEN
    val title = state.taskTitle(task)
    val laborMoney = task.price ?: Money(BigDecimal.ZERO, currency)
    val mechanicName = state.mechanicNameFor(task)
    val unassigned = stringResource(R.string.order_mechanic_unassigned)

    Column(modifier = modifier.fillMaxWidth()) {
        AtelierListRow(
            title = title,
            subtitle = mechanicName ?: unassigned,
            showDivider = false,
            onClick = onOpenTask,
            trailing = {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AtelierChip(
                        text = stringResource(
                            if (task.hasPendingSuspension) R.string.task_suspension_pending
                            else taskStatusLabelRes(task.status, task.holdReason)
                        ),
                        kind = if (task.hasPendingSuspension) ChipKind.Wait else taskStatusChipKind(task.status, task.holdReason)
                    )
                    Text(
                        text = task.cascadeTotal(currency).format(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        )
        Column(modifier = Modifier.padding(start = 32.dp, end = 16.dp, bottom = 8.dp)) {
            DetailLine(
                label = stringResource(R.string.order_task_mechanic_label),
                value = mechanicName ?: unassigned,
                actionLabel = if (state.canChangeMechanic) {
                    stringResource(if (mechanicName != null) R.string.order_reassign_mechanic_btn else R.string.order_assign_mechanic_btn)
                } else null,
                onClick = if (state.canChangeMechanic) onAssignMechanic else null
            )
            DetailLine(
                label = stringResource(R.string.order_task_labor),
                value = laborMoney.format(),
                actionLabel = if (state.canModifyTasks) stringResource(R.string.order_edit_labor) else null,
                onClick = if (state.canModifyTasks) onEditLabor else null
            )
            task.products.forEach { product ->
                PartLine(
                    product = product,
                    title = state.productTitle(product) ?: stringResource(R.string.task_part_unnamed),
                    currency = currency,
                    canEdit = state.canEditTaskParts,
                    onEdit = { onEditPart(product) },
                    onReturn = { onReturnPart(product) }
                )
            }
            if (state.canAddPartToTask) {
                AtelierButton(
                    text = stringResource(R.string.order_add_part),
                    onClick = onAddPart,
                    style = AtelierButtonStyle.Text
                )
            }
            if (task.isSuspended && state.canValidateSuspensions) {
                AtelierButton(
                    text = stringResource(R.string.orders_resume_task),
                    onClick = onResume,
                    style = AtelierButtonStyle.Tonal,
                    enabled = state.canDecideSuspension,
                    loading = state.busyAction == WorkOrderDetailAction.RESUME_TASK
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Línea rótulo y valor de 48 dp; con [onClick] muestra su acción en el color de acento. */
@Composable
private fun DetailLine(label: String, value: String, actionLabel: String?, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = AtelierTheme.colors.inkSecondary)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        if (actionLabel != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PartLine(
    product: TaskProduct,
    title: String,
    currency: String,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onReturn: () -> Unit
) {
    val unit = product.unitPrice ?: Money(BigDecimal.ZERO, currency)
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = stringResource(R.string.order_part_line_detail, product.quantity.formatQuantity(), unit.format()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
            }
            Text(
                text = product.resolvedTotal(currency).format(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        if (canEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtelierButton(text = stringResource(R.string.task_part_edit), onClick = onEdit, style = AtelierButtonStyle.Text)
                AtelierButton(text = stringResource(R.string.task_part_return), onClick = onReturn, style = AtelierButtonStyle.Text)
            }
        }
    }
}

/**
 * US22 + US24: la proforma en cascada (`Subtotal`, `IGV 18 %`, `Total`) y las acciones para generar el PDF y
 * enviarlo por correo. Sin conexión las acciones se apagan y se dice por qué.
 */
@Composable
internal fun QuotationSection(
    breakdown: CascadeBreakdown,
    canGeneratePdf: Boolean,
    canEmailQuotation: Boolean,
    showPdfActions: Boolean,
    showEmailAction: Boolean,
    busyAction: WorkOrderDetailAction?,
    onGeneratePdf: () -> Unit,
    onSendEmail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SummaryRow(stringResource(R.string.quotation_labor_subtotal), breakdown.laborSubtotal.format())
        SummaryRow(stringResource(R.string.quotation_parts_subtotal), breakdown.partsSubtotal.format())
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SummaryRow(stringResource(R.string.quotation_subtotal), breakdown.subtotal.format())
        SummaryRow(stringResource(R.string.quotation_igv), breakdown.taxAmount.format())
        SummaryRow(stringResource(R.string.quotation_total), breakdown.totalAmount.format(), emphasized = true)
        if (showPdfActions || showEmailAction) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showPdfActions) {
                    AtelierButton(
                        text = stringResource(R.string.quotation_generate_pdf),
                        onClick = onGeneratePdf,
                        modifier = Modifier.weight(1f),
                        style = AtelierButtonStyle.Tonal,
                        enabled = canGeneratePdf,
                        loading = busyAction == WorkOrderDetailAction.DOWNLOAD_PDF,
                        icon = Icons.Outlined.Description
                    )
                }
                if (showEmailAction) {
                    AtelierButton(
                        text = stringResource(R.string.orders_quotation_send_short),
                        onClick = onSendEmail,
                        modifier = Modifier.weight(1f),
                        style = AtelierButtonStyle.Outlined,
                        enabled = canEmailQuotation,
                        loading = busyAction == WorkOrderDetailAction.SEND_EMAIL
                    )
                }
            }
        }
    }
}

/** Rótulo a la izquierda y cifra a la derecha. */
@Composable
internal fun SummaryRow(label: String, value: String, emphasized: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (emphasized) MaterialTheme.colorScheme.onSurface else AtelierTheme.colors.inkSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
