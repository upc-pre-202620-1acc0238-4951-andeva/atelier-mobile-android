package com.andeva.atelier.mobile.features.reception.application

import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.AppointmentStatus
import com.andeva.atelier.mobile.features.reception.domain.Customer
import com.andeva.atelier.mobile.features.reception.domain.CustomerType
import com.andeva.atelier.mobile.features.reception.domain.InvalidReceptionInputException
import com.andeva.atelier.mobile.features.reception.domain.ReceptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReceptionCounterUseCasesTest {

    private val repository = mockk<ReceptionRepository>()
    private val scheduledAt = Instant.parse("2026-11-23T15:00:00Z")

    private val sampleAppointment = Appointment(
        id = "a1", branchId = "b1", customerId = "c1", vehicleId = "v1",
        scheduledAt = scheduledAt, estimatedDurationMinutes = 60, reason = "Mantenimiento",
        status = AppointmentStatus.PENDING
    )

    private fun field(result: Result<*>) = (result.exceptionOrNull() as InvalidReceptionInputException).field

    @Test
    fun `validaciones locales rechazan entradas invalidas sin llamar al backend`() = runTest {
        assertEquals("customerId", field(CreateAppointmentUseCase(repository)("", "v1", scheduledAt)))
        assertEquals("vehicleId", field(CreateAppointmentUseCase(repository)("c1", "", scheduledAt)))
        assertEquals("estimatedDurationMinutes", field(CreateAppointmentUseCase(repository)("c1", "v1", scheduledAt, 10)))
        assertEquals("reason", field(CreateAppointmentUseCase(repository)("c1", "v1", scheduledAt, 30, "a".repeat(1001))))
        assertEquals("appointmentId", field(ConfirmAppointmentUseCase(repository)("")))
        assertEquals("appointmentId", field(RescheduleAppointmentUseCase(repository)("", scheduledAt)))
        assertEquals("appointmentId", field(CancelAppointmentUseCase(repository)("", "Motivo")))
        assertEquals("cancellationReason", field(CancelAppointmentUseCase(repository)("a1", "   ")))
        assertEquals("cancellationReason", field(CancelAppointmentUseCase(repository)("a1", "a".repeat(501))))
        assertEquals("customerId", field(UpdateCustomerContactUseCase(repository)("", "a@b.pe", "999")))
        assertEquals("email", field(UpdateCustomerContactUseCase(repository)("c1", "sin-arroba", "999")))
        assertEquals("phone", field(UpdateCustomerContactUseCase(repository)("c1", "a@b.pe", "1".repeat(21))))

        coVerify(exactly = 0) { repository.createAppointment(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { repository.confirmAppointment(any()) }
        coVerify(exactly = 0) { repository.rescheduleAppointment(any(), any()) }
        coVerify(exactly = 0) { repository.cancelAppointment(any(), any()) }
        coVerify(exactly = 0) { repository.updateCustomerContact(any(), any(), any()) }
    }

    @Test
    fun `casos de uso validos delegan al repositorio`() = runTest {
        val customer = Customer("c1", CustomerType.INDIVIDUAL, "Ana Torres", "45678912", "nuevo@correo.pe", "987654321", "ACTIVE")
        coEvery { repository.createAppointment("c1", "v1", scheduledAt, 60, "Frenos") } returns Result.success(sampleAppointment)
        coEvery { repository.confirmAppointment("a1") } returns Result.success(sampleAppointment.copy(status = AppointmentStatus.CONFIRMED))
        coEvery { repository.rescheduleAppointment("a1", scheduledAt) } returns Result.success(sampleAppointment)
        coEvery { repository.cancelAppointment("a1", "No asistirá") } returns Result.success(sampleAppointment.copy(status = AppointmentStatus.CANCELED))
        coEvery { repository.updateCustomerContact("c1", "nuevo@correo.pe", "987654321") } returns Result.success(customer)

        assertTrue(CreateAppointmentUseCase(repository)("c1", "v1", scheduledAt, 60, "Frenos").isSuccess)
        assertEquals(AppointmentStatus.CONFIRMED, ConfirmAppointmentUseCase(repository)("a1").getOrThrow().status)
        assertTrue(RescheduleAppointmentUseCase(repository)("a1", scheduledAt).isSuccess)
        assertEquals(AppointmentStatus.CANCELED, CancelAppointmentUseCase(repository)("a1", "  No asistirá ").getOrThrow().status)
        assertEquals("nuevo@correo.pe", UpdateCustomerContactUseCase(repository)("c1", "nuevo@correo.pe", "987654321").getOrThrow().email)
    }
}
