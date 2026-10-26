package com.andeva.atelier.mobile.features.cashier.infrastructure.local

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** Caché `vouchers` (docs/06-offline-sync.md §4): cabeceras por orden consultada, que se cargan al abrirla. */
@Dao
abstract class VoucherDao {
    @Upsert
    abstract suspend fun upsert(voucher: VoucherEntity)

    @Upsert
    protected abstract suspend fun upsertAll(vouchers: List<VoucherEntity>)

    @Query("DELETE FROM vouchers WHERE work_order_id = :workOrderId")
    protected abstract suspend fun deleteWorkOrder(workOrderId: String)

    /** Recarga completa de los comprobantes de una orden; los de otras órdenes no se tocan. */
    @Transaction
    open suspend fun replaceWorkOrder(workOrderId: String, vouchers: List<VoucherEntity>) {
        deleteWorkOrder(workOrderId)
        upsertAll(vouchers)
    }

    @Query("SELECT * FROM vouchers WHERE id = :id")
    abstract suspend fun findById(id: String): VoucherEntity?

    @Query("SELECT * FROM vouchers WHERE work_order_id = :workOrderId ORDER BY serie, number")
    abstract fun observeByWorkOrder(workOrderId: String): Flow<List<VoucherEntity>>
}
