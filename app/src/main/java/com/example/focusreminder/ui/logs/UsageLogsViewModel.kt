package com.example.focusreminder.ui.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusreminder.domain.model.UsageLogSummary
import com.example.focusreminder.platform.logs.UsageLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsageLogsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UsageLogRepository.getInstance(application)

    private val _summary = MutableStateFlow<UsageLogSummary?>(null)
    val summary: StateFlow<UsageLogSummary?> = _summary.asStateFlow()

    private val _selectedDateOffset = MutableStateFlow(0) // 0 = Today, -1 = Yesterday
    val selectedDateOffset: StateFlow<Int> = _selectedDateOffset.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sector Filter: null = "Any Time" (All Sectors), or "sector_morning", "sector_afternoon", "sector_evening", "sector_night"
    private val _selectedSectorId = MutableStateFlow<String?>(null)
    val selectedSectorId: StateFlow<String?> = _selectedSectorId.asStateFlow()

    // Package Filter: specific app filter (e.g. redirected from Analytics app sector click)
    private val _selectedPackageFilter = MutableStateFlow<String?>(null)
    val selectedPackageFilter: StateFlow<String?> = _selectedPackageFilter.asStateFlow()

    private val _selectedAppName = MutableStateFlow<String?>(null)
    val selectedAppName: StateFlow<String?> = _selectedAppName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadLogs()
    }

    fun setDateOffset(offset: Int) {
        if (_selectedDateOffset.value != offset) {
            _selectedDateOffset.value = offset
            loadLogs()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedSector(sectorId: String?) {
        _selectedSectorId.value = sectorId
    }

    fun setInitialFilters(sectorId: String?, packageName: String?) {
        _selectedSectorId.value = sectorId
        if (!packageName.isNullOrBlank()) {
            _selectedPackageFilter.value = packageName
            val label = try {
                val pm = getApplication<Application>().packageManager
                val info = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
            }
            _selectedAppName.value = label
        }
    }

    fun clearPackageFilter() {
        _selectedPackageFilter.value = null
        _selectedAppName.value = null
    }

    fun loadLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repo.getUsageLogs(_selectedDateOffset.value)
            _summary.value = result
            _isLoading.value = false
        }
    }
}
