package com.andeva.atelier.mobile.features.main.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.icon.assignment
import com.andeva.atelier.mobile.core.designsystem.icon.bluetooth_searching
import com.andeva.atelier.mobile.core.designsystem.icon.home
import com.andeva.atelier.mobile.core.designsystem.icon.person
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.AttendanceHomeRoute
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.AttendanceRoute
import com.andeva.atelier.mobile.features.auth.presentation.navigation.BranchSelectionRoute
import com.andeva.atelier.mobile.features.bays.presentation.BaysScreen
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.InventoryNavGraphRoute
import com.andeva.atelier.mobile.features.main.presentation.attendance.AdminAttendanceScreen
import com.andeva.atelier.mobile.features.main.presentation.dashboard.AdminDashboardScreen
import com.andeva.atelier.mobile.features.cashier.presentation.navigation.DailyCashRoute
import com.andeva.atelier.mobile.features.cashier.presentation.navigation.FiscalSeriesRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.InventoryValuationRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.PurchaseOrdersRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.StockAlertsRoute
import com.andeva.atelier.mobile.features.main.presentation.more.MoreDestination
import com.andeva.atelier.mobile.features.reports.presentation.navigation.ReportsRoute
import com.andeva.atelier.mobile.features.main.presentation.notifications.NotificationsScreen
import com.andeva.atelier.mobile.features.main.presentation.obd.ObdScreen
import com.andeva.atelier.mobile.features.main.presentation.profile.ProfileScreen
import com.andeva.atelier.mobile.features.main.presentation.sync.PendingOperationsScreen
import com.andeva.atelier.mobile.features.reception.presentation.navigation.ReceptionNavGraphRoute
import com.andeva.atelier.mobile.features.reception.presentation.navigation.RegisterCustomerRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.MyTasksRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.TasksNavGraphRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.WorkOrderDetailRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.WorkOrdersRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/** B1: Dashboard operativo de sede para dueño, administrador, jefe de taller y asesor de recepción. */
@Serializable
data object AdminDashboardRoute

/** Tablero de bahías de trabajo de la sede (US15 / US16 / Tarea 5). */
@Serializable
data object BaysRoute

/** Hub de asistencia de sede y revisión de justificaciones (US10) + asistencia propia. */
@Serializable
data object AdminAttendanceRoute

/** Centro de notificaciones y alertas operativas. */
@Serializable
data object NotificationsRoute

/** Cola de operaciones pendientes de sincronizar (`pending_operations`). */
@Serializable
data object PendingOperationsRoute

/** Escáneres OBD-II registrados en el taller (EP03). */
@Serializable
data object ObdRoute

/** K1: perfil y cierre de sesión. */
@Serializable
data object ProfileRoute

/**
 * `true` cuando la sesión tiene permisos de gestión, recepción u operaciones de sede que requieren el Dashboard
 * operativo como pantalla inicial en lugar de la marcación GPS personal.
 */
fun Session.prefersOperationalDashboard(): Boolean =
    has(Permission.WORK_ORDERS_CREATE) ||
        has(Permission.QUOTATIONS_APPROVE) ||
        has(Permission.ATTENDANCE_AUDIT_ALL) ||
        has(Permission.BAYS_ASSIGN) ||
        has(Permission.OBD_DEVICES_REGISTER)

/**
 * Pestaña de la barra inferior (galería B1/B2). [graph] es el destino al que se navega; [root] es la pantalla
 * inicial de ese destino: la barra solo se muestra en ellas y se oculta en los detalles.
 */
sealed class MainTab(
    val graph: Any,
    val root: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector
) {
    data object Home : MainTab(AttendanceRoute, AttendanceHomeRoute::class, R.string.main_tab_home, home)
    data object Today : MainTab(AdminDashboardRoute, AdminDashboardRoute::class, R.string.main_tab_today, Icons.Outlined.Today)
    data object Tasks : MainTab(TasksNavGraphRoute, MyTasksRoute::class, R.string.main_tab_tasks, assignment)
    data object Orders : MainTab(WorkOrdersRoute, WorkOrdersRoute::class, R.string.main_tab_orders, assignment)
    data object Team : MainTab(AdminAttendanceRoute, AdminAttendanceRoute::class, R.string.main_tab_team, Icons.Outlined.Groups)
    data object Obd : MainTab(ObdRoute, ObdRoute::class, R.string.main_tab_obd, bluetooth_searching)
    data object Profile : MainTab(ProfileRoute, ProfileRoute::class, R.string.main_tab_profile, person)

    companion object {
        /** Quien registra tiempo de tareas y no supervisa la sede usa la barra de campo (sin Módulos). */
        private fun isFieldStaff(session: Session) =
            !session.prefersOperationalDashboard() && session.has(Permission.TASKS_TRACK_TIME)

        /** La barra de supervisión añade al final el slot "Módulos", que abre la hoja y no es un destino. */
        fun hasModules(session: Session) = !isFieldStaff(session)

        /**
         * Destinos de la barra según los permisos de la sesión, nunca según el rol (docs/02-roles-and-permissions.md).
         * Quien supervisa la sede ve Hoy · Órdenes · Equipo (+ Módulos); recepción, repuestos, caja y reportes cuelgan
         * de la hoja de módulos y del botón de acción "Recibir vehículo".
         */
        fun tabsFor(session: Session): List<MainTab> =
            if (isFieldStaff(session)) {
                buildList {
                    add(Home)
                    if (session.has(Permission.TASKS_READ)) add(Tasks)
                    if (session.has(Permission.WORK_ORDERS_CREATE)) add(Orders)
                    if (session.canUseTelemetry()) add(Obd)
                    add(Profile)
                }
            } else {
                buildList {
                    add(Today)
                    if (session.has(Permission.WORK_ORDERS_READ)) add(Orders)
                    if (session.has(Permission.ATTENDANCE_AUDIT_ALL)) add(Team)
                }
            }
    }
}

/** Cambia de pestaña conservando el estado de cada una (scroll, búsqueda) y sin apilar copias. */
fun NavController.navigateToTab(tab: MainTab) {
    val startDestId = graph.findStartDestination().id
    val isStartInBackStack = currentBackStack.value.any { it.destination.id == startDestId }
    navigate(tab.graph) {
        if (isStartInBackStack) {
            popUpTo(startDestId) {
                saveState = true
            }
        }
        launchSingleTop = true
        restoreState = true
    }
}

/** Abre un destino de la hoja de módulos (los mismos que las baldosas de `ModulesSheet`). */
fun NavController.navigateToModule(destination: MoreDestination) {
    val route: Any = when (destination) {
        MoreDestination.BRANCH -> BranchSelectionRoute
        MoreDestination.RECEPTION -> ReceptionNavGraphRoute
        MoreDestination.BAYS -> BaysRoute
        MoreDestination.DAILY_CASH -> DailyCashRoute
        MoreDestination.FISCAL_SERIES -> FiscalSeriesRoute
        MoreDestination.REPORTS -> ReportsRoute
        MoreDestination.PARTS -> InventoryNavGraphRoute
        MoreDestination.STOCK_ALERTS -> StockAlertsRoute
        MoreDestination.PURCHASE_ORDERS -> PurchaseOrdersRoute
        MoreDestination.VALUATION -> InventoryValuationRoute
        MoreDestination.OBD -> ObdRoute
        MoreDestination.NOTIFICATIONS -> NotificationsRoute
        MoreDestination.SYNC_QUEUE -> PendingOperationsRoute
        MoreDestination.PROFILE -> ProfileRoute
    }
    navigate(route)
}

/** Destinos propios de `main`: Dashboard, Más, Bahías, Asistencia de Sede, Notificaciones, Cola Offline, OBD-II y Perfil. */
fun NavGraphBuilder.mainTabsDestinations(navController: NavController) {
    composable<AdminDashboardRoute> {
        AdminDashboardScreen(
            onSelectBranch = { navController.navigate(BranchSelectionRoute) },
            onOpenNotifications = { navController.navigate(NotificationsRoute) },
            onOpenSyncQueue = { navController.navigate(PendingOperationsRoute) },
            onOpenProfile = { navController.navigate(ProfileRoute) },
            onReceiveVehicle = { navController.navigate(ReceptionNavGraphRoute) },
            onRegisterWalkIn = { navController.navigate(RegisterCustomerRoute()) },
            onOpenOrder = { id -> navController.navigate(WorkOrderDetailRoute(id)) },
            onOpenOrders = { navController.navigateToTab(MainTab.Orders) },
            onOpenBays = { navController.navigate(BaysRoute) },
            onOpenStockAlerts = { navController.navigate(StockAlertsRoute) },
            onOpenTeam = { navController.navigateToTab(MainTab.Team) },
            onOpenObd = { navController.navigate(ObdRoute) }
        )
    }
    composable<BaysRoute> {
        BaysScreen(
            onBack = { navController.popBackStack() },
            onOpenOrder = { workOrderId -> navController.navigate(WorkOrderDetailRoute(workOrderId)) }
        )
    }
    composable<AdminAttendanceRoute> {
        AdminAttendanceScreen()
    }
    composable<NotificationsRoute> {
        NotificationsScreen(
            onBack = { navController.popBackStack() },
            onOpenOrder = { workOrderId -> navController.navigate(WorkOrderDetailRoute(workOrderId)) },
            onOpenStockAlerts = { navController.navigate(StockAlertsRoute) },
            onOpenAttendanceReview = { navController.navigate(AdminAttendanceRoute) },
            onOpenSyncQueue = { navController.navigate(PendingOperationsRoute) }
        )
    }
    composable<PendingOperationsRoute> {
        PendingOperationsScreen(onBack = { navController.popBackStack() })
    }
    composable<ObdRoute> {
        ObdScreen(onBack = { navController.popBackStack() })
    }
    composable<ProfileRoute> {
        val canPop = navController.previousBackStackEntry != null
        ProfileScreen(
            onBack = if (canPop) { { navController.popBackStack() } } else null,
            onSelectBranch = { navController.navigate(BranchSelectionRoute) },
            onOpenSyncQueue = { navController.navigate(PendingOperationsRoute) },
            onOpenNotifications = { navController.navigate(NotificationsRoute) }
        )
    }
}
