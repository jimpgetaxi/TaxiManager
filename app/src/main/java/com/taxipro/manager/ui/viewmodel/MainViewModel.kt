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

import com.taxipro.manager.data.local.entity.Expense

import com.taxipro.manager.data.local.entity.ShiftSummary

import java.util.Calendar

enum class ReportPeriod {
    MONTHLY, YEARLY
}

data class ReportsUiState(
    val reportPeriod: ReportPeriod = ReportPeriod.MONTHLY,
    val selectedDate: Long = System.currentTimeMillis(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netIncome: Double = 0.0,
    val vatCollected: Double = 0.0,
    val vatDeductible: Double = 0.0,
    val vatPayable: Double = 0.0,
    val currencySymbol: String = "€"
)

data class DashboardUiState(
    val activeShift: Shift? = null,
    val currentJobs: List<Job> = emptyList(),
    val currentRevenue: Double = 0.0,
    val totalReceipts: Double = 0.0,
    val totalVat: Double = 0.0, // Revenue VAT
    val totalDeductibleVat: Double = 0.0, // Expense VAT
    val payableVat: Double = 0.0, // Revenue VAT - Expense VAT
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

    // Reports State
    private val _reportPeriod = MutableStateFlow(ReportPeriod.MONTHLY)
    private val _reportSelectedDate = MutableStateFlow(System.currentTimeMillis())

    val initialHistoricalKm = userPreferencesRepository.initialHistoricalKm
    val initialHistoricalExpenses = userPreferencesRepository.initialHistoricalExpenses
    
    val allExpenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val costPerKm: StateFlow<Double> = combine(
        initialHistoricalKm,
        initialHistoricalExpenses,
        repository.getTotalKilometers(),
        repository.getTotalExpensesForCostPerKm()
    ) { initKm, initExp, appKm, appExp ->
        val totalKm = initKm + appKm
        val totalExp = initExp + appExp
        if (totalKm > 0) totalExp / totalKm else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val shiftHistory: StateFlow<List<ShiftSummary>> = repository.shiftHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportsUiState: StateFlow<ReportsUiState> = combine(
        _reportPeriod,
        _reportSelectedDate,
        _currencySymbol
    ) { period, date, currency ->
        Triple(period, date, currency)
    }.flatMapLatest { (period, date, currency) ->
        val (start, end) = calculateDateRange(period, date)
        
        combine(
            repository.getShiftSummariesInRange(start, end),
            repository.getExpensesInRange(start, end)
        ) { shifts, expenses ->
            val totalIncome = shifts.sumOf { it.totalRevenue ?: 0.0 }
            val totalExpenses = expenses.sumOf { it.amount }
            val totalReceipts = shifts.sumOf { it.totalReceipts ?: 0.0 }
            
            // VAT Calculation (assuming 13% included in receipts)
            val vatCollected = (totalReceipts / 1.13) * 0.13
            val vatDeductible = expenses.sumOf { it.vatAmount }
            
            ReportsUiState(
                reportPeriod = period,
                selectedDate = date,
                totalIncome = totalIncome,
                totalExpenses = totalExpenses,
                netIncome = totalIncome - totalExpenses,
                vatCollected = vatCollected,
                vatDeductible = vatDeductible,
                vatPayable = vatCollected - vatDeductible,
                currencySymbol = currency
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(
        _activeShift,
        _currencySymbol,
        costPerKm,
        repository.getTotalDeductibleVat()
    ) { shift, currency, costPerKmValue, deductibleVat ->
        Triple(Pair(shift, currency), costPerKmValue, deductibleVat)
    }.flatMapLatest { (shiftAndCurrency, costPerKmValue, deductibleVat) ->
        val (shift, currency) = shiftAndCurrency
        if (shift == null) {
            flowOf(DashboardUiState(currencySymbol = currency, totalDeductibleVat = deductibleVat, payableVat = 0.0 - deductibleVat))
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
                val totalRevenueVat = (totalReceipts / 1.13) * 0.13
                
                val payableVat = totalRevenueVat - deductibleVat

                DashboardUiState(
                    activeShift = shift,
                    currentJobs = jobs,
                    currentRevenue = revenue ?: 0.0,
                    totalReceipts = totalReceipts,
                    totalVat = totalRevenueVat,
                    totalDeductibleVat = deductibleVat,
                    payableVat = payableVat,
                    currentMileage = if (mileage > 0) mileage else 0.0,
                    vehicleCostForCurrentShift = mileage * costPerKmValue,
                    currencySymbol = currency
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private fun calculateDateRange(period: ReportPeriod, dateInMillis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateInMillis
        
        val start: Long
        val end: Long
        
        when (period) {
            ReportPeriod.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                end = calendar.timeInMillis
            }
            ReportPeriod.YEARLY -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
                
                calendar.add(Calendar.YEAR, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                end = calendar.timeInMillis
            }
        }
        return start to end
    }

    fun setReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
    }

    fun setReportDate(dateInMillis: Long) {
        _reportSelectedDate.value = dateInMillis
    }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            repository.addExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }


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
