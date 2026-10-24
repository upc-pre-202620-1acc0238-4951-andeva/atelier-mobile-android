package com.andeva.atelier.mobile.features.tasks.application

import com.andeva.atelier.mobile.features.tasks.domain.EvidenceKind
import com.andeva.atelier.mobile.features.tasks.domain.InvalidTaskInputException
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskRepository
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TaskUseCasesTest {

    private val repository = mockk<TaskRepository>()
    private val id = TaskId("t1")

    private fun field(result: Result<*>) = (result.exceptionOrNull() as InvalidTaskInputException).field

    @Test
    fun `datos invalidos no llegan al backend`() = runTest {
        assertEquals("actualLaborHours", field(CompleteTaskUseCase(repository)(id, BigDecimal("-1"))))
        assertEquals("notes", field(CompleteTaskUseCase(repository)(id, BigDecimal.ONE, "x".repeat(1001))))
        assertEquals("missingItemDescription", field(RequestSuspensionUseCase(repository)(id, " ")))
        assertEquals("quantity", field(AddPartToTaskUseCase(repository)(id, "p1", BigDecimal.ZERO)))
        assertEquals("quantity", field(UpdateTaskPartQuantityUseCase(repository)(id, "p1", BigDecimal("-2"))))
        assertEquals(
            "description",
            field(AttachTaskEvidenceUseCase(repository)(id, "wo1", File("x.jpg"), EvidenceKind.ASSEMBLY, "x".repeat(500)))
        )
        coVerify(exactly = 0) { repository.complete(any(), any(), any()) }
        coVerify(exactly = 0) { repository.attachEvidence(any(), any(), any(), any()) }
    }

    @Test
    fun `la foto se encola con el tipo al inicio de la descripcion`() = runTest {
        val photo = File("x.jpg")
        coEvery { repository.attachEvidence(id, "wo1", photo, "Desmontaje: disco rayado") } returns Result.success(Unit)

        assertTrue(AttachTaskEvidenceUseCase(repository)(id, "wo1", photo, EvidenceKind.DISASSEMBLY, "disco rayado").isSuccess)
    }

    @Test
    fun `horas sugeridas desde el inicio y nada si no empezo`() {
        val clock = Clock.fixed(Instant.parse("2026-10-24T15:30:00Z"), ZoneOffset.UTC)
        val suggest = SuggestLaborHoursUseCase(clock)
        val task = Task(
            id = id, workOrderId = "wo1", workOrderNumber = 1, workOrderStatus = null, vehicleId = "v1", plate = null,
            branchId = null, bayId = null, bayName = null, serviceName = null, mechanicId = null, status = TaskStatus.IN_PROGRESS,
            description = "", estimatedHours = null, holdReason = null, missingItemDescription = null,
            pendingSuspensionReason = null, startedAt = Instant.parse("2026-10-24T13:00:00Z"), totalPausedSeconds = 1800
        )

        assertEquals(BigDecimal("2.00"), suggest(task))
        assertNull(suggest(task.copy(startedAt = null)))
    }
}
