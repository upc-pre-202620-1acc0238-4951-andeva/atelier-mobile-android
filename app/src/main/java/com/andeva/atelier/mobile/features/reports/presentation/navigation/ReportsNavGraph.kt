package com.andeva.atelier.mobile.features.reports.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.andeva.atelier.mobile.features.reports.presentation.ReportsScreen
import kotlinx.serialization.Serializable

/** Flujo de caja del taller y descargas en PDF. */
@Serializable
data object ReportsRoute

fun NavGraphBuilder.reportsNavGraph(navController: NavController) {
    composable<ReportsRoute> { ReportsScreen(onBack = { navController.popBackStack() }) }
}
