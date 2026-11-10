package com.andeva.atelier.mobile.features.workorders.presentation.detail

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.presentation.detail.ChargeForm
import com.andeva.atelier.mobile.features.tasks.presentation.detail.PartEditForm
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

private val IGV_RATE = BigDecimal("0.18")
private const val GENERIC_SERVICE_NAME = "Standard Service"
private const val GENERIC_PRODUCT_PREFIX = "Product "

/** Acción en línea en curso dentro del detalle/proforma de la orden. */
enum class WorkOrderDetailAction {
    ADD_TASK,
    EDIT_LABOR,
    CHARGE_PART,
    EDIT_PART,
    REMOVE_PART,
    DOWNLOAD_PDF,
    SEND_EMAIL,
    APPROVE_QUOTATION,
    REJECT_QUOTATION,
    ASSIGN_BAY,
    ASSIGN_MECHANIC
}

/** US22 E1: formulario para agregar una nueva tarea con su servicio y mano de obra sin IGV. */
data class AddTaskForm(
    val serviceId: String? = null,
    val description: String = "",
    val price: String = "",
    val mechanicId: String? = null
)

/** US22 E2: formulario para actualizar el precio de mano de obra sin IGV de una tarea. */
data class EditLaborForm(
    val taskId: TaskId,
    val taskTitle: String,
    val price: String
)

/** US25 E2: modal «Cancelar la orden» con el motivo obligatorio del desistimiento del cliente. */
data class RejectQuotationForm(
    val reason: String = ""
)

/**
 * US22: liquidación contable en cascada libre de errores de redondeo (`BigDecimal`, escala 2, `HALF_EVEN`).
 * $\text{Subtotal} = \sum \text{Mano de obra} + \sum \text{Repuestos}$, $\text{IGV} = 18\%$, $\text{Total} = \text{Subtotal} + \text{IGV}$.
 */
data class CascadeBreakdown(
    val currency: String,
    val laborSubtotal: Money,
    val partsSubtotal: Money,
    val subtotal: Money,
    val taxAmount: Money,
    val totalAmount: Money
)

data class WorkOrderDetailUiState(
    val detail: WorkOrderDetail? = null,
    val services: List<ServiceOption> = emptyList(),
    val partNamesById: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val busyAction: WorkOrderDetailAction? = null,
    val addTaskForm: AddTaskForm? = null,
    val editLaborForm: EditLaborForm? = null,
    val chargeTaskId: TaskId? = null,
    val chargeForm: ChargeForm? = null,
    val editingPartTaskId: TaskId? = null,
    val partEdit: PartEditForm? = null,
    val rejectForm: RejectQuotationForm? = null,
    val generatedPdf: File? = null,
    val proformaSentLabel: String? = null,
    val notice: UiText? = null,
    val error: UiText? = null,
    val invalidField: String? = null,
    val canUpdateWorkOrder: Boolean = false,
    val canChargeParts: Boolean = false,
    val canReadParts: Boolean = false,
    val canReadQuotation: Boolean = false,
    val canSendQuotation: Boolean = false,
    val canApproveQuotation: Boolean = false,
    val canAssignBay: Boolean = false,
    val availableBays: List<WorkBay> = emptyList(),
    val showAssignBayDialog: Boolean = false,
    val canReassignMechanic: Boolean = false,
    val availableMechanics: List<MechanicOption> = emptyList(),
    val assigningMechanicTask: TaskDetail? = null
) {
    val isBusy: Boolean get() = busyAction != null

    val status: WorkOrderStatus? get() = detail?.workOrder?.status
    val isDraft: Boolean get() = status == WorkOrderStatus.DRAFT
    val isEditableOrder: Boolean get() = status == WorkOrderStatus.DRAFT || status == WorkOrderStatus.IN_PROGRESS

    val canModifyTasks: Boolean get() = canUpdateWorkOrder && isEditableOrder && isOnline && !isBusy
    val canChangeMechanic: Boolean get() = canReassignMechanic && isEditableOrder && isOnline && !isBusy
    val canAddPartToTask: Boolean get() = canChargeParts && isEditableOrder && isOnline && !isBusy
    val canEditTaskParts: Boolean get() = canUpdateWorkOrder && isEditableOrder && isOnline && !isBusy
    val canGeneratePdf: Boolean get() = canReadQuotation && detail != null && isOnline && !isBusy
    val canEmailQuotation: Boolean get() = canSendQuotation && detail != null && isOnline && !isBusy
    val canResolveQuotation: Boolean get() = canApproveQuotation && isDraft && isOnline && !isBusy

    val breakdown: CascadeBreakdown? get() = detail?.cascadeBreakdown()

    /** Resuelve el nombre comercial del servicio desde el catálogo si el backend devolvió el genérico (H11). */
    fun taskTitle(task: TaskDetail): String {
        val fromCatalog = task.serviceId?.let { id -> services.firstOrNull { it.id == id }?.name }
        val rawName = task.serviceName?.takeIf { it.isNotBlank() && it != GENERIC_SERVICE_NAME }
        return fromCatalog ?: rawName ?: task.description
    }

    /** Minutos estimados del servicio en el catálogo para mostrar junto a «Mano de obra» (p. ej. `2 h`, `45 min`). */
    fun estimatedMinutesFor(task: TaskDetail): Int? =
        task.serviceId?.let { id -> services.firstOrNull { it.id == id }?.estimatedMinutes }

    /** Resuelve el nombre del mecánico asignado a la tarea cruzando con los colaboradores de la sede (H11). */
    fun mechanicNameFor(task: TaskDetail): String? {
        val assigned = task.mechanicId?.let { id -> availableMechanics.firstOrNull { it.membershipId == id } }
        return assigned?.displayName ?: task.mechanicName?.takeIf { it.isNotBlank() }
    }

    /** Resuelve el nombre comercial del repuesto si el backend devolvió `Product <uuid>` (H11, H36). */
    fun productTitle(product: TaskProduct): String? {
        val fromCatalog = partNamesById[product.productId]
        val rawName = product.productName?.takeIf { it.isNotBlank() && !it.startsWith(GENERIC_PRODUCT_PREFIX) }
        return fromCatalog ?: rawName ?: product.productName
    }
}

/** Importe total de la línea de repuesto: usa `totalAmount` o calcula `cantidad × unitario` con escala 2. */
fun TaskProduct.resolvedTotal(currency: String = unitPrice?.currency ?: Money.PEN): Money {
    totalAmount?.let { return it }
    val unit = unitPrice?.amount ?: BigDecimal.ZERO
    return Money(unit.multiply(quantity).setScale(2, RoundingMode.HALF_EVEN), unitPrice?.currency ?: currency)
}

/** Suma de todos los repuestos imputados a la tarea (sin IGV). */
fun TaskDetail.partsSubtotal(currency: String = price?.currency ?: Money.PEN): Money {
    val sum = products.fold(BigDecimal.ZERO) { acc, item ->
        acc.add(item.resolvedTotal(currency).amount)
    }.setScale(2, RoundingMode.HALF_EVEN)
    return Money(sum, currency)
}

/**
 * US22: $\text{Precio Tarea} = \text{Mano de obra} + \sum(\text{Precio Repuesto} \times \text{Cantidad})$
 * con precisión decimal (`BigDecimal`, escala 2, `HALF_EVEN`).
 */
fun TaskDetail.cascadeTotal(currency: String = price?.currency ?: Money.PEN): Money {
    val labor = price?.amount ?: BigDecimal.ZERO
    val parts = partsSubtotal(currency).amount
    return Money(labor.add(parts).setScale(2, RoundingMode.HALF_EVEN), currency)
}

/**
 * US22: calcula en cascada el subtotal de mano de obra, el subtotal de repuestos, el subtotal imponible,
 * el IGV (18 %) y el total general de la orden de trabajo.
 */
fun WorkOrderDetail.cascadeBreakdown(): CascadeBreakdown {
    val currency = workOrder.totalAmount?.currency
        ?: workOrder.subtotal?.currency
        ?: tasks.firstNotNullOfOrNull { it.price?.currency }
        ?: Money.PEN

    val laborSum = tasks.fold(BigDecimal.ZERO) { acc, task ->
        acc.add(task.price?.amount ?: BigDecimal.ZERO)
    }.setScale(2, RoundingMode.HALF_EVEN)

    val partsSum = tasks.fold(BigDecimal.ZERO) { acc, task ->
        acc.add(task.partsSubtotal(currency).amount)
    }.setScale(2, RoundingMode.HALF_EVEN)

    val computedSubtotal = laborSum.add(partsSum).setScale(2, RoundingMode.HALF_EVEN)
    val hasTaskAmounts = tasks.any { it.price != null || it.products.isNotEmpty() }

    val subtotalAmount = if (hasTaskAmounts) {
        computedSubtotal
    } else {
        workOrder.subtotal?.amount?.setScale(2, RoundingMode.HALF_EVEN) ?: computedSubtotal
    }

    val taxAmount = if (hasTaskAmounts) {
        subtotalAmount.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_EVEN)
    } else {
        workOrder.taxAmount?.amount?.setScale(2, RoundingMode.HALF_EVEN)
            ?: subtotalAmount.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_EVEN)
    }

    val totalAmount = if (hasTaskAmounts) {
        subtotalAmount.add(taxAmount).setScale(2, RoundingMode.HALF_EVEN)
    } else {
        workOrder.totalAmount?.amount?.setScale(2, RoundingMode.HALF_EVEN)
            ?: subtotalAmount.add(taxAmount).setScale(2, RoundingMode.HALF_EVEN)
    }

    return CascadeBreakdown(
        currency = currency,
        laborSubtotal = Money(laborSum, currency),
        partsSubtotal = Money(partsSum, currency),
        subtotal = Money(subtotalAmount, currency),
        taxAmount = Money(taxAmount, currency),
        totalAmount = Money(totalAmount, currency)
    )
}
