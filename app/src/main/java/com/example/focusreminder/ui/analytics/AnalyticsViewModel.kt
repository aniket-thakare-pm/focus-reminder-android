package com.example.focusreminder.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusreminder.domain.model.AnalyticsSummary
import com.example.focusreminder.domain.model.AnalyticsTimeframe
import com.example.focusreminder.domain.model.BufferGoal
import com.example.focusreminder.platform.analytics.AnalyticsRepository
import com.example.focusreminder.platform.analytics.SectorConfigStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AnalyticsRepository.getInstance(application)
    private val sectorStore = SectorConfigStore.getInstance(application)

    private val _summary = MutableStateFlow<AnalyticsSummary?>(null)
    val summary: StateFlow<AnalyticsSummary?> = _summary.asStateFlow()

    private val _timeframe = MutableStateFlow(AnalyticsTimeframe.TODAY)
    val timeframe: StateFlow<AnalyticsTimeframe> = _timeframe.asStateFlow()

    private val _dateOffset = MutableStateFlow(0)
    val dateOffset: StateFlow<Int> = _dateOffset.asStateFlow()

    private val _selectedPackage = MutableStateFlow<String?>(null)
    val selectedPackage: StateFlow<String?> = _selectedPackage.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isControlledOnly = MutableStateFlow(false)
    val isControlledOnly: StateFlow<Boolean> = _isControlledOnly.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    fun setTimeframe(tf: AnalyticsTimeframe) {
        _timeframe.value = tf
        _dateOffset.value = 0
        loadData()
    }

    fun setDateOffset(delta: Int) {
        _dateOffset.value += delta
        loadData()
    }

    fun resetDateOffset() {
        _dateOffset.value = 0
        loadData()
    }

    fun focusPackage(pkg: String) {
        _selectedPackage.value = pkg
        _selectedCategory.value = null
        _isControlledOnly.value = false
        loadData()
    }

    fun focusCategory(cat: String) {
        _selectedCategory.value = cat
        _selectedPackage.value = null
        _isControlledOnly.value = false
        loadData()
    }

    fun setControlledOnly(controlled: Boolean) {
        _isControlledOnly.value = controlled
        _selectedPackage.value = null
        _selectedCategory.value = null
        loadData()
    }

    fun clearScope() {
        _selectedPackage.value = null
        _selectedCategory.value = null
        _isControlledOnly.value = false
        loadData()
    }

    fun saveBufferGoal(goal: BufferGoal) {
        sectorStore.saveBufferGoal(goal)
        loadData()
    }

    fun deleteBufferGoal(goalId: String) {
        sectorStore.deleteBufferGoal(goalId)
        loadData()
    }

    fun isPackageExcluded(packageName: String): Boolean {
        val restrictionRepo = com.example.focusreminder.platform.AppRestrictionRepository.getInstance(getApplication())
        return restrictionRepo.isExcludedFromAnalytics(packageName)
    }

    fun toggleExcludePackage(packageName: String) {
        val restrictionRepo = com.example.focusreminder.platform.AppRestrictionRepository.getInstance(getApplication())
        val currentlyExcluded = restrictionRepo.isExcludedFromAnalytics(packageName)
        restrictionRepo.setExcludedFromAnalytics(packageName, !currentlyExcluded)
        if (!currentlyExcluded) {
            clearScope()
        } else {
            loadData()
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            val data = withContext(Dispatchers.IO) {
                repo.getAnalyticsSummary(
                    timeframe = _timeframe.value,
                    dateOffset = _dateOffset.value,
                    filterPackage = _selectedPackage.value,
                    filterCategory = _selectedCategory.value,
                    isControlledOnly = _isControlledOnly.value
                )
            }
            _summary.value = data
            _isLoading.value = false
        }
    }
}
