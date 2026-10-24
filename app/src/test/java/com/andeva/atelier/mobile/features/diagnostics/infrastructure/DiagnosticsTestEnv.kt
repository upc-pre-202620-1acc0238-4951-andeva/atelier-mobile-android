package com.andeva.atelier.mobile.features.diagnostics.infrastructure

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.AuthInterceptor
import com.andeva.atelier.mobile.core.data.remote.PdfDownloader
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.remote.session.TenantSummaryDto
import com.andeva.atelier.mobile.core.data.sync.PendingOperationQueue
import com.andeva.atelier.mobile.core.data.sync.SyncScheduler
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.remote.IotService
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.repository.DiagnosticsRepositoryImpl
import com.andeva.atelier.mobile.features.diagnostics.infrastructure.repository.HealthReportRepositoryImpl
import com.google.gson.Gson
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Backend simulado (MockWebServer) + Room en memoria + sesión y cola reales para los tests de `diagnostics`. Hora fija:
 * 2026-10-24 09:00 de Lima.
 */
class DiagnosticsTestEnv {
    val gson = Gson()
    val clock: Clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneOffset.UTC)
    private val server = MockWebServer()

    /** "METHOD /ruta" sin query → respuesta. Lo demás, 404. */
    val responses = ConcurrentHashMap<String, MockResponse>()
    val requests = CopyOnWriteArrayList<RecordedRequest>()
    val syncScheduler = mockk<SyncScheduler>(relaxed = true)

    val context: Context get() = ApplicationProvider.getApplicationContext()

    lateinit var db: AppDatabase
    lateinit var sessionManager: SessionManager
    lateinit var iotService: IotService

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
        iotService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(sessionManager)).build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(IotService::class.java)
    }

    fun stop() {
        server.shutdown()
        db.close()
    }

    fun queue() = PendingOperationQueue(db.pendingOperationDao(), sessionManager, syncScheduler, gson)

    fun repository() = DiagnosticsRepositoryImpl(
        iotService = iotService,
        readingDao = db.telemetryReadingDao(),
        faultDao = db.vehicleFaultDao(),
        alertDao = db.predictiveAlertDao(),
        pendingOperationDao = db.pendingOperationDao(),
        queue = queue(),
        sessionManager = sessionManager,
        gson = gson,
        clock = clock
    )

    fun healthReports() = HealthReportRepositoryImpl(iotService, PdfDownloader(context, gson), gson)

    /** Técnico mecánico. */
    suspend fun signIn(
        permissions: List<Permission> = listOf(
            Permission.TELEMETRY_INGEST, Permission.FAULTS_READ, Permission.FAULTS_RESOLVE, Permission.DEVICES_READ,
            Permission.INSTALLATIONS_MANAGE, Permission.PREDICTIVE_ALERTS_READ, Permission.HEALTH_REPORTS_GENERATE
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

    fun disconnect(): MockResponse = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)

    fun fault(id: String, code: String, vehicleId: String = "v1", resolved: Boolean = false): Map<String, Any?> = mapOf(
        "id" to id, "vehicleId" to vehicleId, "dtcCode" to code, "severity" to "MEDIUM", "description" to "Falla $code",
        "detectedAt" to "2026-10-24T13:00:00Z", "isResolved" to resolved, "resolvedAt" to if (resolved) "2026-10-24T14:00:00Z" else null
    )

    fun installation(id: String = "i1", vehicleId: String = "v1"): Map<String, Any?> = mapOf(
        "id" to id, "deviceId" to "d1", "vehicleId" to vehicleId, "tenantId" to "t1", "installedAt" to "2026-10-24T13:00:00Z",
        "uninstalledAt" to null, "initialOdometerKm" to 45000, "finalOdometerKm" to null, "isActive" to true
    )

    fun healthReport(id: String = "r1"): Map<String, Any?> = mapOf(
        "reportId" to id, "vehicleId" to "v1", "overallHealthScore" to 78, "executiveSummary" to "Motor estable",
        "totalRisksDetected" to 2, "generatedAt" to "2026-10-24T14:00:00Z", "jsonResourceUrl" to null, "pdfDownloadUrl" to null
    )

    fun alert(id: String, status: String = "DISPATCHED"): Map<String, Any?> = mapOf(
        "id" to id, "vehicleId" to "v1", "recommendedServiceId" to "s1", "alertType" to "ENGINE_OVERHEATING_RISK",
        "confidenceScore" to 0.87, "message" to "Riesgo de sobrecalentamiento", "status" to status, "createdAt" to "2026-10-24T12:00:00Z"
    )

    fun requested(method: String, path: String) = requests.filter { it.method == method && it.path!!.substringBefore('?') == path }
}
