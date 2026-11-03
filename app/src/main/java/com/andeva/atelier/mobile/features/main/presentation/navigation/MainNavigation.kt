package com.andeva.atelier.mobile.features.main.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.icon.assignment
import com.andeva.atelier.mobile.core.designsystem.icon.bluetooth_searching
import com.andeva.atelier.mobile.core.designsystem.icon.home
import com.andeva.atelier.mobile.core.designsystem.icon.person
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.AttendanceHomeRoute
import com.andeva.atelier.mobile.features.attendance.presentation.navigation.AttendanceRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.InventoryNavGraphRoute
import com.andeva.atelier.mobile.features.inventory.presentation.navigation.PartsCatalogRoute
import com.andeva.atelier.mobile.features.main.presentation.obd.ObdScreen
import com.andeva.atelier.mobile.features.main.presentation.profile.ProfileScreen
import com.andeva.atelier.mobile.features.reception.presentation.navigation.ReceptionNavGraphRoute
import com.andeva.atelier.mobile.features.reception.presentation.navigation.VehicleSearchRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.MyTasksRoute
import com.andeva.atelier.mobile.features.tasks.presentation.navigation.TasksNavGraphRoute
import com.andeva.atelier.mobile.features.workorders.presentation.navigation.WorkOrdersRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/** Escáner OBD-II (EP03). La pantalla real llega con la capa de presentación de `diagnostics`. */
@Serializable
data object ObdRoute

/** K1: perfil (técnico) o ajustes (administrador) de la persona con sesión. */
@Serializable
data object ProfileRoute

/**
 * Pestaña de la barra inferior (gallería B1/B2). [graph] es el destino al que se navega; [root] es la pantalla
 * inicial de ese destino: la barra solo se muestra en ellas y se oculta en los detalles.
 */
sealed class MainTab(
    val graph: Any,
    val root: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector
) {
    data object Home : MainTab(AttendanceRoute, AttendanceHomeRoute::class, R.string.main_tab_home, home)
    data object Tasks : MainTab(TasksNavGraphRoute, MyTasksRoute::class, R.string.main_tab_tasks, assignment)
    data object Orders : MainTab(WorkOrdersRoute, WorkOrdersRoute::class, R.string.main_tab_orders, assignment)
    data object Reception : MainTab(
        ReceptionNavGraphRoute, VehicleSearchRoute::class, R.string.main_tab_reception, Icons.Outlined.AddCircleOutline
    )
    data object Obd : MainTab(ObdRoute, ObdRoute::class, R.string.main_tab_obd, bluetooth_searching)
    data object Stock : MainTab(
        InventoryNavGraphRoute, PartsCatalogRoute::class, R.string.main_tab_stock, Icons.Outlined.Inventory2
    )
    data object Profile : MainTab(ProfileRoute, ProfileRoute::class, R.string.main_tab_profile, person)
    data object Settings : MainTab(ProfileRoute, ProfileRoute::class, R.string.main_tab_settings, Icons.Outlined.Settings)

    companion object {
        /**
         * Pestañas según los permisos de la sesión, nunca según el rol (docs/02-roles-and-permissions.md).
         * - Con `operations:tasks:track_time` (quien ejecuta tareas) la barra es la del técnico: Inicio, Tareas,
         *   [Órdenes si puede abrirlas], OBD-II y Perfil.
         * - Sin él es la del administrador: Inicio, Órdenes, Recepción, Stock y Ajustes.
         * Cada pestaña aparece solo si hay permiso para su pantalla principal.
         */
        fun tabsFor(session: Session): List<MainTab> {
            return if (session.has(Permission.TASKS_TRACK_TIME)) {
                buildList {
                    add(Home)
                    if (session.has(Permission.TASKS_READ)) add(Tasks)
                    if (session.has(Permission.WORK_ORDERS_CREATE)) add(Orders)
                    if (session.canUseTelemetry()) add(Obd)
                    add(Profile)
                }
            } else {
                buildList {
                    add(Home)
                    if (session.has(Permission.WORK_ORDERS_READ)) add(Orders)
                    if (session.has(Permission.VEHICLES_CREATE)) add(Reception)
                    if (session.has(Permission.PARTS_READ)) add(Stock)
                    add(Settings)
                }
            }
        }
    }
}

/** Cambia de pestaña conservando el estado de cada una (scroll, búsqueda) y sin apilar copias. */
fun NavController.navigateToTab(tab: MainTab) {
    navigate(tab.graph) {
        popUpTo<AttendanceRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Destinos propios de `main`: OBD-II y perfil. Los demás los aportan sus features. */
fun NavGraphBuilder.mainTabsDestinations() {
    composable<ObdRoute> { ObdScreen() }
    composable<ProfileRoute> { ProfileScreen() }
}
