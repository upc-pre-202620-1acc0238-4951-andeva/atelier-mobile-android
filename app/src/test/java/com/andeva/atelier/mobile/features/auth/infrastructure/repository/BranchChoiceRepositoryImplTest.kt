package com.andeva.atelier.mobile.features.auth.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.InMemoryPreferencesDataStore
import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.session.AuthenticatedUserDto
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.BranchService
import kotlinx.coroutines.test.runTest
import com.google.gson.Gson
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BranchChoiceRepositoryImplTest {

    private val gson = Gson()
    private val server = MockWebServer()
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: BranchChoiceRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        sessionManager = SessionManager(InMemoryPreferencesDataStore())
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        repository = BranchChoiceRepositoryImpl(retrofit.create(BranchService::class.java), sessionManager, gson)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `lista las sedes activas ordenadas por nombre`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """[
                  {"id":"3","name":"Sede Surquillo","isActive":true,"geofenceRadiusMeters":80},
                  {"id":"1","name":"Sede Ate","isActive":true},
                  {"id":"2","name":"Sede Cerrada","isActive":false},
                  {"id":"4","name":"  ","isActive":true},
                  {"id":"5","name":"Sede San Isidro","geofenceRadiusMeters":50}
                ]"""
            )
        )

        val branches = repository.availableBranches().getOrThrow()

        assertEquals(listOf("Sede Ate", "Sede San Isidro", "Sede Surquillo"), branches.map { it.name })
        assertEquals(listOf(null, 50, 80), branches.map { it.geofenceRadiusMeters })
        assertEquals("/api/v1/branches", server.takeRequest().path)
    }

    @Test
    fun `un error del backend llega con su mensaje`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"code":"FORBIDDEN","message":"Sin permiso","details":[]}"""))

        val error = repository.availableBranches().exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals("Sin permiso", (error as ApiException).message)
    }

    @Test
    fun `elegir sede la guarda en la sesion sin llamar al backend`() = runTest {
        sessionManager.markBranchChoicePending()
        sessionManager.save(
            AuthenticatedUserDto(
                userId = "u1", email = "dueno@taller.pe", fullName = "Dueño", token = "jwt-1", tokenType = "Bearer",
                activeTenant = null, permissions = emptyList(), refreshToken = "r", membershipId = "m1", plan = null
            )
        )

        repository.selectBranch("b2")

        assertEquals("b2", sessionManager.getSession()!!.branchId)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `elegir despues no deja sede`() = runTest {
        repository.skipBranchChoice()
        assertNull(sessionManager.getSession())
    }
}
