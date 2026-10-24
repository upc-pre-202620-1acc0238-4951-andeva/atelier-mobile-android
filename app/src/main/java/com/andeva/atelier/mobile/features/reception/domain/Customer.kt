package com.andeva.atelier.mobile.features.reception.domain

/** `CustomerType` de docs/api/README.md §8. */
enum class CustomerType { INDIVIDUAL, COMPANY }

/** Cliente del taller (`CustomerResource`). [displayName] es el nombre o la razón social que arma el backend. */
data class Customer(
    val id: String,
    val type: CustomerType?,
    val displayName: String,
    val taxId: String?,
    val email: String?,
    val phone: String?,
    val status: String?
)

/** Alta de persona natural (US11 E2, US44 E2): `CreateIndividualCustomerResource`. */
data class NewIndividualCustomer(
    val firstName: String,
    val lastName: String,
    val taxId: String,
    val email: String? = null,
    val phone: String? = null
)

/** Alta de empresa (US11 E2): `CreateCompanyCustomerResource`. */
data class NewCompanyCustomer(
    val companyName: String,
    val taxId: String,
    val email: String? = null,
    val phone: String? = null
)
