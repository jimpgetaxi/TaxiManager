package com.taxipro.manager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.taxipro.manager.data.local.dao.ExpenseDao
import com.taxipro.manager.data.local.dao.RecurringExpenseDao
import com.taxipro.manager.data.local.dao.TaxiDao
import com.taxipro.manager.data.local.entity.Expense
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.data.local.entity.RecurringExpense
import com.taxipro.manager.data.local.entity.Shift

import androidx.room.TypeConverters

@Database(entities = [Shift::class, Job::class, Expense::class, RecurringExpense::class], version = 7, exportSchema = false)
@TypeConverters(Converters::class)
abstract class TaxiDatabase : RoomDatabase() {
    abstract fun taxiDao(): TaxiDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

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
