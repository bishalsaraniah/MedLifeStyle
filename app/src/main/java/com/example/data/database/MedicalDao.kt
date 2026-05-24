package com.example.data.database

import androidx.room.*
import com.example.data.model.EmergencyProfile
import com.example.data.model.MedicalRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicalDao {
    // Medical Records Queries
    @Query("SELECT * FROM medical_records ORDER BY date DESC")
    fun getAllRecordsFlow(): Flow<List<MedicalRecord>>

    @Query("SELECT * FROM medical_records WHERE id = :id")
    suspend fun getRecordById(id: Int): MedicalRecord?

    @Query("SELECT * FROM medical_records WHERE category = :category ORDER BY date DESC")
    fun getRecordsByCategoryFlow(category: String): Flow<List<MedicalRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MedicalRecord): Long

    @Update
    suspend fun updateRecord(record: MedicalRecord)

    @Delete
    suspend fun deleteRecord(record: MedicalRecord)

    // Emergency Profile Queries (Fixed single row)
    @Query("SELECT * FROM emergency_profile WHERE id = 1")
    fun getEmergencyProfileFlow(): Flow<EmergencyProfile?>

    @Query("SELECT * FROM emergency_profile WHERE id = 1")
    suspend fun getEmergencyProfile(): EmergencyProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEmergencyProfile(profile: EmergencyProfile)
}
