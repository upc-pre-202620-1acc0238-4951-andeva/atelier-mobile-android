package com.andeva.atelier.mobile.features.tasks.presentation.navigation

import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.andeva.atelier.mobile.core.designsystem.component.LocalFloatingBarInset
import com.andeva.atelier.mobile.features.tasks.presentation.detail.TaskDetailScreen
import com.andeva.atelier.mobile.features.tasks.presentation.mytasks.MyTasksScreen
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.ReportFindingRoute
import kotlinx.serialization.Serializable

/** Grafo de tareas del mecánico (EP06). */
@Serializable
data object TasksNavGraphRoute

/** US26: bandeja de Mis tareas. */
@Serializable
data object MyTasksRoute

/** US27, US33: detalle de una tarea con cronómetro y repuestos imputados. */
@Serializable
data class TaskDetailRoute(val workOrderId: String, val taskId: String)

fun NavGraphBuilder.tasksNavGraph(
    navController: NavController,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenSync: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onOpenObd: (vehicleId: String, plate: String?) -> Unit = { _, _ -> }
) {
    navigation<TasksNavGraphRoute>(startDestination = MyTasksRoute) {
        composable<MyTasksRoute> {
            val isTabRoot = LocalFloatingBarInset.current > 0.dp
            val canPop = !isTabRoot && navController.previousBackStackEntry != null
            MyTasksScreen(
                onOpenTask = { workOrderId, taskId -> navController.navigate(TaskDetailRoute(workOrderId, taskId)) },
                onBack = if (canPop) ({ navController.popBackStack() }) else null,
                onOpenNotifications = onOpenNotifications,
                onOpenSync = onOpenSync,
                onOpenProfile = onOpenProfile
            )
        }
        composable<TaskDetailRoute> {
            TaskDetailScreen(
                onBack = { navController.popBackStack() },
                onReportFinding = { workOrderId -> navController.navigate(ReportFindingRoute(workOrderId)) },
                onOpenObd = onOpenObd
            )
        }
    }
}
