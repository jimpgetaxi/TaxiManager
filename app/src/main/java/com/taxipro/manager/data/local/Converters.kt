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

    @TypeConverter
    fun fromPaymentMethod(value: com.taxipro.manager.data.local.entity.PaymentMethod): String {
        return value.name
    }

    @TypeConverter
    fun toPaymentMethod(value: String): com.taxipro.manager.data.local.entity.PaymentMethod {
        return try {
            com.taxipro.manager.data.local.entity.PaymentMethod.valueOf(value)
        } catch (e: IllegalArgumentException) {
            com.taxipro.manager.data.local.entity.PaymentMethod.CASH
        }
    }
}
