package com.andeva.atelier.mobile.features.team.infrastructure.remote

import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import com.andeva.atelier.mobile.features.attendance.infrastructure.remote.AttendanceDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TeamAttendanceMappersTest {
    private fun dto(
        id: String = "a1",
        membershipId: String? = "m1",
        branchId: String? = "b1",
        clockIn: String? = null,
        status: String? = "ABSENT",
        justificationStatus: String? = "PENDING",
        reviewedAt: String? = null
    ) = AttendanceDto(
        id = id, branchId = branchId, membershipId = membershipId, shiftId = null, clockIn = clockIn,
        clockOut = null, status = status, latitude = null, longitude = null, distanceToBranchMeters = null,
        justificationReason = "Cita", justificationStatus = justificationStatus, workedMinutes = null,
        regularMinutes = null, overtimeMinutes = null, justificationReviewedBy = "boss",
        justificationReviewedAt = reviewedAt
    )

    @Test
    fun `una falta ABSENT sin clockIn se mapea`() {
        val result = dto().toTeamAttendanceOrNull()
        assertNotNull(result)
        assertNull(result!!.clockIn)
        assertEquals(AttendanceStatus.ABSENT, result.status)
        assertEquals(JustificationStatus.PENDING, result.justificationStatus)
        assertTrue(result.awaitsReview)
        assertEquals("boss", result.reviewedBy)
    }

    @Test
    fun `un estado desconocido queda en null`() {
        val result = dto(status = "NEW_STATUS", justificationStatus = "NEW").toTeamAttendanceOrNull()!!
        assertNull(result.status)
        assertNull(result.justificationStatus)
    }

    @Test
    fun `lee clockIn y reviewedAt en UTC`() {
        val result = dto(clockIn = "2026-10-20T13:00:00Z", reviewedAt = "2026-10-21T15:00:00Z").toTeamAttendanceOrNull()!!
        assertEquals(Instant.parse("2026-10-20T13:00:00Z"), result.clockIn)
        assertEquals(Instant.parse("2026-10-21T15:00:00Z"), result.reviewedAt)
    }

    @Test
    fun `sin membershipId branchId o id falla el mapeo`() {
        assertNull(dto(membershipId = null).toTeamAttendanceOrNull())
        assertNull(dto(branchId = null).toTeamAttendanceOrNull())
        assertNull(dto(id = "").toTeamAttendanceOrNull())
    }

    @Test
    fun `una fecha ilegible falla el mapeo`() {
        assertNull(dto(clockIn = "ayer").toTeamAttendanceOrNull())
    }
}
