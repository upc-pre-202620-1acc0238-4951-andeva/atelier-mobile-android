package com.andeva.atelier.mobile.features.reception.infrastructure.repository

import java.time.LocalDate

/** Alcances de `cache_metadata` y vigencias de docs/06-offline-sync.md §4 para las cachés de recepción. */
object ReceptionCachePolicy {
    const val VEHICLES_TTL_MILLIS = 7 * 24 * 60 * 60 * 1000L
    const val CUSTOMERS_TTL_MILLIS = 7 * 24 * 60 * 60 * 1000L
    const val PORTFOLIO_TTL_MILLIS = 15 * 60 * 1000L
    const val APPOINTMENTS_TTL_MILLIS = 5 * 60 * 1000L
    const val PAGE_SIZE = 50
    const val MAX_PAGES = 20
    const val SCOPE_CUSTOMERS_PORTFOLIO = "customers:portfolio"

    fun vehicleScope(vehicleId: String) = "vehicles:$vehicleId"

    fun customerScope(customerId: String) = "customers:$customerId"

    fun appointmentsScope(branchId: String, date: LocalDate) = "appointments:$branchId:$date"
}
