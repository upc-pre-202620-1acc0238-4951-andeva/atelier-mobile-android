package com.andeva.atelier.mobile.features.workorders.infrastructure.local

import com.andeva.atelier.mobile.core.data.remote.employee.EmployeeProfileDto
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.workorders.domain.IntakeImage
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.PartMovement
import com.andeva.atelier.mobile.features.workorders.domain.PartProfitability
import com.andeva.atelier.mobile.features.workorders.domain.Profitability
import com.andeva.atelier.mobile.features.workorders.domain.Proposal
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.ProposalStatus
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.PartMovementDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.ServiceDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.TaskProposalDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderImageDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderProfitabilityDto
import java.math.BigDecimal
import java.time.Instant

/** Room guarda los montos como texto decimal (docs/06-offline-sync.md §5); sin moneda se asume PEN. */
fun WorkOrderDto.toEntity(syncedAt: Long) = WorkOrderEntity(
    id = id,
    internalNumber = internalNumber,
    branchId = branchId,
    vehicleId = vehicleId,
    customerId = customerId,
    currentBayId = currentBayId,
    bayName = bayName,
    status = status.orEmpty(),
    diagnosticSummary = diagnosticSummary,
    mileageIn = mileageIn,
    subtotal = subtotal?.toPlainString(),
    taxAmount = taxAmount?.toPlainString(),
    totalAmount = (totalAmount ?: BigDecimal.ZERO).toPlainString(),
    currency = currency ?: Money.PEN,
    syncedAt = syncedAt
)

fun WorkOrderEntity.toDomain(plate: String? = null, customerName: String? = null) = WorkOrder(
    id = id,
    internalNumber = internalNumber,
    branchId = branchId,
    vehicleId = vehicleId,
    customerId = customerId,
    currentBayId = currentBayId,
    bayName = bayName,
    status = status.toEnum<WorkOrderStatus>(),
    diagnosticSummary = diagnosticSummary,
    mileageIn = mileageIn,
    subtotal = money(subtotal?.toBigDecimalOrNull(), currency),
    taxAmount = money(taxAmount?.toBigDecimalOrNull(), currency),
    totalAmount = money(totalAmount.toBigDecimalOrNull(), currency),
    plate = plate,
    customerName = customerName
)

fun WorkOrderWithRefs.toDomain() = workOrder.toDomain(plate, customerName)

/** [header] es la fila ya guardada (con placa y cliente si están en caché). */
fun WorkOrderDto.toDetail(header: WorkOrder) = WorkOrderDetail(
    workOrder = header,
    tasks = tasks.orEmpty().map { it.toDomain() },
    proposals = proposals.orEmpty().map { it.toDomain() },
    intakeImages = intakeImages.orEmpty().map { it.toDomain() },
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant()
)

fun TaskProposalDto.toDomain() = Proposal(
    id = id,
    workOrderId = workOrderId,
    taskId = taskId,
    serviceId = serviceId,
    mechanicId = mechanicId,
    description = description.orEmpty(),
    severity = severity.toEnum<ProposalSeverity>(),
    imageUrl = imageUrl,
    status = status.toEnum<ProposalStatus>(),
    customerNotes = customerNotes,
    createdAt = createdAt.toInstant()
)

fun WorkOrderImageDto.toDomain() = IntakeImage(
    id = id,
    imageUrl = imageUrl,
    description = description,
    uploadedAt = uploadedAt.toInstant()
)

fun ServiceDto.toDomain() = ServiceOption(
    id = id,
    name = name.orEmpty(),
    basePrice = money(basePrice, currency),
    estimatedMinutes = estimatedMinutes
)

/** Sin `membershipId` el colaborador no se puede asignar: se descarta. */
fun EmployeeProfileDto.toMechanicOption(): MechanicOption? =
    membershipId?.let { MechanicOption(it, jobTitle, employmentStatus) }

fun PartMovementDto.toDomain() = PartMovement(
    allocationId = allocationId,
    productId = productId,
    taskId = taskId,
    batchId = batchId,
    chargedQuantity = chargedQuantity ?: BigDecimal.ZERO,
    returnedQuantity = returnedQuantity ?: BigDecimal.ZERO,
    unitCost = money(unitCost, currency),
    chargedAt = chargedAt.toInstant(),
    lastReturnedAt = lastReturnedAt.toInstant(),
    lastReturnedBy = lastReturnedBy,
    lastReturnReason = lastReturnReason
)

fun WorkOrderProfitabilityDto.toDomain() = Profitability(
    workOrderId = workOrderId,
    currency = currency,
    laborRevenue = laborRevenue,
    partsRevenue = partsRevenue,
    partsCostOfSales = partsCostOfSales,
    partsGrossProfit = partsGrossProfit,
    subtotal = subtotal,
    lines = lines.orEmpty().map { line ->
        PartProfitability(
            taskId = line.taskId,
            productId = line.productId,
            quantity = line.quantity ?: BigDecimal.ZERO,
            unitPrice = line.unitPrice,
            revenue = line.revenue,
            costOfSales = line.costOfSales,
            grossProfit = line.grossProfit
        )
    }
)

private inline fun <reified T : Enum<T>> String?.toEnum(): T? = enumValues<T>().firstOrNull { it.name == this }

private fun String?.toInstant(): Instant? = this?.let { Instant.parse(it) }

/** null si falta el monto o la moneda no es PEN/USD (Money no lo admitiría). */
private fun money(amount: BigDecimal?, currency: String?): Money? {
    if (amount == null || currency == null) return null
    return runCatching { Money(amount, currency) }.getOrNull()
}
