package com.andeva.atelier.mobile.features.main.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNavItem
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNavigationBar
import com.andeva.atelier.mobile.features.main.presentation.navigation.MainTab
import com.andeva.atelier.mobile.features.main.presentation.navigation.navigateToTab

/**
 * Barra de pestañas de la sesión. Solo se dibuja sobre la pantalla principal de una pestaña: en los detalles
 * (una orden, una tarea, un repuesto) se oculta para que la pantalla use todo el alto.
 */
@Composable
fun MainNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val destination = navController.currentBackStackEntryAsState().value?.destination ?: return
    val selectedIndex = state.tabs.indexOfFirst { destination.hasRoute(it.root) }
    if (selectedIndex < 0) return

    val items = state.tabs.map { tab ->
        AtelierNavItem(
            label = stringResource(tab.label),
            icon = tab.icon,
            badge = if (tab == state.tabs.last()) state.pendingCount else 0
        )
    }
    AtelierNavigationBar(
        items = items,
        selectedIndex = selectedIndex,
        onSelect = { index ->
            val tab = state.tabs[index]
            if (index != selectedIndex) navController.navigateToTab(tab)
        },
        modifier = modifier
    )
}

/** `true` si [destination] es la pantalla principal de alguna pestaña (para reservar el espacio de la barra). */
fun isTabRoot(destination: androidx.navigation.NavDestination?, tabs: List<MainTab>): Boolean =
    destination != null && tabs.any { destination.hasRoute(it.root) }
