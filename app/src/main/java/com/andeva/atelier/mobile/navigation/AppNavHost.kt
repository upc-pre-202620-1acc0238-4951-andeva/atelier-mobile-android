package com.andeva.atelier.mobile.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.rememberUpdatedState
import com.andeva.atelier.mobile.core.designsystem.component.EaseOutQuint
import com.andeva.atelier.mobile.core.designsystem.component.rememberReducedMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.component.LocalFloatingBarInset
import com.andeva.atelier.mobile.features.main.presentation.more.ModulesSheet
import com.andeva.atelier.mobile.features.main.presentation.navigation.navigateToModule
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.notifications.AuthDeepLink
import com.andeva.atelier.mobile.core.notifications.PushDestination
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.AttendanceRoute
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.attendanceNavGraph
import com.andeva.atelier.mobile.features.auth.presentation.navigation.BranchSelectionRoute
import com.andeva.atelier.mobile.features.auth.presentation.navigation.RegisterRoute
import com.andeva.atelier.mobile.features.auth.presentation.navigation.ResetPasswordRoute
import com.andeva.atelier.mobile.features.auth.presentation.navigation.authNavGraph
import com.andeva.atelier.mobile.features.auth.presentation.navigation.branchSelectionDestination
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.StockAlertsRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.inventoryNavGraph
import com.andeva.atelier.mobile.features.main.presentation.MainNavigationBar
import com.andeva.atelier.mobile.features.main.presentation.MainViewModel
import com.andeva.atelier.mobile.features.main.presentation.isTabRoot
import com.andeva.atelier.mobile.features.main.presentation.navigation.AdminAttendanceRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.AdminDashboardRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.MainTab
import com.andeva.atelier.mobile.features.main.presentation.navigation.mainTabsDestinations
import com.andeva.atelier.mobile.features.cashier.presentation.navigation.WorkOrderCheckoutRoute
import com.andeva.atelier.mobile.features.cashier.presentation.navigation.cashierNavGraph
import com.andeva.atelier.mobile.features.diagnostics.presentation.navigation.DiagnosticsHubRoute
import com.andeva.atelier.mobile.features.diagnostics.presentation.navigation.diagnosticsNavGraph
import com.andeva.atelier.mobile.features.main.presentation.navigation.NotificationsRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.PendingOperationsRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.ProfileRoute
import com.andeva.atelier.mobile.features.reception.presentation.navigation.ReceptionNavGraphRoute
import com.andeva.atelier.mobile.features.reception.presentation.navigation.receptionNavGraph
import com.andeva.atelier.mobile.features.reports.presentation.navigation.reportsNavGraph
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.TaskDetailRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.tasksNavGraph
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.ReportFindingRoute
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
    if (sessionState == SessionState.Loading) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val signedInState = sessionState as? SessionState.SignedIn
    val signedIn = signedInState != null
    val navController = rememberNavController()

    // La barra solo existe con sesión y sobre la pantalla principal de una pestaña; los detalles usan todo el alto.
    val currentEntry by navController.currentBackStackEntryAsState()
    val mainViewModel: MainViewModel = hiltViewModel()
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val showBar = signedIn && isTabRoot(currentEntry?.destination, mainState.tabs)
    val tabs by rememberUpdatedState(mainState.tabs)
    val reducedMotion = rememberReducedMotion()

    val pendingPush by viewModel.pendingPush.collectAsStateWithLifecycle()
    LaunchedEffect(pendingPush, signedIn) {
        if (!signedIn || pendingPush == null) return@LaunchedEffect
        val destination = viewModel.consumePush() ?: return@LaunchedEffect
        val target: Any = when (destination) {
            is PushDestination.TaskDetail -> TaskDetailRoute(destination.workOrderId, destination.taskId)
            is PushDestination.TaskSuspensionReview -> WorkOrderDetailRoute(destination.workOrderId)
            is PushDestination.TaskProducts -> TaskDetailRoute(destination.workOrderId, destination.taskId)
            is PushDestination.WorkOrderDetail -> WorkOrderDetailRoute(destination.workOrderId)
            is PushDestination.LowStockAlerts -> StockAlertsRoute
            is PushDestination.CollaboratorAttendance -> AdminAttendanceRoute
            is PushDestination.MyAttendance -> AttendanceRoute
            is PushDestination.ClockIn -> AttendanceRoute
        }
        navController.navigate(target) { launchSingleTop = true }
    }

    val context = LocalContext.current
    val signedInDeepLinkWarning = stringResource(R.string.auth_deeplink_signed_in_warning)
    val pendingAuthDeepLink by viewModel.pendingAuthDeepLink.collectAsStateWithLifecycle()
    LaunchedEffect(pendingAuthDeepLink, signedIn) {
        if (pendingAuthDeepLink == null) return@LaunchedEffect
        val link = viewModel.consumeAuthDeepLink() ?: return@LaunchedEffect
        if (signedIn) {
            Toast.makeText(context, signedInDeepLinkWarning, Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }
        val target: Any = when (link) {
            is AuthDeepLink.ResetPassword -> ResetPasswordRoute(token = link.token)
            is AuthDeepLink.AcceptInvitation -> RegisterRoute(token = link.token)
        }
        navController.navigate(target) { launchSingleTop = true }
    }

    // La barra flota sobre el contenido: no reserva alto. Publica el que tapa (LocalFloatingBarInset) y las raíces de
    // pestaña lo suman al final de su lista con Modifier.floatingBarPadding().
    var modulesOpen by rememberSaveable { mutableStateOf(false) }
    var barInset by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CompositionLocalProvider(LocalFloatingBarInset provides if (showBar) barInset else 0.dp) {
        NavHost(
            navController = navController,
            startDestination = if (signedIn) MainNavGraphRoute else AuthNavGraphRoute,
            // Con la barra a la vista, el inset inferior del sistema ya lo cuenta ella.
            modifier = Modifier
                .fillMaxSize()
                .then(if (showBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
            // Entre pestañas, un fundido corto (se ve decenas de veces al día). Hacia un detalle, eje compartido:
            // la pantalla entra desde el lado al que el gesto Atrás la devuelve.
            enterTransition = {
                when {
                    reducedMotion -> EnterTransition.None
                    isTabRoot(initialState.destination, tabs) && isTabRoot(targetState.destination, tabs) ->
                        fadeIn(tween(120))
                    else -> fadeIn(tween(200, easing = EaseOutQuint)) +
                        slideInHorizontally(tween(240, easing = EaseOutQuint)) { it / 12 }
                }
            },
            exitTransition = { if (reducedMotion) ExitTransition.None else fadeOut(tween(90)) },
            popEnterTransition = {
                if (reducedMotion) EnterTransition.None
                else fadeIn(tween(200, easing = EaseOutQuint)) +
                    slideInHorizontally(tween(240, easing = EaseOutQuint)) { -it / 12 }
            },
            popExitTransition = {
                if (reducedMotion) ExitTransition.None
                else fadeOut(tween(120)) + slideOutHorizontally(tween(200, easing = EaseOutQuint)) { it / 12 }
            }
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
                workOrderNavGraph(
                    navController = navController,
                    onOpenCheckout = { id -> navController.navigate(WorkOrderCheckoutRoute(id)) },
                    onOpenObd = { vehicleId, _ ->
                        navController.navigate(DiagnosticsHubRoute(initialVehicleId = vehicleId))
                    }
                )
                cashierNavGraph(
                    navController = navController,
                    showDailyCashBack = MainTab.Cash !in mainState.tabs
                )
                reportsNavGraph(navController)
                tasksNavGraph(
                    navController = navController,
                    onOpenNotifications = { navController.navigate(NotificationsRoute) },
                    onOpenSync = { navController.navigate(PendingOperationsRoute) },
                    onOpenProfile = { navController.navigate(ProfileRoute) },
                    onOpenObd = { vehicleId, _ ->
                        navController.navigate(DiagnosticsHubRoute(initialVehicleId = vehicleId))
                    }
                )
                inventoryNavGraph(navController)
                diagnosticsNavGraph(
                    navController = navController,
                    onReportFinding = { woId -> navController.navigate(ReportFindingRoute(woId)) },
                    onOpenNotifications = { navController.navigate(NotificationsRoute) },
                    onOpenSyncQueue = { navController.navigate(PendingOperationsRoute) },
                    onOpenProfile = { navController.navigate(ProfileRoute) }
                )
                mainTabsDestinations(navController)
            }
        }
        }
        if (showBar) {
            MainNavigationBar(
                navController = navController,
                modulesOpen = modulesOpen,
                onOpenModules = { modulesOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { barInset = with(density) { it.height.toDp() } + 8.dp }
            )
        }
    }
    if (modulesOpen) {
        ModulesSheet(
            sessionFlow = viewModel.sessionFlow,
            pendingSyncCountFlow = viewModel.pendingSyncCountFlow,
            onDismiss = { modulesOpen = false },
            onOpen = { destination ->
                modulesOpen = false
                navController.navigateToModule(destination)
            }
        )
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
