package com.andeva.atelier.mobile.features.reception.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Recepción (US11, US44): clientes, vehículos y citas. Lecturas offline-first con las cachés `customers`,
 * `vehicles` y `appointments` (docs/06-offline-sync.md §4); las altas, la afiliación y la llegada son en línea
 * (06 §2): sin red fallan con `IOException` y no se encolan.
 */
interface ReceptionRepository {
    /** Por placa normalizada. null = primer ingreso (404). Sin red responde con la caché si el vehículo ya se vio. */
    suspend fun findVehicleByPlate(plate: String): Result<VehicleWithOwner?>

    fun observeVehicle(id: String): Flow<Vehicle?>

    fun observeCustomer(id: String): Flow<Customer?>

    /** `GET /customers/{id}` en línea (caja: puede no haberlo visto nunca); lo guarda en la caché `customers`. */
    suspend fun getCustomer(id: String): Result<Customer>

    /** Por documento o nombre. Sin red busca en la caché `customers`. */
    suspend fun searchCustomers(query: String): Result<List<Customer>>

    suspend fun getCustomerVehicles(customerId: String): Result<List<Vehicle>>

    suspend fun registerIndividual(customer: NewIndividualCustomer): Result<Customer>

    suspend fun registerCompany(customer: NewCompanyCustomer): Result<Customer>

    suspend fun registerVehicle(vehicle: NewVehicle): Result<Vehicle>

    suspend fun lookupAffiliation(email: String): Result<AffiliationLookup>

    /** Afilia la cuenta de la app de clientes y guarda en caché el cliente y sus vehículos. */
    suspend fun affiliate(email: String, taxId: String, phone: String?): Result<Customer>

    /** Recarga la agenda de la sede de la sesión para [date] (día de Lima) y los vehículos y clientes que falten. */
    suspend fun refreshAppointments(date: LocalDate): Result<Unit>

    fun observeAppointments(date: LocalDate): Flow<List<Appointment>>

    /** `POST /appointments/{id}/arrive`: devuelve la cita en `ARRIVED` (la orden se crea aparte, H10). */
    suspend fun markArrived(id: String): Result<Appointment>
}
