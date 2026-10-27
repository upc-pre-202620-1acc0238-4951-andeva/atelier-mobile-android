package com.andeva.atelier.mobile.features.workorders.presentation.proposals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.features.workorders.domain.Proposal
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.ProposalStatus
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.presentation.components.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.components.labelRes

/** US15: panel del asesor para notificar al cliente las propuestas y registrar su aprobación o rechazo. */
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

@Composable
internal fun ProposalsPanelContent(
    state: ProposalsPanelUiState,
    actions: ProposalsPanelActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.proposals_title), onBack = actions.onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.proposals_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
            state.notice?.let {
                AtelierBanner(message = it.asString(), icon = Icons.Outlined.CheckCircle, kind = BannerKind.Ok)
            }
            // El error del formulario se muestra dentro de su tarjeta; aquí solo el de carga o el de un aviso.
            if (state.form == null) {
                state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
            }

            when {
                state.isLoading -> Text(
                    text = stringResource(R.string.search_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
                state.proposals.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.proposals_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AtelierTheme.colors.inkSecondary
                    )
                    if (state.error != null) {
                        AtelierButton(
                            text = stringResource(R.string.branch_retry),
                            onClick = actions.onRetry,
                            modifier = Modifier.fillMaxWidth(),
                            style = AtelierButtonStyle.Text
                        )
                    }
                }
                else -> state.proposals.forEach { proposal -> ProposalCard(proposal, state, actions) }
            }

            if (state.canReport) {
                AtelierButton(
                    text = stringResource(R.string.proposals_report),
                    onClick = actions.onReportFinding,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined
                )
            }
        }
    }
}

@Composable
private fun ProposalCard(proposal: Proposal, state: ProposalsPanelUiState, actions: ProposalsPanelActions) {
    val form = state.form?.takeIf { it.proposalId == proposal.id }
    val busy = state.busyProposalId == proposal.id
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            proposal.severity?.let { AtelierChip(text = stringResource(it.labelRes()), kind = it.chipKind()) }
            proposal.status?.let {
                AtelierChip(
                    text = stringResource(it.labelRes()),
                    kind = it.chipKind()
                )
            }
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
                    .clip(CardShape),
                contentScale = ContentScale.Crop
            )
        }
        proposal.customerNotes?.let {
            Text(
                text = stringResource(R.string.proposals_customer_notes, it),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )
        }

        if (proposal.status == ProposalStatus.PENDING_REVIEW) {
            if (form != null) {
                ProposalFormFields(form, state, actions)
            } else {
                ProposalButtons(proposal, state, busy, actions)
            }
        }
    }
}

@Composable
private fun ProposalButtons(proposal: Proposal, state: ProposalsPanelUiState, busy: Boolean, actions: ProposalsPanelActions) {
    if (state.canNotify) {
        AtelierButton(
            text = stringResource(R.string.proposals_notify),
            onClick = { actions.onNotify(proposal.id) },
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Tonal,
            enabled = !state.isBusy,
            loading = busy
        )
    }
    if (state.canResolve) {
        AtelierButton(
            text = stringResource(R.string.proposals_approve),
            onClick = { actions.onOpenForm(proposal.id, ProposalAction.APPROVE) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isBusy
        )
        AtelierButton(
            text = stringResource(R.string.proposals_reject),
            onClick = { actions.onOpenForm(proposal.id, ProposalAction.REJECT) },
            modifier = Modifier.fillMaxWidth(),
            style = AtelierButtonStyle.Outlined,
            enabled = !state.isBusy
        )
    }
    if (!state.canNotify && !state.canResolve) {
        AtelierBanner(
            message = stringResource(R.string.reception_no_permission),
            icon = Icons.Outlined.Info,
            kind = BannerKind.Wait
        )
    }
}

@Composable
private fun ProposalFormFields(form: ProposalForm, state: ProposalsPanelUiState, actions: ProposalsPanelActions) {
    val approve = form.action == ProposalAction.APPROVE
    val saving = state.isBusy
    if (approve) {
        Text(
            text = stringResource(R.string.proposals_service),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        state.services.forEach { service ->
            AtelierButton(
                text = service.name,
                onClick = { actions.onServiceChange(service.id) },
                modifier = Modifier.fillMaxWidth(),
                style = if (form.serviceId == service.id) AtelierButtonStyle.Tonal else AtelierButtonStyle.Outlined,
                enabled = !saving
            )
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
    state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
    AtelierButton(
        text = stringResource(R.string.proposals_confirm),
        onClick = actions.onConfirm,
        modifier = Modifier.fillMaxWidth(),
        enabled = state.canConfirm,
        loading = saving
    )
    AtelierButton(
        text = stringResource(R.string.proposals_cancel),
        onClick = actions.onCloseForm,
        modifier = Modifier.fillMaxWidth(),
        style = AtelierButtonStyle.Text,
        enabled = !saving
    )
}

private fun previewProposal(id: String, status: ProposalStatus, notes: String? = null) = Proposal(
    id, "wo1", null, "s1", "m1", "Fuga de aceite en el cárter", ProposalSeverity.CRITICAL, null, status, notes, null
)

private val previewActions = ProposalsPanelActions({}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

@Preview(name = "Propuestas · lista · oscuro", widthDp = 360, heightDp = 1000, showBackground = true)
@Composable
private fun ProposalsPanelPreviewList() = AtelierWorkshopTheme(darkTheme = true) {
    ProposalsPanelContent(
        state = ProposalsPanelUiState(
            isLoading = false, canNotify = true, canResolve = true, canReport = true,
            proposals = listOf(
                previewProposal("p1", ProposalStatus.PENDING_REVIEW),
                previewProposal("p2", ProposalStatus.REJECTED, "Es muy caro")
            ),
            notice = UiText.Dynamic("Correo enviado a ana@correo.pe.")
        ),
        actions = previewActions
    )
}

@Preview(name = "Propuestas · aprobar · claro", widthDp = 360, heightDp = 1000, showBackground = true)
@Composable
private fun ProposalsPanelPreviewApprove() = AtelierWorkshopTheme(darkTheme = false) {
    ProposalsPanelContent(
        state = ProposalsPanelUiState(
            isLoading = false, canNotify = true, canResolve = true,
            proposals = listOf(previewProposal("p1", ProposalStatus.PENDING_REVIEW)),
            services = listOf(ServiceOption("s1", "Cambio de empaque", null, 90)),
            form = ProposalForm("p1", ProposalAction.APPROVE, serviceId = "s1", finalPrice = "120", laborHours = "1.5")
        ),
        actions = previewActions
    )
}
