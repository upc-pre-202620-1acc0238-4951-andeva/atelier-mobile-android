package com.andeva.atelier.mobile.features.main.presentation

import com.andeva.atelier.mobile.features.main.presentation.navigation.MainTab

data class MainUiState(
    val tabs: List<MainTab> = emptyList(),
    /** La barra añade al final el slot "Módulos", que abre la hoja de módulos. */
    val showModules: Boolean = false,
    /** El botón circular de la barra: "Recibir vehículo" (`operations:work_orders:create`). */
    val canReceiveVehicle: Boolean = false,
    /** Operaciones sin sincronizar del usuario; van como contador en "Módulos" (o en la última pestaña sin ese slot). */
    val pendingCount: Int = 0
)
