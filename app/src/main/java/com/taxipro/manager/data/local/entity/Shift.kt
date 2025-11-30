package com.taxipro.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val startOdometer: Double,
    val endOdometer: Double? = null,
    val vehicleCost: Double? = null,
    val isActive: Boolean = true
)
