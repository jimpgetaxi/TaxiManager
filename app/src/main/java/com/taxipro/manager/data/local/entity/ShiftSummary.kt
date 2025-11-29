package com.taxipro.manager.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class ShiftSummary(
    @Embedded val shift: Shift,
    val totalRevenue: Double?,
    val totalReceipts: Double?
)