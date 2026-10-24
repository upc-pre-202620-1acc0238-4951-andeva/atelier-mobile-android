package com.andeva.atelier.mobile.features.attendance.application

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceRepository
import com.andeva.atelier.mobile.features.attendance.domain.Branch
import com.andeva.atelier.mobile.features.attendance.domain.DeviceLocation
import com.andeva.atelier.mobile.features.attendance.domain.InvalidJustificationException
import com.andeva.atelier.mobile.features.attendance.domain.LocationPermissionDeniedException
import com.andeva.atelier.mobile.features.attendance.domain.LocationProvider
import com.andeva.atelier.mobile.features.attendance.domain.LocationTooInaccurateException
import com.andeva.atelier.mobile.features.attendance.domain.MissingAssignmentException
import com.andeva.atelier.mobile.features.attendance.domain.SubmitOutcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceUseCasesTest {

    private val repository = mockk<AttendanceRepository>()
    private var location: Result<DeviceLocation> = Result.success(DeviceLocation(-12.0500, -77.0428, 12f, isMock = false))
    private val locationProvider = object : LocationProvider {
        override suspend fun currentLocation() = location
    }
    private val clockIn = ClockInUseCase(repository, locationProvider)
    private val branch = Branch("b1", "Sede Lima", -12.0464, -77.0428, 100.0, true)

    @Test
    fun `fuera de la geocerca igual se envia y avisa la distancia`() = runTest {
        coEvery { repository.findBranch("b1") } returns branch
        coEvery { repository.clockIn("b1", "s1", any()) } returns Result.success(SubmitOutcome.Queued)

        val result = clockIn("b1", "s1").getOrThrow()

        assertEquals(SubmitOutcome.Queued, result.outcome)
        assertFalse(result.geofence!!.isInside)
        assertTrue(result.geofence!!.distanceMeters > 300)
        coVerify { repository.clockIn("b1", "s1", any()) }
    }

    @Test
    fun `ubicacion simulada se marca y sin sede en cache no hay aviso`() = runTest {
        location = Result.success(DeviceLocation(-12.0464, -77.0428, 5f, isMock = true))
        coEvery { repository.findBranch("b1") } returns branch
        coEvery { repository.clockIn(any(), any(), any()) } returns Result.success(SubmitOutcome.Queued)
        assertTrue(clockIn("b1", "s1").getOrThrow().geofence!!.isMock)

        coEvery { repository.findBranch("b1") } returns null
        assertNull(clockIn("b1", "s1").getOrThrow().geofence)
    }

    @Test
    fun `precision peor que 50 m no llama al repositorio`() = runTest {
        location = Result.success(DeviceLocation(-12.0464, -77.0428, 80f, isMock = false))

        val error = clockIn("b1", "s1").exceptionOrNull()

        assertTrue(error is LocationTooInaccurateException)
        assertEquals(80f, (error as LocationTooInaccurateException).accuracyMeters)
        coVerify(exactly = 0) { repository.clockIn(any(), any(), any()) }
    }

    @Test
    fun `sin permiso o sin sede y turno falla antes de enviar`() = runTest {
        location = Result.failure(LocationPermissionDeniedException())
        assertTrue(clockIn("b1", "s1").exceptionOrNull() is LocationPermissionDeniedException)

        assertTrue(clockIn(null, "s1").exceptionOrNull() is MissingAssignmentException)
        assertTrue(clockIn("b1", " ").exceptionOrNull() is MissingAssignmentException)
        coVerify(exactly = 0) { repository.clockIn(any(), any(), any()) }
    }

    @Test
    fun `descargo vacio o demasiado largo no se envia`() = runTest {
        val submit = SubmitJustificationUseCase(repository)
        assertTrue(submit(AttendanceId("a1"), " ").exceptionOrNull() is InvalidJustificationException)
        assertTrue(submit(AttendanceId("a1"), "x".repeat(501)).exceptionOrNull() is InvalidJustificationException)
        coVerify(exactly = 0) { repository.submitJustification(any(), any()) }

        coEvery { repository.submitJustification(AttendanceId("a1"), "Tráfico") } returns Result.success(SubmitOutcome.Queued)
        assertEquals(SubmitOutcome.Queued, submit(AttendanceId("a1"), "Tráfico").getOrThrow())
    }
}
