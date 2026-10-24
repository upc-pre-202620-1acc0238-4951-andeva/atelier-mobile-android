package com.andeva.atelier.mobile.features.attendance.infrastructure

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.AuthInterceptor
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.data.remote.employee.EmployeeService
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.sync.PendingOperationQueue
import com.andeva.atelier.mobile.core.data.sync.SyncScheduler
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceService
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.BranchService
import com.andeva.atelier.mobile.features.attendance.infrastructure.repository.AttendanceCache
import com.andeva.atelier.mobile.features.attendance.infrastructure.repository.AttendanceRepositoryImpl
import com.google.gson.Gson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Backend simulado (MockWebServer) + Room en memoria + sesión en memoria para los tests de
 * `attendance`. Hora fija: 2026-10-24 09:00 de Lima.
 */
class AttendanceTestEnv {
    val gson = Gson()
    val clock: Clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneOffset.UTC)
    private val server = MockWebServer()

    /** "METHOD /ruta" sin query → respuesta. Lo que no está responde 404. */
    val responses = ConcurrentHashMap<String, MockResponse>()
    val requests = CopyOnWriteArrayList<RecordedRequest>()

    val isOnline = MutableStateFlow(true)
    val syncScheduler = mockk<SyncScheduler>(relaxed = true)
    private val connectivity = mockk<ConnectivityObserver>().also { every { it.isOnline } returns isOnline }

    lateinit var db: AppDatabase
    lateinit var sessionManager: SessionManager
    lateinit var attendanceService: AttendanceService
    lateinit var cache: AttendanceCache

    fun start() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
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
        attendanceService = retrofit().create(AttendanceService::class.java)
        cache = AttendanceCache(attendanceService, db.attendanceDao(), gson, clock)
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

    fun repository() = AttendanceRepositoryImpl(
        attendanceService = attendanceService,
        branchService = retrofit().create(BranchService::class.java),
        employeeService = retrofit().create(EmployeeService::class.java),
        attendanceDao = db.attendanceDao(),
        shiftDao = db.shiftDao(),
        branchDao = db.branchDao(),
        cacheMetadataDao = db.cacheMetadataDao(),
        pendingOperationDao = db.pendingOperationDao(),
        queue = queue(),
        cache = cache,
        sessionManager = sessionManager,
        connectivity = connectivity,
        gson = gson,
        clock = clock
    )

    suspend fun signIn(
        permissions: List<Permission> = listOf(
            Permission.ATTENDANCE_CLOCK_IN, Permission.ATTENDANCE_CLOCK_OUT, Permission.ATTENDANCE_READ_OWN,
            Permission.SHIFTS_READ, Permission.BRANCHES_READ, Permission.JUSTIFICATIONS_CREATE
        ),
        branchId: String? = "b1",
        shiftId: String? = "s1"
    ) {
        sessionManager.save(
            AuthenticatedUserDto(
                userId = "u1", email = "mecanico@taller.pe", fullName = "Mecánico", token = "jwt", tokenType = "Bearer",
                activeTenant = null, permissions = permissions.map { it.value }, refreshToken = "refresh",
                membershipId = "m1", plan = null
            )
        )
        sessionManager.updateAssignment(branchId, shiftId)
    }

    fun json(body: Any, code: Int = 200): MockResponse = MockResponse().setResponseCode(code).setBody(gson.toJson(body))

    fun error(code: Int, errorCode: String, message: String): MockResponse =
        MockResponse().setResponseCode(code).setBody("""{"code":"$errorCode","message":"$message","details":[]}""")

    fun disconnect(): MockResponse = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)

    fun attendance(
        id: String = "a1",
        clockIn: String = "2026-10-24T13:05:00Z",
        clockOut: String? = null,
        status: String = "ON_TIME"
    ): Map<String, Any?> = mapOf(
        "id" to id, "branchId" to "b1", "membershipId" to "m1", "shiftId" to "s1", "clockIn" to clockIn,
        "clockOut" to clockOut, "status" to status, "latitude" to -12.0464, "longitude" to -77.0428,
        "distanceToBranchMeters" to 12.5, "workedMinutes" to if (clockOut != null) 480 else null
    )

    fun requested(method: String, path: String) = requests.filter { it.method == method && it.path!!.substringBefore('?') == path }
}
