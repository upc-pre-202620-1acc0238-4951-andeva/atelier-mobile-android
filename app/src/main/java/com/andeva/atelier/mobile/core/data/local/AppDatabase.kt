package com.andeva.atelier.mobile.core.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.andeva.atelier.mobile.core.data.sync.CacheMaintenanceDao
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataDao
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataEntity
import com.andeva.atelier.mobile.core.data.sync.PendingOperationDao
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.core.data.sync.PendingUploadDao
import com.andeva.atelier.mobile.core.data.sync.PendingUploadEntity
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.AttendanceDao
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.AttendanceEntity
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.BranchDao
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.BranchEntity
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.ShiftDao
import com.andeva.atelier.mobile.features.attendance.infrastructure.local.ShiftEntity
import com.andeva.atelier.mobile.features.bays.infrastructure.local.BayDao
import com.andeva.atelier.mobile.features.bays.infrastructure.local.BayEntity
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.VoucherDao
import com.andeva.atelier.mobile.features.cashier.infrastructure.local.VoucherEntity
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.PredictiveAlertDao
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.PredictiveAlertEntity
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.TelemetryReadingDao
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.TelemetryReadingEntity
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.VehicleFaultDao
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.local.VehicleFaultEntity
import com.andeva.atelier.mobile.features.inventory.infrastructure.local.BatchDao
import com.andeva.atelier.mobile.features.inventory.infrastructure.local.BatchEntity
import com.andeva.atelier.mobile.features.inventory.infrastructure.local.PartDao
import com.andeva.atelier.mobile.features.inventory.infrastructure.local.PartEntity
import com.andeva.atelier.mobile.features.reception.infrastructure.local.AppointmentDao
import com.andeva.atelier.mobile.features.reception.infrastructure.local.AppointmentEntity
import com.andeva.atelier.mobile.features.reception.infrastructure.local.CustomerDao
import com.andeva.atelier.mobile.features.reception.infrastructure.local.CustomerEntity
import com.andeva.atelier.mobile.features.reception.infrastructure.local.VehicleDao
import com.andeva.atelier.mobile.features.reception.infrastructure.local.VehicleEntity
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.TaskDao
import com.andeva.atelier.mobile.features.tasks.infrastructure.local.TaskEntity
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderDao
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderEntity

/** Única base Room de la app (docs/06-offline-sync.md §5). Cada feature agrega aquí su DAO. */
@Database(
    entities = [
        PendingOperationEntity::class, PendingUploadEntity::class, CacheMetadataEntity::class,
        BranchEntity::class, ShiftEntity::class, AttendanceEntity::class,
        TaskEntity::class, WorkOrderEntity::class, BayEntity::class,
        CustomerEntity::class, VehicleEntity::class, AppointmentEntity::class,
        PartEntity::class, BatchEntity::class, VoucherEntity::class,
        TelemetryReadingEntity::class, VehicleFaultEntity::class, PredictiveAlertEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun pendingUploadDao(): PendingUploadDao
    abstract fun cacheMetadataDao(): CacheMetadataDao
    abstract fun cacheMaintenanceDao(): CacheMaintenanceDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun shiftDao(): ShiftDao
    abstract fun branchDao(): BranchDao
    abstract fun taskDao(): TaskDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun customerDao(): CustomerDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun bayDao(): BayDao
    abstract fun workOrderDao(): WorkOrderDao
    abstract fun partDao(): PartDao
    abstract fun batchDao(): BatchDao
    abstract fun voucherDao(): VoucherDao
    abstract fun telemetryReadingDao(): TelemetryReadingDao
    abstract fun vehicleFaultDao(): VehicleFaultDao
    abstract fun predictiveAlertDao(): PredictiveAlertDao

    companion object {
        const val NAME = "atelier.db"
    }
}
