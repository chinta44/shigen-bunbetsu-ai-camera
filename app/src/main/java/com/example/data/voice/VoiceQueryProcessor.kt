package com.example.data.voice

import com.example.data.ai.WasteClassifierEngine
import com.example.data.model.CollectionSchedule
import com.example.data.model.Municipality
import com.example.data.model.NextDateInfo
import com.example.data.model.SortingResult
import com.example.data.repository.MunicipalityData
import java.time.LocalDate

object VoiceQueryProcessor {

    /**
     * Interprets a speech query in Japanese or English and generates verbal and textual responses
     */
    fun processQuery(
        query: String,
        municipality: Municipality,
        classifierEngine: WasteClassifierEngine
    ): VoiceQueryResult {
        val q = query.trim().lowercase()

        // 1. Is this a schedule query? ("When is the next pickup?", "次のゴミの日はいつ？", "燃えるゴミはいつ？")
        val isScheduleRelated = q.contains("いつ") ||
                q.contains("何曜日") ||
                q.contains("次の日") ||
                q.contains("次のゴミ") ||
                q.contains("収集日") ||
                q.contains("回収日") ||
                q.contains("next") ||
                q.contains("pickup") ||
                q.contains("when") ||
                q.contains("スケジュール") ||
                q.contains("カレンダー")

        if (isScheduleRelated) {
            return handleScheduleQuery(q, query, municipality)
        }

        // 2. Is this a general "What trash is this?" without specific item named?
        if (q == "これは何ゴミ？" || q == "これ何ゴミ" || q == "何ゴミ？" || q == "what trash is this" || q == "what trash is this?") {
            return VoiceQueryResult(
                userQuery = query,
                displayTitle = "ゴミの品名を教えてください",
                spokenResponse = "何のゴミについてお調べしますか？品名を声に出すか、カメラで写真を撮影してください。",
                displayText = "「ペットボトル」「スプレー缶」「プラスチックケース」のように品名をお話しいただくか、カメラで撮影・バーコード読取を行ってください。",
                needsClarification = false
            )
        }

        // 3. Waste sorting classification query (e.g. "ペットボトルは何ゴミ？", "スプレー缶はどう捨てる？", "フライパンの分別は？")
        val cleanItemName = extractItemName(query)
        val analysis = classifierEngine.analyzeByKeyword(cleanItemName, municipality)

        return when (analysis) {
            is WasteClassifierEngine.AnalysisOutput.Resolved -> {
                val r = analysis.result
                val spoken = buildString {
                    append("${r.itemName}は、${municipality.name}では【${r.categoryName}】です。")
                    if (r.daysRemainingText.isNotEmpty()) {
                        append("次回の収集は${r.daysRemainingText}、${r.nextDateText}です。")
                    }
                    if (r.disposalAdvice.isNotEmpty()) {
                        // Take first sentence or up to 60 chars for spoken brevity
                        val adviceShort = r.disposalAdvice.lines().firstOrNull { it.isNotBlank() } ?: ""
                        append(adviceShort.take(80))
                    }
                }

                VoiceQueryResult(
                    userQuery = query,
                    displayTitle = "${r.itemName}の分別結果",
                    spokenResponse = spoken,
                    displayText = buildString {
                        appendLine("【品名】${r.itemName}")
                        appendLine("【分別区分】${r.categoryName}（${municipality.name}）")
                        if (r.daysRemainingText.isNotEmpty()) {
                            appendLine("【次回収集日】${r.nextDateText}（${r.daysRemainingText}）")
                        }
                        appendLine()
                        appendLine("■ 捨て方・出し方:")
                        appendLine(r.disposalAdvice)
                        if (r.sizeMaterialNotes.isNotEmpty()) {
                            appendLine()
                            appendLine("■ 注意事項: ${r.sizeMaterialNotes}")
                        }
                    },
                    categoryBadge = r.categoryName,
                    categoryColorHex = r.colorHex,
                    nextDateInfo = null,
                    sortingResult = r,
                    needsClarification = false
                )
            }
            is WasteClassifierEngine.AnalysisOutput.NeedsClarification -> {
                val spoken = "${analysis.itemName}は、大きさや材質によって分別が分かれます。画面の質問から当てはまる項目をお選びください。"
                VoiceQueryResult(
                    userQuery = query,
                    displayTitle = "${analysis.itemName}：確認が必要です",
                    spokenResponse = spoken,
                    displayText = "${analysis.itemName}は自治体ルールにより大きさや材質で分別が異なります。\n${analysis.initialNote}\n\n画面に表示される質問にお答えいただくと正確な分別を判定します。",
                    categoryBadge = "要確認",
                    categoryColorHex = 0xFFFF9800,
                    needsClarification = true
                )
            }
            is WasteClassifierEngine.AnalysisOutput.Error -> {
                val spoken = "申し訳ありません。${cleanItemName}の分別情報を確認できませんでした。文字入力または写真撮影をお試しください。"
                VoiceQueryResult(
                    userQuery = query,
                    displayTitle = "判定できませんでした",
                    spokenResponse = spoken,
                    displayText = "「${cleanItemName}」の分別ルールが見つかりませんでした。別の言い方（例：缶、瓶、紙パック等）でお試しいただくか、カメラで撮影してください。",
                    needsClarification = false
                )
            }
        }
    }

    private fun handleScheduleQuery(q: String, originalQuery: String, municipality: Municipality): VoiceQueryResult {
        // Check if a specific category was mentioned
        val specificCategory = when {
            q.contains("可燃") || q.contains("燃える") || q.contains("もえる") || q.contains("生ごみ") ->
                municipality.categories.firstOrNull { it.id == "burnable" }
            q.contains("プラ") || q.contains("プラスチック") || q.contains("容器包装") ->
                municipality.categories.firstOrNull { it.id == "plastic" }
            q.contains("資源") || q.contains("ペット") || q.contains("缶") || q.contains("びん") || q.contains("瓶") ->
                municipality.categories.firstOrNull { it.id == "resources" }
            q.contains("不燃") || q.contains("燃えない") || q.contains("もえない") || q.contains("金属") ->
                municipality.categories.firstOrNull { it.id == "non_burnable" }
            q.contains("紙") || q.contains("古紙") || q.contains("ダンボール") ->
                municipality.categories.firstOrNull { it.id == "paper" }
            q.contains("粗大") ->
                municipality.categories.firstOrNull { it.id == "oversized" }
            else -> null
        }

        if (specificCategory != null) {
            val sched = municipality.schedules.firstOrNull { it.categoryId == specificCategory.id }
            val next = sched?.getNextCollectionDate()

            if (next != null && next.rawDate != null) {
                val spoken = "${municipality.name}の【${specificCategory.name}】は、${next.daysRemainingText}、${next.dateText} ${next.dayOfWeekText}です。${sched.instructions}。"
                return VoiceQueryResult(
                    userQuery = originalQuery,
                    displayTitle = "${specificCategory.name}の次回収集日",
                    spokenResponse = spoken,
                    displayText = buildString {
                        appendLine("【自治体】${municipality.name} (${municipality.district})")
                        appendLine("【品目】${specificCategory.name}")
                        appendLine("【次回収集日】${next.dateText} ${next.dayOfWeekText} (${next.daysRemainingText})")
                        appendLine("【定期日程】${sched.scheduleDisplay}")
                        appendLine("【出し方】${sched.instructions}")
                        appendLine("【指定容器】${specificCategory.containerType}")
                    },
                    categoryBadge = specificCategory.name,
                    categoryColorHex = specificCategory.colorHex,
                    nextDateInfo = next
                )
            } else if (specificCategory.id == "oversized") {
                val spoken = "${municipality.name}の粗大ごみは事前予約制です。電話またはインターネットでお申し込みください。"
                return VoiceQueryResult(
                    userQuery = originalQuery,
                    displayTitle = "粗大ごみの収集案内",
                    spokenResponse = spoken,
                    displayText = "${municipality.name}の粗大ごみは月1回の予約制収集です。事前に粗大ごみ受付センター（インターネットまたは電話）へお申し込みください。",
                    categoryBadge = specificCategory.name,
                    categoryColorHex = specificCategory.colorHex
                )
            }
        }

        // Nearest upcoming pickup for all categories
        val upcoming = municipality.schedules
            .mapNotNull { sched ->
                val next = sched.getNextCollectionDate()
                if (next.rawDate != null) sched to next else null
            }
            .minByOrNull { it.second.rawDate!! }

        if (upcoming != null) {
            val (sched, next) = upcoming
            val cat = municipality.categories.firstOrNull { it.id == sched.categoryId }
            val catName = cat?.name ?: "ごみ"
            val spoken = "${municipality.name}の直近の収集日は、${next.daysRemainingText}、${next.dateText} ${next.dayOfWeekText}の【${catName}】です。${sched.instructions}。"

            return VoiceQueryResult(
                userQuery = originalQuery,
                displayTitle = "直近のゴミ収集日",
                spokenResponse = spoken,
                displayText = buildString {
                    appendLine("【自治体】${municipality.name} (${municipality.district})")
                    appendLine("【次の収集】${next.daysRemainingText}・${next.dateText} ${next.dayOfWeekText}")
                    appendLine("【種別】${catName}")
                    appendLine("【定期日程】${sched.scheduleDisplay}")
                    appendLine("【注意事項】${sched.instructions}")
                },
                categoryBadge = catName,
                categoryColorHex = cat?.colorHex ?: 0xFF4CAF50,
                nextDateInfo = next
            )
        }

        val fallbackSpoken = "${municipality.name}の収集日程は収集カレンダー画面からご確認いただけます。"
        return VoiceQueryResult(
            userQuery = originalQuery,
            displayTitle = "収集日程のご案内",
            spokenResponse = fallbackSpoken,
            displayText = "${municipality.name}の収集日程はアプリ下部の「カレンダー」タブより一覧でご確認いただけます。"
        )
    }

    private fun extractItemName(raw: String): String {
        return raw.replace("？", "")
            .replace("?", "")
            .replace("これは", "")
            .replace("これ", "")
            .replace("は何ゴミ", "")
            .replace("は何ごみ", "")
            .replace("何ゴミ", "")
            .replace("何ごみ", "")
            .replace("はどうやって捨てる", "")
            .replace("はどう捨てる", "")
            .replace("はどう分別する", "")
            .replace("の捨て方", "")
            .replace("の分別", "")
            .replace("はどうする", "")
            .replace("を捨てたい", "")
            .replace("って何ゴミ", "")
            .trim()
    }
}
