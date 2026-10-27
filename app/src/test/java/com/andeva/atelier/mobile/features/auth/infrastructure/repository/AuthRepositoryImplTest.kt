package com.andeva.atelier.mobile.features.auth.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.AuthInterceptor
import com.andeva.atelier.mobile.core.data.remote.employee.EmployeeService
import com.andeva.atelier.mobile.core.data.remote.session.AuthRefreshService
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.data.sync.CacheMaintenanceDao
import com.andeva.atelier.mobile.core.data.sync.PendingOperationDao
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.notifications.DeviceTokenRegistrar
import com.andeva.atelier.mobile.features.auth.domain.AcceptInvitation
import com.andeva.atelier.mobile.features.auth.domain.InvalidInvitationException
import com.andeva.atelier.mobile.features.auth.infrastructure.remote.AuthService
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CopyOnWriteArrayList

class AuthRepositoryImplTest {

    private val gson = Gson()
    private val server = MockWebServer()
    private val requests = CopyOnWriteArrayList<String>()
    private val responses = mutableMapOf<String, MockResponse>()
    @Volatile private var tokenSavedWhenProfileRequested: String? = "not-requested"
    @Volatile private var profileAuthorization: String? = null

    private lateinit var sessionManager: SessionManager
    private val deviceTokenRegistrar = mockk<DeviceTokenRegistrar>()
    private val cacheMaintenanceDao = mockk<CacheMaintenanceDao>(relaxed = true)
    private val pendingOperationDao = mockk<PendingOperationDao>(relaxed = true)
    private lateinit var repository: AuthRepositoryImpl

    private fun user(membershipId: String? = "m1") = AuthenticatedUserDto(
        userId = "u1", email = "mecanico@taller.pe", fullName = "Mecánico", token = "jwt-1", tokenType = "Bearer",
        activeTenant = null, permissions = listOf("operations:tasks:read"), refreshToken = "refresh-1",
        membershipId = membershipId, plan = null
    )

    private fun json(body: Any, code: Int = 200) = MockResponse().setResponseCode(code).setBody(gson.toJson(body))

    private fun error(code: Int, errorCode: String, message: String) =
        MockResponse().setResponseCode(code).setBody("""{"code":"$errorCode","message":"$message","details":[]}""")

    @Before
    fun setUp() {
        sessionManager = SessionManager(InMemoryPreferencesDataStore())
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path!!.substringBefore('?')
                requests += "${request.method} $path"
                if (path.startsWith("/api/v1/hr/employees/membership/")) {
                    tokenSavedWhenProfileRequested = runBlocking { sessionManager.getAuthToken() }
                    profileAuthorization = request.getHeader("Authorization")
                }
                return responses[path] ?: MockResponse().setResponseCode(404)
            }
        }
        server.start()

        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(sessionManager)).build()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        coEvery { deviceTokenRegistrar.registerCurrentToken() } returns Result.failure(IllegalStateException("no FCM"))
        coEvery { deviceTokenRegistrar.clear() } returns Result.success(Unit)
        repository = AuthRepositoryImpl(
            authService = retrofit.create(AuthService::class.java),
            employeeService = retrofit.create(EmployeeService::class.java),
            refreshService = retrofit.create(AuthRefreshService::class.java),
            sessionManager = sessionManager,
            deviceTokenRegistrar = deviceTokenRegistrar,
            cacheMaintenanceDao = cacheMaintenanceDao,
            pendingOperationDao = pendingOperationDao,
            gson = gson
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login pide la sede con el token recibido antes de guardar la sesion`() = runTest {
        responses["/api/v1/auth/sign-in"] = json(user())
        responses["/api/v1/hr/employees/membership/m1"] =
            json(mapOf("id" to "p1", "branchId" to "b1", "membershipId" to "m1", "assignedShiftId" to "s1"))

        val result = repository.signIn("mecanico@taller.pe", "secreto123")

        assertTrue(result.isSuccess)
        assertEquals("Bearer jwt-1", profileAuthorization)
        assertNull(tokenSavedWhenProfileRequested)           // aún no había sesión guardada
        val session = sessionManager.getSession()!!
        assertEquals("b1", session.branchId)
        assertEquals("s1", session.shiftId)
        assertFalse(session.needsBranchChoice)               // con sede asignada no se ofrece elegir
        coVerify { deviceTokenRegistrar.registerCurrentToken() }
    }

    @Test
    fun `sin perfil laboral el login igual entra sin sede`() = runTest {
        responses["/api/v1/auth/sign-in"] = json(user())
        // /hr/employees/membership/m1 responde 404 (dueño o administrador)

        val result = repository.signIn("dueno@taller.pe", "secreto123")

        assertTrue(result.isSuccess)
        val session = sessionManager.getSession()!!
        assertEquals("u1", session.userId)
        assertNull(session.branchId)
        assertTrue(session.needsBranchChoice)                // 404: dueño o administrador, puede elegir sede
    }

    @Test
    fun `un corte de red al pedir el perfil no deja elegir sede`() = runTest {
        responses["/api/v1/auth/sign-in"] = json(user())
        responses["/api/v1/hr/employees/membership/m1"] = MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)

        val result = repository.signIn("mecanico@taller.pe", "secreto123")

        assertTrue(result.isSuccess)
        val session = sessionManager.getSession()!!
        assertNull(session.branchId)
        assertFalse(session.needsBranchChoice)               // un técnico con mala señal no debe poder escoger cualquier sede
    }

    @Test
    fun `sin membershipId no se consulta el perfil laboral`() = runTest {
        responses["/api/v1/auth/google-sign-in"] = json(user(membershipId = null))

        val result = repository.signInWithGoogle("google-id-token")

        assertTrue(result.isSuccess)
        assertEquals(listOf("POST /api/v1/auth/google-sign-in"), requests.toList())
    }

    @Test
    fun `correo sin verificar devuelve el codigo del backend y no guarda sesion`() = runTest {
        responses["/api/v1/auth/sign-in"] = error(403, "EMAIL_NOT_VERIFIED", "Debes verificar tu correo")

        val error = repository.signIn("nuevo@taller.pe", "secreto123").exceptionOrNull() as ApiException

        assertEquals(403, error.httpStatus)
        assertEquals("EMAIL_NOT_VERIFIED", error.code)
        assertNull(sessionManager.getSession())
    }

    @Test
    fun `invitacion con valid false falla como invitacion invalida`() = runTest {
        responses["/api/v1/invitations/validate"] =
            json(mapOf("valid" to false, "email" to "x@taller.pe", "tenantName" to "Taller", "roleName" to "Mecánico"))

        assertTrue(repository.validateInvitation("token").exceptionOrNull() is InvalidInvitationException)
    }

    @Test
    fun `invitacion vencida conserva el mensaje del backend`() = runTest {
        responses["/api/v1/invitations/accept"] = error(410, "GONE", "La invitación expiró")

        val error = repository.acceptInvitation(AcceptInvitation("token", "Ana", "Pérez", "secreto123", null))
            .exceptionOrNull() as ApiException

        assertEquals(410, error.httpStatus)
        assertEquals("La invitación expiró", error.message)
    }

    @Test
    fun `aceptar invitacion deja la sesion iniciada`() = runTest {
        responses["/api/v1/invitations/accept"] = json(user(), code = 201)

        assertTrue(repository.acceptInvitation(AcceptInvitation("token", "Ana", "Pérez", "secreto123", "")).isSuccess)
        assertEquals("u1", sessionManager.getSession()?.userId)
        val body = server.takeRequest().body.readUtf8()
        assertTrue("phone vacío no se envía", !body.contains("\"phone\":\"\""))
    }

    @Test
    fun `cerrar sesion borra token FCM, revoca el refresh, borra caches y sesion sin tocar la cola`() = runTest {
        sessionManager.save(user())
        responses["/api/v1/auth/sign-out"] = json(mapOf("message" to "ok"))

        repository.signOut()

        coVerifyOrder {
            deviceTokenRegistrar.clear()
            cacheMaintenanceDao.clearCaches()
        }
        assertTrue(requests.contains("POST /api/v1/auth/sign-out"))
        assertTrue(server.takeRequest().body.readUtf8().contains("refresh-1"))
        assertNull(sessionManager.getSession())
        coVerify(exactly = 0) { pendingOperationDao.deleteForUser(any()) }
    }

    @Test
    fun `cerrar sesion sin red igual limpia el dispositivo`() = runTest {
        sessionManager.save(user())
        server.shutdown()

        repository.signOut()

        coVerify { cacheMaintenanceDao.clearCaches() }
        assertNull(sessionManager.getSession())
    }

    @Test
    fun `pendientes se cuentan para el usuario de la sesion`() = runTest {
        sessionManager.save(user())
        every { pendingOperationDao.countPending("u1") } returns flowOf(3)

        assertEquals(3, repository.pendingOperationCount())
    }
}
