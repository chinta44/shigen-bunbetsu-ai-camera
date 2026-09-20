package com.example.data.voice

import com.example.data.model.NextDateInfo
import com.example.data.model.SortingResult

/**
 * Result returned from processing a user voice query
 */
data class VoiceQueryResult(
    val userQuery: String,
    val displayTitle: String,
    val spokenResponse: String,         // Spoken aloud via Text-to-Speech
    val displayText: String,            // Displayed in UI
    val categoryBadge: String? = null,
    val categoryColorHex: Long? = null,
    val nextDateInfo: NextDateInfo? = null,
    val sortingResult: SortingResult? = null,
    val needsClarification: Boolean = false
)
