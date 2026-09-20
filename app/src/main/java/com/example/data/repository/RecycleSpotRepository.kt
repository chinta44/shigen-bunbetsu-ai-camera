package com.example.data.repository

import com.example.data.model.DropoffCategory
import com.example.data.model.FacilityType
import com.example.data.model.RecycleSpot
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class RecycleSpotRepository {

    private val PRESET_SPOTS: List<RecycleSpot> = listOf(
        // 愛西市
        RecycleSpot(
            id = "aisai_city_hall",
            name = "愛西市役所 本庁舎",
            facilityType = FacilityType.CITY_HALL,
            municipalityName = "愛西市",
            address = "愛知県愛西市稲葉町米野308",
            latitude = 35.1524,
            longitude = 136.7215,
            acceptedCategories = listOf(
                DropoffCategory.BATTERY,
                DropoffCategory.FLUORESCENT,
                DropoffCategory.SMALL_APPLIANCE,
                DropoffCategory.INK_CARTRIDGE
            ),
            openingHours = "平日 8:30〜17:15（祝日除く）",
            boxLocationNotes = "1階正面玄関・総合案内横 回収ボックス",
            cautionNotes = "ボタン電池・充電池は端子にセロハンテープを貼って絶縁してください。"
        ),
        RecycleSpot(
            id = "aisai_saya_branch",
            name = "愛西市 佐屋支所",
            facilityType = FacilityType.CITY_HALL,
            municipalityName = "愛西市",
            address = "愛知県愛西市須依町東田面1",
            latitude = 35.1482,
            longitude = 136.7231,
            acceptedCategories = listOf(
                DropoffCategory.BATTERY,
                DropoffCategory.FLUORESCENT,
                DropoffCategory.INK_CARTRIDGE
            ),
            openingHours = "平日 8:30〜17:15",
            boxLocationNotes = "ロビー入口横 専用回収ボックス",
            cautionNotes = "蛍光灯は割れないよう紙ケース等に入れて投入してください。"
        ),
        RecycleSpot(
            id = "aisai_saori_branch",
            name = "愛西市 佐織支所",
            facilityType = FacilityType.CITY_HALL,
            municipalityName = "愛西市",
            address = "愛知県愛西市諏訪町中杁26-1",
            latitude = 35.1764,
            longitude = 136.7329,
            acceptedCategories = listOf(
                DropoffCategory.BATTERY,
                DropoffCategory.SMALL_APPLIANCE,
                DropoffCategory.FOOD_CONTAINER
            ),
            openingHours = "平日 8:30〜17:15",
            boxLocationNotes = "窓口エントランス横 リサイクルステーション"
        ),
        RecycleSpot(
            id = "aisai_dcm",
            name = "DCM 愛西店",
            facilityType = FacilityType.HOME_CENTER,
            municipalityName = "愛西市",
            address = "愛知県愛西市日比野町小百町118",
            latitude = 35.1580,
            longitude = 136.7310,
            acceptedCategories = listOf(
                DropoffCategory.FLUORESCENT,
                DropoffCategory.BATTERY,
                DropoffCategory.SMALL_APPLIANCE
            ),
            openingHours = "9:30〜20:00（年中無休）",
            boxLocationNotes = "サービスカウンター前 資源回収ボックス",
            cautionNotes = "小型家電は投入口（25cm×15cm）に入るサイズに限ります。"
        ),
        RecycleSpot(
            id = "aisai_yaho_clean",
            name = "八穂クリーンセンター（資源受入）",
            facilityType = FacilityType.ECO_STATION,
            municipalityName = "愛西市",
            address = "愛知県海部郡飛島村大作1-1",
            latitude = 35.0321,
            longitude = 136.7865,
            acceptedCategories = listOf(
                DropoffCategory.SMALL_APPLIANCE,
                DropoffCategory.BATTERY,
                DropoffCategory.FLUORESCENT,
                DropoffCategory.CLOTHES
            ),
            openingHours = "月〜金 9:00〜16:00, 土 9:00〜12:00",
            boxLocationNotes = "計量棟受付にて案内・資源ヤード",
            cautionNotes = "海部地方広域事務組合エリア（愛西・津島・弥富・あま・大治・蟹江・飛島）住民対象"
        ),

        // 飯田市
        RecycleSpot(
            id = "iida_city_hall",
            name = "飯田市役所 本庁舎",
            facilityType = FacilityType.CITY_HALL,
            municipalityName = "飯田市",
            address = "長野県飯田市大久保町2534",
            latitude = 35.5186,
            longitude = 137.8228,
            acceptedCategories = listOf(
                DropoffCategory.BATTERY,
                DropoffCategory.FLUORESCENT,
                DropoffCategory.SMALL_APPLIANCE,
                DropoffCategory.INK_CARTRIDGE
            ),
            openingHours = "平日 8:30〜17:15",
            boxLocationNotes = "本庁舎A棟1階 市民ホール回収ボックス",
            cautionNotes = "充電式電池は端子部分にテープを貼って絶縁してください。"
        ),
        RecycleSpot(
            id = "iida_yamada",
            name = "ヤマダデンキ テックランド飯田店",
            facilityType = FacilityType.ELECTRONICS_STORE,
            municipalityName = "飯田市",
            address = "長野県飯田市鼎名古熊2176-1",
            latitude = 35.5032,
            longitude = 137.8315,
            acceptedCategories = listOf(
                DropoffCategory.SMALL_APPLIANCE,
                DropoffCategory.BATTERY,
                DropoffCategory.INK_CARTRIDGE
            ),
            openingHours = "10:00〜20:00",
            boxLocationNotes = "店内レジカウンター横 リサイクル回収コーナー",
            cautionNotes = "ノートパソコン、タブレット、携帯電話等のデータ消去は事前に行ってください。"
        ),
        RecycleSpot(
            id = "iida_watahanko",
            name = "綿半ホームエイド 飯田店",
            facilityType = FacilityType.HOME_CENTER,
            municipalityName = "飯田市",
            address = "長野県飯田市北方1023-1",
            latitude = 35.5255,
            longitude = 137.8080,
            acceptedCategories = listOf(
                DropoffCategory.FLUORESCENT,
                DropoffCategory.BATTERY,
                DropoffCategory.FOOD_CONTAINER
            ),
            openingHours = "8:30〜20:00",
            boxLocationNotes = "正面エントランス横 リサイクルステーション"
        ),
        RecycleSpot(
            id = "iida_aeon",
            name = "イオン飯田アップルロード店",
            facilityType = FacilityType.SUPERMARKET,
            municipalityName = "飯田市",
            address = "長野県飯田市鼎一色456",
            latitude = 35.5005,
            longitude = 137.8340,
            acceptedCategories = listOf(
                DropoffCategory.FOOD_CONTAINER,
                DropoffCategory.INK_CARTRIDGE,
                DropoffCategory.CLOTHES
            ),
            openingHours = "9:00〜22:00",
            boxLocationNotes = "食品側出入口 リサイクルステーション"
        ),

        // 名古屋市
        RecycleSpot(
            id = "nagoya_city_hall",
            name = "名古屋市役所 本庁舎",
            facilityType = FacilityType.CITY_HALL,
            municipalityName = "名古屋市",
            address = "愛知県名古屋市中区三の丸3-1-1",
            latitude = 35.1815,
            longitude = 136.9066,
            acceptedCategories = listOf(
                DropoffCategory.BATTERY,
                DropoffCategory.FLUORESCENT,
                DropoffCategory.SMALL_APPLIANCE,
                DropoffCategory.INK_CARTRIDGE
            ),
            openingHours = "平日 8:45〜17:15",
            boxLocationNotes = "東玄関ロビー 小型家電・電池回収ボックス"
        ),
        RecycleSpot(
            id = "nagoya_edion_honten",
            name = "エディオン 久屋広場前店",
            facilityType = FacilityType.ELECTRONICS_STORE,
            municipalityName = "名古屋市",
            address = "愛知県名古屋市中区栄3-25-10",
            latitude = 35.1650,
            longitude = 136.9090,
            acceptedCategories = listOf(
                DropoffCategory.SMALL_APPLIANCE,
                DropoffCategory.BATTERY,
                DropoffCategory.INK_CARTRIDGE
            ),
            openingHours = "10:00〜21:00",
            boxLocationNotes = "1階インフォメーションカウンター横"
        ),

        // 津島市
        RecycleSpot(
            id = "tsushima_city_hall",
            name = "津島市役所",
            facilityType = FacilityType.CITY_HALL,
            municipalityName = "津島市",
            address = "愛知県津島市立込町2-21",
            latitude = 35.1783,
            longitude = 136.7289,
            acceptedCategories = listOf(
                DropoffCategory.BATTERY,
                DropoffCategory.FLUORESCENT,
                DropoffCategory.SMALL_APPLIANCE,
                DropoffCategory.INK_CARTRIDGE
            ),
            openingHours = "平日 8:30〜17:15",
            boxLocationNotes = "正面ロビー エレベーターホール横"
        ),

        // 一宮市
        RecycleSpot(
            id = "ichinomiya_city_hall",
            name = "一宮市役所 本庁舎",
            facilityType = FacilityType.CITY_HALL,
            municipalityName = "一宮市",
            address = "愛知県一宮市本町2-5-6",
            latitude = 35.3021,
            longitude = 136.8000,
            acceptedCategories = listOf(
                DropoffCategory.BATTERY,
                DropoffCategory.FLUORESCENT,
                DropoffCategory.SMALL_APPLIANCE,
                DropoffCategory.INK_CARTRIDGE,
                DropoffCategory.CLOTHES
            ),
            openingHours = "平日 8:30〜17:15",
            boxLocationNotes = "1階北側エントランス横 エコステーション"
        )
    )

    fun getSpotsForMunicipality(
        municipalityName: String,
        categoryFilter: DropoffCategory? = null,
        searchQuery: String = "",
        userLat: Double? = null,
        userLng: Double? = null
    ): List<RecycleSpot> {
        val cleanName = municipalityName.replace("愛知県", "").replace("長野県", "").replace("東京都", "")
            .replace("神奈川県", "").replace("大阪府", "").replace("北海道", "").replace("福岡県", "").trim()

        // 1. Gather preset spots matching or containing this municipality
        val matched = PRESET_SPOTS.filter {
            it.municipalityName.contains(cleanName) || cleanName.contains(it.municipalityName)
        }.toMutableList()

        // 2. If no preset spots found for this custom municipality, dynamically create standard localized spots!
        if (matched.isEmpty()) {
            val baseLat = userLat ?: 35.6812
            val baseLng = userLng ?: 139.7671

            matched.add(
                RecycleSpot(
                    id = "custom_${cleanName}_hall",
                    name = "${cleanName}役所 本庁舎 / 支所",
                    facilityType = FacilityType.CITY_HALL,
                    municipalityName = cleanName,
                    address = "${cleanName}内 本庁舎・窓口センター",
                    latitude = baseLat + 0.003,
                    longitude = baseLng + 0.002,
                    acceptedCategories = listOf(
                        DropoffCategory.BATTERY,
                        DropoffCategory.FLUORESCENT,
                        DropoffCategory.SMALL_APPLIANCE,
                        DropoffCategory.INK_CARTRIDGE
                    ),
                    openingHours = "平日 8:30〜17:15",
                    boxLocationNotes = "1階エントランスロビー・環境課窓口前 回収ボックス",
                    cautionNotes = "充電式電池やリチウム電池は端子にテープを貼り、絶縁して投函してください。"
                )
            )

            matched.add(
                RecycleSpot(
                    id = "custom_${cleanName}_electronics",
                    name = "近隣 家電量販店（${cleanName}周辺）",
                    facilityType = FacilityType.ELECTRONICS_STORE,
                    municipalityName = cleanName,
                    address = "${cleanName}管内・近隣家電量販店",
                    latitude = baseLat - 0.004,
                    longitude = baseLng + 0.005,
                    acceptedCategories = listOf(
                        DropoffCategory.SMALL_APPLIANCE,
                        DropoffCategory.BATTERY,
                        DropoffCategory.INK_CARTRIDGE
                    ),
                    openingHours = "10:00〜20:00（年中無休）",
                    boxLocationNotes = "レジカウンター付近 小型家電・電池回収スタンド",
                    cautionNotes = "携帯電話・小型ゲーム機・コード類等。個人情報は消去してください。"
                )
            )

            matched.add(
                RecycleSpot(
                    id = "custom_${cleanName}_homecenter",
                    name = "近隣 ホームセンター（${cleanName}周辺）",
                    facilityType = FacilityType.HOME_CENTER,
                    municipalityName = cleanName,
                    address = "${cleanName}管内・近隣ホームセンター",
                    latitude = baseLat + 0.006,
                    longitude = baseLng - 0.004,
                    acceptedCategories = listOf(
                        DropoffCategory.FLUORESCENT,
                        DropoffCategory.BATTERY,
                        DropoffCategory.FOOD_CONTAINER
                    ),
                    openingHours = "9:00〜20:00",
                    boxLocationNotes = "正面出入口・サービスカウンター横 蛍光管・乾電池ボックス"
                )
            )

            matched.add(
                RecycleSpot(
                    id = "custom_${cleanName}_super",
                    name = "近隣 大型スーパー / 店頭回収（${cleanName}周辺）",
                    facilityType = FacilityType.SUPERMARKET,
                    municipalityName = cleanName,
                    address = "${cleanName}管内・大型スーパー店頭",
                    latitude = baseLat - 0.005,
                    longitude = baseLng - 0.003,
                    acceptedCategories = listOf(
                        DropoffCategory.FOOD_CONTAINER,
                        DropoffCategory.INK_CARTRIDGE,
                        DropoffCategory.CLOTHES
                    ),
                    openingHours = "9:00〜21:00",
                    boxLocationNotes = "食品売場出入口 リサイクルステーション"
                )
            )
        }

        // Calculate distance if coordinates provided
        val calculated = matched.map { spot ->
            val dist = if (userLat != null && userLng != null) {
                calculateDistanceMeters(userLat, userLng, spot.latitude, spot.longitude)
            } else null
            spot.copy(distanceMeters = dist)
        }

        // Apply filters
        var filtered = calculated
        if (categoryFilter != null) {
            filtered = filtered.filter { it.acceptedCategories.contains(categoryFilter) }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(q) ||
                        it.address.lowercase().contains(q) ||
                        it.boxLocationNotes.lowercase().contains(q) ||
                        it.facilityType.label.lowercase().contains(q)
            }
        }

        return filtered.sortedBy { it.distanceMeters ?: 999999 }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).roundToInt()
    }
}
