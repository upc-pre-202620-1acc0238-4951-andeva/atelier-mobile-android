package com.andeva.atelier.mobile.features.workorders.presentation.services

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.format
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption

/** Catálogo de servicios estándar del taller con alta, edición y eliminación (`SERVICES_MANAGE`). */
@Composable
fun ServicesCatalogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServicesCatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ServicesCatalogContent(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::load,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onOpenCreate = viewModel::onOpenCreate,
        onOpenEdit = viewModel::onOpenEdit,
        onDismissForm = viewModel::onDismissForm,
        onNameChange = viewModel::onNameChange,
        onBasePriceChange = viewModel::onBasePriceChange,
        onEstimatedMinutesChange = viewModel::onEstimatedMinutesChange,
        onConfirmForm = viewModel::onConfirmForm,
        onOpenDelete = viewModel::onOpenDelete,
        onDismissDelete = viewModel::onDismissDelete,
        onConfirmDelete = viewModel::onConfirmDelete,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServicesCatalogContent(
    state: ServicesCatalogUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenCreate: () -> Unit,
    onOpenEdit: (ServiceOption) -> Unit,
    onDismissForm: () -> Unit,
    onNameChange: (String) -> Unit,
    onBasePriceChange: (String) -> Unit,
    onEstimatedMinutesChange: (String) -> Unit,
    onConfirmForm: () -> Unit,
    onOpenDelete: (ServiceOption) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val delayedLoading = rememberDelayedLoading(state.services.isEmpty() && state.isLoading)
    val showSkeleton = state.services.isEmpty() && state.isLoading && (LocalInspectionMode.current || delayedLoading)

    Scaffold(
        modifier = modifier.navigationBarsPadding().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state.canCreate) {
                AtelierFab(
                    text = stringResource(R.string.services_add),
                    icon = Icons.Outlined.Add,
                    onClick = onOpenCreate
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AtelierTopBar(
                title = stringResource(R.string.services_title),
                onBack = onBack
            )
            AtelierRefreshBar(visible = state.isLoading && state.services.isNotEmpty())

            if (!state.canRead && !state.isLoading) {
                AtelierNoPermissionState(
                    text = stringResource(R.string.services_readonly_hint),
                    modifier = Modifier.padding(Spacing.md)
                )
                return@Column
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading && state.services.isNotEmpty(),
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.md,
                        end = Spacing.md,
                        top = Spacing.sm,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    item(key = "subtitle") {
                        Text(
                            text = stringResource(R.string.services_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AtelierTheme.colors.inkSecondary
                        )
                    }

                    if (!state.isOnline) {
                        item(key = "offline-banner") {
                            AtelierRequiresConnection()
                        }
                    }

                    if (!state.canManage && state.canRead) {
                        item(key = "readonly-banner") {
                            AtelierBanner(
                                message = stringResource(R.string.services_readonly_hint),
                                icon = Icons.Outlined.Info,
                                kind = BannerKind.Info
                            )
                        }
                    }

                    state.notice?.let { notice ->
                        item(key = "notice") {
                            AtelierBanner(
                                message = notice.asString(),
                                icon = Icons.Outlined.CheckCircle,
                                kind = BannerKind.Ok
                            )
                        }
                    }

                    if (state.error != null && state.form == null && state.deletingService == null && state.services.isNotEmpty()) {
                        item(key = "error-banner") {
                            AtelierBanner(
                                message = state.error.asString(),
                                icon = Icons.Outlined.ErrorOutline
                            )
                        }
                    }

                    if (state.services.isNotEmpty()) {
                        item(key = "search") {
                            AtelierTextField(
                                label = "",
                                placeholder = stringResource(R.string.services_search_hint),
                                value = state.searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = Icons.Outlined.Search,
                                trailing = if (state.searchQuery.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(Icons.Outlined.Close, contentDescription = null)
                                        }
                                    }
                                } else null,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                            )
                        }
                    }

                    when {
                        showSkeleton -> {
                            items(4, key = { "skeleton-$it" }) {
                                ServiceSkeletonCard()
                            }
                        }
                        state.services.isEmpty() && !state.isLoading && !state.isOnline -> {
                            item(key = "offline-empty") {
                                AtelierOfflineState(onRetry = onRefresh)
                            }
                        }
                        state.services.isEmpty() && state.error != null && !state.isLoading -> {
                            item(key = "error-empty") {
                                AtelierErrorState(
                                    message = state.error.asString(),
                                    onRetry = onRefresh
                                )
                            }
                        }
                        state.services.isEmpty() && !state.isLoading -> {
                            item(key = "empty") {
                                AtelierEmptyState(
                                    text = stringResource(R.string.services_empty),
                                    actionLabel = if (state.canCreate) stringResource(R.string.services_add) else null,
                                    onAction = if (state.canCreate) onOpenCreate else null
                                )
                            }
                        }
                        state.filteredServices.isEmpty() -> {
                            item(key = "empty-search") {
                                AtelierEmptyState(text = stringResource(R.string.services_empty_search))
                            }
                        }
                        else -> {
                            items(state.filteredServices, key = { it.id }) { service ->
                                ServiceCatalogCard(
                                    service = service,
                                    canManage = state.canManage && state.isOnline,
                                    onClick = { onOpenEdit(service) },
                                    onDelete = { onOpenDelete(service) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    state.form?.let { form ->
        val editingService = form.serviceId?.let { id -> state.services.firstOrNull { it.id == id } }
        ServiceEditorSheet(
            form = form,
            isSaving = state.isSaving,
            canConfirm = state.canConfirmForm,
            invalidField = state.invalidField,
            error = state.error,
            onNameChange = onNameChange,
            onBasePriceChange = onBasePriceChange,
            onEstimatedMinutesChange = onEstimatedMinutesChange,
            onConfirm = onConfirmForm,
            onDelete = editingService?.let { svc -> { onOpenDelete(svc) } },
            onDismiss = onDismissForm
        )
    }

    state.deletingService?.let { service ->
        DeleteServiceSheet(
            serviceName = service.name,
            isSaving = state.isSaving,
            error = state.error,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete
        )
    }
}

@Composable
private fun ServiceCatalogCard(
    service: ServiceOption,
    canManage: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val subtitle = service.estimatedMinutes?.let { stringResource(R.string.services_duration_min, it) }
    AtelierSurface(
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        AtelierListRow(
            title = service.name,
            subtitle = subtitle,
            showDivider = false,
            onClick = if (canManage) onClick else null,
            trailing = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    service.basePrice?.let { price ->
                        Text(
                            text = price.format(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                    if (canManage) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.services_delete_action),
                                tint = AtelierTheme.colors.bad
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun ServiceSkeletonCard() {
    AtelierSurface(
        tier = SurfaceTier.Card,
        shape = CardLargeShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Box(modifier = Modifier.fillMaxWidth(0.68f).height(18.dp).bone(loading = true))
            Box(modifier = Modifier.fillMaxWidth(0.32f).height(14.dp).bone(loading = true))
        }
    }
}

@Composable
private fun ServiceEditorSheet(
    form: ServiceEditorForm,
    isSaving: Boolean,
    canConfirm: Boolean,
    invalidField: String?,
    error: com.andeva.atelier.mobile.core.designsystem.text.UiText?,
    onNameChange: (String) -> Unit,
    onBasePriceChange: (String) -> Unit,
    onEstimatedMinutesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(if (form.isEditing) R.string.services_edit_title else R.string.services_new_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(if (form.isEditing) R.string.services_save_edit else R.string.services_save_create),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = canConfirm,
                loading = isSaving
            )
            if (onDelete != null) {
                AtelierButton(
                    text = stringResource(R.string.services_delete_action),
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Destructive,
                    enabled = !isSaving
                )
            }
        }
    ) {
        AtelierTextField(
            label = stringResource(R.string.services_name_label),
            placeholder = stringResource(R.string.services_name_hint),
            value = form.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            isError = invalidField == "name",
            errorText = if (invalidField == "name") stringResource(R.string.services_field_name) else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        AtelierTextField(
            label = stringResource(R.string.services_price_label),
            placeholder = stringResource(R.string.services_price_hint),
            value = form.basePrice,
            onValueChange = onBasePriceChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            isError = invalidField == "basePrice",
            errorText = if (invalidField == "basePrice") stringResource(R.string.services_field_price) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
        )
        AtelierTextField(
            label = stringResource(R.string.services_duration_label),
            placeholder = stringResource(R.string.services_duration_hint),
            value = form.estimatedMinutes,
            onValueChange = onEstimatedMinutesChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving,
            isError = invalidField == "estimatedMinutes",
            errorText = if (invalidField == "estimatedMinutes") stringResource(R.string.services_field_duration) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
        )
        error?.let {
            AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline)
        }
    }
}

@Composable
private fun DeleteServiceSheet(
    serviceName: String,
    isSaving: Boolean,
    error: com.andeva.atelier.mobile.core.designsystem.text.UiText?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.services_delete_title),
        onDismiss = onDismiss,
        actions = {
            AtelierButton(
                text = stringResource(R.string.services_delete_confirm),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.DestructiveFilled,
                enabled = !isSaving,
                loading = isSaving
            )
            AtelierButton(
                text = stringResource(R.string.quotation_reject_back),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !isSaving
            )
        }
    ) {
        Text(
            text = stringResource(R.string.services_delete_body, serviceName),
            style = MaterialTheme.typography.bodyLarge,
            color = AtelierTheme.colors.inkSecondary
        )
        error?.let {
            AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline)
        }
    }
}

// region Previews

private val previewServicesState = ServicesCatalogUiState(
    services = listOf(
        ServiceOption("s1", "Cambio de aceite y filtro", Money.of("120.00", Money.PEN), 45),
        ServiceOption("s2", "Cambio de pastillas de freno", Money.of("150.00", Money.PEN), 60),
        ServiceOption("s3", "Revisión del sistema de refrigeración", Money.of("180.00", Money.PEN), 90),
        ServiceOption("s4", "Revisión del sistema eléctrico y alternador", Money.of("110.00", Money.PEN), 45)
    ),
    isLoading = false,
    isOnline = true,
    canRead = true,
    canManage = true
)

@Preview(name = "Servicios · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ServicesCatalogPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    ServicesCatalogContent(
        state = previewServicesState,
        onBack = {}, onRefresh = {}, onSearchQueryChange = {},
        onOpenCreate = {}, onOpenEdit = {}, onDismissForm = {},
        onNameChange = {}, onBasePriceChange = {}, onEstimatedMinutesChange = {},
        onConfirmForm = {}, onOpenDelete = {}, onDismissDelete = {}, onConfirmDelete = {}
    )
}

@Preview(
    name = "Servicios · oscuro", widthDp = 390, heightDp = 844, showBackground = true,
    backgroundColor = 0xFF14171A, uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ServicesCatalogPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    ServicesCatalogContent(
        state = previewServicesState,
        onBack = {}, onRefresh = {}, onSearchQueryChange = {},
        onOpenCreate = {}, onOpenEdit = {}, onDismissForm = {},
        onNameChange = {}, onBasePriceChange = {}, onEstimatedMinutesChange = {},
        onConfirmForm = {}, onOpenDelete = {}, onDismissDelete = {}, onConfirmDelete = {}
    )
}

// endregion
