package com.qrbarcode.scanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qrbarcode.scanner.data.repository.ScanRepository
import com.qrbarcode.scanner.domain.model.ScanItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(
    val scans: List<ScanItem> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val showClearConfirm: Boolean = false,
    val selectedScan: ScanItem? = null
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val repository: ScanRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _showClearConfirm = MutableStateFlow(false)
    private val _selectedScan = MutableStateFlow<ScanItem?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        _searchQuery
            .debounce(300)
            .flatMapLatest { query ->
                if (query.isBlank()) repository.getAllScans()
                else repository.searchScans(query)
            },
        _searchQuery,
        _showClearConfirm,
        _selectedScan
    ) { scans, query, showClear, selected ->
        HistoryUiState(
            scans = scans,
            searchQuery = query,
            isLoading = false,
            showClearConfirm = showClear,
            selectedScan = selected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteScan(scan: ScanItem) {
        viewModelScope.launch { repository.deleteScan(scan) }
    }

    fun togglePin(scan: ScanItem) {
        viewModelScope.launch { repository.togglePin(scan.id, !scan.isPinned) }
    }

    fun requestClearAll() {
        _showClearConfirm.value = true
    }

    fun dismissClearConfirm() {
        _showClearConfirm.value = false
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.deleteAllScans()
            _showClearConfirm.value = false
        }
    }

    fun selectScan(scan: ScanItem) {
        _selectedScan.value = scan
    }

    fun dismissSelected() {
        _selectedScan.value = null
    }

    fun getAllScansForExport(callback: (List<ScanItem>) -> Unit) {
        viewModelScope.launch {
            callback(repository.getAllScansOnce())
        }
    }

    class Factory(private val repository: ScanRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(repository) as T
    }
}
