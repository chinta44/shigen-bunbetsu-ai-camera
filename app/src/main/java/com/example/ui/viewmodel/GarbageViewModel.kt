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

    private val _activeBarcodeResult = MutableStateFlow<BarcodeAnalysisResult?>(null)
    val activeBarcodeResult: StateFlow<BarcodeAnalysisResult?> = _activeBarcodeResult.asStateFlow()

    // Voice Assistant & TTS State
    private val ttsManager = VoiceTtsManager(application)
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    private val _isVoiceAssistantOpen = MutableStateFlow(false)
    val isVoiceAssistantOpen: StateFlow<Boolean> = _isVoiceAssistantOpen.asStateFlow()

    private val _voiceQueryResult = MutableStateFlow<VoiceQueryResult?>(null)
    val voiceQueryResult: StateFlow<VoiceQueryResult?> = _voiceQueryResult.asStateFlow()

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
    fun analyzeImage(bitmap: Bitmap) {
        _currentBitmap.value = bitmap
        _isAnalyzing.value = true
        _activeResult.value = null
        _pendingClarification.value = null

        viewModelScope.launch {
            val municipality = currentMunicipality.value
            val output = repository.analyzeWaste(bitmap, null, municipality)
            _isAnalyzing.value = false

            when (output) {
                is WasteClassifierEngine.AnalysisOutput.Resolved -> {
                    _activeResult.value = output.result
                    repository.saveToHistory(output.result)
                }
                is WasteClassifierEngine.AnalysisOutput.NeedsClarification -> {
                    _pendingClarification.value = ClarificationState(
                        itemName = output.itemName,
                        note = output.initialNote,
                        questions = output.questions,
                        answers = emptyMap()
                    )
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
                }
                is WasteClassifierEngine.AnalysisOutput.NeedsClarification -> {
                    _pendingClarification.value = ClarificationState(
                        itemName = output.itemName,
                        note = output.initialNote,
                        questions = output.questions,
                        answers = emptyMap()
                    )
                }
                is WasteClassifierEngine.AnalysisOutput.Error -> {
                    _statusNotification.value = output.message
                }
            }
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
        _isBarcodeScannerOpen.value = true
    }

    fun closeBarcodeScanner() {
        _isBarcodeScannerOpen.value = false
        _isBarcodeScanning.value = false
    }

    fun dismissBarcodeResult() {
        _activeBarcodeResult.value = null
    }

    fun scanBarcodeBitmap(bitmap: Bitmap) {
        _isBarcodeScanning.value = true
        viewModelScope.launch {
            val barcode = BarcodeScannerHelper.decodeBarcodeFromBitmap(bitmap)
            _isBarcodeScanning.value = false
            if (barcode != null) {
                scanBarcodeValue(barcode)
            } else {
                _statusNotification.value = "バーコードを認識できませんでした。枠内に合わせてピントを確認してください。"
            }
        }
    }

    fun scanBarcodeValue(barcode: String) {
        _isBarcodeScannerOpen.value = false
        _isBarcodeScanning.value = false
        val product = BarcodeDatabase.findByBarcode(barcode)
        val municipality = currentMunicipality.value

        if (product != null) {
            val resolved = BarcodeScannerHelper.resolveProductForMunicipality(product, municipality)
            _activeBarcodeResult.value = resolved
            viewModelScope.launch {
                repository.saveToHistory(resolved.primarySortingResult)
            }
            _statusNotification.value = "バーコードから「${product.productName}」を特定しました"
        } else {
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

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}

