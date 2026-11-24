package com.andeva.atelier.mobile.features.main.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.icon.assignment
import com.andeva.atelier.mobile.core.designsystem.icon.bluetooth_searching
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.AttendanceDetailRoute
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.AttendanceHomeRoute
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.AttendanceRoute
import com.andeva.atelier.mobile.features.auth.presentation.navigation.BranchSelectionRoute
import com.andeva.atelier.mobile.features.bays.presentation.BaysScreen
import com.andeva.atelier.mobile.features.cashier.presentation.navigation.DailyCashRoute
import com.andeva.atelier.mobile.features.cashier.presentation.navigation.FiscalSeriesRoute
import com.andeva.atelier.mobile.features.cashier.presentation.navigation.VouchersRoute
import com.andeva.atelier.mobile.features.diagnostics.presentation.navigation.DiagnosticsHubRoute
import com.andeva.atelier.mobile.features.diagnostics.presentation.navigation.DiagnosticsNavGraphRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.InventoryNavGraphRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.InventoryValuationRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.PartsCatalogRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.PurchaseOrdersRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.StockAlertsRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.SuppliersRoute
import com.andeva.atelier.mobile.features.main.presentation.attendance.AdminAttendanceScreen
import com.andeva.atelier.mobile.features.main.presentation.dashboard.AdminDashboardScreen
import com.andeva.atelier.mobile.features.main.presentation.more.MoreDestination
import com.andeva.atelier.mobile.features.main.presentation.notifications.NotificationsScreen
import com.andeva.atelier.mobile.features.main.presentation.profile.ProfileScreen
import com.andeva.atelier.mobile.features.main.presentation.sync.PendingOperationsScreen
import com.andeva.atelier.mobile.features.reception.presentation.navigation.ReceptionNavGraphRoute
import com.andeva.atelier.mobile.features.reception.presentation.navigation.RegisterCustomerRoute
import com.andeva.atelier.mobile.features.reception.presentation.navigation.VehicleSearchRoute
import com.andeva.atelier.mobile.features.reports.presentation.navigation.ReportsRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.MyTasksRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.TasksNavGraphRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.ServicesCatalogRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.WorkOrderDetailRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.WorkOrdersRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/** B1: Dashboard operativo de sede para quienes supervisan la operación o abren órdenes de trabajo. */
@Serializable
data object AdminDashboardRoute

/** Tablero de bahías de trabajo de la sede (US40). */
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

/** K1: perfil y cierre de sesión. */
@Serializable
data object ProfileRoute

/**
 * `true` cuando la sesión tiene permisos de supervisión de sede o apertura/resolución de órdenes de trabajo.
 * Quien no los tiene aterriza en `FloorHub` (`AttendanceRoute`), adaptado a sus permisos de piso o mostrador.
 */
fun Session.prefersOperationalDashboard(): Boolean =
    has(Permission.WORK_ORDERS_CREATE) ||
        has(Permission.QUOTATIONS_APPROVE) ||
        has(Permission.ATTENDANCE_AUDIT_ALL) ||
        has(Permission.BAYS_ASSIGN) ||
        has(Permission.OBD_DEVICES_REGISTER) ||
        has(Permission.CASHFLOW_EXPORT_PDF) ||
        has(Permission.FISCAL_CONFIG_MANAGE)

/** Acción principal del botón circular (64 dp) a la derecha de la barra flotante, decidida por permiso. */
enum class BarPrimaryAction {
    RECEIVE_VEHICLE,
    NEW_APPOINTMENT,
    COLLECT_PAYMENT,
    SCAN_SKU,
    MY_TASKS,
    NONE;

    companion object {
        fun forSession(session: Session): BarPrimaryAction = when {
            session.has(Permission.WORK_ORDERS_CREATE) -> RECEIVE_VEHICLE
            session.has(Permission.APPOINTMENTS_CREATE) -> NEW_APPOINTMENT
            session.has(Permission.INVOICES_ISSUE_SUNAT) || session.has(Permission.PAYMENTS_CREATE) -> COLLECT_PAYMENT
            session.has(Permission.PARTS_MANAGE) || session.has(Permission.PURCHASE_ORDERS_CREATE) -> SCAN_SKU
            session.has(Permission.TASKS_TRACK_TIME) &&
                session.has(Permission.TASKS_READ) &&
                MainTab.Tasks !in MainTab.tabsFor(session) -> MY_TASKS
            else -> NONE
        }
    }
}

/**
 * Pestaña raíz de la barra inferior flotante. [graph] es el destino de navegación; [root] es la pantalla
 * inicial de ese destino (la barra se dibuja solo sobre estas raíces y se oculta en pantallas de detalle).
 */
sealed class MainTab(
    val graph: Any,
    val root: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector
) {
    data object Shift : MainTab(AttendanceRoute, AttendanceHomeRoute::class, R.string.main_tab_shift, Icons.Outlined.Schedule)
    data object Today : MainTab(AdminDashboardRoute, AdminDashboardRoute::class, R.string.main_tab_today, Icons.Outlined.Today)
    data object Tasks : MainTab(TasksNavGraphRoute, MyTasksRoute::class, R.string.main_tab_tasks, assignment)
    data object Reception : MainTab(ReceptionNavGraphRoute, VehicleSearchRoute::class, R.string.main_tab_reception, Icons.Outlined.DirectionsCar)
    data object Orders : MainTab(WorkOrdersRoute, WorkOrdersRoute::class, R.string.main_tab_orders, assignment)
    data object Team : MainTab(AdminAttendanceRoute, AdminAttendanceRoute::class, R.string.main_tab_team, Icons.Outlined.Groups)
    data object Inventory : MainTab(InventoryNavGraphRoute, PartsCatalogRoute::class, R.string.main_tab_stock, Icons.Outlined.Inventory2)
    data object Purchases : MainTab(PurchaseOrdersRoute, PurchaseOrdersRoute::class, R.string.main_tab_purchases, Icons.Outlined.ShoppingCart)
    data object Cash : MainTab(DailyCashRoute, DailyCashRoute::class, R.string.main_tab_cash, Icons.Outlined.Payments)
    data object Bays : MainTab(BaysRoute, BaysRoute::class, R.string.main_tab_bays, Icons.Outlined.Garage)
    data object Obd : MainTab(DiagnosticsNavGraphRoute, DiagnosticsHubRoute::class, R.string.main_tab_obd, bluetooth_searching)

    companion object {
        private const val MAX_PRIMARY_TABS = 3

        /**
         * Todas las sesiones autenticadas cuentan con el slot "Módulos" al final de la cápsula (máx. 4 slots),
         * garantizando acceso a cualquier pantalla permitida y a Cuenta/Notificaciones/Sync sin bloqueos.
         */
        fun hasModules(@Suppress("UNUSED_PARAMETER") session: Session): Boolean = true

        /**
         * Destinos de la barra estrictamente por permiso atómico (nunca por rol).
         * Siempre incluye el destino de inicio en el índice 0 para garantizar que la barra sea visible al arrancar.
         */
        fun tabsFor(session: Session): List<MainTab> {
            val result = mutableListOf<MainTab>()
            fun addUnique(tab: MainTab) {
                if (result.size < MAX_PRIMARY_TABS && tab !in result) result.add(tab)
            }

            if (session.prefersOperationalDashboard()) {
                addUnique(Today)
                if (session.has(Permission.ATTENDANCE_AUDIT_ALL)) {
                    // Dueño, Administrador, Mecánico Jefe: Hoy · Órdenes · Equipo (+ Módulos)
                    if (session.has(Permission.WORK_ORDERS_READ)) addUnique(Orders)
                    addUnique(Team)
                } else {
                    // Asesor de Servicio u operación sin auditoría de equipo: Hoy · Recepción · Órdenes (+ Módulos)
                    if (session.has(Permission.APPOINTMENTS_READ) || session.has(Permission.CUSTOMERS_READ)) {
                        addUnique(Reception)
                    }
                    if (session.has(Permission.WORK_ORDERS_READ)) addUnique(Orders)
                }
                // Relleno por si un rol personalizado tiene supervisión parcial
                if (session.has(Permission.BAYS_READ)) addUnique(Bays)
                return result
            }

            // Personal de piso y mostrador (Segmento 2): siempre arranca en "Turno" (FloorHub modular)
            addUnique(Shift)

            // Prioridad 1: ejecución en fosa (Técnico Mecánico)
            if (session.has(Permission.TASKS_TRACK_TIME) && session.has(Permission.TASKS_READ)) {
                addUnique(Tasks)
                if (session.canUseTelemetry()) addUnique(Obd)
                else if (session.has(Permission.BAYS_READ)) addUnique(Bays)
                else if (session.has(Permission.WORK_ORDERS_READ)) addUnique(Orders)
            }

            // Prioridad 2: mostrador / agenda (Recepcionista)
            if (session.has(Permission.APPOINTMENTS_READ) || session.has(Permission.CUSTOMERS_CREATE)) {
                addUnique(Reception)
                if (session.has(Permission.WORK_ORDERS_READ)) addUnique(Orders)
            }

            // Prioridad 3: almacén / suministros (Encargado de Inventario)
            if (session.has(Permission.PARTS_MANAGE) ||
                session.has(Permission.PURCHASE_ORDERS_CREATE) ||
                session.has(Permission.BATCHES_RECEIVE)
            ) {
                if (session.has(Permission.PARTS_READ)) addUnique(Inventory)
                if (session.has(Permission.PURCHASE_ORDERS_READ)) addUnique(Purchases)
            }

            // Prioridad 4: caja y facturación (Cajero)
            if (session.has(Permission.INVOICES_ISSUE_SUNAT) || session.has(Permission.PAYMENTS_CREATE)) {
                if (session.has(Permission.WORK_ORDERS_READ)) addUnique(Orders)
                if (session.has(Permission.INVOICES_READ)) addUnique(Cash)
            }

            // Relleno por permiso para roles híbridos o personalizados
            if (session.has(Permission.TASKS_READ)) addUnique(Tasks)
            if (session.has(Permission.WORK_ORDERS_READ)) addUnique(Orders)
            if (session.has(Permission.PARTS_READ)) addUnique(Inventory)
            if (session.has(Permission.BAYS_READ)) addUnique(Bays)
            if (session.canUseTelemetry() || session.canReadTelemetry()) addUnique(Obd)

            return result
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
        MoreDestination.MY_ATTENDANCE -> AttendanceDetailRoute
        MoreDestination.MY_TASKS -> TasksNavGraphRoute
        MoreDestination.WORK_ORDERS -> WorkOrdersRoute
        MoreDestination.RECEPTION -> ReceptionNavGraphRoute
        MoreDestination.BAYS -> BaysRoute
        MoreDestination.SERVICES -> ServicesCatalogRoute
        MoreDestination.DAILY_CASH -> DailyCashRoute
        MoreDestination.VOUCHERS -> VouchersRoute
        MoreDestination.FISCAL_SERIES -> FiscalSeriesRoute
        MoreDestination.REPORTS -> ReportsRoute
        MoreDestination.PARTS -> InventoryNavGraphRoute
        MoreDestination.STOCK_ALERTS -> StockAlertsRoute
        MoreDestination.PURCHASE_ORDERS -> PurchaseOrdersRoute
        MoreDestination.SUPPLIERS -> SuppliersRoute
        MoreDestination.VALUATION -> InventoryValuationRoute
        MoreDestination.OBD -> DiagnosticsNavGraphRoute
        MoreDestination.NOTIFICATIONS -> NotificationsRoute
        MoreDestination.SYNC_QUEUE -> PendingOperationsRoute
        MoreDestination.PROFILE -> ProfileRoute
    }
    navigate(route) { launchSingleTop = true }
}

/** Destinos propios de `main`: Dashboard, Bahías, Asistencia de Sede, Notificaciones, Cola Offline, OBD-II y Perfil. */
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
            onOpenObd = { navController.navigate(DiagnosticsNavGraphRoute) },
            onOpenMyAttendance = { navController.navigate(AttendanceDetailRoute) },
            onOpenMyTasks = { navController.navigate(TasksNavGraphRoute) }
        )
    }
    composable<BaysRoute> {
        BaysScreen(
            onBack = { navController.popBackStack() },
            onOpenOrder = { workOrderId -> navController.navigate(WorkOrderDetailRoute(workOrderId)) }
        )
    }
    composable<AdminAttendanceRoute> {
        val canPop = navController.previousBackStackEntry != null &&
            navController.previousBackStackEntry?.destination?.route?.contains("AdminDashboardRoute") != true
        AdminAttendanceScreen(
            onBack = if (canPop) { { navController.popBackStack() } } else null,
            onOpenNotifications = { navController.navigate(NotificationsRoute) },
            onOpenSync = { navController.navigate(PendingOperationsRoute) },
            onOpenProfile = { navController.navigate(ProfileRoute) }
        )
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
