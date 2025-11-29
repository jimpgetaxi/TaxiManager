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

import com.taxipro.manager.data.local.entity.ShiftSummary

data class DashboardUiState(
    val activeShift: Shift? = null,
    val currentJobs: List<Job> = emptyList(),
    val currentRevenue: Double = 0.0,
    val totalReceipts: Double = 0.0,
    val totalVat: Double = 0.0,
    val currentMileage: Double = 0.0,
    val vehicleCostForCurrentShift: Double = 0.0,
    val currencySymbol: String = "€"
)

class MainViewModel(
    private val repository: TaxiRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _activeShift = repository.activeShift
    private val _currencySymbol = userPreferencesRepository.currencySymbol

    val initialHistoricalKm = userPreferencesRepository.initialHistoricalKm
    val initialHistoricalExpenses = userPreferencesRepository.initialHistoricalExpenses

    val costPerKm: StateFlow<Double> = combine(
        initialHistoricalKm,
        initialHistoricalExpenses,
        repository.getTotalKilometers()
    ) { initKm, initExp, appKm ->
        val totalKm = initKm + appKm
        if (totalKm > 0) initExp / totalKm else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val shiftHistory: StateFlow<List<ShiftSummary>> = repository.shiftHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        _activeShift,
        _currencySymbol,
        costPerKm
    ) { shift, currency, costPerKmValue ->
        Pair(Pair(shift, currency), costPerKmValue)
    }.flatMapLatest { (shiftAndCurrency, costPerKmValue) ->
        val (shift, currency) = shiftAndCurrency
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
                
                val vehicleCost = mileage * costPerKmValue

                DashboardUiState(
                    activeShift = shift,
                    currentJobs = jobs,
                    currentRevenue = revenue ?: 0.0,
                    totalReceipts = totalReceipts,
                    totalVat = totalVat,
                    currentMileage = if (mileage > 0) mileage else 0.0,
                    vehicleCostForCurrentShift = vehicleCost,
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
        val currentCostPerKm = costPerKm.value
        val distance = endOdometer - shift.startOdometer
        val vehicleCost = if (distance > 0) distance * currentCostPerKm else 0.0
        
        viewModelScope.launch {
            repository.endShift(shift, endOdometer, vehicleCost)
        }
    }

    fun addJob(revenue: Double, receiptAmount: Double?, notes: String?, currentOdometer: Double?) {
        val shift = uiState.value.activeShift ?: return
        addJobToShift(shift.id, revenue, receiptAmount, notes, currentOdometer)
    }

    fun addJobToShift(shiftId: Long, revenue: Double, receiptAmount: Double?, notes: String?, currentOdometer: Double?) {
        viewModelScope.launch {
            repository.addJob(shiftId, revenue, receiptAmount, notes, currentOdometer)
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

    fun updateShift(shift: Shift) {
        viewModelScope.launch {
            repository.updateShift(shift)
        }
    }

    fun updateShiftDetails(shift: Shift) {
        viewModelScope.launch {
            repository.updateShift(shift)
        }
    }

    fun getShift(shiftId: Long): Flow<Shift?> = repository.getShiftById(shiftId)
    fun getJobs(shiftId: Long): Flow<List<Job>> = repository.getJobsForShift(shiftId)

    fun updateCurrency(symbol: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCurrencySymbol(symbol)
        }
    }

    fun updateVehicleSettings(km: Double, expenses: Double) {
        viewModelScope.launch {
            userPreferencesRepository.setInitialHistoricalKm(km)
            userPreferencesRepository.setInitialHistoricalExpenses(expenses)
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
