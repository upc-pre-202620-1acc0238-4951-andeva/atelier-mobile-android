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
import com.andeva.atelier.mobile.features.inventory.presentation.scanner.SkuScannerScreen
import com.andeva.atelier.mobile.features.inventory.presentation.suppliers.SuppliersScreen
import com.andeva.atelier.mobile.features.inventory.presentation.valuation.InventoryValuationScreen
import kotlinx.serialization.Serializable

/** Grafo de inventario (EP07). */
@Serializable
data object InventoryNavGraphRoute

/** US31: catálogo y buscador de repuestos. */
@Serializable
data object PartsCatalogRoute

/** US31 E1: escáner de códigos de barras/QR por SKU. */
@Serializable
data object SkuScannerRoute

/** US32: detalle del repuesto con sus lotes en orden FIFO. */
@Serializable
data class PartDetailRoute(val partId: String)

/** US34 E1: alertas de stock mínimo. */
@Serializable
data object StockAlertsRoute

/** Catálogo y alta/edición de proveedores homologados. */
@Serializable
data object SuppliersRoute

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
    val openScanner: () -> Unit = { navController.navigate(SkuScannerRoute) }
    val openPurchaseOrders: () -> Unit = { navController.navigate(PurchaseOrdersRoute) }

    navigation<InventoryNavGraphRoute>(startDestination = PartsCatalogRoute) {
        composable<PartsCatalogRoute> {
            PartsCatalogScreen(
                onBack = back,
                onOpenPart = openPart,
                onOpenScanner = openScanner
            )
        }
        composable<SkuScannerRoute> {
            SkuScannerScreen(
                onBack = back,
                onOpenPart = { partId ->
                    navController.navigate(PartDetailRoute(partId)) {
                        popUpTo<SkuScannerRoute> { inclusive = true }
                    }
                }
            )
        }
        composable<PartDetailRoute> {
            PartDetailScreen(
                onBack = back,
                onOpenPurchaseOrders = openPurchaseOrders
            )
        }
        composable<StockAlertsRoute> {
            StockAlertsScreen(
                onBack = back,
                onOpenPart = openPart,
                onCreatePurchaseOrder = openPurchaseOrders
            )
        }
        composable<SuppliersRoute> { SuppliersScreen(onBack = back) }
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
