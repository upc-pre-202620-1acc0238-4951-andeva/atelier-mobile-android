package com.andeva.atelier.mobile.features.inventory.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.andeva.atelier.mobile.features.inventory.presentation.alerts.StockAlertsScreen
import com.andeva.atelier.mobile.features.inventory.presentation.catalog.PartsCatalogScreen
import com.andeva.atelier.mobile.features.inventory.presentation.detail.PartDetailScreen
import com.andeva.atelier.mobile.features.inventory.presentation.orders.PurchaseOrderDetailScreen
import com.andeva.atelier.mobile.features.inventory.presentation.orders.PurchaseOrdersScreen
import com.andeva.atelier.mobile.features.inventory.presentation.valuation.InventoryValuationScreen
import kotlinx.serialization.Serializable

/** Grafo de inventario (EP07). Se entra desde "Más". */
@Serializable
data object InventoryNavGraphRoute

/** US31: catálogo y buscador de repuestos. */
@Serializable
data object PartsCatalogRoute

/** US32: detalle del repuesto con sus lotes en orden FIFO. */
@Serializable
data class PartDetailRoute(val partId: String)

/** US34 E1: alertas de stock mínimo. */
@Serializable
data object StockAlertsRoute

/** US34 E2: órdenes de compra. */
@Serializable
data object PurchaseOrdersRoute

/** US34 E2: una orden de compra y su recepción. */
@Serializable
data class PurchaseOrderDetailRoute(val purchaseOrderId: String)

/** Valorización del inventario. */
@Serializable
data object InventoryValuationRoute

fun NavGraphBuilder.inventoryNavGraph(navController: NavController) {
    val back: () -> Unit = { navController.popBackStack() }
    val openPart: (String) -> Unit = { partId -> navController.navigate(PartDetailRoute(partId)) }
    navigation<InventoryNavGraphRoute>(startDestination = PartsCatalogRoute) {
        composable<PartsCatalogRoute> { PartsCatalogScreen(onBack = back, onOpenPart = openPart) }
        composable<PartDetailRoute> { PartDetailScreen(onBack = back) }
        composable<StockAlertsRoute> { StockAlertsScreen(onBack = back, onOpenPart = openPart) }
        composable<PurchaseOrdersRoute> {
            PurchaseOrdersScreen(
                onBack = back,
                onOpenOrder = { id -> navController.navigate(PurchaseOrderDetailRoute(id)) }
            )
        }
        composable<PurchaseOrderDetailRoute> { PurchaseOrderDetailScreen(onBack = back) }
        composable<InventoryValuationRoute> { InventoryValuationScreen(onBack = back) }
    }
}
