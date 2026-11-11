package com.andeva.atelier.mobile.features.attendance.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.andeva.atelier.mobile.features.attendance.presentation.AttendanceScreen
import com.andeva.atelier.mobile.features.attendance.presentation.dashboard.EmployeeDashboardScreen
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.InventoryNavGraphRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.MoreHubRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.NotificationsRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.ObdRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.PendingOperationsRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.MyTasksRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.ReportFindingRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.WorkOrdersRoute
import kotlinx.serialization.Serializable

@Serializable
data object AttendanceRoute

/** Grafo de la feature de asistencia y panel principal de colaborador operativo. */
fun NavGraphBuilder.attendanceNavGraph(navController: NavController) {
    navigation<AttendanceRoute>(startDestination = AttendanceHomeRoute) {
        composable<AttendanceHomeRoute> {
            EmployeeDashboardScreen(
                onOpenMenu = { navController.navigate(MoreHubRoute) },
                onOpenNotifications = { navController.navigate(NotificationsRoute) },
                onOpenSyncQueue = { navController.navigate(PendingOperationsRoute) },
                onOpenTasks = { navController.navigate(MyTasksRoute) },
                onOpenTaskDetail = { _ -> navController.navigate(MyTasksRoute) },
                onOpenObd = { navController.navigate(ObdRoute) },
                onOpenReportFinding = { workOrderId -> navController.navigate(ReportFindingRoute(workOrderId)) },
                onOpenInventory = { navController.navigate(InventoryNavGraphRoute) },
                onOpenAttendanceDetails = { navController.navigate(AttendanceDetailRoute) }
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
