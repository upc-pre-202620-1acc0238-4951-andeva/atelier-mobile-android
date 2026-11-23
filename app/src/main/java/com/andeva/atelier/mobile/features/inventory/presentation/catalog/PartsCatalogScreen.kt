package com.andeva.atelier.mobile.features.inventory.presentation.catalog

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierNoPermissionState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRefreshBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.core.presentation.formatQuantity
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.presentation.catalog.components.CategoryFilterChips
import com.andeva.atelier.mobile.features.inventory.presentation.stockChipKind
import com.andeva.atelier.mobile.features.inventory.presentation.stockLabelRes
import java.math.BigDecimal

/** US31: catálogo con buscador por nombre o SKU, categorías y semáforo de existencias. Requiere `PARTS_READ`. */
@Composable
fun PartsCatalogScreen(
    onBack: () -> Unit,
    onOpenPart: (partId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PartsCatalogViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PartsCatalogContent(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onCategoryClick = viewModel::onCategoryClick,
        onClearFilters = viewModel::onClearFilters,
        onRefresh = { viewModel.refresh(force = true) },
        onOpenPart = onOpenPart,
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
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
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
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
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                        )
                        CategoryFilterChips(selected = state.category, onClick = onCategoryClick)
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
                    !state.isLoaded -> items(6, key = { "bone-$it" }) {
                        AtelierListRow(title = "Pastillas de freno", subtitle = "SKU BRK-001 · Stock: 12", loading = true)
                    }
                    state.parts.isEmpty() && error != null -> item(key = "error") {
                        AtelierErrorState(
                            message = error.asString(),
                            onRetry = onRefresh,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    state.parts.isEmpty() -> item(key = "empty") {
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

private fun previewPart(id: String, name: String, stock: String, min: String = "5") = Part(
    id = id, name = name, sku = "BRK-00$id", category = ItemCategory.BRAKES,
    basePrice = Money(BigDecimal("89.90"), Money.PEN), totalStock = BigDecimal(stock),
    minimumStock = BigDecimal(min), unitOfMeasure = "UNIT", status = PartStatus.ACTIVE
)

private val previewState = PartsCatalogUiState(
    isLoaded = true,
    parts = listOf(
        previewPart("1", "Pastillas de freno delanteras", "12"),
        previewPart("2", "Disco de freno ventilado", "3"),
        previewPart("3", "Líquido de frenos DOT4", "0")
    )
)

@Composable
private fun PreviewContent(state: PartsCatalogUiState) = PartsCatalogContent(
    state = state, onBack = {}, onQueryChange = {}, onCategoryClick = {}, onClearFilters = {}, onRefresh = {},
    onOpenPart = {}
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
