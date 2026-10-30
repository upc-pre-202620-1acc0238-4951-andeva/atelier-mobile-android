package com.andeva.atelier.mobile.features.inventory.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.data.remote.ConnectivityObserver
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.application.ObservePartBrandsUseCase
import com.andeva.atelier.mobile.features.inventory.application.ObservePartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.application.RefreshPartsCatalogUseCase
import com.andeva.atelier.mobile.features.inventory.application.SearchPartsCatalogUseCase
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
 * US31: buscador reactivo del catálogo. Al abrir recarga el catálogo (1 h de caché) y observa Room; el texto se
 * busca por nombre o SKU con debounce y la categoría filtra. Sin red se busca en la caché.
 */
@HiltViewModel
class PartsCatalogViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val observeCatalog: ObservePartsCatalogUseCase,
    private val refreshCatalog: RefreshPartsCatalogUseCase,
    private val searchCatalog: SearchPartsCatalogUseCase,
    observeBrands: ObservePartBrandsUseCase,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private data class Filters(val query: String, val category: ItemCategory?, val brand: String?)

    private val _uiState = MutableStateFlow(PartsCatalogUiState())
    val uiState: StateFlow<PartsCatalogUiState> = _uiState.asStateFlow()

    private val filters = MutableStateFlow(Filters("", null, null))

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeFilters() = viewModelScope.launch {
        filters.debounce(SEARCH_DEBOUNCE_MS).collectLatest { filter ->
            if (filter.query.isBlank()) {
                _uiState.update { it.copy(isSearching = false) }
                observeCatalog(filter.category, filter.brand).collect { parts ->
                    _uiState.update { it.copy(parts = parts, isLoaded = true) }
                }
            } else {
                _uiState.update { it.copy(isSearching = true, error = null) }
                searchCatalog(filter.query.trim(), filter.category, filter.brand)
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
                _uiState.update { it.copy(canRead = session?.has(Permission.PARTS_READ) == true) }
            }
        }
        viewModelScope.launch {
            observeBrands().collect { brands ->
                // Si la marca elegida ya no existe (recarga sin ella), se quita el filtro.
                _uiState.update { it.copy(brands = brands, brand = it.brand?.takeIf { b -> b in brands }) }
                filters.update { f -> if (f.brand != null && f.brand !in brands) f.copy(brand = null) else f }
            }
        }
        observeFilters()
        // D2: al volver la red tras una recarga fallida, se actualiza sola y el aviso se quita.
        viewModelScope.launch {
            connectivity.isOnline.collect { online -> if (online && _uiState.value.showingCache) refresh() }
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

    /** Tocar la marca activa la quita. */
    fun onBrandClick(brand: String) {
        val next = if (_uiState.value.brand == brand) null else brand
        _uiState.update { it.copy(brand = next) }
        filters.update { it.copy(brand = next) }
    }

    fun onClearFilters() {
        _uiState.update { it.copy(query = "", category = null, brand = null) }
        filters.value = Filters("", null, null)
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
