package com.andeva.atelier.mobile.features.cashier.domain

/** `SeriesConfigurationResource` (docs/api/invoicing.md). */
data class FiscalSeries(
    val id: String,
    val branchId: String?,
    val type: VoucherType?,
    val serie: String,
    val currentCorrelative: Int,
    val active: Boolean
)
