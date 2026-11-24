package com.andeva.atelier.mobile.features.inventory.presentation.suppliers

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFab
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOfflineState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.features.inventory.domain.Supplier

/** Catálogo buscable de proveedores con alta y edición en hoja inferior (`SUPPLIERS_MANAGE`). */
@Composable
fun SuppliersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SuppliersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAtelierHaptics()

    LaunchedEffect(state.hapticEvent) {
        if (state.hapticEvent > 0) {
            if (state.hapticIsError) haptics.reject() else haptics.confirm()
        }
    }

    SuppliersContent(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onRefresh = viewModel::refresh,
        onOpenCreate = viewModel::onOpenCreate,
        onOpenEdit = viewModel::onOpenEdit,
        onDismissSheet = viewModel::onDismissSheet,
        onBusinessNameChange = viewModel::onBusinessNameChange,
        onTaxIdChange = viewModel::onTaxIdChange,
        onContactNameChange = viewModel::onContactNameChange,
        onPhoneChange = viewModel::onPhoneChange,
        onEmailChange = viewModel::onEmailChange,
        onAddressChange = viewModel::onAddressChange,
        onActiveChange = viewModel::onActiveChange,
        onSave = viewModel::onSave,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuppliersContent(
    state: SuppliersUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenEdit: (Supplier) -> Unit,
    onDismissSheet: () -> Unit,
    onBusinessNameChange: (String) -> Unit,
    onTaxIdChange: (String) -> Unit,
    onContactNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val delayedLoading = rememberDelayedLoading(state.suppliers.isEmpty() && state.isLoading)
    val showSkeleton = state.suppliers.isEmpty() && state.isLoading && (LocalInspectionMode.current || delayedLoading)

    Scaffold(
        modifier = modifier.navigationBarsPadding().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state.canManage) {
                AtelierFab(
                    text = stringResource(R.string.inv_seg2_suppliers_new_fab),
                    icon = Icons.Outlined.Add,
                    onClick = onOpenCreate
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AtelierTopBar(title = stringResource(R.string.inv_seg2_suppliers_title), onBack = onBack)
            if (!state.canManage) {
                AtelierNoPermissionState(
                    text = stringResource(R.string.inv_seg2_suppliers_no_permission),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                return@Column
            }
            AtelierRefreshBar(visible = state.isLoading && state.suppliers.isNotEmpty())
            PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                    item(key = "search") {
                        AtelierTextField(
                            label = stringResource(R.string.inv_seg2_suppliers_search_label),
                            value = state.query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            leadingIcon = Icons.Outlined.Search,
                            placeholder = stringResource(R.string.inv_seg2_suppliers_search_hint),
                            trailing = if (state.query.isNotEmpty()) {
                                {
                                    IconButton(onClick = { onQueryChange("") }) {
                                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.inv_seg2_clear))
                                    }
                                }
                            } else null,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )
                    }
                    state.bannerMessage?.let { msg ->
                        item(key = "banner") {
                            AtelierBanner(
                                message = msg.asString(),
                                icon = Icons.Outlined.CheckCircle,
                                kind = BannerKind.Ok,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            )
                        }
                    }
                    val error = state.error
                    when {
                        showSkeleton -> items(5, key = { "bone-$it" }) {
                            AtelierListRow(
                                title = "Distribuidora Automotriz SAC",
                                subtitle = "RUC 20512345678 · Carlos Ruiz",
                                loading = true
                            )
                        }
                        state.suppliers.isEmpty() && error != null && !state.isOnline -> item(key = "offline") {
                            AtelierOfflineState(onRetry = onRefresh, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                        state.suppliers.isEmpty() && error != null -> item(key = "error") {
                            AtelierErrorState(
                                message = error.asString(),
                                onRetry = onRefresh,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        state.suppliers.isEmpty() && !state.isLoading -> item(key = "empty") {
                            AtelierEmptyState(
                                text = stringResource(
                                    if (state.query.isNotBlank()) R.string.inv_seg2_suppliers_empty_filtered
                                    else R.string.inv_seg2_suppliers_empty
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        else -> items(state.suppliers, key = { it.id }) { supplier ->
                            SupplierRow(supplier = supplier, onClick = { onOpenEdit(supplier) })
                        }
                    }
                }
            }
        }
    }

    if (state.sheetOpen) {
        SupplierFormSheet(
            state = state,
            onDismiss = onDismissSheet,
            onBusinessNameChange = onBusinessNameChange,
            onTaxIdChange = onTaxIdChange,
            onContactNameChange = onContactNameChange,
            onPhoneChange = onPhoneChange,
            onEmailChange = onEmailChange,
            onAddressChange = onAddressChange,
            onActiveChange = onActiveChange,
            onSave = onSave
        )
    }
}

@Composable
private fun SupplierRow(supplier: Supplier, onClick: () -> Unit) {
    val subtitle = listOfNotNull(
        stringResource(R.string.inv_seg2_suppliers_tax_id_line, supplier.taxId),
        supplier.contactName,
        supplier.phone
    ).joinToString(" · ")
    AtelierListRow(
        title = supplier.businessName,
        subtitle = subtitle,
        onClick = onClick,
        trailing = {
            AtelierChip(
                text = stringResource(
                    if (supplier.active) R.string.inv_seg2_suppliers_active else R.string.inv_seg2_suppliers_inactive
                ),
                kind = if (supplier.active) ChipKind.Ok else ChipKind.Idle
            )
        }
    )
}

@Composable
private fun SupplierFormSheet(
    state: SuppliersUiState,
    onDismiss: () -> Unit,
    onBusinessNameChange: (String) -> Unit,
    onTaxIdChange: (String) -> Unit,
    onContactNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(
            if (state.isEditing) R.string.inv_seg2_suppliers_edit_title else R.string.inv_seg2_suppliers_create_title
        ),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.inv_seg2_save),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline && !state.isSaving,
                loading = state.isSaving
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !state.isSaving
            )
        }
    ) {
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_supplier_business_name),
            value = state.businessName,
            onValueChange = onBusinessNameChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.nameError != null,
            errorText = state.nameError?.let { stringResource(it) },
            enabled = !state.isSaving
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_supplier_tax_id),
            value = state.taxId,
            onValueChange = onTaxIdChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.taxIdError != null,
            errorText = state.taxIdError?.let { stringResource(it) },
            enabled = !state.isEditing && !state.isSaving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_supplier_contact),
            value = state.contactName,
            onValueChange = onContactNameChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_supplier_phone),
            value = state.phone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_supplier_email),
            value = state.email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.emailError != null,
            errorText = state.emailError?.let { stringResource(it) },
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_supplier_address),
            value = state.address,
            onValueChange = onAddressChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving
        )
        if (state.isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.inv_seg2_supplier_active_toggle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(checked = state.active, onCheckedChange = onActiveChange, enabled = !state.isSaving)
            }
        }
        state.submitError?.let { err ->
            AtelierBanner(message = err.asString(), icon = Icons.Outlined.ErrorOutline)
        }
    }
}

private val previewSuppliers = listOf(
    Supplier("s1", "Repuestos Pacífico SAC", "20512345678", "Carlos Ruiz", "998112233", null, "Av. Nicolás Arriola 450", true),
    Supplier("s2", "Lubricantes del Sur EIRL", "20601122334", "María Torres", "987654321", null, null, false)
)

@Preview(name = "Proveedores · claro", widthDp = 390, heightDp = 844)
@Composable
private fun SuppliersPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    SuppliersContent(
        state = SuppliersUiState(suppliers = previewSuppliers, isLoading = false),
        onBack = {}, onQueryChange = {}, onRefresh = {}, onOpenCreate = {}, onOpenEdit = {},
        onDismissSheet = {}, onBusinessNameChange = {}, onTaxIdChange = {}, onContactNameChange = {},
        onPhoneChange = {}, onEmailChange = {}, onAddressChange = {}, onActiveChange = {}, onSave = {}
    )
}

@Preview(name = "Proveedores · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SuppliersPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    SuppliersContent(
        state = SuppliersUiState(suppliers = previewSuppliers, isLoading = false),
        onBack = {}, onQueryChange = {}, onRefresh = {}, onOpenCreate = {}, onOpenEdit = {},
        onDismissSheet = {}, onBusinessNameChange = {}, onTaxIdChange = {}, onContactNameChange = {},
        onPhoneChange = {}, onEmailChange = {}, onAddressChange = {}, onActiveChange = {}, onSave = {}
    )
}

@Preview(name = "Proveedores · cargando", widthDp = 390, heightDp = 844)
@Composable
private fun SuppliersPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    SuppliersContent(
        state = SuppliersUiState(isLoading = true),
        onBack = {}, onQueryChange = {}, onRefresh = {}, onOpenCreate = {}, onOpenEdit = {},
        onDismissSheet = {}, onBusinessNameChange = {}, onTaxIdChange = {}, onContactNameChange = {},
        onPhoneChange = {}, onEmailChange = {}, onAddressChange = {}, onActiveChange = {}, onSave = {}
    )
}
