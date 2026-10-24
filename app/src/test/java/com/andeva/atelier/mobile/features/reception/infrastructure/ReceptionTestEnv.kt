package com.andeva.atelier.mobile.features.reception.infrastructure

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.AuthInterceptor
import com.andeva.atelier.mobile.core.data.remote.PdfDownloader
import com.andeva.atelier.mobile.core.data.remote.employee.EmployeeService
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.remote.session.TenantSummaryDto
import com.andeva.atelier.mobile.core.data.sync.PendingOperationQueue
import com.andeva.atelier.mobile.core.data.sync.SyncScheduler
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.media.PhotoCapture
import com.andeva.atelier.mobile.core.media.SupabaseUploader
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.AppointmentService
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.CustomerService
import com.andeva.atelier.mobile.features.reception.infrastructure.remote.VehicleService
import com.andeva.atelier.mobile.features.reception.infrastructure.repository.ReceptionRepositoryImpl
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.ServiceCatalogService
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderService
import com.andeva.atelier.mobile.features.workorders.infrastructure.repository.WorkOrderRepositoryImpl
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Backend simulado (MockWebServer) + Room en memoria + sesión en memoria para los tests del paso 6 (`reception` y
 * `workorders`, que comparten cachés). Hora fija: 2026-10-24 09:00 de Lima. Supabase simulado como en `tasks`.
 */
class ReceptionTestEnv {
    val gson = Gson()
    val clock: Clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneOffset.UTC)
    private val server = MockWebServer()

    /** "METHOD /ruta" sin query → respuesta. Lo que no está responde 404. */
    val responses = ConcurrentHashMap<String, MockResponse>()
    val requests = CopyOnWriteArrayList<RecordedRequest>()

    val syncScheduler = mockk<SyncScheduler>(relaxed = true)
    val uploader = mockk<SupabaseUploader>().also { uploader ->
        coEvery { uploader.compress(any(), any()) } coAnswers {
            val target = secondArg<File>()
            target.parentFile?.mkdirs()
            firstArg<File>().copyTo(target, overwrite = true)
        }
        every { uploader.publicUrlFor(any()) } answers { "https://demo.supabase.co/storage/v1/object/public/atelier/${firstArg<String>()}" }
    }

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

    fun <T> service(type: Class<T>): T = retrofit.create(type)

    fun queue() = PendingOperationQueue(db.pendingOperationDao(), sessionManager, syncScheduler, gson)

    fun receptionRepository() = ReceptionRepositoryImpl(
        customerService = service(CustomerService::class.java),
        vehicleService = service(VehicleService::class.java),
        appointmentService = service(AppointmentService::class.java),
        customerDao = db.customerDao(),
        vehicleDao = db.vehicleDao(),
        appointmentDao = db.appointmentDao(),
        cacheMetadataDao = db.cacheMetadataDao(),
        sessionManager = sessionManager,
        gson = gson,
        clock = clock
    )

    fun workOrderRepository() = WorkOrderRepositoryImpl(
        workOrderService = service(WorkOrderService::class.java),
        serviceCatalogService = service(ServiceCatalogService::class.java),
        employeeService = service(EmployeeService::class.java),
        workOrderDao = db.workOrderDao(),
        cacheMetadataDao = db.cacheMetadataDao(),
        pendingOperationDao = db.pendingOperationDao(),
        pendingUploadDao = db.pendingUploadDao(),
        photoCapture = PhotoCapture(uploader, db.pendingUploadDao(), queue(), sessionManager, context, clock),
        pdfDownloader = PdfDownloader(context, gson),
        sessionManager = sessionManager,
        gson = gson,
        clock = clock
    )

    /** Asesor de servicio con sede `b1` (y lo que haga falta para leer citas y órdenes). */
    suspend fun signIn(
        permissions: List<Permission> = listOf(
            Permission.CUSTOMERS_READ, Permission.CUSTOMERS_CREATE, Permission.VEHICLES_READ, Permission.VEHICLES_CREATE,
            Permission.APPOINTMENTS_READ, Permission.APPOINTMENTS_UPDATE, Permission.WORK_ORDERS_READ,
            Permission.WORK_ORDERS_CREATE, Permission.WORK_ORDERS_UPDATE, Permission.SERVICES_READ,
            Permission.INSPECTIONS_UPLOAD_PHOTOS, Permission.PROPOSALS_CREATE, Permission.QUOTATIONS_READ
        ),
        branchId: String? = "b1"
    ) {
        sessionManager.save(
            AuthenticatedUserDto(
                userId = "u1", email = "asesor@taller.pe", fullName = "Asesor", token = "jwt", tokenType = "Bearer",
                activeTenant = TenantSummaryDto("t1", "Taller Suárez", null), permissions = permissions.map { it.value },
                refreshToken = "refresh", membershipId = "m1", plan = null
            )
        )
        sessionManager.updateAssignment(branchId, null)
    }

    fun json(body: Any, code: Int = 200): MockResponse = MockResponse().setResponseCode(code).setBody(gson.toJson(body))

    fun error(code: Int, errorCode: String, message: String): MockResponse =
        MockResponse().setResponseCode(code).setBody("""{"code":"$errorCode","message":"$message","details":[]}""")

    fun disconnect(): MockResponse = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)

    fun customer(id: String = "c1", name: String = "Ana Torres", taxId: String = "45678912"): Map<String, Any?> = mapOf(
        "id" to id, "tenantId" to "t1", "type" to "INDIVIDUAL", "firstName" to "Ana", "lastName" to "Torres",
        "displayName" to name, "taxId" to taxId, "email" to "ana@correo.pe", "phone" to "999888777", "status" to "ACTIVE"
    )

    fun vehicle(id: String = "v1", plate: String = "ABC123", ownerId: String? = "c1"): Map<String, Any?> = mapOf(
        "id" to id, "plate" to plate, "vin" to null, "brand" to "Toyota", "model" to "Yaris", "year" to 2020,
        "engineType" to "GASOLINE", "currentOwnerId" to ownerId, "currentMileage" to 42000
    )

    fun appointment(id: String = "a1", status: String = "CONFIRMED", scheduledAt: String = "2026-10-24T15:00:00Z"): Map<String, Any?> = mapOf(
        "id" to id, "tenantId" to "t1", "branchId" to "b1", "customerId" to "c1", "vehicleId" to "v1",
        "scheduledAt" to scheduledAt, "estimatedDurationMinutes" to 60, "reason" to "Mantenimiento", "status" to status
    )

    fun workOrderSummary(id: String = "wo1", status: String = "DRAFT", vehicleId: String = "v1", total: Double = 0.0): Map<String, Any?> = mapOf(
        "id" to id, "tenantId" to "t1", "internalNumber" to 15, "vehicleId" to vehicleId, "customerId" to "c1",
        "branchId" to "b1", "currentBayId" to null, "bayName" to null, "status" to status, "totalAmount" to total,
        "currency" to "PEN", "createdAt" to "2026-10-24T13:00:00Z"
    )

    fun workOrder(id: String = "wo1", status: String = "DRAFT", mileageIn: Int = 42000, total: Double = 118.0): Map<String, Any?> = mapOf(
        "id" to id, "tenantId" to "t1", "internalNumber" to 15, "vehicleId" to "v1", "customerId" to "c1", "branchId" to "b1",
        "currentBayId" to null, "mileageIn" to mileageIn, "status" to status, "subtotal" to 100.0, "taxAmount" to 18.0,
        "totalAmount" to total, "currency" to "PEN"
    )

    fun workOrderDetail(
        id: String = "wo1",
        tasks: List<Map<String, Any?>> = emptyList(),
        proposals: List<Map<String, Any?>> = emptyList()
    ): Map<String, Any?> = workOrder(id) + mapOf(
        "bayName" to null, "diagnosticSummary" to "Ruido al frenar", "tasks" to tasks, "proposals" to proposals,
        "intakeImages" to listOf(
            mapOf("id" to "img1", "workOrderId" to id, "imageUrl" to "https://x/1.jpg", "description" to "Raspón", "uploadedAt" to "2026-10-24T13:10:00Z")
        ),
        "createdAt" to "2026-10-24T13:00:00Z", "updatedAt" to "2026-10-24T13:30:00Z"
    )

    fun task(id: String = "t1", pendingSuspensionRequestedAt: String? = null): Map<String, Any?> = mapOf(
        "id" to id, "workOrderId" to "wo1", "serviceId" to "srv1", "serviceName" to "Standard Service", "mechanicId" to "m9",
        "mechanicName" to null, "status" to "IN_PROGRESS", "description" to "Cambio de pastillas", "price" to 100.0,
        "currency" to "PEN", "holdReason" to null, "missingItemDescription" to null, "totalPausedSeconds" to 0,
        "startedAt" to "2026-10-24T13:00:00Z", "completedAt" to null,
        "pendingSuspensionReason" to pendingSuspensionRequestedAt?.let { "Pastillas defectuosas" },
        "pendingSuspensionRequestedAt" to pendingSuspensionRequestedAt, "products" to emptyList<Any>(),
        "evidenceImages" to emptyList<Any>()
    )

    fun requested(method: String, path: String) = requests.filter { it.method == method && it.path!!.substringBefore('?') == path }
}
