package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.WasteClassifierEngine
import com.example.data.barcode.BarcodeAnalysisResult
import com.example.data.barcode.BarcodeDatabase
import com.example.data.barcode.BarcodeScannerHelper
import com.example.data.local.ScanHistoryEntity
import com.example.data.model.ClarificationQuestion
import com.example.data.model.Municipality
import com.example.data.model.SortingResult
import com.example.data.repository.GarbageRepository
import com.example.data.repository.MunicipalityData
import com.example.data.voice.VoiceQueryProcessor
import com.example.data.voice.VoiceQueryResult
import com.example.data.voice.VoiceTtsManager
import com.example.data.update.AppUpdateManager
import com.example.data.update.AppUpdateInfo
import com.example.data.update.AppUpdateCheckResult
import com.example.data.model.AppVersionManager
import java.io.File
import com.example.ui.util.FeedbackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClarificationState(
    val itemName: String,
    val note: String,
    val questions: List<ClarificationQuestion>,
    val answers: Map<String, String> = emptyMap()
)

class GarbageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GarbageRepository(application)

    val currentMunicipality: StateFlow<Municipality> = repository.selectedMunicipalityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MunicipalityData.NAGOYA)

    val historyList: StateFlow<List<ScanHistoryEntity>> = repository.scanHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userApiKey: StateFlow<String> = repository.userApiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Feedback Manager for Shutter & Result (Sound + Vibration)
    private val feedbackManager = FeedbackManager(application)

    val isHapticsEnabled: StateFlow<Boolean> = repository.hapticsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSoundEnabled: StateFlow<Boolean> = repository.soundEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleHaptics() {
        viewModelScope.launch {
            val next = !isHapticsEnabled.value
            repository.saveHapticsEnabled(next)
            _statusNotification.value = if (next) "振動フィードバックをONにしました" else "振動フィードバックをOFFにしました"
        }
    }

    fun toggleSound() {
        viewModelScope.launch {
            val next = !isSoundEnabled.value
            repository.saveSoundEnabled(next)
            _statusNotification.value = if (next) "シャッター音をONにしました（ボヨ〜ン♪）" else "シャッター音をOFFにしました"
            if (next) {
                feedbackManager.playShutterFeedback(hapticsEnabled = false, soundEnabled = true)
            }
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveHapticsEnabled(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSoundEnabled(enabled)
        }
    }

    fun triggerShutterFeedback() {
        feedbackManager.playShutterFeedback(isHapticsEnabled.value, isSoundEnabled.value)
    }

    // Target Item Specification (For multiple objects in frame)
    private val _targetItemHint = MutableStateFlow("")
    val targetItemHint: StateFlow<String> = _targetItemHint.asStateFlow()

    fun setTargetItemHint(hint: String) {
        _targetItemHint.value = hint
    }

    private val _isApiKeyDialogOpen = MutableStateFlow(false)
    val isApiKeyDialogOpen: StateFlow<Boolean> = _isApiKeyDialogOpen.asStateFlow()

    private val _isVersionDialogOpen = MutableStateFlow(false)
    val isVersionDialogOpen: StateFlow<Boolean> = _isVersionDialogOpen.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    private val _activeResult = MutableStateFlow<SortingResult?>(null)
    val activeResult: StateFlow<SortingResult?> = _activeResult.asStateFlow()

    private val _pendingClarification = MutableStateFlow<ClarificationState?>(null)
    val pendingClarification: StateFlow<ClarificationState?> = _pendingClarification.asStateFlow()

    private val _isMunicipalityPickerOpen = MutableStateFlow(false)
    val isMunicipalityPickerOpen: StateFlow<Boolean> = _isMunicipalityPickerOpen.asStateFlow()

    private val _statusNotification = MutableStateFlow<String?>(null)
    val statusNotification: StateFlow<String?> = _statusNotification.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: 分別判定, 1: 収集カレンダー, 2: 分別履歴
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Barcode Scanning State
    private val _isBarcodeScannerOpen = MutableStateFlow(false)
    val isBarcodeScannerOpen: StateFlow<Boolean> = _isBarcodeScannerOpen.asStateFlow()

    private val _isBarcodeScanning = MutableStateFlow(false)
    val isBarcodeScanning: StateFlow<Boolean> = _isBarcodeScanning.asStateFlow()

    private val _barcodeScanError = MutableStateFlow<String?>(null)
    val barcodeScanError: StateFlow<String?> = _barcodeScanError.asStateFlow()

    private val _activeBarcodeResult = MutableStateFlow<BarcodeAnalysisResult?>(null)
    val activeBarcodeResult: StateFlow<BarcodeAnalysisResult?> = _activeBarcodeResult.asStateFlow()

    // Voice Assistant & TTS State
    private val ttsManager = VoiceTtsManager(application)
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    private val _isVoiceAssistantOpen = MutableStateFlow(false)
    val isVoiceAssistantOpen: StateFlow<Boolean> = _isVoiceAssistantOpen.asStateFlow()

    private val _voiceQueryResult = MutableStateFlow<VoiceQueryResult?>(null)
    val voiceQueryResult: StateFlow<VoiceQueryResult?> = _voiceQueryResult.asStateFlow()

    // Plan 1: Custom Municipalities & AI Generation
    val allAvailableMunicipalities: StateFlow<List<Municipality>> = repository.allAvailableMunicipalitiesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MunicipalityData.ALL_MUNICIPALITIES)

    val customMunicipalities: StateFlow<List<Municipality>> = repository.customMunicipalitiesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGeneratingMunicipality = MutableStateFlow(false)
    val isGeneratingMunicipality: StateFlow<Boolean> = _isGeneratingMunicipality.asStateFlow()

    private val _editingMunicipality = MutableStateFlow<Municipality?>(null)
    val editingMunicipality: StateFlow<Municipality?> = _editingMunicipality.asStateFlow()

    // Plan 2: Recycle Spots
    private val _recycleCategoryFilter = MutableStateFlow<com.example.data.model.DropoffCategory?>(null)
    val recycleCategoryFilter: StateFlow<com.example.data.model.DropoffCategory?> = _recycleCategoryFilter.asStateFlow()

    private val _recycleSearchQuery = MutableStateFlow("")
    val recycleSearchQuery: StateFlow<String> = _recycleSearchQuery.asStateFlow()

    val recycleSpots: StateFlow<List<com.example.data.model.RecycleSpot>> = kotlinx.coroutines.flow.combine(
        currentMunicipality,
        _recycleCategoryFilter,
        _recycleSearchQuery
    ) { municipality, category, query ->
        repository.getRecycleSpots(
            municipalityName = municipality.name,
            category = category,
            searchQuery = query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // App Auto-Update (GitHub Releases)
    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateCheckResult = MutableStateFlow<AppUpdateCheckResult?>(null)
    val updateCheckResult: StateFlow<AppUpdateCheckResult?> = _updateCheckResult.asStateFlow()

    private val _availableUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdateInfo: StateFlow<AppUpdateInfo?> = _availableUpdateInfo.asStateFlow()

    private val _isUpdateDialogOpen = MutableStateFlow(false)
    val isUpdateDialogOpen: StateFlow<Boolean> = _isUpdateDialogOpen.asStateFlow()

    private val _isDownloadingApk = MutableStateFlow(false)
    val isDownloadingApk: StateFlow<Boolean> = _isDownloadingApk.asStateFlow()

    private val _downloadProgress = MutableStateFlow(-1) // 0..100, -1: idle, -2: downloaded
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _downloadedApkFile = MutableStateFlow<File?>(null)
    val downloadedApkFile: StateFlow<File?> = _downloadedApkFile.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    init {
        // Automatically check for app updates in the background on startup
        checkForAppUpdate(isManual = false)
    }

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openMunicipalityPicker() {
        _isMunicipalityPickerOpen.value = true
    }

    fun closeMunicipalityPicker() {
        _isMunicipalityPickerOpen.value = false
    }

    fun selectMunicipality(municipalityId: String) {
        viewModelScope.launch {
            repository.saveSelectedMunicipality(municipalityId)
            _isMunicipalityPickerOpen.value = false
            val m = MunicipalityData.findById(municipalityId)
            _statusNotification.value = "分別基準を「${m.name}」に変更しました"
        }
    }

    fun detectGpsLocation() {
        viewModelScope.launch {
            _statusNotification.value = "GPS現在地を取得中..."
            val detected = repository.autoDetectMunicipalityWithGps()
            if (detected != null) {
                _statusNotification.value = "現在地から「${detected.name}」を自動認識しました"
            } else {
                _statusNotification.value = "GPSから市町村を取得できませんでした。リストから選択してください。"
                _isMunicipalityPickerOpen.value = true
            }
        }
    }

    fun clearStatusNotification() {
        _statusNotification.value = null
    }

    fun openApiKeyDialog() {
        _isApiKeyDialogOpen.value = true
    }

    fun closeApiKeyDialog() {
        _isApiKeyDialogOpen.value = false
    }

    fun saveApiKey(newKey: String) {
        viewModelScope.launch {
            repository.saveUserApiKey(newKey.trim())
            _isApiKeyDialogOpen.value = false
            if (newKey.isBlank()) {
                _statusNotification.value = "Gemini APIキーを解除しました（自治体辞書・ルールベースで動作します）"
            } else {
                _statusNotification.value = "Gemini APIキーを保存しました（AI画像分析が有効化されました）"
            }
        }
    }

    fun openVersionDialog() {
        _isVersionDialogOpen.value = true
    }

    fun closeVersionDialog() {
        _isVersionDialogOpen.value = false
    }

    /**
     * Process an image taken from camera or gallery
     */
    fun analyzeImage(bitmap: Bitmap, targetHint: String? = null) {
        // Immediate feedback upon shooting / capture
        triggerShutterFeedback()

        _currentBitmap.value = bitmap
        val effectiveHint = targetHint?.trim()?.ifBlank { null } ?: _targetItemHint.value.trim().ifBlank { null }
        if (effectiveHint != null) {
            _targetItemHint.value = effectiveHint
        }
        _isAnalyzing.value = true
        _activeResult.value = null
        _pendingClarification.value = null

        viewModelScope.launch {
            val municipality = currentMunicipality.value
            val output = repository.analyzeWaste(
                bitmap = bitmap,
                keyword = null,
                municipality = municipality,
                targetItemHint = effectiveHint
            )
            _isAnalyzing.value = false

            when (output) {
                is WasteClassifierEngine.AnalysisOutput.Resolved -> {
                    _activeResult.value = output.result
                    repository.saveToHistory(output.result)
                    feedbackManager.playResultFeedback(isHapticsEnabled.value, isSoundEnabled.value)
                }
                is WasteClassifierEngine.AnalysisOutput.NeedsClarification -> {
                    _pendingClarification.value = ClarificationState(
                        itemName = output.itemName,
                        note = output.initialNote,
                        questions = output.questions,
                        answers = emptyMap()
                    )
                    feedbackManager.playResultFeedback(isHapticsEnabled.value, isSoundEnabled.value)
                }
                is WasteClassifierEngine.AnalysisOutput.Error -> {
                    _statusNotification.value = "判定に失敗しました: ${output.message}"
                }
            }
        }
    }

    /**
     * Process text search or quick category item
     */
    fun analyzeKeyword(keyword: String) {
        _isAnalyzing.value = true
        _activeResult.value = null
        _pendingClarification.value = null

        viewModelScope.launch {
            val municipality = currentMunicipality.value
            val output = repository.analyzeWaste(null, keyword, municipality)
            _isAnalyzing.value = false

            when (output) {
                is WasteClassifierEngine.AnalysisOutput.Resolved -> {
                    _activeResult.value = output.result
                    repository.saveToHistory(output.result)
                    feedbackManager.playResultFeedback(isHapticsEnabled.value, isSoundEnabled.value)
                }
                is WasteClassifierEngine.AnalysisOutput.NeedsClarification -> {
                    _pendingClarification.value = ClarificationState(
                        itemName = output.itemName,
                        note = output.initialNote,
                        questions = output.questions,
                        answers = emptyMap()
                    )
                    feedbackManager.playResultFeedback(isHapticsEnabled.value, isSoundEnabled.value)
                }
                is WasteClassifierEngine.AnalysisOutput.Error -> {
                    _statusNotification.value = output.message
                }
            }
        }
    }

    /**
     * Correct misclassified item name and re-analyze with AI & dictionary
     * e.g. User corrects "メガネケース" -> "革製の長財布"
     */
    fun correctItemNameAndReanalyze(correctedName: String) {
        val trimmed = correctedName.trim()
        if (trimmed.isBlank()) return

        _isAnalyzing.value = true
        _pendingClarification.value = null
        _targetItemHint.value = trimmed

        viewModelScope.launch {
            val municipality = currentMunicipality.value
            val output = repository.analyzeWaste(
                bitmap = _currentBitmap.value,
                keyword = trimmed,
                municipality = municipality,
                targetItemHint = trimmed
            )
            _isAnalyzing.value = false

            when (output) {
                is WasteClassifierEngine.AnalysisOutput.Resolved -> {
                    val finalResult = output.result.copy(itemName = trimmed)
                    _activeResult.value = finalResult
                    repository.saveToHistory(finalResult)
                    feedbackManager.playResultFeedback(isHapticsEnabled.value, isSoundEnabled.value)
                    _statusNotification.value = "品名を「$trimmed」に訂正し、分別ルールを更新しました"
                }
                is WasteClassifierEngine.AnalysisOutput.NeedsClarification -> {
                    _pendingClarification.value = ClarificationState(
                        itemName = trimmed,
                        note = output.initialNote,
                        questions = output.questions,
                        answers = emptyMap()
                    )
                    feedbackManager.playResultFeedback(isHapticsEnabled.value, isSoundEnabled.value)
                }
                is WasteClassifierEngine.AnalysisOutput.Error -> {
                    _statusNotification.value = "再判定に失敗しました: ${output.message}"
                }
            }
        }
    }

    /**
     * Directly set category and item name manually
     */
    fun manuallySetCategory(itemName: String, categoryId: String, customAdvice: String? = null) {
        val trimmed = itemName.trim().ifBlank { _activeResult.value?.itemName ?: "指定品目" }
        val municipality = currentMunicipality.value
        val newResult = repository.createCustomResult(trimmed, categoryId, municipality, customAdvice)
        _activeResult.value = newResult
        viewModelScope.launch {
            repository.saveToHistory(newResult)
            feedbackManager.playResultFeedback(isHapticsEnabled.value, isSoundEnabled.value)
            _statusNotification.value = "「$trimmed」を「${newResult.categoryName}」として保存しました"
        }
    }

    /**
     * Update clarification answer
     */
    fun setClarificationAnswer(questionId: String, optionId: String) {
        val current = _pendingClarification.value ?: return
        val newAnswers = current.answers.toMutableMap().apply {
            put(questionId, optionId)
        }
        _pendingClarification.value = current.copy(answers = newAnswers)
    }

    /**
     * Submit answered clarification questions to get final verdict
     */
    fun submitClarificationAnswers() {
        val state = _pendingClarification.value ?: return
        val municipality = currentMunicipality.value
        val result = repository.resolveQuestions(state.itemName, municipality, state.answers)
        _pendingClarification.value = null
        _activeResult.value = result

        viewModelScope.launch {
            repository.saveToHistory(result)
        }
    }

    fun dismissClarification() {
        _pendingClarification.value = null
    }

    fun dismissResult() {
        _activeResult.value = null
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _statusNotification.value = "検索履歴をクリアしました"
        }
    }

    // ==========================================
    // Barcode Scanning Controls
    // ==========================================
    fun openBarcodeScanner() {
        _barcodeScanError.value = null
        _isBarcodeScannerOpen.value = true
    }

    fun closeBarcodeScanner() {
        _barcodeScanError.value = null
        _isBarcodeScannerOpen.value = false
        _isBarcodeScanning.value = false
    }

    fun clearBarcodeScanError() {
        _barcodeScanError.value = null
    }

    fun dismissBarcodeResult() {
        _activeBarcodeResult.value = null
    }

    fun scanBarcodeBitmap(bitmap: Bitmap) {
        _isBarcodeScanning.value = true
        _barcodeScanError.value = null
        viewModelScope.launch {
            val barcode = BarcodeScannerHelper.decodeBarcodeFromBitmap(bitmap)
            _isBarcodeScanning.value = false
            if (barcode != null) {
                scanBarcodeValue(barcode)
            } else {
                _barcodeScanError.value = "バーコードを認識できませんでした。\n白黒の縞模様にピントを合わせ、正面から水平に大きく撮影してください。"
                _statusNotification.value = "バーコードを認識できませんでした。枠内に合わせてピントをご確認ください。"
            }
        }
    }

    fun scanBarcodeValue(barcode: String) {
        _barcodeScanError.value = null
        _isBarcodeScanning.value = false
        val product = BarcodeDatabase.findByBarcode(barcode)
        val municipality = currentMunicipality.value

        if (product != null) {
            _isBarcodeScannerOpen.value = false
            val resolved = BarcodeScannerHelper.resolveProductForMunicipality(product, municipality)
            _activeBarcodeResult.value = resolved
            viewModelScope.launch {
                repository.saveToHistory(resolved.primarySortingResult)
            }
            feedbackManager.playResultFeedback(isHapticsEnabled.value, isSoundEnabled.value)
            _statusNotification.value = "バーコードから「${product.productName}」を特定しました"
        } else {
            _barcodeScanError.value = "バーコード（$barcode）の商品情報が見つかりませんでした。"
            _statusNotification.value = "バーコード($barcode)の商品情報が見つかりませんでした。"
        }
    }

    // ==========================================
    // Voice Assistant & Text-to-Speech Controls
    // ==========================================
    fun openVoiceAssistant() {
        _isVoiceAssistantOpen.value = true
    }

    fun closeVoiceAssistant() {
        _isVoiceAssistantOpen.value = false
        stopSpeaking()
    }

    fun dismissVoiceResult() {
        _voiceQueryResult.value = null
        stopSpeaking()
    }

    fun processVoiceQuery(query: String, autoSpeak: Boolean = true) {
        if (query.isBlank()) return
        _isAnalyzing.value = true

        viewModelScope.launch {
            val municipality = currentMunicipality.value
            val result = VoiceQueryProcessor.processQuery(query, municipality, repository.engine)
            _isAnalyzing.value = false
            _voiceQueryResult.value = result

            if (result.sortingResult != null) {
                repository.saveToHistory(result.sortingResult)
            }

            if (autoSpeak && result.spokenResponse.isNotBlank()) {
                ttsManager.speak(result.spokenResponse)
            }
        }
    }

    fun speakText(text: String) {
        ttsManager.speak(text)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    // ==========================================
    // Custom Municipality & AI Generation
    // ==========================================
    fun generateMunicipalityWithAi(query: String) {
        if (query.isBlank()) return
        _isGeneratingMunicipality.value = true
        viewModelScope.launch {
            try {
                val result = repository.generateMunicipalityWithAi(query)
                _isGeneratingMunicipality.value = false
                _isMunicipalityPickerOpen.value = false
                _statusNotification.value = result.summaryMessage
            } catch (e: Exception) {
                _isGeneratingMunicipality.value = false
                _statusNotification.value = "自治体ルールの生成中にエラーが発生しました: ${e.message}"
            }
        }
    }

    fun openMunicipalityEditor(municipality: Municipality) {
        _editingMunicipality.value = municipality
    }

    fun closeMunicipalityEditor() {
        _editingMunicipality.value = null
    }

    fun saveCustomMunicipality(municipality: Municipality) {
        viewModelScope.launch {
            repository.saveCustomMunicipality(municipality)
            repository.saveSelectedMunicipality(municipality.id)
            _editingMunicipality.value = null
            _statusNotification.value = "「${municipality.name}」の分別ルールとカレンダーを更新しました"
        }
    }

    fun deleteCustomMunicipality(id: String) {
        viewModelScope.launch {
            repository.deleteCustomMunicipality(id)
            _statusNotification.value = "自治体設定を削除しました"
        }
    }

    // ==========================================
    // Recycle Drop-Off Spots (Plan 2)
    // ==========================================
    fun setRecycleCategoryFilter(category: com.example.data.model.DropoffCategory?) {
        _recycleCategoryFilter.value = category
    }

    fun setRecycleSearchQuery(query: String) {
        _recycleSearchQuery.value = query
    }

    fun navigateToRecycleMap(category: com.example.data.model.DropoffCategory? = null) {
        _recycleCategoryFilter.value = category
        _selectedTab.value = 3
    }

    // ==========================================
    // GitHub Releases Auto-Update Actions
    // ==========================================
    fun checkForAppUpdate(isManual: Boolean = false) {
        if (_isCheckingUpdate.value || _isDownloadingApk.value) return

        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _downloadError.value = null

            val result = AppUpdateManager.checkForUpdate(AppVersionManager.CURRENT_VERSION_NAME)
            _updateCheckResult.value = result
            _isCheckingUpdate.value = false

            when (result) {
                is AppUpdateCheckResult.UpdateAvailable -> {
                    _availableUpdateInfo.value = result.info
                    _downloadProgress.value = -1
                    _isUpdateDialogOpen.value = true
                }
                is AppUpdateCheckResult.UpToDate -> {
                    if (isManual) {
                        _statusNotification.value = "お使いのアプリは最新バージョン (v${result.latestVersion}) です"
                    }
                }
                is AppUpdateCheckResult.NoReleaseFound -> {
                    if (isManual) {
                        _statusNotification.value = "GitHubにまだReleaseが公開されていません"
                    }
                }
                is AppUpdateCheckResult.Error -> {
                    if (isManual) {
                        _statusNotification.value = "更新確認に失敗しました: ${result.message}"
                    }
                }
            }
        }
    }

    fun startApkDownload(updateInfo: AppUpdateInfo) {
        val downloadUrl = updateInfo.apkDownloadUrl ?: return
        if (_isDownloadingApk.value) return

        viewModelScope.launch {
            _isDownloadingApk.value = true
            _downloadProgress.value = 0
            _downloadError.value = null

            val result = AppUpdateManager.downloadApk(
                context = getApplication(),
                downloadUrl = downloadUrl,
                fileName = updateInfo.apkFileName ?: "update.apk",
                onProgress = { percent, _, _ ->
                    _downloadProgress.value = percent
                }
            )

            _isDownloadingApk.value = false

            result.fold(
                onSuccess = { file ->
                    _downloadedApkFile.value = file
                    _downloadProgress.value = -2 // downloaded
                    installDownloadedApk()
                },
                onFailure = { error ->
                    _downloadError.value = "ダウンロードに失敗しました: ${error.localizedMessage}"
                    _downloadProgress.value = -1
                }
            )
        }
    }

    fun installDownloadedApk(): Boolean {
        val file = _downloadedApkFile.value ?: return false
        return AppUpdateManager.launchInstaller(getApplication(), file)
    }

    fun openInstallPermissionSettings() {
        AppUpdateManager.openInstallPermissionSettings(getApplication())
    }

    fun canInstallPackages(): Boolean {
        return AppUpdateManager.canInstallPackages(getApplication())
    }

    fun dismissUpdateDialog() {
        if (!_isDownloadingApk.value) {
            _isUpdateDialogOpen.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
        feedbackManager.release()
    }
}

