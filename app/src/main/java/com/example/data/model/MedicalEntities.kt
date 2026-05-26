package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medical_records")
data class MedicalRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val patientName: String,
    val doctorName: String,
    val clinicOrHospital: String,
    val date: Long = System.currentTimeMillis(),
    val category: String, // e.g. "Prescription", "Lab Report", "Vaccine", "Discharge Summary", "Radiology / Scan"
    val summary: String,
    val diagnoses: String, // Comma separated items
    val prescribedMeds: String, // Comma separated items
    val rawText: String = "",
    val imageUrl: String = "",
    val isSynced: Boolean = false, // Simulation of modern medical vault cloud storage
    val department: String = "General Medicine",
    val diseaseOrCheckupType: String = "General Wellness"
)

@Entity(tableName = "emergency_profile")
data class EmergencyProfile(
    @PrimaryKey val id: Int = 1, // Single profile owner on the phone
    val fullName: String = "John Doe",
    val userEmail: String = "john.doe@example.com",
    val bloodType: String = "O+",
    val dateOfBirth: String = "01-01-1990",
    val allergies: String = "Penicillin, Peanuts",
    val chronicConditions: String = "Asthma",
    val currentMedications: String = "Albuterol Inhaler as needed",
    val emergencyContactName: String = "Jane Doe",
    val emergencyContactPhone: String = "+91-98765-43210",
    val emergencyContactEmail: String = "jane.doe@example.com",
    val insuranceProvider: String = "Blue Cross",
    val insuranceNumber: String = "BC-98765432-A",
    val organDonor: Boolean = true,
    val isVerified: Boolean = false // Simulated cloud health pass verification
)
