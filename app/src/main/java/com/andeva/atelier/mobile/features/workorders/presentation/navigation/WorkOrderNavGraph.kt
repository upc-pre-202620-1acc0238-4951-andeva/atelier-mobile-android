package com.andeva.atelier.mobile.features.workorders.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.TaskDetailRoute
import com.andeva.atelier.mobile.features.workorders.presentation.detail.WorkOrderDetailScreen
import com.andeva.atelier.mobile.features.workorders.presentation.finding.ReportFindingScreen
import com.andeva.atelier.mobile.features.workorders.presentation.intake.IntakeGalleryScreen
import com.andeva.atelier.mobile.features.workorders.presentation.list.WorkOrdersScreen
import com.andeva.atelier.mobile.features.workorders.presentation.open.OpenWorkOrderScreen
import com.andeva.atelier.mobile.features.workorders.presentation.proposals.ProposalsPanelScreen
import kotlinx.serialization.Serializable

/**
 * US12: abrir la orden del vehículo. [workOrderId] llega cuando la orden ya existe en borrador (llegó una cita) y
 * solo se completa; sin él, la pantalla busca un borrador del vehículo y, si no hay, crea una orden nueva.
 */
@Serializable
data class OpenWorkOrderRoute(
    val vehicleId: String,
    val customerId: String,
    val workOrderId: String? = null
)

/** Órdenes no cerradas de la sede (`operations:work_orders:read`). */
@Serializable
data object WorkOrdersRoute

/** Detalle de una orden con sus tareas; desde aquí el supervisor abre una tarea (US33). */
@Serializable
data class WorkOrderDetailRoute(val workOrderId: String)

/** US13: galería pericial de la orden. */
@Serializable
data class IntakeGalleryRoute(val workOrderId: String)

/** US15: panel de propuestas de la orden. */
@Serializable
data class ProposalsPanelRoute(val workOrderId: String)

/** US14: reporte de un hallazgo en foso para la orden. */
@Serializable
data class ReportFindingRoute(val workOrderId: String)

/**
 * Destinos de órdenes de trabajo. Se llega a ellos desde otra pantalla con el id de la orden o del vehículo, así que
 * no hay un destino inicial propio: no se agrupan en un `navigation<>`.
 */
fun NavGraphBuilder.workOrderNavGraph(navController: NavController) {
    composable<OpenWorkOrderRoute> {
        OpenWorkOrderScreen(
            onBack = { navController.popBackStack() },
            // La orden ya existe: se sigue con las fotos y al volver se regresa a la búsqueda, no al formulario.
            onOpened = { workOrderId ->
                navController.navigate(IntakeGalleryRoute(workOrderId)) {
                    popUpTo<OpenWorkOrderRoute> { inclusive = true }
                }
            }
        )
    }
    composable<WorkOrdersRoute> {
        WorkOrdersScreen(
            onBack = { navController.popBackStack() },
            onOpenOrder = { workOrderId -> navController.navigate(WorkOrderDetailRoute(workOrderId)) }
        )
    }
    composable<WorkOrderDetailRoute> {
        WorkOrderDetailScreen(
            onBack = { navController.popBackStack() },
            onOpenTask = { workOrderId, taskId -> navController.navigate(TaskDetailRoute(workOrderId, taskId)) }
        )
    }
    composable<IntakeGalleryRoute> {
        IntakeGalleryScreen(
            onBack = { navController.popBackStack() },
            onDone = { navController.popBackStack() },
            onOpenProposals = { workOrderId -> navController.navigate(ProposalsPanelRoute(workOrderId)) }
        )
    }
    composable<ProposalsPanelRoute> { entry ->
        val workOrderId = entry.toRoute<ProposalsPanelRoute>().workOrderId
        ProposalsPanelScreen(
            onBack = { navController.popBackStack() },
            onReportFinding = { navController.navigate(ReportFindingRoute(workOrderId)) }
        )
    }
    composable<ReportFindingRoute> {
        ReportFindingScreen(onBack = { navController.popBackStack() })
    }
}
