package com.andeva.atelier.mobile.features.main.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCodeScanner
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
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.SkuScannerRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.BarPrimaryAction
import com.andeva.atelier.mobile.features.main.presentation.navigation.MainTab
import com.andeva.atelier.mobile.features.main.presentation.navigation.navigateToTab
import com.andeva.atelier.mobile.features.reception.presentation.navigation.ReceptionNavGraphRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.TasksNavGraphRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.WorkOrdersRoute

/**
 * Barra flotante de la sesión. Solo se dibuja sobre la pantalla principal de una pestaña: en los detalles se oculta.
 * El último slot, "Módulos", no navega: abre la hoja universal de módulos ([onOpenModules]) y muestra el contador
 * de operaciones pendientes. El botón circular de la derecha expone la acción principal que habilitan los permisos.
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
    val barAction: AtelierBarAction? = when (state.primaryAction) {
        BarPrimaryAction.RECEIVE_VEHICLE -> AtelierBarAction(
            label = stringResource(R.string.bar_action_receive),
            description = stringResource(R.string.today_receive_vehicle),
            icon = Icons.Outlined.DirectionsCar,
            onClick = { navController.navigate(ReceptionNavGraphRoute) { launchSingleTop = true } }
        )
        BarPrimaryAction.NEW_APPOINTMENT -> AtelierBarAction(
            label = stringResource(R.string.bar_action_appointment),
            description = stringResource(R.string.bar_action_appointment_desc),
            icon = Icons.Outlined.EventAvailable,
            onClick = { navController.navigate(ReceptionNavGraphRoute) { launchSingleTop = true } }
        )
        BarPrimaryAction.COLLECT_PAYMENT -> AtelierBarAction(
            label = stringResource(R.string.bar_action_collect),
            description = stringResource(R.string.bar_action_collect_desc),
            icon = Icons.Outlined.Payments,
            onClick = { navController.navigate(WorkOrdersRoute) { launchSingleTop = true } }
        )
        BarPrimaryAction.SCAN_SKU -> AtelierBarAction(
            label = stringResource(R.string.bar_action_scan),
            description = stringResource(R.string.bar_action_scan_desc),
            icon = Icons.Outlined.QrCodeScanner,
            onClick = { navController.navigate(SkuScannerRoute) { launchSingleTop = true } }
        )
        BarPrimaryAction.MY_TASKS -> AtelierBarAction(
            label = stringResource(R.string.bar_action_tasks),
            description = stringResource(R.string.bar_action_tasks_desc),
            icon = Icons.Outlined.AssignmentTurnedIn,
            onClick = { navController.navigate(TasksNavGraphRoute) { launchSingleTop = true } }
        )
        BarPrimaryAction.NONE -> null
    }

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
        action = barAction
    )
}

/** `true` si [destination] es la pantalla principal de alguna pestaña (la barra solo se dibuja ahí). */
fun isTabRoot(destination: androidx.navigation.NavDestination?, tabs: List<MainTab>): Boolean =
    destination != null && tabs.any { destination.hasRoute(it.root) }
