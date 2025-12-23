package com.taxipro.manager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PaymentType {
    CASH, POS, CONTRACT
}

@Entity(
    tableName = "jobs",
    foreignKeys = [
        ForeignKey(
            entity = Shift::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("shiftId")]
)
data class Job(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val revenue: Double,
    val receiptAmount: Double? = null,
    val notes: String? = null,
    val currentOdometer: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val paymentType: PaymentType = PaymentType.CASH,
    val isPaid: Boolean = true // Defaults to true for backward compatibility (assumed Cash)
)