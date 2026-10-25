package com.andeva.atelier.mobile.features.auth.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.andeva.atelier.mobile.features.auth.presentation.login.LoginScreen
import com.andeva.atelier.mobile.features.auth.presentation.register.RegisterScreen
import com.andeva.atelier.mobile.features.auth.presentation.verifyemail.VerifyEmailScreen
import com.andeva.atelier.mobile.navigation.AuthNavGraphRoute
import kotlinx.serialization.Serializable

/** A5. [verifiedEmail] llega desde A4 al verificar el correo: precarga el campo y muestra el aviso. */
@Serializable
data class LoginRoute(val verifiedEmail: String? = null)

/** A3. [token] queda para el deep link del correo (H1 de docs/03-traceability.md); hoy se pega el enlace. */
@Serializable
data class RegisterRoute(val token: String? = null)

/** A4. */
@Serializable
data class VerifyEmailRoute(val email: String)

/**
 * Grafo sin sesión. Al iniciar sesión o aceptar la invitación no se navega desde aquí:
 * `AppNavHost` cambia al grafo con sesión cuando `SessionManager` guarda la sesión.
 */
fun NavGraphBuilder.authNavGraph(navController: NavController) {
    navigation<AuthNavGraphRoute>(startDestination = LoginRoute()) {
        composable<LoginRoute> {
            LoginScreen(
                onHaveInvitation = { navController.navigate(RegisterRoute()) },
                onVerifyEmail = { email -> navController.navigate(VerifyEmailRoute(email)) }
                // onForgotPassword: se conecta con A6 (recuperar contraseña)
            )
        }
        composable<RegisterRoute> {
            RegisterScreen(onBack = { navController.popBackStack() })
        }
        composable<VerifyEmailRoute> {
            VerifyEmailScreen(
                onBack = { navController.popBackStack() },
                onVerified = { email ->
                    navController.navigate(LoginRoute(verifiedEmail = email)) {
                        popUpTo<LoginRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
