package com.andeva.atelier.mobile.features.tasks.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.andeva.atelier.mobile.features.tasks.presentation.mytasks.MyTasksScreen
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

fun NavGraphBuilder.tasksNavGraph(navController: NavController) {
    navigation<TasksNavGraphRoute>(startDestination = MyTasksRoute) {
        composable<MyTasksRoute> {
            MyTasksScreen(
                onBack = { navController.popBackStack() },
                onOpenTask = { workOrderId, taskId -> navController.navigate(TaskDetailRoute(workOrderId, taskId)) }
            )
        }
    }
}
