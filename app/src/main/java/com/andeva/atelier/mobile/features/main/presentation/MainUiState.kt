package com.andeva.atelier.mobile.features.main.presentation

import com.andeva.atelier.mobile.features.main.presentation.navigation.MainTab

data class MainUiState(
    val tabs: List<MainTab> = emptyList(),
    /** Operaciones sin sincronizar del usuario; se muestran como contador en la última pestaña. */
    val pendingCount: Int = 0
)
