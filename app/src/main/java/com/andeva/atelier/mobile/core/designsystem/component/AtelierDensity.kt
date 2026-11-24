package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Densidad táctil ambiental (DESIGN.md y segment-2-floor-counter):
 * - Estándar (mostrador, administración): 48 dp.
 * - Piso y fosa (`FloorHub`, `TaskDetail`, `DiagnosticsHub`, `LiveTelemetry`, `VehicleFaults`): 56 dp
 *   provisto en la raíz de pantalla para manos con grasa o guantes.
 */
val LocalAtelierMinTouchTarget = compositionLocalOf { 48.dp }
