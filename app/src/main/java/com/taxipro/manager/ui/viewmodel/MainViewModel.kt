package com.taxipro.manager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.taxipro.manager.data.local.entity.*
import com.taxipro.manager.data.repository.TaxiRepository
import com.taxipro.manager.data.repository.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ReportPeriod { MONTHLY, YEARLY }

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
    val totalVat: Double = 0.0,
    val totalDeductibleVat: Double = 0.0,
    val payableVat: Double = 0.0,
    val currentMileage: Double = 0.0,
    val vehicleCostForCurrentShift: Double = 0.0,
    val currencySymbol: String = "€",
    val creditCardDebt: Double = 0.0
)

data class ForecastUiState(
    val months: List<MonthForecast> = emptyList(),
    val currencySymbol: String = "€"
)

data class MonthForecast(
    val monthName: String,
    val totalExpenses: Double,
    val recurringAmount: Double,
    val installmentsAmount: Double
)

class MainViewModel(
    private val repository: TaxiRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _reportPeriod = MutableStateFlow(ReportPeriod.MONTHLY)
    private val _reportSelectedDate = MutableStateFlow(System.currentTimeMillis())
    private val _currencySymbol = userPreferencesRepository.currencySymbol

    val allExpenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allRecurringExpenses = repository.allRecurringExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val unpaidJobs = repository.getUnpaidJobs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allInstallments = repository.allInstallments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val shiftHistory: StateFlow<List<ShiftSummary>> = repository.shiftHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val initialHistoricalKm = userPreferencesRepository.initialHistoricalKm
    val initialHistoricalExpenses = userPreferencesRepository.initialHistoricalExpenses

    val costPerKm: StateFlow<Double> = combine(
        initialHistoricalKm,
        initialHistoricalExpenses,
        repository.getTotalKilometers(),
        repository.getTotalExpensesForCostPerKm()
    ) { ik, ie, ak, ae ->
        val tKm = (ik ?: 0.0) + (ak ?: 0.0)
        val tExp = (ie ?: 0.0) + (ae ?: 0.0)
        if (tKm > 0.0) tExp / tKm else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportsUiState: StateFlow<ReportsUiState> = combine(_reportPeriod, _reportSelectedDate, _currencySymbol) { p, d, c ->
        val (s, e) = calculateDateRange(p, d)
        repository.getShiftSummariesInRange(s, e).combine(repository.getExpensesInRange(s, e)) { sh, ex ->
            val inc = sh.sumOf { it.totalRevenue ?: 0.0 }
            val exp = ex.sumOf { it.amount }
            val rec = sh.sumOf { it.totalReceipts ?: 0.0 }
            val vC = (rec / 1.13) * 0.13
            val vD = ex.sumOf { it.vatAmount }
            ReportsUiState(p, d, inc, exp, inc - exp, vC, vD, vC - vD, c)
        }
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    val forecastUiState: StateFlow<ForecastUiState> = combine(allRecurringExpenses, allInstallments, _currencySymbol) { rec, ins, cur ->
        val list = mutableListOf<MonthForecast>()
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("el-GR"))
        for (i in 0 until 12) {
            val m = cal.get(Calendar.MONTH)
            val y = cal.get(Calendar.YEAR)
            val rA = rec.filter { it.frequency == "MONTHLY" || (it.frequency == "YEARLY" && it.dayOfMonth == cal.get(Calendar.DAY_OF_YEAR)) }.sumOf { it.amount }
            val iA = ins.filter {
                val sCal = Calendar.getInstance().apply { timeInMillis = it.startDate }
                val diff = (y - sCal.get(Calendar.YEAR)) * 12 + (m - sCal.get(Calendar.MONTH))
                diff >= 0 && diff < it.totalInstallments
            }.sumOf { it.monthlyAmount }
            list.add(MonthForecast(fmt.format(cal.time), rA + iA, rA, iA))
            cal.add(Calendar.MONTH, 1)
        }
        ForecastUiState(list, cur)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ForecastUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = combine(repository.activeShift, _currencySymbol, costPerKm, repository.getTotalDeductibleVat(), allInstallments) { s, c, ck, dv, ins ->
        val debt = ins.sumOf { it.monthlyAmount * it.remainingInstallments }
        if (s == null) {
            flowOf(DashboardUiState(null, emptyList(), 0.0, 0.0, 0.0, dv, -dv, 0.0, 0.0, c, debt))
        } else {
            repository.getJobsForShift(s.id).combine(repository.getShiftRevenue(s.id)) { j, r ->
                val mO = j.mapNotNull { it.currentOdometer }.maxOrNull()
                val ml = if (mO != null) mO - s.startOdometer else 0.0
                val rc = j.sumOf { it.receiptAmount ?: 0.0 }
                val v = (rc / 1.13) * 0.13
                DashboardUiState(s, j, r ?: 0.0, rc, v, dv, v - dv, if (ml > 0) ml else 0.0, ml * ck, c, debt)
            }
        }
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    private fun calculateDateRange(p: ReportPeriod, d: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = d }
        val s: Long
        val e: Long
        if (p == ReportPeriod.MONTHLY) {
            cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            s = cal.timeInMillis
            cal.add(Calendar.MONTH, 1); cal.add(Calendar.MILLISECOND, -1)
            e = cal.timeInMillis
        } else {
            cal.set(Calendar.MONTH, Calendar.JANUARY); cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            s = cal.timeInMillis
            cal.add(Calendar.YEAR, 1); cal.add(Calendar.MILLISECOND, -1)
            e = cal.timeInMillis
        }
        return s to e
    }

    fun setReportPeriod(p: ReportPeriod) { _reportPeriod.value = p }
    fun setReportDate(d: Long) { _reportSelectedDate.value = d }

    fun addExpense(desc: String, amt: Double, vAmt: Double, aff: Boolean, meth: PaymentMethod, insts: Int) {
        viewModelScope.launch {
            if (meth == PaymentMethod.CREDIT_CARD && insts > 1) {
                val mon = amt / insts
                val iId = repository.addInstallment(Installment(0, desc, amt, mon, insts, insts, System.currentTimeMillis(), null, System.currentTimeMillis()))
                repository.addExpense(Expense(0, System.currentTimeMillis(), "$desc (1/$insts)", mon, vAmt / insts, aff, PaymentMethod.CREDIT_CARD, iId))
            } else {
                repository.addExpense(Expense(0, System.currentTimeMillis(), desc, amt, vAmt, aff, meth))
            }
        }
    }

    fun applyRecurringExpense(re: RecurringExpense, d: Long) { addExpense(re.description, re.amount, re.vatAmount, re.affectsCostPerKm, PaymentMethod.CASH, 1) }
    fun deleteExpense(e: Expense) { viewModelScope.launch { repository.deleteExpense(e) } }
    fun addRecurringExpense(re: RecurringExpense) { viewModelScope.launch { repository.addRecurringExpense(re) } }
    fun updateRecurringExpense(re: RecurringExpense) { viewModelScope.launch { repository.updateRecurringExpense(re) } }
    fun deleteRecurringExpense(re: RecurringExpense) { viewModelScope.launch { repository.deleteRecurringExpense(re) } }
    fun startShift(o: Double) { viewModelScope.launch { repository.startShift(o) } }
    fun endShift(o: Double) {
        val s = uiState.value.activeShift ?: return
        val d = o - s.startOdometer
        val c = if (d > 0) d * costPerKm.value else 0.0
        viewModelScope.launch { repository.endShift(s, o, c) }
    }
    fun addJobToShift(sid: Long, rev: Double, rec: Double?, n: String?, odo: Double?, pt: PaymentType, ip: Boolean) { viewModelScope.launch { repository.addJob(sid, rev, rec, n, odo, pt, ip) } }
    fun addJob(rev: Double, rec: Double?, n: String?, odo: Double?, pt: PaymentType, ip: Boolean) { val s = uiState.value.activeShift ?: return; addJobToShift(s.id, rev, rec, n, odo, pt, ip) }
    fun updateJob(j: Job, rev: Double, rec: Double?, n: String?, odo: Double?, pt: PaymentType?, ip: Boolean?) { viewModelScope.launch { repository.updateJob(j.copy(revenue = rev, receiptAmount = rec, notes = n, currentOdometer = odo, paymentType = pt ?: j.paymentType, isPaid = ip ?: j.isPaid)) } }
    fun markJobAsPaid(j: Job) { viewModelScope.launch { repository.updateJob(j.copy(isPaid = true)) } }
    fun updateShiftDetails(s: Shift) { viewModelScope.launch { repository.updateShift(s) } }
    fun getShift(id: Long): Flow<Shift?> = repository.getShiftById(id)
    fun getJobs(id: Long): Flow<List<Job>> = repository.getJobsForShift(id)
    fun getReportData(): Flow<Pair<List<ShiftSummary>, List<Expense>>> {
        val (s, e) = calculateDateRange(_reportPeriod.value, _reportSelectedDate.value)
        return repository.getShiftSummariesInRange(s, e).combine(repository.getExpensesInRange(s, e)) { sh, ex -> sh to ex }
    }
    fun updateCurrency(s: String) { viewModelScope.launch { userPreferencesRepository.setCurrencySymbol(s) } }
    fun updateVehicleSettings(km: Double, exp: Double) { viewModelScope.launch { userPreferencesRepository.setInitialHistoricalKm(km) ; userPreferencesRepository.setInitialHistoricalExpenses(exp) } }
    suspend fun checkpoint() { repository.checkpoint() }
}

class MainViewModelFactory(private val r: TaxiRepository, private val upr: UserPreferencesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) { @Suppress("UNCHECKED_CAST") return MainViewModel(r, upr) as T }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}