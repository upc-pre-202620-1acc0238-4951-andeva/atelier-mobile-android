package com.andeva.atelier.mobile.features.diagnostics.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.andeva.atelier.mobile.core.designsystem.component.LocalFloatingBarInset
import com.andeva.atelier.mobile.features.diagnostics.presentation.faults.VehicleFaultsScreen
import com.andeva.atelier.mobile.features.diagnostics.presentation.hub.DiagnosticsHubScreen
import com.andeva.atelier.mobile.features.diagnostics.presentation.live.LiveTelemetryScreen
import com.andeva.atelier.mobile.features.diagnostics.presentation.report.AiHealthReportScreen
import kotlinx.serialization.Serializable

/** Grafo de diagnóstico OBD-II, telemetría, fallas DTC, alertas predictivas e informe IA (EP04). */
@Serializable
data object DiagnosticsNavGraphRoute

/** Hub principal de diagnóstico OBD-II con pestañas de Escáner, Dispositivos y Alertas predictivas. */
@Serializable
data class DiagnosticsHubRoute(val initialVehicleId: String? = null)

/** Telemetría en vivo por Bluetooth o lectura remota (`iot:telemetry:ingest` / `iot:telemetry:read`). */
@Serializable
data class LiveTelemetryRoute(val vehicleId: String, val plate: String? = null)

/** Códigos de falla electrónicos (DTC) activos, escaneo modo 03 y borrado modo 04. */
@Serializable
data class VehicleFaultsRoute(val vehicleId: String, val plate: String? = null)

/** Informe de salud vehicular con IA y descarga en PDF (US21, US43 E1). */
@Serializable
data class AiHealthReportRoute(val vehicleId: String, val plate: String? = null)

fun NavGraphBuilder.diagnosticsNavGraph(
    navController: NavController,
    onReportFinding: (String) -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenSyncQueue: () -> Unit = {},
    onOpenProfile: () -> Unit = {}
) {
    val back: () -> Unit = { navController.popBackStack() }
    navigation<DiagnosticsNavGraphRoute>(startDestination = DiagnosticsHubRoute()) {
        composable<DiagnosticsHubRoute> {
            val showBack = LocalFloatingBarInset.current.value == 0f && navController.previousBackStackEntry != null
            DiagnosticsHubScreen(
                onBack = if (showBack) back else null,
                onOpenLiveTelemetry = { vehicleId, plate ->
                    navController.navigate(LiveTelemetryRoute(vehicleId, plate))
                },
                onOpenVehicleFaults = { vehicleId, plate ->
                    navController.navigate(VehicleFaultsRoute(vehicleId, plate))
                },
                onOpenAiHealthReport = { vehicleId, plate ->
                    navController.navigate(AiHealthReportRoute(vehicleId, plate))
                },
                onReportFinding = onReportFinding,
                onOpenNotifications = onOpenNotifications,
                onOpenSyncQueue = onOpenSyncQueue,
                onOpenProfile = onOpenProfile
            )
        }
        composable<LiveTelemetryRoute> {
            LiveTelemetryScreen(
                onBack = back,
                onOpenFaults = { vehicleId, plate ->
                    navController.navigate(VehicleFaultsRoute(vehicleId, plate))
                }
            )
        }
        composable<VehicleFaultsRoute> {
            VehicleFaultsScreen(onBack = back)
        }
        composable<AiHealthReportRoute> {
            AiHealthReportScreen(onBack = back)
        }
    }
}
