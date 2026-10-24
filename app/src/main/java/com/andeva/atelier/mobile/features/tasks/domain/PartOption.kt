package com.andeva.atelier.mobile.features.tasks.domain

import java.math.BigDecimal

/** Repuesto del catálogo para imputar (`PartResource` de docs/api/inventory.md). */
data class PartOption(
    val id: String,
    val name: String,
    val sku: String?,
    val category: String?,
    val basePrice: BigDecimal?,                       // precio de catálogo sin IGV; se copia al imputar (US35 E1)
    val totalStock: BigDecimal,
    val unitOfMeasure: String?
) {
    /** Sin saldo se deshabilita en la UI; el backend respondería 409 (US31 E2). */
    val isAvailable: Boolean get() = totalStock > BigDecimal.ZERO
}
