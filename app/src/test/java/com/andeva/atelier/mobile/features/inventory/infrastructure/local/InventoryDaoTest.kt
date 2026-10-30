package com.andeva.atelier.mobile.features.inventory.infrastructure.local

import com.andeva.atelier.mobile.features.inventory.infrastructure.InventoryTestEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InventoryDaoTest {

    private val env = InventoryTestEnv()

    @Before
    fun setUp() = env.start()

    @After
    fun tearDown() = env.stop()

    private fun part(id: String, stock: String, minimum: String, category: String = "BRAKES", brand: String? = null) =
        PartEntity(id, "Repuesto $id", "SKU-$id", category, "10", stock, minimum, "UNIT", "ACTIVE", 0, brand)

    @Test
    fun `el stock bajo compara numeros y no texto`() = runTest {
        // Como texto "10" <= "9" sería verdadero; como número no
        env.db.partDao().replaceAll(listOf(part("a", "10", "9"), part("b", "9", "10"), part("c", "5.0", "5")))

        assertEquals(listOf("b", "c"), env.db.partDao().findLowStock().map { it.id })
        assertEquals(listOf("b", "c"), env.db.partDao().observeLowStock().first().map { it.id })
    }

    @Test
    fun `filtro por categoria y lotes de un repuesto en orden FIFO`() = runTest {
        env.db.partDao().replaceAll(listOf(part("a", "1", "0"), part("b", "1", "0", "FILTERS")))
        env.db.batchDao().replaceForItem(
            "a", listOf(BatchEntity("new", "a", "L2", "3", "40", 2_000, 0), BatchEntity("old", "a", "L1", "1", "35", 1_000, 0))
        )

        assertEquals(listOf("b"), env.db.partDao().observeAll("FILTERS", null).first().map { it.id })
        assertEquals(2, env.db.partDao().observeAll(null, null).first().size)
        assertEquals(listOf("old", "new"), env.db.batchDao().observeForItem("a").first().map { it.id })
    }

    @Test
    fun `filtro por marca en el catalogo y en la busqueda sin red`() = runTest {
        env.db.partDao().replaceAll(
            listOf(
                part("a", "1", "0", brand = "Bosch"),
                part("b", "1", "0", brand = "Mobil"),
                part("c", "1", "0", "FILTERS", brand = "Bosch"),
                part("d", "1", "0")
            )
        )

        assertEquals(listOf("a", "c"), env.db.partDao().observeAll(null, "Bosch").first().map { it.id })
        assertEquals(listOf("c"), env.db.partDao().observeAll("FILTERS", "Bosch").first().map { it.id })
        assertEquals(4, env.db.partDao().observeAll(null, null).first().size)
        assertEquals(listOf("b"), env.db.partDao().search("Repuesto", null, "Mobil").map { it.id })
        assertEquals(4, env.db.partDao().search("Repuesto", null, null).size)
    }

    @Test
    fun `las marcas son distintas, ordenadas y sin vacios`() = runTest {
        env.db.partDao().replaceAll(
            listOf(
                part("a", "1", "0", brand = "Mobil"),
                part("b", "1", "0", brand = "Bosch"),
                part("c", "1", "0", brand = "Bosch"),
                part("d", "1", "0", brand = ""),
                part("e", "1", "0")
            )
        )

        assertEquals(listOf("Bosch", "Mobil"), env.db.partDao().observeBrands().first())
    }

    @Test
    fun `sin marcas en el catalogo la lista de marcas esta vacia`() = runTest {
        env.db.partDao().replaceAll(listOf(part("a", "1", "0"), part("b", "1", "0")))

        assertEquals(emptyList<String>(), env.db.partDao().observeBrands().first())
    }
}
