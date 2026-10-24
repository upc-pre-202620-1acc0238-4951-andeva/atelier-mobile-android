package com.andeva.atelier.mobile.features.attendance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class AttendanceDomainTest {

    private val branch = Branch("b1", "Sede Lima", latitude = -12.0464, longitude = -77.0428, geofenceRadiusMeters = 100.0, isActive = true)

    private fun location(lat: Double, lon: Double, mock: Boolean = false) = DeviceLocation(lat, lon, accuracyMeters = 10f, isMock = mock)

    @Test
    fun `haversine con una distancia conocida`() {
        // Un grado de latitud ≈ 111.2 km
        val distance = Geofence.distanceMeters(0.0, 0.0, 1.0, 0.0)
        assertEquals(111_195.0, distance, 50.0)
    }

    @Test
    fun `geocerca dentro y fuera del radio`() {
        val inside = Geofence.check(branch, location(-12.0468, -77.0428))        // ~45 m
        assertTrue(inside.isInside)
        assertEquals(44.5, inside.distanceMeters, 2.0)

        val outside = Geofence.check(branch, location(-12.0500, -77.0428, mock = true))   // ~400 m
        assertFalse(outside.isInside)
        assertTrue(outside.isMock)
    }

    @Test
    fun `validacion del descargo y de la precision`() {
        assertTrue(AttendanceValidation.isJustification("Tráfico en la Panamericana"))
        assertFalse(AttendanceValidation.isJustification("   "))
        assertFalse(AttendanceValidation.isJustification("a".repeat(501)))
        assertTrue(AttendanceValidation.isJustification("a".repeat(500)))

        assertTrue(AttendanceValidation.isAccurate(location(0.0, 0.0).copy(accuracyMeters = 50f)))
        assertFalse(AttendanceValidation.isAccurate(location(0.0, 0.0).copy(accuracyMeters = 50.1f)))
    }

    @Test
    fun `semana del turno con descansos`() {
        val shift = WorkShift(
            id = "s1", name = "Mañana", startTime = LocalTime.of(8, 0), endTime = LocalTime.of(17, 0), gracePeriodMinutes = 15,
            workDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY),
            restDays = setOf(DayOfWeek.SUNDAY), isActive = true
        )
        val days = WeeklySchedule("b1", "s1", shift, branch).days

        assertEquals(DayOfWeek.entries.toList(), days.map { it.dayOfWeek })
        assertEquals(listOf(DayOfWeek.SUNDAY), days.filterNot { it.isWorkDay }.map { it.dayOfWeek })
    }

    @Test
    fun `sin workDays publicados se trabaja todo menos el descanso`() {
        val shift = WorkShift("s1", "Mañana", LocalTime.of(8, 0), LocalTime.of(17, 0), 15, emptySet(), setOf(DayOfWeek.SUNDAY), true)
        assertEquals(6, WeeklySchedule(null, "s1", shift, null).days.count { it.isWorkDay })
    }
}
