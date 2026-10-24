package com.andeva.atelier.mobile.features.tasks.infrastructure

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.AuthInterceptor
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.remote.session.TenantSummaryDto
import com.andeva.atelier.mobile.core.data.sync.PendingOperationQueue
import com.andeva.atelier.mobile.core.data.sync.SyncScheduler
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.media.PhotoCapture
import com.andeva.atelier.mobile.core.media.SupabaseUploader
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.PartSearchService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.TaskService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.VehicleLookupService
import com.andeva.atelier.mobile.features.tasks.infrastructure.remote.WorkBayLookupService
import com.andeva.atelier.mobile.features.tasks.infrastructure.repository.TaskEvidenceCapture
import com.andeva.atelier.mobile.features.tasks.infrastructure.repository.TaskRepositoryImpl
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
 * Backend simulado (MockWebServer) + Room en memoria + sesión en memoria para los tests de `tasks`.
 * Hora fija: 2026-10-24 09:00 de Lima. Supabase simulado: `compress` copia el archivo y la URL es determinista.
 */
class TaskTestEnv {
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
    lateinit var taskService: TaskService

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
        taskService = retrofit().create(TaskService::class.java)
    }

    fun stop() {
        server.shutdown()
        db.close()
    }

    private fun retrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(sessionManager)).build())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    fun queue() = PendingOperationQueue(db.pendingOperationDao(), sessionManager, syncScheduler, gson)

    fun photoCapture() = PhotoCapture(uploader, db.pendingUploadDao(), queue(), sessionManager, context, clock)

    fun capture() = TaskEvidenceCapture(photoCapture())

    fun repository() = TaskRepositoryImpl(
        taskService = taskService,
        partSearchService = retrofit().create(PartSearchService::class.java),
        vehicleService = retrofit().create(VehicleLookupService::class.java),
        bayService = retrofit().create(WorkBayLookupService::class.java),
        taskDao = db.taskDao(),
        vehicleDao = db.vehicleDao(),
        bayDao = db.bayDao(),
        cacheMetadataDao = db.cacheMetadataDao(),
        pendingOperationDao = db.pendingOperationDao(),
        pendingUploadDao = db.pendingUploadDao(),
        evidenceCapture = capture(),
        sessionManager = sessionManager,
        gson = gson,
        clock = clock
    )

    suspend fun signIn(
        permissions: List<Permission> = listOf(
            Permission.TASKS_READ, Permission.TASKS_TRACK_TIME, Permission.TASKS_UPLOAD_PHOTOS, Permission.TASKS_HOLD_REQUEST,
            Permission.TASKS_COMPLETE, Permission.TASKS_CHARGE_PARTS, Permission.VEHICLES_READ, Permission.BAYS_READ,
            Permission.PARTS_READ
        )
    ) {
        sessionManager.save(
            AuthenticatedUserDto(
                userId = "u1", email = "mecanico@taller.pe", fullName = "Mecánico", token = "jwt", tokenType = "Bearer",
                activeTenant = TenantSummaryDto("t1", "Taller Suárez", null), permissions = permissions.map { it.value },
                refreshToken = "refresh", membershipId = "m1", plan = null
            )
        )
    }

    fun json(body: Any, code: Int = 200): MockResponse = MockResponse().setResponseCode(code).setBody(gson.toJson(body))

    fun error(code: Int, errorCode: String, message: String): MockResponse =
        MockResponse().setResponseCode(code).setBody("""{"code":"$errorCode","message":"$message","details":[]}""")

    fun noContent(): MockResponse = MockResponse().setResponseCode(204)

    fun disconnect(): MockResponse = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)

    fun assigned(
        id: String = "t1",
        status: String = "ASSIGNED",
        vehicleId: String = "v1",
        branchId: String? = "b1",
        bayId: String? = "bay1",
        number: Int = 101
    ): Map<String, Any?> = mapOf(
        "taskId" to id, "workOrderId" to "wo1", "workOrderNumber" to number, "workOrderStatus" to "IN_PROGRESS",
        "vehicleId" to vehicleId, "branchId" to branchId, "bayId" to bayId, "status" to status,
        "description" to "Cambio de pastillas", "estimatedHours" to 1.5, "startedAt" to null, "totalPausedSeconds" to 0
    )

    fun detail(
        id: String = "t1",
        status: String = "IN_PROGRESS",
        holdReason: String? = null,
        pendingSuspensionReason: String? = null,
        products: List<Map<String, Any?>> = emptyList()
    ): Map<String, Any?> = mapOf(
        "id" to id, "workOrderId" to "wo1", "serviceId" to "srv1", "serviceName" to "Frenos", "mechanicId" to "u1",
        "mechanicName" to "Mecánico", "status" to status, "description" to "Cambio de pastillas", "price" to 120.0,
        "currency" to "PEN", "holdReason" to holdReason, "missingItemDescription" to null, "totalPausedSeconds" to 600,
        "startedAt" to "2026-10-24T13:00:00Z", "completedAt" to null, "pendingSuspensionReason" to pendingSuspensionReason,
        "pendingSuspensionRequestedAt" to null, "products" to products, "evidenceImages" to emptyList<Any>()
    )

    fun product(id: String = "tp1", productId: String = "p1"): Map<String, Any?> = mapOf(
        "id" to id, "taskId" to "t1", "productId" to productId, "productName" to "Bujía", "quantity" to 3,
        "unitPrice" to 12.0, "totalAmount" to 36.0, "currency" to "PEN"
    )

    fun requested(method: String, path: String) = requests.filter { it.method == method && it.path!!.substringBefore('?') == path }
}
