package com.taxipro.manager.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import com.taxipro.manager.data.local.TaxiDatabase
import com.taxipro.manager.data.local.dao.ExpenseDao
import com.taxipro.manager.data.local.dao.TaxiDao
import com.taxipro.manager.data.local.entity.Expense
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.data.local.entity.PaymentType
import com.taxipro.manager.data.local.entity.Shift
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.taxipro.manager.data.local.entity.ShiftSummary

import com.taxipro.manager.data.local.dao.RecurringExpenseDao
import com.taxipro.manager.data.local.entity.RecurringExpense

class TaxiRepository(
    private val database: TaxiDatabase,
    private val taxiDao: TaxiDao,
    private val expenseDao: ExpenseDao,
    private val recurringExpenseDao: RecurringExpenseDao
) {
    val activeShift: Flow<Shift?> = taxiDao.getActiveShift()
    val shiftHistory: Flow<List<ShiftSummary>> = taxiDao.getShiftSummaries()
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allRecurringExpenses: Flow<List<RecurringExpense>> = recurringExpenseDao.getAllRecurringExpenses()

    fun getShiftById(shiftId: Long): Flow<Shift?> = taxiDao.getShiftById(shiftId)

    // Shift Operations
    suspend fun startShift(startOdometer: Double) {
        val shift = Shift(
            startTime = System.currentTimeMillis(),
            startOdometer = startOdometer
        )
        taxiDao.insertShift(shift)
    }

    suspend fun endShift(shift: Shift, endOdometer: Double, vehicleCost: Double) {
        val updatedShift = shift.copy(
            endTime = System.currentTimeMillis(),
            endOdometer = endOdometer,
            vehicleCost = vehicleCost,
            isActive = false
        )
        taxiDao.updateShift(updatedShift)
    }

    suspend fun updateShift(shift: Shift) {
        taxiDao.updateShift(shift)
    }

    // Job Operations
    suspend fun addJob(
        shiftId: Long, 
        revenue: Double, 
        receiptAmount: Double?, 
        notes: String?, 
        currentOdometer: Double?,
        paymentType: PaymentType = PaymentType.CASH,
        isPaid: Boolean = true
    ) {
        val job = Job(
            shiftId = shiftId,
            revenue = revenue,
            receiptAmount = receiptAmount,
            notes = notes,
            currentOdometer = currentOdometer,
            paymentType = paymentType,
            isPaid = isPaid
        )
        taxiDao.insertJob(job)
    }

    suspend fun updateJob(job: Job) {
        taxiDao.updateJob(job)
    }

    fun getUnpaidJobs(): Flow<List<Job>> = taxiDao.getUnpaidJobs()

    // Expense Operations
    suspend fun addExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    // Recurring Expense Operations
    suspend fun addRecurringExpense(recurringExpense: RecurringExpense) {
        recurringExpenseDao.insertRecurringExpense(recurringExpense)
    }

    suspend fun updateRecurringExpense(recurringExpense: RecurringExpense) {
        recurringExpenseDao.updateRecurringExpense(recurringExpense)
    }

    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense) {
        recurringExpenseDao.deleteRecurringExpense(recurringExpense)
    }

    fun getTotalExpensesForCostPerKm(): Flow<Double> = expenseDao.getTotalExpensesForCostPerKm().map { it ?: 0.0 }
    
    fun getTotalDeductibleVat(): Flow<Double> = expenseDao.getTotalDeductibleVat().map { it ?: 0.0 }

    // Stats
    fun getTotalKilometers(): Flow<Double> = taxiDao.getTotalKilometers().map { it ?: 0.0 }

    fun getJobsForShift(shiftId: Long): Flow<List<Job>> = taxiDao.getJobsForShift(shiftId)
    
    fun getShiftRevenue(shiftId: Long): Flow<Double?> = taxiDao.getTotalRevenueForShift(shiftId)

    // Reports
    fun getShiftSummariesInRange(start: Long, end: Long): Flow<List<ShiftSummary>> = taxiDao.getShiftSummariesInRange(start, end)

    fun getExpensesInRange(start: Long, end: Long): Flow<List<Expense>> = expenseDao.getExpensesInRange(start, end)

    suspend fun checkpoint() {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
    }
}
