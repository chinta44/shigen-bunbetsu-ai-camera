package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Recycling
import com.example.data.model.AppVersionManager
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.GarbageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GarbageViewModel) {
    val currentMunicipality by viewModel.currentMunicipality.collectAsStateWithLifecycle()
    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val currentBitmap by viewModel.currentBitmap.collectAsStateWithLifecycle()
    val activeResult by viewModel.activeResult.collectAsStateWithLifecycle()
    val pendingClarification by viewModel.pendingClarification.collectAsStateWithLifecycle()
    val isMunicipalityPickerOpen by viewModel.isMunicipalityPickerOpen.collectAsStateWithLifecycle()
    val statusNotification by viewModel.statusNotification.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    val isBarcodeScannerOpen by viewModel.isBarcodeScannerOpen.collectAsStateWithLifecycle()
    val isBarcodeScanning by viewModel.isBarcodeScanning.collectAsStateWithLifecycle()
    val barcodeScanError by viewModel.barcodeScanError.collectAsStateWithLifecycle()
    val activeBarcodeResult by viewModel.activeBarcodeResult.collectAsStateWithLifecycle()

    val isHapticsEnabled by viewModel.isHapticsEnabled.collectAsStateWithLifecycle()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsStateWithLifecycle()
    val targetItemHint by viewModel.targetItemHint.collectAsStateWithLifecycle()

    val isVoiceAssistantOpen by viewModel.isVoiceAssistantOpen.collectAsStateWithLifecycle()
    val voiceQueryResult by viewModel.voiceQueryResult.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()

    val userApiKey by viewModel.userApiKey.collectAsStateWithLifecycle()
    val isApiKeyDialogOpen by viewModel.isApiKeyDialogOpen.collectAsStateWithLifecycle()
    val isVersionDialogOpen by viewModel.isVersionDialogOpen.collectAsStateWithLifecycle()

    val isUpdateDialogOpen by viewModel.isUpdateDialogOpen.collectAsStateWithLifecycle()
    val availableUpdateInfo by viewModel.availableUpdateInfo.collectAsStateWithLifecycle()
    val isDownloadingApk by viewModel.isDownloadingApk.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadError by viewModel.downloadError.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()

    val allMunicipalities by viewModel.allAvailableMunicipalities.collectAsStateWithLifecycle()
    val customMunicipalities by viewModel.customMunicipalities.collectAsStateWithLifecycle()
    val isGeneratingMunicipality by viewModel.isGeneratingMunicipality.collectAsStateWithLifecycle()
    val editingMunicipality by viewModel.editingMunicipality.collectAsStateWithLifecycle()

    val recycleSpots by viewModel.recycleSpots.collectAsStateWithLifecycle()
    val recycleCategoryFilter by viewModel.recycleCategoryFilter.collectAsStateWithLifecycle()
    val recycleSearchQuery by viewModel.recycleSearchQuery.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // GPS Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectGpsLocation()
        } else {
            viewModel.openMunicipalityPicker()
        }
    }

    LaunchedEffect(statusNotification) {
        statusNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusNotification()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Recycling,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ゴミ分別",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            onClick = { viewModel.openVersionDialog() },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("top_version_badge")
                        ) {
                            Text(
                                text = "v${AppVersionManager.CURRENT_VERSION_NAME}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // API Key Settings Button
                    IconButton(
                        onClick = { viewModel.openApiKeyDialog() },
                        modifier = Modifier.testTag("top_api_key_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "Gemini APIキー設定",
                            tint = if (userApiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    Surface(
                        onClick = { viewModel.openMunicipalityPicker() },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("top_municipality_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentMunicipality.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.testTag("top_gps_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "GPS現在地判定",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "分別スキャン") },
                    label = { Text("分別判定") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_scan")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "収集日") },
                    label = { Text("収集日程") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_calendar")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = { Icon(Icons.Default.History, contentDescription = "履歴") },
                    label = { Text("分別履歴") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_history")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = { Icon(Icons.Default.Place, contentDescription = "拠点マップ") },
                    label = { Text("拠点マップ") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_recycle_map")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ScanScreen(
                    municipality = currentMunicipality,
                    isAnalyzing = isAnalyzing,
                    currentBitmap = currentBitmap,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                    onSearchSubmit = { viewModel.analyzeKeyword(it) },
                    onImageCaptured = { bmp, hint -> viewModel.analyzeImage(bmp, hint ?: targetItemHint) },
                    onOpenBarcodeScanner = { viewModel.openBarcodeScanner() },
                    onOpenVoiceAssistant = { viewModel.openVoiceAssistant() },
                    onChangeMunicipality = { viewModel.openMunicipalityPicker() },
                    onGpsClicked = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onViewCalendar = { viewModel.setSelectedTab(1) },
                    userApiKey = userApiKey,
                    onOpenApiKeySettings = { viewModel.openApiKeyDialog() },
                    onOpenVersionInfo = { viewModel.openVersionDialog() },
                    isHapticsEnabled = isHapticsEnabled,
                    isSoundEnabled = isSoundEnabled,
                    onToggleHaptics = { viewModel.toggleHaptics() },
                    onToggleSound = { viewModel.toggleSound() },
                    targetItemHint = targetItemHint,
                    onTargetItemHintChanged = { viewModel.setTargetItemHint(it) },
                    onClearImage = { viewModel.clearCurrentBitmap() }
                )
                1 -> CalendarScreen(
                    municipality = currentMunicipality,
                    onChangeMunicipality = { viewModel.openMunicipalityPicker() },
                    onEditSchedule = { viewModel.openMunicipalityEditor(currentMunicipality) }
                )
                2 -> HistoryScreen(
                    historyList = historyList,
                    onDeleteItem = { viewModel.deleteHistoryItem(it) },
                    onClearAll = { viewModel.clearAllHistory() },
                    onStartScan = { viewModel.setSelectedTab(0) }
                )
                3 -> RecycleMapScreen(
                    currentMunicipality = currentMunicipality,
                    spots = recycleSpots,
                    selectedCategory = recycleCategoryFilter,
                    onCategorySelected = { viewModel.setRecycleCategoryFilter(it) },
                    searchQuery = recycleSearchQuery,
                    onSearchQueryChanged = { viewModel.setRecycleSearchQuery(it) }
                )
            }
        }
    }

    // Modal Clarification Dialog (迷う物は追加質問)
    pendingClarification?.let { state ->
        ClarificationDialog(
            state = state,
            municipalityName = currentMunicipality.name,
            onAnswerSelected = { qId, optId ->
                viewModel.setClarificationAnswer(qId, optId)
            },
            onSubmit = { viewModel.submitClarificationAnswers() },
            onDismiss = { viewModel.dismissClarification() }
        )
    }

    // Modal Sorting Result Sheet (次回の収集日表示・ルール)
    activeResult?.let { result ->
        SortingResultSheet(
            result = result,
            municipality = currentMunicipality,
            onDismiss = { viewModel.dismissResult() },
            onViewCalendar = {
                viewModel.dismissResult()
                viewModel.setSelectedTab(1)
            },
            onViewRecycleMap = { cat ->
                viewModel.dismissResult()
                viewModel.navigateToRecycleMap(cat)
            },
            onCorrectItemName = { correctedName ->
                viewModel.correctItemNameAndReanalyze(correctedName)
            },
            onManuallySetCategory = { itemName, categoryId ->
                viewModel.manuallySetCategory(itemName, categoryId)
            }
        )
    }

    // Municipality Picker Bottom Sheet
    if (isMunicipalityPickerOpen) {
        MunicipalityPickerSheet(
            currentMunicipality = currentMunicipality,
            allMunicipalities = allMunicipalities,
            customMunicipalities = customMunicipalities,
            isGenerating = isGeneratingMunicipality,
            onMunicipalitySelected = { municipalityId ->
                viewModel.selectMunicipality(municipalityId)
            },
            onGenerateWithAi = { query ->
                viewModel.generateMunicipalityWithAi(query)
            },
            onEditMunicipality = { m ->
                viewModel.openMunicipalityEditor(m)
            },
            onDeleteCustomMunicipality = { id ->
                viewModel.deleteCustomMunicipality(id)
            },
            onGpsClicked = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDismiss = { viewModel.closeMunicipalityPicker() }
        )
    }

    // Municipality Edit / Fine-tuning Dialog
    editingMunicipality?.let { m ->
        MunicipalityEditDialog(
            municipality = m,
            onSave = { updated ->
                viewModel.saveCustomMunicipality(updated)
            },
            onDismiss = { viewModel.closeMunicipalityEditor() }
        )
    }

    // Barcode Scanner Dialog
    BarcodeScanDialog(
        isOpen = isBarcodeScannerOpen,
        onDismiss = { viewModel.closeBarcodeScanner() },
        isScanning = isBarcodeScanning,
        errorMessage = barcodeScanError,
        onClearError = { viewModel.clearBarcodeScanError() },
        onBarcodeScanned = { viewModel.scanBarcodeValue(it) },
        onBarcodeBitmapCaptured = { viewModel.scanBarcodeBitmap(it) }
    )

    // Barcode Analysis Result Sheet
    activeBarcodeResult?.let { result ->
        BarcodeResultSheet(
            result = result,
            municipality = currentMunicipality,
            onDismiss = { viewModel.dismissBarcodeResult() },
            onViewCalendar = {
                viewModel.dismissBarcodeResult()
                viewModel.setSelectedTab(1)
            },
            onSpeak = { viewModel.speakText(it) }
        )
    }

    // Voice Assistant Dialog & Audio Responder
    VoiceAssistantDialog(
        isOpen = isVoiceAssistantOpen,
        onDismiss = { viewModel.closeVoiceAssistant() },
        voiceResult = voiceQueryResult,
        isSpeaking = isSpeaking,
        isAnalyzing = isAnalyzing,
        onQuerySubmitted = { viewModel.processVoiceQuery(it, autoSpeak = true) },
        onSpeakText = { viewModel.speakText(it) },
        onStopSpeaking = { viewModel.stopSpeaking() },
        onViewCalendar = {
            viewModel.closeVoiceAssistant()
            viewModel.setSelectedTab(1)
        }
    )

    // User Gemini API Key Settings Dialog
    if (isApiKeyDialogOpen) {
        ApiKeySettingsDialog(
            currentApiKey = userApiKey,
            onSaveApiKey = { viewModel.saveApiKey(it) },
            onDismiss = { viewModel.closeApiKeyDialog() }
        )
    }

    // App Version & Changelog Dialog
    if (isVersionDialogOpen) {
        AppVersionDialog(
            onDismiss = { viewModel.closeVersionDialog() },
            onCheckForUpdate = { viewModel.checkForAppUpdate(isManual = true) },
            isCheckingUpdate = isCheckingUpdate
        )
    }

    // GitHub Releases In-App Auto-Updater Dialog
    if (isUpdateDialogOpen && availableUpdateInfo != null) {
        AppUpdateDialog(
            updateInfo = availableUpdateInfo!!,
            isDownloading = isDownloadingApk,
            downloadProgress = downloadProgress,
            downloadError = downloadError,
            onStartDownload = { viewModel.startApkDownload(availableUpdateInfo!!) },
            onInstallNow = { viewModel.installDownloadedApk() },
            onDismiss = { viewModel.dismissUpdateDialog() },
            canInstallPackages = viewModel.canInstallPackages(),
            onOpenInstallSettings = { viewModel.openInstallPermissionSettings() }
        )
    }
}

