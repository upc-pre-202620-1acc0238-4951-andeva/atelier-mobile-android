package com.andeva.atelier.mobile.features.reception.domain

/** Qué sigue tras buscar el correo (US44): ya es cliente, afiliar su cuenta o registrarlo presencialmente. */
enum class AffiliationStep { ALREADY_CUSTOMER, AFFILIATE, REGISTER_WALK_IN }

/** Cuenta de la app de clientes con ese correo. */
data class PlatformAccount(
    val userId: String,
    val email: String,
    val fullName: String?,
    val status: String?
)

/** Respuesta de `GET /customers/affiliation-lookup`: [vehicles] son los que la cuenta ya registró en su app. */
data class AffiliationLookup(
    val workshopCustomer: Customer?,
    val platformAccount: PlatformAccount?,
    val vehicles: List<Vehicle>,
    val nextStep: AffiliationStep?
)
