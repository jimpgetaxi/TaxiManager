package com.taxipro.manager.data.local.dao

import androidx.room.*
import com.taxipro.manager.data.local.entity.Installment
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments ORDER BY nextPaymentDate ASC")
    fun getAllInstallments(): Flow<List<Installment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: Installment): Long

    @Update
    suspend fun updateInstallment(installment: Installment)

    @Delete
    suspend fun deleteInstallment(installment: Installment)

    @Query("SELECT * FROM installments WHERE remainingInstallments > 0")
    fun getActiveInstallments(): Flow<List<Installment>>
}
