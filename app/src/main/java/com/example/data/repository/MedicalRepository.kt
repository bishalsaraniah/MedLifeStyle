package com.example.data.repository

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.database.MedicalDao
import com.example.data.model.EmergencyProfile
import com.example.data.model.MedicalRecord
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// --- Gemini REST API Request & Response Data Structures for Moshi ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    @Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType") val mimeType: String,
    val data: String // Base64 raw representation
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String = "application/json",
    val temperature: Float = 0.1f
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

// --- Local structured JSON parsing results ---
@JsonClass(generateAdapter = true)
data class OcrRecordResult(
    val title: String,
    val patientName: String,
    val doctorName: String,
    val clinicOrHospital: String,
    val category: String, // Prescription, Lab Report, Vaccine, Discharge Summary, Radiology / Scan
    val summary: String,
    val diagnoses: List<String>,
    val prescribedMeds: List<String>
)

class MedicalRepository(private val medicalDao: MedicalDao) {

    // --- Room Database Operations ---

    val allRecords: Flow<List<MedicalRecord>> = medicalDao.getAllRecordsFlow()

    fun getRecordsByCategory(category: String): Flow<List<MedicalRecord>> =
        medicalDao.getRecordsByCategoryFlow(category)

    suspend fun getRecordById(id: Int): MedicalRecord? = withContext(Dispatchers.IO) {
        medicalDao.getRecordById(id)
    }

    suspend fun insertRecord(record: MedicalRecord): Long = withContext(Dispatchers.IO) {
        medicalDao.insertRecord(record)
    }

    suspend fun updateRecord(record: MedicalRecord) = withContext(Dispatchers.IO) {
        medicalDao.updateRecord(record)
    }

    suspend fun deleteRecord(record: MedicalRecord) = withContext(Dispatchers.IO) {
        medicalDao.deleteRecord(record)
    }

    val emergencyProfile: Flow<EmergencyProfile?> = medicalDao.getEmergencyProfileFlow()

    suspend fun getEmergencyProfile(): EmergencyProfile? = withContext(Dispatchers.IO) {
        medicalDao.getEmergencyProfile() ?: createDefaultProfile()
    }

    suspend fun saveEmergencyProfile(profile: EmergencyProfile) = withContext(Dispatchers.IO) {
        medicalDao.saveEmergencyProfile(profile)
    }

    private suspend fun createDefaultProfile(): EmergencyProfile {
        val defaultProfile = EmergencyProfile()
        medicalDao.saveEmergencyProfile(defaultProfile)
        return defaultProfile
    }

    // --- Gemini OCR Scanning API Client ---

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Real API OCR analysis using user's key or BuildConfig.GEMINI_API_KEY
    suspend fun performOcr(
        inputText: String?,
        base64Image: String?,
        mimeType: String = "image/jpeg"
    ): OcrRecordResult = withContext(Dispatchers.IO) {
        // Retrieve key safely. If not set, raise a clear message for the UI to pick up
        val geminiApiKey = BuildConfig.GEMINI_API_KEY
        if (geminiApiKey.isEmpty() || geminiApiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("APIKEY_MISSING")
        }

        // Construct Gemini prompt instruction
        val promptText = """
            You are an expert healthcare document parser and OCR system.
            Please analyze this clinical report / prescription details and extract clinical info as structured JSON.
            
            Document Details to Analyze:
            ${inputText ?: "See attached image for full medical text."}
            
            Analyze thoroughly and return a valid JSON object matching the requested schema. Ensure fields are concise, helpful, and readable for a doctor:
            
            Schema:
            {
              "title": "Clear, short name of report, e.g. Complete Blood Count, Liver Function Test, Lipid Panel, Amoxicillin Prescription",
              "patientName": "Full name of patient",
              "doctorName": "FullName of consulting doctor if any",
              "clinicOrHospital": "Hospital or clinic name",
              "category": "Must be exactly one of: Prescription, Lab Report, Vaccine, Discharge Summary, Radiology / Scan, or Other",
              "summary": "Compact 2-3 sentence clinical summary of the document, findings, and clinical orders.",
              "diagnoses": ["List of suspected or confirmed diagnostics / symptoms, e.g. Hypertension, Acute Tonsillitis"],
              "prescribedMeds": ["List of prescribed drug names, dosages, and frequency, e.g. Metformin 500mg (1-0-1), Paracetamol 650mg as needed"]
            }
            Ensure the response is strictly JSON and has no additional markdown blocks or explaining text.
        """.trimIndent()

        val partsList = mutableListOf<GeminiPart>()
        partsList.add(GeminiPart(text = promptText))

        if (base64Image != null) {
            partsList.add(GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = base64Image)))
        }

        val requestPayload = GeminiRequest(
            contents = listOf(GeminiContent(parts = partsList)),
            generationConfig = GeminiGenerationConfig(responseMimeType = "application/json"),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = "You are a secure medical database system. Output strict, valid JSON. Never hallucinate patient data. If field info is absolutely absent in the document, fill with 'Not Specified'."))
            )
        )

        val jsonAdapter = moshi.adapter(GeminiRequest::class.java)
        val requestBodyString = jsonAdapter.toJson(requestPayload)

        // API Endpoint with Model gemini-3.5-flash as default per guidelines
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$geminiApiKey"

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBodyString.toRequestBody(mediaType))
            .build()

        try {
            val response = okHttpClient.newCall(httpRequest).execute()
            val rawResponse = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("MedicalRepository", "Gemini API failure: Code ${response.code}, Body: $rawResponse")
                throw Exception("API returned error code ${response.code}")
            }

            val responseAdapter = moshi.adapter(GeminiResponse::class.java)
            val parsedResponse = responseAdapter.fromJson(rawResponse)
            val extractedText = parsedResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (extractedText.isNullOrEmpty()) {
                throw Exception("Could not retrieve AI scanning response from Gemini.")
            }

            // Parse returned JSON from the model into OcrRecordResult
            val resultAdapter = moshi.adapter(OcrRecordResult::class.java)
            return@withContext resultAdapter.fromJson(extractedText) 
                ?: throw Exception("Failed to structure clinical data. Raw text: $extractedText")

        } catch (e: Exception) {
            Log.e("MedicalRepository", "Error during OCR scanning", e)
            if (e.message == "APIKEY_MISSING") {
                throw e
            }
            throw Exception("Scanning failed: ${e.localizedMessage ?: "Unknown network error"}")
        }
    }
}
