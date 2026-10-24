package com.andeva.atelier.mobile.features.diagnostics.application

import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsRepository
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsSyncState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Lecturas y fallas del vehículo que aún esperan señal (US19 E1). */
class ObserveDiagnosticsSyncStateUseCase @Inject constructor(
    private val diagnosticsRepository: DiagnosticsRepository
) {
    operator fun invoke(vehicleId: String): Flow<DiagnosticsSyncState> = diagnosticsRepository.observeSyncState(vehicleId)
}
