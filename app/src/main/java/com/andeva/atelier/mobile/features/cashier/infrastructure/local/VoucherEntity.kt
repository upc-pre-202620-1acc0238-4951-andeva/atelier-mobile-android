package com.andeva.atelier.mobile.features.cashier.infrastructure.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "vouchers", indices = [Index("work_order_id")])
data class VoucherEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "work_order_id") val workOrderId: String?,
    @ColumnInfo(name = "voucher_type") val voucherType: String,          // 01 FACTURA | 03 BOLETA | 07 NOTA_CREDITO
    val serie: String,
    val number: Int,
    @ColumnInfo(name = "total_amount") val totalAmount: String,
    @ColumnInfo(name = "paid_amount") val paidAmount: String,
    @ColumnInfo(name = "pending_balance") val pendingBalance: String,
    @ColumnInfo(name = "payment_status") val paymentStatus: String,      // PENDING | PARTIAL | PAID
    val status: String,                                                  // DRAFT | ISSUED | ACCEPTED_SUNAT | REJECTED_SUNAT | VOIDED
    @ColumnInfo(name = "pdf_url") val pdfUrl: String?,
    @ColumnInfo(name = "synced_at") val syncedAt: Long
)
