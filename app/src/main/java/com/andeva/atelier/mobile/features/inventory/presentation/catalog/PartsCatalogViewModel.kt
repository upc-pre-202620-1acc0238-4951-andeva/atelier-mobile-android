package com.andeva.atelier.mobile.features.inventory.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.application.CreatePartUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.application.RefreshPartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.application.SearchPartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.inventory.domain.ItemCategory
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * US31: buscador reactivo del catálogo + alta de repuestos (`PARTS_MANAGE`). Al abrir recarga el catálogo (1 h de caché)
 * y observa Room; el texto se busca por nombre o SKU con debounce y la categoría filtra. Sin red se busca en la caché.
 */
@HiltViewModel
class PartsCatalogViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val observeCatalog: ObservePartsCatalogUseCase,
    private val refreshCatalog: RefreshPartsCatalogUseCase,
    private val searchCatalog: SearchPartsCatalogUseCase,
    connectivity: ConnectivityObserver,
    private val createPart: CreatePartUseCase
) : ViewModel() {

    private data class Filters(val query: String, val category: ItemCategory?)

    private val _uiState = MutableStateFlow(PartsCatalogUiState())
    val uiState: StateFlow<PartsCatalogUiState> = _uiState.asStateFlow()

    private val filters = MutableStateFlow(Filters("", null))

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeFilters() = viewModelScope.launch {
        filters.debounce(SEARCH_DEBOUNCE_MS).collectLatest { filter ->
            if (filter.query.isBlank()) {
                _uiState.update { it.copy(isSearching = false) }
                observeCatalog(filter.category).collect { parts ->
                    _uiState.update { it.copy(parts = parts, isLoaded = true) }
                }
            } else {
                _uiState.update { it.copy(isSearching = true, error = null) }
                searchCatalog(filter.query.trim(), filter.category)
                    .onSuccess { parts ->
                        _uiState.update { it.copy(parts = parts, isLoaded = true, isSearching = false) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isSearching = false, isLoaded = true, error = e.toUiText()) }
                    }
            }
        }
    }

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update {
                    it.copy(
                        canRead = session?.has(Permission.PARTS_READ) == true,
                        canManage = session?.has(Permission.PARTS_MANAGE) == true
                    )
                }
            }
        }
        observeFilters()
        // D2: al volver la red tras una recarga fallida, se actualiza sola y el aviso se quita.
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val retry = online && _uiState.value.showingCache
                _uiState.update { it.copy(isOnline = online) }
                if (retry) refresh()
            }
        }
        refresh()
    }

    /** Al abrir y al tocar actualizar: [force] ignora la antigüedad de la caché. */
    fun refresh(force: Boolean = false) {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            refreshCatalog(force)
                .onSuccess { _uiState.update { it.copy(isRefreshing = false, showingCache = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isRefreshing = false, showingCache = true, error = e.toUiText()) }
                }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
        filters.update { it.copy(query = value) }
    }

    /** Tocar la categoría activa la quita. */
    fun onCategoryClick(category: ItemCategory?) {
        val next = if (_uiState.value.category == category) null else category
        _uiState.update { it.copy(category = next) }
        filters.update { it.copy(category = next) }
    }

    fun onClearFilters() {
        _uiState.update { it.copy(query = "", category = null) }
        filters.value = Filters("", null)
    }

    fun onOpenCreateSheet() {
        _uiState.update {
            it.copy(
                createSheetOpen = true,
                formName = "",
                formSku = "",
                formCategory = it.category ?: ItemCategory.LUBRICANTS,
                formBasePrice = "",
                formMinimumStock = "",
                nameError = null,
                skuError = null,
                priceError = null,
                minStockError = null,
                createError = null
            )
        }
    }

    fun onDismissCreateSheet() {
        if (_uiState.value.isCreating) return
        _uiState.update { it.copy(createSheetOpen = false, createError = null) }
    }

    fun onFormNameChange(v: String) = _uiState.update { it.copy(formName = v, nameError = null, createError = null) }
    fun onFormSkuChange(v: String) = _uiState.update { it.copy(formSku = v, skuError = null, createError = null) }
    fun onFormCategoryChange(v: ItemCategory) = _uiState.update { it.copy(formCategory = v, createError = null) }
    fun onFormBasePriceChange(v: String) = _uiState.update { it.copy(formBasePrice = v, priceError = null, createError = null) }
    fun onFormMinimumStockChange(v: String) = _uiState.update { it.copy(formMinimumStock = v, minStockError = null, createError = null) }

    fun onCreatePart() {
        val state = _uiState.value
        if (state.isCreating || !state.isOnline) return
        val price = state.formBasePrice.trim().replace(',', '.').toBigDecimalOrNull()
        val minStock = state.formMinimumStock.trim().replace(',', '.').toBigDecimalOrNull()

        val nameErr = if (!InventoryValidation.isNonEmptyText(state.formName)) R.string.inv_seg2_part_err_name else null
        val skuErr = if (!InventoryValidation.isSku(state.formSku)) R.string.inv_seg2_part_err_sku else null
        val priceErr = if (price == null || !InventoryValidation.isNonNegative(price)) R.string.inv_seg2_part_err_price else null
        val minErr = if (minStock == null || !InventoryValidation.isNonNegative(minStock)) R.string.inv_seg2_part_err_min_stock else null

        if (nameErr != null || skuErr != null || priceErr != null || minErr != null) {
            _uiState.update {
                it.copy(
                    nameError = nameErr,
                    skuError = skuErr,
                    priceError = priceErr,
                    minStockError = minErr,
                    hapticEvent = it.hapticEvent + 1,
                    hapticIsError = true
                )
            }
            return
        }

        _uiState.update { it.copy(isCreating = true, createError = null) }
        viewModelScope.launch {
            createPart(
                name = state.formName,
                sku = state.formSku,
                category = state.formCategory,
                basePrice = price!!,
                minimumStock = minStock!!
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            createSheetOpen = false,
                            bannerMessage = UiText.Resource(R.string.inv_seg2_part_saved),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            createError = e.toUiText(),
                            hapticEvent = it.hapticEvent + 1,
                            hapticIsError = true
                        )
                    }
                }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
