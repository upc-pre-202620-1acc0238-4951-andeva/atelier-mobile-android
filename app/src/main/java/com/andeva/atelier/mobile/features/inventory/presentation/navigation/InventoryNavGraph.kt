package com.andeva.atelier.mobile.features.inventory.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.andeva.atelier.mobile.features.inventory.presentation.catalog.PartsCatalogScreen
import com.andeva.atelier.mobile.features.inventory.presentation.detail.PartDetailScreen
import kotlinx.serialization.Serializable

/** Grafo de inventario (EP07). */
@Serializable
data object InventoryNavGraphRoute

/** US31: catálogo y buscador de repuestos. */
@Serializable
data object PartsCatalogRoute

/** US32: detalle del repuesto con sus lotes en orden FIFO. */
@Serializable
data class PartDetailRoute(val partId: String)

fun NavGraphBuilder.inventoryNavGraph(navController: NavController) {
    navigation<InventoryNavGraphRoute>(startDestination = PartsCatalogRoute) {
        composable<PartsCatalogRoute> {
            PartsCatalogScreen(
                onBack = { navController.popBackStack() },
                onOpenPart = { partId -> navController.navigate(PartDetailRoute(partId)) }
            )
        }
        composable<PartDetailRoute> {
            PartDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
