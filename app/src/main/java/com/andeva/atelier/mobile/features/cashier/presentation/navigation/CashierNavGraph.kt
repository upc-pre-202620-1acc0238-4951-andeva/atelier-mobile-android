package com.andeva.atelier.mobile.features.cashier.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.andeva.atelier.mobile.features.cashier.presentation.checkout.WorkOrderCheckoutScreen
import com.andeva.atelier.mobile.features.cashier.presentation.dailycash.DailyCashScreen
import com.andeva.atelier.mobile.features.cashier.presentation.series.FiscalSeriesScreen
import kotlinx.serialization.Serializable

/** Caja del día de la sede (`invoicing:invoices:read`). */
@Serializable
data object DailyCashRoute

/** Series SUNAT de la sede (`invoicing:fiscal_config:manage`). */
@Serializable
data object FiscalSeriesRoute

/** "Cobrar y entregar" una orden: comprobante, cobros, pase de salida y entrega. */
@Serializable
data class WorkOrderCheckoutRoute(val workOrderId: String)

fun NavGraphBuilder.cashierNavGraph(navController: NavController) {
    composable<DailyCashRoute> { DailyCashScreen(onBack = { navController.popBackStack() }) }
    composable<FiscalSeriesRoute> { FiscalSeriesScreen(onBack = { navController.popBackStack() }) }
    composable<WorkOrderCheckoutRoute> { WorkOrderCheckoutScreen(onBack = { navController.popBackStack() }) }
}
