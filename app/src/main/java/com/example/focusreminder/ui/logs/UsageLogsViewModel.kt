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

    fun loadLogs() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repo.getUsageLogs(_selectedDateOffset.value)
            _summary.value = result
            _isLoading.value = false
        }
    }
}
