package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.EmergencyProfile
import com.example.data.model.MedicalRecord

@Database(
    entities = [MedicalRecord::class, EmergencyProfile::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicalDao(): MedicalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medical_records_database"
                )
                .fallbackToDestructiveMigration() // Simple fallback for any schema changes during prototyping
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
