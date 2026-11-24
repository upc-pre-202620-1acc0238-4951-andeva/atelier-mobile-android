package com.andeva.atelier.mobile.features.attendance.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.andeva.atelier.mobile.features.attendance.presentation.AttendanceScreen
import com.andeva.atelier.mobile.features.attendance.presentation.dashboard.FloorHubScreen
import com.andeva.atelier.mobile.features.cashier.presentation.navigation.DailyCashRoute
import com.andeva.atelier.mobile.features.cashier.presentation.navigation.WorkOrderCheckoutRoute
import com.andeva.atelier.mobile.features.diagnostics.presentation.navigation.DiagnosticsNavGraphRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.InventoryNavGraphRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.PartDetailRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.PurchaseOrderDetailRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.PurchaseOrdersRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.StockAlertsRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.BaysRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.NotificationsRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.PendingOperationsRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.ProfileRoute
import com.andeva.atelier.mobile.features.reception.presentation.navigation.ReceptionNavGraphRoute
import com.andeva.atelier.mobile.features.reception.presentation.navigation.RegisterCustomerRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.MyTasksRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.TaskDetailRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.ReportFindingRoute
import kotlinx.serialization.Serializable

@Serializable
data object AttendanceRoute

/** Grafo de la feature de asistencia y hub modular de piso/mostrador (Segmento 2). */
fun NavGraphBuilder.attendanceNavGraph(navController: NavController) {
    navigation<AttendanceRoute>(startDestination = AttendanceHomeRoute) {
        composable<AttendanceHomeRoute> {
            FloorHubScreen(
                onOpenProfile = { navController.navigate(ProfileRoute) },
                onOpenNotifications = { navController.navigate(NotificationsRoute) },
                onOpenSyncQueue = { navController.navigate(PendingOperationsRoute) },
                onOpenAttendanceDetail = { navController.navigate(AttendanceDetailRoute) },
                onOpenTasks = { navController.navigate(MyTasksRoute) },
                onOpenTaskDetail = { workOrderId, taskId ->
                    navController.navigate(TaskDetailRoute(workOrderId, taskId))
                },
                onOpenObd = { navController.navigate(DiagnosticsNavGraphRoute) },
                onOpenReportFinding = { workOrderId -> navController.navigate(ReportFindingRoute(workOrderId)) },
                onOpenBays = { navController.navigate(BaysRoute) },
                onOpenReception = { navController.navigate(ReceptionNavGraphRoute) },
                onRegisterWalkIn = { navController.navigate(RegisterCustomerRoute()) },
                onOpenInventory = { navController.navigate(InventoryNavGraphRoute) },
                onOpenPartDetail = { partId -> navController.navigate(PartDetailRoute(partId)) },
                onOpenStockAlerts = { navController.navigate(StockAlertsRoute) },
                onOpenPurchaseOrders = { navController.navigate(PurchaseOrdersRoute) },
                onOpenPurchaseOrderDetail = { poId -> navController.navigate(PurchaseOrderDetailRoute(poId)) },
                onOpenWorkOrderCheckout = { woId -> navController.navigate(WorkOrderCheckoutRoute(woId)) },
                onOpenDailyCash = { navController.navigate(DailyCashRoute) }
            )
        }
        composable<AttendanceDetailRoute> {
            AttendanceScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Serializable
data object AttendanceHomeRoute

@Serializable
data object AttendanceDetailRoute
