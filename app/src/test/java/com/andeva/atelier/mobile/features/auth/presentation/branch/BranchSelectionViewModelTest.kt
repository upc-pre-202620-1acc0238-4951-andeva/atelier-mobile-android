package com.andeva.atelier.mobile.features.auth.presentation.branch

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.domain.ApiException
import com.andeva.atelier.mobile.features.auth.application.GetSelectableBranchesUseCase
import com.andeva.atelier.mobile.features.auth.application.SelectBranchUseCase
import com.andeva.atelier.mobile.features.auth.application.SkipBranchChoiceUseCase
import com.andeva.atelier.mobile.features.auth.domain.BranchChoiceRepository
import com.andeva.atelier.mobile.features.auth.domain.BranchOption
import com.andeva.atelier.mobile.features.auth.presentation.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class BranchSelectionViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val repository = mockk<BranchChoiceRepository>(relaxUnitFun = true)
    private val sanIsidro = BranchOption("1", "Sede San Isidro", 50)
    private val surquillo = BranchOption("2", "Sede Surquillo", 80)

    private fun viewModel() = BranchSelectionViewModel(
        GetSelectableBranchesUseCase(repository),
        SelectBranchUseCase(repository),
        SkipBranchChoiceUseCase(repository)
    )

    private fun BranchSelectionViewModel.settle() = mainRule.dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `carga las sedes y deja la primera preseleccionada`() = runTest(mainRule.dispatcher) {
        coEvery { repository.availableBranches() } returns Result.success(listOf(sanIsidro, surquillo))
        val vm = viewModel()
        assertTrue(vm.uiState.value.isLoading)
        vm.settle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(sanIsidro, surquillo), state.branches)
        assertEquals("1", state.selectedId)
        assertTrue(state.canContinue)
    }

    @Test
    fun `cambiar de sede actualiza la seleccion`() = runTest(mainRule.dispatcher) {
        coEvery { repository.availableBranches() } returns Result.success(listOf(sanIsidro, surquillo))
        val vm = viewModel()
        vm.settle()
        vm.onSelect("2")
        assertEquals(surquillo, vm.uiState.value.selected)
    }

    @Test
    fun `continuar guarda la sede elegida y avisa que termino`() = runTest(mainRule.dispatcher) {
        coEvery { repository.availableBranches() } returns Result.success(listOf(sanIsidro, surquillo))
        val vm = viewModel()
        vm.settle()
        vm.onSelect("2")
        vm.onContinue()
        vm.settle()

        coVerify(exactly = 1) { repository.selectBranch("2") }
        assertTrue(vm.uiState.value.done)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `elegir despues no guarda ninguna sede`() = runTest(mainRule.dispatcher) {
        coEvery { repository.availableBranches() } returns Result.success(listOf(sanIsidro))
        val vm = viewModel()
        vm.settle()
        vm.onSkip()
        vm.settle()

        coVerify(exactly = 1) { repository.skipBranchChoice() }
        coVerify(exactly = 0) { repository.selectBranch(any()) }
        assertTrue(vm.uiState.value.done)
    }

    @Test
    fun `sin sedes no se puede continuar pero si elegir despues`() = runTest(mainRule.dispatcher) {
        coEvery { repository.availableBranches() } returns Result.success(emptyList())
        val vm = viewModel()
        vm.settle()

        assertTrue(vm.uiState.value.branches.isEmpty())
        assertNull(vm.uiState.value.selectedId)
        assertFalse(vm.uiState.value.canContinue)
        vm.onContinue()
        vm.settle()
        coVerify(exactly = 0) { repository.selectBranch(any()) }
    }

    @Test
    fun `un error muestra el mensaje y reintentar vuelve a cargar`() = runTest(mainRule.dispatcher) {
        coEvery { repository.availableBranches() } returnsMany listOf(
            Result.failure(ApiException(500, "ERR", "No se pudieron cargar las sedes.")),
            Result.success(listOf(sanIsidro))
        )
        val vm = viewModel()
        vm.settle()
        assertEquals("No se pudieron cargar las sedes.", (vm.uiState.value.error as UiText.Dynamic).value)

        vm.load()
        vm.settle()
        assertNull(vm.uiState.value.error)
        assertEquals("1", vm.uiState.value.selectedId)
    }

    @Test
    fun `sin red se muestra el aviso de conexion`() = runTest(mainRule.dispatcher) {
        coEvery { repository.availableBranches() } returns Result.failure(IOException("sin red"))
        val vm = viewModel()
        vm.settle()
        assertTrue(vm.uiState.value.error is UiText.Resource)
    }

    @Test
    fun `al reintentar se conserva la sede que ya habia elegido`() = runTest(mainRule.dispatcher) {
        coEvery { repository.availableBranches() } returns Result.success(listOf(sanIsidro, surquillo))
        val vm = viewModel()
        vm.settle()
        vm.onSelect("2")
        vm.load()
        vm.settle()
        assertEquals("2", vm.uiState.value.selectedId)
    }
}
