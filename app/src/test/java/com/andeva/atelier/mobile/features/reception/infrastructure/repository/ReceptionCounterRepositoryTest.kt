package com.andeva.atelier.mobile.features.reception.infrastructure.repository

import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.features.reception.domain.AppointmentStatus
import com.andeva.atelier.mobile.features.reception.infrastructure.ReceptionTestEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReceptionCounterRepositoryTest {

    private val env = ReceptionTestEnv()
    private lateinit var repository: ReceptionRepositoryImpl
    private val today = LocalDate.of(2026, 10, 24)

    @Before
    fun setUp() {
        env.start()
        repository = env.receptionRepository()
    }

    @After
    fun tearDown() = env.stop()

    @Test
    fun `crear cita requiere sede y guarda en Room al crearse`() = runTest {
        env.signIn(branchId = null)
        val withoutBranch = repository.createAppointment("c1", "v1", Instant.parse("2026-10-24T15:00:00Z"))
        assertTrue(withoutBranch.exceptionOrNull() is MissingBranchException)

        env.signIn(branchId = "b1")
        env.responses["POST /api/v1/appointments"] = env.json(env.appointment(id = "a9", status = "PENDING"), code = 201)

        val created = repository.createAppointment("c1", "v1", Instant.parse("2026-10-24T15:00:00Z"), 60, "Frenos").getOrThrow()

        assertEquals("a9", created.id)
        assertEquals(AppointmentStatus.PENDING, created.status)
        assertEquals("a9", repository.observeAppointments(today).first().single().id)
    }

    @Test
    fun `confirmar, reprogramar y cancelar cita actualizan la agenda en Room`() = runTest {
        env.signIn()
        env.responses["POST /api/v1/appointments/a1/confirm"] = env.json(env.appointment(id = "a1", status = "CONFIRMED"))
        env.responses["POST /api/v1/appointments/a1/reschedule"] = env.json(
            env.appointment(id = "a1", status = "CONFIRMED", scheduledAt = "2026-10-24T18:00:00Z")
        )
        env.responses["POST /api/v1/appointments/a1/cancel"] = env.json(env.appointment(id = "a1", status = "CANCELED"))

        val confirmed = repository.confirmAppointment("a1").getOrThrow()
        assertEquals(AppointmentStatus.CONFIRMED, confirmed.status)

        val rescheduled = repository.rescheduleAppointment("a1", Instant.parse("2026-10-24T18:00:00Z")).getOrThrow()
        assertEquals(Instant.parse("2026-10-24T18:00:00Z"), rescheduled.scheduledAt)

        val canceled = repository.cancelAppointment("a1", "Cliente reprogramará").getOrThrow()
        assertEquals(AppointmentStatus.CANCELED, canceled.status)
        assertEquals(AppointmentStatus.CANCELED, repository.observeAppointments(today).first().single().status)
    }

    @Test
    fun `editar contacto actualiza el cliente en el backend y en Room`() = runTest {
        env.signIn()
        env.responses["PUT /api/v1/customers/c1/contact"] = env.json(
            env.customer(id = "c1").toMutableMap().apply {
                this["email"] = "nuevo@correo.pe"
                this["phone"] = "911222333"
            }
        )

        val updated = repository.updateCustomerContact("c1", "nuevo@correo.pe", "911222333").getOrThrow()

        assertEquals("nuevo@correo.pe", updated.email)
        assertEquals("911222333", updated.phone)
        assertEquals("nuevo@correo.pe", repository.observeCustomer("c1").first()!!.email)
    }
}
