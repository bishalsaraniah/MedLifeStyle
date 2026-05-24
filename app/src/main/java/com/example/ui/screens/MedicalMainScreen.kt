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
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Clinical Vault",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.5).sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Secure Records",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Cloud Sync status
                                CloudSyncStatusIndicator(
                                    syncState = syncState,
                                    onSyncClick = { viewModel.syncRecordsToCloud() }
                                )

                                // Search simulation active background badge button
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDDE2EA))
                                        .clickable {
                                            if (currentTab != 0) {
                                                currentTab = 0
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Quick Emergency button (Clinical design matching HTML bg-[#93000A])
                                IconButton(
                                    onClick = { viewModel.triggerEmergencyMode(true) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF93000A))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Emergency,
                                        contentDescription = "Trigger Emergency Pass",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 0) Icons.Filled.FolderCopy else Icons.Outlined.FolderCopy,
                                        contentDescription = "Vault View"
                                    )
                                },
                                label = { Text("Vault", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 1) Icons.Filled.DocumentScanner else Icons.Outlined.DocumentScanner,
                                        contentDescription = "AI Scanner"
                                    )
                                },
                                label = { Text("AI Scan", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 2) Icons.Filled.Shield else Icons.Outlined.Shield,
                                        contentDescription = "MedPass Link"
                                    )
                                },
                                label = { Text("MedPass ID", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 3) Icons.Filled.Share else Icons.Outlined.Share,
                                        contentDescription = "Dr Gateway"
                                    )
                                },
                                label = { Text("Dr. Gateway", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                            )
                            NavigationBarItem(
                                selected = currentTab == 4,
                                onClick = { currentTab = 4 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 4) Icons.Filled.Person else Icons.Outlined.Person,
                                        contentDescription = "Profile Panel"
                                    )
                                },
                                label = { Text("Profile", fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                            )
                        }
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
                            onEmergencyPassClick = { currentTab = 2 },
                            onDelete = { viewModel.deleteRecord(it) },
                            onToggleSync = { viewModel.updateRecord(it.copy(isSynced = !it.isSynced)) },
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
                            onSave = { updated -> viewModel.updateEmergencyProfile(updated) },
                            onTriggerFullBright = { viewModel.triggerEmergencyMode(true) }
                        )
                        3 -> DoctorSharingGateway(
                            records = records,
                            passcode = doctorPasscode,
                            onGenerateCode = { viewModel.generateDoctorSharingPasscode() },
                            onClearCode = { viewModel.clearDoctorSharingPasscode() }
                        )
                        4 -> ProfileSduiScreen(
                            viewModel = viewModel,
                            emergencyProfile = emergencyProfile ?: EmergencyProfile(),
                            recordsCount = records.size,
                            syncState = syncState
                        )
                    }
                }
            }
        }
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
    onEmergencyPassClick: () -> Unit,
    onDelete: (MedicalRecord) -> Unit,
    onToggleSync: (MedicalRecord) -> Unit,
    onUpdateRecord: (MedicalRecord) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    // Sub-tab: 0 = Timeline Feed, 1 = Disease & Checkups Tracking
    var dashboardTab by remember { mutableStateOf(0) }
    
    // Disease Tracking state fields
    var selectedDeptFilter by remember { mutableStateOf("All Departments") }
    var selectedHospitalFilter by remember { mutableStateOf("All Hospitals") }
    var showDeptDropdown by remember { mutableStateOf(false) }
    var showHospitalDropdown by remember { mutableStateOf(false) }
    
    // Active record being edited
    var editingRecord by remember { mutableStateOf<MedicalRecord?>(null) }

    val categories = listOf("All", "Prescription", "Lab Report", "Vaccine", "Discharge Summary", "Radiology / Scan", "Other")

    // Filter logic for Timeline Feed
    val filteredTimelineRecords = records.filter { record ->
        val matchesCategory = selectedCategory == "All" || record.category.equals(selectedCategory, ignoreCase = true)
        val matchesSearch = record.title.contains(searchQuery, ignoreCase = true) ||
                record.doctorName.contains(searchQuery, ignoreCase = true) ||
                record.clinicOrHospital.contains(searchQuery, ignoreCase = true) ||
                record.diagnoses.contains(searchQuery, ignoreCase = true) ||
                record.summary.contains(searchQuery, ignoreCase = true) ||
                record.department.contains(searchQuery, ignoreCase = true) ||
                record.diseaseOrCheckupType.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    // Filter logic for Disease Tracker
    val filteredTrackerRecords = records.filter { record ->
        val matchesDept = selectedDeptFilter == "All Departments" || record.department.equals(selectedDeptFilter, ignoreCase = true)
        val matchesHospital = selectedHospitalFilter == "All Hospitals" || record.clinicOrHospital.equals(selectedHospitalFilter, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() || 
                record.title.contains(searchQuery, ignoreCase = true) ||
                record.diseaseOrCheckupType.contains(searchQuery, ignoreCase = true) ||
                record.diagnoses.contains(searchQuery, ignoreCase = true) ||
                record.summary.contains(searchQuery, ignoreCase = true)
        matchesDept && matchesHospital && matchesSearch
    }

    // Grouping by Disease / Checkup Type
    val groupedByDisease = remember(filteredTrackerRecords) {
        filteredTrackerRecords.groupBy { it.diseaseOrCheckupType }.toList().sortedByDescending { it.second.size }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            placeholder = { Text("Search title, doctor, condition, center...") },
            leadingIcon = { Icon(Icons.Filled.Search, "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, "Clear search")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Emergency Access Badge Card matching Design HTML exactly!
        EmergencyAccessBadgeCard(
            profile = emergencyProfile,
            onClick = onEmergencyPassClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sub-tabs Segmented Selector (Material 3 TabRow)
        TabRow(
            selectedTabIndex = dashboardTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF006492),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Tab(
                selected = dashboardTab == 0,
                onClick = { dashboardTab = 0 },
                text = { Text("Timeline Feed", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = dashboardTab == 1,
                onClick = { dashboardTab = 1 },
                text = { Text("Condition Tracking", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (dashboardTab == 0) {
            // --- TAB 0: TIMELINE FEED ---
            Text(
                text = "Categories",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF41484D),
                letterSpacing = 1.sp,
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
                        label = { Text(category, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD1E4FF),
                            selectedLabelColor = Color(0xFF001D36),
                            containerColor = Color.White,
                            labelColor = Color(0xFF41484D)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFFDDE2EA),
                            selectedBorderColor = Color(0xFF006492),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        ),
                        shape = RoundedCornerShape(16.dp)
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF41484D),
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Reset Filter",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006492),
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
                            onToggleSync = { onToggleSync(record) },
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
                                .border(1.dp, Color(0xFFDDE2EA), RoundedCornerShape(20.dp))
                                .background(Color.White)
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
                                            .background(Color(0xFFE3F2FD)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = null,
                                            tint = Color(0xFF006492),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = diseaseType,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.Black
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${diseaseRecords.size} Checkups done",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF006492)
                                            )
                                            Text("·", fontSize = 12.sp, color = Color.Gray)
                                            Text(
                                                text = diseaseRecords.firstOrNull()?.department ?: "General",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = if (openTimeline) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "Toggle timeline viewer",
                                    tint = Color(0xFF006492)
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
                                        .background(Color(0xFFF8FAFC))
                                        .padding(horizontal = 16.dp, vertical = 20.dp)
                                ) {
                                    Divider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(bottom = 16.dp))
                                    
                                    Text(
                                        text = "CHRONOLOGICAL HEALTH TIMELINE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF006492),
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
                                                        .background(Color(0xFF006492))
                                                        .border(3.dp, Color.White, CircleShape)
                                                )
                                                if (index < sortedList.size - 1) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.dp)
                                                            .height(110.dp)
                                                            .background(Color(0xFFC2E7FF))
                                                    )
                                                }
                                            }
                                            
                                            // Consultation Details Inside Timeline Card
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                                                colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                                            color = Color(0xFF006492)
                                                        )
                                                        Text(
                                                            text = record.category,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.DarkGray,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFFEEF1F6))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = record.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.Black
                                                    )
                                                    Text(
                                                        text = "Doctor: ${record.doctorName} (${record.clinicOrHospital})",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = record.summary,
                                                        fontSize = 11.sp,
                                                        color = Color.DarkGray,
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
                                                            Icon(Icons.Filled.Edit, null, tint = Color(0xFF006492), modifier = Modifier.size(14.dp))
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        IconButton(
                                                            onClick = { onDelete(record) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Filled.Delete, null, tint = EmergencyRed, modifier = Modifier.size(14.dp))
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

// Single Expanded Medical Record view item
@Composable
fun RecordCardItem(
    record: MedicalRecord,
    onDelete: () -> Unit,
    onToggleSync: () -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val formattedDate = remember(record.date) {
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(record.date))
    }

    val bannerColor = when (record.category) {
        "Prescription" -> Color(0xFFD2E8D4) to Color(0xFF00210B) // Medication
        "Lab Report" -> Color(0xFFD1E4FF) to Color(0xFF001D36) // Lab Results
        "Radiology / Scan" -> Color(0xFFFAD8FD) to Color(0xFF2B1230) // Imaging
        "Discharge Summary", "Vaccine" -> Color(0xFFFFDCC0) to Color(0xFF2F1500) // History
        else -> Color(0xFFEEF1F6) to Color(0xFF41484D) // Other
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFDDE2EA), RoundedCornerShape(24.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = "Synced",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text("Vaulted", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = record.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onToggleSync,
                                colors = ButtonDefaults.textButtonColors(contentColor = bannerColor.second)
                            ) {
                                Icon(
                                    imageVector = if (record.isSynced) Icons.Filled.CloudOff else Icons.Filled.CloudSync,
                                    contentDescription = "Cloud Vault Sync Toggle",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (record.isSynced) "Make Offline" else "Store In Cloud", fontSize = 13.sp)
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onEdit,
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF006492))
                                ) {
                                    Icon(Icons.Filled.Edit, "Edit Medical Record")
                                }
                                IconButton(
                                    onClick = onDelete,
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = EmergencyRed)
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
    onSave: (EmergencyProfile) -> Unit,
    onTriggerFullBright: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    // State bindings
    var name by remember(profile) { mutableStateOf(profile.fullName) }
    var bloodType by remember(profile) { mutableStateOf(profile.bloodType) }
    var dob by remember(profile) { mutableStateOf(profile.dateOfBirth) }
    var allergies by remember(profile) { mutableStateOf(profile.allergies) }
    var conditions by remember(profile) { mutableStateOf(profile.chronicConditions) }
    var meds by remember(profile) { mutableStateOf(profile.currentMedications) }
    var contactsName by remember(profile) { mutableStateOf(profile.emergencyContactName) }
    var contactsPhone by remember(profile) { mutableStateOf(profile.emergencyContactPhone) }
    var insurance by remember(profile) { mutableStateOf(profile.insuranceProvider) }
    var policyId by remember(profile) { mutableStateOf(profile.insuranceNumber) }
    var donor by remember(profile) { mutableStateOf(profile.organDonor) }

    val bloodTypesList = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            // Big Emergency Trigger Widget
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(24.dp)
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
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "EMERGENCY MEDPASS ID",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Press below to open a full-brightness, high-contrast critical health card widget. Intended for emergency responders or doctors to see your record instantly without password unlock.",
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
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.FlashOn, "Full bright")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SHOW ACCESSIBILITY EMERGENCY PASS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditing) "Edit Medical ID profile" else "MedPass Identity Data",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(
                            onClick = {
                                if (isEditing) {
                                    onSave(
                                        profile.copy(
                                            fullName = name,
                                            bloodType = bloodType,
                                            dateOfBirth = dob,
                                            allergies = allergies,
                                            chronicConditions = conditions,
                                            currentMedications = meds,
                                            emergencyContactName = contactsName,
                                            emergencyContactPhone = contactsPhone,
                                            insuranceProvider = insurance,
                                            insuranceNumber = policyId,
                                            organDonor = donor
                                        )
                                    )
                                }
                                isEditing = !isEditing
                            }
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Filled.Beenhere else Icons.Filled.EditCalendar,
                                contentDescription = if (isEditing) "Save profile" else "Edit profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Patient Full Name") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { dob = it },
                            label = { Text("Date of Birth (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        Text("Select Blood Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            items(bloodTypesList) { bt ->
                                FilterChip(
                                    selected = bloodType == bt,
                                    onClick = { bloodType = bt },
                                    label = { Text(bt) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = allergies,
                            onValueChange = { allergies = it },
                            label = { Text("Lethal Allergies (comma separated)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = conditions,
                            onValueChange = { conditions = it },
                            label = { Text("Chronic Conditions (comma separated)") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = meds,
                            onValueChange = { meds = it },
                            label = { Text("Current Care Medications") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = contactsName,
                            onValueChange = { contactsName = it },
                            label = { Text("Emergency Contact Name") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = contactsPhone,
                            onValueChange = { contactsPhone = it },
                            label = { Text("Emergency Contact Phone Number") },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { donor = !donor },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(checked = donor, onCheckedChange = { donor = it })
                            Text("Organ Donor Consent Approved", fontSize = 13.sp)
                        }

                    } else {
                        // Display mode
                        ProfileValueRow(label = "Full Name", value = profile.fullName, leadingIcon = Icons.Filled.Person)
                        ProfileValueRow(label = "Date of Birth", value = profile.dateOfBirth, leadingIcon = Icons.Filled.CalendarMonth)
                        ProfileValueRow(label = "Blood Group", value = profile.bloodType, leadingIcon = Icons.Filled.WaterDrop, highlightColor = EmergencyRed)
                        ProfileValueRow(label = "Lethal Allergies", value = profile.allergies, leadingIcon = Icons.Filled.Warning, highlightColor = EmergencyRed)
                        ProfileValueRow(label = "Chronic Conditions", value = profile.chronicConditions, leadingIcon = Icons.Filled.HeartBroken, highlightColor = UrgentGold)
                        ProfileValueRow(label = "Active Medications", value = profile.currentMedications, leadingIcon = Icons.Filled.MedicalServices)
                        ProfileValueRow(label = "Emergency Contact", value = "${profile.emergencyContactName} (${profile.emergencyContactPhone})", leadingIcon = Icons.Filled.PhoneInTalk)
                        ProfileValueRow(label = "Organ Donor", value = if (profile.organDonor) "Yes, Approved" else "No / Unspecified", leadingIcon = Icons.Filled.VolunteerActivism)
                    }
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


// --- TAB 3: DOCTOR SHARING ACCESS GATEWAY ---

@Composable
fun DoctorSharingGateway(
    records: List<MedicalRecord>,
    passcode: String?,
    onGenerateCode: () -> Unit,
    onClearCode: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var doctorConsoleCode by remember { mutableStateOf("") }
    var isCodeVerifiedForDoctorScreen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // General clinical share info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Secure Dr. Cloud Connector",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Instantly show files on any clinic's workstation screen. Generate a temporary encrypted lookup pass. No wires or paper binders needed.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (passcode == null) {
                        Button(
                            onClick = onGenerateCode,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.VpnKey, "Key")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Temporary Dr. Code")
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .padding(16.dp)
                        ) {
                            Text("TEMPORARY PASSCODE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = passcode,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 4.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Expires in 10 minutes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(passcode))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Copy PIN Link")
                            }

                            TextButton(onClick = onClearCode) {
                                Text("Revoke Credentials", color = EmergencyRed)
                            }
                        }
                    }
                }
            }
        }

        // doctor terminal simulation block - extremely engaging UX detail!
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SIMULATOR: Clinic Workstation Screen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Type the 6-digit active passcode below to simulate what the Doctor would see on their medical workstation screen.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (isCodeVerifiedForDoctorScreen) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Doctor Console Connection Active", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                isCodeVerifiedForDoctorScreen = false
                                doctorConsoleCode = ""
                            }) {
                                Text("Log out Terminal")
                            }
                        }

                        // Clinical View inside Doctor Simulation
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Active Clinical Patient Feed:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                item {
                                    Text("CLINIC TERMINAL SECURE CONNECTION GATEWAY", color = Color.Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text("RESTRICTED TO PRE-VERIFIED HEALTH SYSTEMS", color = Color.Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                items(records) { rec ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("> [${rec.category}] ${rec.title}", color = Color.Cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        Text("  Patient: ${rec.patientName} | Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(rec.date))}", color = Color.LightGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("  Impressions: ${rec.summary}", color = Color.Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text("  Active treatments: ${rec.prescribedMeds}", color = Color.Yellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = doctorConsoleCode,
                            onValueChange = {
                                if (it.length <= 6) doctorConsoleCode = it
                            },
                            label = { Text("Enter Clinical Doctor Passcode") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Button(
                                    onClick = {
                                        if (doctorConsoleCode == passcode && passcode != null) {
                                            isCodeVerifiedForDoctorScreen = true
                                        } else {
                                            isCodeVerifiedForDoctorScreen = false
                                        }
                                    },
                                    enabled = doctorConsoleCode.length == 6
                                ) {
                                    Text("Verify")
                                }
                            }
                        )
                        if (doctorConsoleCode.isNotEmpty() && doctorConsoleCode != passcode) {
                            Text("Wrong PIN code or expired. Generate active code above first.", color = EmergencyRed, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
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
            EmergencyValueCard(label = "BLOOD GROUP", value = profile.bloodType, important = true, isCriticalRedAlert = true)
            EmergencyValueCard(label = "LETHAL ALLERGIES", value = profile.allergies, important = true, isCriticalRedAlert = true)
            EmergencyValueCard(label = "CHRONIC MEDICAL CONDITIONS", value = profile.chronicConditions, important = true)
            EmergencyValueCard(label = "ACTIVE MEDICATION INTAKE", value = profile.currentMedications)
            EmergencyValueCard(label = "PRIMARY EMERGENCY CONTACT", value = "${profile.emergencyContactName} : ${profile.emergencyContactPhone}", important = true)
            EmergencyValueCard(label = "INSURANCE HEALTH PLAN ID", value = "${profile.insuranceProvider} | Plan #${profile.insuranceNumber}")
        }

        // Emergency validation QR Code
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray.copy(alpha = 0.2f))
                .padding(12.dp)
        ) {
            QrCodeCanvas(modifier = Modifier.fillMaxSize())
        }

        Text(
            text = "SCAN FOR CLOUD HEALTH PASS CHECK-IN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

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
    syncState: SyncUiState
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    text = "Dynamic Health Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Layout driven dynamically via SDUI engine",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

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
