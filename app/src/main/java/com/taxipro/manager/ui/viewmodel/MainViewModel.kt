package com.taxipro.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.data.local.entity.Shift
import com.taxipro.manager.data.repository.TaxiRepository
import com.taxipro.manager.data.repository.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val activeShift: Shift? = null,
    val currentJobs: List<Job> = emptyList(),
    val currentRevenue: Double = 0.0,
    val totalReceipts: Double = 0.0,
    val totalVat: Double = 0.0,
    val currentMileage: Double = 0.0,
    val currencySymbol: String = "€"
)

class MainViewModel(
    private val repository: TaxiRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _activeShift = repository.activeShift
    private val _currencySymbol = userPreferencesRepository.currencySymbol

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        _activeShift,
        _currencySymbol
    ) { shift, currency ->
        Pair(shift, currency)
    }.flatMapLatest { (shift, currency) ->
        if (shift == null) {
            flowOf(DashboardUiState(currencySymbol = currency))
        } else {
            combine(
                repository.getJobsForShift(shift.id),
                repository.getShiftRevenue(shift.id)
            ) { jobs, revenue ->
                val maxJobOdometer = jobs.mapNotNull { it.currentOdometer }.maxOrNull()
                val mileage = if (maxJobOdometer != null) {
                     maxJobOdometer - shift.startOdometer
                } else {
                    0.0
                }

                val totalReceipts = jobs.sumOf { it.receiptAmount ?: 0.0 }
                // Calculate VAT (13%) included in the gross receipt amount
                // Formula: Tax = Gross * (Rate / (1 + Rate)) -> Gross * (0.13 / 1.13)
                val totalVat = (totalReceipts / 1.13) * 0.13
                
                DashboardUiState(
                    activeShift = shift,
                    currentJobs = jobs,
                    currentRevenue = revenue ?: 0.0,
                    totalReceipts = totalReceipts,
                    totalVat = totalVat,
                    currentMileage = if (mileage > 0) mileage else 0.0,
                    currencySymbol = currency
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun startShift(startOdometer: Double) {
        viewModelScope.launch {
            repository.startShift(startOdometer)
        }
    }

    fun endShift(endOdometer: Double) {
        val shift = uiState.value.activeShift ?: return
        viewModelScope.launch {
            repository.endShift(shift, endOdometer)
        }
    }

    fun addJob(revenue: Double, receiptAmount: Double?, notes: String?, currentOdometer: Double?) {
        val shift = uiState.value.activeShift ?: return
        viewModelScope.launch {
            repository.addJob(shift.id, revenue, receiptAmount, notes, currentOdometer)
        }
    }

    fun updateJob(job: Job, revenue: Double, receiptAmount: Double?, notes: String?, currentOdometer: Double?) {
        viewModelScope.launch {
            repository.updateJob(
                job.copy(
                    revenue = revenue,
                    receiptAmount = receiptAmount,
                    notes = notes,
                    currentOdometer = currentOdometer
                )
            )
        }
    }

    fun updateCurrency(symbol: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCurrencySymbol(symbol)
        }
    }
}

class MainViewModelFactory(
    private val repository: TaxiRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
