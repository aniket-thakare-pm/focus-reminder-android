package com.example.focusreminder.ui.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusreminder.domain.model.TimeSector
import com.example.focusreminder.domain.model.UsageLogSummary
import com.example.focusreminder.platform.analytics.SectorConfigStore
import com.example.focusreminder.platform.logs.UsageLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class UsageLogsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UsageLogRepository.getInstance(application)
    private val sectorStore = SectorConfigStore.getInstance(application)

    private val _summary = MutableStateFlow<UsageLogSummary?>(null)
    val summary: StateFlow<UsageLogSummary?> = _summary.asStateFlow()

    private val _selectedDateOffset = MutableStateFlow(0) // 0 = Today, -1 = Yesterday, -2...
    val selectedDateOffset: StateFlow<Int> = _selectedDateOffset.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Sector Filter: null = All Day / No specific sector filter, or sectorId
    private val _selectedSectorId = MutableStateFlow<String?>(null)
    val selectedSectorId: StateFlow<String?> = _selectedSectorId.asStateFlow()

    // Custom Time Window Filter: Pair(startMinutesFromMidnight, endMinutesFromMidnight) or null
    private val _customTimeRange = MutableStateFlow<Pair<Int, Int>?>(null)
    val customTimeRange: StateFlow<Pair<Int, Int>?> = _customTimeRange.asStateFlow()

    private val _customTimeRangeFormatted = MutableStateFlow<String?>(null)
    val customTimeRangeFormatted: StateFlow<String?> = _customTimeRangeFormatted.asStateFlow()

    // Package Filter: specific app filter (e.g. redirected from Analytics app sector click)
    private val _selectedPackageFilter = MutableStateFlow<String?>(null)
    val selectedPackageFilter: StateFlow<String?> = _selectedPackageFilter.asStateFlow()

    private val _selectedAppName = MutableStateFlow<String?>(null)
    val selectedAppName: StateFlow<String?> = _selectedAppName.asStateFlow()

    private val _sectors = MutableStateFlow<List<TimeSector>>(sectorStore.getSectors())
    val sectors: StateFlow<List<TimeSector>> = _sectors.asStateFlow()

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

    fun stepDateOffset(delta: Int) {
        val next = _selectedDateOffset.value + delta
        if (next <= 0) { // Cannot go into the future
            setDateOffset(next)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedSector(sectorId: String?) {
        _selectedSectorId.value = sectorId
        if (sectorId != null) {
            clearCustomTimeRange()
        }
    }

    fun setCustomTimeRange(startH: Int, startM: Int, endH: Int, endM: Int) {
        val startMins = startH * 60 + startM
        val endMins = endH * 60 + endM
        _customTimeRange.value = Pair(startMins, endMins)
        _selectedSectorId.value = null // Custom time overrides sector chip

        fun formatTime(hour: Int, min: Int): String {
            val amPm = if (hour >= 12) "PM" else "AM"
            val h12 = when (hour % 12) {
                0 -> 12
                else -> hour % 12
            }
            return String.format(Locale.getDefault(), "%02d:%02d %s", h12, min, amPm)
        }
        _customTimeRangeFormatted.value = "${formatTime(startH, startM)} – ${formatTime(endH, endM)}"
    }

    fun clearCustomTimeRange() {
        _customTimeRange.value = null
        _customTimeRangeFormatted.value = null
    }

    fun resetAllTimeFilters() {
        _selectedSectorId.value = null
        clearCustomTimeRange()
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

    fun refreshSectors() {
        _sectors.value = sectorStore.getSectors()
    }

    fun saveConfiguredSectors(newSectors: List<TimeSector>) {
        sectorStore.saveSectors(newSectors)
        refreshSectors()
        loadLogs()
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
