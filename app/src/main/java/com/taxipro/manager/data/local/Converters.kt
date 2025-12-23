package com.taxipro.manager.data.local

import androidx.room.TypeConverter
import com.taxipro.manager.data.local.entity.PaymentType

class Converters {
    @TypeConverter
    fun fromPaymentType(value: PaymentType): String {
        return value.name
    }

    @TypeConverter
    fun toPaymentType(value: String): PaymentType {
        return try {
            PaymentType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            PaymentType.CASH // Fallback
        }
    }
}
