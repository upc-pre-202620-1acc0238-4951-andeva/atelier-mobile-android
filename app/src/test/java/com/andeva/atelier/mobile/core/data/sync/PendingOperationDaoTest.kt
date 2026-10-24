package com.andeva.atelier.mobile.core.data.sync

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

/** Las consultas SQL reales de la cola contra Room en memoria. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PendingOperationDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PendingOperationDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.pendingOperationDao()
    }

    @After
    fun tearDown() = db.close()

    private fun op(id: String, userId: String = "u1", dedupeKey: String = "CLOCK_IN:$id", nextAttemptAt: Long = 0, createdAt: Long = 0, dependsOn: String? = null) =
        PendingOperationEntity(
            id = id, type = "CLOCK_IN", payload = "{}", dedupeKey = dedupeKey, userId = userId, tenantId = "t1",
            nextAttemptAt = nextAttemptAt, createdAt = createdAt, dependsOn = dependsOn
        )

    @Test
    fun `dedupe_key evita encolar dos veces el mismo hecho`() = runTest {
        assertEquals(true, dao.enqueue(op("a", dedupeKey = "CLOCK_IN:m1:2026-10-23")) > 0)
        assertEquals(-1L, dao.enqueue(op("b", dedupeKey = "CLOCK_IN:m1:2026-10-23")))
        assertNull(dao.findById("b"))
    }

    @Test
    fun `nextBatch filtra por usuario, estado y hora, ordenado por captura`() = runTest {
        dao.enqueue(op("late", createdAt = 20))
        dao.enqueue(op("early", createdAt = 10))
        dao.enqueue(op("future", nextAttemptAt = 1_000))
        dao.enqueue(op("other", userId = "u2"))
        dao.enqueue(op("failed"))
        dao.markFailed("failed", 1, 422, "UNPROCESSABLE_ENTITY", "Fuera de geocerca")

        assertEquals(listOf("early", "late"), dao.nextBatch("u1", now = 100).map { it.id })

        // nextAttemptAt es el mínimo entre las PENDING (las FAILED no cuentan).
        dao.delete("early")
        dao.delete("late")
        assertEquals(1_000L, dao.nextAttemptAt("u1"))
    }

    @Test
    fun `dependientes, reinicio de SYNCING y borrado por usuario`() = runTest {
        dao.enqueue(op("upload"))
        dao.enqueue(op("attach", dependsOn = "upload"))
        dao.enqueue(op("other", userId = "u2"))
        assertEquals(listOf("attach"), dao.dependentIds("upload"))

        dao.markSyncing("upload")
        dao.resetSyncing("u1")
        assertEquals(PendingOperationEntity.STATUS_PENDING, dao.findById("upload")!!.status)

        assertEquals(2, dao.countPending("u1").first())
        dao.deleteForUser("u1")
        assertEquals(0, dao.countPending("u1").first())
        assertEquals(1, dao.countPending("u2").first())
    }

    @Test
    fun `resetFailed solo reactiva FAILED y allDependentIds incluye FAILED`() = runTest {
        dao.enqueue(op("upload"))
        dao.enqueue(op("attach", dependsOn = "upload"))
        dao.markFailed("upload", 3, 500, "INTERNAL_ERROR", "Error")
        dao.failAsDependent("attach", "INTERNAL_ERROR", "Error")
        assertEquals(emptyList<String>(), dao.dependentIds("upload"))
        assertEquals(listOf("attach"), dao.allDependentIds("upload"))

        dao.resetFailed("upload", now = 50)
        val upload = dao.findById("upload")!!
        assertEquals(PendingOperationEntity.STATUS_PENDING, upload.status)
        assertEquals(0, upload.attempts)
        assertEquals(50L, upload.nextAttemptAt)

        dao.markSyncing("upload")
        dao.resetFailed("upload", now = 99)
        assertEquals(PendingOperationEntity.STATUS_SYNCING, dao.findById("upload")!!.status)
    }

    @Test
    fun `findByDedupeKey y observeByDedupeKey encuentran el hecho en cola`() = runTest {
        assertNull(dao.observeByDedupeKey("CLOCK_IN:m1:2026-10-24").first())
        dao.enqueue(op("a", dedupeKey = "CLOCK_IN:m1:2026-10-24"))

        assertEquals("a", dao.findByDedupeKey("CLOCK_IN:m1:2026-10-24")!!.id)
        assertEquals("a", dao.observeByDedupeKey("CLOCK_IN:m1:2026-10-24").first()!!.id)
        assertNull(dao.findByDedupeKey("CLOCK_IN:m1:2026-10-25"))
    }

    @Test
    fun `deleteAll borra solo los ids indicados`() = runTest {
        dao.enqueue(op("a"))
        dao.enqueue(op("b"))
        dao.enqueue(op("c"))

        dao.deleteAll(listOf("a", "b"))

        assertNull(dao.findById("a"))
        assertNull(dao.findById("b"))
        assertEquals("c", dao.findById("c")!!.id)
    }
}
