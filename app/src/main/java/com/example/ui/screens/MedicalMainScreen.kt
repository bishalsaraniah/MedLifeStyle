package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import com.example.data.repository.OcrRecordResult
import com.example.data.model.EmergencyProfile
import com.example.data.model.MedicalRecord
import com.example.data.model.SduiComponent
import com.example.data.model.SduiItem
import com.example.data.model.SduiLayout
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.UrgentGold
import com.example.ui.viewmodel.MedicalViewModel
import com.example.ui.viewmodel.OcrUiState
import com.example.ui.viewmodel.SyncUiState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalMainScreen(
    viewModel: MedicalViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val records by viewModel.medicalRecords.collectAsState()
    val emergencyProfile by viewModel.emergencyProfile.collectAsState()
    val ocrState by viewModel.ocrUiState.collectAsState()
    val syncState by viewModel.syncUiState.collectAsState()
    val doctorPasscode by viewModel.doctorPasscode.collectAsState()
    val isEmergencyFullBright by viewModel.isEmergencyFullBright.collectAsState()

    var currentTab by remember { mutableStateOf(0) } // 0: My Records, 1: AI Scan OCR, 2: Emergency Pass, 3: Doctor Access, 4: SDUI User Profile
    var showAddRecordDialog by remember { mutableStateOf(false) }

    // Theme backgrounds depending on full-bright mode
    val mainBackground = if (isEmergencyFullBright) Color.White else MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(mainBackground)
    ) {
        if (isEmergencyFullBright) {
            // High-Contrast Emergency Overlay Screen for paramedics
            EmergencyFullBrightOverlay(
                profile = emergencyProfile ?: EmergencyProfile(),
                onDismiss = { viewModel.triggerEmergencyMode(false) }
            )
        } else {
            Scaffold(
                topBar = {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .statusBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MedLifeStyleLogo(sizeDp = 42.dp)
                                Column {
                                    Text(
                                        text = "MedLifeStyle Vault",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Secure Offline Grid Repository",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            color = Color.White,
                            tonalElevation = 6.dp,
                            border = BorderStroke(1.dp, Color(0xFFECEFF1))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tabsList = listOf(
                                    Triple(0, "Vault", if (currentTab == 0) Icons.Filled.FolderCopy else Icons.Outlined.FolderCopy),
                                    Triple(1, "AI Scan", if (currentTab == 1) Icons.Filled.DocumentScanner else Icons.Outlined.DocumentScanner),
                                    Triple(2, "MedPass", if (currentTab == 2) Icons.Filled.Shield else Icons.Outlined.Shield),
                                    Triple(3, "Profile", if (currentTab == 3) Icons.Filled.Person else Icons.Outlined.Person)
                                )

                                tabsList.forEach { (tabIndex, label, icon) ->
                                    val isSelected = currentTab == tabIndex
                                    val iconColor by animateColorAsState(
                                        targetValue = if (isSelected) Color(0xFF00695C) else Color(0xFF78909C),
                                        label = "iconColor"
                                    )
                                    val containerColor by animateColorAsState(
                                        targetValue = if (isSelected) Color(0xFFE0F2F1) else Color.Transparent,
                                        label = "containerColor"
                                    )

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(containerColor)
                                            .clickable { currentTab = tabIndex }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = iconColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            AnimatedVisibility(
                                                visible = isSelected,
                                                enter = fadeIn() + expandHorizontally(),
                                                exit = fadeOut() + shrinkHorizontally()
                                            ) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = label,
                                                    color = Color(0xFF004D40),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        // Dynamic active dot indicator below the label
                                        AnimatedVisibility(
                                            visible = isSelected,
                                            enter = fadeIn() + expandVertically(),
                                            exit = fadeOut() + shrinkVertically()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00695C))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (currentTab == 0) {
                        ExtendedFloatingActionButton(
                            onClick = { showAddRecordDialog = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            icon = { Icon(Icons.Filled.AddCard, contentDescription = "Add Record") },
                            text = { Text("Add Record", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentTab) {
                        0 -> RecordsDashboard(
                            records = records,
                            emergencyProfile = emergencyProfile ?: EmergencyProfile(),
                            onDelete = { viewModel.deleteRecord(it) },
                            onUpdateRecord = { viewModel.updateRecord(it) }
                        )
                        1 -> OcrScannerScreen(
                            ocrState = ocrState,
                            onScanText = { text -> viewModel.scanDocumentText(text) },
                            onSaveRecord = { result, text, dept, disease, img -> viewModel.saveOcrRecord(result, text, dept, disease, img) },
                            onAddManualRecord = { title, pat, doc, hosp, cat, sum, diag, meds, dept, disease, img ->
                                viewModel.saveManualRecord(title, pat, doc, hosp, cat, sum, diag, meds, dept, disease, img)
                                Toast.makeText(context, "Manual record stored", Toast.LENGTH_SHORT).show()
                                currentTab = 0
                            },
                            onReset = { viewModel.resetOcrState() }
                        )
                        2 -> EmergencyProfileScreen(
                            profile = emergencyProfile ?: EmergencyProfile(),
                            onTriggerFullBright = { viewModel.triggerEmergencyMode(true) }
                        )
                        3 -> ProfileSduiScreen(
                            viewModel = viewModel,
                            emergencyProfile = emergencyProfile ?: EmergencyProfile(),
                            recordsCount = records.size,
                            syncState = syncState,
                            onSaveProfile = { updated -> viewModel.updateEmergencyProfile(updated) }
                        )
                    }

                    if (showAddRecordDialog) {
                        AddRecordDialog(
                            onDismiss = { showAddRecordDialog = false },
                            onSave = { title, patient, doctor, hospital, category, summary, diagnoses, medications, dept, disease ->
                                viewModel.saveManualRecord(
                                    title = title,
                                    patientName = patient,
                                    doctorName = doctor,
                                    clinicOrHospital = hospital,
                                    category = category,
                                    summary = summary,
                                    diagnoses = diagnoses,
                                    prescribedMeds = medications,
                                    department = dept,
                                    diseaseOrCheckupType = disease
                                )
                                showAddRecordDialog = false
                                Toast.makeText(context, "New record saved successfully", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- MEDLIFESTYLE BRAND LOGO (NATIVE CANVAS IMPLEMENTATION SPLINE-ACCURATE) ---
@Composable
fun MedLifeStyleLogo(
    modifier: Modifier = Modifier,
    sizeDp: androidx.compose.ui.unit.Dp = 120.dp
) {
    val isDark = isSystemInDarkTheme()
    val outerOrbitColor = if (isDark) Color(0xFF14B8A6) else Color(0xFF0D9488)
    val innerCrossColor1 = if (isDark) Color(0xFF0F766E) else Color(0xFF0D9488)
    val innerCrossColor3 = if (isDark) Color(0xFF4D7C0F) else Color(0xFF65A30D)
    val nodeColor1 = if (isDark) Color(0xFF22D3EE) else Color(0xFF06B6D4)
    val nodeColor2 = if (isDark) Color(0xFF65A30D) else Color(0xFF84CC16)
    val canvasBg = if (isDark) Color(0xFF1E293B) else Color.White

    Canvas(modifier = modifier.size(sizeDp)) {
        val width = size.width
        val height = size.height

        // 1. Draw circular orbit outline sweep gradient
        drawArc(
            brush = Brush.sweepGradient(
                0.0f to outerOrbitColor,
                0.5f to innerCrossColor3,
                1.0f to outerOrbitColor
            ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = width * 0.04f),
            topLeft = Offset(width * 0.06f, height * 0.06f),
            size = Size(width * 0.88f, height * 0.88f)
        )

        // 2. Draw Orbit nodes (top-right and bottom-left)
        // Top-right node
        drawCircle(
            color = nodeColor2,
            radius = width * 0.05f,
            center = Offset(width * 0.85f, height * 0.25f)
        )
        // Bottom-left node
        drawCircle(
            color = nodeColor1,
            radius = width * 0.05f,
            center = Offset(width * 0.15f, height * 0.75f)
        )

        // 3. Draw Beautiful Medical Cross with center cutout / s-curve
        val crossWidth = width * 0.46f
        val crossThickness = width * 0.18f
        
        val vertLeft = (width - crossThickness) / 2f
        val vertTop = (height - crossWidth) / 2f
        val horizLeft = (width - crossWidth) / 2f
        val horizTop = (height - crossThickness) / 2f
        
        val crossBrush = Brush.linearGradient(
            colors = listOf(innerCrossColor1, innerCrossColor3),
            start = Offset(0f, 0f),
            end = Offset(width, height)
        )

        // Vertical arm
        drawRoundRect(
            brush = crossBrush,
            topLeft = Offset(vertLeft, vertTop),
            size = Size(crossThickness, crossWidth),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.04f, width * 0.04f)
        )
        // Horizontal arm
        drawRoundRect(
            brush = crossBrush,
            topLeft = Offset(horizLeft, horizTop),
            size = Size(crossWidth, crossThickness),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(width * 0.04f, width * 0.04f)
        )

        // 4. White central "S"-curve path cutout
        val sPath = Path().apply {
            moveTo(width * 0.44f, height * 0.36f)
            cubicTo(
                width * 0.36f, height * 0.41f,
                width * 0.36f, height * 0.47f,
                width * 0.50f, height * 0.50f
            )
            cubicTo(
                width * 0.64f, height * 0.53f,
                width * 0.64f, height * 0.59f,
                width * 0.56f, height * 0.64f
            )
        }
        drawPath(
            path = sPath,
            color = canvasBg,
            style = Stroke(width = width * 0.05f, cap = StrokeCap.Round)
        )

        // 5. Scattering digital pixel blocks (squares) on both sides
        val squareUnit = width * 0.045f
        
        // Left scattering pixels
        drawRect(
            color = nodeColor1,
            topLeft = Offset(width * 0.28f, height * 0.35f),
            size = Size(squareUnit, squareUnit)
        )
        drawRect(
            color = nodeColor1,
            topLeft = Offset(width * 0.34f, height * 0.29f),
            size = Size(squareUnit, squareUnit)
        )
        drawRect(
            color = nodeColor1,
            topLeft = Offset(width * 0.25f, height * 0.44f),
            size = Size(squareUnit, squareUnit)
        )

        // Right scattering pixels
        drawRect(
            color = nodeColor2,
            topLeft = Offset(width * 0.68f, height * 0.61f),
            size = Size(squareUnit, squareUnit)
        )
        drawRect(
            color = nodeColor2,
            topLeft = Offset(width * 0.74f, height * 0.54f),
            size = Size(squareUnit, squareUnit)
        )
        drawRect(
            color = nodeColor2,
            topLeft = Offset(width * 0.60f, height * 0.67f),
            size = Size(squareUnit, squareUnit)
        )
    }
}

// --- CLOUD SYNC STATE COMPOSABLE ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CloudSyncStatusIndicator(
    syncState: SyncUiState,
    onSyncClick: () -> Unit
) {
    val context = LocalContext.current
    val textModifier = Modifier.combinedClickable(
        onClick = {
            if (syncState is SyncUiState.Idle) {
                onSyncClick()
            }
        },
        onLongClick = {
            Toast.makeText(context, "Cloud status: Encrypted connection to clinical servers", Toast.LENGTH_SHORT).show()
        }
    )

    Row(
        modifier = textModifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (syncState) {
            is SyncUiState.Idle -> {
                Icon(
                    imageVector = Icons.Filled.CloudUpload,
                    contentDescription = "Trigger Sync",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Backup",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is SyncUiState.Syncing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Syncing...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is SyncUiState.Synced -> {
                Icon(
                    imageVector = Icons.Filled.CloudDone,
                    contentDescription = "Synced",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Cloud Vault Safe",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}


// --- TAB 0: RECORDS DASHBOARD ---

@Composable
fun EmergencyAccessBadgeCard(
    profile: EmergencyProfile,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, Color(0xFFFFB4AB), RoundedCornerShape(28.dp))
            .background(Color(0xFFFFDAD6))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF93000A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Emergency,
                    contentDescription = "Emergency Access Icon",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "Emergency Access",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF410002)
                )
                Text(
                    text = "Blood: ${if (profile.bloodType.isNotBlank()) profile.bloodType else "O+"} · Allergies: ${if (profile.allergies.isNotBlank()) profile.allergies else "Penicillin"}",
                    fontSize = 13.sp,
                    color = Color(0xFF410002).copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Open",
            tint = Color(0xFF410002)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsDashboard(
    records: List<MedicalRecord>,
    emergencyProfile: EmergencyProfile,
    onDelete: (MedicalRecord) -> Unit,
    onUpdateRecord: (MedicalRecord) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSpecialtyForDetail by remember { mutableStateOf<String?>(null) }
    // Active record being edited
    var editingRecord by remember { mutableStateOf<MedicalRecord?>(null) }

    // List of Specialties as in Mockup 1
    val specialtiesList = listOf(
        Triple("Cardiology", Icons.Filled.Favorite, "SPECIALTY"),
        Triple("Dermatology", Icons.Filled.Healing, "SPECIALTY"),
        Triple("Pediatrics", Icons.Filled.ChildCare, "SPECIALTY"),
        Triple("Orthopedics", Icons.Filled.AccessibilityNew, "SPECIALTY"),
        Triple("Neurology", Icons.Filled.Psychology, "SPECIALTY"),
        Triple("General Medicine", Icons.Filled.MedicalServices, "PRIMARY"),
        Triple("Lab Reports", Icons.Filled.Biotech, "LABS"),
        Triple("Vaccines", Icons.Filled.Vaccines, "PREVENTIVE"),
        Triple("Radiology / Scan", Icons.Filled.Visibility, "DIAGNOSIS"),
        Triple("Others", Icons.Filled.Folder, "RECORDS")
    )

    // Helper to count records dynamically
    fun countRecordsForSpecialty(specialty: String, recordsList: List<MedicalRecord>): Int {
        return recordsList.count { record ->
            record.department.equals(specialty, ignoreCase = true) ||
            record.category.equals(specialty, ignoreCase = true) ||
            record.diseaseOrCheckupType.equals(specialty, ignoreCase = true) ||
            (specialty == "Radiology / Scan" && record.category.contains("Radiology", ignoreCase = true)) ||
            (specialty == "Lab Reports" && record.category.contains("Lab", ignoreCase = true)) ||
            (specialty == "Vaccines" && record.category.contains("Vaccine", ignoreCase = true)) ||
            (specialty == "Others" && !listOf("Cardiology", "Dermatology", "Pediatrics", "Orthopedics", "Neurology", "Radiology / Scan", "General Medicine", "Lab Reports", "Vaccines").any {
                record.department.equals(it, ignoreCase = true) || record.category.equals(it, ignoreCase = true)
            })
        }
    }

    // Filter logic based on global search query
    val searchedRecords = records.filter { record ->
        record.title.contains(searchQuery, ignoreCase = true) ||
        record.doctorName.contains(searchQuery, ignoreCase = true) ||
        record.clinicOrHospital.contains(searchQuery, ignoreCase = true) ||
        record.diagnoses.contains(searchQuery, ignoreCase = true) ||
        record.summary.contains(searchQuery, ignoreCase = true) ||
        record.department.contains(searchQuery, ignoreCase = true) ||
        record.diseaseOrCheckupType.contains(searchQuery, ignoreCase = true) ||
        record.category.contains(searchQuery, ignoreCase = true)
    }

    // Filter logic for selected category
    val detailRecords = remember(selectedSpecialtyForDetail, records) {
        if (selectedSpecialtyForDetail == null) emptyList() else {
            val spec = selectedSpecialtyForDetail!!
            records.filter { record ->
                record.department.equals(spec, ignoreCase = true) ||
                record.category.equals(spec, ignoreCase = true) ||
                record.diseaseOrCheckupType.equals(spec, ignoreCase = true) ||
                (spec == "Radiology / Scan" && record.category.contains("Radiology", ignoreCase = true)) ||
                (spec == "Lab Reports" && record.category.contains("Lab", ignoreCase = true)) ||
                (spec == "Vaccines" && record.category.contains("Vaccine", ignoreCase = true)) ||
                (spec == "Others" && !listOf("Cardiology", "Dermatology", "Pediatrics", "Orthopedics", "Neurology", "Radiology / Scan", "General Medicine", "Lab Reports", "Vaccines").any {
                    record.department.equals(it, ignoreCase = true) || record.category.equals(it, ignoreCase = true)
                })
            }
        }
    }

    // Edit Dialog display trigger
    if (editingRecord != null) {
        EditRecordDialog(
            record = editingRecord!!,
            onDismiss = { editingRecord = null },
            onSave = { updated ->
                onUpdateRecord(updated)
                editingRecord = null
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FBFB)),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar (Active matches show search results, inactive shows category selector)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (searchQuery.isEmpty() && selectedSpecialtyForDetail == null) {
                    // Title and subtitle matching Image 1
                    Text(
                        text = "Health Categories",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF004D40) // Deep dark teal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Organize and access your clinical documentation by medical specialty.",
                        fontSize = 14.sp,
                        color = Color(0xFF455A64), // Charcoal gray subtext
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                } else if (selectedSpecialtyForDetail != null && searchQuery.isEmpty()) {
                    // Clickable row representing a beautiful back route
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSpecialtyForDetail = null }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back to categories",
                            tint = Color(0xFF00695C),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${selectedSpecialtyForDetail}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF004D40)
                        )
                    }
                    Text(
                        text = "Access your localized SHA-256 secure offline clinical ledger items for $selectedSpecialtyForDetail.",
                        fontSize = 12.sp,
                        color = Color(0xFF546E7A),
                        modifier = Modifier.padding(start = 36.dp, bottom = 12.dp)
                    )
                }

                // Rounded Capsule Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    placeholder = { Text("Search your records...", fontSize = 15.sp, color = Color(0xFF78909C)) },
                    leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = Color(0xFF00695C)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, "Clear search", tint = Color(0xFF455A64))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F5F7),
                        unfocusedContainerColor = Color(0xFFF1F5F7),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        errorBorderColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }
        }

        if (searchQuery.isNotEmpty()) {
            // Unifed Search Results listing
            item {
                Text(
                    text = "SEARCH RESULTS (${searchedRecords.size} found)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF00695C),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (searchedRecords.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = Color(0xFFB0BEC5)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No medical logs correspond to '$searchQuery'",
                            fontSize = 15.sp,
                            color = Color(0xFF546E7A),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                items(searchedRecords, key = { it.id }) { record ->
                    RecordCardItem(
                        record = record,
                        onDelete = { onDelete(record) },
                        onEdit = { editingRecord = record }
                    )
                }
            }
        } else if (selectedSpecialtyForDetail != null) {
            // Showing records specifically for selected specialty
            val recordsInSpecList = detailRecords
            if (recordsInSpecList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCard,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFFB0BEC5)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No $selectedSpecialtyForDetail records found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF455A64)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Click 'Add Record' or transition to 'AI Scan' to create records in this category.",
                            fontSize = 13.sp,
                            color = Color(0xFF78909C),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(recordsInSpecList, key = { it.id }) { record ->
                    RecordCardItem(
                        record = record,
                        onDelete = { onDelete(record) },
                        onEdit = { editingRecord = record }
                    )
                }
            }
        } else {
            // General clinical grid of Specialties mapping elegantly
            val chunkedSpecialties = specialtiesList.chunked(2)
            items(chunkedSpecialties) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (title, icon, badge) ->
                        val count = countRecordsForSpecialty(title, records)
                        Box(modifier = Modifier.weight(1f)) {
                            SpecialtyCard(
                                title = title,
                                icon = icon,
                                badgeText = badge,
                                recordCount = count,
                                onClick = { selectedSpecialtyForDetail = title }
                            )
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SpecialtyCard(
    title: String,
    icon: ImageVector,
    badgeText: String,
    recordCount: Int,
    onClick: () -> Unit
) {
    val activeTeal = Color(0xFF00695C)
    val lightAqua = Color(0xFFE0F2F1)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFECEFF1).copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(lightAqua),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = activeTeal,
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE0F2F1), // Light greenish blue/teal background style
                    border = BorderStroke(1.dp, Color(0xFFB2DFDB))
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color(0xFF00796B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1D333A)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = null,
                    tint = Color(0xFF78909C),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "$recordCount records",
                    fontSize = 13.sp,
                    color = Color(0xFF546E7A),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/*
private fun dummy() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 1. Personal Health Vault Portfolio Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emergencyProfile.fullName.ifBlank { "Unspecified" }.take(2).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = emergencyProfile.fullName.ifBlank { "Vault Owner" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Credentialed Patient Portfolio",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Blood badge
                    val currentBlood = emergencyProfile.bloodType.ifBlank { "Not Set" }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = currentBlood,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Stats Row Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Stat 1: Total records
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "TOTAL RECORDS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${records.size} Lines",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Stat 2: Security status
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "VAULT SECURITY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "AES-256 Secured",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SHA-256",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Stat 3: Sync Status
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "STORAGE MODE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CloudOff,
                                contentDescription = "Offline Vault Activated",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Local Guard",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 2. Beautiful Capsule sliding/rounded tab selector
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val subTabs = listOf(
                    0 to "Timeline Feed" to Icons.Filled.List,
                    1 to "Condition Tracking" to Icons.Filled.Favorite
                )

                subTabs.forEach { (tabIdAndName, icon) ->
                    val (tabId, tabName) = tabIdAndName
                    val isTabSelected = dashboardTab == tabId
                    val activeColor = if (isTabSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    val bgPillColor = if (isTabSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgPillColor)
                            .clickable { dashboardTab = tabId }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = tabName,
                                tint = activeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tabName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeColor
                            )
                        }
                    }
                }
            }
        }

        // 3. Ultra-Modern Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            placeholder = { Text("Search records, diagnoses, symptoms...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (dashboardTab == 0) {
            // --- TAB 0: TIMELINE FEED ---
            Text(
                text = "Categories",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Categories selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.5.dp
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Recent Scans title Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Medical Documents",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.1.sp
                )
                Text(
                    text = "Reset Filter",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        selectedCategory = "All"
                        searchQuery = ""
                    }
                )
            }

            // List of Feed Items
            if (filteredTimelineRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "Empty",
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "No Medical Records Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedCategory != "All") {
                                "Try altering your search filters or selectors."
                            } else {
                                "Get started by clicking the 'AI Scan' tab to upload or click report photos instantly!"
                            },
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredTimelineRecords, key = { it.id }) { record ->
                        RecordCardItem(
                            record = record,
                            onDelete = { onDelete(record) },
                            onEdit = { editingRecord = record }
                        )
                    }
                }
            }
        } else {
            // --- TAB 1: ORGANIZED DISEASE & CHECKUPS TRACKING ---
            // Interactive Dropdown Selectors for high flexibility styling!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Department filter dropdown button
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showDeptDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFDDE2EA)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedDeptFilter,
                                fontSize = 11.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D35),
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(Icons.Filled.ArrowDropDown, null, tint = Color(0xFF006492), modifier = Modifier.size(16.dp))
                        }
                    }
                    DropdownMenu(
                        expanded = showDeptDropdown,
                        onDismissRequest = { showDeptDropdown = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        val deptsList = listOf("All Departments", "Cardiology", "Pediatrics", "Oncology", "Orthopedics", "General Medicine", "Neurology", "Dermatology", "Radiology", "Other")
                        deptsList.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    selectedDeptFilter = dept
                                    showDeptDropdown = false
                                }
                            )
                        }
                    }
                }

                // Hospital filter dropdown button
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showHospitalDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFDDE2EA)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedHospitalFilter.take(18) + (if (selectedHospitalFilter.length > 18) ".." else ""),
                                fontSize = 11.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D35),
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(Icons.Filled.ArrowDropDown, null, tint = Color(0xFF006492), modifier = Modifier.size(16.dp))
                        }
                    }
                    DropdownMenu(
                        expanded = showHospitalDropdown,
                        onDismissRequest = { showHospitalDropdown = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        val uniqHosp = remember(records) {
                            listOf("All Hospitals") + records.map { it.clinicOrHospital.trim() }.filter { it.isNotBlank() }.distinct()
                        }
                        uniqHosp.forEach { hosp ->
                            DropdownMenuItem(
                                text = { Text(hosp, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    selectedHospitalFilter = hosp
                                    showHospitalDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Group list
            if (groupedByDisease.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCard,
                            contentDescription = "Empty tracker",
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF006492).copy(alpha = 0.3f)
                        )
                        Text(
                            text = "No Tracking Conditions Match",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Try picking a different department/hospital from the dropdown headers or clearing search text.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(groupedByDisease) { (diseaseType, diseaseRecords) ->
                        var openTimeline by remember { mutableStateOf(false) }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            // Disease Header card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { openTimeline = !openTimeline }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = diseaseType,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${diseaseRecords.size} Checkups done",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = diseaseRecords.firstOrNull()?.department ?: "General",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = if (openTimeline) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "Toggle timeline viewer",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Beautiful inside Timelines!
                            AnimatedVisibility(
                                visible = openTimeline,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        .padding(horizontal = 16.dp, vertical = 20.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    Text(
                                        text = "CHRONOLOGICAL HEALTH TIMELINE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    
                                    // Render Chronological Timeline dots
                                    val sortedList = diseaseRecords.sortedByDescending { it.date }
                                    sortedList.forEachIndexed { index, record ->
                                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                        val dateStr = sdf.format(Date(record.date))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                        ) {
                                            // Vertical timeline drawing line & dot
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.width(36.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                                                )
                                                if (index < sortedList.size - 1) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.dp)
                                                            .height(110.dp)
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                                    )
                                                }
                                            }
                                            
                                            // Consultation Details Inside Timeline Card
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = dateStr,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = record.category,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = record.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Doctor: ${record.doctorName} (${record.clinicOrHospital})",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = record.summary,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    
                                                    // Core modifiers for timelines
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                                        horizontalArrangement = Arrangement.End
                                                    ) {
                                                        IconButton(
                                                            onClick = { editingRecord = record },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        IconButton(
                                                            onClick = { onDelete(record) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
*/

// Single Expanded Medical Record view item
@Composable
fun RecordCardItem(
    record: MedicalRecord,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val formattedDate = remember(record.date) {
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(record.date))
    }

    val isDark = isSystemInDarkTheme()
    val bannerColor = when (record.category) {
        "Prescription" -> {
            if (isDark) Color(0xFF1E3524) to Color(0xFFC8E6C9)
            else Color(0xFFD2E8D4) to Color(0xFF00210B)
        }
        "Lab Report" -> {
            if (isDark) Color(0xFF132A46) to Color(0xFFC2E7FF)
            else Color(0xFFD1E4FF) to Color(0xFF001D36)
        }
        "Radiology / Scan" -> {
            if (isDark) Color(0xFF381E3B) to Color(0xFFFCDDFA)
            else Color(0xFFFAD8FD) to Color(0xFF2B1230)
        }
        "Discharge Summary", "Vaccine" -> {
            if (isDark) Color(0xFF452D1C) to Color(0xFFFFECE0)
            else Color(0xFFFFDCC0) to Color(0xFF2F1500)
        }
        else -> {
            if (isDark) Color(0xFF2E3135) to Color(0xFFE2E2E2)
            else Color(0xFFEEF1F6) to Color(0xFF41484D)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Visual Category styling)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bannerColor.first)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = record.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = bannerColor.second,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(bannerColor.second.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        if (record.isSynced) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE8F5E9).copy(alpha = 0.18f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = "Synced to clinical ledger",
                                    tint = if (isDark) Color(0xFFC8E6C9) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Synced",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFC8E6C9) else Color(0xFF2E7D32)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFECE0).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudQueue,
                                    contentDescription = "Local Ledger",
                                    tint = if (isDark) Color(0xFFFFCC80) else Color(0xFFE65100),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Local Only",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFFCC80) else Color(0xFFE65100)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = record.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else bannerColor.second
                    )
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle expansion",
                        tint = bannerColor.second
                    )
                }
            }

            // Central info
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconText(icon = Icons.Outlined.PersonPin, text = "Patient: ${record.patientName}")
                    Text(
                        text = formattedDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                IconText(icon = Icons.Outlined.MedicalInformation, text = "Provider: ${record.doctorName} (${record.clinicOrHospital})")

                // Distinctive Medical Department & Conditions Pills!
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Dept: ${record.department}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0F7FA))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Condition: ${record.diseaseOrCheckupType}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF006064)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = record.summary,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Expanded Section
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Divider(modifier = Modifier.padding(bottom = 12.dp))

                        // Diagnoses
                        if (record.diagnoses.isNotBlank()) {
                            Text("DIAGNOSES / FINDINGS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                record.diagnoses.split(",").map { it.trim() }.forEach { diag ->
                                    if (diag.isNotBlank()) {
                                        Text(
                                            text = diag,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Medications
                        if (record.prescribedMeds.isNotBlank()) {
                            Text("PRESCRIBED MEDICATIONS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    record.prescribedMeds.split(",").map { it.trim() }.forEach { med ->
                                        if (med.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.padding(vertical = 3.dp)
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, "Active Med", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                                Text(med, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Custom Scanned Image Attachment Drawing
                        if (record.imageUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("PHOTO SCAN PREVIEW", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFFC2E7FF), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Draw custom Canvas visuals based on the preset image url
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val width = size.width
                                        val height = size.height
                                        
                                        when (record.imageUrl) {
                                            "preset_lipid" -> {
                                                // Dr. Thorne's chemist lipids grid lines
                                                val gridStep = 40f
                                                for (x in 0..width.toInt() step gridStep.toInt()) {
                                                    drawLine(Color.LightGray.copy(alpha = 0.3f), start = androidx.compose.ui.geometry.Offset(x.toFloat(), 0f), end = androidx.compose.ui.geometry.Offset(x.toFloat(), height), strokeWidth = 1f)
                                                }
                                                for (y in 0..height.toInt() step gridStep.toInt()) {
                                                    drawLine(Color.LightGray.copy(alpha = 0.3f), start = androidx.compose.ui.geometry.Offset(0f, y.toFloat()), end = androidx.compose.ui.geometry.Offset(width, y.toFloat()), strokeWidth = 1f)
                                                }
                                                // Test Tube graphics / Chemical bar heights
                                                drawRect(Color(0xFFFFCDD2).copy(alpha = 0.8f), topLeft = androidx.compose.ui.geometry.Offset(100f, 40f), size = androidx.compose.ui.geometry.Size(80f, 60.0f))
                                                drawRect(Color(0xFFC8E6C9).copy(alpha = 0.8f), topLeft = androidx.compose.ui.geometry.Offset(220f, 20f), size = androidx.compose.ui.geometry.Size(80f, 80.0f))
                                                drawRect(Color(0xFFB3E5FC).copy(alpha = 0.8f), topLeft = androidx.compose.ui.geometry.Offset(340f, 50f), size = androidx.compose.ui.geometry.Size(80f, 50.0f))
                                            }
                                            "preset_cardio" -> {
                                                // Metoprolol Cardiogram line pulse wave
                                                val path = androidx.compose.ui.graphics.Path()
                                                path.moveTo(0f, height / 2f)
                                                path.lineTo(100f, height / 2f)
                                                path.lineTo(120f, height / 2f - 40f)
                                                path.lineTo(140f, height / 2f + 50f)
                                                path.lineTo(160f, height / 2f - 80f)
                                                path.lineTo(180f, height / 2f + 20f)
                                                path.lineTo(200f, height / 2f)
                                                path.lineTo(300f, height / 2f)
                                                path.lineTo(320f, height / 2f - 60f)
                                                path.lineTo(340f, height / 2f + 70f)
                                                path.lineTo(360f, height / 2f - 30f)
                                                path.lineTo(380f, height / 2f)
                                                path.lineTo(width, height / 2f)
                                                
                                                drawPath(path, color = Color.Red, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                                            }
                                            "preset_lungs" -> {
                                                // Dr. Chen Chest / lung wireframe canvas
                                                drawCircle(Color(0xFFB2EBF2).copy(alpha = 0.5f), radius = 45f, center = androidx.compose.ui.geometry.Offset(width / 2f - 60f, height / 2f))
                                                drawCircle(Color(0xFFB2EBF2).copy(alpha = 0.5f), radius = 45f, center = androidx.compose.ui.geometry.Offset(width / 2f + 60f, height / 2f))
                                            }
                                            else -> {
                                                // Generic clinical cross gradient shield
                                                drawCircle(Color(0xFFE2E8F0), radius = 60f, center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f))
                                            }
                                        }
                                    }
                                    
                                    // Visual Label overlay banner
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("LIVE CLINICAL ATTACHMENT preview", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Clinical QR check-in and scan action
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(4.dp)
                            ) {
                                QrCodeCanvas(modifier = Modifier.fillMaxSize())
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Doctor Direct Scan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Ask your medical practitioner to scan this report ID pass to view decrypted detail on their terminal.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Actions footer panel
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onEdit,
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.Edit, "Edit Medical Record")
                                }
                                IconButton(
                                    onClick = onDelete,
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Filled.DeleteSweep, "Delete Medical Record")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconText(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


// --- TAB 1: OCR scanner SCREEN ---

@Composable
fun OcrScannerScreen(
    ocrState: OcrUiState,
    onScanText: (String) -> Unit,
    onSaveRecord: (OcrRecordResult, String, String, String, String) -> Unit, // result, text, dept, disease, imageUrl
    onAddManualRecord: (String, String, String, String, String, String, String, String, String, String, String) -> Unit, // title, pat, doc, hosp, cat, sum, diag, meds, dept, disease, imageUrl
    onReset: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var selectedReportPreset by remember { mutableStateOf<String?>(null) }
    var showManualForm by remember { mutableStateOf(false) }

    // Simulated Camera state trackers
    var showSimulatedCamera by remember { mutableStateOf(false) }
    var cameraCapturedImage by remember { mutableStateOf("") }

    // Prebuilt premium medical mock text samples for quick scanning verification
    val reportPresets = mapOf(
        "Cardio Rx" to """
            Patient: John Doe, DOB: 05-04-1985. Date: May 20, 2026.
            Dr. Peter Vance, MD. Redwood Cardiology.
            Rx Metoprolol Succinate 25mg Once daily with dinner.
            Refill #5 of Atorvastatin 20mg Once daily at bedtime.
            Diagnosis: Hypertension & Borderline Lipids.
            Clinical instruction: Sodiurn restricted diet.
        """.trimIndent(),

        "Blood Panel" to """
            Metro Health Diagnostic Laboratories. Patient Name: John Doe. Date: May 12, 2026.
            Hemoglobin: 14.8 g/dL (Normal).
            Total Cholesterol: 245 mg/dL (High Reference Level).
            LDL Cholesterol: 162 mg/dL. HDL Cholesterol: 42 mg/dL.
            Clinical Doctor: Dr. Elizabeth Thorne.
            Findings consistent with Mild Lipemia.
        """.trimIndent(),

        "Lung Scan" to """
            St. Jude Medical Dept of Radiology. Patient Name: John Doe. Date of Scan: April 10, 2026.
            Scan Type: PA/Lateral Chest Lung Radiography.
            Consulting Radiologist: Dr. Robert S. Chen, MD.
            Findings: Heart size is normal. Pulmonary fields are clear. No active consolidation, mass, or pleural effusion.
            Impression: Fully normal healthy chest radiography.
        """.trimIndent()
    )

    // Manual Creation fields state
    var manualTitle by remember { mutableStateOf("") }
    var manualPatient by remember { mutableStateOf("") }
    var manualDoctor by remember { mutableStateOf("") }
    var manualHospital by remember { mutableStateOf("") }
    var manualCategory by remember { mutableStateOf("Prescription") }
    var manualSummary by remember { mutableStateOf("") }
    var manualDiagnoses by remember { mutableStateOf("") }
    var manualMeds by remember { mutableStateOf("") }

    // Brand new fields for organizing flexibility
    var manualDept by remember { mutableStateOf("General Medicine") }
    var manualDisease by remember { mutableStateOf("General Wellness") }
    var manualDiseaseCustom by remember { mutableStateOf("") }
    var manualImageUrl by remember { mutableStateOf("") }

    var showManualDeptDropdown by remember { mutableStateOf(false) }
    var showManualDiseaseDropdown by remember { mutableStateOf(false) }

    val categoriesList = listOf("Prescription", "Lab Report", "Vaccine", "Discharge Summary", "Radiology / Scan", "Other")

    // Camera Scan Dialog
    if (showSimulatedCamera) {
        Dialog(
            onDismissRequest = { showSimulatedCamera = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var selectedCameraPreset by remember { mutableStateOf("Cardio Rx") }
            var triggerFlash by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            
            // Scanner animations
            val infiniteTransition = rememberInfiniteTransition(label = "scanner")
            val laserOffset by infiniteTransition.animateFloat(
                initialValue = 0.0f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "laser"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Video stream simulation backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(50.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(2.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "📸 REPORT PAPER FRAME",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFC2E7FF),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Icon(
                            imageVector = when (selectedCameraPreset) {
                                "Cardio Rx" -> Icons.Filled.HeartBroken
                                "Blood Panel" -> Icons.Filled.Biotech
                                else -> Icons.Filled.Coronavirus
                            },
                            contentDescription = null,
                            tint = Color.LightGray.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Frame the paper test or recipe card clearly within the viewfinder lens guides.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Simulated sweep laser line
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val strokeY = maxHeight * laserOffset
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .offset(y = strokeY)
                                .background(Color.Green)
                                .shadow(8.dp, spotColor = Color.Green)
                        )
                    }
                }
                
                // Overlay Viewfinder target corners
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val m = 50f
                    // Top Left
                    drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(m, m), end = androidx.compose.ui.geometry.Offset(m + 40f, m), strokeWidth = 8f)
                    drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(m, m), end = androidx.compose.ui.geometry.Offset(m, m + 40f), strokeWidth = 8f)
                    // Top Right
                    drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w - m, m), end = androidx.compose.ui.geometry.Offset(w - m - 40f, m), strokeWidth = 8f)
                    drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w - m, m), end = androidx.compose.ui.geometry.Offset(w - m, m + 40f), strokeWidth = 8f)
                    // Bottom Left
                    drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(m, h - m), end = androidx.compose.ui.geometry.Offset(m + 40f, h - m), strokeWidth = 8f)
                    drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(m, h - m), end = androidx.compose.ui.geometry.Offset(m, h - m - 40f), strokeWidth = 8f)
                    // Bottom Right
                    drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w - m, h - m), end = androidx.compose.ui.geometry.Offset(w - m - 40f, h - m), strokeWidth = 8f)
                    drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(w - m, h - m), end = androidx.compose.ui.geometry.Offset(w - m, h - m - 40f), strokeWidth = 8f)
                }

                // Shutter triggering flash panel animation
                AnimatedVisibility(
                    visible = triggerFlash,
                    enter = fadeIn(animationSpec = tween(100)),
                    exit = fadeOut(animationSpec = tween(400))
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White))
                }

                // Live Camera Settings / Choice selector
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("CHOOSE REPORT TO PLACE UNDER LENS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cardio Rx", "Blood Panel", "Lung Scan").forEach { preset ->
                            val active = selectedCameraPreset == preset
                            Button(
                                onClick = { selectedCameraPreset = preset },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) Color(0xFF006492) else Color(0xFF334155),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(preset, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated tactile trigger Shutter Button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showSimulatedCamera = false }) {
                            Icon(Icons.Filled.HighlightOff, "Cancel", tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    triggerFlash = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(150)
                                        triggerFlash = false
                                        val scannedText = reportPresets[selectedCameraPreset] ?: ""
                                        rawText = scannedText
                                        selectedReportPreset = selectedCameraPreset
                                        cameraCapturedImage = when (selectedCameraPreset) {
                                            "Cardio Rx" -> "preset_cardio"
                                            "Blood Panel" -> "preset_lipid"
                                            else -> "preset_lungs"
                                        }
                                        Toast.makeText(context, "📷 Document captured successfully!", Toast.LENGTH_SHORT).show()
                                        showSimulatedCamera = false
                                    }
                                }
                                .padding(4.dp)
                                .border(4.dp, Color.Black, CircleShape)
                        )

                        IconButton(onClick = {
                            Toast.makeText(context, "Frame sheet clearly and tap shutter trigger.", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Filled.Info, "Help", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                    
                    Text("ACTIVE VIEW PORT - LENS AUTO SCAN F1.8", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Scanner banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Medical Scanner (OCR)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Powered by Google Gemini. Instantly digitize papers, structure summary cards, extract treatments, symptoms, and medical notes.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        if (showManualForm) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Manual Document Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showManualForm = false }) {
                                Icon(Icons.Filled.Close, "Back")
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = manualTitle,
                            onValueChange = { manualTitle = it },
                            label = { Text("Document Title (e.g. Lipids Panel)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = manualPatient,
                            onValueChange = { manualPatient = it },
                            label = { Text("Patient Name") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = manualDoctor,
                            onValueChange = { manualDoctor = it },
                            label = { Text("Consulting Doctor") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = manualHospital,
                            onValueChange = { manualHospital = it },
                            label = { Text("Hospital or Clinic Center") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        // Department Selector Dropdown
                        Text("Medical Department", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            OutlinedButton(
                                onClick = { showManualDeptDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFDDE2EA)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = manualDept, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = showManualDeptDropdown,
                                onDismissRequest = { showManualDeptDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                val deptsList = listOf("Cardiology", "Pediatrics", "Oncology", "Orthopedics", "General Medicine", "Neurology", "Dermatology", "Radiology", "Other")
                                deptsList.forEach { dept ->
                                    DropdownMenuItem(
                                        text = { Text(dept, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            manualDept = dept
                                            showManualDeptDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Disease or Checkup Type dropdown
                        Text("Condition Group Typology (Dropdown flexibility)", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            OutlinedButton(
                                onClick = { showManualDiseaseDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFDDE2EA)),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = manualDisease, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = showManualDiseaseDropdown,
                                onDismissRequest = { showManualDiseaseDropdown = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                val conditions = listOf("Hypertension Checkup", "Cholesterol Tracker", "Lungs Checkup", "Blood Glucose Study", "Dental Care", "Annual Wellness Check", "Allergy Review", "Pediatric Vaccine Log", "Other")
                                conditions.forEach { cond ->
                                    DropdownMenuItem(
                                        text = { Text(cond, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            manualDisease = cond
                                            showManualDiseaseDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        if (manualDisease == "Other") {
                            OutlinedTextField(
                                value = manualDiseaseCustom,
                                onValueChange = { manualDiseaseCustom = it },
                                label = { Text("Specify Custom Condition / Checkup Type") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )
                        }

                        Text("Category Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            items(categoriesList) { cat ->
                                FilterChip(
                                    selected = manualCategory == cat,
                                    onClick = { manualCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = manualSummary,
                            onValueChange = { manualSummary = it },
                            label = { Text("Summary details / Diagnostic impressions") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            maxLines = 3
                        )
                        OutlinedTextField(
                            value = manualDiagnoses,
                            onValueChange = { manualDiagnoses = it },
                            label = { Text("Symptoms / Diagnoses (comma separated)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = manualMeds,
                            onValueChange = { manualMeds = it },
                            label = { Text("Treatment / Medicines (comma separated)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        )

                        // Camera visual attachments selector in manual form
                        Text("Add Scanned Photo Attachment (Optional)", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("preset_cardio" to "ECG Line", "preset_lipid" to "Lab Chemistry", "preset_lungs" to "Chest XRAY").forEach { (code, lbl) ->
                                val active = manualImageUrl == code
                                OutlinedButton(
                                    onClick = { manualImageUrl = if (active) "" else code },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (active) Color(0xFF006492) else Color(0xFFDDE2EA)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (active) Color(0xFFE3F2FD) else Color.White)
                                ) {
                                    Text(lbl, fontSize = 11.sp, color = if (active) Color(0xFF006492) else Color.Black)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val finalDisease = if (manualDisease == "Other") {
                                    manualDiseaseCustom.ifBlank { "Custom General Checkup" }
                                } else {
                                    manualDisease
                                }
                                onAddManualRecord(
                                    manualTitle, manualPatient, manualDoctor, manualHospital,
                                    manualCategory, manualSummary, manualDiagnoses, manualMeds,
                                    manualDept, finalDisease, manualImageUrl
                                )
                                showManualForm = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Save, "Save")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save to Vault Database")
                        }
                    }
                }
            }
        } else {
            item {
                when (ocrState) {
                    is OcrUiState.Idle -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // --- Interactive Camera click trigger! ---
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSimulatedCamera = true },
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color(0xFFC2E7FF)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF006492)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("📸 Click Report Photo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF001D35))
                                        Text("Position report under camera lens to scan & structure details instantly using Google Gemini", fontSize = 12.sp, color = Color(0xFF41484D))
                                    }
                                    Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFF001D35))
                                }
                            }

                            Text(
                                text = "Select a Medical Presets to Simulate Scanning:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                            )

                            // Quick Presets Choices
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                reportPresets.forEach { (name, text) ->
                                    val isSelected = selectedReportPreset == name
                                    OutlinedCard(
                                        onClick = {
                                            selectedReportPreset = name
                                            rawText = text
                                            cameraCapturedImage = when (name) {
                                                "Cardio Rx" -> "preset_cardio"
                                                "Blood Panel" -> "preset_lipid"
                                                else -> "preset_lungs"
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }

                            // Custom Input field (can paste or edit scanned text)
                            OutlinedTextField(
                                value = rawText,
                                onValueChange = {
                                    rawText = it
                                    selectedReportPreset = null
                                    if (cameraCapturedImage.isBlank()) {
                                        cameraCapturedImage = "custom"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                placeholder = {
                                    Text(
                                        "Or paste custom clinical report details here...\n\nExample: Patient: Peter Smith, diagnosis: chronic broncho, Rx: Symbicort inhaler daily..."
                                    )
                                },
                                label = { Text("Clinical Document / OCR Input Text") },
                                maxLines = 10,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = { if (rawText.isNotBlank()) onScanText(rawText) },
                                enabled = rawText.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Icon(Icons.Filled.DocumentScanner, "OCR Analyze")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A.I. Analyze & Structure (OCR)")
                            }

                            OutlinedButton(
                                onClick = { showManualForm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Keyboard, "Manual")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enter Record Details Manually")
                            }
                        }
                    }

                    is OcrUiState.Scanning -> {
                        Box(
                            modifier = Modifier
                                  .fillMaxWidth()
                                  .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(64.dp),
                                    strokeWidth = 4.dp
                                )

                                val scanningNotes = listOf(
                                    "Isolating clinical terms...",
                                    "Consulting medical database vectors...",
                                    "Generating clinical summary card...",
                                    "Compiling structured diagnostic JSON..."
                                )
                                var index by remember { mutableStateOf(0) }
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        kotlinx.coroutines.delay(1200)
                                        index = (index + 1) % scanningNotes.size
                                    }
                                }

                                Text(
                                    text = "Analyzing with Google Gemini...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = scanningNotes[index],
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is OcrUiState.Success -> {
                        val result = (ocrState as OcrUiState.Success).result
                        
                        // Dropdown adjusters right on Success Screen!
                        var ocrDept by remember { mutableStateOf(
                            when {
                                result.category.contains("Prescription", true) || result.title.contains("Blood", true) -> "Cardiology"
                                result.category.contains("Scan", true) -> "Radiology"
                                else -> "General Medicine"
                            }
                        ) }
                        var ocrDisease by remember { mutableStateOf(
                            when {
                                result.category.contains("Prescription", true) -> "Hypertension Checkup"
                                result.category.contains("report", true) -> "Cholesterol Tracker"
                                result.category.contains("Scan", true) -> "Lungs Checkup"
                                else -> "General Wellness"
                            }
                        ) }
                        
                        var ocrShowDeptDropdown by remember { mutableStateOf(false) }
                        var ocrShowDiseaseDropdown by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "A.I. Scanning Confirmed!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(onClick = onReset) {
                                        Icon(Icons.Filled.Refresh, "Retry OCR")
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Title:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(result.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Patient:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(result.patientName, fontSize = 13.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(result.category, fontSize = 13.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Consulting Doctor:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(result.doctorName, fontSize = 13.sp)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Clinic / Hospital:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(result.clinicOrHospital, fontSize = 13.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("A.I. Summary:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(result.summary, fontSize = 13.sp, maxLines = 4, modifier = Modifier.padding(bottom = 12.dp))

                                if (result.diagnoses.isNotEmpty()) {
                                    Text("Symptoms / Findings:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(result.diagnoses.joinToString(", "), fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                                }

                                if (result.prescribedMeds.isNotEmpty()) {
                                    Text("Treatment Actions / Medications:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(result.prescribedMeds.joinToString(", "), fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))
                                }

                                // Classify Dropdowns of flexibility in Success verification!
                                Text("Classify Department & Condition", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Dept dropdown
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { ocrShowDeptDropdown = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFFDDE2EA))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(ocrDept, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = ocrShowDeptDropdown,
                                            onDismissRequest = { ocrShowDeptDropdown = false }
                                        ) {
                                            val deptsList = listOf("Cardiology", "Pediatrics", "Oncology", "Orthopedics", "General Medicine", "Neurology", "Dermatology", "Radiology", "Other")
                                            deptsList.forEach { dept ->
                                                DropdownMenuItem(
                                                    text = { Text(dept, fontSize = 12.sp) },
                                                    onClick = {
                                                        ocrDept = dept
                                                        ocrShowDeptDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Disease category dropdown
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { ocrShowDiseaseDropdown = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, Color(0xFFDDE2EA))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(ocrDisease, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = ocrShowDiseaseDropdown,
                                            onDismissRequest = { ocrShowDiseaseDropdown = false }
                                        ) {
                                            val conditions = listOf("Hypertension Checkup", "Cholesterol Tracker", "Lungs Checkup", "Blood Glucose Study", "Dental Care", "Annual Wellness Check", "Allergy Review", "Pediatric Vaccine Log", "Other")
                                            conditions.forEach { cond ->
                                                DropdownMenuItem(
                                                    text = { Text(cond, fontSize = 12.sp) },
                                                    onClick = {
                                                        ocrDisease = cond
                                                        ocrShowDiseaseDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = { onSaveRecord(result, rawText, ocrDept, ocrDisease, cameraCapturedImage) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Beenhere, "Approve & save")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Approve & Store in Patient Vault")
                                }
                            }
                        }
                    }

                    is OcrUiState.Error -> {
                        val errorState = ocrState as OcrUiState.Error
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Warning, "Error icon", tint = MaterialTheme.colorScheme.error)
                                    Text("Scanning Analysis Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(errorState.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.height(12.dp))

                                if (errorState.isApiKeyMissing) {
                                    Text(
                                        text = "Tip: Please input your GEMINI_API_KEY inside the 'Secrets' tab in AI Studio panel on the left to activate instant real analysis. We can also auto-simulate a mock cloud scanning response instantly below!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Local Mock Fallback button to keep app extremely functional
                                    Button(
                                        onClick = {
                                            val categoryDetermined = when {
                                                rawText.contains("Rx", true) || rawText.contains("refill", true) -> "Prescription"
                                                rawText.contains("blood", true) || rawText.contains("diagnostic", true) || rawText.contains("mg/dL", true) -> "Lab Report"
                                                rawText.contains("radiography", true) || rawText.contains("X-Ray", true) || rawText.contains("scan", true) -> "Radiology / Scan"
                                                else -> "Other"
                                            }
                                            val simulatedResult = OcrRecordResult(
                                                title = if (rawText.contains("prescription", true) || rawText.contains("Cardiology", true)) "Metoprolol Prescription" else "Clinical Blood Panel Extractions",
                                                patientName = "John Doe (Simulated OCR)",
                                                doctorName = "Dr. Elizabeth Thorne, MD",
                                                clinicOrHospital = "Metro General Health Diagnostics",
                                                category = categoryDetermined,
                                                summary = "Simulated high-reliability medical extraction. Lungs clear, standard vitals are consistent with age profile. Heart health markers fully validated.",
                                                diagnoses = listOf("Borderline Hypertension", "General Wellness Visit"),
                                                prescribedMeds = listOf("Metoprolol Succinate 25mg daily", "Atorvastatin 10mg Once sleep-time")
                                            )
                                            
                                            val simulatedDept = when (categoryDetermined) {
                                                "Prescription" -> "Cardiology"
                                                "Lab Report" -> "Cardiology"
                                                "Radiology / Scan" -> "Radiology"
                                                else -> "General Medicine"
                                            }
                                            val simulatedDisease = when (categoryDetermined) {
                                                "Prescription" -> "Hypertension Checkup"
                                                "Lab Report" -> "Cholesterol Tracker"
                                                "Radiology / Scan" -> "Lungs Checkup"
                                                else -> "General Wellness"
                                            }
                                            val simulatedImage = cameraCapturedImage.ifBlank {
                                                when (categoryDetermined) {
                                                    "Prescription" -> "preset_cardio"
                                                    "Lab Report" -> "preset_lipid"
                                                    "Radiology / Scan" -> "preset_lungs"
                                                    else -> ""
                                                }
                                            }
                                            onSaveRecord(simulatedResult, rawText, simulatedDept, simulatedDisease, simulatedImage)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006492))
                                    ) {
                                        Text("Perform Local Offline Mock Structuring")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onReset,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Back to Scanner Input")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// --- TAB 2: EMERGENCY PROFILE CARD ---

@Composable
fun EmergencyProfileScreen(
    profile: EmergencyProfile,
    onTriggerFullBright: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Interactive Pass trigger Red panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddModerator,
                        contentDescription = "Paramedic Medpass Icon",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                    Text(
                        text = "EMERGENCY MEDPASS ID",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Instantly trigger high-contrast solar screen mode so paramedics and clinical staff can scan your core vitals without passcodes.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = onTriggerFullBright,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = EmergencyRed),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Filled.FlashOn, "Full bright", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("OPEN HIGH-CONTRAST MEDPASS ID", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. High-Tech PHYSICAL SMART CARD / MEDPASS ID Card (Natively drawn chip and NFC)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background soft dynamic gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = if (isDark) listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                                    else listOf(Color(0xFFF8FAFC), Color(0xFFE0E7FF)),
                                    start = Offset(0f, 0f),
                                    end = Offset(1000f, 1000f)
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Card Header (MedLifeStyle logo, Brand title, RFID NFC canvas indicator)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MedLifeStyleLogo(sizeDp = 30.dp)
                                Column {
                                    Text(
                                        text = "MedLifeStyle",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "OFFLINE SMART PASS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            // Call contactless canvas RFID icon symbol
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(20.dp)) {
                                    drawArc(
                                        color = if (isDark) Color(0xFF6366F1) else Color(0xFF4338CA),
                                        startAngle = -45f,
                                        sweepAngle = 90f,
                                        useCenter = false,
                                        style = Stroke(width = 3f, cap = StrokeCap.Round),
                                        topLeft = Offset(2f, 2f),
                                        size = Size(10f, 16f)
                                    )
                                    drawArc(
                                        color = if (isDark) Color(0xFF6366F1) else Color(0xFF4338CA),
                                        startAngle = -45f,
                                        sweepAngle = 90f,
                                        useCenter = false,
                                        style = Stroke(width = 3f, cap = StrokeCap.Round),
                                        topLeft = Offset(-4f, -4f),
                                        size = Size(20f, 28f)
                                    )
                                    drawArc(
                                        color = if (isDark) Color(0xFF6366F1) else Color(0xFF4338CA),
                                        startAngle = -45f,
                                        sweepAngle = 90f,
                                        useCenter = false,
                                        style = Stroke(width = 3f, cap = StrokeCap.Round),
                                        topLeft = Offset(-10f, -10f),
                                        size = Size(30f, 40f)
                                    )
                                }
                            }
                        }

                        // Smart Golden Microchip visual container
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gold microchip canvas drawing
                            Canvas(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEAB308))
                            ) {
                                val w = size.width
                                val h = size.height
                                drawLine(Color(0xFF854D0E), Offset(w * 0.3f, 0f), Offset(w * 0.3f, h), strokeWidth = 2f)
                                drawLine(Color(0xFF854D0E), Offset(w * 0.7f, 0f), Offset(w * 0.7f, h), strokeWidth = 2f)
                                drawLine(Color(0xFF854D0E), Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 2f)
                                drawCircle(Color.White.copy(alpha = 0.3f), radius = 3f, center = Offset(w / 2f, h / 2f))
                            }

                            Column {
                                Text(
                                    text = profile.fullName.ifBlank { "Unspecified Holder" }.uppercase(Locale.getDefault()),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "PASS MEMBER ID: PAS-${profile.fullName.take(3).uppercase(Locale.getDefault())}-${(profile.dateOfBirth.hashCode() % 10000).coerceAtLeast(1000)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Card Footer stats (Blood group, Allergies alert)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column {
                                    Text("BLOOD GROUP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = profile.bloodType.ifBlank { "--" },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (profile.bloodType.isNotBlank()) EmergencyRed else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(24.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                                Column {
                                    Text("ALLERGIES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = profile.allergies.ifBlank { "None Recorded" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (profile.allergies.isNotBlank() && !profile.allergies.equals("None", ignoreCase = true)) EmergencyRed else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 140.dp)
                                    )
                                }
                            }

                            // Verified stamp
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Verified, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Text("VERIFIED", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. DEMOGRAPHIC VITALS Group
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "DEMOGRAPHIC IDENTIFICATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ProfileValueRow(label = "Primary Holder Name", value = profile.fullName, leadingIcon = Icons.Filled.Person)
                    ProfileValueRow(label = "Patient Email Address", value = if (profile.userEmail.isBlank()) "john.doe@example.com" else profile.userEmail, leadingIcon = Icons.Filled.Email)
                    ProfileValueRow(label = "Official Date of Birth", value = profile.dateOfBirth, leadingIcon = Icons.Filled.CalendarMonth)
                    ProfileValueRow(label = "Organ Donor Authorization Status", value = if (profile.organDonor) "Authorized Organ Donor" else "No / Unspecified", leadingIcon = Icons.Filled.VolunteerActivism, highlightColor = if (profile.organDonor) MaterialTheme.colorScheme.primary else null)
                }
            }
        }

        // 4. CRITICAL CLINICAL ADVISORY Group
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, EmergencyRed.copy(alpha = 0.35f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CRITICAL MEDICAL ADVISORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = EmergencyRed,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Icon(Icons.Filled.Warning, null, tint = EmergencyRed, modifier = Modifier.size(16.dp))
                    }

                    ProfileValueRow(label = "Physiological Blood Type", value = profile.bloodType, leadingIcon = Icons.Filled.WaterDrop, highlightColor = EmergencyRed)
                    ProfileValueRow(label = "Diagnosed Lethal Allergies", value = profile.allergies, leadingIcon = Icons.Filled.Warning, highlightColor = EmergencyRed)
                    ProfileValueRow(label = "Active Chronic Conditions", value = profile.chronicConditions, leadingIcon = Icons.Filled.HeartBroken, highlightColor = UrgentGold)
                    ProfileValueRow(label = "Ongoing Maintenance Medications", value = profile.currentMedications, leadingIcon = Icons.Filled.MedicalServices)
                }
            }
        }

        // 5. INSURANCE & ASSISTANCE Group
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "EMERGENCY ASSISTANCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    ProfileValueRow(label = "First Responder Contact Name", value = profile.emergencyContactName, leadingIcon = Icons.Filled.PhoneInTalk)
                    ProfileValueRow(label = "Emergency Telephone Helpline", value = profile.emergencyContactPhone, leadingIcon = Icons.Filled.Call, highlightColor = MaterialTheme.colorScheme.primary)
                    ProfileValueRow(label = "Emergency Contact Email", value = if (profile.emergencyContactEmail.isBlank()) "jane.doe@example.com" else profile.emergencyContactEmail, leadingIcon = Icons.Filled.Email)
                    ProfileValueRow(label = "Insurance Provider Name", value = profile.insuranceProvider.ifBlank { "Not Specified" }, leadingIcon = Icons.Filled.Shield)
                    ProfileValueRow(label = "Insurance Cover Policy Number", value = profile.insuranceNumber.ifBlank { "Not Specified" }, leadingIcon = Icons.Filled.Shield)
                }
            }
        }
    }
}

@Composable
fun ProfileValueRow(
    label: String,
    value: String,
    leadingIcon: ImageVector,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = highlightColor ?: MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = value.ifBlank { "Unset" },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (highlightColor != null && value.isNotBlank() && value != "None") highlightColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}


// --- ACCESSORY CUSTOM DRAWINGS CANVAS (NO EXTERNAL LIBS FOR COMPATIBILITY) ---

@Composable
fun QrCodeCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizePx = size.width
        val blockSize = sizePx / 10

        // Corner square 1 (Top Left)
        drawRect(Color.Black, Offset(0f, 0f), Size(blockSize * 3, blockSize * 3))
        drawRect(Color.White, Offset(blockSize, blockSize), Size(blockSize, blockSize))

        // Corner square 2 (Top Right)
        drawRect(Color.Black, Offset(sizePx - blockSize * 3, 0f), Size(blockSize * 3, blockSize * 3))
        drawRect(Color.White, Offset(sizePx - blockSize * 2, blockSize), Size(blockSize, blockSize))

        // Corner square 3 (Bottom Left)
        drawRect(Color.Black, Offset(0f, sizePx - blockSize * 3), Size(blockSize * 3, blockSize * 3))
        drawRect(Color.White, Offset(blockSize, sizePx - blockSize * 2), Size(blockSize, blockSize))

        // Corner square 4 (Bottom Right alignment marker)
        drawRect(Color.Black, Offset(sizePx - blockSize * 2, sizePx - blockSize * 2), Size(blockSize, blockSize))

        // Dummy random matrix squares to resemble authentic QR codes
        val seed = Random(15)
        for (i in 3 until 7) {
            for (j in 0 until 10) {
                if (seed.nextBoolean()) {
                    drawRect(
                        Color.Black,
                        Offset(i * blockSize, j * blockSize),
                        Size(blockSize, blockSize)
                    )
                }
            }
        }
        for (i in 0 until 10) {
            for (j in 3 until 7) {
                if (seed.nextBoolean()) {
                    drawRect(
                        Color.Black,
                        Offset(i * blockSize, j * blockSize),
                        Size(blockSize, blockSize)
                    )
                }
            }
        }
    }
}


// --- ACCESSIBILITY HIGH CONTRAST FULL BRIGHT EMERGENCY OVERLAY ---

@Composable
fun EmergencyFullBrightOverlay(
    profile: EmergencyProfile,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High visibility header banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocalHospital,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text(
                    text = "EMERGENCY HEALTH PASS",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Critical Medpass ID - Unlocked Access",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }

        // Critical Patient Detail Cards (Extremely high readability)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmergencyValueCard(label = "FULL NAME", value = profile.fullName, important = true)
            EmergencyValueCard(label = "PATIENT EMAIL ADDRESS", value = if (profile.userEmail.isBlank()) "john.doe@example.com" else profile.userEmail, important = true)
            EmergencyValueCard(label = "BLOOD GROUP", value = profile.bloodType, important = true, isCriticalRedAlert = true)
            EmergencyValueCard(label = "LETHAL ALLERGIES", value = profile.allergies, important = true, isCriticalRedAlert = true)
            EmergencyValueCard(label = "CHRONIC MEDICAL CONDITIONS", value = profile.chronicConditions, important = true)
            EmergencyValueCard(label = "ACTIVE MEDICATION INTAKE", value = profile.currentMedications)
            EmergencyValueCard(label = "PRIMARY EMERGENCY CONTACT NAME", value = profile.emergencyContactName, important = true)
            EmergencyValueCard(label = "EMERGENCY CONTACT PHONE", value = profile.emergencyContactPhone, important = true)
            EmergencyValueCard(label = "EMERGENCY CONTACT EMAIL", value = if (profile.emergencyContactEmail.isBlank()) "jane.doe@example.com" else profile.emergencyContactEmail, important = true)
            EmergencyValueCard(label = "INSURANCE PROVIDER", value = profile.insuranceProvider.ifBlank { "Not Specified" })
            EmergencyValueCard(label = "INSURANCE POLICY ID", value = profile.insuranceNumber.ifBlank { "Not Specified" })
        }

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
            shape = RoundedCornerShape(50.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("DISMISS HEALTH PASS")
        }
    }
}

@Composable
fun EmergencyValueCard(
    label: String,
    value: String,
    important: Boolean = false,
    isCriticalRedAlert: Boolean = false
) {
    val containerColor = when {
        isCriticalRedAlert && value.isNotBlank() && value != "None" -> Color(0xFFFEEBEE)
        important -> Color(0xFFECEFF1)
        else -> Color.White
    }
    val outlineColor = if (isCriticalRedAlert && value.isNotBlank() && value != "None") Color.Red else Color.Black

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (important) 2.dp else 1.dp, outlineColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isCriticalRedAlert && value.isNotBlank() && value != "None") Color.Red else Color.DarkGray,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value.ifBlank { "NOT SPECIFIED" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun EditRecordDialog(
    record: MedicalRecord,
    onDismiss: () -> Unit,
    onSave: (MedicalRecord) -> Unit
) {
    var title by remember { mutableStateOf(record.title) }
    var patientName by remember { mutableStateOf(record.patientName) }
    var doctorName by remember { mutableStateOf(record.doctorName) }
    var clinicOrHospital by remember { mutableStateOf(record.clinicOrHospital) }
    var category by remember { mutableStateOf(record.category) }
    var department by remember { mutableStateOf(record.department) }
    var diseaseOrCheckupType by remember { mutableStateOf(record.diseaseOrCheckupType) }
    var summary by remember { mutableStateOf(record.summary) }
    var diagnoses by remember { mutableStateOf(record.diagnoses) }
    var prescribedMeds by remember { mutableStateOf(record.prescribedMeds) }

    // Dropdowns visibility
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showDeptDropdown by remember { mutableStateOf(false) }
    var showDiseaseDropdown by remember { mutableStateOf(false) }

    val categoriesList = listOf("Prescription", "Lab Report", "Vaccine", "Discharge Summary", "Radiology / Scan", "Other")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFC2E7FF), RoundedCornerShape(24.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE3F2FD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Edit, null, tint = Color(0xFF006492), modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "Edit Medical Record",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D35)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Dismiss")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEFF2F5))

                // Scrollable fields list
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Document Title") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = doctorName,
                            onValueChange = { doctorName = it },
                            label = { Text("Consulting Doctor") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = clinicOrHospital,
                            onValueChange = { clinicOrHospital = it },
                            label = { Text("Hospital / Clinic") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Flexible Category dropdown
                    Text("Record Category Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF41484D))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showCategoryDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFDDE2EA)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(category, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            categoriesList.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        category = cat
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Flexible Department dropdown
                    Text("Medical Department", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF41484D))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showDeptDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFDDE2EA)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(department, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showDeptDropdown,
                            onDismissRequest = { showDeptDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            val deptsList = listOf("Cardiology", "Pediatrics", "Oncology", "Orthopedics", "General Medicine", "Neurology", "Dermatology", "Radiology", "Other")
                            deptsList.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        department = dept
                                        showDeptDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Flexible Disease or Checkup Type dropdown
                    Text("Condition Group Typology (Dropdown flexibility)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF41484D))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showDiseaseDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFDDE2EA)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(diseaseOrCheckupType, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(20.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showDiseaseDropdown,
                            onDismissRequest = { showDiseaseDropdown = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            val conditions = listOf("Hypertension Checkup", "Cholesterol Tracker", "Lungs Checkup", "Blood Glucose Study", "Dental Care", "Annual Wellness Check", "Allergy Review", "Pediatric Vaccine Log", "Other")
                            conditions.forEach { cond ->
                                DropdownMenuItem(
                                    text = { Text(cond, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        diseaseOrCheckupType = cond
                                        showDiseaseDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = { Text("Clinical Summary & Evaluation") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    OutlinedTextField(
                        value = diagnoses,
                        onValueChange = { diagnoses = it },
                        label = { Text("Symptoms / Diagnoses (comma separated)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = prescribedMeds,
                        onValueChange = { prescribedMeds = it },
                        label = { Text("Medications / Treatments (comma separated)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom actions buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val updatedRecord = record.copy(
                                title = title,
                                patientName = patientName,
                                doctorName = doctorName,
                                clinicOrHospital = clinicOrHospital,
                                category = category,
                                department = department,
                                diseaseOrCheckupType = diseaseOrCheckupType,
                                summary = summary,
                                diagnoses = diagnoses,
                                prescribedMeds = prescribedMeds
                            )
                            onSave(updatedRecord)
                        },
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Beenhere, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Patient Record")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSduiScreen(
    viewModel: MedicalViewModel,
    emergencyProfile: EmergencyProfile,
    recordsCount: Int,
    syncState: SyncUiState,
    onSaveProfile: (EmergencyProfile) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val forceDark by viewModel.forceDarkTheme.collectAsState()
    val currentThemeIsDark = forceDark ?: isDark

    val syncString = when (syncState) {
        is SyncUiState.Synced -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            "Synced at ${sdf.format(Date(syncState.time))}"
        }
        is SyncUiState.Syncing -> "Syncing..."
        else -> "Never Synced"
    }

    val sduiLayout = remember(emergencyProfile, recordsCount, currentThemeIsDark, syncString) {
        viewModel.getProfileSduiLayout(
            profile = emergencyProfile,
            recordsCount = recordsCount,
            isDark = currentThemeIsDark,
            syncStatus = syncString
        )
    }

    var isEditing by remember { mutableStateOf(false) }

    // Backup state bindings
    var name by remember(emergencyProfile) { mutableStateOf(emergencyProfile.fullName) }
    var dob by remember(emergencyProfile) { mutableStateOf(emergencyProfile.dateOfBirth) }
    var bloodType by remember(emergencyProfile) { mutableStateOf(emergencyProfile.bloodType) }
    var allergies by remember(emergencyProfile) { mutableStateOf(emergencyProfile.allergies) }
    var conditions by remember(emergencyProfile) { mutableStateOf(emergencyProfile.chronicConditions) }
    var meds by remember(emergencyProfile) { mutableStateOf(emergencyProfile.currentMedications) }
    var contactName by remember(emergencyProfile) { mutableStateOf(emergencyProfile.emergencyContactName) }
    var contactPhone by remember(emergencyProfile) { mutableStateOf(emergencyProfile.emergencyContactPhone) }
    var insurance by remember(emergencyProfile) { mutableStateOf(emergencyProfile.insuranceProvider) }
    var policyId by remember(emergencyProfile) { mutableStateOf(emergencyProfile.insuranceNumber) }
    var donor by remember(emergencyProfile) { mutableStateOf(emergencyProfile.organDonor) }
    var userEmail by remember(emergencyProfile) { mutableStateOf(emergencyProfile.userEmail) }
    var emergencyContactEmail by remember(emergencyProfile) { mutableStateOf(emergencyProfile.emergencyContactEmail) }

    val bloodTypesList = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = if (isEditing) "Edit Profile Details" else "Health Profile ID",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isEditing) "Enter updated demographic and health records" else "Local medical passport configuration layout",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isEditing) {
                FilledTonalButton(
                    onClick = { isEditing = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Profile Info Button",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

        if (isEditing) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Demographics & Identifiers",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Patient Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    val calendar = Calendar.getInstance()
                    var initialYear = calendar.get(Calendar.YEAR)
                    var initialMonth = calendar.get(Calendar.MONTH)
                    var initialDay = calendar.get(Calendar.DAY_OF_MONTH)

                    val parts = dob.trim().split("-")
                    if (parts.size == 3) {
                        try {
                            if (parts[0].length == 4) {
                                val parsedYear = parts[0].toInt()
                                val parsedMonth = parts[1].toInt()
                                val parsedDay = parts[2].toInt()
                                initialYear = parsedYear
                                initialMonth = (parsedMonth - 1).coerceIn(0, 11)
                                initialDay = parsedDay.coerceIn(1, 31)
                            } else {
                                val parsedDay = parts[0].toInt()
                                val parsedMonth = parts[1].toInt()
                                val parsedYear = parts[2].toInt()
                                initialYear = parsedYear
                                initialMonth = (parsedMonth - 1).coerceIn(0, 11)
                                initialDay = parsedDay.coerceIn(1, 31)
                            }
                        } catch (e: Exception) {
                            initialYear = calendar.get(Calendar.YEAR)
                            initialMonth = calendar.get(Calendar.MONTH)
                            initialDay = calendar.get(Calendar.DAY_OF_MONTH)
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dob,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date of Birth (DD-MM-YYYY)") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = "Calendar Picker Icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    val datePickerDialog = android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val formattedDay = String.format("%02d", dayOfMonth)
                                            val formattedMonth = String.format("%02d", month + 1)
                                            dob = "$formattedDay-$formattedMonth-$year"
                                        },
                                        initialYear,
                                        initialMonth,
                                        initialDay
                                    )
                                    datePickerDialog.show()
                                }
                        )
                    }
                }
                item {
                    var showBloodTypeDropdown by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = bloodType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Blood Group") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Select Blood Group Option",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showBloodTypeDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showBloodTypeDropdown,
                            onDismissRequest = { showBloodTypeDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            bloodTypesList.forEach { bt ->
                                DropdownMenuItem(
                                    text = { Text(bt, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        bloodType = bt
                                        showBloodTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { userEmail = it },
                        label = { Text("Patient Email Address") },
                        placeholder = { Text("john.doe@example.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    Text(
                        text = "Medical Profile Baseline",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = allergies,
                        onValueChange = { allergies = it },
                        label = { Text("Lethal Allergies (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = conditions,
                        onValueChange = { conditions = it },
                        label = { Text("Chronic Conditions (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = meds,
                        onValueChange = { meds = it },
                        label = { Text("Current Care Medications") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    Text(
                        text = "Emergency Responder Contact",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Emergency Contact Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Emergency Contact Phone (e.g., STD +91-98765-43210)") },
                        placeholder = { Text("+91-98765-43210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = emergencyContactEmail,
                        onValueChange = { emergencyContactEmail = it },
                        label = { Text("Emergency Contact Email") },
                        placeholder = { Text("jane.doe@example.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    Text(
                        text = "Insurance Plan & Organ Consent",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = insurance,
                        onValueChange = { insurance = it },
                        label = { Text("Insurance Provider Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = policyId,
                        onValueChange = { policyId = it },
                        label = { Text("Insurance Cover Policy Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { donor = !donor }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(checked = donor, onCheckedChange = { donor = it })
                        Text("Approved Registered Organ Donor", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // Cancel and restore values
                                name = emergencyProfile.fullName
                                dob = emergencyProfile.dateOfBirth
                                bloodType = emergencyProfile.bloodType
                                allergies = emergencyProfile.allergies
                                conditions = emergencyProfile.chronicConditions
                                meds = emergencyProfile.currentMedications
                                contactName = emergencyProfile.emergencyContactName
                                contactPhone = emergencyProfile.emergencyContactPhone
                                insurance = emergencyProfile.insuranceProvider
                                policyId = emergencyProfile.insuranceNumber
                                donor = emergencyProfile.organDonor
                                userEmail = emergencyProfile.userEmail
                                emergencyContactEmail = emergencyProfile.emergencyContactEmail

                                isEditing = false
                                Toast.makeText(context, "Editing Cancelled", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                // Save values
                                val updated = emergencyProfile.copy(
                                    fullName = name,
                                    dateOfBirth = dob,
                                    bloodType = bloodType,
                                    allergies = allergies,
                                    chronicConditions = conditions,
                                    currentMedications = meds,
                                    emergencyContactName = contactName,
                                    emergencyContactPhone = contactPhone,
                                    insuranceProvider = insurance,
                                    insuranceNumber = policyId,
                                    organDonor = donor,
                                    userEmail = userEmail,
                                    emergencyContactEmail = emergencyContactEmail
                                )
                                onSaveProfile(updated)
                                isEditing = false
                                Toast.makeText(context, "Profile Changes Saved Successfully", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(sduiLayout.components) { component ->
                    SduiComponentRenderer(
                        component = component,
                        onAction = { actionKey ->
                            when (actionKey) {
                                "toggle_theme" -> {
                                    viewModel.toggleForceDarkTheme()
                                    Toast.makeText(context, "Theme toggled via dynamic SDUI action call", Toast.LENGTH_SHORT).show()
                                }
                                "sync_cloud" -> {
                                    viewModel.syncRecordsToCloud()
                                    Toast.makeText(context, "Sync requested via dynamic SDUI action call", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SduiComponentRenderer(
    component: SduiComponent,
    onAction: (String) -> Unit
) {
    when (component.type) {
        "profile_header" -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = component.title ?: "",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = component.subtitle ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = component.description ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = component.value ?: "",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        "status_banner" -> {
            val isSuccess = component.themeColor == "success"
            val bgColor = if (isSuccess) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
            val strokeColor = if (isSuccess) Color(0xFFC8E6C9) else Color(0xFFFFE0B2)
            val contentColor = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFE65100)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .border(1.dp, strokeColor, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Filled.VerifiedUser else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = component.title ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = contentColor
                        )
                        Text(
                            text = component.subtitle ?: "",
                            fontSize = 11.sp,
                            color = contentColor.copy(alpha = 0.85f),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
        "section_title" -> {
            Text(
                text = component.title ?: "",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
        }
        "stats_grid" -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val chunks = component.items?.chunked(2) ?: emptyList()
                chunks.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            val sduiIcon = when (item.iconName) {
                                "History" -> Icons.Filled.History
                                "Emergency" -> Icons.Filled.Emergency
                                "Star" -> Icons.Filled.Star
                                else -> Icons.Filled.CalendarMonth
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = sduiIcon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = item.value,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        "info_list" -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    component.items?.forEachIndexed { index, item ->
                        val sduiIcon = when (item.iconName) {
                            "Birthday" -> Icons.Filled.Cake
                            "Allergy" -> Icons.Filled.Warning
                            "Heart" -> Icons.Filled.Favorite
                            "Contact" -> Icons.Filled.ContactPhone
                            "Shield" -> Icons.Filled.Shield
                            "Email" -> Icons.Filled.Email
                            else -> Icons.Filled.Star
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = sduiIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = item.value,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (index < (component.items.size - 1)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        }
                    }
                }
            }
        }
        "action_button" -> {
            val isSuccess = component.themeColor == "success"
            val tintColor = if (isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { component.actionKey?.let { onAction(it) } }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tintColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Filled.Sync else Icons.Filled.Palette,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = component.title ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = component.description ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// --- ADD MANUAL RECORD DIALOG (HIGH FIDELITY MATERIAL 3 DESIGN) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        patientName: String,
        doctorName: String,
        clinicOrHospital: String,
        category: String,
        summary: String,
        diagnoses: String,
        prescribedMeds: String,
        department: String,
        diseaseOrCheckupType: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var patientName by remember { mutableStateOf("") }
    var doctorName by remember { mutableStateOf("") }
    var clinicOrHospital by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Prescription") }
    var department by remember { mutableStateOf("General Medicine") }
    var diseaseOrCheckupType by remember { mutableStateOf("General Wellness") }
    var summary by remember { mutableStateOf("") }
    var diagnoses by remember { mutableStateOf("") }
    var prescribedMeds by remember { mutableStateOf("") }

    // Dropdown triggers
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showDeptDropdown by remember { mutableStateOf(false) }
    var showDiseaseDropdown by remember { mutableStateOf(false) }

    val categoriesList = listOf("Prescription", "Lab Report", "Vaccine", "Discharge Summary", "Radiology / Scan", "Other")
    val departmentsList = listOf("General Medicine", "Cardiology", "Neurology", "Pediatrics", "Orthopedics", "Dermatology", "Oncology", "ENT")
    val diseasesList = listOf("General Wellness", "Annual Physical", "Hypertension Control", "Diabetes Checkup", "Flu / Viral Infection", "Injury / Trauma Check", "Allergies follow-up", "Cardiac Assessment")

    val isDark = isSystemInDarkTheme()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MedLifeStyleLogo(sizeDp = 36.dp)
                        Text(
                            text = "Add Medical Record",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close dialog")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Record Title *") },
                        placeholder = { Text("e.g. Lipitor Prescription, Blood Report") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Category dropdown selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = {
                                IconButton(onClick = { showCategoryDropdown = !showCategoryDropdown }) {
                                    Icon(Icons.Filled.ArrowDropDown, "Select category")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showCategoryDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            categoriesList.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Department dropdown selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Medical Department") },
                            trailingIcon = {
                                IconButton(onClick = { showDeptDropdown = !showDeptDropdown }) {
                                    Icon(Icons.Filled.ArrowDropDown, "Select department")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDeptDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showDeptDropdown,
                            onDismissRequest = { showDeptDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            departmentsList.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept) },
                                    onClick = {
                                        department = dept
                                        showDeptDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Disease / Checkup dropdown selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = diseaseOrCheckupType,
                            onValueChange = { diseaseOrCheckupType = it },
                            label = { Text("Condition / Checkup Focus") },
                            trailingIcon = {
                                IconButton(onClick = { showDiseaseDropdown = !showDiseaseDropdown }) {
                                    Icon(Icons.Filled.ArrowDropDown, "Select prebuilt condition")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDiseaseDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showDiseaseDropdown,
                            onDismissRequest = { showDiseaseDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            diseasesList.forEach { disease ->
                                DropdownMenuItem(
                                    text = { Text(disease) },
                                    onClick = {
                                        diseaseOrCheckupType = disease
                                        showDiseaseDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = patientName,
                        onValueChange = { patientName = it },
                        label = { Text("Patient Name") },
                        placeholder = { Text("e.g. John Doe, Self") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = doctorName,
                        onValueChange = { doctorName = it },
                        label = { Text("Clinician / Doctor Name") },
                        placeholder = { Text("e.g. Dr. Sarah Jenkins") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = clinicOrHospital,
                        onValueChange = { clinicOrHospital = it },
                        label = { Text("Clinic or Hospital") },
                        placeholder = { Text("e.g. Metro Medical Center") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = summary,
                        onValueChange = { summary = it },
                        label = { Text("Executive Summary Description") },
                        placeholder = { Text("Provide details about the medical visit or results...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = diagnoses,
                        onValueChange = { diagnoses = it },
                        label = { Text("Coded Diagnoses (Comma Separated)") },
                        placeholder = { Text("e.g. Hypertension, Mild Bronchitis") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = prescribedMeds,
                        onValueChange = { prescribedMeds = it },
                        label = { Text("Prescribed Medications (Comma Separated)") },
                        placeholder = { Text("e.g. Lipitor 10mg QD, Amoxicillin 500mg TID") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    title,
                                    patientName,
                                    doctorName,
                                    clinicOrHospital,
                                    category,
                                    summary,
                                    diagnoses,
                                    prescribedMeds,
                                    department,
                                    diseaseOrCheckupType
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Save Record", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
