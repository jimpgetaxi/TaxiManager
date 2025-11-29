package com.taxipro.manager.data.repository

import com.taxipro.manager.data.local.dao.TaxiDao
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.data.local.entity.Shift
import kotlinx.coroutines.flow.Flow

class TaxiRepository(private val taxiDao: TaxiDao) {
    val activeShift: Flow<Shift?> = taxiDao.getActiveShift()

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

    suspend fun addJob(shiftId: Long, revenue: Double, notes: String?, currentOdometer: Double?) {
        val job = Job(
            shiftId = shiftId,
            revenue = revenue,
            notes = notes,
            currentOdometer = currentOdometer
        )
        taxiDao.insertJob(job)
    }

    suspend fun updateJob(job: Job) {
        taxiDao.updateJob(job)
    }

    fun getJobsForShift(shiftId: Long): Flow<List<Job>> = taxiDao.getJobsForShift(shiftId)
    
    fun getShiftRevenue(shiftId: Long): Flow<Double?> = taxiDao.getTotalRevenueForShift(shiftId)
}
