package com.andeva.atelier.mobile.features.tasks.infrastructure.local

import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.bays.infrastructure.local.BayEntity
import com.andeva.atelier.mobile.features.reception.infrastructure.local.VehicleEntity
import com.andeva.atelier.mobile.features.tasks.domain.HoldReason
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskEvidence
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.AssignedTaskDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.PartDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.TaskProductDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.VehicleDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkBayDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkOrderTaskDto
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkOrderTaskImageDto
import java.math.BigDecimal
import java.time.Instant

fun AssignedTaskDto.toEntity(syncedAt: Long) = TaskEntity(
    id = taskId,
    workOrderId = workOrderId,
    workOrderNumber = workOrderNumber,
    workOrderStatus = workOrderStatus,
    vehicleId = vehicleId,
    branchId = branchId,
    bayId = bayId,
    serviceName = null,
    mechanicId = null,
    status = status.orEmpty(),
    description = description.orEmpty(),
    estimatedHours = estimatedHours?.toPlainString(),
    holdReason = null,
    missingItemDescription = null,
    pendingSuspensionReason = null,
    startedAt = startedAt.toEpochMillis(),
    totalPausedSeconds = totalPausedSeconds ?: 0,
    syncedAt = syncedAt
)

fun TaskWithRefs.toDomain() = Task(
    id = TaskId(task.id),
    workOrderId = task.workOrderId,
    workOrderNumber = task.workOrderNumber,
    workOrderStatus = task.workOrderStatus,
    vehicleId = task.vehicleId,
    plate = plate,
    branchId = task.branchId,
    bayId = task.bayId,
    bayName = bayName,
    serviceName = task.serviceName,
    mechanicId = task.mechanicId,
    status = task.status.toTaskStatus(),
    description = task.description,
    estimatedHours = task.estimatedHours?.let { BigDecimal(it) },
    holdReason = task.holdReason.toHoldReason(),
    missingItemDescription = task.missingItemDescription,
    pendingSuspensionReason = task.pendingSuspensionReason,
    startedAt = task.startedAt?.let { Instant.ofEpochMilli(it) },
    totalPausedSeconds = task.totalPausedSeconds
)

fun WorkOrderTaskDto.toDomain() = TaskDetail(
    id = TaskId(id),
    workOrderId = workOrderId,
    serviceId = serviceId,
    serviceName = serviceName,
    mechanicId = mechanicId,
    mechanicName = mechanicName,
    status = status.toTaskStatus(),
    description = description.orEmpty(),
    price = money(price, currency),
    holdReason = holdReason.toHoldReason(),
    missingItemDescription = missingItemDescription,
    totalPausedSeconds = totalPausedSeconds ?: 0,
    startedAt = startedAt.toInstant(),
    completedAt = completedAt.toInstant(),
    pendingSuspensionReason = pendingSuspensionReason,
    pendingSuspensionRequestedAt = pendingSuspensionRequestedAt.toInstant(),
    products = products.orEmpty().map { it.toDomain() },
    evidenceImages = evidenceImages.orEmpty().map { it.toDomain() }
)

fun TaskProductDto.toDomain() = TaskProduct(
    id = id,
    productId = productId,
    productName = productName,
    quantity = quantity ?: BigDecimal.ZERO,
    unitPrice = money(unitPrice, currency),
    totalAmount = money(totalAmount, currency)
)

fun WorkOrderTaskImageDto.toDomain() = TaskEvidence(
    id = id,
    imageUrl = imageUrl,
    description = description,
    uploadedAt = uploadedAt.toInstant()
)

fun PartDto.toDomain() = PartOption(
    id = id,
    name = name.orEmpty(),
    sku = sku,
    category = category,
    basePrice = basePrice,
    totalStock = totalStock ?: BigDecimal.ZERO,
    unitOfMeasure = unitOfMeasure
)

fun VehicleDto.toEntity(syncedAt: Long) = VehicleEntity(
    id = id,
    plate = plate,
    vin = vin,
    brand = brand.orEmpty(),
    model = model.orEmpty(),
    year = year,
    engineType = engineType,
    currentOwnerId = currentOwnerId,
    currentMileage = currentMileage,
    syncedAt = syncedAt
)

/** Sin `branchId` en la respuesta se usa la sede consultada, para que caiga en su alcance `bays:{branchId}`. */
fun WorkBayDto.toEntity(fallbackBranchId: String, syncedAt: Long) = BayEntity(
    id = id,
    branchId = branchId ?: fallbackBranchId,
    name = name.orEmpty(),
    bayType = bayType.orEmpty(),
    status = status.orEmpty(),
    currentWorkOrderId = currentWorkOrderId,
    syncedAt = syncedAt
)

private fun String?.toTaskStatus() = TaskStatus.entries.firstOrNull { it.name == this }

private fun String?.toHoldReason() = HoldReason.entries.firstOrNull { it.name == this }

private fun String?.toInstant(): Instant? = this?.let { Instant.parse(it) }

internal fun String?.toEpochMillis(): Long? = toInstant()?.toEpochMilli()

/** null si falta el monto o la moneda no es PEN/USD (Money no lo admitiría). */
private fun money(amount: BigDecimal?, currency: String?): Money? {
    if (amount == null || currency == null) return null
    return runCatching { Money(amount, currency) }.getOrNull()
}
