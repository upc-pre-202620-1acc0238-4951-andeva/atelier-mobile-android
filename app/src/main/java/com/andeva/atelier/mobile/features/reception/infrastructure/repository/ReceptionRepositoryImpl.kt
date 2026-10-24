package com.andeva.atelier.mobile.features.reception.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataDao
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataEntity
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.reception.domain.AffiliationLookup
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.NewCompanyCustomer
import com.andeva.atelier.mobile.features.reception.domain.NewIndividualCustomer
import com.andeva.atelier.mobile.features.reception.domain.NewVehicle
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import com.andeva.atelier.mobile.features.reception.domain.ReceptionValidation
import com.andeva.atelier.mobile.features.reception.domain.Vehicle
import com.andeva.atelier.mobile.features.reception.domain.VehicleWithOwner
import com.andeva.atelier.mobile.features.reception.infrastructure.local.AppointmentDao
import com.andeva.atelier.mobile.features.reception.infrastructure.local.CustomerDao
import com.andeva.atelier.mobile.features.reception.infrastructure.local.VehicleDao
import com.andeva.atelier.mobile.features.reception.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.reception.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.AffiliateCustomerRequestDto
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.AppointmentService
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.CreateCompanyCustomerRequestDto
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.CreateIndividualCustomerRequestDto
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.CreateVehicleRequestDto
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.CustomerDto
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.CustomerService
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.VehicleDto
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.VehicleService
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Offline-first para leer (docs/06-offline-sync.md §4): lo que llega del backend se guarda en `customers`,
 * `vehicles` y `appointments` y la UI observa Room. Altas, afiliación y llegada son en línea (06 §2).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceptionRepositoryImpl @Inject constructor(
    private val customerService: CustomerService,
    private val vehicleService: VehicleService,
    private val appointmentService: AppointmentService,
    private val customerDao: CustomerDao,
    private val vehicleDao: VehicleDao,
    private val appointmentDao: AppointmentDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock
) : ReceptionRepository {

    override suspend fun findVehicleByPlate(plate: String): Result<VehicleWithOwner?> {
        val normalized = ReceptionValidation.normalizePlate(plate)
        val result = apiCall(gson, { vehicleService.getByPlate(normalized) }) { it }
        val error = result.exceptionOrNull()
        return when {
            error == null -> {
                val vehicle = saveVehicle(result.getOrThrow())
                Result.success(VehicleWithOwner(vehicle, vehicle.currentOwnerId?.let { ownerOf(it) }))
            }
            error is ApiException && error.httpStatus == 404 -> Result.success(null)        // primer ingreso (US11 E2)
            error is IOException -> cachedByPlate(normalized)?.let { Result.success(it) } ?: Result.failure(error)
            else -> Result.failure(error)
        }
    }

    override fun observeVehicle(id: String): Flow<Vehicle?> = vehicleDao.observeById(id).map { it?.toDomain() }

    override fun observeCustomer(id: String): Flow<Customer?> = customerDao.observeById(id).map { it?.toDomain() }

    override suspend fun searchCustomers(query: String): Result<List<Customer>> {
        val text = query.trim()
        val result = apiCall(gson, { customerService.search(text.ifEmpty { null }) }) { it }
            .map { dtos -> dtos.map { saveCustomer(it) } }
        val error = result.exceptionOrNull()
        if (error !is IOException || text.isEmpty()) return result
        return Result.success(customerDao.search(text).map { it.toDomain() })
    }

    override suspend fun getCustomerVehicles(customerId: String): Result<List<Vehicle>> =
        apiCall(gson, { customerService.getVehicles(customerId) }) { it }
            .map { dtos -> dtos.map { saveVehicle(it) } }

    override suspend fun registerIndividual(customer: NewIndividualCustomer): Result<Customer> {
        val body = CreateIndividualCustomerRequestDto(
            firstName = customer.firstName.trim(),
            lastName = customer.lastName.trim(),
            taxId = customer.taxId.trim(),
            email = customer.email.blankToNull(),
            phone = customer.phone.blankToNull()
        )
        return apiCall(gson, { customerService.createIndividual(body) }) { it }.map { saveCustomer(it) }
    }

    override suspend fun registerCompany(customer: NewCompanyCustomer): Result<Customer> {
        val body = CreateCompanyCustomerRequestDto(
            companyName = customer.companyName.trim(),
            taxId = customer.taxId.trim(),
            email = customer.email.blankToNull(),
            phone = customer.phone.blankToNull()
        )
        return apiCall(gson, { customerService.createCompany(body) }) { it }.map { saveCustomer(it) }
    }

    override suspend fun registerVehicle(vehicle: NewVehicle): Result<Vehicle> {
        val body = CreateVehicleRequestDto(
            plate = ReceptionValidation.normalizePlate(vehicle.plate),
            brand = vehicle.brand.trim(),
            model = vehicle.model.trim(),
            engineType = vehicle.engineType.name,
            customerId = vehicle.customerId,
            vin = vehicle.vin.blankToNull(),
            year = vehicle.year
        )
        return apiCall(gson, { vehicleService.create(body) }) { it }.map { saveVehicle(it) }
    }

    override suspend fun lookupAffiliation(email: String): Result<AffiliationLookup> =
        apiCall(gson, { customerService.lookupAffiliation(email.trim()) }) { it.toDomain(clock.millis()) }

    /** El backend vincula los vehículos de la cuenta; se leen después para dejarlos en caché (si falla, se sigue). */
    override suspend fun affiliate(email: String, taxId: String, phone: String?): Result<Customer> {
        val body = AffiliateCustomerRequestDto(email.trim(), taxId.trim(), phone.blankToNull())
        return apiCall(gson, { customerService.affiliate(body) }) { it }
            .map { saveCustomer(it) }
            .onSuccess { customer -> getCustomerVehicles(customer.id) }
    }

    override suspend fun refreshAppointments(date: LocalDate): Result<Unit> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        val branchId = session.branchId ?: return Result.failure(MissingBranchException())
        if (!session.has(Permission.APPOINTMENTS_READ)) return Result.success(Unit)
        val now = clock.millis()
        val (from, to) = dayRange(date)
        return apiCall(gson, { appointmentService.getAppointments(branchId, date.toString()) }) { it }
            .mapCatching { dtos ->
                val rows = dtos.map { it.toEntity(now) }
                appointmentDao.replaceDay(branchId, from, to, rows)
                cacheMetadataDao.upsert(CacheMetadataEntity(ReceptionCachePolicy.appointmentsScope(branchId, date), now, rows.size))
                rows
            }
            .onSuccess { rows -> enrich(rows.map { it.vehicleId }, rows.map { it.customerId }, session, now) }
            .map { }
    }

    override fun observeAppointments(date: LocalDate): Flow<List<Appointment>> = sessionManager.session
        .map { it?.branchId }
        .distinctUntilChanged()
        .flatMapLatest { branchId ->
            if (branchId == null) return@flatMapLatest flowOf(emptyList())
            val (from, to) = dayRange(date)
            appointmentDao.observeDay(branchId, from, to).map { rows -> rows.map { it.toDomain() } }
        }

    override suspend fun markArrived(id: String): Result<Appointment> =
        apiCall(gson, { appointmentService.arrive(id) }) { it }.map { dto ->
            val row = dto.toEntity(clock.millis())
            appointmentDao.upsert(row)
            row.toDomain()
        }

    private suspend fun saveVehicle(dto: VehicleDto): Vehicle {
        val now = clock.millis()
        val row = dto.toEntity(now)
        vehicleDao.saveFromServer(row)
        cacheMetadataDao.upsert(CacheMetadataEntity(ReceptionCachePolicy.vehicleScope(dto.id), now, 1))
        return row.toDomain()
    }

    private suspend fun saveCustomer(dto: CustomerDto): Customer {
        val now = clock.millis()
        val row = dto.toEntity(now)
        customerDao.upsert(row)
        cacheMetadataDao.upsert(CacheMetadataEntity(ReceptionCachePolicy.customerScope(dto.id), now, 1))
        return row.toDomain()
    }

    /** El titular desde la red; si no se puede leer (sin permiso, sin red), el de la caché o null. */
    private suspend fun ownerOf(customerId: String): Customer? =
        apiCall(gson, { customerService.getCustomer(customerId) }) { it }
            .map { saveCustomer(it) }
            .getOrElse { customerDao.findById(customerId)?.toDomain() }

    private suspend fun cachedByPlate(plate: String): VehicleWithOwner? {
        val vehicle = vehicleDao.findByPlate(plate)?.toDomain() ?: return null
        return VehicleWithOwner(vehicle, vehicle.currentOwnerId?.let { customerDao.findById(it)?.toDomain() })
    }

    /** Placa y nombre para la agenda: solo lo que falta o venció, y solo con permiso para leerlo. */
    private suspend fun enrich(vehicleIds: List<String>, customerIds: List<String>, session: Session, now: Long) {
        if (session.has(Permission.VEHICLES_READ)) {
            vehicleIds.distinct()
                .filter { isStale(ReceptionCachePolicy.vehicleScope(it), ReceptionCachePolicy.VEHICLES_TTL_MILLIS, now) }
                .forEach { id -> refresh { vehicleService.getVehicle(id) }?.let { saveVehicle(it) } }
        }
        if (session.has(Permission.CUSTOMERS_READ)) {
            customerIds.distinct()
                .filter { isStale(ReceptionCachePolicy.customerScope(it), ReceptionCachePolicy.CUSTOMERS_TTL_MILLIS, now) }
                .forEach { id -> refresh { customerService.getCustomer(id) }?.let { saveCustomer(it) } }
        }
    }

    private suspend fun <T> refresh(call: suspend () -> Response<T>): T? = apiCall(gson, call) { it }.getOrNull()

    private suspend fun isStale(scope: String, ttlMillis: Long, now: Long): Boolean {
        val lastSyncedAt = cacheMetadataDao.get(scope)?.lastSyncedAt ?: return true
        return now - lastSyncedAt >= ttlMillis
    }

    private fun dayRange(date: LocalDate): Pair<Long, Long> =
        BusinessTime.startOfDay(date).toEpochMilli() to BusinessTime.startOfDay(date.plusDays(1)).toEpochMilli()

    private fun String?.blankToNull() = this?.trim()?.takeIf { it.isNotEmpty() }
}
