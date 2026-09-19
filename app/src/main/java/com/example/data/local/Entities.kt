package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemName: String,
    val categoryName: String,
    val categoryId: String,
    val colorHex: Long,
    val municipalityName: String,
    val disposalAdvice: String,
    val nextDateText: String,
    val daysRemainingText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey
    val key: String,
    val value: String
)

@Entity(tableName = "custom_municipalities")
data class CustomMunicipalityEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val prefecture: String,
    val district: String,
    val oversizedThresholdCm: Int,
    val plasticRuleNotes: String,
    val schedulesJson: String,
    val categoriesJson: String = "",
    val officialGuidePdfUrl: String? = null,
    val officialGuidePdfTitle: String? = null,
    val officialWebUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
