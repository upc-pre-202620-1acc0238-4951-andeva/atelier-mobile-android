package com.andeva.atelier.mobile.features.main.presentation.attendance

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierDecisionRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMainTopBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierReportPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSegmentedControl
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.LocalFloatingBarInset
import com.andeva.atelier.mobile.core.designsystem.component.PanelFigure
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.presentation.formatLimaDate
import com.andeva.atelier.mobile.features.attendance.domain.Attendance
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.BranchAttendanceItem
import com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus
import com.andeva.atelier.mobile.features.attendance.presentation.AttendanceScreen
import com.andeva.atelier.mobile.features.team.domain.TeamAttendance
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ContextDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-PE"))
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(BusinessTime.ZONE)

/**
 * "Equipo": raíz de pestaña para quien audita la asistencia de la sede. Dice cuántos están en turno, lista a
 * cada colaborador del día y reúne los descargos que esperan decisión. Quien además marca su propia asistencia
 * tiene una segunda pestaña, "Mi asistencia".
 */
@Composable
fun AdminAttendanceScreen(
    onBack: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenSync: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: AdminAttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AdminAttendanceContent(
        state = state,
        onBack = onBack,
        onOpenNotifications = onOpenNotifications,
        onOpenSync = onOpenSync,
        onOpenProfile = onOpenProfile,
        onTabChange = viewModel::onTabChange,
        onRefresh = viewModel::refresh,
        onOpenJustification = viewModel::onOpenJustification,
        onDismissJustification = viewModel::onDismissJustification,
        onReviewJustification = viewModel::onReviewJustification,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminAttendanceContent(
    state: AdminAttendanceUiState,
    onBack: (() -> Unit)?,
    onOpenNotifications: (() -> Unit)? = null,
    onOpenSync: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onTabChange: (TeamTab) -> Unit,
    onRefresh: () -> Unit,
    onOpenJustification: (TeamAttendance) -> Unit,
    onDismissJustification: () -> Unit,
    onReviewJustification: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // La decisión se confirma en la mano además de en la fila.
    val haptics = rememberAtelierHaptics()
    LaunchedEffect(state.resolved.size) {
        if (state.resolved.isNotEmpty()) haptics.confirm()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (onBack != null) {
                AtelierTopBar(title = stringResource(R.string.team_title), onBack = onBack)
            } else {
                AtelierMainTopBar(
                    title = stringResource(R.string.team_title),
                    onOpenNotifications = onOpenNotifications,
                    onOpenSync = onOpenSync,
                    onOpenProfile = onOpenProfile
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.canAuditAll && !state.canClockIn -> {
                    AtelierNoPermissionState(
                        text = stringResource(R.string.team_no_permission),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                state.tab == TeamTab.MINE || !state.canAuditAll -> {
                    if (state.hasTabs) TeamTabs(state.tab, onTabChange)
                    Box(modifier = Modifier.weight(1f)) { AttendanceScreen() }
                }
                else -> TeamList(
                    state = state,
                    onTabChange = onTabChange,
                    onRefresh = onRefresh,
                    onOpenJustification = onOpenJustification
                )
            }
        }
    }

    state.reviewing?.let { item ->
        JustificationSheet(
            item = item,
            who = state.labelFor(item),
            state = state,
            onDismiss = onDismissJustification,
            onReview = onReviewJustification
        )
    }
}

@Composable
private fun TeamTabs(selected: TeamTab, onTabChange: (TeamTab) -> Unit) {
    val teamLabel = stringResource(R.string.team_tab_team)
    val mineLabel = stringResource(R.string.team_tab_mine)
    AtelierSegmentedControl(
        options = TeamTab.entries,
        selected = selected,
        onSelect = onTabChange,
        label = { tab -> if (tab == TeamTab.TEAM) teamLabel else mineLabel },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamList(
    state: AdminAttendanceUiState,
    onTabChange: (TeamTab) -> Unit,
    onRefresh: () -> Unit,
    onOpenJustification: (TeamAttendance) -> Unit
) {
    val showSkeleton = rememberDelayedLoading(state.showSkeleton)
    PullToRefreshBox(
        isRefreshing = state.isRefreshing && state.hasLoaded,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalFloatingBarInset.current)) {
            item(key = "panel") { TeamPanel(state, showSkeleton) }
            item(key = "refresh") { AtelierRefreshBar(visible = state.isRefreshing && state.hasLoaded) }
            if (state.hasTabs) item(key = "tabs") { TeamTabs(state.tab, onTabChange) }
            if (!state.isOnline) {
                item(key = "offline") {
                    AtelierBanner(
                        message = stringResource(R.string.team_offline),
                        icon = Icons.Outlined.Info,
                        kind = BannerKind.Info,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    )
                }
            }
            val error = state.error
            if (error != null && state.records.isNotEmpty()) {
                item(key = "error") {
                    AtelierBanner(
                        message = error.asString(),
                        icon = Icons.Outlined.ErrorOutline,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    )
                }
            }
            decisions(state, onOpenJustification)
            item(key = "today-header") {
                AtelierSectionHeader(
                    title = stringResource(R.string.team_today),
                    count = state.records.size.takeIf { state.hasLoaded },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
                )
            }
            when {
                showSkeleton -> items(4, key = { "bone-$it" }) {
                    AtelierListRow(title = "Colaborador", subtitle = "Entrada", loading = true)
                }
                state.showSkeleton -> Unit
                state.records.isEmpty() && error != null && !state.isOnline -> item(key = "offline-state") {
                    AtelierOfflineState(modifier = Modifier.padding(horizontal = 16.dp), onRetry = onRefresh)
                }
                state.records.isEmpty() && error != null -> item(key = "error-state") {
                    AtelierErrorState(
                        message = error.asString(),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onRetry = onRefresh
                    )
                }
                state.records.isEmpty() -> item(key = "empty") {
                    AtelierEmptyState(
                        text = stringResource(R.string.team_empty),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                else -> items(state.records, key = { it.attendance.id.value }) { record -> AttendanceRow(record) }
            }
        }
    }
}

@Composable
private fun TeamPanel(state: AdminAttendanceUiState, loading: Boolean) {
    val date = ContextDateFormatter.format(state.today)
    AtelierReportPanel(
        context = state.branchName?.let { stringResource(R.string.today_context, it, date) } ?: date,
        legend = stringResource(R.string.team_legend, state.onShiftCount, state.records.size),
        caption = stringResource(R.string.team_caption),
        figures = listOf(
            PanelFigure(state.onTimeCount.toString(), stringResource(R.string.team_figure_on_time)),
            PanelFigure(state.lateCount.toString(), stringResource(R.string.team_figure_late)),
            PanelFigure(state.absentCount.toString(), stringResource(R.string.team_figure_absent))
        ),
        loading = loading
    )
}

/** Descargos por revisar. Sin nada pendiente (y sin consulta parcial), la sección no existe. */
private fun androidx.compose.foundation.lazy.LazyListScope.decisions(
    state: AdminAttendanceUiState,
    onOpenJustification: (TeamAttendance) -> Unit
) {
    if (state.justifications.isEmpty() && state.failedJustificationDays == 0) return
    item(key = "decisions-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.today_decisions),
            count = state.justifications.count { it.id !in state.resolved },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
        )
    }
    items(state.justifications, key = { "just-${it.id}" }) { item ->
        val approved = state.resolved[item.id]
        AtelierDecisionRow(
            title = stringResource(item.status.justificationTitleRes()),
            subtitle = item.whoAndWhen(state.labelFor(item)),
            resolved = approved?.let {
                stringResource(if (it) R.string.today_justification_approved else R.string.today_justification_rejected)
            },
            onClick = { onOpenJustification(item) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    if (state.failedJustificationDays > 0) {
        item(key = "decisions-partial") {
            AtelierBanner(
                message = pluralStringResource(
                    R.plurals.today_justifications_partial, state.failedJustificationDays, state.failedJustificationDays
                ),
                icon = Icons.Outlined.Info,
                kind = BannerKind.Wait,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun AttendanceRow(record: BranchAttendanceItem) {
    val attendance = record.attendance
    val subtitle = when {
        attendance.status == AttendanceStatus.ABSENT -> stringResource(R.string.team_no_clock_in)
        attendance.clockOut != null -> stringResource(
            R.string.team_clock_range, TimeFormatter.format(attendance.clockIn), TimeFormatter.format(attendance.clockOut)
        )
        else -> stringResource(R.string.team_clock_in_open, TimeFormatter.format(attendance.clockIn))
    }
    AtelierListRow(
        title = record.collaboratorLabel,
        subtitle = subtitle,
        trailing = attendance.status?.let { status ->
            { AtelierChip(text = stringResource(status.labelRes()), kind = status.chipKind()) }
        }
    )
}

@Composable
private fun JustificationSheet(
    item: TeamAttendance,
    who: String?,
    state: AdminAttendanceUiState,
    onDismiss: () -> Unit,
    onReview: (Boolean) -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(item.status.justificationTitleRes()),
        onDismiss = onDismiss,
        actions = {
            when {
                !state.canApproveJustifications ->
                    AtelierNoPermissionState(stringResource(R.string.today_justification_no_permission))
                !state.isOnline -> AtelierRequiresConnection()
                else -> {
                    AtelierButton(
                        text = stringResource(R.string.today_justification_approve),
                        onClick = { onReview(true) },
                        loading = state.isSubmittingReview
                    )
                    AtelierButton(
                        text = stringResource(R.string.today_justification_reject),
                        onClick = { onReview(false) },
                        style = AtelierButtonStyle.Destructive,
                        enabled = !state.isSubmittingReview
                    )
                }
            }
        }
    ) {
        Text(
            text = item.whoAndWhen(who),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
        Text(
            text = item.justificationReason?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.today_justification_no_reason),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        state.reviewError?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

/** El backend no siempre envía el nombre (H36): se usa el de la asistencia del día o, si no, el cargo. */
@Composable
private fun TeamAttendance.whoAndWhen(label: String?): String =
    listOfNotNull(
        label ?: jobTitle ?: stringResource(R.string.today_collaborator_fallback),
        clockIn?.formatLimaDate()
    ).joinToString(" · ")

private fun AttendanceStatus?.justificationTitleRes(): Int = when (this) {
    AttendanceStatus.LATE -> R.string.today_decision_justification_late
    AttendanceStatus.ABSENT -> R.string.today_decision_justification_absent
    else -> R.string.today_decision_justification
}

private fun AttendanceStatus.labelRes(): Int = when (this) {
    AttendanceStatus.ON_TIME -> R.string.team_status_on_time
    AttendanceStatus.LATE -> R.string.team_status_late
    AttendanceStatus.ABSENT -> R.string.team_status_absent
    AttendanceStatus.EXCUSED -> R.string.team_status_excused
}

private fun AttendanceStatus.chipKind(): ChipKind = when (this) {
    AttendanceStatus.ON_TIME, AttendanceStatus.EXCUSED -> ChipKind.Ok
    AttendanceStatus.LATE -> ChipKind.Warn
    AttendanceStatus.ABSENT -> ChipKind.Bad
}

// region Previews

private fun previewRecord(
    id: String, name: String, status: AttendanceStatus, clockIn: String, clockOut: String? = null
) = BranchAttendanceItem(
    attendance = Attendance(
        id = AttendanceId(id), branchId = "b1", shiftId = "s1", clockIn = Instant.parse(clockIn),
        clockOut = clockOut?.let { Instant.parse(it) }, status = status, distanceToBranchMeters = 14.0,
        workedMinutes = null, regularMinutes = null, overtimeMinutes = null,
        justificationReason = null, justificationStatus = null
    ),
    membershipId = "m$id",
    collaboratorLabel = name
)

private fun previewJustification() = TeamAttendance(
    id = "a3", membershipId = "m3", branchId = "b1", shiftId = "s1",
    clockIn = Instant.parse("2026-11-16T13:42:00Z"), clockOut = null, status = AttendanceStatus.LATE,
    distanceToBranchMeters = 12.0, workedMinutes = null, overtimeMinutes = null,
    justificationReason = "Paro de transporte en la Panamericana Sur; adjunté la constancia.",
    justificationStatus = JustificationStatus.PENDING, reviewedBy = null, reviewedAt = null, jobTitle = "Mecánico"
)

private val previewState = AdminAttendanceUiState(
    today = LocalDate.of(2026, 11, 16),
    branchName = "Sede Surco",
    records = listOf(
        previewRecord("1", "Andrés Villanueva", AttendanceStatus.ON_TIME, "2026-11-16T12:58:00Z"),
        previewRecord("2", "Marcela Ccori", AttendanceStatus.ON_TIME, "2026-11-16T13:01:00Z", "2026-11-16T22:05:00Z"),
        previewRecord("3", "Jhon Paredes", AttendanceStatus.LATE, "2026-11-16T13:42:00Z"),
        previewRecord("4", "Lucía Torres", AttendanceStatus.ABSENT, "2026-11-16T13:00:00Z", "2026-11-16T13:00:00Z")
    ),
    justifications = listOf(previewJustification()),
    failedJustificationDays = 1,
    hasLoaded = true,
    canAuditAll = true,
    canApproveJustifications = true
)

@Composable
private fun PreviewContent(state: AdminAttendanceUiState) {
    AdminAttendanceContent(
        state = state,
        onBack = null,
        onTabChange = {},
        onRefresh = {},
        onOpenJustification = {},
        onDismissJustification = {},
        onReviewJustification = {}
    )
}

@Preview(name = "Equipo · claro", widthDp = 390, heightDp = 844)
@Composable
private fun AdminAttendancePreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Equipo · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdminAttendancePreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Equipo · cargando", widthDp = 390, heightDp = 844)
@Composable
private fun AdminAttendancePreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(AdminAttendanceUiState(today = LocalDate.of(2026, 11, 16), canAuditAll = true))
}

// endregion
