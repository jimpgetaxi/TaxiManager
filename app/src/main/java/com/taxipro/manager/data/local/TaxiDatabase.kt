package com.taxipro.manager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.taxipro.manager.data.local.dao.TaxiDao
import com.taxipro.manager.data.local.entity.Job
import com.taxipro.manager.data.local.entity.Shift

@Database(entities = [Shift::class, Job::class], version = 3, exportSchema = false)
abstract class TaxiDatabase : RoomDatabase() {
    abstract fun taxiDao(): TaxiDao

    companion object {
        @Volatile
        private var INSTANCE: TaxiDatabase? = null

        fun getDatabase(context: Context): TaxiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaxiDatabase::class.java,
                    "taxi_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
