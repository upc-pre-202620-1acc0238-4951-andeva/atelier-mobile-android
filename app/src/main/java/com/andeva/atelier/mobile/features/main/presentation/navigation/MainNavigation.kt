package com.andeva.atelier.mobile.features.main.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
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
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.PartsCatalogRoute
import com.andeva.atelier.mobile.features.main.presentation.attendance.AdminAttendanceScreen
import com.andeva.atelier.mobile.features.main.presentation.dashboard.AdminDashboardScreen
import com.andeva.atelier.mobile.features.main.presentation.more.MoreHubScreen
import com.andeva.atelier.mobile.features.main.presentation.notifications.NotificationsScreen
import com.andeva.atelier.mobile.features.main.presentation.obd.ObdScreen
import com.andeva.atelier.mobile.features.main.presentation.profile.ProfileScreen
import com.andeva.atelier.mobile.features.main.presentation.sync.PendingOperationsScreen
import com.andeva.atelier.mobile.features.reception.presentation.navigation.ReceptionNavGraphRoute
import com.andeva.atelier.mobile.features.reception.presentation.navigation.RegisterCustomerRoute
import com.andeva.atelier.mobile.features.reception.presentation.navigation.VehicleSearchRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.MyTasksRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.TasksNavGraphRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.WorkOrderDetailRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.WorkOrdersRoute
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/** B1: Dashboard operativo de sede para dueño, administrador, jefe de taller y asesor de recepción. */
@Serializable
data object AdminDashboardRoute

/** Hub "Más" con todos los módulos administrativos y operativos del taller. */
@Serializable
data object MoreHubRoute

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
    data object DashboardHome : MainTab(AdminDashboardRoute, AdminDashboardRoute::class, R.string.main_tab_home, home)
    data object Tasks : MainTab(TasksNavGraphRoute, MyTasksRoute::class, R.string.main_tab_tasks, assignment)
    data object Orders : MainTab(WorkOrdersRoute, WorkOrdersRoute::class, R.string.main_tab_orders, assignment)
    data object Reception : MainTab(
        ReceptionNavGraphRoute, VehicleSearchRoute::class, R.string.main_tab_reception, Icons.Outlined.AddCircleOutline
    )
    data object Obd : MainTab(ObdRoute, ObdRoute::class, R.string.main_tab_obd, bluetooth_searching)
    data object Stock : MainTab(
        InventoryNavGraphRoute, PartsCatalogRoute::class, R.string.main_tab_stock, Icons.Outlined.Inventory2
    )
    data object Profile : MainTab(ProfileRoute, ProfileRoute::class, R.string.main_tab_profile, person)
    data object Settings : MainTab(MoreHubRoute, MoreHubRoute::class, R.string.main_tab_more, Icons.Outlined.Settings)

    companion object {
        /**
         * Pestañas según los permisos de la sesión, nunca según el rol (docs/02-roles-and-permissions.md).
         */
        fun tabsFor(session: Session): List<MainTab> {
            return if (session.prefersOperationalDashboard()) {
                buildList {
                    add(DashboardHome)
                    if (session.has(Permission.WORK_ORDERS_READ)) add(Orders)
                    if (session.has(Permission.VEHICLES_CREATE) || session.has(Permission.CUSTOMERS_READ)) add(Reception)
                    if (session.has(Permission.PARTS_READ)) add(Stock)
                    add(Profile)
                }
            } else if (session.has(Permission.TASKS_TRACK_TIME)) {
                buildList {
                    add(Home)
                    if (session.has(Permission.TASKS_READ)) add(Tasks)
                    if (session.has(Permission.WORK_ORDERS_CREATE)) add(Orders)
                    if (session.canUseTelemetry()) add(Obd)
                    add(Profile)
                }
            } else {
                buildList {
                    add(DashboardHome)
                    if (session.has(Permission.WORK_ORDERS_READ)) add(Orders)
                    if (session.has(Permission.VEHICLES_CREATE)) add(Reception)
                    if (session.has(Permission.PARTS_READ)) add(Stock)
                    add(Profile)
                }
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

/** Destinos propios de `main`: Dashboard, Más, Bahías, Asistencia de Sede, Notificaciones, Cola Offline, OBD-II y Perfil. */
fun NavGraphBuilder.mainTabsDestinations(
    navController: NavController,
    sessionFlow: StateFlow<Session?>,
    pendingSyncCountFlow: StateFlow<Int>
) {
    composable<AdminDashboardRoute> {
        AdminDashboardScreen(
            onOpenMoreHub = { navController.navigate(MoreHubRoute) },
            onSelectBranch = { navController.navigate(BranchSelectionRoute) },
            onOpenNotifications = { navController.navigate(NotificationsRoute) },
            onOpenSyncQueue = { navController.navigate(PendingOperationsRoute) },
            onOpenReception = { navController.navigate(ReceptionNavGraphRoute) },
            onOpenWalkInRegister = { navController.navigate(RegisterCustomerRoute()) },
            onOpenOrdersList = { navController.navigate(WorkOrdersRoute) },
            onOpenOrderDetail = { id -> navController.navigate(WorkOrderDetailRoute(id)) },
            onOpenBays = { navController.navigate(BaysRoute) },
            onOpenStockAlerts = { navController.navigate(NotificationsRoute) },
            onOpenAttendanceHub = { navController.navigate(AdminAttendanceRoute) },
            onOpenObdInventory = { navController.navigate(ObdRoute) }
        )
    }
    composable<MoreHubRoute> {
        MoreHubScreen(
            sessionFlow = sessionFlow,
            pendingSyncCountFlow = pendingSyncCountFlow,
            onBack = { navController.popBackStack() },
            onSelectBranch = { navController.navigate(BranchSelectionRoute) },
            onOpenReception = { navController.navigate(ReceptionNavGraphRoute) },
            onOpenBays = { navController.navigate(BaysRoute) },
            onOpenObdInventory = { navController.navigate(ObdRoute) },
            onOpenAttendance = { navController.navigate(AdminAttendanceRoute) },
            onOpenStockCatalog = { navController.navigate(InventoryNavGraphRoute) },
            onOpenStockAlerts = { navController.navigate(NotificationsRoute) },
            onOpenPurchaseOrders = { navController.navigate(InventoryNavGraphRoute) },
            onOpenValuation = { navController.navigate(InventoryNavGraphRoute) },
            onOpenNotifications = { navController.navigate(NotificationsRoute) },
            onOpenSyncQueue = { navController.navigate(PendingOperationsRoute) },
            onOpenProfile = { navController.navigate(ProfileRoute) }
        )
    }
    composable<BaysRoute> {
        BaysScreen(
            onBack = { navController.popBackStack() },
            onOpenOrder = { workOrderId -> navController.navigate(WorkOrderDetailRoute(workOrderId)) }
        )
    }
    composable<AdminAttendanceRoute> {
        AdminAttendanceScreen(onBack = { navController.popBackStack() })
    }
    composable<NotificationsRoute> {
        NotificationsScreen(
            onBack = { navController.popBackStack() },
            onOpenOrder = { workOrderId -> navController.navigate(WorkOrderDetailRoute(workOrderId)) },
            onOpenStockAlerts = { navController.navigate(InventoryNavGraphRoute) },
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
            onOpenMenu = { navController.navigate(MoreHubRoute) },
            onSelectBranch = { navController.navigate(BranchSelectionRoute) },
            onOpenSyncQueue = { navController.navigate(PendingOperationsRoute) },
            onOpenNotifications = { navController.navigate(NotificationsRoute) }
        )
    }
}
