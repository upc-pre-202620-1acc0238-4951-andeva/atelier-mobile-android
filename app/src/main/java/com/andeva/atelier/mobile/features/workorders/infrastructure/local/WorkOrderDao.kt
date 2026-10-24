package com.andeva.atelier.mobile.features.workorders.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Una fila de `work_orders` con la placa (`vehicles`) y el nombre del cliente (`customers`) si ya están en caché. */
data class WorkOrderWithRefs(
    @Embedded val workOrder: WorkOrderEntity,
    @ColumnInfo(name = "plate") val plate: String?,
    @ColumnInfo(name = "customer_name") val customerName: String?
)

/** Caché `work_orders` (docs/06-offline-sync.md §4, órdenes no cerradas de la sede · 5 min). */
@Dao
abstract class WorkOrderDao {
    @Upsert
    protected abstract suspend fun upsertAll(workOrders: List<WorkOrderEntity>)

    @Query("SELECT * FROM work_orders WHERE branch_id = :branchId")
    protected abstract suspend fun findBranch(branchId: String): List<WorkOrderEntity>

    @Query("DELETE FROM work_orders WHERE branch_id = :branchId")
    protected abstract suspend fun deleteBranch(branchId: String)

    @Query("SELECT * FROM work_orders WHERE id = :id")
    abstract suspend fun findById(id: String): WorkOrderEntity?

    @Query("DELETE FROM work_orders WHERE id = :id")
    abstract suspend fun delete(id: String)

    /**
     * Recarga completa del alcance `work_orders:{branchId}`. La lista (`WorkOrderSummaryResource`) no trae subtotal,
     * IGV, kilometraje ni diagnóstico: se conservan los que ya vinieron con el detalle.
     */
    @Transaction
    open suspend fun replaceBranch(branchId: String, workOrders: List<WorkOrderEntity>) {
        val previous = findBranch(branchId).associateBy { it.id }
        deleteBranch(branchId)
        upsertAll(workOrders.map { row -> previous[row.id]?.let { row.keepDetailOf(it) } ?: row })
    }

    /** Cabecera que devolvió una acción o el detalle; lo que no trae se conserva de la fila guardada. */
    @Transaction
    open suspend fun saveHeader(workOrder: WorkOrderEntity) {
        val previous = findById(workOrder.id)
        upsertAll(listOf(previous?.let { workOrder.keepDetailOf(it) } ?: workOrder))
    }

    @Query(
        "SELECT w.*, v.plate AS plate, c.display_name AS customer_name FROM work_orders w " +
            "LEFT JOIN vehicles v ON v.id = w.vehicle_id LEFT JOIN customers c ON c.id = w.customer_id " +
            "WHERE w.branch_id = :branchId ORDER BY w.internal_number DESC"
    )
    abstract fun observeBranch(branchId: String): Flow<List<WorkOrderWithRefs>>

    @Query(
        "SELECT w.*, v.plate AS plate, c.display_name AS customer_name FROM work_orders w " +
            "LEFT JOIN vehicles v ON v.id = w.vehicle_id LEFT JOIN customers c ON c.id = w.customer_id WHERE w.id = :id"
    )
    abstract fun observeById(id: String): Flow<WorkOrderWithRefs?>
}

private fun WorkOrderEntity.keepDetailOf(previous: WorkOrderEntity) = copy(
    internalNumber = internalNumber ?: previous.internalNumber,
    bayName = bayName ?: previous.bayName.takeIf { currentBayId == previous.currentBayId },
    diagnosticSummary = diagnosticSummary ?: previous.diagnosticSummary,
    mileageIn = mileageIn ?: previous.mileageIn,
    subtotal = subtotal ?: previous.subtotal.takeIf { totalAmount == previous.totalAmount },
    taxAmount = taxAmount ?: previous.taxAmount.takeIf { totalAmount == previous.totalAmount }
)
