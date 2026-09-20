package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.ai.GeneratedMunicipalityResult
import com.example.data.ai.MunicipalityAiService
import com.example.data.ai.WasteClassifierEngine
import com.example.data.local.AppConfigEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CustomMunicipalityEntity
import com.example.data.local.ScanHistoryEntity
import com.example.data.location.LocationHelper
import com.example.data.model.CollectionSchedule
import com.example.data.model.DropoffCategory
import com.example.data.model.Municipality
import com.example.data.model.RecycleSpot
import com.example.data.model.ScheduleRecurrence
import com.example.data.model.SortingResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

class GarbageRepository(
    private val context: Context,
    private val db: AppDatabase = AppDatabase.getInstance(context),
    private val classifierEngine: WasteClassifierEngine = WasteClassifierEngine(),
    private val locationHelper: LocationHelper = LocationHelper(context),
    private val municipalityAiService: MunicipalityAiService = MunicipalityAiService(),
    private val recycleSpotRepository: RecycleSpotRepository = RecycleSpotRepository()
) {
    private val historyDao = db.scanHistoryDao()
    private val configDao = db.appConfigDao()
    private val customMunicipalityDao = db.customMunicipalityDao()

    val scanHistoryFlow: Flow<List<ScanHistoryEntity>> = historyDao.getAllHistory()
    val engine: WasteClassifierEngine get() = classifierEngine

    val customMunicipalitiesFlow: Flow<List<Municipality>> = customMunicipalityDao.getAllCustomMunicipalities()
        .map { list -> list.map { entityToMunicipality(it) } }

    val allAvailableMunicipalitiesFlow: Flow<List<Municipality>> = customMunicipalitiesFlow.map { customList ->
        val preset = MunicipalityData.ALL_MUNICIPALITIES
        val combined = customList + preset
        combined.distinctBy { it.id }
    }

    val selectedMunicipalityFlow: Flow<Municipality> = combine(
        configDao.getConfig("selected_municipality_id"),
        customMunicipalitiesFlow
    ) { config, customList ->
        val id = config?.value ?: "aisai"
        val customMatch = customList.firstOrNull { it.id.equals(id, ignoreCase = true) }
        if (customMatch != null) {
            customMatch
        } else {
            MunicipalityData.findById(id)
        }
    }

    val userApiKeyFlow: Flow<String> = configDao.getConfig("gemini_api_key")
        .map { it?.value.orEmpty() }

    suspend fun saveSelectedMunicipality(municipalityId: String) {
        configDao.setConfig(AppConfigEntity("selected_municipality_id", municipalityId))
    }

    suspend fun saveUserApiKey(key: String) {
        configDao.setConfig(AppConfigEntity("gemini_api_key", key.trim()))
    }

    val hapticsEnabledFlow: Flow<Boolean> = configDao.getConfig("haptics_enabled")
        .map { it?.value?.toBooleanStrictOrNull() ?: true }

    val soundEnabledFlow: Flow<Boolean> = configDao.getConfig("sound_enabled")
        .map { it?.value?.toBooleanStrictOrNull() ?: true }

    suspend fun saveHapticsEnabled(enabled: Boolean) {
        configDao.setConfig(AppConfigEntity("haptics_enabled", enabled.toString()))
    }

    suspend fun saveSoundEnabled(enabled: Boolean) {
        configDao.setConfig(AppConfigEntity("sound_enabled", enabled.toString()))
    }

    suspend fun autoDetectMunicipalityWithGps(): Municipality? {
        val detected = locationHelper.getCurrentMunicipality()
        if (detected != null) {
            saveSelectedMunicipality(detected.id)
        }
        return detected
    }

    suspend fun generateMunicipalityWithAi(query: String): GeneratedMunicipalityResult {
        val apiKey = userApiKeyFlow.firstOrNull()
        val result = municipalityAiService.generateMunicipalityRules(query, apiKey)
        // Automatically save to custom database
        saveCustomMunicipality(result.municipality)
        saveSelectedMunicipality(result.municipality.id)
        return result
    }

    suspend fun saveCustomMunicipality(municipality: Municipality) {
        val entity = municipalityToEntity(municipality)
        customMunicipalityDao.insert(entity)
    }

    suspend fun deleteCustomMunicipality(id: String) {
        customMunicipalityDao.deleteById(id)
        // If current selected was deleted, reset to aisai
        val currentSelected = configDao.getConfig("selected_municipality_id").firstOrNull()?.value
        if (currentSelected == id) {
            saveSelectedMunicipality("aisai")
        }
    }

    fun getRecycleSpots(
        municipalityName: String,
        category: DropoffCategory? = null,
        searchQuery: String = "",
        userLat: Double? = null,
        userLng: Double? = null
    ): List<RecycleSpot> {
        return recycleSpotRepository.getSpotsForMunicipality(
            municipalityName = municipalityName,
            categoryFilter = category,
            searchQuery = searchQuery,
            userLat = userLat,
            userLng = userLng
        )
    }

    suspend fun analyzeWaste(
        bitmap: Bitmap?,
        keyword: String?,
        municipality: Municipality,
        customApiKey: String? = null,
        targetItemHint: String? = null
    ): WasteClassifierEngine.AnalysisOutput {
        val key = if (!customApiKey.isNullOrBlank()) {
            customApiKey
        } else {
            userApiKeyFlow.firstOrNull()
        }
        return classifierEngine.analyzeImage(bitmap, municipality, keyword, key, targetItemHint)
    }

    fun createCustomResult(
        itemName: String,
        categoryId: String,
        municipality: Municipality,
        customAdvice: String? = null
    ): SortingResult {
        return classifierEngine.createCustomSortingResult(itemName, categoryId, municipality, customAdvice)
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

    // --- Entity <-> Model conversion ---
    private fun municipalityToEntity(m: Municipality): CustomMunicipalityEntity {
        val jsonArray = JSONArray()
        m.schedules.forEach { s ->
            val obj = JSONObject().apply {
                put("categoryId", s.categoryId)
                put("dayOfWeek", s.dayOfWeek?.name ?: "NONE")
                put("recurrenceType", when (val rec = s.recurrence) {
                    is ScheduleRecurrence.Weekly -> "WEEKLY"
                    is ScheduleRecurrence.MonthlyWeeks -> "MONTHLY_${rec.weeksOfMonth.joinToString(",")}"
                    is ScheduleRecurrence.OnDemandReservation -> "RESERVATION"
                })
                put("scheduleDisplay", s.scheduleDisplay)
                put("instructions", s.instructions)
            }
            jsonArray.put(obj)
        }

        return CustomMunicipalityEntity(
            id = m.id,
            name = m.name,
            prefecture = m.prefecture,
            district = m.district,
            oversizedThresholdCm = m.oversizedThresholdCm,
            plasticRuleNotes = m.plasticRuleNotes,
            schedulesJson = jsonArray.toString(),
            officialGuidePdfUrl = m.officialGuidePdfUrl,
            officialGuidePdfTitle = m.officialGuidePdfTitle,
            officialWebUrl = m.officialWebUrl
        )
    }

    private fun entityToMunicipality(e: CustomMunicipalityEntity): Municipality {
        val schedules = mutableListOf<CollectionSchedule>()
        try {
            val jsonArray = JSONArray(e.schedulesJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val catId = obj.getString("categoryId")
                val dayStr = obj.optString("dayOfWeek", "NONE")
                val day = if (dayStr != "NONE") {
                    try { DayOfWeek.valueOf(dayStr) } catch (_: Exception) { null }
                } else null

                val recType = obj.optString("recurrenceType", "WEEKLY")
                val recurrence = when {
                    recType.startsWith("MONTHLY_") -> {
                        val weeks = recType.removePrefix("MONTHLY_").split(",")
                            .mapNotNull { it.toIntOrNull() }
                        ScheduleRecurrence.MonthlyWeeks(if (weeks.isEmpty()) listOf(1, 3) else weeks)
                    }
                    recType == "RESERVATION" -> ScheduleRecurrence.OnDemandReservation
                    else -> ScheduleRecurrence.Weekly
                }

                val display = obj.optString("scheduleDisplay", "定期収集")
                val instr = obj.optString("instructions", "朝8:30までに出してください")
                schedules.add(CollectionSchedule(catId, day, recurrence, display, instr))
            }
        } catch (_: Exception) {
            // fallback standard schedules
            schedules.addAll(MunicipalityData.AISAI.schedules)
        }

        return Municipality(
            id = e.id,
            name = e.name,
            prefecture = e.prefecture,
            district = e.district,
            oversizedThresholdCm = e.oversizedThresholdCm,
            plasticRuleNotes = e.plasticRuleNotes,
            categories = listOf(
                MunicipalityData.CAT_BURNABLE,
                MunicipalityData.CAT_PLASTIC,
                MunicipalityData.CAT_BOTTLE_CAN,
                MunicipalityData.CAT_PAPER,
                MunicipalityData.CAT_NON_BURNABLE,
                MunicipalityData.CAT_OVERSIZED,
                MunicipalityData.CAT_HAZARDOUS
            ),
            schedules = schedules,
            officialGuidePdfUrl = e.officialGuidePdfUrl,
            officialGuidePdfTitle = e.officialGuidePdfTitle,
            officialWebUrl = e.officialWebUrl
        )
    }
}
