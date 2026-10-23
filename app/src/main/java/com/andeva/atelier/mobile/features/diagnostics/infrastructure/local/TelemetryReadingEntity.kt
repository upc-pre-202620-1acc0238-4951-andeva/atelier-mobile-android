package com.andeva.atelier.mobile.features.diagnostics.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "telemetry_readings", indices = [Index(value = ["vehicle_id", "timestamp"], unique = true)])
data class TelemetryReadingEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,
    val timestamp: Long,                                                 // momento de la lectura (UTC)
    @ColumnInfo(name = "engine_temp_celsius") val engineTempCelsius: Double,   // obligatorio en el backend
    @ColumnInfo(name = "engine_rpm") val engineRpm: Int?,
    @ColumnInfo(name = "speed_kmh") val speedKmh: Int?,
    @ColumnInfo(name = "battery_voltage") val batteryVoltage: Double?,
    @ColumnInfo(name = "fuel_percentage") val fuelPercentage: Double?,
    val latitude: Double?,
    val longitude: Double?,
    @ColumnInfo(name = "batch_operation_id") val batchOperationId: String? = null  // id del TELEMETRY_BATCH que la lleva
)
