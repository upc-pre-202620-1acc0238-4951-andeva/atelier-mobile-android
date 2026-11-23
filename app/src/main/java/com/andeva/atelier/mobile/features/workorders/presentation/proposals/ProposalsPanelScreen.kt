package com.andeva.atelier.mobile.features.workorders.presentation.proposals

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierDecisionRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.features.workorders.domain.Proposal
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.ProposalStatus
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.presentation.components.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.components.labelRes
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.PickRow
import java.time.Instant

/**
 * US15: panel de propuestas por hallazgo. Las que esperan una decisión (`proposals:resolve`) salen en ámbar y se
 * deciden en una hoja inferior: notificar al cliente, registrar su aprobación o su rechazo. Todo en línea.
 */
@Composable
fun ProposalsPanelScreen(
    onBack: () -> Unit,
    onReportFinding: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProposalsPanelViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProposalsPanelContent(
        state = state,
        actions = ProposalsPanelActions(
            onBack = onBack,
            onReportFinding = onReportFinding,
            onRetry = viewModel::load,
            onNotify = viewModel::onNotify,
            onOpenForm = viewModel::onOpenForm,
            onCloseForm = viewModel::onCloseForm,
            onServiceChange = viewModel::onServiceChange,
            onFinalPriceChange = viewModel::onFinalPriceChange,
            onLaborHoursChange = viewModel::onLaborHoursChange,
            onNotesChange = viewModel::onNotesChange,
            onConfirm = viewModel::onConfirm
        ),
        modifier = modifier
    )
}

internal class ProposalsPanelActions(
    val onBack: () -> Unit,
    val onReportFinding: () -> Unit,
    val onRetry: () -> Unit,
    val onNotify: (String) -> Unit,
    val onOpenForm: (String, ProposalAction) -> Unit,
    val onCloseForm: () -> Unit,
    val onServiceChange: (String) -> Unit,
    val onFinalPriceChange: (String) -> Unit,
    val onLaborHoursChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onConfirm: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProposalsPanelContent(
    state: ProposalsPanelUiState,
    actions: ProposalsPanelActions,
    modifier: Modifier = Modifier
) {
    // La hoja abierta sobre una propuesta; se cierra sola cuando la propuesta deja de estar en revisión.
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = state.proposals.firstOrNull { it.id == selectedId && it.status == ProposalStatus.PENDING_REVIEW }
    val showSkeleton = rememberDelayedLoading(state.isLoading && state.proposals.isEmpty())
    val canDecideAnything = state.canNotify || state.canResolve

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AtelierTopBar(title = stringResource(R.string.proposals_title), onBack = actions.onBack) },
        floatingActionButton = {
            if (state.canReport) {
                AtelierFab(
                    text = stringResource(R.string.proposals_report),
                    icon = Icons.Outlined.Report,
                    onClick = actions.onReportFinding
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.proposals.isNotEmpty(),
            onRefresh = actions.onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                item(key = "refresh") { AtelierRefreshBar(visible = state.isLoading && state.proposals.isNotEmpty()) }
                state.notice?.let { notice ->
                    item(key = "notice") {
                        AtelierBanner(
                            message = notice.asString(),
                            icon = Icons.Outlined.CheckCircle,
                            kind = BannerKind.Ok,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                when {
                    showSkeleton -> items(3, key = { "bone-$it" }) {
                        AtelierListRow(title = "Hallazgo en el foso", subtitle = "Severidad", loading = true)
                    }
                    state.isLoading && state.proposals.isEmpty() -> Unit
                    state.proposals.isEmpty() && state.error != null -> item(key = "error") {
                        AtelierErrorState(
                            message = state.error.asString(),
                            onRetry = actions.onRetry,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    state.proposals.isEmpty() -> item(key = "empty") {
                        AtelierEmptyState(
                            text = stringResource(R.string.proposals_empty),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    else -> {
                        val pending = state.proposals.filter { it.status == ProposalStatus.PENDING_REVIEW }
                        val resolved = state.proposals - pending.toSet()
                        if (pending.isNotEmpty()) {
                            item(key = "pending-header") {
                                AtelierSectionHeader(
                                    title = stringResource(R.string.orders_section_decisions),
                                    count = pending.size,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            items(pending, key = { "pending-${it.id}" }) { proposal ->
                                val severity = proposal.severity?.let { stringResource(it.labelRes()) }
                                if (state.canResolve) {
                                    AtelierDecisionRow(
                                        title = proposal.description,
                                        subtitle = severity,
                                        onClick = { selectedId = proposal.id },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                } else {
                                    AtelierListRow(
                                        title = proposal.description,
                                        subtitle = severity,
                                        onClick = if (canDecideAnything) ({ selectedId = proposal.id }) else null,
                                        trailing = {
                                            AtelierChip(
                                                text = stringResource(ProposalStatus.PENDING_REVIEW.labelRes()),
                                                kind = ProposalStatus.PENDING_REVIEW.chipKind()
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        if (resolved.isNotEmpty()) {
                            item(key = "resolved-header") {
                                AtelierSectionHeader(
                                    title = stringResource(R.string.orders_proposals_resolved),
                                    count = resolved.size,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            items(resolved, key = { "resolved-${it.id}" }) { proposal ->
                                AtelierListRow(
                                    title = proposal.description,
                                    subtitle = listOfNotNull(
                                        proposal.severity?.let { stringResource(it.labelRes()) },
                                        proposal.customerNotes?.let { stringResource(R.string.proposals_customer_notes, it) }
                                    ).joinToString(" · ").ifEmpty { null },
                                    trailing = proposal.status?.let { status ->
                                        { AtelierChip(text = stringResource(status.labelRes()), kind = status.chipKind()) }
                                    }
                                )
                            }
                        }
                        state.error?.takeIf { state.form == null }?.let { error ->
                            item(key = "error-banner") {
                                AtelierBanner(
                                    message = error.asString(),
                                    icon = Icons.Outlined.ErrorOutline,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { proposal ->
        ProposalSheet(
            proposal = proposal,
            state = state,
            actions = actions,
            onDismiss = {
                actions.onCloseForm()
                selectedId = null
            }
        )
    }
}

@Composable
private fun ProposalSheet(
    proposal: Proposal,
    state: ProposalsPanelUiState,
    actions: ProposalsPanelActions,
    onDismiss: () -> Unit
) {
    val form = state.form?.takeIf { it.proposalId == proposal.id }
    val busy = state.busyProposalId == proposal.id
    val canAct = state.isOnline && !state.isBusy
    AtelierBottomSheet(
        title = stringResource(R.string.orders_proposal_sheet_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            if (form != null) {
                AtelierButton(
                    text = stringResource(R.string.proposals_confirm),
                    onClick = actions.onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canConfirm,
                    loading = busy
                )
                AtelierButton(
                    text = stringResource(R.string.proposals_cancel),
                    onClick = actions.onCloseForm,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !state.isBusy
                )
            } else {
                if (state.canResolve) {
                    AtelierButton(
                        text = stringResource(R.string.proposals_approve),
                        onClick = { actions.onOpenForm(proposal.id, ProposalAction.APPROVE) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canAct
                    )
                    AtelierButton(
                        text = stringResource(R.string.proposals_reject),
                        onClick = { actions.onOpenForm(proposal.id, ProposalAction.REJECT) },
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Destructive,
                        enabled = canAct
                    )
                }
                if (state.canNotify) {
                    AtelierButton(
                        text = stringResource(R.string.proposals_notify),
                        onClick = { actions.onNotify(proposal.id) },
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Tonal,
                        enabled = canAct,
                        loading = busy
                    )
                }
            }
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            proposal.severity?.let { AtelierChip(text = stringResource(it.labelRes()), kind = it.chipKind()) }
        }
        Text(
            text = proposal.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        proposal.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = stringResource(R.string.finding_photo),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop
            )
        }
        state.notice?.let {
            AtelierBanner(message = it.asString(), icon = Icons.Outlined.CheckCircle, kind = BannerKind.Ok)
        }
        if (form != null) {
            ProposalFormFields(form, state, actions)
        } else {
            state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
        }
    }
}

@Composable
private fun ProposalFormFields(form: ProposalForm, state: ProposalsPanelUiState, actions: ProposalsPanelActions) {
    val approve = form.action == ProposalAction.APPROVE
    val saving = state.isBusy
    if (approve) {
        Text(
            text = stringResource(R.string.proposals_service),
            style = MaterialTheme.typography.labelLarge,
            color = AtelierTheme.colors.inkSecondary
        )
        androidx.compose.foundation.layout.Column {
            state.services.forEach { service ->
                PickRow(
                    title = service.name,
                    selected = form.serviceId == service.id,
                    enabled = !saving,
                    onClick = { actions.onServiceChange(service.id) }
                )
            }
        }
        if (state.invalidField == "serviceId") {
            Text(
                text = stringResource(R.string.proposals_field_service),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.bad
            )
        }
        AtelierTextField(
            label = stringResource(R.string.proposals_final_price),
            value = form.finalPrice,
            onValueChange = actions.onFinalPriceChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            isError = state.invalidField == "finalPrice",
            errorText = if (state.invalidField == "finalPrice") stringResource(R.string.proposals_field_amount) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
        )
        AtelierTextField(
            label = stringResource(R.string.proposals_labor_hours),
            value = form.laborHours,
            onValueChange = actions.onLaborHoursChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            isError = state.invalidField == "laborHours",
            errorText = if (state.invalidField == "laborHours") stringResource(R.string.proposals_field_amount) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
        )
    }
    AtelierTextField(
        label = stringResource(if (approve) R.string.proposals_notes_optional else R.string.proposals_customer_notes_label),
        value = form.notes,
        onValueChange = actions.onNotesChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = !saving,
        isError = state.invalidField == "notes" || state.invalidField == "customerNotes",
        errorText = if (state.invalidField == "notes" || state.invalidField == "customerNotes") {
            stringResource(R.string.proposals_field_notes)
        } else null
    )
    state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
}

// region Previews

private fun previewProposal(id: String, description: String, status: ProposalStatus, severity: ProposalSeverity, notes: String? = null) =
    Proposal(id, "wo1", null, "s1", "m1", description, severity, null, status, notes, Instant.parse("2026-11-16T14:20:00Z"))

private val previewActions = ProposalsPanelActions({}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

private val previewState = ProposalsPanelUiState(
    isLoading = false, canNotify = true, canResolve = true, canReport = true,
    proposals = listOf(
        previewProposal("p1", "Discos de freno con desgaste bajo el mínimo", ProposalStatus.PENDING_REVIEW, ProposalSeverity.CRITICAL),
        previewProposal("p2", "Fuga de aceite en el cárter", ProposalStatus.REJECTED, ProposalSeverity.MEDIUM, "Es muy caro, lo ve el sábado."),
        previewProposal("p3", "Bujías con electrodo gastado", ProposalStatus.APPROVED, ProposalSeverity.LOW)
    ),
    services = listOf(ServiceOption("s1", "Cambio de discos de freno", null, 90)),
    notice = UiText.Dynamic("Correo enviado a rosa.quispe@correo.pe.")
)

@Preview(name = "Propuestas · claro", widthDp = 390, heightDp = 844)
@Composable
private fun ProposalsPanelPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    ProposalsPanelContent(state = previewState, actions = previewActions)
}

@Preview(name = "Propuestas · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProposalsPanelPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    ProposalsPanelContent(state = previewState, actions = previewActions)
}

@Preview(name = "Propuestas · sin conexión", widthDp = 390, heightDp = 844)
@Composable
private fun ProposalsPanelPreviewOffline() = AtelierWorkshopTheme(darkTheme = false) {
    ProposalsPanelContent(state = previewState.copy(isOnline = false, notice = null), actions = previewActions)
}

// endregion
