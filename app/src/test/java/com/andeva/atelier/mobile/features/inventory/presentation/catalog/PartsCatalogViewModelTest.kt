package com.andeva.atelier.mobile.features.inventory.presentation.catalog

import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.inventory.application.ObservePartBrandsUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.application.RefreshPartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.application.SearchPartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

class PartsCatalogViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<InventoryRepository>()
    private val online = MutableStateFlow(true)
    private val brands = MutableStateFlow<List<String>>(emptyList())
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns online }

    private fun repuesto(
        id: String, stock: String, min: String = "2", category: ItemCategory = ItemCategory.BRAKES, brand: String? = null
    ) = Part(
        id = id, name = "Repuesto $id", sku = "SKU-$id", category = category,
        basePrice = Money(BigDecimal("50.00"), Money.PEN), totalStock = BigDecimal(stock),
        minimumStock = BigDecimal(min), unitOfMeasure = "UNIT", status = PartStatus.ACTIVE, brand = brand
    )

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u", email = "m@taller.pe", fullName = "Mecánico", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(permissions: List<Permission> = listOf(Permission.PARTS_READ)): PartsCatalogViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions))
        every { repository.observeBrands() } returns brands
        return PartsCatalogViewModel(
            observeSession,
            ObservePartsCatalogUseCase(repository),
            RefreshPartsCatalogUseCase(repository),
            SearchPartsCatalogUseCase(repository),
            ObservePartBrandsUseCase(repository),
            connectivity
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    private fun conCatalogo(vararg parts: Part, category: ItemCategory? = null) {
        every { repository.observeCatalog(category) } returns flowOf(parts.toList())
    }

    @Test
    fun `al abrir recarga el catalogo y muestra la cache`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10"), repuesto("2", "3"))
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        val vm = viewModel()
        settle()

        assertEquals(listOf("1", "2"), vm.uiState.value.parts.map { it.id })
        assertTrue(vm.uiState.value.isLoaded)
        assertFalse(vm.uiState.value.showingCache)
        coVerify(exactly = 1) { repository.refreshCatalog(false) }
    }

    @Test
    fun `sin red conserva el catalogo guardado y avisa`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10"))
        coEvery { repository.refreshCatalog(false) } returns Result.failure(IOException())
        val vm = viewModel()
        settle()

        assertEquals(1, vm.uiState.value.parts.size)
        assertTrue(vm.uiState.value.showingCache)
        assertTrue(vm.uiState.value.error is UiText.Resource)
    }

    @Test
    fun `busca por nombre o sku despues del debounce`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10"), repuesto("2", "3"))
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        coEvery { repository.searchParts("SKU-2", null) } returns Result.success(listOf(repuesto("2", "3")))
        val vm = viewModel()
        settle()

        vm.onQueryChange("SKU-2")
        mainRule.dispatcher.scheduler.advanceTimeBy(100)
        coVerify(exactly = 0) { repository.searchParts(any(), any()) }

        settle()
        assertEquals(listOf("2"), vm.uiState.value.parts.map { it.id })
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun `solo busca el ultimo texto tecleado`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10"))
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        coEvery { repository.searchParts(any(), any()) } returns Result.success(emptyList())
        val vm = viewModel()
        settle()

        vm.onQueryChange("pas")
        mainRule.dispatcher.scheduler.advanceTimeBy(100)
        vm.onQueryChange("pastilla")
        settle()

        coVerify(exactly = 1) { repository.searchParts("pastilla", null) }
        coVerify(exactly = 0) { repository.searchParts("pas", any()) }
    }

    @Test
    fun `la categoria filtra el catalogo y tocarla de nuevo la quita`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10"), repuesto("2", "3", category = ItemCategory.ENGINE))
        conCatalogo(repuesto("1", "10"), category = ItemCategory.BRAKES)
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        val vm = viewModel()
        settle()

        vm.onCategoryClick(ItemCategory.BRAKES)
        settle()
        assertEquals(ItemCategory.BRAKES, vm.uiState.value.category)
        assertEquals(listOf("1"), vm.uiState.value.parts.map { it.id })

        vm.onCategoryClick(ItemCategory.BRAKES)
        settle()
        assertNull(vm.uiState.value.category)
        assertEquals(2, vm.uiState.value.parts.size)
    }

    @Test
    fun `un error de busqueda muestra el mensaje del backend`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10"))
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        coEvery { repository.searchParts("x", null) } returns
            Result.failure(ApiException(400, "BAD_REQUEST", "Búsqueda inválida."))
        val vm = viewModel()
        settle()

        vm.onQueryChange("x")
        settle()

        assertEquals("Búsqueda inválida.", (vm.uiState.value.error as UiText.Dynamic).value)
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun `quitar filtros vuelve al catalogo completo`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10"), repuesto("2", "0"))
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        coEvery { repository.searchParts("zzz", null) } returns Result.success(emptyList())
        val vm = viewModel()
        settle()

        vm.onQueryChange("zzz")
        settle()
        assertTrue(vm.uiState.value.parts.isEmpty())
        assertTrue(vm.uiState.value.hasFilters)

        vm.onClearFilters()
        settle()
        assertEquals(2, vm.uiState.value.parts.size)
        assertFalse(vm.uiState.value.hasFilters)
    }

    @Test
    fun `los agotados se detectan para bloquear su seleccion`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10"), repuesto("2", "0"))
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        val vm = viewModel()
        settle()

        val byId = vm.uiState.value.parts.associateBy { it.id }
        assertFalse(byId.getValue("1").isOutOfStock)
        assertTrue(byId.getValue("2").isOutOfStock)
        assertFalse(byId.getValue("2").isSelectable)
    }

    @Test
    fun `sin el permiso de repuestos marca la pantalla como no permitida`() = runTest(mainRule.dispatcher) {
        conCatalogo()
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        val vm = viewModel(listOf(Permission.TASKS_READ))
        settle()
        assertFalse(vm.uiState.value.canRead)
    }

    @Test
    fun `actualizar manualmente fuerza la recarga`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10"))
        coEvery { repository.refreshCatalog(any()) } returns Result.success(Unit)
        val vm = viewModel()
        settle()

        vm.refresh(force = true)
        settle()
        coVerify(exactly = 1) { repository.refreshCatalog(true) }
    }

    @Test
    fun `al volver la red recarga el catalogo y quita el aviso`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10"))
        coEvery { repository.refreshCatalog(false) } returnsMany listOf(Result.failure(IOException()), Result.success(Unit))
        online.value = false
        val vm = viewModel()
        settle()
        assertTrue(vm.uiState.value.showingCache)

        online.value = true
        settle()

        assertFalse(vm.uiState.value.showingCache)
        coVerify(exactly = 2) { repository.refreshCatalog(false) }
    }

    // --- US31: filtro por marca (H35) ---

    @Test
    fun `sin marcas en el catalogo no hay filtro de marca`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10"))
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        val vm = viewModel()
        settle()

        assertTrue(vm.uiState.value.brands.isEmpty())
        assertNull(vm.uiState.value.brand)
    }

    @Test
    fun `la marca filtra el catalogo y tocarla de nuevo la quita`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10", brand = "Bosch"), repuesto("2", "3", brand = "Mobil"))
        every { repository.observeCatalog(null, "Bosch") } returns flowOf(listOf(repuesto("1", "10", brand = "Bosch")))
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        brands.value = listOf("Bosch", "Mobil")
        val vm = viewModel()
        settle()
        assertEquals(listOf("Bosch", "Mobil"), vm.uiState.value.brands)

        vm.onBrandClick("Bosch")
        settle()
        assertEquals("Bosch", vm.uiState.value.brand)
        assertEquals(listOf("1"), vm.uiState.value.parts.map { it.id })
        assertTrue(vm.uiState.value.hasFilters)

        vm.onBrandClick("Bosch")
        settle()
        assertNull(vm.uiState.value.brand)
        assertEquals(2, vm.uiState.value.parts.size)
    }

    @Test
    fun `la marca se combina con el texto de busqueda`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10", brand = "Bosch"))
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        coEvery { repository.searchParts("pas", null, "Bosch") } returns Result.success(listOf(repuesto("1", "10", brand = "Bosch")))
        brands.value = listOf("Bosch")
        val vm = viewModel()
        settle()

        vm.onBrandClick("Bosch")
        vm.onQueryChange("pas")
        settle()

        coVerify { repository.searchParts("pas", null, "Bosch") }
        assertEquals(listOf("1"), vm.uiState.value.parts.map { it.id })
    }

    @Test
    fun `si la marca elegida desaparece del catalogo se quita el filtro`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10", brand = "Bosch"))
        every { repository.observeCatalog(null, "Bosch") } returns flowOf(listOf(repuesto("1", "10", brand = "Bosch")))
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        brands.value = listOf("Bosch")
        val vm = viewModel()
        settle()
        vm.onBrandClick("Bosch")
        settle()
        assertEquals("Bosch", vm.uiState.value.brand)

        brands.value = emptyList()
        settle()

        assertNull(vm.uiState.value.brand)
        assertEquals(1, vm.uiState.value.parts.size)
    }

    @Test
    fun `quitar filtros tambien quita la marca`() = runTest(mainRule.dispatcher) {
        conCatalogo(repuesto("1", "10", brand = "Bosch"))
        every { repository.observeCatalog(null, "Bosch") } returns flowOf(listOf(repuesto("1", "10", brand = "Bosch")))
        coEvery { repository.refreshCatalog(false) } returns Result.success(Unit)
        brands.value = listOf("Bosch")
        val vm = viewModel()
        settle()
        vm.onBrandClick("Bosch")
        settle()

        vm.onClearFilters()
        settle()

        assertNull(vm.uiState.value.brand)
        assertFalse(vm.uiState.value.hasFilters)
    }
}
