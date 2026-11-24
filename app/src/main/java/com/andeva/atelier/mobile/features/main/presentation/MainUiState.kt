package com.andeva.atelier.mobile.features.main.presentation

import com.andeva.atelier.mobile.features.main.presentation.navigation.BarPrimaryAction
import com.andeva.atelier.mobile.features.main.presentation.navigation.MainTab

data class MainUiState(
    val tabs: List<MainTab> = emptyList(),
    /** La barra añade al final el slot "Módulos", que abre la hoja universal de módulos. */
    val showModules: Boolean = true,
    /** Acción principal del botón circular de 64 dp a la derecha de la cápsula, decidida por permiso. */
    val primaryAction: BarPrimaryAction = BarPrimaryAction.NONE,
    /** Operaciones sin sincronizar del usuario; van como contador en "Módulos". */
    val pendingCount: Int = 0
)
