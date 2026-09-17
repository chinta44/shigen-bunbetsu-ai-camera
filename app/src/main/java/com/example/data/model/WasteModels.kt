package com.example.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Waste disposal category
 */
data class WasteCategory(
    val id: String,
    val name: String,
    val shortName: String,
    val icon: String, // e.g. "burnable", "plastic", "metal", "paper", "bottle", "oversized", "danger"
    val colorHex: Long,
    val containerType: String, // e.g. "指定可燃袋(黄色)", "透明・半透明袋", "直接収集"
    val generalRules: String
)

/**
 * Recurrence pattern for garbage pickup
 */
sealed class ScheduleRecurrence {
    object Weekly : ScheduleRecurrence()
    data class MonthlyWeeks(val weeksOfMonth: List<Int>) : ScheduleRecurrence() // e.g. 1st and 3rd week
    object OnDemandReservation : ScheduleRecurrence() // 粗大ごみ等の予約制
}

/**
 * Collection schedule for a category in a municipality
 */
data class CollectionSchedule(
    val categoryId: String,
    val dayOfWeek: DayOfWeek?,
    val recurrence: ScheduleRecurrence = ScheduleRecurrence.Weekly,
    val scheduleDisplay: String, // e.g. "毎週火・金曜日", "第2・第4水曜日", "要予約 (月1回収集)"
    val instructions: String = "朝8:30までに出してください"
) {
    /**
     * Calculates the next collection date starting from fromDate
     */
    fun getNextCollectionDate(fromDate: LocalDate = LocalDate.now()): NextDateInfo {
        if (dayOfWeek == null || recurrence is ScheduleRecurrence.OnDemandReservation) {
            return NextDateInfo(
                dateText = "随時受付・要予約",
                dayOfWeekText = "電話/WEB予約制",
                daysRemainingText = "月1回収集",
                isUpcomingSoon = false,
                rawDate = null
            )
        }

        var candidate = fromDate
        for (i in 0..60) {
            val checkDate = fromDate.plusDays(i.toLong())
            if (checkDate.dayOfWeek == dayOfWeek) {
                val matchesRecurrence = when (val rec = recurrence) {
                    is ScheduleRecurrence.Weekly -> true
                    is ScheduleRecurrence.MonthlyWeeks -> {
                        val weekOfMonth = ((checkDate.dayOfMonth - 1) / 7) + 1
                        rec.weeksOfMonth.contains(weekOfMonth)
                    }
                    is ScheduleRecurrence.OnDemandReservation -> false
                }

                if (matchesRecurrence) {
                    val daysDiff = ChronoUnit.DAYS.between(fromDate, checkDate)
                    val daysText = when (daysDiff) {
                        0L -> "本日収集日！"
                        1L -> "明日"
                        2L -> "あさって"
                        else -> "あと${daysDiff}日"
                    }
                    val dayOfWeekJapanese = when (checkDate.dayOfWeek) {
                        DayOfWeek.MONDAY -> "月"
                        DayOfWeek.TUESDAY -> "火"
                        DayOfWeek.WEDNESDAY -> "水"
                        DayOfWeek.THURSDAY -> "木"
                        DayOfWeek.FRIDAY -> "金"
                        DayOfWeek.SATURDAY -> "土"
                        DayOfWeek.SUNDAY -> "日"
                    }
                    return NextDateInfo(
                        dateText = "${checkDate.monthValue}月${checkDate.dayOfMonth}日",
                        dayOfWeekText = "（${dayOfWeekJapanese}）",
                        daysRemainingText = daysText,
                        isUpcomingSoon = daysDiff <= 1L,
                        rawDate = checkDate
                    )
                }
            }
        }

        return NextDateInfo("未定", "", "", false, null)
    }
}

data class NextDateInfo(
    val dateText: String,
    val dayOfWeekText: String,
    val daysRemainingText: String,
    val isUpcomingSoon: Boolean,
    val rawDate: LocalDate?
)

/**
 * Official Municipality Oversized Waste Information & Fee Guide
 */
data class OversizedFeeItem(
    val itemName: String,
    val feeYen: Int,
    val stickerDescription: String
)

data class OversizedOfficialInfo(
    val reservationWebUrl: String,
    val phoneReservationNumber: String,
    val phoneReservationDisplay: String,
    val receptionHours: String,
    val feeRulesSummary: String,
    val stickerName: String,
    val purchasePlaces: String,
    val feeGuideItems: List<OversizedFeeItem> = emptyList()
)

/**
 * Municipality with rules and schedule
 */
data class Municipality(
    val id: String,
    val name: String,
    val prefecture: String,
    val district: String,
    val oversizedThresholdCm: Int = 30, // 粗大ごみの基準 (名古屋市: 30cm, 横浜市: 金属50cm/その他30cm)
    val plasticRuleNotes: String,
    val categories: List<WasteCategory>,
    val schedules: List<CollectionSchedule>,
    val oversizedOfficialInfo: OversizedOfficialInfo? = null
)

/**
 * Interactive Clarification Question (when AI or rules detect ambiguity)
 */
data class ClarificationQuestion(
    val id: String,
    val title: String,
    val questionText: String,
    val options: List<QuestionOption>
)

data class QuestionOption(
    val id: String,
    val label: String,
    val subtext: String? = null
)

/**
 * Item breakdown part for composite items (e.g. bottle body, cap, sponge/nozzle, remaining liquid)
 */
data class WastePartItem(
    val partName: String,
    val material: String,
    val categoryName: String,
    val disposalMethod: String
)

/**
 * Final Sorting Verdict
 */
data class SortingResult(
    val itemName: String,
    val categoryName: String,
    val categoryId: String,
    val colorHex: Long,
    val municipalityName: String,
    val nextDateText: String,
    val daysRemainingText: String,
    val disposalAdvice: String,
    val sizeMaterialNotes: String,
    val requiresReservation: Boolean = false,
    val isConfidenceHigh: Boolean = true,
    val confidenceScore: Int = 92, // 0 to 100 percentage
    val partsBreakdown: List<WastePartItem> = emptyList(),
    val detectedTexts: List<String> = emptyList()
)
