package com.taxipro.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val description: String, // e.g., "Καύσιμα", "Service"
    val amount: Double, // Το συνολικό ποσό πληρωμής
    val vatAmount: Double = 0.0, // Το ποσό ΦΠΑ που εκπίπτει (αν υπάρχει)
    val affectsCostPerKm: Boolean = true // Αν επηρεάζει τον υπολογισμό κόστους/χλμ
)
