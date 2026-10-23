package com.andeva.atelier.mobile.features.workorders.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "work_orders", indices = [Index(value = ["branch_id", "status"])])
data class WorkOrderEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "internal_number") val internalNumber: Int?,
    @ColumnInfo(name = "branch_id") val branchId: String,
    @ColumnInfo(name = "vehicle_id") val vehicleId: String,              // placa y modelo: tabla vehicles
    @ColumnInfo(name = "customer_id") val customerId: String,            // nombre: tabla customers
    @ColumnInfo(name = "current_bay_id") val currentBayId: String?,
    @ColumnInfo(name = "bay_name") val bayName: String?,
    val status: String,                                                  // DRAFT | IN_PROGRESS | COMPLETED | PAID | CANCELED
    @ColumnInfo(name = "diagnostic_summary") val diagnosticSummary: String?,
    @ColumnInfo(name = "mileage_in") val mileageIn: Int?,
    val subtotal: String?,
    @ColumnInfo(name = "tax_amount") val taxAmount: String?,
    @ColumnInfo(name = "total_amount") val totalAmount: String,
    val currency: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
