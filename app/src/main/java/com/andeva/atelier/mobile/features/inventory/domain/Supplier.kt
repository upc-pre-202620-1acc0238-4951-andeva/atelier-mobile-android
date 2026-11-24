package com.andeva.atelier.mobile.features.inventory.domain

/** Proveedor homologado (`SupplierResource` de docs/api/inventory.md). */
data class Supplier(
    val id: String,
    val businessName: String,
    val taxId: String,
    val contactName: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val active: Boolean
)
