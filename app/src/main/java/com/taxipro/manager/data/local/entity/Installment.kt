package com.taxipro.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installments")
data class Installment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val totalAmount: Double,
    val monthlyAmount: Double,
    val totalInstallments: Int,
    val remainingInstallments: Int,
    val startDate: Long,
    val lastPaymentDate: Long? = null,
    val nextPaymentDate: Long
)
