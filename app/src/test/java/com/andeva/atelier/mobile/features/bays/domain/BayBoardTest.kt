package com.andeva.atelier.mobile.features.bays.domain

import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class BayBoardTest {

    private val now = Instant.parse("2026-01-10T15:00:00Z")

    private fun task(status: TaskStatus?, startedAt: Instant? = null, pausedSeconds: Long = 0) = TaskDetail(
        id = TaskId("t1"), workOrderId = "wo1", serviceId = null, serviceName = null, mechanicId = null, mechanicName = null,
        status = status, description = "", price = null, holdReason = null, missingItemDescription = null,
        totalPausedSeconds = pausedSeconds, startedAt = startedAt, completedAt = null,
        pendingSuspensionReason = null, pendingSuspensionRequestedAt = null, products = emptyList(), evidenceImages = emptyList()
    )

    private fun bay(status: BayStatus?) = WorkBay(
        id = "b1", branchId = "br1", name = "Bahía 1", type = BayType.LIFT, status = status, currentWorkOrderId = null
    )

    @Test
    fun `laborElapsed suma solo las tareas en curso`() {
        val tasks = listOf(
            task(TaskStatus.IN_PROGRESS, startedAt = now.minus(Duration.ofHours(2)), pausedSeconds = 1800),
            task(TaskStatus.ON_HOLD, startedAt = now.minus(Duration.ofHours(3)), pausedSeconds = 600)
        )
        assertEquals(Duration.ofMinutes(90), laborElapsed(tasks, now))
    }

    @Test
    fun `laborElapsed es null sin tareas en curso`() {
        val tasks = listOf(task(TaskStatus.ON_HOLD, startedAt = now.minusSeconds(3600)), task(TaskStatus.COMPLETED))
        assertNull(laborElapsed(tasks, now))
        assertNull(laborElapsed(emptyList(), now))
    }

    @Test
    fun `canReceive solo con AVAILABLE`() {
        assertTrue(bay(BayStatus.AVAILABLE).canReceive)
        assertFalse(bay(BayStatus.OCCUPIED).canReceive)
        assertFalse(bay(BayStatus.MAINTENANCE).canReceive)
        assertFalse(bay(null).canReceive)
    }
}
