package com.andeva.atelier.mobile.features.inventory.presentation.detail

import androidx.lifecycle.SavedStateHandle
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import com.andeva.atelier.mobile.features.inventory.application.DispatchFifoBatchUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartBatchesUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartUseCase
import com.andeva.atelier.mobile.features.inventory.application.ReceiveDirectBatchUseCase
import com.andeva.atelier.mobile.features.inventory.application.RefreshPartBatchesUseCase
import com.andeva.atelier.mobile.features.inventory.application.RestoreBatchUseCase
import com.andeva.atelier.mobile.features.inventory.application.UpdatePartUseCase
import com.andeva.atelier.mobile.features.inventory.domain.BatchRestoreResult
import com.andeva.atelier.mobile.features.inventory.domain.StockAllocation
import java.time.Clock
import java.time.ZoneOffset
import com.andeva.atelier.mobile.features.inventory.domain.Batch
import com.andeva.atelier.mobile.features.inventory.domain.InventoryRepository
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import io.mockk.coEvery
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
import java.time.Instant

class PartDetailViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<InventoryRepository>()
    private val online = MutableStateFlow(true)
    private val connectivity = mockk<ConnectivityObserver> { every { isOnline } returns online }
    private val clock = Clock.fixed(Instant.parse("2026-10-24T14:00:00Z"), ZoneOffset.UTC)

    private val repuesto = Part(
        id = "p1", name = "Pastillas", sku = "BRK-001", category = ItemCategory.BRAKES,
        basePrice = Money(BigDecimal("80.00"), Money.PEN), totalStock = BigDecimal("14"),
        minimumStock = BigDecimal("5"), unitOfMeasure = "UNIT", status = PartStatus.ACTIVE
    )

    private fun lote(id: String, remaining: String, arrival: String?) = Batch(
        id = id, itemId = "p1", batchNumber = "L-$id", remainingQty = BigDecimal(remaining),
        unitCost = Money(BigDecimal("50.00"), Money.PEN), arrivalDate = arrival?.let { Instant.parse(it) }
    )

    private fun sessionWith(permissions: List<Permission>) = Session(
        userId = "u", email = "m@taller.pe", fullName = "Mecánico", tenantId = "t", tenantName = "Taller",
        membershipId = "m", permissions = permissions.map { it.value }.toSet(), plan = null
    )

    private fun viewModel(
        batches: List<Batch>,
        permissions: List<Permission> = listOf(Permission.PARTS_READ, Permission.BATCHES_READ)
    ): PartDetailViewModel {
        val observeSession = mockk<ObserveSessionUseCase>()
        every { observeSession() } returns flowOf(sessionWith(permissions))
        every { repository.observePart("p1") } returns flowOf(repuesto)
        every { repository.observeBatches("p1") } returns flowOf(batches)
        return PartDetailViewModel(
            observeSession,
            ObservePartUseCase(repository),
            ObservePartBatchesUseCase(repository),
            RefreshPartBatchesUseCase(repository),
            connectivity,
            SavedStateHandle(mapOf("partId" to "p1")),
            UpdatePartUseCase(repository),
            ReceiveDirectBatchUseCase(repository, clock),
            DispatchFifoBatchUseCase(repository),
            RestoreBatchUseCase(repository)
        )
    }

    private fun settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `ordena los lotes del mas antiguo al mas nuevo`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBatches("p1", false) } returns Result.success(Unit)
        val vm = viewModel(
            listOf(
                lote("c", "8", "2026-07-10T12:00:00Z"),
                lote("a", "0", "2026-03-10T12:00:00Z"),
                lote("b", "6", "2026-05-10T12:00:00Z")
            )
        )
        settle()

        assertEquals(listOf("a", "b", "c"), vm.uiState.value.batches.map { it.id })
    }

    @Test
    fun `el siguiente en salir es el lote activo mas antiguo`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBatches("p1", false) } returns Result.success(Unit)
        val vm = viewModel(
            listOf(
                lote("a", "0", "2026-03-10T12:00:00Z"),
                lote("b", "6", "2026-05-10T12:00:00Z"),
                lote("c", "8", "2026-07-10T12:00:00Z")
            )
        )
        settle()

        assertEquals("b", vm.uiState.value.nextBatchId)
        assertEquals(BigDecimal("14"), vm.uiState.value.batchesRemaining)
    }

    @Test
    fun `sin lotes con saldo no hay siguiente`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBatches("p1", false) } returns Result.success(Unit)
        val vm = viewModel(listOf(lote("a", "0", "2026-03-10T12:00:00Z")))
        settle()

        assertNull(vm.uiState.value.nextBatchId)
    }

    @Test
    fun `un lote sin fecha va al final`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBatches("p1", false) } returns Result.success(Unit)
        val vm = viewModel(listOf(lote("x", "3", null), lote("a", "2", "2026-03-10T12:00:00Z")))
        settle()

        assertEquals(listOf("a", "x"), vm.uiState.value.batches.map { it.id })
    }

    @Test
    fun `muestra la ficha del repuesto desde la cache`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBatches("p1", false) } returns Result.success(Unit)
        val vm = viewModel(emptyList())
        settle()

        assertEquals("Pastillas", vm.uiState.value.part?.name)
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.showingCache)
    }

    @Test
    fun `sin red conserva los lotes guardados y avisa`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBatches("p1", false) } returns Result.failure(IOException())
        val vm = viewModel(listOf(lote("a", "4", "2026-03-10T12:00:00Z")))
        settle()

        assertEquals(1, vm.uiState.value.batches.size)
        assertTrue(vm.uiState.value.showingCache)
        assertTrue(vm.uiState.value.error is UiText.Resource)
    }

    @Test
    fun `un error del backend muestra su mensaje`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBatches("p1", false) } returns
            Result.failure(ApiException(403, "FORBIDDEN", "No puedes ver los lotes."))
        val vm = viewModel(emptyList())
        settle()

        assertEquals("No puedes ver los lotes.", (vm.uiState.value.error as UiText.Dynamic).value)
    }

    @Test
    fun `solo con el permiso de lotes se habilita su lectura`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBatches("p1", false) } returns Result.success(Unit)
        val vm = viewModel(emptyList(), listOf(Permission.PARTS_READ))
        settle()

        assertFalse(vm.uiState.value.canReadBatches)
    }

    @Test
    fun `al volver la red recarga los lotes y quita el aviso`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBatches("p1", false) } returnsMany listOf(Result.failure(IOException()), Result.success(Unit))
        online.value = false
        val vm = viewModel(listOf(lote("a", "4", "2026-03-10T12:00:00Z")))
        settle()
        assertTrue(vm.uiState.value.showingCache)

        online.value = true
        settle()

        assertFalse(vm.uiState.value.showingCache)
    }

    @Test
    fun `edicion y operaciones directas de lote FIFO actualizan estado`() = runTest(mainRule.dispatcher) {
        coEvery { repository.refreshBatches("p1", false) } returns Result.success(Unit)
        coEvery {
            repository.updatePart("p1", "Pastillas Brembo", ItemCategory.BRAKES, BigDecimal("95.00"), BigDecimal("6"), PartStatus.ACTIVE, "UNIT")
        } returns Result.success(repuesto.copy(name = "Pastillas Brembo"))
        coEvery {
            repository.receiveDirectBatch("p1", "L-99", BigDecimal("5"), BigDecimal("40.00"), null, null)
        } returns Result.success(lote("b99", "5", "2026-10-24T14:00:00Z"))
        coEvery {
            repository.dispatchFifoBatch("p1", BigDecimal("2"), null, null)
        } returns Result.success(StockAllocation("a1", BigDecimal("2"), Money(BigDecimal("80.00"), Money.PEN), emptyList()))
        coEvery {
            repository.restoreBatch("p1", BigDecimal("1"), null, "Sobró", null)
        } returns Result.success(BatchRestoreResult("RESTORED", "OK", "b99", BigDecimal("1")))

        val vm = viewModel(
            emptyList(),
            listOf(
                Permission.PARTS_READ, Permission.PARTS_MANAGE, Permission.BATCHES_READ,
                Permission.BATCHES_RECEIVE, Permission.BATCHES_DISPATCH_FIFO, Permission.BATCHES_RESTORE
            )
        )
        settle()

        vm.onOpenEditPart()
        vm.onEditNameChange("Pastillas Brembo")
        vm.onEditBasePriceChange("95.00")
        vm.onEditMinStockChange("6")
        vm.onConfirmEditPart()
        settle()
        assertEquals(PartDetailSheet.NONE, vm.uiState.value.activeSheet)

        vm.onOpenReceiveBatch()
        vm.onBatchNumberChange("L-99")
        vm.onBatchQuantityChange("5")
        vm.onBatchUnitCostChange("40.00")
        vm.onConfirmReceiveBatch()
        settle()
        assertEquals(PartDetailSheet.NONE, vm.uiState.value.activeSheet)

        vm.onOpenDispatchFifo()
        vm.onBatchQuantityChange("2")
        vm.onConfirmDispatchFifo()
        settle()
        assertEquals(PartDetailSheet.NONE, vm.uiState.value.activeSheet)

        vm.onOpenRestoreBatch()
        vm.onBatchQuantityChange("1")
        vm.onRestoreReasonChange("Sobró")
        vm.onConfirmRestoreBatch()
        settle()
        assertEquals(PartDetailSheet.NONE, vm.uiState.value.activeSheet)
    }
}
