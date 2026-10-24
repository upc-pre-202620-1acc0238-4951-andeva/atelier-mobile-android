package com.andeva.atelier.mobile.features.tasks.infrastructure.local

import com.andeva.atelier.mobile.features.bays.infrastructure.local.BayEntity
import com.andeva.atelier.mobile.features.reception.infrastructure.local.VehicleEntity
import com.andeva.atelier.mobile.features.tasks.infrastructure.TaskTestEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TaskDaoTest {

    private val env = TaskTestEnv()
    private val dao get() = env.db.taskDao()

    @Before
    fun setUp() = env.start()

    @After
    fun tearDown() = env.stop()

    private fun row(id: String, status: String, number: Int, bayId: String? = null) = TaskEntity(
        id = id, workOrderId = "wo$number", workOrderNumber = number, workOrderStatus = null, vehicleId = "v1",
        branchId = "b1", bayId = bayId, serviceName = null, mechanicId = null, status = status, description = "",
        estimatedHours = null, holdReason = null, missingItemDescription = null, pendingSuspensionReason = null,
        startedAt = null, totalPausedSeconds = 0, syncedAt = 0
    )

    @Test
    fun `ordena en curso, luego en pausa, luego asignadas, y trae placa y bahia`() = runTest {
        env.db.vehicleDao().upsert(VehicleEntity("v1", "ABC-123", null, "Toyota", "Yaris", 2020, null, null, null, 0))
        env.db.bayDao().replaceBranch("b1", listOf(BayEntity("bay1", "b1", "Elevador 1", "LIFT", "OCCUPIED", null, 0)))
        dao.replaceMine(listOf(row("a", "ASSIGNED", 1), row("h", "ON_HOLD", 3), row("p", "IN_PROGRESS", 2, bayId = "bay1")))

        val rows = dao.observeAll().first()

        assertEquals(listOf("p", "h", "a"), rows.map { it.task.id })
        assertEquals("ABC-123", rows.first().plate)
        assertEquals("Elevador 1", rows.first().bayName)
        assertNull(rows.last().bayName)
    }

    @Test
    fun `recargar conserva el detalle y solo descarta el motivo de pausa si cambio el estado`() = runTest {
        dao.replaceMine(listOf(row("t1", "ON_HOLD", 1), row("t2", "ON_HOLD", 2)))
        listOf("t1", "t2").forEach {
            dao.mergeDetail(it, "ON_HOLD", "", "Frenos", "u1", "BREAK", null, null, null, 0, 1)
        }

        dao.replaceMine(listOf(row("t1", "ON_HOLD", 1), row("t2", "IN_PROGRESS", 2)))

        val kept = dao.findById("t1")!!
        assertEquals("Frenos", kept.serviceName)
        assertEquals("BREAK", kept.holdReason)
        val changed = dao.findById("t2")!!
        assertEquals("Frenos", changed.serviceName)
        assertNull(changed.holdReason)
    }

    @Test
    fun `recargar borra las tareas que ya no estan asignadas`() = runTest {
        dao.replaceMine(listOf(row("t1", "ASSIGNED", 1), row("t2", "ASSIGNED", 2)))
        dao.replaceMine(listOf(row("t2", "ASSIGNED", 2)))

        assertEquals(listOf("t2"), dao.observeAll().first().map { it.task.id })
        assertEquals(0, dao.mergeDetail("t1", "IN_PROGRESS", "", null, null, null, null, null, null, 0, 1))
    }
}
