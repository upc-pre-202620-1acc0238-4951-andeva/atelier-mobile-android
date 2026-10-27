package com.andeva.atelier.mobile.features.workorders.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
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

/**
 * Destinos de órdenes de trabajo. Se llega a ellos desde otra pantalla con el id de la orden o del vehículo, así que
 * no hay un destino inicial propio: no se agrupan en un `navigation<>`.
 */
fun NavGraphBuilder.workOrderNavGraph(navController: NavController) {
    composable<OpenWorkOrderRoute> {
        OpenWorkOrderScreen(
            onBack = { navController.popBackStack() },
            onOpened = { navController.popBackStack() }
        )
    }
}
