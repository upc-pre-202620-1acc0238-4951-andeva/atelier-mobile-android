package com.andeva.atelier.mobile.features.attendance.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.andeva.atelier.mobile.features.attendance.presentation.AttendanceScreen
import kotlinx.serialization.Serializable

@Serializable
data object AttendanceRoute

/** Grafo de la feature de asistencia; la pantalla obtiene sesión y permisos desde el núcleo. */
fun NavGraphBuilder.attendanceNavGraph(navController: NavController) {
    navigation<AttendanceRoute>(startDestination = AttendanceHomeRoute) {
        composable<AttendanceHomeRoute> { AttendanceScreen() }
    }
}

@Serializable
data object AttendanceHomeRoute
