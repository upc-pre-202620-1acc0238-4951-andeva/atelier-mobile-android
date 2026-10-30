package com.andeva.atelier.mobile.features.inventory.presentation.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.domain.Money
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.inventory.domain.Part
import com.andeva.atelier.mobile.features.inventory.domain.PartStatus
import com.andeva.atelier.mobile.features.inventory.presentation.catalog.components.BrandFilterChips
import com.andeva.atelier.mobile.features.inventory.presentation.catalog.components.CategoryFilterChips
import com.andeva.atelier.mobile.features.inventory.presentation.catalog.components.PartCard
import java.math.BigDecimal

/** US31: catálogo con buscador por nombre o SKU, chips de categoría y existencias. */
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
        actions = PartsCatalogActions(
            onBack = onBack,
            onQueryChange = viewModel::onQueryChange,
            onCategoryClick = viewModel::onCategoryClick,
            onBrandClick = viewModel::onBrandClick,
            onClearFilters = viewModel::onClearFilters,
            onRefresh = { viewModel.refresh(force = true) },
            onOpenPart = onOpenPart
        ),
        modifier = modifier
    )
}

internal class PartsCatalogActions(
    val onBack: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onCategoryClick: (ItemCategory) -> Unit,
    val onBrandClick: (String) -> Unit,
    val onClearFilters: () -> Unit,
    val onRefresh: () -> Unit,
    val onOpenPart: (String) -> Unit
)

@Composable
internal fun PartsCatalogContent(
    state: PartsCatalogUiState,
    actions: PartsCatalogActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.catalog_title), onBack = actions.onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!state.canRead) {
                AtelierBanner(
                    message = stringResource(R.string.catalog_no_permission),
                    icon = Icons.Outlined.Info,
                    kind = BannerKind.Wait
                )
            }
            AtelierTextField(
                label = stringResource(R.string.catalog_search_label),
                value = state.query,
                onValueChange = actions.onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = Icons.Outlined.Search,
                placeholder = stringResource(R.string.catalog_search_hint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            CategoryFilterChips(selected = state.category, onClick = actions.onCategoryClick)
            BrandFilterChips(brands = state.brands, selected = state.brand, onClick = actions.onBrandClick)

            if (state.showingCache) {
                AtelierBanner(
                    message = state.error?.asString() ?: stringResource(R.string.catalog_cache_notice),
                    icon = Icons.Outlined.WifiOff,
                    kind = BannerKind.Wait
                )
            } else {
                state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
            }

            when {
                !state.isLoaded || state.isSearching -> Text(
                    text = stringResource(R.string.search_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AtelierTheme.colors.inkSecondary
                )
                state.parts.isEmpty() -> {
                    Text(
                        text = stringResource(
                            if (state.hasFilters) R.string.catalog_empty_filtered else R.string.catalog_empty
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AtelierTheme.colors.inkSecondary
                    )
                    if (state.hasFilters) {
                        AtelierButton(
                            text = stringResource(R.string.catalog_clear_filters),
                            onClick = actions.onClearFilters,
                            modifier = Modifier.fillMaxWidth(),
                            style = AtelierButtonStyle.Text
                        )
                    }
                }
                else -> state.parts.forEach { part ->
                    PartCard(part, onClick = { actions.onOpenPart(part.id) })
                }
            }

            AtelierButton(
                text = stringResource(R.string.tasks_refresh),
                onClick = actions.onRefresh,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Outlined,
                loading = state.isRefreshing
            )
        }
    }
}

private fun previewPart(id: String, name: String, stock: String, min: String = "5", category: ItemCategory = ItemCategory.BRAKES) =
    Part(
        id = id, name = name, sku = "SKU-$id", category = category,
        basePrice = Money(BigDecimal("89.90"), Money.PEN), totalStock = BigDecimal(stock),
        minimumStock = BigDecimal(min), unitOfMeasure = "UNIT", status = PartStatus.ACTIVE
    )

private val previewActions = PartsCatalogActions({}, {}, {}, {}, {}, {}, {})

@Preview(name = "Catálogo · oscuro", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun PartsCatalogPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    PartsCatalogContent(
        state = PartsCatalogUiState(
            isLoaded = true, category = ItemCategory.BRAKES,
            parts = listOf(
                previewPart("1", "Pastillas de freno delanteras", "12"),
                previewPart("2", "Disco de freno ventilado", "3"),
                previewPart("3", "Líquido de frenos DOT4", "0")
            )
        ),
        actions = previewActions
    )
}

@Preview(name = "Catálogo · sin resultados · claro", widthDp = 360, heightDp = 600, showBackground = true)
@Composable
private fun PartsCatalogPreviewEmpty() = AtelierWorkshopTheme(darkTheme = false) {
    PartsCatalogContent(
        state = PartsCatalogUiState(isLoaded = true, query = "xyz"),
        actions = previewActions
    )
}
