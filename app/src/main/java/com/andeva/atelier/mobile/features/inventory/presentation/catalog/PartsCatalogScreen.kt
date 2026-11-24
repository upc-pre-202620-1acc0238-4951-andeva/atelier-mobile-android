package com.andeva.atelier.mobile.features.inventory.presentation.catalog

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.LocalFloatingBarInset
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.component.rememberDelayedLoading
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.presentation.catalog.components.CategoryFilterChips
import com.andeva.atelier.mobile.features.inventory.presentation.labelRes
import com.andeva.atelier.mobile.features.inventory.presentation.stockChipKind
import com.andeva.atelier.mobile.features.inventory.presentation.stockLabelRes
import java.math.BigDecimal

/** US31: catálogo con buscador por nombre o SKU, escáner, categorías, semáforo y alta (`PARTS_MANAGE`). */
@Composable
fun PartsCatalogScreen(
    onBack: () -> Unit,
    onOpenPart: (partId: String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenScanner: () -> Unit = {},
    viewModel: PartsCatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAtelierHaptics()

    LaunchedEffect(state.hapticEvent) {
        if (state.hapticEvent > 0) {
            if (state.hapticIsError) haptics.reject() else haptics.confirm()
        }
    }

    PartsCatalogContent(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onCategoryClick = viewModel::onCategoryClick,
        onClearFilters = viewModel::onClearFilters,
        onRefresh = { viewModel.refresh(force = true) },
        onOpenPart = onOpenPart,
        onOpenScanner = onOpenScanner,
        onOpenCreateSheet = viewModel::onOpenCreateSheet,
        onDismissCreateSheet = viewModel::onDismissCreateSheet,
        onFormNameChange = viewModel::onFormNameChange,
        onFormSkuChange = viewModel::onFormSkuChange,
        onFormCategoryChange = viewModel::onFormCategoryChange,
        onFormBasePriceChange = viewModel::onFormBasePriceChange,
        onFormMinimumStockChange = viewModel::onFormMinimumStockChange,
        onCreatePart = viewModel::onCreatePart,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartsCatalogContent(
    state: PartsCatalogUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCategoryClick: (ItemCategory) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onOpenPart: (String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenCreateSheet: () -> Unit,
    onDismissCreateSheet: () -> Unit,
    onFormNameChange: (String) -> Unit,
    onFormSkuChange: (String) -> Unit,
    onFormCategoryChange: (ItemCategory) -> Unit,
    onFormBasePriceChange: (String) -> Unit,
    onFormMinimumStockChange: (String) -> Unit,
    onCreatePart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val delayedLoading = rememberDelayedLoading(!state.isLoaded)
    val showSkeleton = !state.isLoaded && (LocalInspectionMode.current || delayedLoading)

    Scaffold(
        modifier = modifier.navigationBarsPadding().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state.canRead && state.canManage) {
                AtelierFab(
                    text = stringResource(R.string.inv_seg2_part_new_fab),
                    icon = Icons.Outlined.Add,
                    onClick = onOpenCreateSheet,
                    modifier = Modifier.floatingBarPadding()
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            AtelierTopBar(title = stringResource(R.string.catalog_title), onBack = onBack)
            if (!state.canRead) {
                AtelierNoPermissionState(
                    text = stringResource(R.string.catalog_no_permission),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                return@Column
            }
            AtelierRefreshBar(visible = state.isRefreshing || state.isSearching)
            PullToRefreshBox(isRefreshing = false, onRefresh = onRefresh, modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = (if (state.canManage) 96.dp else 24.dp) + LocalFloatingBarInset.current
                    )
                ) {
                    item(key = "filters") {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AtelierTextField(
                                label = stringResource(R.string.catalog_search_label),
                                value = state.query,
                                onValueChange = onQueryChange,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = Icons.Outlined.Search,
                                placeholder = stringResource(R.string.catalog_search_hint),
                                trailing = {
                                    Row {
                                        if (state.query.isNotEmpty()) {
                                            IconButton(onClick = { onQueryChange("") }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = stringResource(R.string.inv_seg2_clear)
                                                )
                                            }
                                        }
                                        IconButton(onClick = onOpenScanner) {
                                            Icon(
                                                imageVector = Icons.Outlined.QrCodeScanner,
                                                contentDescription = stringResource(R.string.inv_seg2_scanner_open_desc)
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                            )
                            CategoryFilterChips(selected = state.category, onClick = onCategoryClick)
                        }
                    }
                    state.bannerMessage?.let { msg ->
                        item(key = "created-banner") {
                            AtelierBanner(
                                message = msg.asString(),
                                icon = Icons.Outlined.CheckCircle,
                                kind = BannerKind.Ok,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            )
                        }
                    }
                    val error = state.error
                    if (state.showingCache && state.parts.isNotEmpty()) {
                        item(key = "cache") {
                            AtelierBanner(
                                message = error?.asString() ?: stringResource(R.string.catalog_cache_notice),
                                icon = Icons.Outlined.Info,
                                kind = BannerKind.Info,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            )
                        }
                    }
                    when {
                        showSkeleton -> items(6, key = { "bone-$it" }) {
                            AtelierListRow(
                                title = "Pastillas de freno",
                                subtitle = "SKU BRK-001 · Stock: 12",
                                loading = true
                            )
                        }
                        !state.isLoaded && state.parts.isEmpty() -> Unit
                        state.parts.isEmpty() && error != null -> item(key = "error") {
                            AtelierErrorState(
                                message = error.asString(),
                                onRetry = onRefresh,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        state.parts.isEmpty() && state.isLoaded -> item(key = "empty") {
                            AtelierEmptyState(
                                text = stringResource(
                                    if (state.hasFilters) R.string.catalog_empty_filtered else R.string.catalog_empty
                                ),
                                actionLabel = if (state.hasFilters) stringResource(R.string.catalog_clear_filters) else null,
                                onAction = if (state.hasFilters) onClearFilters else null,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        else -> items(state.parts, key = { it.id }) { part ->
                            AtelierListRow(
                                title = part.name,
                                subtitle = stringResource(
                                    R.string.inventory_part_subtitle, part.sku, part.totalStock.formatQuantity()
                                ),
                                onClick = { onOpenPart(part.id) },
                                trailing = {
                                    AtelierChip(text = stringResource(part.stockLabelRes()), kind = part.stockChipKind())
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.createSheetOpen) {
        CreatePartSheet(
            state = state,
            onDismiss = onDismissCreateSheet,
            onNameChange = onFormNameChange,
            onSkuChange = onFormSkuChange,
            onCategoryChange = onFormCategoryChange,
            onBasePriceChange = onFormBasePriceChange,
            onMinimumStockChange = onFormMinimumStockChange,
            onConfirm = onCreatePart
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreatePartSheet(
    state: PartsCatalogUiState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onSkuChange: (String) -> Unit,
    onCategoryChange: (ItemCategory) -> Unit,
    onBasePriceChange: (String) -> Unit,
    onMinimumStockChange: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.inv_seg2_part_create_title),
        onDismiss = onDismiss,
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.inv_seg2_save),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isOnline && !state.isCreating,
                loading = state.isCreating
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !state.isCreating
            )
        }
    ) {
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_part_name),
            value = state.formName,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.nameError != null,
            errorText = state.nameError?.let { stringResource(it) },
            enabled = !state.isCreating
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_part_sku),
            value = state.formSku,
            onValueChange = onSkuChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.skuError != null,
            errorText = state.skuError?.let { stringResource(it) },
            enabled = !state.isCreating,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
        )
        Text(
            text = stringResource(R.string.inv_seg2_part_category),
            style = MaterialTheme.typography.titleSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ItemCategory.entries.forEach { category ->
                FilterChip(
                    selected = state.formCategory == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(stringResource(category.labelRes())) },
                    shape = ControlShape,
                    enabled = !state.isCreating
                )
            }
        }
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_part_base_price),
            value = state.formBasePrice,
            onValueChange = onBasePriceChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.priceError != null,
            errorText = state.priceError?.let { stringResource(it) },
            enabled = !state.isCreating,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        AtelierTextField(
            label = stringResource(R.string.inv_seg2_part_min_stock),
            value = state.formMinimumStock,
            onValueChange = onMinimumStockChange,
            modifier = Modifier.fillMaxWidth(),
            isError = state.minStockError != null,
            errorText = state.minStockError?.let { stringResource(it) },
            enabled = !state.isCreating,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        state.createError?.let { err ->
            AtelierBanner(message = err.asString(), icon = Icons.Outlined.ErrorOutline)
        }
    }
}

private fun previewPart(id: String, name: String, stock: String, min: String = "5") = Part(
    id = id, name = name, sku = "BRK-00$id", category = ItemCategory.BRAKES,
    basePrice = Money(BigDecimal("89.90"), Money.PEN), totalStock = BigDecimal(stock),
    minimumStock = BigDecimal(min), unitOfMeasure = "UNIT", status = PartStatus.ACTIVE
)

private val previewState = PartsCatalogUiState(
    isLoaded = true,
    canManage = true,
    parts = listOf(
        previewPart("1", "Pastillas de freno delanteras", "12"),
        previewPart("2", "Disco de freno ventilado", "3"),
        previewPart("3", "Líquido de frenos DOT4", "0")
    )
)

@Composable
private fun PreviewContent(state: PartsCatalogUiState) = PartsCatalogContent(
    state = state, onBack = {}, onQueryChange = {}, onCategoryClick = {}, onClearFilters = {}, onRefresh = {},
    onOpenPart = {}, onOpenScanner = {}, onOpenCreateSheet = {}, onDismissCreateSheet = {},
    onFormNameChange = {}, onFormSkuChange = {}, onFormCategoryChange = {}, onFormBasePriceChange = {},
    onFormMinimumStockChange = {}, onCreatePart = {}
)

@Preview(name = "Repuestos · claro", widthDp = 390, heightDp = 844)
@Composable
private fun PartsCatalogPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Repuestos · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PartsCatalogPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewContent(previewState) }

@Preview(name = "Repuestos · cargando sin caché", widthDp = 390, heightDp = 844)
@Composable
private fun PartsCatalogPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    PreviewContent(PartsCatalogUiState())
}
