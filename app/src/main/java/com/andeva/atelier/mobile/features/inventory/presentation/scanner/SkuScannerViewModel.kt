package com.andeva.atelier.mobile.features.inventory.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.features.inventory.application.FindPartBySkuUseCase
import com.andeva.atelier.mobile.features.inventory.domain.InvalidInventoryInputException
import com.andeva.atelier.mobile.features.inventory.domain.InventoryValidation
import com.andeva.atelier.mobile.features.main.application.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SkuScannerUiState(
    val manualSku: String = "",
    val lastScannedSku: String? = null,
    val isSearching: Boolean = false,
    val notFoundSku: String? = null,
    val invalidSku: String? = null,
    val error: UiText? = null,
    val foundPartId: String? = null,
    val canRead: Boolean = true
)

/** Lector de códigos de barras/QR por SKU (US31 E1) con entrada manual siempre accesible. */
@HiltViewModel
class SkuScannerViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val findPartBySku: FindPartBySkuUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SkuScannerUiState())
    val uiState: StateFlow<SkuScannerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeSession().collect { session ->
                _uiState.update { it.copy(canRead = session?.has(Permission.PARTS_READ) == true) }
            }
        }
    }

    fun onBarcodeDetected(rawValue: String) {
        val normalized = InventoryValidation.normalizeSku(rawValue)
        val current = _uiState.value
        if (!current.canRead || current.isSearching || current.foundPartId != null) return
        if (normalized.isEmpty() || normalized == current.lastScannedSku) return
        _uiState.update { it.copy(lastScannedSku = normalized, manualSku = normalized) }
        lookupSku(rawValue)
    }

    fun onManualSkuChange(value: String) {
        _uiState.update {
            it.copy(manualSku = value, notFoundSku = null, invalidSku = null, error = null)
        }
    }

    fun onSubmitManualSku() {
        val current = _uiState.value
        if (!current.canRead || current.isSearching) return
        val normalized = InventoryValidation.normalizeSku(current.manualSku)
        _uiState.update { it.copy(lastScannedSku = normalized) }
        lookupSku(current.manualSku)
    }

    fun onResetScan() {
        _uiState.update {
            it.copy(lastScannedSku = null, notFoundSku = null, invalidSku = null, error = null)
        }
    }

    fun onPartNavigated() {
        _uiState.update { it.copy(foundPartId = null, lastScannedSku = null) }
    }

    private fun lookupSku(rawSku: String) {
        _uiState.update { it.copy(isSearching = true, notFoundSku = null, invalidSku = null, error = null) }
        viewModelScope.launch {
            findPartBySku(rawSku)
                .onSuccess { part ->
                    if (part != null) {
                        _uiState.update { it.copy(isSearching = false, foundPartId = part.id) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSearching = false,
                                notFoundSku = InventoryValidation.normalizeSku(rawSku)
                            )
                        }
                    }
                }
                .onFailure { e ->
                    if (e is InvalidInventoryInputException && e.field == "sku") {
                        _uiState.update {
                            it.copy(isSearching = false, invalidSku = rawSku.trim().ifEmpty { rawSku })
                        }
                    } else {
                        _uiState.update { it.copy(isSearching = false, error = e.toUiText()) }
                    }
                }
        }
    }
}
