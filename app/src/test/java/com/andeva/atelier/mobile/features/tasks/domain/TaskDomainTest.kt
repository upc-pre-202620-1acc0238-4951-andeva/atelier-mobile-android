package com.andeva.atelier.mobile.features.tasks.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

class TaskDomainTest {

    private fun task(status: TaskStatus?, holdReason: HoldReason? = null, pendingSuspension: String? = null) = Task(
        id = TaskId("t1"), workOrderId = "wo1", workOrderNumber = 1, workOrderStatus = null, vehicleId = "v1", plate = null,
        branchId = null, bayId = null, bayName = null, serviceName = null, mechanicId = null, status = status,
        description = "", estimatedHours = null, holdReason = holdReason, missingItemDescription = null,
        pendingSuspensionReason = pendingSuspension, startedAt = null, totalPausedSeconds = 0
    )

    @Test
    fun `pausa por refrigerio la reanuda el mecanico y la suspension no`() {
        val paused = task(TaskStatus.ON_HOLD, HoldReason.BREAK)
        assertTrue(paused.isPausedByMechanic)
        assertFalse(paused.isSuspended)

        val suspended = task(TaskStatus.ON_HOLD, HoldReason.DEFECTIVE_PART)
        assertFalse(suspended.isPausedByMechanic)
        assertTrue(suspended.isSuspended)

        assertFalse(task(TaskStatus.IN_PROGRESS).isPausedByMechanic)
        assertTrue(task(TaskStatus.IN_PROGRESS, pendingSuspension = "Pieza rota").hasPendingSuspension)
    }

    @Test
    fun `horas sugeridas descuentan la pausa y redondean a dos decimales`() {
        val start = Instant.parse("2026-10-24T13:00:00Z")
        val now = Instant.parse("2026-10-24T15:00:00Z")

        assertEquals(BigDecimal("1.83"), LaborTime.suggestedHours(start, totalPausedSeconds = 600, now = now))
        assertEquals(BigDecimal("0.00"), LaborTime.suggestedHours(now, totalPausedSeconds = 600, now = start))
    }

    @Test
    fun `el tipo de foto va al inicio de la descripcion`() {
        assertEquals("Desmontaje: pastilla gastada", EvidenceKind.DISASSEMBLY.describe("  pastilla gastada "))
        assertEquals("Montaje", EvidenceKind.ASSEMBLY.describe(" "))
        assertEquals("Montaje", EvidenceKind.ASSEMBLY.describe(null))
    }

    @Test
    fun `validaciones de los requests`() {
        assertTrue(TaskValidation.isLaborHours(BigDecimal.ZERO))
        assertFalse(TaskValidation.isLaborHours(BigDecimal("-0.5")))
        assertFalse(TaskValidation.isMissingItem("  "))
        assertFalse(TaskValidation.isMissingItem("x".repeat(501)))
        assertTrue(TaskValidation.isNotes(null))
        assertFalse(TaskValidation.isNotes("x".repeat(1001)))
        assertFalse(TaskValidation.isQuantity(BigDecimal.ZERO))
        assertTrue(PartOption("p1", "Bujía", null, null, null, BigDecimal.ONE, null).isAvailable)
        assertFalse(PartOption("p1", "Bujía", null, null, null, BigDecimal.ZERO, null).isAvailable)
    }
}
