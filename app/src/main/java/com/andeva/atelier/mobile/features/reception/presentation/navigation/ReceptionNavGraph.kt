package com.andeva.atelier.mobile.features.reception.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.andeva.atelier.mobile.features.reception.presentation.affiliation.AffiliationScreen
import com.andeva.atelier.mobile.features.reception.presentation.register.RegisterCustomerScreen
import kotlinx.serialization.Serializable

@Serializable
data object ReceptionNavGraphRoute

/** US44: buscar el correo del conductor y afiliarlo o registrarlo. */
@Serializable
data object AffiliationRoute

/** US11 E2 y US44 E2: alta presencial de cliente y vehículo. [email] llega precargado desde la afiliación. */
@Serializable
data class RegisterCustomerRoute(val email: String? = null)

/** Grafo de la feature de recepción; las pantallas obtienen sesión y permisos desde el núcleo. */
fun NavGraphBuilder.receptionNavGraph(navController: NavController) {
    navigation<ReceptionNavGraphRoute>(startDestination = AffiliationRoute) {
        composable<AffiliationRoute> {
            AffiliationScreen(
                onBack = { navController.popBackStack() },
                onRegisterWalkIn = { email -> navController.navigate(RegisterCustomerRoute(email)) }
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
