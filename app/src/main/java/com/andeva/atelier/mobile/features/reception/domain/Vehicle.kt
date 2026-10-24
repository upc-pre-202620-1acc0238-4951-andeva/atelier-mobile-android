package com.andeva.atelier.mobile.features.reception.domain

/** `EngineType` de docs/api/README.md §8. */
enum class EngineType { GASOLINE, DIESEL, ELECTRIC, HYBRID }

/** Vehículo (`VehicleResource`). [currentOwnerId] es el cliente titular; puede faltar. */
data class Vehicle(
    val id: String,
    val plate: String,
    val vin: String?,
    val brand: String,
    val model: String,
    val year: Int?,
    val engineType: EngineType?,
    val currentOwnerId: String?,
    val currentMileage: Int?
)

/** Resultado de buscar por placa (US11 E1): [owner] es null si el vehículo no tiene titular o no se pudo leer. */
data class VehicleWithOwner(
    val vehicle: Vehicle,
    val owner: Customer?
)

/** Alta de vehículo (US11 E2, US44 E2): `CreateVehicleResource`, con el cliente titular. */
data class NewVehicle(
    val plate: String,
    val brand: String,
    val model: String,
    val engineType: EngineType,
    val customerId: String?,
    val vin: String? = null,
    val year: Int? = null
)
