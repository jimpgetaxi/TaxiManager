package com.taxipro.manager.data.repository

import com.taxipro.manager.data.local.dao.TaxiDao
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.data.local.entity.Shift
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.taxipro.manager.data.local.entity.ShiftSummary

class TaxiRepository(private val taxiDao: TaxiDao) {
    val activeShift: Flow<Shift?> = taxiDao.getActiveShift()
    val shiftHistory: Flow<List<ShiftSummary>> = taxiDao.getShiftSummaries()

    fun getShiftById(shiftId: Long): Flow<Shift?> = taxiDao.getShiftById(shiftId)

    suspend fun startShift(startOdometer: Double) {
        val shift = Shift(
            startTime = System.currentTimeMillis(),
            startOdometer = startOdometer
        )
        taxiDao.insertShift(shift)
    }

    suspend fun endShift(shift: Shift, endOdometer: Double) {
        val updatedShift = shift.copy(
            endOdometer = endOdometer,
            isActive = false
        )
        taxiDao.updateShift(updatedShift)
    }

    suspend fun updateShift(shift: Shift) {
        taxiDao.updateShift(shift)
    }

    suspend fun addJob(shiftId: Long, revenue: Double, receiptAmount: Double?, notes: String?, currentOdometer: Double?) {
        val job = Job(
            shiftId = shiftId,
            revenue = revenue,
            receiptAmount = receiptAmount,
            notes = notes,
            currentOdometer = currentOdometer
        )
        taxiDao.insertJob(job)
    }

    suspend fun updateJob(job: Job) {
        taxiDao.updateJob(job)
    }

    fun getTotalKilometers(): Flow<Double> = taxiDao.getTotalKilometers().map { it ?: 0.0 }

    fun getJobsForShift(shiftId: Long): Flow<List<Job>> = taxiDao.getJobsForShift(shiftId)
    
    fun getShiftRevenue(shiftId: Long): Flow<Double?> = taxiDao.getTotalRevenueForShift(shiftId)
}
