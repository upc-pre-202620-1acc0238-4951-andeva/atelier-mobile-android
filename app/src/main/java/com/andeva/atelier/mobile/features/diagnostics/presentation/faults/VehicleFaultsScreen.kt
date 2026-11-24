package com.andeva.atelier.mobile.features.diagnostics.presentation.faults

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.LaunchedEffect
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlanLockedState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.LocalAtelierMinTouchTarget
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.features.diagnostics.domain.ClearFaultsResult
import com.andeva.atelier.mobile.features.diagnostics.domain.Dtc
import com.andeva.atelier.mobile.features.diagnostics.domain.DtcCatalog
import com.andeva.atelier.mobile.features.diagnostics.domain.FaultSeverity
import com.andeva.atelier.mobile.features.diagnostics.domain.FreezeFrame
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.domain.VehicleFault
import com.andeva.atelier.mobile.features.diagnostics.presentation.chipKind
import com.andeva.atelier.mobile.features.diagnostics.presentation.formatLimaDateTime
import com.andeva.atelier.mobile.features.diagnostics.presentation.labelRes
import java.time.Instant
import kotlin.math.roundToInt

@Composable
fun VehicleFaultsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VehicleFaultsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VehicleFaultsContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onScanDtcs = viewModel::scanDtcs,
        onOpenClearConfirm = viewModel::openClearConfirm,
        onDismissClearConfirm = viewModel::dismissClearConfirm,
        onConfirmClearDtcs = viewModel::confirmClearDtcs,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VehicleFaultsContent(
    state: VehicleFaultsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onScanDtcs: () -> Unit,
    onOpenClearConfirm: () -> Unit,
    onDismissClearConfirm: () -> Unit,
    onConfirmClearDtcs: () -> Unit,
    modifier: Modifier = Modifier
) = CompositionLocalProvider(LocalAtelierMinTouchTarget provides 56.dp) {
    val haptics = rememberAtelierHaptics()
    val showSkeleton = rememberDelayedLoading(state.isRefreshing && !state.isLoaded && state.faults.isEmpty())
    val vehicleTitle = state.vehicle?.let { "${it.brand} ${it.model}".trim() }.orEmpty()
        .ifBlank { stringResource(R.string.diag_faults_title) }

    LaunchedEffect(state.lastClearResult) {
        val result = state.lastClearResult ?: return@LaunchedEffect
        if (result.isClean) haptics.confirm() else haptics.reject()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AtelierFichaPanel(
                onBack = onBack,
                title = vehicleTitle,
                subtitle = stringResource(R.string.diag_faults_count, state.faults.size),
                identity = {
                    if (state.displayPlate.isNotBlank()) {
                        AtelierPlate(plate = state.displayPlate, size = PlateSize.L)
                    }
                },
                status = if (state.syncState.pendingFaults > 0) {
                    {
                        AtelierChip(
                            text = stringResource(R.string.diag_faults_pending_sync, state.syncState.pendingFaults),
                            kind = ChipKind.Idle
                        )
                    }
                } else null
            )
        },
        bottomBar = {
            if (state.planIncludesTelemetry && (state.canScanFaults || state.canResolveFaults)) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            if (state.canScanFaults) {
                                AtelierButton(
                                    text = stringResource(R.string.diag_faults_scan),
                                    onClick = {
                                        haptics.toggle()
                                        onScanDtcs()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = state.isBluetoothConnected && !state.isScanning && !state.isClearing,
                                    loading = state.isScanning,
                                    icon = Icons.Outlined.Search
                                )
                            }
                            if (state.canResolveFaults && state.faults.isNotEmpty()) {
                                AtelierButton(
                                    text = stringResource(R.string.diag_faults_clear),
                                    onClick = {
                                        haptics.toggle()
                                        onOpenClearConfirm()
                                    },
                                    modifier = Modifier.weight(1f),
                                    style = AtelierButtonStyle.Destructive,
                                    enabled = state.isBluetoothConnected && !state.isScanning && !state.isClearing,
                                    icon = Icons.Outlined.DeleteOutline
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.planIncludesTelemetry -> AtelierPlanLockedState(
                    text = stringResource(R.string.diag_hub_plan_locked),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                !state.canReadFaults -> AtelierNoPermissionState(
                    text = stringResource(R.string.diag_faults_no_permission),
                    modifier = Modifier.padding(horizontal = Spacing.md)
                )
                else -> {
                    AtelierRefreshBar(visible = state.isRefreshing && state.faults.isNotEmpty())
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing && state.faults.isNotEmpty(),
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            if ((state.canScanFaults || state.canResolveFaults) && !state.isBluetoothConnected) {
                                item(key = "bt-hint") {
                                    AtelierBanner(
                                        message = stringResource(R.string.diag_faults_need_bt),
                                        icon = Icons.Outlined.Info,
                                        kind = BannerKind.Info
                                    )
                                }
                            }
                            state.notice?.let { notice ->
                                val isPersistent = state.lastClearResult?.isClean == false
                                item(key = "notice") {
                                    AtelierBanner(
                                        message = notice.asString(),
                                        icon = if (isPersistent) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle,
                                        kind = if (isPersistent) BannerKind.Wait else BannerKind.Ok
                                    )
                                }
                            }
                            state.error?.let { err ->
                                if (state.faults.isNotEmpty()) {
                                    item(key = "error-banner") {
                                        AtelierBanner(message = err.asString(), icon = Icons.Outlined.ErrorOutline)
                                    }
                                }
                            }
                            state.freezeFrame?.let { ff ->
                                item(key = "freeze-frame") {
                                    FreezeFrameCard(ff)
                                }
                            }
                            when {
                                showSkeleton -> items(3, key = { "bone-$it" }) {
                                    AtelierListRow(
                                        title = "P0304",
                                        subtitle = "Falla de encendido en el cilindro 4",
                                        loading = true
                                    )
                                }
                                state.faults.isEmpty() && state.error != null -> item(key = "error") {
                                    AtelierErrorState(message = state.error.asString(), onRetry = onRefresh)
                                }
                                state.faults.isEmpty() -> item(key = "empty") {
                                    AtelierEmptyState(text = stringResource(R.string.diag_faults_empty))
                                }
                                else -> items(state.faults, key = { it.id }) { fault ->
                                    FaultRow(fault)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.confirmClearOpen) {
        AtelierBottomSheet(
            title = stringResource(R.string.diag_faults_clear_title),
            onDismiss = onDismissClearConfirm,
            actions = {
                AtelierButton(
                    text = stringResource(R.string.diag_faults_clear_confirm),
                    onClick = {
                        haptics.confirm()
                        onConfirmClearDtcs()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.DestructiveFilled,
                    enabled = !state.isClearing,
                    loading = state.isClearing,
                    icon = Icons.Outlined.DeleteOutline
                )
                AtelierButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onDismissClearConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !state.isClearing
                )
            }
        ) {
            Text(
                text = stringResource(R.string.diag_faults_clear_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
        }
    }
}

@Composable
private fun FaultRow(fault: VehicleFault) {
    val catalogEntry = fault.dtc?.let { DtcCatalog.describe(it) }
    val severity = fault.severity ?: catalogEntry?.severity ?: FaultSeverity.MEDIUM
    val description = fault.description.ifBlank { catalogEntry?.description.orEmpty() }
    val timeText = fault.detectedAt?.formatLimaDateTime()

    AtelierSurface(
        modifier = Modifier.fillMaxWidth(),
        tier = if (severity == FaultSeverity.CRITICAL) SurfaceTier.Elevated else SurfaceTier.Card,
        shape = CardLargeShape,
        contentPadding = PaddingValues(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = fault.dtcCode,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (fault.isPending) {
                    AtelierChip(
                        text = stringResource(R.string.diag_faults_pending_badge),
                        kind = ChipKind.Idle
                    )
                }
                AtelierChip(
                    text = stringResource(severity.labelRes()),
                    kind = severity.chipKind()
                )
            }
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!timeText.isNullOrBlank()) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AtelierTheme.colors.inkSecondary
                )
            }
        }
    }
}

@Composable
private fun FreezeFrameCard(frame: FreezeFrame) {
    val parts = buildList {
        frame.dtc?.let { add(it.code) }
        frame.engineTempCelsius?.let { add("${it.roundToInt()} °C") }
        frame.engineRpm?.let { add("$it rpm") }
        frame.speedKmh?.let { add("$it km/h") }
    }.joinToString(" · ")
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AtelierSectionHeader(title = stringResource(R.string.diag_faults_freeze_header))
        AtelierSurface(
            modifier = Modifier.fillMaxWidth(),
            tier = SurfaceTier.Elevated,
            shape = CardLargeShape,
            contentPadding = PaddingValues(Spacing.md)
        ) {
            Text(
                text = parts.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// region Previews

private val previewFaultsState = VehicleFaultsUiState(
    vehicleId = "v1",
    plate = "BKL482",
    canReadFaults = true,
    canScanFaults = true,
    canResolveFaults = true,
    linkState = ObdLinkState.CONNECTED,
    isLoaded = true,
    freezeFrame = FreezeFrame(Dtc.of("P0304"), 102.0, 2200, 45),
    faults = listOf(
        VehicleFault(
            id = "local-1",
            vehicleId = "v1",
            dtc = Dtc.of("P0304"),
            dtcCode = "P0304",
            severity = FaultSeverity.CRITICAL,
            description = "Falla de encendido en el cilindro 4",
            detectedAt = Instant.parse("2026-10-24T14:00:00Z"),
            isResolved = false,
            resolvedAt = null
        ),
        VehicleFault(
            id = "f2",
            vehicleId = "v1",
            dtc = Dtc.of("P0420"),
            dtcCode = "P0420",
            severity = FaultSeverity.MEDIUM,
            description = "Eficiencia del catalizador bajo el umbral (banco 1)",
            detectedAt = Instant.parse("2026-10-24T13:30:00Z"),
            isResolved = false,
            resolvedAt = null
        )
    )
)

@Preview(name = "DTC · claro", widthDp = 390, heightDp = 844)
@Composable
private fun FaultsPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    VehicleFaultsContent(previewFaultsState, {}, {}, {}, {}, {}, {})
}

@Preview(name = "DTC · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FaultsPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    VehicleFaultsContent(previewFaultsState, {}, {}, {}, {}, {}, {})
}

// endregion
