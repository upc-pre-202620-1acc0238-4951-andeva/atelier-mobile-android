package com.andeva.atelier.mobile.features.diagnostics.presentation.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSegmentedControl
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.LocalFloatingBarInset
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlert
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertStatus
import com.andeva.atelier.mobile.features.diagnostics.presentation.chipKind
import com.andeva.atelier.mobile.features.diagnostics.presentation.formatLimaDateTime
import com.andeva.atelier.mobile.features.diagnostics.presentation.labelRes
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlertsTabContent(
    state: DiagnosticsHubUiState,
    onSetAlertsScope: (AlertsScope) -> Unit,
    onRefreshAlerts: () -> Unit,
    onAcknowledgeAlert: (String) -> Unit,
    onOpenConvertAlert: (PredictiveAlert) -> Unit
) {
    val alerts = state.visibleAlerts
    val showSkeleton = rememberDelayedLoading(state.isLoadingAlerts && alerts.isEmpty())
    val vehiclePlateById = remember(state.workshopVehicles) {
        state.workshopVehicles.associate { it.vehicle.id to it.vehicle.plate }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.selectedVehicleId != null) {
            val scopeLabels = mapOf(
                AlertsScope.TENANT to stringResource(R.string.diag_alerts_scope_tenant),
                AlertsScope.VEHICLE to stringResource(R.string.diag_alerts_scope_vehicle)
            )
            AtelierSegmentedControl(
                options = listOf(AlertsScope.TENANT, AlertsScope.VEHICLE),
                selected = state.alertsScope,
                onSelect = onSetAlertsScope,
                label = { scopeLabels[it].orEmpty() },
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)
            )
        }
        AtelierRefreshBar(visible = state.isLoadingAlerts && alerts.isNotEmpty())
        PullToRefreshBox(
            isRefreshing = state.isLoadingAlerts && alerts.isNotEmpty(),
            onRefresh = onRefreshAlerts,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.md,
                    end = Spacing.md,
                    top = Spacing.sm,
                    bottom = Spacing.lg + LocalFloatingBarInset.current
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                when {
                    showSkeleton -> items(3, key = { "alert-bone-$it" }) {
                        AtelierListRow(
                            title = "Riesgo de sobrecalentamiento",
                            subtitle = "Confianza: 88% · 24/10/2026",
                            plate = "BKL482",
                            loading = true
                        )
                    }
                    alerts.isEmpty() && state.error != null && !state.isOnline -> item(key = "offline") {
                        AtelierOfflineState(onRetry = onRefreshAlerts)
                    }
                    alerts.isEmpty() && state.error != null -> item(key = "error") {
                        AtelierErrorState(message = state.error.asString(), onRetry = onRefreshAlerts)
                    }
                    alerts.isEmpty() -> item(key = "empty") {
                        AtelierEmptyState(text = stringResource(R.string.diag_alerts_empty))
                    }
                    else -> items(alerts, key = { it.id }) { alert ->
                        AlertCard(
                            alert = alert,
                            plate = vehiclePlateById[alert.vehicleId],
                            canAcknowledge = state.canAcknowledgeAlerts && state.isOnline,
                            canConvert = state.canCreateAppointment && state.isOnline,
                            isBusy = state.isActingOnAlert,
                            onAcknowledge = { onAcknowledgeAlert(alert.id) },
                            onConvert = { onOpenConvertAlert(alert) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(
    alert: PredictiveAlert,
    plate: String?,
    canAcknowledge: Boolean,
    canConvert: Boolean,
    isBusy: Boolean,
    onAcknowledge: () -> Unit,
    onConvert: () -> Unit
) {
    val confidencePct = (alert.confidenceScore * 100.0).roundToInt().coerceIn(0, 100)
    val meta = listOfNotNull(
        stringResource(R.string.diag_alerts_confidence, confidencePct),
        alert.createdAt?.formatLimaDateTime()
    ).joinToString(" · ")

    AtelierSurface(
        modifier = Modifier.fillMaxWidth(),
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!plate.isNullOrBlank()) {
                    AtelierPlate(plate = plate, size = PlateSize.M)
                }
                Text(
                    text = stringResource(alert.alertType.labelRes()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                alert.status?.let { st ->
                    AtelierChip(text = stringResource(st.labelRes()), kind = st.chipKind())
                }
            }
            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = AtelierTheme.colors.inkSecondary
            )
            if ((canAcknowledge && alert.status == PredictiveAlertStatus.DISPATCHED) || canConvert) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    if (canAcknowledge && alert.status == PredictiveAlertStatus.DISPATCHED) {
                        AtelierButton(
                            text = stringResource(R.string.diag_alerts_ack),
                            onClick = onAcknowledge,
                            modifier = Modifier.weight(1f),
                            style = AtelierButtonStyle.Outlined,
                            enabled = !isBusy
                        )
                    }
                    if (canConvert) {
                        AtelierButton(
                            text = stringResource(R.string.diag_alerts_convert),
                            onClick = onConvert,
                            modifier = Modifier.weight(1f),
                            enabled = !isBusy,
                            icon = Icons.Outlined.EventAvailable
                        )
                    }
                }
            }
        }
    }
}
