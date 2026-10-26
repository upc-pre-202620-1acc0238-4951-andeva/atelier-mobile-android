package com.andeva.atelier.mobile.features.team.infrastructure

import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.AuthInterceptor
import com.andeva.atelier.mobile.core.data.remote.employee.EmployeeService
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.remote.session.TenantSummaryDto
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.team.infrastructure.remote.TeamAttendanceService
import com.andeva.atelier.mobile.features.team.infrastructure.repository.TeamRepositoryImpl
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
import java.util.concurrent.TimeUnit

/**
 * Backend simulado (MockWebServer, despachador por ruta y query) + sesión en memoria para los tests de `team`.
 * Hora fija: 2026-10-24 09:00 de Lima.
 */
class TeamTestEnv(now: Instant = Instant.parse("2026-10-24T14:00:00Z")) {
    val gson = Gson()
    val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val server = MockWebServer()

    /** "METHOD /ruta" sin query → respuesta. Lo demás, 404. */
    val responses = ConcurrentHashMap<String, MockResponse>()
    val requests = CopyOnWriteArrayList<RecordedRequest>()

    /** Si devuelve una respuesta, gana sobre [responses] (por ejemplo, según el parámetro `date`). */
    @Volatile
    var handler: ((RecordedRequest) -> MockResponse?)? = null

    lateinit var sessionManager: SessionManager
    private lateinit var retrofit: Retrofit

    fun start() {
        sessionManager = SessionManager(InMemoryPreferencesDataStore())
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val key = "${request.method} ${request.path!!.substringBefore('?')}"
                return handler?.invoke(request) ?: responses[key] ?: error(404, "NOT_FOUND", "No encontrado")
            }
        }
        server.start()
        retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(AuthInterceptor(sessionManager))
                    .readTimeout(1, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun stop() = server.shutdown()

    fun repository() = TeamRepositoryImpl(
        teamAttendanceService = retrofit.create(TeamAttendanceService::class.java),
        employeeService = retrofit.create(EmployeeService::class.java),
        sessionManager = sessionManager,
        gson = gson,
        clock = clock
    )

    /** Auditor con sede `b1` (o sin sede si [branchId] es null). */
    suspend fun signIn(
        permissions: List<Permission> = listOf(Permission.MEMBERS_READ),
        branchId: String? = "b1"
    ) {
        sessionManager.save(
            AuthenticatedUserDto(
                userId = "u1", email = "jefe@taller.pe", fullName = "Jefe", token = "jwt", tokenType = "Bearer",
                activeTenant = TenantSummaryDto("t1", "Taller Suárez", null), permissions = permissions.map { it.value },
                refreshToken = "refresh", membershipId = "m0", plan = null
            )
        )
        sessionManager.updateAssignment(branchId, null)
    }

    fun json(body: Any, code: Int = 200): MockResponse = MockResponse().setResponseCode(code).setBody(gson.toJson(body))

    fun error(code: Int, errorCode: String, message: String): MockResponse =
        MockResponse().setResponseCode(code).setBody("""{"code":"$errorCode","message":"$message","details":[]}""")

    fun requested(method: String, path: String) = requests.filter { it.method == method && it.path!!.substringBefore('?') == path }

    fun attendance(
        id: String = "a1",
        membershipId: String? = "m1",
        clockIn: String? = null,
        status: String = "ABSENT",
        justificationStatus: String? = "PENDING"
    ): Map<String, Any?> = mapOf(
        "id" to id, "branchId" to "b1", "membershipId" to membershipId, "shiftId" to "s1", "clockIn" to clockIn,
        "clockOut" to null, "status" to status, "latitude" to null, "longitude" to null, "distanceToBranchMeters" to null,
        "justificationReason" to "Cita médica", "justificationStatus" to justificationStatus,
        "workedMinutes" to null, "regularMinutes" to null, "overtimeMinutes" to null
    )

    fun profile(membershipId: String?, jobTitle: String?): Map<String, Any?> = mapOf(
        "id" to "e-$membershipId", "branchId" to "b1", "membershipId" to membershipId, "assignedShiftId" to null,
        "jobTitle" to jobTitle, "employmentStatus" to "ACTIVE"
    )
}
