package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.ai.WasteClassifierEngine
import com.example.data.local.AppConfigEntity
import com.example.data.local.AppDatabase
import com.example.data.local.ScanHistoryEntity
import com.example.data.location.LocationHelper
import com.example.data.model.Municipality
import com.example.data.model.SortingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class GarbageRepository(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context),
    private val classifierEngine: WasteClassifierEngine = WasteClassifierEngine(),
    private val locationHelper: LocationHelper = LocationHelper(context)
) {
    private val historyDao = db.scanHistoryDao()
    private val configDao = db.appConfigDao()

    val scanHistoryFlow: Flow<List<ScanHistoryEntity>> = historyDao.getAllHistory()
    val engine: WasteClassifierEngine get() = classifierEngine

    val selectedMunicipalityFlow: Flow<Municipality> = configDao.getConfig("selected_municipality_id")
        .map { config ->
            val id = config?.value ?: "aisai"
            MunicipalityData.findById(id)
        }

    suspend fun saveSelectedMunicipality(municipalityId: String) {
        configDao.setConfig(AppConfigEntity("selected_municipality_id", municipalityId))
    }

    suspend fun autoDetectMunicipalityWithGps(): Municipality? {
        val detected = locationHelper.getCurrentMunicipality()
        if (detected != null) {
            saveSelectedMunicipality(detected.id)
        }
        return detected
    }

    suspend fun analyzeWaste(
        bitmap: Bitmap?,
        keyword: String?,
        municipality: Municipality
    ): WasteClassifierEngine.AnalysisOutput {
        return classifierEngine.analyzeImage(bitmap, municipality, keyword)
    }

    fun resolveQuestions(
        itemName: String,
        municipality: Municipality,
        answers: Map<String, String>
    ): SortingResult {
        return classifierEngine.resolveAnswers(itemName, municipality, answers)
    }

    suspend fun saveToHistory(result: SortingResult) {
        val entity = ScanHistoryEntity(
            itemName = result.itemName,
            categoryName = result.categoryName,
            categoryId = result.categoryId,
            colorHex = result.colorHex,
            municipalityName = result.municipalityName,
            disposalAdvice = result.disposalAdvice,
            nextDateText = result.nextDateText,
            daysRemainingText = result.daysRemainingText
        )
        historyDao.insert(entity)
    }

    suspend fun deleteHistory(id: Long) {
        historyDao.deleteById(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }
}
