package com.andeva.atelier.mobile.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.andeva.atelier.mobile.core.notifications.PushDestination
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.AttendanceRoute
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.attendanceNavGraph
import com.andeva.atelier.mobile.features.auth.presentation.navigation.BranchSelectionRoute
import com.andeva.atelier.mobile.features.auth.presentation.navigation.authNavGraph
import com.andeva.atelier.mobile.features.auth.presentation.navigation.branchSelectionDestination
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.PartDetailRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.inventoryNavGraph
import com.andeva.atelier.mobile.features.main.presentation.MainNavigationBar
import com.andeva.atelier.mobile.features.main.presentation.MainViewModel
import com.andeva.atelier.mobile.features.main.presentation.isTabRoot
import com.andeva.atelier.mobile.features.main.presentation.navigation.AdminAttendanceRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.AdminDashboardRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.mainTabsDestinations
import com.andeva.atelier.mobile.features.reception.presentation.navigation.receptionNavGraph
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.TaskDetailRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.tasksNavGraph
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.WorkOrderDetailRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.workOrderNavGraph
import kotlinx.serialization.Serializable

/** Grafo sin sesión. Sus pantallas las define la feature `auth` (`authNavGraph`). */
@Serializable
data object AuthNavGraphRoute

/** Grafo con sesión. Las pestañas las define `main` ([com.andeva.atelier.mobile.features.main.presentation.navigation.MainTab]). */
@Serializable
data object MainNavGraphRoute

@Serializable
data object MainStartRoute

@Composable
fun AppNavHost(viewModel: AppNavHostViewModel = hiltViewModel()) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    if (sessionState == SessionState.Loading) return

    val signedInState = sessionState as? SessionState.SignedIn
    val signedIn = signedInState != null
    val navController = rememberNavController()

    // La barra solo existe con sesión y sobre la pantalla principal de una pestaña; los detalles usan todo el alto.
    val currentEntry by navController.currentBackStackEntryAsState()
    val mainViewModel: MainViewModel = hiltViewModel()
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val showBar = signedIn && isTabRoot(currentEntry?.destination, mainState.tabs)

    val pendingPush by viewModel.pendingPush.collectAsStateWithLifecycle()
    LaunchedEffect(pendingPush, signedIn) {
        if (!signedIn || pendingPush == null) return@LaunchedEffect
        val destination = viewModel.consumePush() ?: return@LaunchedEffect
        val target: Any = when (destination) {
            is PushDestination.TaskDetail -> TaskDetailRoute(destination.workOrderId, destination.taskId)
            is PushDestination.TaskSuspensionReview -> WorkOrderDetailRoute(destination.workOrderId)
            is PushDestination.TaskProducts -> TaskDetailRoute(destination.workOrderId, destination.taskId)
            is PushDestination.WorkOrderDetail -> WorkOrderDetailRoute(destination.workOrderId)
            is PushDestination.LowStockAlerts -> PartDetailRoute(destination.itemId)
            is PushDestination.CollaboratorAttendance -> AdminAttendanceRoute
            is PushDestination.MyAttendance -> AttendanceRoute
            is PushDestination.ClockIn -> AttendanceRoute
        }
        navController.navigate(target) { launchSingleTop = true }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { if (showBar) MainNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (signedIn) MainNavGraphRoute else AuthNavGraphRoute,
            // El espacio de la barra ya lo reserva este padding: las pantallas no vuelven a descontar la inferior.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            authNavGraph(navController)
            val mainStartDestination: Any = when {
                signedInState?.needsBranchChoice == true -> BranchSelectionRoute
                signedInState?.prefersDashboard == true -> AdminDashboardRoute
                else -> AttendanceRoute
            }
            navigation<MainNavGraphRoute>(startDestination = mainStartDestination) {
                composable<MainStartRoute> {
                    LaunchedEffect(Unit) {
                        navController.navigate(mainStartDestination) {
                            popUpTo<MainStartRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                branchSelectionDestination(
                    onDone = {
                        val homeRoute: Any = if (signedInState?.prefersDashboard == true) {
                            AdminDashboardRoute
                        } else {
                            AttendanceRoute
                        }
                        navController.navigate(homeRoute) {
                            popUpTo<BranchSelectionRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
                attendanceNavGraph(navController)
                receptionNavGraph(navController)
                workOrderNavGraph(navController)
                tasksNavGraph(navController)
                inventoryNavGraph(navController)
                mainTabsDestinations(
                    navController = navController,
                    sessionFlow = viewModel.sessionFlow,
                    pendingSyncCountFlow = viewModel.pendingSyncCountFlow
                )
            }
        }
    }

    // Login, logout o sesión vencida: se cambia de grafo limpiando la pila.
    var shownSignedIn by rememberSaveable { mutableStateOf(signedIn) }
    LaunchedEffect(signedIn) {
        if (signedIn == shownSignedIn) return@LaunchedEffect
        val target: Any = if (signedIn) MainNavGraphRoute else AuthNavGraphRoute
        navController.navigate(target) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
        shownSignedIn = signedIn
    }
}
