package com.andeva.atelier.mobile.core.data.remote

import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.session.AuthRefreshService
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TokenAuthenticatorTest {

    private val gson = Gson()
    private val server = MockWebServer()
    private val refreshCalls = AtomicInteger()
    @Volatile private var refreshStatus = 200
    private lateinit var sessionManager: SessionManager
    private lateinit var client: OkHttpClient

    private fun user(token: String, refresh: String) = AuthenticatedUserDto(
        userId = "u1", email = "m@taller.pe", fullName = "Mecánico", token = token, tokenType = "Bearer",
        activeTenant = null, permissions = listOf("operations:tasks:read"), refreshToken = refresh,
        membershipId = "m1", plan = null
    )

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/v1/auth/refresh" -> {
                    refreshCalls.incrementAndGet()
                    Thread.sleep(100)                     // da tiempo a que la otra petición también reciba 401
                    if (refreshStatus == 200) {
                        MockResponse().setBody(gson.toJson(user("new-token", "refresh-2")))
                    } else {
                        MockResponse().setResponseCode(401).setBody("""{"code":"UNAUTHORIZED","message":"Sesión vencida"}""")
                    }
                }
                else ->
                    if (request.getHeader("Authorization") == "Bearer new-token") MockResponse().setBody("ok")
                    else MockResponse().setResponseCode(401)
            }
        }
        server.start()

        sessionManager = SessionManager(InMemoryPreferencesDataStore())
        runBlocking { sessionManager.save(user("old-token", "refresh-1")) }

        val refreshService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AuthRefreshService::class.java)
        val refresher = TokenRefresher(sessionManager, refreshService)
        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
            .authenticator(TokenAuthenticator(refresher))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun get(path: String) = client.newCall(Request.Builder().url(server.url(path)).build()).execute()

    @Test
    fun `401 renueva el token y repite la peticion`() {
        val response = get("/api/v1/tasks/my")
        assertEquals(200, response.code)
        assertEquals(1, refreshCalls.get())
        assertEquals("new-token", runBlocking { sessionManager.getAuthToken() })
        assertEquals("refresh-2", runBlocking { sessionManager.getRefreshToken() })
    }

    @Test
    fun `refresh rechazado borra la sesion`() {
        refreshStatus = 401
        val response = get("/api/v1/tasks/my")
        assertEquals(401, response.code)
        assertNull(runBlocking { sessionManager.getSession() })
    }

    @Test
    fun `dos 401 a la vez hacen un solo refresh`() {
        val executor = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        val codes = (1..2).map {
            executor.submit<Int> {
                start.await()
                get("/api/v1/tasks/my").code
            }
        }
        start.countDown()
        assertEquals(listOf(200, 200), codes.map { it.get(10, TimeUnit.SECONDS) })
        assertEquals(1, refreshCalls.get())
        executor.shutdown()
    }

    @Test
    fun `las rutas publicas no llevan token ni disparan refresh`() {
        val response = get("/api/v1/auth/sign-in")
        assertEquals(401, response.code)
        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals(0, refreshCalls.get())
    }
}
