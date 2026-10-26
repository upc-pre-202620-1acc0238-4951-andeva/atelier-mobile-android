package com.andeva.atelier.mobile.features.team.domain

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamAttendanceTest {
    private fun attendance(status: JustificationStatus?) = TeamAttendance(
        id = "a1", membershipId = "m1", branchId = "b1", shiftId = null, clockIn = null, clockOut = null,
        status = AttendanceStatus.ABSENT, distanceToBranchMeters = null, workedMinutes = null,
        overtimeMinutes = null, justificationReason = "Cita médica", justificationStatus = status,
        reviewedBy = null, reviewedAt = null
    )

    @Test
    fun `awaitsReview solo con descargo PENDING`() {
        assertTrue(attendance(JustificationStatus.PENDING).awaitsReview)
        assertFalse(attendance(JustificationStatus.APPROVED).awaitsReview)
        assertFalse(attendance(JustificationStatus.REJECTED).awaitsReview)
        assertFalse(attendance(null).awaitsReview)
    }

    @Test
    fun `jobTitle es null hasta que el repositorio lo completa`() {
        assertEquals(null, attendance(null).jobTitle)
        assertEquals("Mecánico", attendance(null).copy(jobTitle = "Mecánico").jobTitle)
    }
}
