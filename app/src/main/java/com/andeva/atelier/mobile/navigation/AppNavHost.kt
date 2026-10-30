package com.andeva.atelier.mobile.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.andeva.atelier.mobile.BuildConfig
import com.andeva.atelier.mobile.features.auth.presentation.navigation.BranchSelectionRoute
import com.andeva.atelier.mobile.features.auth.presentation.navigation.authNavGraph
import com.andeva.atelier.mobile.features.auth.presentation.navigation.branchSelectionDestination
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.AttendanceHomeRoute
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.AttendanceRoute
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.attendanceNavGraph
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.InventoryNavGraphRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.inventoryNavGraph
import com.andeva.atelier.mobile.features.reception.presentation.navigation.receptionNavGraph
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.TasksNavGraphRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.tasksNavGraph
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.WorkOrdersRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.workOrderNavGraph
import kotlinx.serialization.Serializable

/** Grafo sin sesión. Sus pantallas las define la feature `auth` (`authNavGraph`). */
@Serializable
data object AuthNavGraphRoute

/** Grafo con sesión. La feature `main` reemplaza [MainStartRoute] por el Scaffold con pestañas. */
@Serializable
data object MainNavGraphRoute

@Serializable
data object MainStartRoute

@Composable
fun AppNavHost(viewModel: AppNavHostViewModel = hiltViewModel()) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    if (sessionState == SessionState.Loading) return

    val signedIn = sessionState is SessionState.SignedIn
    val navController = rememberNavController()

    Box {
        NavHost(
            navController = navController,
            startDestination = if (signedIn) MainNavGraphRoute else AuthNavGraphRoute
        ) {
            authNavGraph(navController)
            navigation<MainNavGraphRoute>(startDestination = MainStartRoute) {
                composable<MainStartRoute> {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        // Cuenta sin perfil laboral: primero elige sede (A5b); el resto entra directo
                        val needsBranch = (sessionState as? SessionState.SignedIn)?.needsBranchChoice == true
                        val destination: Any = if (needsBranch) BranchSelectionRoute else AttendanceRoute
                        navController.navigate(destination) {
                            popUpTo<MainStartRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                branchSelectionDestination(
                    onDone = {
                        navController.navigate(AttendanceRoute) {
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
            }
        }

        // Acceso provisional a tareas e inventario (solo debug) hasta que `main` tenga sus pestañas.
        val currentEntry by navController.currentBackStackEntryAsState()
        val onAttendance = currentEntry?.destination?.hasRoute<AttendanceHomeRoute>() == true
        if (BuildConfig.DEBUG && signedIn && onAttendance) {
            DebugEntryPoints(
                onOpenTasks = { navController.navigate(TasksNavGraphRoute) },
                onOpenCatalog = { navController.navigate(InventoryNavGraphRoute) },
                onOpenOrders = { navController.navigate(WorkOrdersRoute) },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
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
