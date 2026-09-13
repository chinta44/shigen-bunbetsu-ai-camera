package com.example.data.model

enum class DropoffCategory(
    val title: String,
    val iconEmoji: String,
    val subtitle: String,
    val colorHex: Long
) {
    BATTERY("乾電池・充電池", "🔋", "リチウム電池/ボタン電池", 0xFFE53935),
    FLUORESCENT("蛍光管・電球", "💡", "水銀含有物・割れ物", 0xFFF57C00),
    SMALL_APPLIANCE("使用済小型家電", "📱", "スマホ/コード/ゲーム機", 0xFF1976D2),
    INK_CARTRIDGE("インクカートリッジ", "🖨️", "里帰りプロジェクト", 0xFF7B1FA2),
    FOOD_CONTAINER("食品トレー・牛乳パック", "🥛", "PET/白色トレー/紙パック", 0xFF388E3C),
    CLOTHES("古着・布類", "👕", "衣類・リユース繊維", 0xFF00796B);

    companion object {
        fun fromCategoryIdOrKeyword(categoryId: String, keyword: String = ""): DropoffCategory? {
            val lowerCat = categoryId.lowercase()
            val text = (categoryId + " " + keyword).lowercase()
            return when {
                text.contains("電池") || text.contains("バッテリー") || text.contains("battery") -> BATTERY
                text.contains("蛍光") || text.contains("電球") || text.contains("水銀") || text.contains("ライト") -> FLUORESCENT
                text.contains("小型家電") || text.contains("スマホ") || text.contains("携帯") || text.contains("コード") || text.contains("充電器") -> SMALL_APPLIANCE
                text.contains("インク") || text.contains("プリンタ") || text.contains("トナー") -> INK_CARTRIDGE
                text.contains("トレー") || text.contains("牛乳パック") || text.contains("ペットボトル") -> FOOD_CONTAINER
                text.contains("古着") || text.contains("衣類") || text.contains("服") -> CLOTHES
                lowerCat == "hazardous" -> BATTERY
                else -> null
            }
        }
    }
}

enum class FacilityType(val label: String, val iconEmoji: String) {
    CITY_HALL("市役所・行政窓口", "🏛️"),
    ELECTRONICS_STORE("家電量販店", "🏪"),
    HOME_CENTER("ホームセンター", "🛠️"),
    SUPERMARKET("スーパー・店頭", "🛒"),
    ECO_STATION("リサイクルステーション", "♻️")
}

data class RecycleSpot(
    val id: String,
    val name: String,
    val facilityType: FacilityType,
    val municipalityName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int? = null,
    val acceptedCategories: List<DropoffCategory>,
    val openingHours: String,
    val boxLocationNotes: String,
    val cautionNotes: String = "品目ごとに分別し、異物が混入しないよう投函してください。",
    val phone: String? = null
)
