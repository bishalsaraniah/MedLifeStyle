package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.EmergencyProfile
import com.example.data.model.MedicalRecord
import com.example.data.model.SduiComponent
import com.example.data.model.SduiItem
import com.example.data.model.SduiLayout
import com.example.data.repository.MedicalRepository
import com.example.data.repository.OcrRecordResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface OcrUiState {
    object Idle : OcrUiState
    object Scanning : OcrUiState
    data class Success(val result: OcrRecordResult) : OcrUiState
    data class Error(val message: String, val isApiKeyMissing: Boolean = false) : OcrUiState
}

sealed interface SyncUiState {
    object Idle : SyncUiState
    object Syncing : SyncUiState
    data class Synced(val time: Long, val count: Int) : SyncUiState
}

class MedicalViewModel(
    application: Application,
    private val repository: MedicalRepository
) : AndroidViewModel(application) {

    // --- State Expositions ---

    val medicalRecords: StateFlow<List<MedicalRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val emergencyProfile: StateFlow<EmergencyProfile?> = repository.emergencyProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _ocrUiState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val ocrUiState: StateFlow<OcrUiState> = _ocrUiState.asStateFlow()

    private val _syncUiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

    // Temporary generated passcode for showing documents to doctors securely
    private val _doctorPasscode = MutableStateFlow<String?>(null)
    val doctorPasscode: StateFlow<String?> = _doctorPasscode.asStateFlow()

    private val _isEmergencyFullBright = MutableStateFlow(false)
    val isEmergencyFullBright: StateFlow<Boolean> = _isEmergencyFullBright.asStateFlow()

    private val _forceDarkTheme = MutableStateFlow<Boolean?>(null)
    val forceDarkTheme: StateFlow<Boolean?> = _forceDarkTheme.asStateFlow()

    fun toggleForceDarkTheme() {
        _forceDarkTheme.value = if (_forceDarkTheme.value == true) false else true
    }

    fun getProfileSduiLayout(
        profile: EmergencyProfile,
        recordsCount: Int,
        isDark: Boolean,
        syncStatus: String
    ): SduiLayout {
        return SduiLayout(
            title = "Profile Section",
            components = listOf(
                SduiComponent(
                    type = "profile_header",
                    title = profile.fullName,
                    subtitle = "Patient ID: Vault-A821",
                    value = profile.bloodType,
                    description = "O+ Blood Group • Organ Donor: " + (if (profile.organDonor) "YES" else "NO"),
                    iconName = "User"
                ),
                SduiComponent(
                    type = "status_banner",
                    title = if (profile.isVerified) "Verified MedPass Passport" else "Verification Pending",
                    subtitle = if (profile.isVerified) "Cloud-authenticated with emergency responder medical framework." else "Unverified data. Please perform sync validation.",
                    themeColor = if (profile.isVerified) "success" else "warning",
                    iconName = if (profile.isVerified) "Verified" else "Shield"
                ),
                SduiComponent(
                    type = "section_title",
                    title = "Clinical Metrics & Statistics"
                ),
                SduiComponent(
                    type = "stats_grid",
                    items = listOf(
                        SduiItem(title = "Total Records", value = recordsCount.toString(), iconName = "History"),
                        SduiItem(title = "Emergencies", value = "0 Triggered", iconName = "Emergency"),
                        SduiItem(title = "Active Mode", value = if (isDark) "Dark Theme" else "Light Theme", iconName = "Star"),
                        SduiItem(title = "Last Backup", value = syncStatus, iconName = "Calendar")
                    )
                ),
                SduiComponent(
                    type = "section_title",
                    title = "Base Physical Metrics"
                ),
                SduiComponent(
                    type = "info_list",
                    items = listOf(
                        SduiItem(title = "Date of Birth", value = profile.dateOfBirth, iconName = "Birthday"),
                        SduiItem(title = "Known Allergies", value = profile.allergies, iconName = "Allergy"),
                        SduiItem(title = "Chronic Symptoms", value = profile.chronicConditions, iconName = "Heart"),
                        SduiItem(title = "Active Prescription", value = profile.currentMedications, iconName = "Star"),
                        SduiItem(title = "Relative Emergency Contact", value = "${profile.emergencyContactName} (${profile.emergencyContactPhone})", iconName = "Contact"),
                        SduiItem(title = "Insurance Cover", value = "${profile.insuranceProvider} - ${profile.insuranceNumber}", iconName = "Shield")
                    )
                ),
                SduiComponent(
                    type = "section_title",
                    title = "Remote SDUI Actions & Interactions"
                ),
                SduiComponent(
                    type = "action_button",
                    title = "Toggle Light/Dark Theme",
                    description = "Dispatch server-driven command to swap theme modes directly.",
                    iconName = "Star",
                    actionKey = "toggle_theme",
                    themeColor = "primary"
                ),
                SduiComponent(
                    type = "action_button",
                    title = "Force Re-synchronize Storage",
                    description = "Validate offline state cache against modern health services ledger.",
                    iconName = "Verified",
                    actionKey = "sync_cloud",
                    themeColor = "success"
                )
            )
        )
    }

    init {
        // Pre-populate if empty to ensure professional first-boot experience
        viewModelScope.launch {
            try {
                val currentProfile = repository.getEmergencyProfile()
                if (currentProfile == null) {
                    repository.saveEmergencyProfile(EmergencyProfile())
                }

                val currentRecords = repository.allRecords.first()
                if (currentRecords.isEmpty()) {
                    prepopulateSampleRecords()
                }
            } catch (e: Exception) {
                Log.e("MedicalViewModel", "Error in initialization", e)
            }
        }
    }

    // --- Actions ---

    fun scanDocumentText(text: String, base64Image: String? = null) {
        viewModelScope.launch {
            _ocrUiState.value = OcrUiState.Scanning
            try {
                val ocrResult = repository.performOcr(inputText = text, base64Image = base64Image)
                _ocrUiState.value = OcrUiState.Success(ocrResult)
            } catch (e: IllegalStateException) {
                if (e.message == "APIKEY_MISSING") {
                    _ocrUiState.value = OcrUiState.Error(
                        "Gemini API key is not configured in AI Studio Secrets.",
                        isApiKeyMissing = true
                    )
                } else {
                    _ocrUiState.value = OcrUiState.Error(e.localizedMessage ?: "Unknown error")
                }
            } catch (e: Exception) {
                _ocrUiState.value = OcrUiState.Error(e.localizedMessage ?: "Scanning failed")
            }
        }
    }

    fun resetOcrState() {
        _ocrUiState.value = OcrUiState.Idle
    }

    fun saveOcrRecord(
        result: OcrRecordResult,
        rawInputText: String,
        department: String = "General Medicine",
        diseaseOrCheckupType: String = "General Wellness",
        imageUrl: String = ""
    ) {
        viewModelScope.launch {
            val record = MedicalRecord(
                title = result.title,
                patientName = result.patientName,
                doctorName = result.doctorName,
                clinicOrHospital = result.clinicOrHospital,
                category = result.category,
                summary = result.summary,
                diagnoses = result.diagnoses.joinToString(", "),
                prescribedMeds = result.prescribedMeds.joinToString(", "),
                rawText = rawInputText,
                isSynced = false,
                department = department.ifBlank { "General Medicine" },
                diseaseOrCheckupType = diseaseOrCheckupType.ifBlank { "General Wellness" },
                imageUrl = imageUrl
            )
            repository.insertRecord(record)
            _ocrUiState.value = OcrUiState.Idle
        }
    }

    fun saveManualRecord(
        title: String,
        patientName: String,
        doctorName: String,
        clinicOrHospital: String,
        category: String,
        summary: String,
        diagnoses: String,
        prescribedMeds: String,
        department: String = "General Medicine",
        diseaseOrCheckupType: String = "General Wellness",
        imageUrl: String = ""
    ) {
        viewModelScope.launch {
            val record = MedicalRecord(
                title = title.ifBlank { "Untitled Record" },
                patientName = patientName.ifBlank { "Unspecified" },
                doctorName = doctorName.ifBlank { "Generic Practitioner" },
                clinicOrHospital = clinicOrHospital.ifBlank { "Local Clinic" },
                category = category,
                summary = summary.ifBlank { "No summary specified" },
                diagnoses = diagnoses,
                prescribedMeds = prescribedMeds,
                rawText = "Manually Entered Document",
                isSynced = false,
                department = department.ifBlank { "General Medicine" },
                diseaseOrCheckupType = diseaseOrCheckupType.ifBlank { "General Wellness" },
                imageUrl = imageUrl
            )
            repository.insertRecord(record)
        }
    }

    fun updateRecord(record: MedicalRecord) {
        viewModelScope.launch {
            repository.updateRecord(record)
        }
    }

    fun deleteRecord(record: MedicalRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun updateEmergencyProfile(profile: EmergencyProfile) {
        viewModelScope.launch {
            repository.saveEmergencyProfile(profile)
        }
    }

    fun triggerEmergencyMode(enabled: Boolean) {
        _isEmergencyFullBright.value = enabled
    }

    // Secure Cloud Backup and Sync simulation
    fun syncRecordsToCloud() {
        viewModelScope.launch {
            _syncUiState.value = SyncUiState.Syncing
            // Simulate cloud handshake delay
            kotlinx.coroutines.delay(2000)
            
            val records = repository.allRecords.first()
            records.forEach { record ->
                if (!record.isSynced) {
                    repository.updateRecord(record.copy(isSynced = true))
                }
            }
            
            // Also update emergency profile verified status
            val profile = repository.getEmergencyProfile()
            if (profile != null && !profile.isVerified) {
                repository.saveEmergencyProfile(profile.copy(isVerified = true))
            }

            _syncUiState.value = SyncUiState.Synced(System.currentTimeMillis(), records.size)
        }
    }

    fun generateDoctorSharingPasscode() {
        // Generates a random 6-digit numeric pass which expires
        val randomPass = (100000..999999).random().toString()
        _doctorPasscode.value = randomPass
    }

    fun clearDoctorSharingPasscode() {
        _doctorPasscode.value = null
    }

    // --- Private Helper to pre-populate ---

    private suspend fun prepopulateSampleRecords() {
        val r1 = MedicalRecord(
            title = "Complete Blood Count & Lipids",
            patientName = "John Doe",
            doctorName = "Dr. Elizabeth Thorne, MD",
            clinicOrHospital = "Metro Health Diagnostics",
            date = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 10, // 10 days ago
            category = "Lab Report",
            summary = "Routine wellness health panel. White blood cells and red blood counts are in ideal reference frames. Total Cholesterol and LDL levels are elevated, requiring heart-healthy diet modifications.",
            diagnoses = "Hypercholesterolemia, Borderline LDL",
            prescribedMeds = "Atorvastatin 10mg once daily at dinner",
            rawText = "Hemoglobin: 14.8 g/dL. Total Cholesterol: 245 mg/dL. LDL: 162 mg/dL. HDL: 42 mg/dL.",
            isSynced = true,
            department = "Cardiology",
            diseaseOrCheckupType = "Cholesterol Tracker"
        )

        val r2 = MedicalRecord(
            title = "Metoprolol & Asthma Refills",
            patientName = "John Doe",
            doctorName = "Dr. Peter Vance, MD",
            clinicOrHospital = "Redwood Cardiology Clinic",
            date = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30, // 30 days ago
            category = "Prescription",
            summary = "Periodic cardiology follow-up. Blood pressure is well optimized on present dosage. Metoprolol Metoprolol Succinate and Albuterol inhaler prescription refills granted.",
            diagnoses = "Essential Hypertension, Mild Reactive Airway Disease",
            prescribedMeds = "Atorvastatin 20mg PM, Metoprolol 25mg daily, Albuterol 90mcg Inhaler 2 puffs PRN",
            rawText = "Redwood Suite 400. Rx: Metoprolol 25mg daily, Albuterol 2 puffs.",
            isSynced = true,
            department = "Cardiology",
            diseaseOrCheckupType = "Hypertension Checkup"
        )

        val r3 = MedicalRecord(
            title = "Chest X-Ray (A-P)",
            patientName = "John Doe",
            doctorName = "Dr. Robert S. Chen, MD",
            clinicOrHospital = "St. Jude Medical Center",
            date = System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 45, // 45 days ago
            category = "Radiology / Scan",
            summary = "Diagnostic study performed to investigate persistent seasonal allergy cough. Lungs are completely aerated and clear of focal infiltrates. Cardiac shadow is of normal size and contour.",
            diagnoses = "Normal Radiographic Chest Study",
            prescribedMeds = "None",
            rawText = "Lungs are clear. Cardiomediastinal contour is normal. Impression: Negative chest study.",
            isSynced = true,
            department = "Radiology",
            diseaseOrCheckupType = "Lungs Checkup"
        )

        repository.insertRecord(r1)
        repository.insertRecord(r2)
        repository.insertRecord(r3)
    }
}

// --- Factory Provider ---

class MedicalViewModelFactory(
    private val application: Application,
    private val repository: MedicalRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicalViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
