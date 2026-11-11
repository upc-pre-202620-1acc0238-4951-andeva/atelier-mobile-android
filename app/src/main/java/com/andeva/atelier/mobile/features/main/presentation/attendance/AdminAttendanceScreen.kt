package com.andeva.atelier.mobile.features.main.presentation.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceId
import com.andeva.atelier.mobile.features.attendance.domain.AttendanceStatus
import com.andeva.atelier.mobile.features.attendance.domain.BranchAttendanceItem
import com.andeva.atelier.mobile.features.attendance.presentation.AttendanceScreen
import java.time.format.DateTimeFormatter

private val AttendanceTimeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(BusinessTime.ZONE)

@Composable
fun AdminAttendanceScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: AdminAttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        if (onBack != null) {
            AtelierTopBar(title = stringResource(R.string.admin_attendance_title), onBack = onBack)
        } else {
            Text(
                text = stringResource(R.string.admin_attendance_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        if (state.canAuditAll) {
            val tabs = AdminAttendanceTab.entries
            PrimaryTabRow(selectedTabIndex = tabs.indexOf(state.tab)) {
                tabs.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.onTabChange(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    AdminAttendanceTab.BRANCH_DAILY -> stringResource(
                                        R.string.admin_attendance_tab_branch,
                                        state.branchRecords.size
                                    )
                                    AdminAttendanceTab.JUSTIFICATIONS -> stringResource(
                                        R.string.admin_attendance_tab_justifications,
                                        state.pendingJustifications.size
                                    )
                                    AdminAttendanceTab.MY_ATTENDANCE -> stringResource(R.string.admin_attendance_tab_own)
                                },
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }

        when (state.tab) {
            AdminAttendanceTab.MY_ATTENDANCE -> Box(modifier = Modifier.weight(1f)) {
                AttendanceScreen()
            }
            AdminAttendanceTab.BRANCH_DAILY -> BranchDailyListSection(
                state = state,
                items = state.branchRecords,
                onRefresh = viewModel::loadBranchDaily,
                onReview = viewModel::onReviewJustification,
                modifier = Modifier.weight(1f)
            )
            AdminAttendanceTab.JUSTIFICATIONS -> BranchDailyListSection(
                state = state,
                items = state.pendingJustifications,
                onRefresh = viewModel::loadBranchDaily,
                onReview = viewModel::onReviewJustification,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BranchDailyListSection(
    state: AdminAttendanceUiState,
    items: List<BranchAttendanceItem>,
    onRefresh: () -> Unit,
    onReview: (AttendanceId, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.notice?.let {
            AtelierBanner(message = it.asString(), icon = Icons.Outlined.CheckCircle, kind = BannerKind.Ok)
        }
        state.error?.let {
            AtelierBanner(message = it.asString(), icon = Icons.Filled.Error)
        }

        when {
            state.isLoading -> Text(
                text = stringResource(R.string.search_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            items.isEmpty() -> AtelierBanner(
                message = stringResource(R.string.admin_attendance_empty),
                icon = Icons.Outlined.Info,
                kind = BannerKind.Info
            )
            else -> items.forEach { item ->
                BranchAttendanceCard(
                    item = item,
                    canApprove = state.canApproveJustifications,
                    isReviewing = state.reviewingId == item.attendance.id,
                    onApprove = { onReview(item.attendance.id, true) },
                    onReject = { onReview(item.attendance.id, false) }
                )
            }
        }

        AtelierButton(
            text = stringResource(R.string.bays_refresh),
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Text,
            loading = state.isLoading
        )
    }
}

@Composable
private fun BranchAttendanceCard(
    item: BranchAttendanceItem,
    canApprove: Boolean,
    isReviewing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val attendance = item.attendance
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.collaboratorLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            attendance.status?.let { status ->
                AtelierChip(
                    text = status.name,
                    kind = when (status) {
                        AttendanceStatus.ON_TIME, AttendanceStatus.EXCUSED -> ChipKind.Ok
                        AttendanceStatus.LATE -> ChipKind.Wait
                        AttendanceStatus.ABSENT -> ChipKind.Bad
                    }
                )
            }
        }
        val inTime = AttendanceTimeFormat.format(attendance.clockIn)
        val outTime = attendance.clockOut?.let { AttendanceTimeFormat.format(it) } ?: "En turno"
        Text(
            text = "$inTime → $outTime",
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )
        attendance.justificationReason?.let { reason ->
            Text(
                text = stringResource(R.string.admin_attendance_justification_line, reason),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (canApprove && attendance.justificationStatus == com.andeva.atelier.mobile.features.attendance.domain.JustificationStatus.PENDING) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AtelierButton(
                    text = stringResource(R.string.admin_attendance_approve_btn),
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    style = AtelierButtonStyle.Tonal,
                    enabled = !isReviewing,
                    loading = isReviewing
                )
                AtelierButton(
                    text = stringResource(R.string.admin_attendance_reject_btn),
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    style = AtelierButtonStyle.Outlined,
                    enabled = !isReviewing
                )
            }
        }
    }
}
