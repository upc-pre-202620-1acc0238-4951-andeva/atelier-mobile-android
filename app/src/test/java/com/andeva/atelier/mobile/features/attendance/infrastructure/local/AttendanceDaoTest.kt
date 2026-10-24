package com.andeva.atelier.mobile.features.attendance.infrastructure.local

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.andeva.atelier.mobile.core.data.local.AppDatabase
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

/** Las consultas SQL de la caché de asistencia contra Room en memoria. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AttendanceDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AttendanceDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.attendanceDao()
    }

    @After
    fun tearDown() = db.close()

    private fun row(id: String, clockIn: Long, clockOut: Long? = null, membershipId: String = "m1") = AttendanceEntity(
        id = id, branchId = "b1", membershipId = membershipId, shiftId = "s1", clockIn = clockIn, clockOut = clockOut,
        status = "ON_TIME", latitude = 0.0, longitude = 0.0, distanceToBranchMeters = 0.0, workedMinutes = null,
        regularMinutes = null, overtimeMinutes = null, justificationReason = null, justificationStatus = null, syncedAt = 0
    )

    @Test
    fun `ultima jornada, jornada abierta e historial desde una fecha`() = runTest {
        dao.upsertAll(listOf(row("a", 100, 200), row("b", 300), row("c", 50, 60), row("x", 999, membershipId = "m2")))

        assertEquals("b", dao.observeLatest("m1").first()!!.id)
        assertEquals("b", dao.findOpen("m1")!!.id)
        assertEquals(listOf("b", "a"), dao.observeSince("m1", 100).first().map { it.id })
        assertNull(dao.observeLatest("m3").first())
    }

    @Test
    fun `replaceRange solo reemplaza el rango del colaborador`() = runTest {
        dao.upsertAll(listOf(row("old", 10, 20), row("in-range", 150, 160), row("other", 150, membershipId = "m2")))

        dao.replaceRange("m1", fromMillis = 100, toMillis = 200, attendances = listOf(row("new", 120, 130)))

        assertEquals(listOf("new", "old"), dao.observeSince("m1", 0).first().map { it.id })
        assertEquals("other", dao.findById("other")!!.id)
        assertNull(dao.findById("in-range"))
    }
}
