package com.andeva.atelier.mobile.features.reception.infrastructure.local

import com.andeva.atelier.mobile.features.reception.domain.AffiliationLookup
import com.andeva.atelier.mobile.features.reception.domain.AffiliationStep
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.AppointmentStatus
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.EngineType
import com.andeva.atelier.mobile.features.reception.domain.PlatformAccount
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.AffiliationLookupDto
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.AppointmentDto
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.CustomerDto
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.VehicleDto
import java.time.Instant

fun CustomerDto.toEntity(syncedAt: Long) = CustomerEntity(
    id = id,
    type = type.orEmpty(),
    displayName = displayName ?: listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { companyName.orEmpty() },
    taxId = taxId,
    email = email,
    phone = phone,
    status = status.orEmpty(),
    syncedAt = syncedAt
)

fun CustomerEntity.toDomain() = Customer(
    id = id,
    type = type.toEnum<CustomerType>(),
    displayName = displayName,
    taxId = taxId,
    email = email,
    phone = phone,
    status = status.ifEmpty { null }
)

fun CustomerDto.toDomain(syncedAt: Long) = toEntity(syncedAt).toDomain()

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

fun VehicleEntity.toDomain() = Vehicle(
    id = id,
    plate = plate,
    vin = vin,
    brand = brand,
    model = model,
    year = year,
    engineType = engineType.toEnum<EngineType>(),
    currentOwnerId = currentOwnerId,
    currentMileage = currentMileage
)

fun VehicleDto.toDomain(syncedAt: Long) = toEntity(syncedAt).toDomain()

fun AppointmentDto.toEntity(syncedAt: Long) = AppointmentEntity(
    id = id,
    branchId = branchId,
    customerId = customerId,
    vehicleId = vehicleId,
    scheduledAt = Instant.parse(scheduledAt).toEpochMilli(),
    estimatedDurationMinutes = estimatedDurationMinutes,
    reason = reason,
    status = status.orEmpty(),
    syncedAt = syncedAt
)

fun AppointmentEntity.toDomain(plate: String? = null, customerName: String? = null) = Appointment(
    id = id,
    branchId = branchId,
    customerId = customerId,
    vehicleId = vehicleId,
    scheduledAt = Instant.ofEpochMilli(scheduledAt),
    estimatedDurationMinutes = estimatedDurationMinutes,
    reason = reason,
    status = status.toEnum<AppointmentStatus>(),
    plate = plate,
    customerName = customerName
)

fun AppointmentWithRefs.toDomain() = appointment.toDomain(plate, customerName)

fun AffiliationLookupDto.toDomain(syncedAt: Long) = AffiliationLookup(
    workshopCustomer = workshopCustomer?.toDomain(syncedAt),
    platformAccount = platformAccount?.let { PlatformAccount(it.userId, it.email, it.fullName, it.status) },
    vehicles = vehicles.orEmpty().map { it.toDomain(syncedAt) },
    nextStep = nextStep.toEnum<AffiliationStep>()
)

private inline fun <reified T : Enum<T>> String?.toEnum(): T? = enumValues<T>().firstOrNull { it.name == this }
