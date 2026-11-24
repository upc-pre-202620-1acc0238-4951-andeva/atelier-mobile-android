package com.andeva.atelier.mobile.features.cashier.domain

/** Perfil fiscal del taller (`TenantResource` de `GET / PUT /api/v1/tenants/current`, US42). */
data class TenantFiscalProfile(
    val id: String,
    val name: String,
    val legalName: String = "",
    val taxId: String = "",
    val status: String? = null
)
