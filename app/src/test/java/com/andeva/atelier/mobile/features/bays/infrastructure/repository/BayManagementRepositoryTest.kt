package com.andeva.atelier.mobile.features.bays.infrastructure.repository

import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.infrastructure.BayTestEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BayManagementRepositoryTest {

    private val env = BayTestEnv()
    private lateinit var repository: BayRepositoryImpl

    @Before
    fun setUp() {
        env.start()
        repository = env.repository()
    }

    @After
    fun tearDown() = env.stop()

    @Test
    fun `liberar bahia llama DELETE y refresca el tablero`() = runTest {
        env.signIn()
        env.responses["DELETE /api/v1/work-orders/wo1/bay"] = MockResponse().setResponseCode(204)
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay1", "Elevador 1", "AVAILABLE", null)))

        val result = repository.releaseBay("wo1")

        assertTrue(result.isSuccess)
        assertEquals(1, env.requested("DELETE", "/api/v1/work-orders/wo1/bay").size)
        val board = repository.observeBoard().first()
        assertEquals(BayStatus.AVAILABLE, board.single().bay.status)
        assertNull(board.single().occupant)
    }

    @Test
    fun `mantenimiento y restauracion actualizan el estado en Room`() = runTest {
        env.signIn()
        env.responses["PUT /api/v1/work-bays/bay1/maintenance"] = env.json(env.bay("bay1", "Elevador 1", "MAINTENANCE"))
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay1", "Elevador 1", "MAINTENANCE")))

        val underMaintenance = repository.setBayMaintenance("bay1", "Revisión hidráulica").getOrThrow()
        assertEquals(BayStatus.MAINTENANCE, underMaintenance.status)
        assertEquals(BayStatus.MAINTENANCE, repository.observeBoard().first().single().bay.status)

        env.responses["PUT /api/v1/work-bays/bay1/restore"] = env.json(env.bay("bay1", "Elevador 1", "AVAILABLE"))
        env.responses["GET /api/v1/work-bays"] = env.json(listOf(env.bay("bay1", "Elevador 1", "AVAILABLE")))

        val restored = repository.restoreBay("bay1").getOrThrow()
        assertEquals(BayStatus.AVAILABLE, restored.status)
        assertEquals(BayStatus.AVAILABLE, repository.observeBoard().first().single().bay.status)
    }
}
