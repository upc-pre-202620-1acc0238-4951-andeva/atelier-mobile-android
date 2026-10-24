package com.andeva.atelier.mobile.features.reception.infrastructure.repository

import java.time.LocalDate

/** Alcances de `cache_metadata` y vigencias de docs/06-offline-sync.md §4 para las cachés de recepción. */
object ReceptionCachePolicy {
    const val VEHICLES_TTL_MILLIS = 7 * 24 * 60 * 60 * 1000L
    const val CUSTOMERS_TTL_MILLIS = 7 * 24 * 60 * 60 * 1000L
    const val APPOINTMENTS_TTL_MILLIS = 5 * 60 * 1000L

    fun vehicleScope(vehicleId: String) = "vehicles:$vehicleId"

    fun customerScope(customerId: String) = "customers:$customerId"

    fun appointmentsScope(branchId: String, date: LocalDate) = "appointments:$branchId:$date"
}
