package com.andeva.atelier.mobile.features.diagnostics.presentation.live

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFichaPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierLiveBeacon
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMetricBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPidTile
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlanLockedState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.LocalAtelierMinTouchTarget
import com.andeva.atelier.mobile.core.designsystem.component.PidSeverity
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.features.diagnostics.domain.DiagnosticsSyncState
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetryHistoryBucket
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetrySample
import com.andeva.atelier.mobile.features.diagnostics.domain.TelemetryThresholds
import com.andeva.atelier.mobile.features.diagnostics.presentation.chipKind
import com.andeva.atelier.mobile.features.diagnostics.presentation.formatLimaDateTime
import com.andeva.atelier.mobile.features.diagnostics.presentation.formatLimaTime
import com.andeva.atelier.mobile.features.diagnostics.presentation.labelRes
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToInt

private data class PidMetric(
    val label: String,
    val value: String,
    val unit: String,
    val severity: PidSeverity = PidSeverity.NORMAL,
    val statusText: String? = null,
    val progress: Float? = null,
    val markerAt: Float? = null
)

@Composable
fun LiveTelemetryScreen(
    onBack: () -> Unit,
    onOpenFaults: (vehicleId: String, plate: String?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveTelemetryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LiveTelemetryContent(
        state = state,
        onBack = onBack,
        onStartStreaming = viewModel::startStreamingIfEligible,
        onStopStreaming = viewModel::stopStreaming,
        onFreezeSample = viewModel::freezeSample,
        onDismissFrozenSnapshot = viewModel::dismissFrozenSnapshot,
        onRefreshRemote = viewModel::loadRemoteTelemetry,
        onOpenFaults = { onOpenFaults(state.vehicleId, state.displayPlate.ifBlank { null }) },
        onReconnect = viewModel::reconnect,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LiveTelemetryContent(
    state: LiveTelemetryUiState,
    onBack: () -> Unit,
    onStartStreaming: () -> Unit,
    onStopStreaming: () -> Unit,
    onFreezeSample: () -> Unit,
    onDismissFrozenSnapshot: () -> Unit,
    onRefreshRemote: () -> Unit,
    onOpenFaults: () -> Unit,
    modifier: Modifier = Modifier,
    onReconnect: () -> Unit = {}
) = CompositionLocalProvider(LocalAtelierMinTouchTarget provides 56.dp) {
    val haptics = rememberAtelierHaptics()
    val isRemote = state.isRemoteMode
    val showSkeleton = rememberDelayedLoading(
        isRemote && state.isLoadingRemote && state.remoteTelemetry == null
    )
    val totalPending = state.syncState.pendingReadings + state.syncState.pendingFaults
    val syncSubtitle = if (totalPending > 0) {
        stringResource(R.string.diag_live_sync_pending, totalPending)
    } else {
        stringResource(R.string.diag_live_sync_ok)
    }
    val vehicleTitle = state.vehicle?.let { "${it.brand} ${it.model}".trim() }.orEmpty()
        .ifBlank { stringResource(R.string.diag_live_title) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AtelierFichaPanel(
                onBack = onBack,
                title = vehicleTitle,
                subtitle = syncSubtitle,
                identity = {
                    val plate = state.displayPlate
                    if (plate.isNotBlank()) {
                        AtelierPlate(plate = plate, size = PlateSize.L)
                    }
                },
                status = {
                    if (isRemote) {
                        AtelierChip(
                            text = stringResource(R.string.diag_bt_status_remote),
                            kind = ChipKind.Info,
                            icon = Icons.Outlined.CloudDownload
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            if (state.linkState == ObdLinkState.STREAMING) {
                                AtelierLiveBeacon(
                                    color = AtelierTheme.colors.onPanel,
                                    pulsing = true
                                )
                            }
                            AtelierChip(
                                text = stringResource(state.linkState.labelRes()),
                                kind = state.linkState.chipKind()
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state.planIncludesTelemetry && (state.canUseTelemetry || state.canReadTelemetry)) {
                LiveTelemetryBottomBar(
                    state = state,
                    onStart = {
                        haptics.confirm()
                        onStartStreaming()
                    },
                    onStop = {
                        haptics.toggle()
                        onStopStreaming()
                    },
                    onFreeze = {
                        haptics.confirm()
                        onFreezeSample()
                    },
                    onReconnect = {
                        haptics.confirm()
                        onReconnect()
                    },
                    onRefreshRemote = onRefreshRemote,
                    onOpenFaults = onOpenFaults
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.planIncludesTelemetry -> AtelierPlanLockedState(
                    text = stringResource(R.string.diag_hub_plan_locked),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                !state.canUseTelemetry && !state.canReadTelemetry -> AtelierNoPermissionState(
                    text = stringResource(R.string.diag_live_no_permission),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                else -> {
                    AtelierRefreshBar(visible = isRemote && state.isLoadingRemote && state.remoteTelemetry != null)
                    PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = { if (isRemote) onRefreshRemote() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val metrics = buildPidMetrics(state)
                        val rows = metrics.chunked(2)
                        val sampleTime = if (isRemote) state.remoteTelemetry?.timestamp else state.liveSample?.timestamp

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            if (state.connectedToOtherVehicle) {
                                item(key = "other-vehicle-banner") {
                                    AtelierBanner(
                                        message = stringResource(R.string.diag_live_other_vehicle_warning),
                                        icon = Icons.Outlined.WarningAmber,
                                        kind = BannerKind.Wait
                                    )
                                }
                            }
                            if (isRemote) {
                                item(key = "remote-banner") {
                                    AtelierBanner(
                                        message = stringResource(R.string.diag_live_remote_banner),
                                        icon = Icons.Outlined.Info,
                                        kind = BannerKind.Info
                                    )
                                }
                            }
                            if (state.missingCoolantPid0105) {
                                item(key = "h23-banner") {
                                    AtelierBanner(
                                        message = stringResource(R.string.diag_live_missing_pid_0105),
                                        icon = Icons.Outlined.WarningAmber,
                                        kind = BannerKind.Wait
                                    )
                                }
                            }
                            if (state.linkState == ObdLinkState.LOST) {
                                item(key = "lost-banner") {
                                    AtelierBanner(
                                        message = stringResource(R.string.diag_err_connection_lost),
                                        icon = Icons.Outlined.ErrorOutline,
                                        kind = BannerKind.Bad
                                    )
                                }
                            }
                            state.error?.let { err ->
                                if (!isRemote || state.remoteTelemetry != null) {
                                    item(key = "error-banner") {
                                        AtelierBanner(message = err.asString(), icon = Icons.Outlined.ErrorOutline)
                                    }
                                }
                            }

                            when {
                                isRemote && state.remoteTelemetry == null && state.error != null && !state.isOnline -> {
                                    item(key = "offline") {
                                        AtelierOfflineState(onRetry = onRefreshRemote)
                                    }
                                }
                                isRemote && state.remoteTelemetry == null && state.error != null -> {
                                    item(key = "error") {
                                        AtelierErrorState(message = state.error.asString(), onRetry = onRefreshRemote)
                                    }
                                }
                                isRemote && !state.isLoadingRemote && state.remoteTelemetry == null -> {
                                    item(key = "empty-remote") {
                                        AtelierEmptyState(
                                            text = stringResource(R.string.diag_live_remote_empty),
                                            actionLabel = stringResource(R.string.diag_live_refresh),
                                            onAction = onRefreshRemote
                                        )
                                    }
                                }
                                else -> {
                                    item(key = "sample-time") {
                                        val timeText = sampleTime?.let {
                                            stringResource(R.string.diag_live_last_sample, it.formatLimaTime())
                                        } ?: stringResource(R.string.diag_live_waiting_sample)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                        ) {
                                            if (state.linkState == ObdLinkState.STREAMING) {
                                                AtelierLiveBeacon(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    pulsing = true
                                                )
                                            }
                                            Text(
                                                text = timeText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = AtelierTheme.colors.inkSecondary
                                            )
                                        }
                                    }
                                    items(rows.size, key = { "pid-row-$it" }) { idx ->
                                        val pair = rows[idx]
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                        ) {
                                            pair.forEach { metric ->
                                                CalibratedPidTile(
                                                    metric = metric,
                                                    loading = showSkeleton,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (pair.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                    if (isRemote && state.telemetryHistory.isNotEmpty()) {
                                        item(key = "history-header") {
                                            AtelierSectionHeader(
                                                title = stringResource(R.string.diag_live_history_header),
                                                count = state.telemetryHistory.size,
                                                modifier = Modifier.padding(top = Spacing.sm)
                                            )
                                        }
                                        items(state.telemetryHistory, key = { it.bucketTime?.toString() ?: it.hashCode() }) { bucket ->
                                            HistoryBucketRow(bucket)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.frozenSnapshot?.let { snapshot ->
        AtelierBottomSheet(
            title = stringResource(R.string.diag_live_frozen_title),
            onDismiss = onDismissFrozenSnapshot,
            actions = {
                AtelierButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismissFrozenSnapshot,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined
                )
            }
        ) {
            Text(
                text = stringResource(R.string.diag_live_frozen_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
            Text(
                text = stringResource(R.string.diag_live_last_sample, snapshot.sample.timestamp.formatLimaDateTime()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            val frozenMetrics = metricsForSample(snapshot.sample)
            frozenMetrics.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    pair.forEach { metric ->
                        CalibratedPidTile(
                            metric = metric,
                            loading = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LiveTelemetryBottomBar(
    state: LiveTelemetryUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onFreeze: () -> Unit,
    onReconnect: () -> Unit,
    onRefreshRemote: () -> Unit,
    onOpenFaults: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (state.isBluetoothSessionActive && state.canUseTelemetry) {
                    if (state.linkState == ObdLinkState.LOST) {
                        AtelierButton(
                            text = stringResource(R.string.diag_live_reconnect),
                            onClick = onReconnect,
                            modifier = Modifier.weight(1f),
                            loading = state.isReconnecting,
                            enabled = !state.isReconnecting,
                            icon = Icons.Outlined.Refresh
                        )
                        if (state.canReadFaults) {
                            AtelierButton(
                                text = stringResource(R.string.diag_live_faults),
                                onClick = onOpenFaults,
                                style = AtelierButtonStyle.Outlined,
                                icon = Icons.Outlined.Build
                            )
                        }
                    } else {
                        AtelierButton(
                            text = stringResource(R.string.diag_live_freeze),
                            onClick = onFreeze,
                            modifier = Modifier.weight(1f),
                            style = AtelierButtonStyle.Outlined,
                            enabled = state.liveSample != null,
                            icon = Icons.Outlined.AcUnit
                        )
                        if (state.canReadFaults) {
                            AtelierButton(
                                text = stringResource(R.string.diag_live_faults),
                                onClick = onOpenFaults,
                                style = AtelierButtonStyle.Outlined,
                                icon = Icons.Outlined.Build
                            )
                        }
                        val streaming = state.linkState == ObdLinkState.STREAMING ||
                            state.linkState == ObdLinkState.RECONNECTING
                        AtelierButton(
                            text = stringResource(if (streaming) R.string.diag_live_stop else R.string.diag_live_start),
                            onClick = if (streaming) onStop else onStart,
                            style = if (streaming) AtelierButtonStyle.Destructive else AtelierButtonStyle.Filled,
                            icon = if (streaming) Icons.Outlined.Stop else Icons.Outlined.PlayArrow
                        )
                    }
                } else {
                    AtelierButton(
                        text = stringResource(R.string.diag_live_refresh),
                        onClick = onRefreshRemote,
                        modifier = Modifier.weight(1f),
                        style = AtelierButtonStyle.Outlined,
                        enabled = !state.isLoadingRemote && state.isOnline,
                        loading = state.isLoadingRemote,
                        icon = Icons.Outlined.Refresh
                    )
                    if (state.canReadFaults) {
                        AtelierButton(
                            text = stringResource(R.string.diag_live_faults),
                            onClick = onOpenFaults,
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Build
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryBucketRow(bucket: TelemetryHistoryBucket) {
    val parts = buildList {
        bucket.avgEngineRpm?.let { add("$it rpm") }
        bucket.avgEngineTempCelsius?.let { add("${it.roundToInt()} °C") }
        bucket.avgBatteryVoltage?.let { add(String.format(Locale.US, "%.1f V", it)) }
        bucket.avgSpeedKmh?.let { add("$it km/h") }
    }.joinToString(" · ")
    AtelierListRow(
        title = bucket.bucketTime?.formatLimaDateTime() ?: "—",
        subtitle = parts.ifBlank { null },
        trailing = {
            AtelierChip(
                text = stringResource(R.string.diag_live_history_samples, bucket.sampleCount),
                kind = ChipKind.Idle
            )
        }
    )
}

@Composable
private fun buildPidMetrics(state: LiveTelemetryUiState): List<PidMetric> {
    val sample = if (state.isRemoteMode) {
        val remote = state.remoteTelemetry
        TelemetrySample(
            timestamp = remote?.timestamp ?: Instant.EPOCH,
            engineTempCelsius = remote?.engineTempCelsius,
            engineRpm = remote?.engineRpm,
            speedKmh = remote?.speedKmh,
            batteryVoltage = remote?.batteryVoltage,
            fuelPercentage = remote?.fuelPercentage,
            intakePressureKpa = null
        )
    } else {
        state.liveSample
    }
    return metricsForSample(sample)
}

@Composable
private fun metricsForSample(sample: TelemetrySample?): List<PidMetric> {
    val rpm = sample?.engineRpm
    val rpmSeverity = when {
        rpm == null -> PidSeverity.NORMAL
        rpm > TelemetryThresholds.MAX_ENGINE_RPM -> PidSeverity.CRITICAL
        rpm > 5000 -> PidSeverity.WARNING
        else -> PidSeverity.NORMAL
    }
    val rpmStatus = when (rpmSeverity) {
        PidSeverity.CRITICAL -> stringResource(R.string.diag_pid_status_high_rpm)
        PidSeverity.WARNING -> stringResource(R.string.diag_pid_status_high_rpm)
        PidSeverity.NORMAL -> if (rpm != null) stringResource(R.string.diag_pid_status_normal) else null
    }

    val temp = sample?.engineTempCelsius
    val tempSeverity = when {
        temp == null -> PidSeverity.NORMAL
        temp > TelemetryThresholds.MAX_COOLANT_CELSIUS -> PidSeverity.CRITICAL
        temp > 98.0 -> PidSeverity.WARNING
        else -> PidSeverity.NORMAL
    }
    val tempStatus = when (tempSeverity) {
        PidSeverity.CRITICAL -> stringResource(R.string.diag_pid_status_overheat)
        PidSeverity.WARNING -> stringResource(R.string.diag_pid_status_temp_high)
        PidSeverity.NORMAL -> if (temp != null) stringResource(R.string.diag_pid_status_normal) else null
    }

    val volt = sample?.batteryVoltage
    val voltSeverity = when {
        volt == null -> PidSeverity.NORMAL
        volt < TelemetryThresholds.MIN_BATTERY_VOLTS || volt > TelemetryThresholds.MAX_BATTERY_VOLTS -> PidSeverity.CRITICAL
        volt < 12.4 -> PidSeverity.WARNING
        else -> PidSeverity.NORMAL
    }
    val voltStatus = when {
        volt == null -> null
        volt < TelemetryThresholds.MIN_BATTERY_VOLTS || volt < 12.4 -> stringResource(R.string.diag_pid_status_low_volt)
        volt > TelemetryThresholds.MAX_BATTERY_VOLTS -> stringResource(R.string.diag_pid_status_high_volt)
        else -> stringResource(R.string.diag_pid_status_normal)
    }

    val load = sample?.engineLoadPercentage
    val loadSeverity = if (load != null && load > 90.0) PidSeverity.WARNING else PidSeverity.NORMAL
    val loadStatus = when {
        load == null -> null
        loadSeverity == PidSeverity.WARNING -> stringResource(R.string.diag_pid_status_high_load)
        else -> stringResource(R.string.diag_pid_status_normal)
    }

    val speed = sample?.speedKmh
    val throttle = sample?.throttlePositionPercentage
    val fuel = sample?.fuelPercentage
    val fuelSeverity = if (fuel != null && fuel < 15.0) PidSeverity.WARNING else PidSeverity.NORMAL
    val fuelStatus = when {
        fuel == null -> null
        fuelSeverity == PidSeverity.WARNING -> stringResource(R.string.diag_pid_status_low_fuel)
        else -> stringResource(R.string.diag_pid_status_normal)
    }

    return listOf(
        PidMetric(
            label = stringResource(R.string.diag_pid_rpm),
            value = rpm?.toString() ?: "—",
            unit = if (rpm != null) stringResource(R.string.diag_pid_unit_rpm) else "",
            severity = rpmSeverity,
            statusText = rpmStatus,
            progress = rpm?.let { (it / 7000f).coerceIn(0f, 1f) }
        ),
        PidMetric(
            label = stringResource(R.string.diag_pid_coolant),
            value = temp?.roundToInt()?.toString() ?: "—",
            unit = if (temp != null) stringResource(R.string.diag_pid_unit_celsius) else "",
            severity = tempSeverity,
            statusText = tempStatus,
            progress = temp?.let { (it.toFloat() / 120f).coerceIn(0f, 1f) },
            markerAt = 0.82f
        ),
        PidMetric(
            label = stringResource(R.string.diag_pid_voltage),
            value = volt?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
            unit = if (volt != null) stringResource(R.string.diag_pid_unit_volts) else "",
            severity = voltSeverity,
            statusText = voltStatus,
            progress = volt?.let { ((it.toFloat() - 10f) / 6f).coerceIn(0f, 1f) }
        ),
        PidMetric(
            label = stringResource(R.string.diag_pid_load),
            value = load?.roundToInt()?.toString() ?: "—",
            unit = if (load != null) stringResource(R.string.diag_pid_unit_percent) else "",
            severity = loadSeverity,
            statusText = loadStatus,
            progress = load?.let { (it.toFloat() / 100f).coerceIn(0f, 1f) }
        ),
        PidMetric(
            label = stringResource(R.string.diag_pid_speed),
            value = speed?.toString() ?: "—",
            unit = if (speed != null) stringResource(R.string.diag_pid_unit_kmh) else "",
            progress = speed?.let { (it / 180f).coerceIn(0f, 1f) }
        ),
        PidMetric(
            label = stringResource(R.string.diag_pid_throttle),
            value = throttle?.roundToInt()?.toString() ?: "—",
            unit = if (throttle != null) stringResource(R.string.diag_pid_unit_percent) else "",
            progress = throttle?.let { (it.toFloat() / 100f).coerceIn(0f, 1f) }
        ),
        PidMetric(
            label = stringResource(R.string.diag_pid_fuel),
            value = fuel?.roundToInt()?.toString() ?: "—",
            unit = if (fuel != null) stringResource(R.string.diag_pid_unit_percent) else "",
            severity = fuelSeverity,
            statusText = fuelStatus,
            progress = fuel?.let { (it.toFloat() / 100f).coerceIn(0f, 1f) }
        ),
        PidMetric(
            label = stringResource(R.string.diag_pid_intake_pressure),
            value = sample?.intakePressureKpa?.toString() ?: "—",
            unit = if (sample?.intakePressureKpa != null) stringResource(R.string.diag_pid_unit_kpa) else "",
            statusText = if (sample?.intakePressureKpa != null) stringResource(R.string.diag_pid_status_normal) else null,
            progress = sample?.intakePressureKpa?.let { (it / 255f).coerceIn(0f, 1f) }
        )
    )
}

@Composable
private fun CalibratedPidTile(
    metric: PidMetric,
    loading: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    val barColor = when (metric.severity) {
        PidSeverity.CRITICAL -> colors.badMark
        PidSeverity.WARNING -> colors.warnMark
        PidSeverity.NORMAL -> MaterialTheme.colorScheme.primary
    }
    val tier = if (metric.severity == PidSeverity.NORMAL) SurfaceTier.Card else SurfaceTier.Elevated

    AtelierSurface(
        modifier = modifier,
        tier = tier,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.sm)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AtelierPidTile(
                label = metric.label,
                value = metric.value,
                unit = metric.unit,
                severity = metric.severity,
                statusText = metric.statusText,
                loading = loading,
                modifier = Modifier.fillMaxWidth()
            )
            if (!loading) {
                AtelierMetricBar(
                    progress = metric.progress ?: 0f,
                    color = barColor,
                    markerAt = metric.markerAt
                )
            }
        }
    }
}

// region Previews

private val previewLiveState = LiveTelemetryUiState(
    vehicleId = "v1",
    plate = "BKL482",
    canUseTelemetry = true,
    canReadTelemetry = true,
    canReadFaults = true,
    linkState = ObdLinkState.STREAMING,
    liveSample = TelemetrySample(
        timestamp = Instant.parse("2026-10-24T14:00:00Z"),
        engineTempCelsius = 108.0,
        engineRpm = 2450,
        speedKmh = 0,
        batteryVoltage = 13.8,
        fuelPercentage = 58.0,
        intakePressureKpa = 36,
        engineLoadPercentage = 42.0,
        throttlePositionPercentage = 18.0
    ),
    syncState = DiagnosticsSyncState(pendingReadings = 12, pendingFaults = 0)
)

@Preview(name = "Telemetría · claro", widthDp = 390, heightDp = 844)
@Composable
private fun LivePreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    LiveTelemetryContent(previewLiveState, {}, {}, {}, {}, {}, {}, {})
}

@Preview(name = "Telemetría · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LivePreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    LiveTelemetryContent(previewLiveState, {}, {}, {}, {}, {}, {}, {})
}

// endregion
