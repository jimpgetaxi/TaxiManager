package com.taxipro.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.taxipro.manager.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getExpensesInRange(start: Long, end: Long): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE affectsCostPerKm = 1")
    fun getTotalExpensesForCostPerKm(): Flow<Double?>

    @Query("SELECT SUM(vatAmount) FROM expenses")
    fun getTotalDeductibleVat(): Flow<Double?>
}
