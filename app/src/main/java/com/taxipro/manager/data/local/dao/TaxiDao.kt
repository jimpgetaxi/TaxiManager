package com.taxipro.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.data.local.entity.Shift
import com.taxipro.manager.data.local.entity.ShiftSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface TaxiDao {
    // Shift Operations
    @Insert
    suspend fun insertShift(shift: Shift): Long

    @Update
    suspend fun updateShift(shift: Shift)

    @Query("SELECT * FROM shifts WHERE isActive = 1 LIMIT 1")
    fun getActiveShift(): Flow<Shift?>

    @Query("SELECT * FROM shifts ORDER BY startTime DESC")
    fun getAllShifts(): Flow<List<Shift>>

    @Query("""
        SELECT 
            shifts.*, 
            SUM(jobs.revenue) as totalRevenue, 
            SUM(jobs.receiptAmount) as totalReceipts 
        FROM shifts 
        LEFT JOIN jobs ON shifts.id = jobs.shiftId 
        GROUP BY shifts.id 
        ORDER BY shifts.startTime DESC
    """)
    fun getShiftSummaries(): Flow<List<ShiftSummary>>

    @Query("SELECT * FROM shifts WHERE id = :shiftId")
    fun getShiftById(shiftId: Long): Flow<Shift?>

    // Job Operations
    @Insert
    suspend fun insertJob(job: Job)

    @Update
    suspend fun updateJob(job: Job)

    @Query("SELECT * FROM jobs WHERE shiftId = :shiftId ORDER BY timestamp DESC")
    fun getJobsForShift(shiftId: Long): Flow<List<Job>>

    @Query("SELECT SUM(revenue) FROM jobs WHERE shiftId = :shiftId")
    fun getTotalRevenueForShift(shiftId: Long): Flow<Double?>
    
    @Query("SELECT SUM(receiptAmount) FROM jobs WHERE shiftId = :shiftId")
    fun getTotalReceiptsForShift(shiftId: Long): Flow<Double?>
}
