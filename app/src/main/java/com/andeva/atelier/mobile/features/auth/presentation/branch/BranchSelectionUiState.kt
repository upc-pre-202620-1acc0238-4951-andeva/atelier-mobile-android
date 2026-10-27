package com.andeva.atelier.mobile.features.auth.presentation.branch

import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.features.auth.domain.BranchOption

data class BranchSelectionUiState(
    val isLoading: Boolean = true,
    val branches: List<BranchOption> = emptyList(),
    val selectedId: String? = null,
    val isSaving: Boolean = false,
    val error: UiText? = null,
    /** Un solo uso: la elección (o "después") quedó guardada y se puede entrar a la app. */
    val done: Boolean = false
) {
    val selected: BranchOption? get() = branches.firstOrNull { it.id == selectedId }
    val canContinue: Boolean get() = selected != null && !isSaving && !isLoading
}
