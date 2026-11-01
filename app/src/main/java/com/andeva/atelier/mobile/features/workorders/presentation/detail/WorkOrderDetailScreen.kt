package com.andeva.atelier.mobile.features.workorders.presentation.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.tasks.domain.PartOption
import com.andeva.atelier.mobile.features.tasks.domain.TaskDetail
import com.andeva.atelier.mobile.features.tasks.domain.TaskId
import com.andeva.atelier.mobile.features.tasks.domain.TaskProduct
import com.andeva.atelier.mobile.features.tasks.domain.TaskStatus
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.ChargePartDialog
import com.andeva.atelier.mobile.features.tasks.presentation.detail.components.PartEditDialog
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.presentation.chipKind
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.AddTaskDialog
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.CustomerResolutionSection
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.EditLaborDialog
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.QuotationCascadeFooter
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.RejectQuotationDialog
import com.andeva.atelier.mobile.features.workorders.presentation.detail.components.TaskCascadeCard
import com.andeva.atelier.mobile.features.workorders.presentation.labelRes
import java.io.File
import java.math.BigDecimal

/**
 * Detalle y proforma comercial de una orden de trabajo:
 * - **US22**: desglose en cascada por tarea (Mano de obra + repuestos) y pie contable (`Subtotal`, `IGV 18 %`, `Total`).
 * - **US23 / US33**: imputación, ajuste de cantidad y retiro de repuestos por tarea.
 * - **US24**: generación y compartición digital de proforma en PDF (`FileProvider`) y envío por correo.
 * - **US25**: formalización de la decisión del cliente (`Aceptó. Iniciar trabajos` / `Rechazó. Cancelar orden`).
 */
@Composable
fun WorkOrderDetailScreen(
    onBack: () -> Unit,
    onOpenTask: (workOrderId: String, taskId: String) -> Unit,
    onOpenProposals: (workOrderId: String) -> Unit = {},
    onOpenGallery: (workOrderId: String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: WorkOrderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.quotation_share_chooser)

    // Al volver desde el detalle de una tarea recarga la orden para reflejar cambios en repuestos o estados.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (state.detail != null && !state.isLoading && !state.isBusy) {
            viewModel.load()
        }
    }

    LaunchedEffect(state.generatedPdf) {
        val pdfFile = state.generatedPdf ?: return@LaunchedEffect
        val shared = shareOrOpenPdf(context, pdfFile, shareTitle)
        if (shared) viewModel.onPdfHandled() else viewModel.onPdfOpenFailed()
    }

    WorkOrderDetailContent(
        state = state,
        actions = WorkOrderDetailActions(
            onBack = onBack,
            onRetry = viewModel::load,
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
            onSendQuotationEmail = viewModel::onSendQuotationEmail,
            onApproveQuotation = viewModel::onApproveQuotation,
            onOpenRejectQuotation = viewModel::onOpenRejectQuotation,
            onDismissRejectQuotation = viewModel::onDismissRejectQuotation,
            onRejectReasonChange = viewModel::onRejectReasonChange,
            onConfirmRejectQuotation = viewModel::onConfirmRejectQuotation
        ),
        modifier = modifier
    )
}

internal class WorkOrderDetailActions(
    val onBack: () -> Unit,
    val onRetry: () -> Unit,
    val onOpenTask: (String, String) -> Unit,
    val onOpenProposals: () -> Unit = {},
    val onOpenGallery: () -> Unit = {},
    val onOpenAddTask: () -> Unit = {},
    val onDismissAddTask: () -> Unit = {},
    val onSelectTaskService: (ServiceOption) -> Unit = {},
    val onAddTaskDescriptionChange: (String) -> Unit = {},
    val onAddTaskPriceChange: (String) -> Unit = {},
    val onConfirmAddTask: () -> Unit = {},
    val onOpenEditLabor: (TaskDetail) -> Unit = {},
    val onDismissEditLabor: () -> Unit = {},
    val onEditLaborPriceChange: (String) -> Unit = {},
    val onConfirmEditLabor: () -> Unit = {},
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
    val onSendQuotationEmail: () -> Unit = {},
    val onApproveQuotation: () -> Unit = {},
    val onOpenRejectQuotation: () -> Unit = {},
    val onDismissRejectQuotation: () -> Unit = {},
    val onRejectReasonChange: (String) -> Unit = {},
    val onConfirmRejectQuotation: () -> Unit = {}
)

@Composable
internal fun WorkOrderDetailContent(
    state: WorkOrderDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenTask: (String, String) -> Unit,
    modifier: Modifier = Modifier
) = WorkOrderDetailContent(
    state = state,
    actions = WorkOrderDetailActions(onBack = onBack, onRetry = onRetry, onOpenTask = onOpenTask),
    modifier = modifier
)

@Composable
internal fun WorkOrderDetailContent(
    state: WorkOrderDetailUiState,
    actions: WorkOrderDetailActions,
    modifier: Modifier = Modifier
) {
    val detail = state.detail
    val order = detail?.workOrder
    val topTitle = order?.internalNumber?.let { stringResource(R.string.order_code_format, it) }
        ?: stringResource(R.string.order_detail_title)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = topTitle, onBack = actions.onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (detail != null && order != null) {
                OrderHeaderSection(order = order)

                state.notice?.let {
                    AtelierBanner(message = it.asString(), icon = Icons.Outlined.CheckCircle, kind = BannerKind.Ok)
                }

                val hasOpenDialog = state.addTaskForm != null || state.editLaborForm != null ||
                    state.chargeForm != null || state.partEdit != null || state.rejectForm != null
                if (!hasOpenDialog) {
                    state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
                }

                Text(
                    text = stringResource(R.string.order_tasks_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (detail.tasks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.order_tasks_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AtelierTheme.colors.inkSecondary
                    )
                }
                detail.tasks.forEach { task ->
                    TaskCascadeCard(
                        task = task,
                        state = state,
                        bayName = order.bayName,
                        onOpenTask = { actions.onOpenTask(order.id, task.id.value) },
                        onEditLabor = { actions.onOpenEditLabor(task) },
                        onAddPart = { actions.onOpenChargePart(task.id) },
                        onEditPart = { product -> actions.onOpenEditPart(task.id, product) },
                        onReturnPart = { product -> actions.onOpenReturnPart(task.id, product) }
                    )
                }

                if (state.canModifyTasks && state.services.isNotEmpty()) {
                    AtelierButton(
                        text = stringResource(R.string.order_add_task),
                        onClick = actions.onOpenAddTask,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Outlined
                    )
                }

                state.breakdown?.let { breakdown ->
                    QuotationCascadeFooter(
                        breakdown = breakdown,
                        canGeneratePdf = state.canGeneratePdf,
                        canEmailQuotation = state.canEmailQuotation,
                        busyAction = state.busyAction,
                        onGeneratePdf = actions.onGenerateQuotationPdf,
                        onSendEmail = actions.onSendQuotationEmail
                    )
                }

                if (state.isDraft && state.canApproveQuotation) {
                    CustomerResolutionSection(
                        canResolve = state.canResolveQuotation,
                        busyAction = state.busyAction,
                        onApprove = actions.onApproveQuotation,
                        onReject = actions.onOpenRejectQuotation
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AtelierButton(
                        text = stringResource(R.string.order_open_proposals),
                        onClick = actions.onOpenProposals,
                        modifier = Modifier.weight(1f),
                        style = AtelierButtonStyle.Text
                    )
                    AtelierButton(
                        text = stringResource(R.string.order_open_gallery),
                        onClick = actions.onOpenGallery,
                        modifier = Modifier.weight(1f),
                        style = AtelierButtonStyle.Text
                    )
                }
            } else if (state.isLoading) {
                Text(
                    text = stringResource(R.string.search_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
            } else {
                state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
                AtelierButton(
                    text = stringResource(R.string.branch_retry),
                    onClick = actions.onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined
                )
            }
        }
    }

    state.addTaskForm?.let { form ->
        AddTaskDialog(
            form = form,
            services = state.services,
            isSaving = state.busyAction == WorkOrderDetailAction.ADD_TASK,
            invalidField = state.invalidField,
            error = state.error,
            onSelectService = actions.onSelectTaskService,
            onDescriptionChange = actions.onAddTaskDescriptionChange,
            onPriceChange = actions.onAddTaskPriceChange,
            onConfirm = actions.onConfirmAddTask,
            onDismiss = actions.onDismissAddTask
        )
    }

    state.editLaborForm?.let { form ->
        EditLaborDialog(
            form = form,
            isSaving = state.busyAction == WorkOrderDetailAction.EDIT_LABOR,
            invalidField = state.invalidField,
            error = state.error,
            onPriceChange = actions.onEditLaborPriceChange,
            onConfirm = actions.onConfirmEditLabor,
            onDismiss = actions.onDismissEditLabor
        )
    }

    state.chargeForm?.let { form ->
        ChargePartDialog(
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
        val isSaving = state.busyAction == WorkOrderDetailAction.EDIT_PART ||
            state.busyAction == WorkOrderDetailAction.REMOVE_PART
        PartEditDialog(
            form = form,
            isSaving = isSaving,
            invalidField = state.invalidField,
            error = state.error,
            onQuantityChange = actions.onPartEditQuantityChange,
            onConfirm = actions.onConfirmPartEdit,
            onDismiss = actions.onDismissPartEdit
        )
    }

    state.rejectForm?.let { form ->
        RejectQuotationDialog(
            form = form,
            isSaving = state.busyAction == WorkOrderDetailAction.REJECT_QUOTATION,
            invalidField = state.invalidField,
            error = state.error,
            onReasonChange = actions.onRejectReasonChange,
            onConfirm = actions.onConfirmRejectQuotation,
            onDismiss = actions.onDismissRejectQuotation
        )
    }
}

@Composable
private fun OrderHeaderSection(order: WorkOrder) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AtelierChip(text = stringResource(order.status.labelRes()), kind = order.status.chipKind())
        }
        val subtitle = listOfNotNull(order.customerName, order.plate).joinToString(" · ")
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = AtelierTheme.colors.inkSecondary
            )
        }
    }
}

/**
 * US24 (docs/04-mobile-integration.md §8): comparte el PDF descargado en `cacheDir/pdfs` mediante el menú nativo de
 * Android (WhatsApp, correo, etc.) usando `FileProvider`.
 */
private fun shareOrOpenPdf(context: Context, pdfFile: File, chooserTitle: String): Boolean = try {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: IllegalArgumentException) {
    false
}

private val previewTask1 = TaskDetail(
    id = TaskId("t1"),
    workOrderId = "wo-424",
    serviceId = "s1",
    serviceName = "Afinamiento mayor",
    mechanicId = "m1",
    mechanicName = "Andrés V.",
    status = TaskStatus.ASSIGNED,
    description = "Afinamiento mayor",
    price = Money(BigDecimal("180.00"), Money.PEN),
    holdReason = null,
    missingItemDescription = null,
    totalPausedSeconds = 0,
    startedAt = null,
    completedAt = null,
    pendingSuspensionReason = null,
    pendingSuspensionRequestedAt = null,
    products = listOf(
        TaskProduct(
            id = "tp1",
            productId = "p1",
            productName = "Bujías NGK",
            quantity = BigDecimal("4"),
            unitPrice = Money(BigDecimal("24.00"), Money.PEN),
            totalAmount = Money(BigDecimal("96.00"), Money.PEN)
        ),
        TaskProduct(
            id = "tp2",
            productId = "p2",
            productName = "Filtro de aire",
            quantity = BigDecimal("1"),
            unitPrice = Money(BigDecimal("35.00"), Money.PEN),
            totalAmount = Money(BigDecimal("35.00"), Money.PEN)
        )
    ),
    evidenceImages = emptyList()
)

private val previewTask2 = TaskDetail(
    id = TaskId("t2"),
    workOrderId = "wo-424",
    serviceId = "s2",
    serviceName = "Diagnóstico de frenos",
    mechanicId = "m1",
    mechanicName = "Andrés V.",
    status = TaskStatus.ASSIGNED,
    description = "Diagnóstico de frenos",
    price = Money(BigDecimal("60.00"), Money.PEN),
    holdReason = null,
    missingItemDescription = null,
    totalPausedSeconds = 0,
    startedAt = null,
    completedAt = null,
    pendingSuspensionReason = null,
    pendingSuspensionRequestedAt = null,
    products = listOf(
        TaskProduct(
            id = "tp3",
            productId = "p3",
            productName = "Aceite 5W-30",
            quantity = BigDecimal("4"),
            unitPrice = Money(BigDecimal("35.00"), Money.PEN),
            totalAmount = Money(BigDecimal("140.00"), Money.PEN)
        )
    ),
    evidenceImages = emptyList()
)

private val previewDetail = WorkOrderDetail(
    workOrder = WorkOrder(
        id = "wo-424",
        internalNumber = 424,
        branchId = "b1",
        vehicleId = "v1",
        customerId = "c1",
        currentBayId = "bay2",
        bayName = "Bahía 2",
        status = WorkOrderStatus.DRAFT,
        diagnosticSummary = null,
        mileageIn = 45000,
        subtotal = Money(BigDecimal("511.00"), Money.PEN),
        taxAmount = Money(BigDecimal("91.98"), Money.PEN),
        totalAmount = Money(BigDecimal("602.98"), Money.PEN),
        plate = "ABC-123",
        customerName = "Toyota Hilux"
    ),
    tasks = listOf(previewTask1, previewTask2),
    proposals = emptyList(),
    intakeImages = emptyList(),
    createdAt = null,
    updatedAt = null
)

@Preview(name = "Proforma · desglose y resolución · oscuro", widthDp = 360, heightDp = 1100, showBackground = true)
@Composable
private fun WorkOrderDetailPreview() = AtelierWorkshopTheme(darkTheme = true) {
    WorkOrderDetailContent(
        state = WorkOrderDetailUiState(
            isLoading = false,
            detail = previewDetail,
            services = listOf(
                ServiceOption("s1", "Afinamiento mayor", Money(BigDecimal("180.00"), Money.PEN), 120),
                ServiceOption("s2", "Diagnóstico de frenos", Money(BigDecimal("60.00"), Money.PEN), 45)
            ),
            canUpdateWorkOrder = true,
            canChargeParts = true,
            canReadQuotation = true,
            canSendQuotation = true,
            canApproveQuotation = true
        ),
        onBack = {},
        onRetry = {},
        onOpenTask = { _, _ -> }
    )
}
