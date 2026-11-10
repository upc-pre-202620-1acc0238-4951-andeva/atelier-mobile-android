package com.andeva.atelier.mobile.features.auth.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.andeva.atelier.mobile.features.auth.presentation.branch.BranchSelectionScreen
import com.andeva.atelier.mobile.features.auth.presentation.forgot.ForgotPasswordScreen
import com.andeva.atelier.mobile.features.auth.presentation.login.LoginScreen
import com.andeva.atelier.mobile.features.auth.presentation.register.RegisterScreen
import com.andeva.atelier.mobile.features.auth.presentation.reset.ResetPasswordScreen
import com.andeva.atelier.mobile.features.auth.presentation.verifyemail.VerifyEmailScreen
import com.andeva.atelier.mobile.navigation.AuthNavGraphRoute
import kotlinx.serialization.Serializable

/**
 * A5. [verifiedEmail] llega desde A4 al verificar el correo: precarga el campo y muestra el aviso.
 * [passwordReset] llega desde A6 al cambiar la contraseña: muestra el aviso de éxito.
 */
@Serializable
data class LoginRoute(val verifiedEmail: String? = null, val passwordReset: Boolean = false)

/** A3. [token] queda para el deep link del correo (H1 de docs/03-traceability.md); hoy se pega el enlace. */
@Serializable
data class RegisterRoute(val token: String? = null)

/** A4. */
@Serializable
data class VerifyEmailRoute(val email: String)

/** A6, paso 1: pedir el enlace de restablecimiento por correo. */
@Serializable
data object ForgotPasswordRoute

/** A6, paso 2. [token] queda para el deep link del correo (H1); hoy se pega el enlace o el código. */
@Serializable
data class ResetPasswordRoute(val token: String? = null)

/**
 * Ya con sesión: la ve el grafo principal cuando `Session.needsBranchChoice` (cuenta sin perfil laboral).
 * Al terminar, quien la registra decide a dónde ir (`onDone`).
 */
@Serializable
data object BranchSelectionRoute

fun NavGraphBuilder.branchSelectionDestination(onDone: () -> Unit) {
    composable<BranchSelectionRoute> { BranchSelectionScreen(onDone = onDone) }
}

/**
 * Grafo sin sesión. Al iniciar sesión o aceptar la invitación no se navega desde aquí:
 * `AppNavHost` cambia al grafo con sesión cuando `SessionManager` guarda la sesión.
 */
fun NavGraphBuilder.authNavGraph(navController: NavController) {
    navigation<AuthNavGraphRoute>(startDestination = LoginRoute()) {
        composable<LoginRoute> {
            LoginScreen(
                onHaveInvitation = { navController.navigate(RegisterRoute()) },
                onVerifyEmail = { email -> navController.navigate(VerifyEmailRoute(email)) },
                onForgotPassword = { navController.navigate(ForgotPasswordRoute) }
            )
        }
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(
                onBack = { navController.popBackStack() },
                onHaveToken = { navController.navigate(ResetPasswordRoute()) }
            )
        }
        composable<ResetPasswordRoute> {
            ResetPasswordScreen(
                onBack = { navController.popBackStack() },
                onRequestNewLink = {
                    // Vuelve a la solicitud sin apilar otra copia.
                    if (!navController.popBackStack<ForgotPasswordRoute>(inclusive = false)) {
                        navController.navigate(ForgotPasswordRoute)
                    }
                },
                onDone = {
                    navController.navigate(LoginRoute(passwordReset = true)) {
                        popUpTo<LoginRoute> { inclusive = true }
                        launchSingleTop = true
                    }
                }
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
