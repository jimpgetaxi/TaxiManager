package com.taxipro.manager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.taxipro.manager.data.local.dao.ExpenseDao
import com.taxipro.manager.data.local.dao.InstallmentDao
import com.taxipro.manager.data.local.dao.RecurringExpenseDao
import com.taxipro.manager.data.local.dao.TaxiDao
import com.taxipro.manager.data.local.entity.Expense
import com.taxipro.manager.data.local.entity.Installment
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.data.local.entity.RecurringExpense
import com.taxipro.manager.data.local.entity.Shift

@Database(entities = [Shift::class, Job::class, Expense::class, RecurringExpense::class, Installment::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TaxiDatabase : RoomDatabase() {
    abstract fun taxiDao(): TaxiDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun installmentDao(): InstallmentDao

    companion object {
        @Volatile
        private var INSTANCE: TaxiDatabase? = null

        fun getDatabase(context: Context): TaxiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaxiDatabase::class.java,
                    "taxi_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
