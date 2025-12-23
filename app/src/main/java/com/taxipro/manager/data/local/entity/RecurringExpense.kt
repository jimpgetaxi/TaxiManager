package com.taxipro.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expenses")
data class RecurringExpense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val amount: Double,
    val vatAmount: Double = 0.0,
    val affectsCostPerKm: Boolean = true,
    val frequency: String = "MONTHLY", // "MONTHLY", "YEARLY", "QUARTERLY"
    val dayOfMonth: Int = 1
)
