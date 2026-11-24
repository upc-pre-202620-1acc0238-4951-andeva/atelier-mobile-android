package com.andeva.atelier.mobile.features.workorders.presentation.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.presentation.DocumentIntents
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierDecisionRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFichaPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSectionHeader
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.bays.domain.WorkBay
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.Proposal
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.ProposalStatus
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.presentation.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.components.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.components.labelRes
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.AddTaskSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.AssignBaySheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.AssignMechanicSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.CancelOrderSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.DeleteTaskSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.EditLaborSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.OrderChargePartSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.OrderPartEditSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.PartMovementsSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.PdfPreviewSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.ProfitabilitySheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.QuotationDecisionSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.QuotationSection
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.RejectQuotationSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.SuspensionSheet
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.TaskBlock
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes
import java.io.File
import java.math.BigDecimal
import java.time.Instant

/**
 * Ficha de una orden de trabajo: la identidad (placa, cliente, estado, total) en el panel, lo que espera una
 * decisión en ámbar, el ciclo de vida de la orden, las tareas con su desglose (US22–23), las propuestas por
 * hallazgo (US14–15), las fotos de recepción (US13) y la proforma (US22–25).
 */
@Composable
fun WorkOrderDetailScreen(
    onBack: () -> Unit,
    onOpenTask: (workOrderId: String, taskId: String) -> Unit,
    onOpenProposals: (workOrderId: String) -> Unit,
    onOpenGallery: (workOrderId: String) -> Unit,
    onCompleteOrder: (vehicleId: String, customerId: String, workOrderId: String) -> Unit,
    onOpenCheckout: (workOrderId: String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenObd: (vehicleId: String, plate: String?) -> Unit = { _, _ -> },
    viewModel: WorkOrderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.quotation_share_chooser)
    val haptics = rememberAtelierHaptics()

    LaunchedEffect(state.notice) {
        if (state.notice != null) haptics.confirm()
    }
    LaunchedEffect(state.error) {
        if (state.error != null) haptics.reject()
    }

    // Al volver desde el detalle de una tarea recarga la orden para reflejar cambios en repuestos o estados.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (state.detail != null && !state.isLoading && !state.isBusy) {
            viewModel.load()
        }
    }

    WorkOrderDetailContent(
        state = state,
        actions = WorkOrderDetailActions(
            onBack = onBack,
            onRetry = viewModel::load,
            onCompleteOrder = onCompleteOrder,
            onOpenCheckout = { onOpenCheckout(viewModel.workOrderId) },
            onOpenObd = onOpenObd,
            onOpenTask = onOpenTask,
            onOpenProposals = { onOpenProposals(viewModel.workOrderId) },
            onOpenGallery = { onOpenGallery(viewModel.workOrderId) },
            onOpenAddTask = viewModel::onOpenAddTask,
            onDismissAddTask = viewModel::onDismissAddTask,
            onSelectTaskService = viewModel::onSelectTaskService,
            onAddTaskDescriptionChange = viewModel::onAddTaskDescriptionChange,
            onAddTaskPriceChange = viewModel::onAddTaskPriceChange,
            onConfirmAddTask = viewModel::onConfirmAddTask,
            onOpenEditLabor = viewModel::onOpenEditLabor,
            onDismissEditLabor = viewModel::onDismissEditLabor,
            onEditLaborPriceChange = viewModel::onEditLaborPriceChange,
            onConfirmEditLabor = viewModel::onConfirmEditLabor,
            onOpenDeleteTask = viewModel::onOpenDeleteTask,
            onDismissDeleteTask = viewModel::onDismissDeleteTask,
            onConfirmDeleteTask = viewModel::onConfirmDeleteTask,
            onOpenChargePart = viewModel::onOpenChargePart,
            onDismissChargePart = viewModel::onDismissChargePart,
            onChargeQueryChange = viewModel::onChargeQueryChange,
            onSelectPart = viewModel::onSelectPart,
            onChargeQuantityChange = viewModel::onChargeQuantityChange,
            onConfirmChargePart = viewModel::onConfirmChargePart,
            onOpenEditPart = viewModel::onOpenEditPart,
            onOpenReturnPart = viewModel::onOpenReturnPart,
            onDismissPartEdit = viewModel::onDismissPartEdit,
            onPartEditQuantityChange = viewModel::onPartEditQuantityChange,
            onConfirmPartEdit = viewModel::onConfirmPartEdit,
            onGenerateQuotationPdf = viewModel::onGenerateQuotationPdf,
            onSharePdf = { pdfFile ->
                val shared = shareOrOpenPdf(context, pdfFile, shareTitle)
                if (shared) viewModel.onPdfHandled() else viewModel.onPdfOpenFailed()
            },
            onDismissPdfPreview = viewModel::onPdfHandled,
            onSendQuotationEmail = viewModel::onSendQuotationEmail,
            onApproveQuotation = viewModel::onApproveQuotation,
            onOpenQuotationDecision = viewModel::onOpenQuotationDecision,
            onDismissQuotationDecision = viewModel::onDismissQuotationDecision,
            onOpenRejectQuotation = viewModel::onOpenRejectQuotation,
            onDismissRejectQuotation = viewModel::onDismissRejectQuotation,
            onRejectReasonChange = viewModel::onRejectReasonChange,
            onConfirmRejectQuotation = viewModel::onConfirmRejectQuotation,
            onOpenAssignBay = viewModel::onOpenAssignBay,
            onDismissAssignBay = viewModel::onDismissAssignBay,
            onAssignBay = viewModel::onAssignBay,
            onOpenAssignMechanic = viewModel::onOpenAssignMechanic,
            onDismissAssignMechanic = viewModel::onDismissAssignMechanic,
            onConfirmAssignMechanic = viewModel::onConfirmAssignMechanic,
            onSelectAddTaskMechanic = viewModel::onSelectAddTaskMechanic,
            onStartOrder = viewModel::onStartOrder,
            onCompleteWorkOrder = viewModel::onCompleteOrder,
            onOpenCancelOrder = viewModel::onOpenCancelOrder,
            onDismissCancelOrder = viewModel::onDismissCancelOrder,
            onCancelReasonChange = viewModel::onCancelReasonChange,
            onConfirmCancelOrder = viewModel::onConfirmCancelOrder,
            onOpenSuspension = viewModel::onOpenSuspension,
            onDismissSuspension = viewModel::onDismissSuspension,
            onApproveSuspension = viewModel::onApproveSuspension,
            onRejectSuspension = viewModel::onRejectSuspension,
            onResumeTask = viewModel::onResumeTask,
            onOpenProfitability = viewModel::onOpenProfitability,
            onDismissProfitability = viewModel::onDismissProfitability,
            onOpenMovements = viewModel::onOpenMovements,
            onDismissMovements = viewModel::onDismissMovements
        ),
        modifier = modifier
    )
}

internal class WorkOrderDetailActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onCompleteOrder: (String, String, String) -> Unit = { _, _, _ -> },
    val onOpenCheckout: () -> Unit = {},
    val onOpenObd: (vehicleId: String, plate: String?) -> Unit = { _, _ -> },
    val onOpenTask: (String, String) -> Unit = { _, _ -> },
    val onOpenProposals: () -> Unit = {},
    val onOpenGallery: () -> Unit = {},
    val onOpenAddTask: () -> Unit = {},
    val onDismissAddTask: () -> Unit = {},
    val onSelectTaskService: (ServiceOption) -> Unit = {},
    val onAddTaskDescriptionChange: (String) -> Unit = {},
    val onAddTaskPriceChange: (String) -> Unit = {},
    val onSelectAddTaskMechanic: (String?) -> Unit = {},
    val onConfirmAddTask: () -> Unit = {},
    val onOpenEditLabor: (TaskDetail) -> Unit = {},
    val onDismissEditLabor: () -> Unit = {},
    val onEditLaborPriceChange: (String) -> Unit = {},
    val onConfirmEditLabor: () -> Unit = {},
    val onOpenDeleteTask: (TaskDetail) -> Unit = {},
    val onDismissDeleteTask: () -> Unit = {},
    val onConfirmDeleteTask: () -> Unit = {},
    val onOpenChargePart: (TaskId) -> Unit = {},
    val onDismissChargePart: () -> Unit = {},
    val onChargeQueryChange: (String) -> Unit = {},
    val onSelectPart: (PartOption) -> Unit = {},
    val onChargeQuantityChange: (String) -> Unit = {},
    val onConfirmChargePart: () -> Unit = {},
    val onOpenEditPart: (TaskId, TaskProduct) -> Unit = { _, _ -> },
    val onOpenReturnPart: (TaskId, TaskProduct) -> Unit = { _, _ -> },
    val onDismissPartEdit: () -> Unit = {},
    val onPartEditQuantityChange: (String) -> Unit = {},
    val onConfirmPartEdit: () -> Unit = {},
    val onGenerateQuotationPdf: () -> Unit = {},
    val onSharePdf: (File) -> Unit = {},
    val onDismissPdfPreview: () -> Unit = {},
    val onSendQuotationEmail: () -> Unit = {},
    val onApproveQuotation: () -> Unit = {},
    val onOpenQuotationDecision: () -> Unit = {},
    val onDismissQuotationDecision: () -> Unit = {},
    val onOpenRejectQuotation: () -> Unit = {},
    val onDismissRejectQuotation: () -> Unit = {},
    val onRejectReasonChange: (String) -> Unit = {},
    val onConfirmRejectQuotation: () -> Unit = {},
    val onOpenAssignBay: () -> Unit = {},
    val onDismissAssignBay: () -> Unit = {},
    val onAssignBay: (WorkBay) -> Unit = {},
    val onOpenAssignMechanic: (TaskDetail) -> Unit = {},
    val onDismissAssignMechanic: () -> Unit = {},
    val onConfirmAssignMechanic: (MechanicOption) -> Unit = {},
    val onStartOrder: () -> Unit = {},
    val onCompleteWorkOrder: () -> Unit = {},
    val onOpenCancelOrder: () -> Unit = {},
    val onDismissCancelOrder: () -> Unit = {},
    val onCancelReasonChange: (String) -> Unit = {},
    val onConfirmCancelOrder: () -> Unit = {},
    val onOpenSuspension: (TaskDetail) -> Unit = {},
    val onDismissSuspension: () -> Unit = {},
    val onApproveSuspension: () -> Unit = {},
    val onRejectSuspension: () -> Unit = {},
    val onResumeTask: (TaskDetail) -> Unit = {},
    val onOpenProfitability: () -> Unit = {},
    val onDismissProfitability: () -> Unit = {},
    val onOpenMovements: () -> Unit = {},
    val onDismissMovements: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkOrderDetailContent(
    state: WorkOrderDetailUiState,
    actions: WorkOrderDetailActions,
    modifier: Modifier = Modifier
) {
    val detail = state.detail
    val order = detail?.workOrder
    val headerOrder = order ?: state.cachedOrder
    val showSkeleton = rememberDelayedLoading(state.isLoading && detail == null)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state.canCheckout) {
                AtelierFab(
                    text = stringResource(R.string.orders_checkout),
                    icon = Icons.Outlined.Payments,
                    onClick = actions.onOpenCheckout
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && detail != null,
            onRefresh = actions.onRetry,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .floatingBarPadding(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item(key = "panel") { OrderPanel(headerOrder, showSkeleton && headerOrder == null, actions.onBack) }
                item(key = "refresh") { AtelierRefreshBar(visible = state.isLoading && detail != null) }
                if (detail == null || order == null) {
                    emptyOrError(state, showSkeleton, actions.onRetry)
                } else {
                    orderContent(state, detail, order, actions)
                }
            }
        }
    }

    OrderSheets(state, actions)
}

@Composable
private fun OrderPanel(order: WorkOrder?, loading: Boolean, onBack: () -> Unit) {
    if (order == null) {
        AtelierFichaPanel(
            onBack = onBack,
            title = stringResource(R.string.order_detail_title),
            subtitle = if (loading) "Cargando ficha…" else null,
            identity = if (loading) {
                { AtelierPlate("BKR418", size = PlateSize.L, modifier = Modifier.bone(true)) }
            } else null
        )
        return
    }
    val label = order.numberLabel()
    AtelierFichaPanel(
        onBack = onBack,
        title = order.customerName ?: label,
        subtitle = listOfNotNull(
            label.takeIf { order.customerName != null },
            order.bayName,
            order.totalAmount?.let { stringResource(R.string.orders_total, it.format()) }
        ).joinToString(" · ").ifEmpty { null },
        identity = order.plate?.let { plate -> { AtelierPlate(plate, size = PlateSize.L) } },
        status = { AtelierChip(text = stringResource(order.status.labelRes()), kind = order.status.chipKind()) }
    )
}

private fun LazyListScope.emptyOrError(state: WorkOrderDetailUiState, showSkeleton: Boolean, onRetry: () -> Unit) {
    when {
        state.error != null -> item(key = "error") {
            if (state.isOnline) {
                AtelierErrorState(
                    message = state.error.asString(),
                    onRetry = onRetry,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                AtelierOfflineState(onRetry = onRetry, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        showSkeleton -> items(4, key = { "bone-$it" }) {
            AtelierListRow(title = "Tarea de la orden", subtitle = "Mecánico", loading = true)
        }
    }
}

private fun LazyListScope.orderContent(
    state: WorkOrderDetailUiState,
    detail: WorkOrderDetail,
    order: WorkOrder,
    actions: WorkOrderDetailActions
) {
    item(key = "banners") {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.notice?.let {
                AtelierBanner(message = it.asString(), icon = Icons.Outlined.CheckCircle, kind = BannerKind.Ok)
            }
            if (!state.hasOpenSheet) {
                state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
            }
            if (state.isDraft && (order.mileageIn == null || order.mileageIn == 0)) {
                AtelierBanner(
                    title = stringResource(R.string.order_intake_incomplete_title),
                    message = stringResource(R.string.order_intake_incomplete_body),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
                AtelierButton(
                    text = stringResource(R.string.order_complete_intake_btn),
                    onClick = { actions.onCompleteOrder(order.vehicleId, order.customerId, order.id) },
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Tonal
                )
            }
        }
    }
    decisions(state, detail, order, actions)
    lifecycle(state, actions)
    if (state.canOpenObd) {
        item(key = "obd-quick-action") {
            AtelierButton(
                text = stringResource(R.string.orders_obd_diagnostic_btn),
                onClick = { actions.onOpenObd(order.vehicleId, order.plate) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                style = AtelierButtonStyle.Outlined,
                icon = Icons.Outlined.BluetoothConnected
            )
        }
    }
    orderData(state, order, actions)
    tasks(state, detail, order, actions)
    proposals(state, detail, actions)
    photos(detail, actions)
    quotation(state, actions)
    costs(state, actions)
}

/** "Esperan tu decisión": suspensiones por validar, propuestas en revisión y la proforma sin respuesta. */
private fun LazyListScope.decisions(
    state: WorkOrderDetailUiState,
    detail: WorkOrderDetail,
    order: WorkOrder,
    actions: WorkOrderDetailActions
) {
    val suspensions = if (state.canValidateSuspensions) detail.tasksAwaitingSuspensionReview else emptyList()
    val pendingProposals = if (state.canResolveProposals) {
        detail.proposals.filter { it.status == ProposalStatus.PENDING_REVIEW }
    } else emptyList()
    val quotationPending = state.isDraft && state.canApproveQuotation
    val count = suspensions.size + pendingProposals.size + if (quotationPending) 1 else 0
    if (count == 0) return

    item(key = "decisions-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.orders_section_decisions),
            count = count,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    items(suspensions, key = { "susp-${it.id.value}" }) { task ->
        AtelierDecisionRow(
            title = stringResource(R.string.orders_decision_suspension, state.taskTitle(task)),
            subtitle = task.pendingSuspensionReason?.takeIf { it.isNotBlank() },
            plate = order.plate,
            onClick = { actions.onOpenSuspension(task) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    items(pendingProposals, key = { "prop-${it.id}" }) { proposal ->
        AtelierDecisionRow(
            title = stringResource(R.string.orders_decision_proposal, proposal.description),
            subtitle = proposal.severity?.let { stringResource(it.labelRes()) },
            plate = order.plate,
            onClick = actions.onOpenProposals,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    if (quotationPending) {
        item(key = "decision-quotation") {
            AtelierDecisionRow(
                title = stringResource(R.string.orders_quotation_decision_title),
                subtitle = stringResource(R.string.orders_quotation_decision_row_hint),
                plate = order.plate,
                amount = state.breakdown?.totalAmount?.format(),
                onClick = actions.onOpenQuotationDecision,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

/** Iniciar, completar y cancelar: en línea y por permiso. Sin red se apagan y se dice por qué. */
private fun LazyListScope.lifecycle(state: WorkOrderDetailUiState, actions: WorkOrderDetailActions) {
    val showStart = state.canUpdateWorkOrder && state.isDraft
    val showComplete = state.canUpdateWorkOrder && state.status == WorkOrderStatus.IN_PROGRESS
    val showCancel = state.canCancelWorkOrder && state.isEditableOrder
    if (!showStart && !showComplete && !showCancel) return
    item(key = "lifecycle") {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showStart) {
                AtelierButton(
                    text = stringResource(R.string.orders_action_start),
                    onClick = actions.onStartOrder,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canStartOrder,
                    loading = state.busyAction == WorkOrderDetailAction.START_ORDER
                )
            }
            if (showComplete) {
                AtelierButton(
                    text = stringResource(R.string.orders_action_complete),
                    onClick = actions.onCompleteWorkOrder,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canCompleteOrder,
                    loading = state.busyAction == WorkOrderDetailAction.COMPLETE_ORDER
                )
            }
            if (showCancel) {
                AtelierButton(
                    text = stringResource(R.string.orders_action_cancel),
                    onClick = actions.onOpenCancelOrder,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Destructive,
                    enabled = state.canCancelOrder
                )
            }
            if (!state.isOnline) AtelierRequiresConnection()
        }
    }
}

/** Bahía de trabajo, kilometraje y diagnóstico de ingreso. */
private fun LazyListScope.orderData(state: WorkOrderDetailUiState, order: WorkOrder, actions: WorkOrderDetailActions) {
    val canAssign = state.canAssignBay && state.isEditableOrder
    val mileage = order.mileageIn?.takeIf { it > 0 }
    val diagnostic = order.diagnosticSummary?.takeIf { it.isNotBlank() }
    val showBay = order.bayName != null || canAssign
    if (!showBay && mileage == null && diagnostic == null) return
    item(key = "data-header") {
        AtelierSectionHeader(title = stringResource(R.string.orders_section_data), modifier = Modifier.padding(horizontal = 16.dp))
    }
    item(key = "data-card") {
        AtelierSurface(
            tier = SurfaceTier.Card,
            shape = CardLargeShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        ) {
            Column {
                if (showBay) {
                    AtelierListRow(
                        title = order.bayName ?: stringResource(R.string.orders_bay_none),
                        subtitle = stringResource(R.string.orders_bay_label),
                        showDivider = mileage != null || diagnostic != null,
                        onClick = if (canAssign) actions.onOpenAssignBay else null,
                        trailing = if (canAssign) {
                            {
                                Text(
                                    text = stringResource(if (order.bayName != null) R.string.orders_bay_change else R.string.orders_bay_assign),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AtelierTheme.colors.primaryText
                                )
                            }
                        } else null
                    )
                }
                if (mileage != null) {
                    AtelierListRow(
                        title = stringResource(R.string.reception_vehicle_mileage_km, mileage),
                        subtitle = stringResource(R.string.orders_mileage_label),
                        showDivider = diagnostic != null
                    )
                }
                if (diagnostic != null) {
                    AtelierListRow(
                        title = diagnostic,
                        subtitle = stringResource(R.string.orders_diagnostic_label),
                        showDivider = false
                    )
                }
            }
        }
    }
}

private fun LazyListScope.tasks(
    state: WorkOrderDetailUiState,
    detail: WorkOrderDetail,
    order: WorkOrder,
    actions: WorkOrderDetailActions
) {
    item(key = "tasks-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.order_tasks_title),
            count = detail.tasks.size,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
    if (detail.tasks.isEmpty()) {
        item(key = "tasks-empty") {
            AtelierEmptyState(text = stringResource(R.string.order_tasks_empty), modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
    items(detail.tasks, key = { "task-${it.id.value}" }) { task ->
        AtelierSurface(
            tier = SurfaceTier.Card,
            shape = CardLargeShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        ) {
            TaskBlock(
                task = task,
                state = state,
                onOpenTask = { actions.onOpenTask(order.id, task.id.value) },
                onEditLabor = { actions.onOpenEditLabor(task) },
                onAddPart = { actions.onOpenChargePart(task.id) },
                onEditPart = { product -> actions.onOpenEditPart(task.id, product) },
                onReturnPart = { product -> actions.onOpenReturnPart(task.id, product) },
                onAssignMechanic = { actions.onOpenAssignMechanic(task) },
                onResume = { actions.onResumeTask(task) },
                onDeleteTask = { actions.onOpenDeleteTask(task) },
                showOuterDivider = false
            )
        }
    }
    if (state.canModifyTasks && state.services.isNotEmpty()) {
        item(key = "tasks-add") {
            AtelierButton(
                text = stringResource(R.string.order_add_task),
                onClick = actions.onOpenAddTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = AtelierButtonStyle.Outlined,
                icon = Icons.Outlined.Add
            )
        }
    }
}

private fun LazyListScope.proposals(state: WorkOrderDetailUiState, detail: WorkOrderDetail, actions: WorkOrderDetailActions) {
    item(key = "proposals-header") {
        AtelierSectionHeader(
            title = stringResource(R.string.orders_section_proposals),
            count = detail.proposals.size,
            modifier = Modifier.padding(start = 16.dp),
            action = {
                AtelierButton(
                    text = stringResource(R.string.orders_see_all),
                    onClick = actions.onOpenProposals,
                    style = AtelierButtonStyle.Text
                )
            }
        )
    }
    if (detail.proposals.isEmpty()) {
        item(key = "proposals-empty") {
            AtelierEmptyState(text = stringResource(R.string.proposals_empty), modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
    items(detail.proposals, key = { "proposal-${it.id}" }) { proposal ->
        // Las que esperan una decisión de quien puede resolverlas ya están en ámbar arriba.
        AtelierListRow(
            title = proposal.description,
            subtitle = proposal.severity?.let { stringResource(it.labelRes()) },
            onClick = actions.onOpenProposals,
            trailing = proposal.status?.let { status ->
                { AtelierChip(text = stringResource(status.labelRes()), kind = status.chipKind()) }
            }
        )
    }
}

private fun LazyListScope.photos(detail: WorkOrderDetail, actions: WorkOrderDetailActions) {
    item(key = "photos-header") {
        AtelierSectionHeader(title = stringResource(R.string.orders_section_photos), modifier = Modifier.padding(horizontal = 16.dp))
    }
    item(key = "photos-row") {
        AtelierSurface(
            tier = SurfaceTier.Card,
            shape = CardLargeShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        ) {
            AtelierListRow(
                title = stringResource(R.string.order_open_gallery),
                subtitle = pluralStringResource(R.plurals.orders_photos_count, detail.intakeImages.size, detail.intakeImages.size),
                showDivider = false,
                onClick = actions.onOpenGallery,
                trailing = { ChevronIcon() }
            )
        }
    }
}

private fun LazyListScope.quotation(state: WorkOrderDetailUiState, actions: WorkOrderDetailActions) {
    val breakdown = state.breakdown ?: return
    item(key = "quotation-header") {
        AtelierSectionHeader(title = stringResource(R.string.orders_section_quotation), modifier = Modifier.padding(horizontal = 16.dp))
    }
    item(key = "quotation") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QuotationSection(
                breakdown = breakdown,
                canGeneratePdf = state.canGeneratePdf,
                canEmailQuotation = state.canEmailQuotation,
                showPdfActions = state.canReadQuotation,
                showEmailAction = state.canSendQuotation,
                busyAction = state.busyAction,
                onGeneratePdf = actions.onGenerateQuotationPdf,
                onSendEmail = actions.onSendQuotationEmail
            )
            if (!state.isOnline && (state.canReadQuotation || state.canSendQuotation)) {
                AtelierRequiresConnection(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

/** Rentabilidad y movimientos de repuestos: se piden al abrir su hoja, no al abrir la ficha. */
private fun LazyListScope.costs(state: WorkOrderDetailUiState, actions: WorkOrderDetailActions) {
    if (!state.canReadProfitability && !state.canReadPartMovements) return
    item(key = "costs-header") {
        AtelierSectionHeader(title = stringResource(R.string.orders_section_costs), modifier = Modifier.padding(horizontal = 16.dp))
    }
    item(key = "costs-card") {
        AtelierSurface(
            tier = SurfaceTier.Card,
            shape = CardLargeShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        ) {
            Column {
                if (state.canReadProfitability) {
                    AtelierListRow(
                        title = stringResource(R.string.orders_profitability_title),
                        subtitle = stringResource(R.string.orders_profitability_hint),
                        showDivider = state.canReadPartMovements,
                        onClick = if (state.isOnline) actions.onOpenProfitability else null,
                        trailing = { ChevronIcon() }
                    )
                }
                if (state.canReadPartMovements) {
                    AtelierListRow(
                        title = stringResource(R.string.orders_movements_title),
                        subtitle = stringResource(R.string.orders_movements_hint),
                        showDivider = false,
                        onClick = if (state.isOnline) actions.onOpenMovements else null,
                        trailing = { ChevronIcon() }
                    )
                }
            }
        }
    }
    if (!state.isOnline) {
        item(key = "costs-offline") { AtelierRequiresConnection(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
    }
}

@Composable
private fun ChevronIcon() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = AtelierTheme.colors.inkSecondary
    )
}

@Composable
private fun OrderSheets(state: WorkOrderDetailUiState, actions: WorkOrderDetailActions) {
    val order = state.detail?.workOrder

    state.addTaskForm?.let { form ->
        AddTaskSheet(
            form = form,
            services = state.services,
            mechanics = state.availableMechanics,
            isSaving = state.busyAction == WorkOrderDetailAction.ADD_TASK,
            invalidField = state.invalidField,
            error = state.error,
            onSelectService = actions.onSelectTaskService,
            onDescriptionChange = actions.onAddTaskDescriptionChange,
            onPriceChange = actions.onAddTaskPriceChange,
            onSelectMechanic = actions.onSelectAddTaskMechanic,
            onConfirm = actions.onConfirmAddTask,
            onDismiss = actions.onDismissAddTask
        )
    }
    state.editLaborForm?.let { form ->
        EditLaborSheet(
            form = form,
            isSaving = state.busyAction == WorkOrderDetailAction.EDIT_LABOR,
            invalidField = state.invalidField,
            error = state.error,
            onPriceChange = actions.onEditLaborPriceChange,
            onConfirm = actions.onConfirmEditLabor,
            onDismiss = actions.onDismissEditLabor
        )
    }
    state.deletingTask?.let { task ->
        DeleteTaskSheet(
            taskTitle = state.taskTitle(task),
            hasProducts = task.products.isNotEmpty(),
            isSaving = state.busyAction == WorkOrderDetailAction.DELETE_TASK,
            error = state.error,
            onConfirm = actions.onConfirmDeleteTask,
            onDismiss = actions.onDismissDeleteTask
        )
    }
    state.chargeForm?.let { form ->
        OrderChargePartSheet(
            form = form,
            isSaving = state.busyAction == WorkOrderDetailAction.CHARGE_PART,
            invalidField = state.invalidField,
            error = state.error,
            onQueryChange = actions.onChargeQueryChange,
            onSelect = actions.onSelectPart,
            onQuantityChange = actions.onChargeQuantityChange,
            onConfirm = actions.onConfirmChargePart,
            onDismiss = actions.onDismissChargePart
        )
    }
    state.partEdit?.let { form ->
        OrderPartEditSheet(
            form = form,
            isSaving = state.busyAction == WorkOrderDetailAction.EDIT_PART ||
                state.busyAction == WorkOrderDetailAction.REMOVE_PART,
            invalidField = state.invalidField,
            error = state.error,
            onQuantityChange = actions.onPartEditQuantityChange,
            onConfirm = actions.onConfirmPartEdit,
            onDismiss = actions.onDismissPartEdit
        )
    }
    if (state.showQuotationDecision) {
        QuotationDecisionSheet(
            isApproving = state.busyAction == WorkOrderDetailAction.APPROVE_QUOTATION,
            isBusy = state.isBusy || !state.isOnline,
            isOnline = state.isOnline,
            error = state.error,
            onApprove = actions.onApproveQuotation,
            onReject = actions.onOpenRejectQuotation,
            onDismiss = actions.onDismissQuotationDecision
        )
    }
    state.rejectForm?.let { form ->
        RejectQuotationSheet(
            form = form,
            isSaving = state.busyAction == WorkOrderDetailAction.REJECT_QUOTATION,
            invalidField = state.invalidField,
            error = state.error,
            onReasonChange = actions.onRejectReasonChange,
            onConfirm = actions.onConfirmRejectQuotation,
            onDismiss = actions.onDismissRejectQuotation
        )
    }
    state.cancelForm?.let { form ->
        CancelOrderSheet(
            form = form,
            isSaving = state.busyAction == WorkOrderDetailAction.CANCEL_ORDER,
            invalidField = state.invalidField,
            error = state.error,
            onReasonChange = actions.onCancelReasonChange,
            onConfirm = actions.onConfirmCancelOrder,
            onDismiss = actions.onDismissCancelOrder
        )
    }
    state.reviewingSuspension?.let { task ->
        SuspensionSheet(
            taskTitle = state.taskTitle(task),
            reason = task.pendingSuspensionReason,
            missingItem = task.missingItemDescription,
            canDecide = state.canValidateSuspensions,
            isOnline = state.isOnline,
            busyApprove = state.busyAction == WorkOrderDetailAction.APPROVE_SUSPENSION,
            busyReject = state.busyAction == WorkOrderDetailAction.REJECT_SUSPENSION,
            error = state.error,
            onApprove = actions.onApproveSuspension,
            onReject = actions.onRejectSuspension,
            onDismiss = actions.onDismissSuspension
        )
    }
    state.generatedPdf?.let { pdfFile ->
        PdfPreviewSheet(
            pdfFile = pdfFile,
            breakdown = state.breakdown,
            onShare = { actions.onSharePdf(pdfFile) },
            onDismiss = actions.onDismissPdfPreview
        )
    }
    if (state.showAssignBayDialog) {
        AssignBaySheet(
            bays = state.availableBays,
            currentBayId = order?.currentBayId,
            isSaving = state.busyAction == WorkOrderDetailAction.ASSIGN_BAY,
            error = state.error,
            onSelectBay = actions.onAssignBay,
            onDismiss = actions.onDismissAssignBay
        )
    }
    state.assigningMechanicTask?.let { task ->
        AssignMechanicSheet(
            taskTitle = state.taskTitle(task),
            currentMechanicId = task.mechanicId,
            mechanics = state.availableMechanics,
            isSaving = state.busyAction == WorkOrderDetailAction.ASSIGN_MECHANIC,
            error = state.error,
            onSelectMechanic = actions.onConfirmAssignMechanic,
            onDismiss = actions.onDismissAssignMechanic
        )
    }
    state.profitability?.let { data ->
        ProfitabilitySheet(
            state = data,
            partName = { state.partNamesById[it] },
            onRetry = actions.onOpenProfitability,
            onDismiss = actions.onDismissProfitability
        )
    }
    state.movements?.let { data ->
        PartMovementsSheet(
            state = data,
            partName = { state.partNamesById[it] },
            onRetry = actions.onOpenMovements,
            onDismiss = actions.onDismissMovements
        )
    }
}

@Composable
private fun WorkOrder.numberLabel(): String =
    internalNumber?.let { stringResource(R.string.orders_order_number, it) } ?: stringResource(R.string.orders_no_number)

/**
 * US24 (docs/04-mobile-integration.md §8): abre o comparte el PDF descargado en `cacheDir/pdfs` mediante
 * [DocumentIntents] (`ACTION_VIEW` con fallback a `ACTION_SEND`).
 */
private fun shareOrOpenPdf(context: Context, pdfFile: File, chooserTitle: String): Boolean =
    DocumentIntents.openPdf(context, pdfFile, chooserTitle)

// region Previews

private fun previewTask(
    id: String,
    service: String,
    labor: String,
    status: TaskStatus,
    products: List<TaskProduct> = emptyList(),
    suspension: String? = null
) = TaskDetail(
    id = TaskId(id),
    workOrderId = "wo-1042",
    serviceId = null,
    serviceName = service,
    mechanicId = "m1",
    mechanicName = "Andrés Villanueva",
    status = status,
    description = service,
    price = Money.of(labor, Money.PEN),
    holdReason = null,
    missingItemDescription = suspension?.let { "Pastillas de freno delanteras" },
    totalPausedSeconds = 0,
    startedAt = null,
    completedAt = null,
    pendingSuspensionReason = suspension,
    pendingSuspensionRequestedAt = suspension?.let { Instant.parse("2026-11-16T15:10:00Z") },
    products = products,
    evidenceImages = emptyList()
)

private val previewDetail = WorkOrderDetail(
    workOrder = WorkOrder(
        id = "wo-1042", internalNumber = 1042, branchId = "b1", vehicleId = "v1", customerId = "c1",
        currentBayId = "bay2", bayName = "Bahía 2", status = WorkOrderStatus.IN_PROGRESS,
        diagnosticSummary = "Ruido al frenar y vibración en el volante.", mileageIn = 84500,
        subtotal = Money.of("510.00", Money.PEN), taxAmount = Money.of("91.80", Money.PEN),
        totalAmount = Money.of("601.80", Money.PEN), plate = "BKR418", customerName = "Rosa Quispe Mamani"
    ),
    tasks = listOf(
        previewTask(
            "t1", "Cambio de pastillas de freno", "180.00", TaskStatus.ON_HOLD,
            products = listOf(
                TaskProduct("tp1", "p1", "Pastillas delanteras Bosch", BigDecimal("1"), Money.of("210.00", Money.PEN), Money.of("210.00", Money.PEN))
            ),
            suspension = "Llegaron pastillas de otra medida; el proveedor trae las correctas mañana."
        ),
        previewTask("t2", "Alineamiento y balanceo", "120.00", TaskStatus.ASSIGNED)
    ),
    proposals = listOf(
        Proposal(
            id = "pr1", workOrderId = "wo-1042", taskId = null, serviceId = null, mechanicId = "m1",
            description = "Discos de freno con desgaste bajo el mínimo", severity = ProposalSeverity.CRITICAL,
            imageUrl = null, status = ProposalStatus.PENDING_REVIEW, customerNotes = null,
            createdAt = Instant.parse("2026-11-16T14:20:00Z")
        )
    ),
    intakeImages = emptyList(),
    createdAt = null,
    updatedAt = null
)

private val previewState = WorkOrderDetailUiState(
    detail = previewDetail,
    isLoading = false,
    services = listOf(ServiceOption("s1", "Cambio de pastillas de freno", Money.of("180.00", Money.PEN), 90)),
    canUpdateWorkOrder = true,
    canChargeParts = true,
    canReadQuotation = true,
    canSendQuotation = true,
    canApproveQuotation = true,
    canCancelWorkOrder = true,
    canResolveProposals = true,
    canValidateSuspensions = true,
    canReadProfitability = true,
    canReadPartMovements = true,
    canOpenObd = true,
    canReassignMechanic = true
)

@Composable
private fun PreviewContent(state: WorkOrderDetailUiState) {
    WorkOrderDetailContent(state = state, actions = WorkOrderDetailActions())
}

@Preview(name = "Ficha de orden · claro", widthDp = 390, heightDp = 844)
@Composable
private fun WorkOrderDetailPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Ficha de orden · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WorkOrderDetailPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Ficha de orden · completada", widthDp = 390, heightDp = 844)
@Composable
private fun WorkOrderDetailPreviewCompleted() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(
        previewState.copy(
            detail = previewDetail.copy(workOrder = previewDetail.workOrder.copy(status = WorkOrderStatus.COMPLETED)),
            hasCheckoutPermission = true
        )
    )
}

@Preview(name = "Ficha de orden · cargando", widthDp = 390, heightDp = 844)
@Composable
private fun WorkOrderDetailPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(WorkOrderDetailUiState(isLoading = true))
}

// endregion
