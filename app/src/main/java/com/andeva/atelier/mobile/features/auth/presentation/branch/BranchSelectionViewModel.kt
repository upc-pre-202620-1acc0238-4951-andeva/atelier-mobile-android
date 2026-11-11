package com.andeva.atelier.mobile.features.auth.presentation.branch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.features.auth.application.GetSelectableBranchesUseCase
import com.andeva.atelier.mobile.features.auth.application.SelectBranchUseCase
import com.andeva.atelier.mobile.features.auth.application.SkipBranchChoiceUseCase
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BranchSelectionViewModel @Inject constructor(
    private val getBranches: GetSelectableBranchesUseCase,
    private val selectBranch: SelectBranchUseCase,
    private val skipBranchChoice: SkipBranchChoiceUseCase,
    private val observeSession: ObserveSessionUseCase? = null
) : ViewModel() {

    constructor(
        getBranches: GetSelectableBranchesUseCase,
        selectBranch: SelectBranchUseCase,
        skipBranchChoice: SkipBranchChoiceUseCase
    ) : this(getBranches, selectBranch, skipBranchChoice, null)

    private val _uiState = MutableStateFlow(BranchSelectionUiState())
    val uiState: StateFlow<BranchSelectionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** `GET /branches`. Preselecciona la sucursal asignada en la sesión o la primera sede disponible. */
    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val sessionBranchId = observeSession?.invoke()?.firstOrNull()?.branchId
            getBranches()
                .onSuccess { branches ->
                    _uiState.update { state ->
                        val assigned = sessionBranchId?.takeIf { id -> branches.any { it.id == id } }
                        val keep = state.selectedId?.takeIf { id -> branches.any { it.id == id } }
                        state.copy(
                            isLoading = false,
                            branches = branches,
                            assignedBranchId = assigned,
                            selectedId = keep ?: assigned ?: branches.firstOrNull()?.id
                        )
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.toUiText()) } }
        }
    }

    fun onSelect(branchId: String) = _uiState.update { if (it.isSaving) it else it.copy(selectedId = branchId) }

    fun onContinue() {
        val state = _uiState.value
        val branchId = state.selected?.id ?: return
        if (!state.canContinue) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            selectBranch(branchId)
            _uiState.update { it.copy(isSaving = false, done = true) }
        }
    }

    /** "Elegir después": entra sin sede, como hasta ahora, y no se vuelve a preguntar. */
    fun onSkip() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            skipBranchChoice()
            _uiState.update { it.copy(isSaving = false, done = true) }
        }
    }
}
