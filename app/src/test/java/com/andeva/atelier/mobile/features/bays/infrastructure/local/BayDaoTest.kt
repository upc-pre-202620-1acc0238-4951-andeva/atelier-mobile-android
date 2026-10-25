package com.andeva.atelier.mobile.features.bays.infrastructure.local

import android.content.Context
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.andeva.atelier.mobile.core.data.local.AppDatabase
import com.andeva.atelier.mobile.features.bays.domain.BayStatus
import com.andeva.atelier.mobile.features.bays.domain.BayType
import com.andeva.atelier.mobile.features.bays.infrastructure.remote.WorkBayDto
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
class BayDaoTest {

    private lateinit var db: AppDatabase
    private val dao get() = db.bayDao()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun row(id: String, branchId: String, name: String) =
        BayEntity(id, branchId, name, "LIFT", "AVAILABLE", null, 0)

    @Test
    fun `replaceBranch borra las bahias que ya no existen y observeBranch ordena por nombre`() = runTest {
        dao.replaceBranch("b1", listOf(row("x", "b1", "Elevador 2"), row("y", "b1", "Elevador 1"), row("gone", "b1", "Vieja")))
        dao.replaceBranch("b2", listOf(row("z", "b2", "Otra sede")))

        dao.replaceBranch("b1", listOf(row("x", "b1", "Elevador 2"), row("y", "b1", "Elevador 1")))

        assertEquals(listOf("y", "x"), dao.observeBranch("b1").first().map { it.id })
        assertNull(dao.findById("gone"))
        assertEquals(listOf("z"), dao.observeBranch("b2").first().map { it.id })
    }

    @Test
    fun `enum desconocido queda en null y el dto sin sede usa la consultada`() {
        val entity = WorkBayDto("x", null, "Foso", "SPACESHIP", "OCCUPIED", "wo1").toEntity("b1", 5)

        val bay = entity.toDomain()

        assertEquals("b1", bay.branchId)
        assertNull(bay.type)
        assertEquals(BayStatus.OCCUPIED, bay.status)
        assertEquals(BayType.LIFT, row("a", "b1", "A").toDomain().type)
    }
}
