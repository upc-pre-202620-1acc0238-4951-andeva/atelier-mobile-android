package com.andeva.atelier.mobile.features.workorders.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.andeva.atelier.mobile.features.workorders.presentation.finding.ReportFindingScreen
import com.andeva.atelier.mobile.features.workorders.presentation.intake.IntakeGalleryScreen
import com.andeva.atelier.mobile.features.workorders.presentation.open.OpenWorkOrderScreen
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

/** US13: galería pericial de la orden. */
@Serializable
data class IntakeGalleryRoute(val workOrderId: String)

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
    composable<IntakeGalleryRoute> {
        IntakeGalleryScreen(
            onBack = { navController.popBackStack() },
            onDone = { navController.popBackStack() }
        )
    }
    composable<ReportFindingRoute> {
        ReportFindingScreen(onBack = { navController.popBackStack() })
    }
}
