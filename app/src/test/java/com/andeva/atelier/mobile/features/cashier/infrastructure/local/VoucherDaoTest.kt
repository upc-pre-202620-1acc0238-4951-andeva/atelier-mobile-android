package com.andeva.atelier.mobile.features.cashier.infrastructure.local

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.features.cashier.domain.VoucherStatus
import com.andeva.atelier.mobile.features.cashier.domain.VoucherType
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoucherDaoTest {

    private lateinit var db: AppDatabase
    private val dao get() = db.voucherDao()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun row(id: String, workOrderId: String?, number: Int = 1, status: String = "ACCEPTED_SUNAT", type: String = "03") =
        VoucherEntity(id, workOrderId, type, "B001", number, "118.00", "0", "118.00", "PENDING", status, null, 0)

    @Test
    fun `replaceWorkOrder reemplaza solo los comprobantes de esa orden`() = runTest {
        dao.replaceWorkOrder("wo1", listOf(row("a", "wo1", 1), row("gone", "wo1", 2)))
        dao.replaceWorkOrder("wo2", listOf(row("z", "wo2")))

        dao.replaceWorkOrder("wo1", listOf(row("a", "wo1", 1), row("b", "wo1", 3)))

        assertEquals(listOf("a", "b"), dao.observeByWorkOrder("wo1").first().map { it.id })
        assertNull(dao.findById("gone"))
        assertEquals(listOf("z"), dao.observeByWorkOrder("wo2").first().map { it.id })
    }

    @Test
    fun `upsert actualiza la fila y el estado desconocido queda en null`() = runTest {
        dao.upsert(row("a", "wo1"))
        dao.upsert(row("a", "wo1", status = "VOIDED"))
        dao.upsert(row("b", "wo1", number = 2, status = "ALGO_NUEVO", type = "99"))

        assertEquals("VOIDED", dao.findById("a")!!.status)
        val summary = dao.findById("b")!!.toDomain()
        assertNull(summary.status)
        assertNull(summary.type)
        assertEquals(VoucherType.BOLETA, dao.findById("a")!!.toDomain().type)
        assertEquals(VoucherStatus.VOIDED, dao.findById("a")!!.toDomain().status)
    }
}
