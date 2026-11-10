package com.andeva.atelier.mobile.features.workorders.presentation.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
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
 * US22 + US23: tarjeta de tarea con desglose en cascada (Mano de obra + repuestos imputados = total de la tarea),
 * edición de mano de obra, corrección/devolución de repuestos y botón «+ Agregar repuesto».
 */
@Composable
internal fun TaskCascadeCard(
    task: TaskDetail,
    state: WorkOrderDetailUiState,
    bayName: String?,
    onOpenTask: () -> Unit,
    onEditLabor: () -> Unit,
    onAddPart: () -> Unit,
    onEditPart: (TaskProduct) -> Unit,
    onReturnPart: (TaskProduct) -> Unit,
    onAssignMechanic: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currency = state.breakdown?.currency ?: task.price?.currency ?: Money.PEN
    val title = state.taskTitle(task)
    val taskTotal = task.cascadeTotal(currency)
    val laborMoney = task.price ?: Money(BigDecimal.ZERO, currency)
    val estimatedMinutes = state.estimatedMinutesFor(task)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AtelierChip(
                text = stringResource(taskStatusLabelRes(task.status, task.holdReason)),
                kind = taskStatusChipKind(task.status, task.holdReason)
            )
            if (task.hasPendingSuspension) {
                AtelierChip(text = stringResource(R.string.task_suspension_pending), kind = ChipKind.Wait)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenTask),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitleParts = listOfNotNull(
                    bayName,
                    task.description.takeIf { it.isNotBlank() && it != title }
                )
                if (subtitleParts.isNotEmpty()) {
                    Text(
                        text = subtitleParts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = AtelierTheme.colors.inkSecondary
                    )
                }
            }
            Text(
                text = taskTotal.format(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        HorizontalDivider(color = AtelierTheme.colors.outlineSoft)

        // Fila de Mecánico asignado (US12 E2, US22 E2)
        val assignedMechanicName = state.mechanicNameFor(task)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (state.canChangeMechanic) {
                        Modifier.clickable(onClick = onAssignMechanic)
                    } else Modifier
                )
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (assignedMechanicName != null) MaterialTheme.colorScheme.primary else AtelierTheme.colors.inkSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.order_task_mechanic_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = AtelierTheme.colors.inkSecondary
                    )
                    Text(
                        text = assignedMechanicName ?: stringResource(R.string.order_mechanic_unassigned),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (assignedMechanicName != null) MaterialTheme.colorScheme.onSurface else AtelierTheme.colors.inkSecondary
                    )
                }
            }
            if (state.canChangeMechanic) {
                Text(
                    text = stringResource(
                        if (assignedMechanicName != null) R.string.order_reassign_mechanic_btn
                        else R.string.order_assign_mechanic_btn
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        HorizontalDivider(color = AtelierTheme.colors.outlineSoft)

        // Fila de Mano de obra (US22)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = state.canModifyTasks, onClick = onEditLabor),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.order_task_labor),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                estimatedMinutes?.takeIf { it > 0 }?.let { minutes ->
                    Text(
                        text = formatDurationMinutes(minutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = AtelierTheme.colors.inkSecondary
                    )
                }
            }
            Text(
                text = laborMoney.format(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Filas de repuestos imputados a la tarea (US23 / US33)
        task.products.forEach { product ->
            HorizontalDivider(color = AtelierTheme.colors.outlineSoft)
            TaskProductCascadeRow(
                product = product,
                productTitle = state.productTitle(product) ?: stringResource(R.string.task_part_unnamed),
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
    }
}

@Composable
private fun TaskProductCascadeRow(
    product: TaskProduct,
    productTitle: String,
    currency: String,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onReturn: () -> Unit
) {
    val unitMoney = product.unitPrice ?: Money(BigDecimal.ZERO, currency)
    val lineTotal = product.resolvedTotal(currency)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canEdit, onClick = onEdit),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = productTitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.order_part_line_detail,
                        product.quantity.formatQuantity(),
                        unitMoney.format()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = AtelierTheme.colors.inkSecondary
                )
            }
            Text(
                text = lineTotal.format(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (canEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AtelierButton(
                    text = stringResource(R.string.task_part_edit),
                    onClick = onEdit,
                    style = AtelierButtonStyle.Text
                )
                AtelierButton(
                    text = stringResource(R.string.task_part_return),
                    onClick = onReturn,
                    style = AtelierButtonStyle.Text
                )
            }
        }
    }
}

/**
 * US22 + US24: tarjeta financiera en cascada (`Subtotal`, `IGV 18 %`, `Total`) y botones para generar/compartir
 * la proforma comercial en PDF y enviarla por correo al cliente.
 */
@Composable
internal fun QuotationCascadeFooter(
    breakdown: CascadeBreakdown,
    canGeneratePdf: Boolean,
    canEmailQuotation: Boolean,
    busyAction: WorkOrderDetailAction?,
    onGeneratePdf: () -> Unit,
    onSendEmail: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryLine(
            label = stringResource(R.string.quotation_labor_subtotal),
            value = breakdown.laborSubtotal.format()
        )
        SummaryLine(
            label = stringResource(R.string.quotation_parts_subtotal),
            value = breakdown.partsSubtotal.format()
        )
        HorizontalDivider(color = AtelierTheme.colors.outlineSoft)
        SummaryLine(
            label = stringResource(R.string.quotation_subtotal),
            value = breakdown.subtotal.format()
        )
        SummaryLine(
            label = stringResource(R.string.quotation_igv),
            value = breakdown.taxAmount.format()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(R.string.quotation_total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = breakdown.totalAmount.format(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (canGeneratePdf || busyAction == WorkOrderDetailAction.DOWNLOAD_PDF) {
            AtelierButton(
                text = stringResource(R.string.quotation_generate_pdf),
                onClick = onGeneratePdf,
                modifier = Modifier.fillMaxWidth(),
                enabled = canGeneratePdf,
                loading = busyAction == WorkOrderDetailAction.DOWNLOAD_PDF,
                icon = Icons.Outlined.Description
            )
        }
        if (canEmailQuotation || busyAction == WorkOrderDetailAction.SEND_EMAIL) {
            AtelierButton(
                text = stringResource(R.string.quotation_send_email),
                onClick = onSendEmail,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Outlined,
                enabled = canEmailQuotation,
                loading = busyAction == WorkOrderDetailAction.SEND_EMAIL
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * US25: bloque «¿Qué respondió el cliente?» para aprobar el presupuesto (`IN_PROGRESS`) o registrar el
 * desistimiento con motivo (`CANCELED`).
 */
@Composable
internal fun CustomerResolutionSection(
    canResolve: Boolean,
    busyAction: WorkOrderDetailAction?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AtelierChip(
            text = stringResource(R.string.order_status_waiting_customer),
            kind = ChipKind.Wait
        )
        Text(
            text = stringResource(R.string.quotation_customer_question),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        AtelierButton(
            text = stringResource(R.string.quotation_approve_action),
            onClick = onApprove,
            modifier = Modifier.fillMaxWidth(),
            enabled = canResolve,
            loading = busyAction == WorkOrderDetailAction.APPROVE_QUOTATION,
            icon = Icons.Outlined.CheckCircle
        )
        AtelierButton(
            text = stringResource(R.string.quotation_reject_action),
            onClick = onReject,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Tonal,
            enabled = canResolve,
            icon = Icons.Outlined.Cancel
        )
    }
}

@Composable
private fun formatDurationMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rem = minutes % 60
    return when {
        hours > 0 && rem > 0 -> stringResource(R.string.order_task_hours_minutes, hours, rem)
        hours > 0 -> stringResource(R.string.order_task_hours, hours)
        else -> stringResource(R.string.order_task_minutes, rem)
    }
}
