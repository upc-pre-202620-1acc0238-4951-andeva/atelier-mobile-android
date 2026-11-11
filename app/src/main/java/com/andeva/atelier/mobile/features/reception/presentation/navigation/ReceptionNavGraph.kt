package com.andeva.atelier.mobile.features.reception.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.andeva.atelier.mobile.features.main.presentation.navigation.MoreHubRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.NotificationsRoute
import com.andeva.atelier.mobile.features.main.presentation.navigation.PendingOperationsRoute
import com.andeva.atelier.mobile.features.reception.presentation.affiliation.AffiliationScreen
import com.andeva.atelier.mobile.features.reception.presentation.register.RegisterCustomerScreen
import com.andeva.atelier.mobile.features.reception.presentation.search.VehicleSearchScreen
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.OpenWorkOrderRoute
import kotlinx.serialization.Serializable

@Serializable
data object ReceptionNavGraphRoute

/** US11: buscar por placa o documento, ver las citas del día, cartera de clientes y vehículos registrados. */
@Serializable
data object VehicleSearchRoute

/** US44: buscar el correo del conductor y afiliarlo o registrarlo. */
@Serializable
data object AffiliationRoute

/**
 * US11 E2 y US44 E2: alta presencial de cliente y vehículo, o alta de un nuevo vehículo para un cliente existente
 * ([customerId] y [customerName]). [email] y [plate] llegan precargados desde búsqueda o afiliación.
 */
@Serializable
data class RegisterCustomerRoute(
    val email: String? = null,
    val plate: String? = null,
    val customerId: String? = null,
    val customerName: String? = null
)

/** Grafo de la feature de recepción; las pantallas obtienen sesión y permisos desde el núcleo. */
fun NavGraphBuilder.receptionNavGraph(navController: NavController) {
    navigation<ReceptionNavGraphRoute>(startDestination = VehicleSearchRoute) {
        composable<VehicleSearchRoute> {
            val canPop = navController.previousBackStackEntry != null
            VehicleSearchScreen(
                onBack = if (canPop) { { navController.popBackStack() } } else null,
                onOpenMenu = { navController.navigate(MoreHubRoute) },
                onOpenNotifications = { navController.navigate(NotificationsRoute) },
                onOpenSync = { navController.navigate(PendingOperationsRoute) },
                onRegisterNew = { initialPlate ->
                    navController.navigate(RegisterCustomerRoute(plate = initialPlate?.takeIf { it.isNotBlank() }))
                },
                onAddVehicleForCustomer = { customer ->
                    navController.navigate(
                        RegisterCustomerRoute(
                            customerId = customer.id,
                            customerName = customer.displayName
                        )
                    )
                },
                onAffiliate = { navController.navigate(AffiliationRoute) },
                onOpenWorkOrder = { vehicleId, customerId, workOrderId ->
                    navController.navigate(OpenWorkOrderRoute(vehicleId, customerId, workOrderId))
                }
            )
        }
        composable<AffiliationRoute> {
            AffiliationScreen(
                onBack = { navController.popBackStack() },
                onRegisterWalkIn = { email -> navController.navigate(RegisterCustomerRoute(email = email)) }
            )
        }
        composable<RegisterCustomerRoute> {
            RegisterCustomerScreen(
                onBack = { navController.popBackStack() },
                onRegistered = { navController.popBackStack() }
            )
        }
    }
}
