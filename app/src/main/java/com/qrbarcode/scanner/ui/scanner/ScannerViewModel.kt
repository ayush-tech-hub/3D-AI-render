package com.qrbarcode.scanner.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import com.qrbarcode.scanner.data.repository.ScanRepository
import com.qrbarcode.scanner.domain.model.ScanItem
import com.qrbarcode.scanner.util.BarcodeTypeDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScannerUiState(
    val isScanning: Boolean = true,
    val flashlightOn: Boolean = false,
    val continuousScan: Boolean = false,
    val soundEnabled: Boolean = true,
    val lastScan: ScanItem? = null,
    val showResult: Boolean = false,
    val noCodeFound: Boolean = false,
    val showPrivacyDialog: Boolean = false
)

class ScannerViewModel(
    private val repository: ScanRepository,
    private val showPrivacy: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState(showPrivacyDialog = showPrivacy))
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var lastScannedValue: String? = null

    fun onBarcodeDetected(barcodes: List<Barcode>) {
        val state = _uiState.value
        if (state.showPrivacyDialog) return

        val barcode = barcodes.firstOrNull() ?: return
        val rawValue = barcode.rawValue ?: return

        // Deduplicate: same code detected again in same scan session
        if (rawValue == lastScannedValue) return
        // Don't accept new scans while result is shown (unless continuous mode)
        if (state.showResult && !state.continuousScan) return

        lastScannedValue = rawValue

        val type = BarcodeTypeDetector.detectType(barcode)
        val format = BarcodeTypeDetector.getBarcodeFormat(barcode.format)
        val content = BarcodeTypeDetector.getDisplayContent(barcode)

        val scanItem = ScanItem(
            content = content,
            rawValue = rawValue,
            format = format,
            type = type,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val id = repository.insertScan(scanItem)
            _uiState.value = _uiState.value.copy(
                lastScan = scanItem.copy(id = id),
                showResult = true,
                isScanning = false
            )
        }
    }

    fun onGalleryNoBarcodeFound() {
        _uiState.value = _uiState.value.copy(noCodeFound = true)
    }

    fun dismissNoCodeFound() {
        _uiState.value = _uiState.value.copy(noCodeFound = false)
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(showResult = false, isScanning = true)
        if (!_uiState.value.continuousScan) {
            lastScannedValue = null
        }
    }

    fun resumeScanning() {
        lastScannedValue = null
        _uiState.value = _uiState.value.copy(showResult = false, isScanning = true)
    }

    fun toggleFlashlight() {
        _uiState.value = _uiState.value.copy(flashlightOn = !_uiState.value.flashlightOn)
    }

    fun toggleContinuousScan() {
        _uiState.value = _uiState.value.copy(continuousScan = !_uiState.value.continuousScan)
    }

    fun toggleSound() {
        _uiState.value = _uiState.value.copy(soundEnabled = !_uiState.value.soundEnabled)
    }

    fun dismissPrivacyDialog() {
        _uiState.value = _uiState.value.copy(showPrivacyDialog = false)
    }

    class Factory(
        private val repository: ScanRepository,
        private val showPrivacy: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ScannerViewModel(repository, showPrivacy) as T
    }
}
