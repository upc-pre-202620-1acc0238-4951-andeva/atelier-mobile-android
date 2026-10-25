package com.andeva.atelier.mobile.features.bays.infrastructure

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.AuthInterceptor
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.remote.session.TenantSummaryDto
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.bays.infrastructure.remote.WorkBayService
import com.andeva.atelier.mobile.features.bays.infrastructure.repository.BayRepositoryImpl
import com.andeva.atelier.mobile.features.reception.infrastructure.local.VehicleEntity
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderService
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Backend simulado (MockWebServer) + Room en memoria + sesión en memoria para los tests de `bays`. Hora fija:
 * 2026-10-24 09:00 de Lima.
 */
class BayTestEnv {
    val gson = Gson()
    val clock: Clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneOffset.UTC)
    private val server = MockWebServer()

    /** "METHOD /ruta" sin query → respuesta. Lo demás, 404. */
    val responses = ConcurrentHashMap<String, MockResponse>()
    val requests = CopyOnWriteArrayList<RecordedRequest>()

    val context: Context get() = ApplicationProvider.getApplicationContext()

    lateinit var db: AppDatabase
    lateinit var sessionManager: SessionManager
    private lateinit var retrofit: Retrofit

    fun start() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionManager = SessionManager(InMemoryPreferencesDataStore())
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val key = "${request.method} ${request.path!!.substringBefore('?')}"
                return responses[key] ?: error(404, "NOT_FOUND", "No encontrado")
            }
        }
        server.start()
        retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(sessionManager)).build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun stop() {
        server.shutdown()
        db.close()
    }

    fun repository() = BayRepositoryImpl(
        workBayService = retrofit.create(WorkBayService::class.java),
        workOrderService = retrofit.create(WorkOrderService::class.java),
        bayDao = db.bayDao(),
        workOrderDao = db.workOrderDao(),
        cacheMetadataDao = db.cacheMetadataDao(),
        sessionManager = sessionManager,
        gson = gson,
        clock = clock
    )

    /** Supervisor con sede `b1` (o sin sede si [branchId] es null). */
    suspend fun signIn(
        permissions: List<Permission> = listOf(Permission.WORK_ORDERS_READ, Permission.WORK_ORDERS_UPDATE),
        branchId: String? = "b1"
    ) {
        sessionManager.save(
            AuthenticatedUserDto(
                userId = "u1", email = "supervisor@taller.pe", fullName = "Supervisor", token = "jwt", tokenType = "Bearer",
                activeTenant = TenantSummaryDto("t1", "Taller Suárez", null), permissions = permissions.map { it.value },
                refreshToken = "refresh", membershipId = "m1", plan = null
            )
        )
        sessionManager.updateAssignment(branchId, null)
    }

    /** Placa en caché para la orden de [vehicleId] (la lista de órdenes no la trae). */
    suspend fun cachePlate(vehicleId: String = "v1", plate: String = "ABC123") {
        db.vehicleDao().upsert(
            VehicleEntity(vehicleId, plate, null, "Toyota", "Yaris", 2020, "GASOLINE", "c1", 42000, clock.millis())
        )
    }

    fun json(body: Any, code: Int = 200): MockResponse = MockResponse().setResponseCode(code).setBody(gson.toJson(body))

    fun error(code: Int, errorCode: String, message: String): MockResponse =
        MockResponse().setResponseCode(code).setBody("""{"code":"$errorCode","message":"$message","details":[]}""")

    fun bay(
        id: String,
        name: String,
        status: String = "AVAILABLE",
        currentWorkOrderId: String? = null
    ): Map<String, Any?> = mapOf(
        "id" to id, "branchId" to "b1", "name" to name, "bayType" to "LIFT", "status" to status,
        "currentWorkOrderId" to currentWorkOrderId
    )

    fun workOrderSummary(id: String = "wo1", currentBayId: String? = null, bayName: String? = null): Map<String, Any?> = mapOf(
        "id" to id, "tenantId" to "t1", "internalNumber" to 15, "vehicleId" to "v1", "customerId" to "c1",
        "branchId" to "b1", "currentBayId" to currentBayId, "bayName" to bayName, "status" to "IN_PROGRESS",
        "totalAmount" to 118.0, "currency" to "PEN", "createdAt" to "2026-10-24T13:00:00Z"
    )

    fun task(
        id: String = "t1",
        mechanicId: String? = "m9",
        status: String = "IN_PROGRESS",
        startedAt: String? = "2026-10-24T13:00:00Z"
    ): Map<String, Any?> = mapOf(
        "id" to id, "workOrderId" to "wo1", "serviceId" to "srv1", "serviceName" to "Frenos", "mechanicId" to mechanicId,
        "mechanicName" to null, "status" to status, "description" to "Cambio de pastillas", "price" to 100.0,
        "currency" to "PEN", "holdReason" to null, "missingItemDescription" to null, "totalPausedSeconds" to 600,
        "startedAt" to startedAt, "completedAt" to null, "pendingSuspensionReason" to null,
        "pendingSuspensionRequestedAt" to null, "products" to emptyList<Any>(), "evidenceImages" to emptyList<Any>()
    )

    fun workOrderDetail(
        id: String = "wo1",
        tasks: List<Map<String, Any?>> = emptyList(),
        currentBayId: String? = null,
        bayName: String? = null
    ): Map<String, Any?> = workOrderSummary(id, currentBayId, bayName) + mapOf(
        "mileageIn" to 42000, "subtotal" to 100.0, "taxAmount" to 18.0, "diagnosticSummary" to "Ruido al frenar",
        "tasks" to tasks, "proposals" to emptyList<Any>(), "intakeImages" to emptyList<Any>(),
        "updatedAt" to "2026-10-24T13:30:00Z"
    )

    fun requested(method: String, path: String) = requests.filter { it.method == method && it.path!!.substringBefore('?') == path }
}
