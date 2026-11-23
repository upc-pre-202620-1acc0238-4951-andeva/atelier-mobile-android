package com.andeva.atelier.mobile.features.main.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBarAction
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNavItem
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNavigationBar
import com.andeva.atelier.mobile.features.main.presentation.navigation.MainTab
import com.andeva.atelier.mobile.features.main.presentation.navigation.navigateToTab
import com.andeva.atelier.mobile.features.reception.presentation.navigation.ReceptionNavGraphRoute

/**
 * Barra flotante de la sesión. Solo se dibuja sobre la pantalla principal de una pestaña: en los detalles se oculta.
 * El último slot, "Módulos", no navega: abre la hoja de módulos ([onOpenModules]) y se resalta mientras está abierta
 * ([modulesOpen]). El botón circular de la derecha recibe un vehículo.
 */
@Composable
fun MainNavigationBar(
    navController: NavController,
    modulesOpen: Boolean,
    onOpenModules: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val destination = navController.currentBackStackEntryAsState().value?.destination ?: return
    val tabIndex = state.tabs.indexOfFirst { destination.hasRoute(it.root) }
    if (tabIndex < 0) return

    val items = buildList {
        state.tabs.forEachIndexed { index, tab ->
            // Sin slot Módulos, la insignia de pendientes va en la última pestaña (barra de campo).
            val badge = if (!state.showModules && index == state.tabs.lastIndex) state.pendingCount else 0
            add(AtelierNavItem(label = stringResource(tab.label), icon = tab.icon, badge = badge))
        }
        if (state.showModules) {
            add(
                AtelierNavItem(
                    label = stringResource(R.string.main_tab_modules),
                    icon = Icons.Outlined.GridView,
                    badge = state.pendingCount
                )
            )
        }
    }
    val modulesIndex = if (state.showModules) items.lastIndex else -1
    val receiveLabel = stringResource(R.string.bar_action_receive)
    val receiveDescription = stringResource(R.string.today_receive_vehicle)
    AtelierNavigationBar(
        items = items,
        selectedIndex = if (modulesOpen && modulesIndex >= 0) modulesIndex else tabIndex,
        onSelect = { index ->
            when {
                index == modulesIndex -> onOpenModules()
                index != tabIndex -> navController.navigateToTab(state.tabs[index])
            }
        },
        modifier = modifier,
        action = if (state.canReceiveVehicle) {
            AtelierBarAction(receiveLabel, receiveDescription, Icons.Outlined.DirectionsCar) {
                navController.navigate(ReceptionNavGraphRoute)
            }
        } else null
    )
}

/** `true` si [destination] es la pantalla principal de alguna pestaña (la barra solo se dibuja ahí). */
fun isTabRoot(destination: androidx.navigation.NavDestination?, tabs: List<MainTab>): Boolean =
    destination != null && tabs.any { destination.hasRoute(it.root) }
