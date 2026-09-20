package com.example.data.ai

import android.graphics.Bitmap
import com.example.data.model.ClarificationQuestion
import com.example.data.model.Municipality
import com.example.data.model.QuestionOption
import com.example.data.model.SortingResult
import com.example.data.repository.MunicipalityData
import org.json.JSONObject
import java.time.LocalDate

class WasteClassifierEngine(
    private val geminiService: GeminiWasteService = GeminiWasteService()
) {

    sealed class AnalysisOutput {
        data class Resolved(val result: SortingResult) : AnalysisOutput()
        data class NeedsClarification(
            val itemName: String,
            val initialNote: String,
            val questions: List<ClarificationQuestion>
        ) : AnalysisOutput()
        data class Error(val message: String) : AnalysisOutput()
    }

    /**
     * Analyze from captured/selected image
     */
    suspend fun analyzeImage(
        bitmap: Bitmap?,
        municipality: Municipality,
        fallbackItemKeyword: String? = null,
        customApiKey: String? = null,
        targetItemHint: String? = null
    ): AnalysisOutput {
        if (bitmap != null) {
            val aiResult = geminiService.analyzeWasteImage(
                bitmap = bitmap,
                municipalityName = municipality.name,
                prefectureName = municipality.prefecture,
                oversizedThresholdCm = municipality.oversizedThresholdCm,
                customApiKey = customApiKey,
                targetItemHint = targetItemHint
            )

            when (aiResult) {
                is WasteAiAnalysisResult.Success -> {
                    val parsed = parseAiResponse(aiResult.jsonText, municipality)
                    if (parsed != null) return parsed
                }
                is WasteAiAnalysisResult.Error -> {
                    // Fall back to rule engine below
                }
                is WasteAiAnalysisResult.KeyMissing -> {
                    // Fall back to rule engine below
                }
            }
        }

        // Rule-based fallback or keyword based analysis
        val query = targetItemHint?.trim()?.ifBlank { null }
            ?: fallbackItemKeyword?.trim()
            ?: "プラスチックケース"
        return analyzeByKeyword(query, municipality)
    }

    /**
     * Analyze by keyword / text query
     */
    fun analyzeByKeyword(query: String, municipality: Municipality): AnalysisOutput {
        val q = query.lowercase()

        // 0. Leather goods / Wallets / Pouches (革財布・長財布・レザー製品)
        if (q.contains("財布") || q.contains("革") || q.contains("レザー") || q.contains("ウォレット") || q.contains("定期入れ") || q.contains("名刺入れ")) {
            val burnableCat = municipality.categories.firstOrNull { it.id == "burnable" }
                ?: MunicipalityData.CAT_BURNABLE
            val schedule = municipality.schedules.firstOrNull { it.categoryId == burnableCat.id }
            val nextInfo = schedule?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            val displayName = if (q.contains("長財布")) "革製の長財布" else if (q.contains("財布")) "財布（革・布・合皮）" else query.trim()
            return AnalysisOutput.Resolved(
                SortingResult(
                    itemName = displayName,
                    categoryName = burnableCat.name,
                    categoryId = burnableCat.id,
                    colorHex = burnableCat.colorHex,
                    municipalityName = municipality.name,
                    nextDateText = nextInfo.dateText + nextInfo.dayOfWeekText,
                    daysRemainingText = nextInfo.daysRemainingText,
                    disposalAdvice = "本革・合皮・布製の長財布や小銭入れは【可燃ごみ（燃やすごみ）】です。\nファスナー金具やホックなど取り外しにくい小さな金属部品は、無理に外さずそのまま出して問題ありません。\n※一辺が${municipality.oversizedThresholdCm}cmを超える特大ケース類の場合は粗大ごみ扱いとなります。",
                    sizeMaterialNotes = "${municipality.name}の分別基準（皮革・繊維・合成皮革は可燃ごみ）に準拠しています。",
                    requiresReservation = false
                )
            )
        }

        // 0-B. Eyeglasses & Eyeglass cases (メガネ・メガネケース)
        if (q.contains("メガネ") || q.contains("眼鏡") || q.contains("サングラス")) {
            val nonBurnableCat = municipality.categories.firstOrNull { it.id == "non_burnable" }
                ?: MunicipalityData.CAT_NON_BURNABLE
            val schedule = municipality.schedules.firstOrNull { it.categoryId == nonBurnableCat.id }
            val nextInfo = schedule?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            val isCase = q.contains("ケース")
            return AnalysisOutput.Resolved(
                SortingResult(
                    itemName = if (isCase) "メガネケース" else "眼鏡・メガネ",
                    categoryName = nonBurnableCat.name,
                    categoryId = nonBurnableCat.id,
                    colorHex = nonBurnableCat.colorHex,
                    municipalityName = municipality.name,
                    nextDateText = nextInfo.dateText + nextInfo.dayOfWeekText,
                    daysRemainingText = nextInfo.daysRemainingText,
                    disposalAdvice = if (isCase) {
                        "プラスチック製や金属製、金属芯入りのメガネケースは【不燃ごみ（燃えないごみ・陶器金属ガラス）】です。\n布や本革製で金属部品がないソフトケースの場合は【可燃ごみ】として出せます。"
                    } else {
                        "フレームやレンズに金属・ガラス・硬質プラが使われているため【不燃ごみ】です。ガラスレンズが破損している場合は紙などに包み「キケン」と表示してください。"
                    },
                    sizeMaterialNotes = "${municipality.name}の分別ルールに準拠しています。",
                    requiresReservation = false
                )
            )
        }

        // 0-C. Smartphones, Small Electronics, Circuit Boards, Fans, Lithium-ion Devices (電子基板・小型家電・扇風機・スマホ等)
        if (q.contains("スマホ") || q.contains("スマートフォン") || q.contains("携帯電話") || q.contains("携帯") ||
            q.contains("タブレット") || q.contains("スマートウォッチ") || q.contains("小型家電") ||
            q.contains("充電器") || q.contains("アダプター") || q.contains("ガラケー") || q.contains("電子辞書") ||
            q.contains("イヤホン") || q.contains("ワイヤレスイヤホン") ||
            q.contains("扇風機") || q.contains("サーキュレーター") || q.contains("ハンディファン") ||
            q.contains("ドライヤー") || q.contains("アイロン") ||
            q.contains("基板") || q.contains("制御基板") || q.contains("プリント基板") || q.contains("回路") ||
            q.contains("電子部品") || q.contains("半導体") || q.contains("コンデンサ") || q.contains("マザーボード")
        ) {
            val isCircuitBoard = q.contains("基板") || q.contains("制御基板") || q.contains("プリント基板") ||
                    q.contains("回路") || q.contains("電子部品") || q.contains("半導体") || q.contains("コンデンサ") || q.contains("マザーボード")
            val isFan = q.contains("扇風機") || q.contains("サーキュレーター") || q.contains("ハンディファン")
            val smallAppCat = municipality.categories.firstOrNull { it.id == "small_appliance" }
                ?: municipality.categories.firstOrNull { it.id == "non_burnable" }
                ?: MunicipalityData.CAT_SMALL_APPLIANCE

            val isPhone = q.contains("スマホ") || q.contains("スマートフォン") || q.contains("携帯")
            val displayName = when {
                isCircuitBoard -> if (q.contains("制御基板")) "電子基板（制御基板）" else "電子基板・電子部品"
                isPhone -> "スマートフォン"
                q.contains("タブレット") -> "タブレット端末"
                q.contains("ハンディファン") -> "携帯用ハンディファン（充電式扇風機）"
                q.contains("サーキュレーター") -> "サーキュレーター（小型扇風機）"
                q.contains("扇風機") -> "小型扇風機（サーキュレーター）"
                else -> query.trim()
            }

            val advice = if (isCircuitBoard) {
                if (municipality.id == "aisai") {
                    "電子基板・制御基板などの電子部品は【プラスチックごみには絶対に出せません】。\n" +
                    "【出し方1（推奨・無料）】愛西市役所本庁舎、各支所（八開・立田・佐織）、中央公民館等の『使用済小型家電回収ボックス』（投入口 縦15cm×横30cm）へ投入してください（有用金属資源リサイクル）。袋は不要です。\n" +
                    "【出し方2】回収ボックスに入らない場合は、愛西市指定の『不燃物専用袋（黄色）』に入れて【不燃ごみ】（毎月第2水曜日）へお出しください。\n" +
                    "⚠️プラスチック資源に混入すると、リサイクル処理工場で再生機器の重大な故障や異物混入事故を引き起こします。"
                } else {
                    "${municipality.name}では電子基板・電子パーツは【${smallAppCat.name}】です。プラスチック資源には出せません。\n" +
                    "※小型家電回収ボックスをご利用いただくか、指定の不燃ごみ袋でお出しください。"
                }
            } else if (isFan) {
                if (municipality.id == "aisai") {
                    "小型扇風機・サーキュレーター等の電化製品は【プラスチックごみには絶対に出せません】。\n" +
                    "【出し方1】市役所・支所設置の『小型家電回収ボックス』（投入口 縦15cm×横30cm以内）に入る卓上サイズは無料リサイクル回収へ。\n" +
                    "【出し方2】ボックスに入らないが愛西市指定不燃物専用袋に入るものは【不燃ごみ】（毎月第2水曜日）へ。\n" +
                    "※市指定袋に入らない大型リビング扇風機は【粗大ごみ】となります。\n" +
                    "⚠️【重要：充電式・ハンディファンの場合】内蔵のリチウムイオン電池は収集車火災の原因となるため、電池を取り外せるものは電池回収協力店へ。取り外せないものは市役所環境課等へご相談ください。"
                } else {
                    "${municipality.name}では小型扇風機・サーキュレーターは【${smallAppCat.name}】です。プラスチックごみには出せません。\n" +
                    "※小型家電回収ボックスをご利用いただくか、指定の不燃ごみ袋でお出しください。\n" +
                    "⚠️充電式（バッテリー内蔵）の場合は発火事故防止のため必ず電池を取り外して危険ごみ・電池回収へ。"
                }
            } else {
                "【発火注意・集積所排出禁止】\nスマートフォン等の充電式電子機器にはリチウムイオン電池が内蔵されており、可燃ごみ・不燃ごみとして出すと収集車や処理施設での重大な火災原因となります。\n\n必ず端末内のデータを初期化・消去し、市役所・支所・公民館・家電量販店などに設置されている「使用済小型家電回収ボックス」に投入してください（袋不要・無料）。各携帯キャリアショップでも無償回収しています。"
            }

            return AnalysisOutput.Resolved(
                SortingResult(
                    itemName = displayName,
                    categoryName = smallAppCat.name,
                    categoryId = smallAppCat.id,
                    colorHex = smallAppCat.colorHex,
                    municipalityName = municipality.name,
                    nextDateText = if ((isFan || isCircuitBoard) && municipality.id == "aisai") "毎月第2水曜日（不燃）/ 拠点BOXは随時" else "市役所・量販店等の回収BOX（開館中随時）",
                    daysRemainingText = if ((isFan || isCircuitBoard) && municipality.id == "aisai") "拠点BOXまたは不燃収集" else "常時持込可",
                    disposalAdvice = advice,
                    sizeMaterialNotes = if (isCircuitBoard) "都市鉱山（金・銀・銅・レアメタル）リサイクル対象品です。プラスチック資源には出せません。" else "モーター・電子基板・配線を含む電化製品です。プラスチック資源には出せません。",
                    requiresReservation = false
                )
            )
        }

        // 0-D. Stainless Steel Water Bottles, Metal Flasks, Thermoses, Tumblers (ステンレス水筒・魔法瓶・金属ボトル)
        if (q.contains("水筒") || q.contains("ステンレスボトル") || q.contains("魔法瓶") || q.contains("まほうびん") ||
            q.contains("タンブラー") || (q.contains("ステンレス") && (q.contains("ボトル") || q.contains("マグ") || q.contains("ポット")))
        ) {
            val isPlasticBottle = (q.contains("プラスチック") || q.contains("プラ製")) && !q.contains("ステンレス") && !q.contains("金属")
            if (isPlasticBottle) {
                // Plastic water bottle
                val plasticCat = municipality.categories.firstOrNull { it.id == "plastic" }
                    ?: municipality.categories.firstOrNull { it.id == "burnable" }
                    ?: MunicipalityData.CAT_PLASTIC
                val sched = municipality.schedules.firstOrNull { it.categoryId == plasticCat.id }
                val nextInfo = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()
                return AnalysisOutput.Resolved(
                    SortingResult(
                        itemName = "プラスチック製水筒・クリアボトル",
                        categoryName = plasticCat.name,
                        categoryId = plasticCat.id,
                        colorHex = plasticCat.colorHex,
                        municipalityName = municipality.name,
                        nextDateText = nextInfo.dateText + nextInfo.dayOfWeekText,
                        daysRemainingText = nextInfo.daysRemainingText,
                        disposalAdvice = "プラスチック素材の水筒は、中をきれいに洗って乾かしてから出してください。ゴムパッキンは外して可燃ごみへ。",
                        sizeMaterialNotes = "${municipality.name}のプラスチック分別ルールに準拠しています。",
                        requiresReservation = false
                    )
                )
            } else {
                // Metal / Stainless steel water bottle (Default for 水筒)
                val metalCat = municipality.categories.firstOrNull { it.id == "non_burnable" || it.id == "metal" }
                    ?: MunicipalityData.CAT_NON_BURNABLE
                val sched = municipality.schedules.firstOrNull { it.categoryId == metalCat.id }
                val nextInfo = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

                val advice = if (municipality.id == "aisai") {
                    "ステンレス製水筒・金属ボトル本体は【不燃ごみ（指定不燃物袋）】です（第2水曜日）。\n※プラスチック製のキャップ・フタやシリコンゴムパッキンは外し、キャップは【プラスチック類ごみ（毎週火曜）】、パッキンは【可燃ごみ（月・木）】へ分別してください。"
                } else {
                    "${municipality.name}ではステンレス水筒・魔法瓶本体は【${metalCat.name}】です。\n※プラスチック製のフタ・飲み口やシリコンパッキンは取り外し、フタはプラスチック資源（または可燃ごみ）、パッキンは可燃ごみへ分別してお出しください。"
                }

                return AnalysisOutput.Resolved(
                    SortingResult(
                        itemName = if (q.contains("ステンレス")) "ステンレス水筒・マグボトル" else "水筒（ステンレス・金属製）",
                        categoryName = metalCat.name,
                        categoryId = metalCat.id,
                        colorHex = metalCat.colorHex,
                        municipalityName = municipality.name,
                        nextDateText = nextInfo.dateText + nextInfo.dayOfWeekText,
                        daysRemainingText = nextInfo.daysRemainingText,
                        disposalAdvice = advice,
                        sizeMaterialNotes = "本体は金属製のため可燃ごみには出せません。分解可能な樹脂パーツは外して分別します。",
                        requiresReservation = false
                    )
                )
            }
        }

        // 0-E. Medicine Containers, Insect Repellent Bottles, Topical Lotion (虫さされ・かゆみ止め・外用薬・薬のボトル)
        if (q.contains("虫さされ") || q.contains("かゆみ止め") || q.contains("医薬品") ||
            q.contains("目薬") || q.contains("外用薬") || q.contains("塗り薬") ||
            (q.contains("薬") && (q.contains("容器") || q.contains("ボトル") || q.contains("瓶") || q.contains("びん")))
        ) {
            val isGlass = q.contains("瓶") || q.contains("びん") || q.contains("ガラス")
            if (isGlass) {
                val glassCat = municipality.categories.firstOrNull { it.id == "bottle_can" || it.id == "non_burnable" }
                    ?: MunicipalityData.CAT_NON_BURNABLE
                val sched = municipality.schedules.firstOrNull { it.categoryId == glassCat.id }
                val nextInfo = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()
                return AnalysisOutput.Resolved(
                    SortingResult(
                        itemName = "医薬品のガラス瓶・ドリンク剤容器",
                        categoryName = glassCat.name,
                        categoryId = glassCat.id,
                        colorHex = glassCat.colorHex,
                        municipalityName = municipality.name,
                        nextDateText = nextInfo.dateText + nextInfo.dayOfWeekText,
                        daysRemainingText = nextInfo.daysRemainingText,
                        disposalAdvice = "残った薬品は新聞紙等に吸わせて可燃ごみへ。ボトルは中を水ですすいで空き瓶・資源ごみへ。プラスチックキャップは外してプラ資源へ出してください。",
                        sizeMaterialNotes = "中身の残液は下水に流さず紙に吸わせてください。",
                        requiresReservation = false,
                        partsBreakdown = listOf(
                            com.example.data.model.WastePartItem("ガラス小瓶", "ガラス", glassCat.name, "水ですすいで空きびん・資源へ"),
                            com.example.data.model.WastePartItem("プラスチックキャップ", "プラスチック", "プラスチック資源", "外してプラ袋へ"),
                            com.example.data.model.WastePartItem("残った薬液", "薬品", "可燃ごみ", "紙に吸わせて可燃袋へ")
                        )
                    )
                )
            } else {
                // Plastic topical medicine bottle (e.g. anti-itch lotion, eye drops)
                val plasticCat = municipality.categories.firstOrNull { it.id == "plastic" }
                    ?: municipality.categories.firstOrNull { it.id == "burnable" }
                    ?: MunicipalityData.CAT_PLASTIC
                val sched = municipality.schedules.firstOrNull { it.categoryId == plasticCat.id }
                val nextInfo = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

                val advice = if (municipality.id == "aisai") {
                    "中身の残液はティッシュや古布に吸わせて【可燃ごみ】へ。プラスチック製ボトル本体・キャップは中を軽く水ですすいで【プラスチック類ごみ（毎週火曜・黄色指定袋）】へ出してください。スポンジ栓は外せれば可燃ごみへ、外れなければ本体ごとプラごみへ出せます。"
                } else {
                    "中身の残液は紙に吸わせて可燃ごみへ。プラスチック製容器本体とキャップは水ですすいで【${plasticCat.name}】へ出してください。スポンジ栓は外せれば可燃ごみへ。"
                }

                return AnalysisOutput.Resolved(
                    SortingResult(
                        itemName = if (q.contains("虫さされ") || q.contains("かゆみ止め")) "虫さされ・かゆみ止め外用薬ボトル" else "プラスチック製医薬品容器・目薬",
                        categoryName = plasticCat.name,
                        categoryId = plasticCat.id,
                        colorHex = plasticCat.colorHex,
                        municipalityName = municipality.name,
                        nextDateText = nextInfo.dateText + nextInfo.dayOfWeekText,
                        daysRemainingText = nextInfo.daysRemainingText,
                        disposalAdvice = advice,
                        sizeMaterialNotes = "外用薬などのプラスチック容器包装です。残った薬液は下水に流さず紙等に吸わせて処分します。",
                        requiresReservation = false,
                        partsBreakdown = listOf(
                            com.example.data.model.WastePartItem("ボトル本体・キャップ", "プラスチック(PE/PP)", plasticCat.name, "軽く水ですすいでプラスチック指定袋へ"),
                            com.example.data.model.WastePartItem("残った薬液（中身）", "外用液体薬", "可燃ごみ", "ティッシュや布に吸わせて可燃袋へ（流しに流さない）"),
                            com.example.data.model.WastePartItem("スポンジ・塗布部ヘッド", "ウレタンスポンジ/プラ", "可燃ごみ（外せる場合）", "外せれば可燃ごみへ。外れなければ本体ごとプラごみへ")
                        )
                    )
                )
            }
        }

        // 0-F. Umbrellas (傘・ビニール傘・折りたたみ傘)
        if (q.contains("傘") || q.contains("かさ") || q.contains("アンブレラ")) {
            val nonBurnableCat = municipality.categories.firstOrNull { it.id == "non_burnable" }
                ?: MunicipalityData.CAT_NON_BURNABLE
            val sched = municipality.schedules.firstOrNull { it.categoryId == nonBurnableCat.id }
            val nextInfo = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()
            return AnalysisOutput.Resolved(
                SortingResult(
                    itemName = if (q.contains("折りたたみ")) "折りたたみ傘" else "傘（ビニール傘・雨傘）",
                    categoryName = nonBurnableCat.name,
                    categoryId = nonBurnableCat.id,
                    colorHex = nonBurnableCat.colorHex,
                    municipalityName = municipality.name,
                    nextDateText = nextInfo.dateText + nextInfo.dayOfWeekText,
                    daysRemainingText = nextInfo.daysRemainingText,
                    disposalAdvice = "骨組みに金属が使用されているため【${nonBurnableCat.name}】です。布やビニール部分を骨組みから容易に外せる場合は、布・ビニールは可燃ごみ、骨組みは不燃ごみ（金属）へ分別してください（無理に外せない場合はそのまま不燃ごみへ）。",
                    sizeMaterialNotes = "${municipality.name}の傘・金属製品ルールに準拠しています。",
                    requiresReservation = false
                )
            )
        }

        // 1. Ambiguous Plastic Container / Case / Box (The user's explicit example)
        if (q.contains("プラスチック") || q.contains("ケース") || q.contains("プラ") || q.contains("衣装") || q.contains("タッパー") || q.contains("バケツ") || q.contains("箱")) {
            return AnalysisOutput.NeedsClarification(
                itemName = "プラスチックケース・容器",
                initialNote = "写真だけでは材質（硬質プラ・軟質プラ）やサイズが確定できないため、追加で確認します。",
                questions = listOf(
                    ClarificationQuestion(
                        id = "material",
                        title = "材質の確認",
                        questionText = "材質はどれに当てはまりますか？",
                        options = listOf(
                            QuestionOption("hard_plastic", "硬いプラ（収納ケース・日用品など）"),
                            QuestionOption("soft_plastic", "柔らかいプラ（プラマークあり包装など）"),
                            QuestionOption("metal_mix", "金属混在・その他")
                        )
                    ),
                    ClarificationQuestion(
                        id = "size",
                        title = "大きさの確認",
                        questionText = "一番長い辺の長さは？",
                        options = listOf(
                            QuestionOption("under_30", "< 30cm（30cm未満）"),
                            QuestionOption("30_to_50", "30〜50cm"),
                            QuestionOption("over_50", "> 50cm（50cm超）")
                        )
                    )
                )
            )
        }

        // 2. Frying pan / pots / pans
        if (q.contains("フライパン") || q.contains("鍋") || q.contains("やかん")) {
            return AnalysisOutput.NeedsClarification(
                itemName = "フライパン・調理器具",
                initialNote = "サイズや金属の割合によって、不燃ごみ・金属類か粗大ごみかに分かれます。",
                questions = listOf(
                    ClarificationQuestion(
                        id = "size",
                        title = "一番長い辺の長さ（取っ手含む）",
                        questionText = "取っ手を含めた最大の長さは？",
                        options = listOf(
                            QuestionOption("under_30", "30cm未満"),
                            QuestionOption("over_30", "30cm以上（大型・粗大ごみ）")
                        )
                    )
                )
            )
        }

        // 3. Spray cans / Cassette cylinders (Safety check)
        if (q.contains("スプレー") || q.contains("カセットボンベ") || q.contains("ガス缶")) {
            return AnalysisOutput.NeedsClarification(
                itemName = "スプレー缶・ガスボンベ",
                initialNote = "中身が残っていると収集車や処理施設での火災・爆発事故に繋がります。",
                questions = listOf(
                    ClarificationQuestion(
                        id = "empty_status",
                        title = "中身の確認",
                        questionText = "中身は完全に使い切りましたか？",
                        options = listOf(
                            QuestionOption("empty", "完全に使い切った（振って音がしない）"),
                            QuestionOption("has_content", "まだ中身が残っている")
                        )
                    )
                )
            )
        }

        // 4. Batteries
        if (q.contains("電池") || q.contains("モバイルバッテリー") || q.contains("リチウム")) {
            val category = municipality.categories.firstOrNull { it.id == "hazardous" }
                ?: MunicipalityData.CAT_HAZARDOUS
            val schedule = municipality.schedules.firstOrNull { it.categoryId == category.id }
            val nextInfo = schedule?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            return AnalysisOutput.Resolved(
                SortingResult(
                    itemName = "乾電池・バッテリー類",
                    categoryName = category.name,
                    categoryId = category.id,
                    colorHex = category.colorHex,
                    municipalityName = municipality.name,
                    nextDateText = nextInfo.dateText + nextInfo.dayOfWeekText,
                    daysRemainingText = nextInfo.daysRemainingText,
                    disposalAdvice = "【発火注意】モバイルバッテリー等のリチウムイオン電池は定期ごみに出せません。電器店やスーパーの回収BOXへお出しください。アルカリ乾電池は電極にセロハンテープを貼って絶縁してください。",
                    sizeMaterialNotes = "可燃・不燃ごみ袋に混入すると収集車の火災事故になります。",
                    requiresReservation = false
                )
            )
        }

        // 5. PET Bottles
        if (q.contains("ペットボトル") || q.contains("pet")) {
            val category = municipality.categories.firstOrNull { it.id == "bottle_can" }
                ?: MunicipalityData.CAT_BOTTLE_CAN
            val schedule = municipality.schedules.firstOrNull { it.categoryId == category.id }
            val nextInfo = schedule?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            return AnalysisOutput.Resolved(
                SortingResult(
                    itemName = "ペットボトル",
                    categoryName = category.name,
                    categoryId = category.id,
                    colorHex = category.colorHex,
                    municipalityName = municipality.name,
                    nextDateText = nextInfo.dateText + nextInfo.dayOfWeekText,
                    daysRemainingText = nextInfo.daysRemainingText,
                    disposalAdvice = "1. キャップとラベルを剥がす（プラスチック資源へ）\n2. 中をさっと水洗いする\n3. 軽くつぶして専用ネット・袋へ出す",
                    sizeMaterialNotes = "汚れやタバコの吸い殻等が入ったものはリサイクルできないため可燃ごみへ。",
                    requiresReservation = false
                )
            )
        }

        // 6. Cardboard / paper
        if (q.contains("段ボール") || q.contains("ダンボール") || q.contains("雑誌") || q.contains("新聞") || q.contains("紙")) {
            val category = municipality.categories.firstOrNull { it.id == "paper" }
                ?: MunicipalityData.CAT_PAPER
            val schedule = municipality.schedules.firstOrNull { it.categoryId == category.id }
            val nextInfo = schedule?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            return AnalysisOutput.Resolved(
                SortingResult(
                    itemName = "段ボール・古紙類",
                    categoryName = category.name,
                    categoryId = category.id,
                    colorHex = category.colorHex,
                    municipalityName = municipality.name,
                    nextDateText = nextInfo.dateText + nextInfo.dayOfWeekText,
                    daysRemainingText = nextInfo.daysRemainingText,
                    disposalAdvice = "品目別にビニール紐や紙紐で十字にしっかり束ねて出してください。粘着テープや配送伝票・留め金は取り除いてください。",
                    sizeMaterialNotes = "雨の日は集団回収で濡れると資源化できなくなるため、次回に出すか濡れない場所へ。",
                    requiresReservation = false
                )
            )
        }

        // Default: Burnable
        val burnableCat = municipality.categories.firstOrNull { it.id == "burnable" }
            ?: MunicipalityData.CAT_BURNABLE
        val schedule = municipality.schedules.firstOrNull { it.categoryId == burnableCat.id }
        val nextInfo = schedule?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

        return AnalysisOutput.Resolved(
            SortingResult(
                itemName = query.ifBlank { "日用品・一般廃棄物" },
                categoryName = burnableCat.name,
                categoryId = burnableCat.id,
                colorHex = burnableCat.colorHex,
                municipalityName = municipality.name,
                nextDateText = nextInfo.dateText + nextInfo.dayOfWeekText,
                daysRemainingText = nextInfo.daysRemainingText,
                disposalAdvice = "自治体指定の収集袋に入れ、収集日の朝8:30までに出してください。生ごみは十分に水切りを行ってください。",
                sizeMaterialNotes = "${municipality.name}の収集ルールに準拠しています。",
                requiresReservation = false
            )
        )
    }

    /**
     * Resolve after user answers clarification questions
     */
    fun resolveAnswers(
        itemName: String,
        municipality: Municipality,
        answers: Map<String, String>
    ): SortingResult {
        val material = answers["material"] ?: "hard_plastic"
        val size = answers["size"] ?: "under_30"
        val emptyStatus = answers["empty_status"]

        // Handling spray cans
        if (emptyStatus != null) {
            if (emptyStatus == "has_content") {
                return SortingResult(
                    itemName = itemName,
                    categoryName = "収集不可（中身あり）",
                    categoryId = "hazardous",
                    colorHex = 0xFFD32F2FL,
                    municipalityName = municipality.name,
                    nextDateText = "中身排出後に回収",
                    daysRemainingText = "注意！",
                    disposalAdvice = "中身が残ったスプレー缶は絶対にごみ収集に出さないでください。火気のない風通しの良い屋外でシューという音がしなくなるまで中身を出し切ってください。",
                    sizeMaterialNotes = "使い切った後は、${municipality.name}の有害ごみまたは不燃ごみへ。",
                    requiresReservation = false
                )
            } else {
                val cat = municipality.categories.firstOrNull { it.id == "hazardous" || it.id == "non_burnable" }
                    ?: MunicipalityData.CAT_HAZARDOUS
                val sched = municipality.schedules.firstOrNull { it.categoryId == cat.id }
                val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()
                return SortingResult(
                    itemName = itemName,
                    categoryName = cat.name,
                    categoryId = cat.id,
                    colorHex = cat.colorHex,
                    municipalityName = municipality.name,
                    nextDateText = next.dateText + next.dayOfWeekText,
                    daysRemainingText = next.daysRemainingText,
                    disposalAdvice = "中身を使い切ったスプレー缶は穴あけ不要で透明袋に入れ、収集日にお出しください。",
                    sizeMaterialNotes = "${municipality.name}の安全基準に準拠しています。",
                    requiresReservation = false
                )
            }
        }

        // Handling oversized threshold
        val isOversized = size == "30_to_50" || size == "over_50" || size == "over_30" || size == "over_threshold"

        if (isOversized) {
            val oversizedCat = municipality.categories.firstOrNull { it.id == "oversized" }
                ?: MunicipalityData.CAT_OVERSIZED
            val sched = municipality.schedules.firstOrNull { it.categoryId == oversizedCat.id }
            val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            val advice = if (municipality.id == "aisai") {
                "愛西市では市指定袋に入らないもの、または重すぎて袋が破れるものは『粗大ごみ』となります。\n【出し方1】集積場所持ち込み：第1水曜日（粗大ごみシール200円）\n【出し方2】戸別回収：第3水曜日（事前電話予約 0567-22-2480・シール500円）\n【出し方3】八穂クリーンセンターへ直接自己搬入（10kgあたり200円・市役所環境課で事前搬入指示書要）"
            } else {
                "${municipality.name}では一番長い辺が${municipality.oversizedThresholdCm}cmを超えるものは定期収集に出せず、『粗大ごみ』となります。\n事前に粗大ごみ受付センター（電話または公式ウェブサイト/LINE）へ予約し、コンビニ等で手数料納付券を購入して貼り付けて出してください。"
            }

            return SortingResult(
                itemName = "$itemName（大型・指定袋超過）",
                categoryName = oversizedCat.name,
                categoryId = oversizedCat.id,
                colorHex = oversizedCat.colorHex,
                municipalityName = municipality.name,
                nextDateText = next.dateText,
                daysRemainingText = next.dayOfWeekText,
                disposalAdvice = advice,
                sizeMaterialNotes = if (municipality.id == "aisai") "愛西市指定袋（可燃・プラ・不燃）に入らないものが粗大ごみです。" else "解体しても元の大きさで判断される自治体が多いためご注意ください。",
                requiresReservation = true
            )
        }

        // Metal / Stainless / Umbrella / Pot / Pan / Water bottle handling
        val isMetalItem = itemName.contains("ステンレス") || itemName.contains("水筒") ||
                itemName.contains("魔法瓶") || itemName.contains("まほうびん") || itemName.contains("タンブラー") ||
                itemName.contains("金属") || itemName.contains("フライパン") || itemName.contains("やかん") ||
                itemName.contains("鍋") || itemName.contains("アルミ") || itemName.contains("スチール") ||
                itemName.contains("金物") || itemName.contains("傘") || itemName.contains("包丁") ||
                material == "metal_mix" || material == "metal_or_mix" || material == "metal"

        val isPurePlasticWaterBottle = (itemName.contains("水筒") || itemName.contains("ボトル")) &&
                (itemName.contains("プラスチック") || itemName.contains("プラ製")) &&
                !itemName.contains("ステンレス") && !itemName.contains("金属")

        if (isMetalItem && !isPurePlasticWaterBottle) {
            val nonBurnableCat = municipality.categories.firstOrNull { it.id == "non_burnable" || it.id == "metal" }
                ?: MunicipalityData.CAT_NON_BURNABLE
            val sched = municipality.schedules.firstOrNull { it.categoryId == nonBurnableCat.id }
            val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            val advice = if (municipality.id == "aisai") {
                if (itemName.contains("水筒") || itemName.contains("ボトル") || itemName.contains("魔法瓶") || itemName.contains("タンブラー")) {
                    "ステンレス製水筒・金属ボトル本体は【不燃ごみ（指定不燃物袋）】です（第2水曜日）。\n※プラスチック製のキャップ・フタやシリコンゴムパッキンは外し、キャップは【プラスチック類ごみ（毎週火曜）】、パッキンは【可燃ごみ（月・木）】へ分別してください。"
                } else {
                    "金属が主材料または金属が混ざった製品は【不燃ごみ（指定不燃物袋）】です（第2水曜日）。\n※プラスチック製のフタやカバーなど簡単に外せる樹脂部分は外して【プラスチック類ごみ（毎週火曜）】へ、外せない場合はそのまま不燃ごみへお出しください。"
                }
            } else {
                "${municipality.name}では金属製・金属混在の製品本体は【${nonBurnableCat.name}】です。\n※簡単に取り外せるプラスチックパーツやパッキンはプラスチック資源または可燃ごみへ分別してください。"
            }

            return SortingResult(
                itemName = "$itemName（指定袋サイズ）",
                categoryName = nonBurnableCat.name,
                categoryId = nonBurnableCat.id,
                colorHex = nonBurnableCat.colorHex,
                municipalityName = municipality.name,
                nextDateText = next.dateText + next.dayOfWeekText,
                daysRemainingText = next.daysRemainingText,
                disposalAdvice = advice,
                sizeMaterialNotes = "本体は金属（不燃ごみ）です。取り外せる樹脂パーツは分別してお出しください。",
                requiresReservation = false
            )
        }

        // Small Appliances & Electronic Parts (基板・電子部品・扇風機・サーキュレーター・ドライヤー・炊飯器・小型家電)
        val isCircuitBoard = itemName.contains("基板") || itemName.contains("制御基板") ||
                itemName.contains("プリント基板") || itemName.contains("基盤") || itemName.contains("回路") ||
                itemName.contains("電子部品") || itemName.contains("半導体") || itemName.contains("コンデンサ") ||
                itemName.contains("マザーボード") || itemName.contains("IC") || itemName.contains("ヒートシンク")

        val isSmallAppliance = isCircuitBoard ||
                itemName.contains("扇風機") || itemName.contains("サーキュレーター") ||
                itemName.contains("ファン") || itemName.contains("ドライヤー") || itemName.contains("アイロン") ||
                itemName.contains("炊飯器") || itemName.contains("ケトル") || (itemName.contains("ポット") && !itemName.contains("植木")) ||
                itemName.contains("電子レンジ") || itemName.contains("トースター") || itemName.contains("掃除機") ||
                itemName.contains("ヒーター") || itemName.contains("ストーブ") || itemName.contains("加湿器") ||
                itemName.contains("空気清浄機") || itemName.contains("時計") || itemName.contains("ラジオ") ||
                itemName.contains("プリンター") || itemName.contains("パソコン") || itemName.contains("ゲーム") ||
                itemName.contains("家電") || itemName.contains("小型家電") || itemName.contains("電気") ||
                material == "appliance"

        if (isSmallAppliance) {
            val appCat = municipality.categories.firstOrNull { it.id == "small_appliance" }
                ?: municipality.categories.firstOrNull { it.id == "non_burnable" }
                ?: MunicipalityData.CAT_NON_BURNABLE
            val sched = municipality.schedules.firstOrNull { it.categoryId == appCat.id }
            val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            val advice = if (isCircuitBoard) {
                if (municipality.id == "aisai") {
                    "電子基板・制御基板などの電子部品は【プラスチックごみには絶対に出せません】。\n" +
                    "【出し方1（推奨）】市役所本庁舎や各支所（八開・立田・佐織）、中央公民館等の『使用済小型家電回収ボックス』（投入口 縦15cm×横30cm）へ投入してください（無料・レアメタルリサイクル）。\n" +
                    "【出し方2】ボックスに入らないものは、愛西市指定の『不燃物専用袋（黄色）』に入れて【不燃ごみ】（毎月第2水曜日）へお出しください。\n" +
                    "⚠️プラスチック資源に混入すると、リサイクル処理工場で破砕機や再生機器を破損させる重大な損害・事故の原因となります。"
                } else {
                    "${municipality.name}では電子基板・精密部品は【${appCat.name}】です。プラスチックごみには出せません。\n" +
                    "※市役所等の小型家電回収ボックスをご利用いただくか、指定の不燃ごみ袋でお出しください。"
                }
            } else if (municipality.id == "aisai") {
                "小型扇風機・サーキュレーター等の電化製品はプラスチックごみには出せません！\n" +
                "【出し方1】市役所・各支所等に設置の『小型家電回収ボックス』（投入口 縦15cm×横30cm以内）に入るものは無料回収。\n" +
                "【出し方2】ボックスに入らないものは愛西市指定の『不燃物専用袋』に入れて【不燃ごみ】（毎月第2水曜日）へ。\n" +
                "⚠️【重要：充電式・ハンディファンの場合】リチウムイオン電池内蔵のものは収集車や処理施設での火災事故防止のため、必ず電池を抜いて回収協力店（JBRC）へ。電池が外せないものは販売店や市役所環境課へご相談ください。"
            } else {
                "${municipality.name}では電化製品は【${appCat.name}】です。プラスチックごみには出せません。\n" +
                "※公共施設等の小型家電回収ボックスをご利用いただくか、指定の不燃ごみ袋でお出しください。\n" +
                "⚠️バッテリー内蔵製品は発火の危険があるため、必ず電池を取り外して危険ごみ・電池回収へ分別してください。"
            }

            return SortingResult(
                itemName = "$itemName（指定袋サイズ）",
                categoryName = appCat.name,
                categoryId = appCat.id,
                colorHex = appCat.colorHex,
                municipalityName = municipality.name,
                nextDateText = next.dateText + next.dayOfWeekText,
                daysRemainingText = next.daysRemainingText,
                disposalAdvice = advice,
                sizeMaterialNotes = if (isCircuitBoard) "都市鉱山（有用金属）リサイクル対象の電子部品です。プラスチック資源には出せません。" else "モーター・電子基板・配線を含む電化製品です。プラスチック資源には出せません。",
                requiresReservation = false
            )
        }

        // Hazardous / Batteries / Mercury / Gas
        val isHazardous = itemName.contains("電池") || itemName.contains("バッテリー") ||
                itemName.contains("リチウム") || itemName.contains("充電池") || itemName.contains("蛍光灯") ||
                itemName.contains("電球") || itemName.contains("水銀") || itemName.contains("ライター")
        if (isHazardous) {
            val hazCat = municipality.categories.firstOrNull { it.id == "hazardous" }
                ?: municipality.categories.firstOrNull { it.id == "non_burnable" }
                ?: MunicipalityData.CAT_HAZARDOUS
            val sched = municipality.schedules.firstOrNull { it.categoryId == hazCat.id }
            val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()
            return SortingResult(
                itemName = itemName,
                categoryName = hazCat.name,
                categoryId = hazCat.id,
                colorHex = hazCat.colorHex,
                municipalityName = municipality.name,
                nextDateText = next.dateText + next.dayOfWeekText,
                daysRemainingText = next.daysRemainingText,
                disposalAdvice = "発火や有害物質漏洩の恐れがある危険物です。電極にテープを貼り、指定の危険ごみ回収日または公共施設の回収拠点へお持ちください。",
                sizeMaterialNotes = "安全のため他のごみと混ぜずに分別してください。",
                requiresReservation = false
            )
        }

        // Fabric / Clothes / Shoes / Leather
        val isFabricOrClothing = itemName.contains("服") || itemName.contains("衣類") ||
                itemName.contains("靴") || itemName.contains("シューズ") || itemName.contains("スニーカー") ||
                itemName.contains("バッグ") || itemName.contains("かばん") || itemName.contains("カバン") ||
                itemName.contains("鞄") || itemName.contains("財布") || itemName.contains("ベルト") ||
                itemName.contains("革") || itemName.contains("布") || itemName.contains("クッション") ||
                itemName.contains("布団") || itemName.contains("毛布") || itemName.contains("カーテン") ||
                itemName.contains("ぬいぐるみ") || itemName.contains("タオル")
        if (isFabricOrClothing) {
            val burnableCat = municipality.categories.firstOrNull { it.id == "burnable" }
                ?: MunicipalityData.CAT_BURNABLE
            val sched = municipality.schedules.firstOrNull { it.categoryId == burnableCat.id }
            val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()
            return SortingResult(
                itemName = "$itemName（指定袋サイズ）",
                categoryName = burnableCat.name,
                categoryId = burnableCat.id,
                colorHex = burnableCat.colorHex,
                municipalityName = municipality.name,
                nextDateText = next.dateText + next.dayOfWeekText,
                daysRemainingText = next.daysRemainingText,
                disposalAdvice = "指定の可燃ごみ袋に入れて口をしっかりしばり、収集日の朝にお出しください。※金属の金具やチャックは外せる範囲で外してください。",
                sizeMaterialNotes = "布・皮革製品は指定袋に入れば可燃ごみとして収集されます。",
                requiresReservation = false
            )
        }

        // Ceramics / Glass / Tableware
        val isCeramicOrGlass = itemName.contains("皿") || itemName.contains("茶碗") ||
                itemName.contains("コップ") || itemName.contains("陶器") || itemName.contains("せともの") ||
                itemName.contains("ガラス") || itemName.contains("割れ物") || itemName.contains("花瓶")
        if (isCeramicOrGlass) {
            val nonBurnableCat = municipality.categories.firstOrNull { it.id == "non_burnable" }
                ?: MunicipalityData.CAT_NON_BURNABLE
            val sched = municipality.schedules.firstOrNull { it.categoryId == nonBurnableCat.id }
            val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()
            return SortingResult(
                itemName = "$itemName（指定袋サイズ）",
                categoryName = nonBurnableCat.name,
                categoryId = nonBurnableCat.id,
                colorHex = nonBurnableCat.colorHex,
                municipalityName = municipality.name,
                nextDateText = next.dateText + next.dayOfWeekText,
                daysRemainingText = next.daysRemainingText,
                disposalAdvice = "割れている場合や割れる危険がある場合は、厚紙や新聞紙に包んで「キケン」または「割れ物」と表記の上、指定不燃ごみ袋に入れてお出しください。",
                sizeMaterialNotes = "陶器・磁器・ガラス製品は不燃ごみとなります。",
                requiresReservation = false
            )
        }

        // Verify if item is actually plastic before routing to plastic recycling rules
        val isConfirmedPlastic = itemName.contains("プラスチック") || itemName.contains("プラ") ||
                itemName.contains("タッパー") || itemName.contains("ケース") || itemName.contains("衣装") ||
                itemName.contains("バケツ") || itemName.contains("ポリ") || itemName.contains("レジ袋") ||
                itemName.contains("トレイ") || itemName.contains("パック") || itemName.contains("ボトル") ||
                itemName.contains("ラップ") || itemName.contains("シート") || itemName.contains("ストロー") ||
                itemName.contains("ハンガー") || itemName.contains("スポンジ") ||
                (answers.containsKey("material") && (material == "hard_plastic" || material == "soft_plastic"))

        if (!isConfirmedPlastic) {
            val nonBurnableCat = municipality.categories.firstOrNull { it.id == "non_burnable" }
                ?: MunicipalityData.CAT_NON_BURNABLE
            val sched = municipality.schedules.firstOrNull { it.categoryId == nonBurnableCat.id }
            val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()
            return SortingResult(
                itemName = "$itemName（指定袋サイズ）",
                categoryName = nonBurnableCat.name,
                categoryId = nonBurnableCat.id,
                colorHex = nonBurnableCat.colorHex,
                municipalityName = municipality.name,
                nextDateText = next.dateText + next.dayOfWeekText,
                daysRemainingText = next.daysRemainingText,
                disposalAdvice = "金属やプラスチック・複合素材等の製品は指定の不燃ごみ袋に入れて、収集日の朝にお出しください。",
                sizeMaterialNotes = "${municipality.name}の収集基準に準拠しています。プラスチック資源ではありません。",
                requiresReservation = false
            )
        }

        // Under threshold: Municipal differences for confirmed plastic items!
        if (municipality.id == "aisai") {
            // Aisai City: Plastic goes to designated plastic bag on Tuesdays
            val cat = municipality.categories.firstOrNull { it.id == "plastic" }
                ?: MunicipalityData.CAT_PLASTIC
            val sched = municipality.schedules.firstOrNull { it.categoryId == cat.id }
            val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            val advice = "愛西市では、市指定の『プラスチック類ごみ専用袋』に入れて毎週火曜日の朝にお出しください。\n" +
                    "【対象】菓子・パン袋、レジ袋、洗剤・シャンプーの柔らかい容器、トレイ、パック類、合成樹脂製品（タッパー等）。\n" +
                    "※中身を空にして汚れを軽く水洗いしてください。\n" +
                    "※白色の発泡スチロールトレイは資源ごみ（第2・第4水曜）へお出しください。"

            return SortingResult(
                itemName = "$itemName（指定袋サイズ）",
                categoryName = cat.name,
                categoryId = cat.id,
                colorHex = cat.colorHex,
                municipalityName = municipality.name,
                nextDateText = next.dateText + next.dayOfWeekText,
                daysRemainingText = next.daysRemainingText,
                disposalAdvice = advice,
                sizeMaterialNotes = "愛西市ルール：市指定プラスチック専用袋を使用（毎週火曜日）。白トレイは資源ごみ。",
                requiresReservation = false
            )
        } else if (municipality.id == "nagoya") {
            // Nagoya allows 100% plastic products in plastic resource if < 30cm!
            val cat = municipality.categories.firstOrNull { it.id == "plastic" }
                ?: MunicipalityData.CAT_PLASTIC
            val sched = municipality.schedules.firstOrNull { it.categoryId == cat.id }
            val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            val advice = if (material == "hard_plastic") {
                "名古屋市では、30cm角未満であれば100%プラスチック製の製品（収納ケース・タッパー・バケツ・ハンガーなど）も『プラスチック資源』として収集されます！汚れを落として中身の見える透明・半透明袋で出してください。"
            } else {
                "プラマークのついた容器包装プラスチックとして、水洗いして汚れを落としてから透明・半透明袋に入れて出してください。"
            }

            return SortingResult(
                itemName = "$itemName（30cm未満）",
                categoryName = cat.name,
                categoryId = cat.id,
                colorHex = cat.colorHex,
                municipalityName = municipality.name,
                nextDateText = next.dateText + next.dayOfWeekText,
                daysRemainingText = next.daysRemainingText,
                disposalAdvice = advice,
                sizeMaterialNotes = "名古屋市独自ルール：製品プラスチックも30cm未満なら資源化対象です。",
                requiresReservation = false
            )
        } else if (municipality.id == "yokohama") {
            // Yokohama: Non-packaging product plastics go to Burnable (燃やすごみ)
            val cat = if (material == "hard_plastic") {
                municipality.categories.firstOrNull { it.id == "burnable" } ?: MunicipalityData.CAT_BURNABLE
            } else {
                municipality.categories.firstOrNull { it.id == "plastic" } ?: MunicipalityData.CAT_PLASTIC
            }
            val sched = municipality.schedules.firstOrNull { it.categoryId == cat.id }
            val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            val advice = if (material == "hard_plastic") {
                "横浜市ではプラマークのない製品プラスチック（タッパー・ケース類）は30cm未満であれば『燃やすごみ』になります。"
            } else {
                "プラマークのある容器包装プラスチックとして、汚れをすすぎ『プラスチック製容器包装』に出してください。"
            }

            return SortingResult(
                itemName = "$itemName（30cm未満）",
                categoryName = cat.name,
                categoryId = cat.id,
                colorHex = cat.colorHex,
                municipalityName = municipality.name,
                nextDateText = next.dateText + next.dayOfWeekText,
                daysRemainingText = next.daysRemainingText,
                disposalAdvice = advice,
                sizeMaterialNotes = "横浜市ルール：製品プラスチックは燃やすごみ、プラマーク容器は資源分別です。",
                requiresReservation = false
            )
        } else {
            // Other municipalities default
            val cat = municipality.categories.firstOrNull { it.id == "plastic" }
                ?: MunicipalityData.CAT_PLASTIC
            val sched = municipality.schedules.firstOrNull { it.categoryId == cat.id }
            val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

            return SortingResult(
                itemName = "$itemName（30cm未満）",
                categoryName = cat.name,
                categoryId = cat.id,
                colorHex = cat.colorHex,
                municipalityName = municipality.name,
                nextDateText = next.dateText + next.dayOfWeekText,
                daysRemainingText = next.daysRemainingText,
                disposalAdvice = "汚れを洗い流し、透明または中身の見える半透明袋に入れて指定日にお出しください。",
                sizeMaterialNotes = "${municipality.name}の分別ガイドラインに適合しています。",
                requiresReservation = false
            )
        }
    }

    private fun parseAiResponse(jsonText: String, municipality: Municipality): AnalysisOutput? {
        try {
            val root = JSONObject(jsonText)
            val itemName = root.optString("itemName", "検出品目")
            val isAmbiguous = root.optBoolean("isAmbiguous", false)

            if (isAmbiguous) {
                val questionsArray = root.optJSONArray("questions")
                val questions = mutableListOf<ClarificationQuestion>()

                if (questionsArray != null && questionsArray.length() > 0) {
                    for (i in 0 until questionsArray.length()) {
                        val qObj = questionsArray.getJSONObject(i)
                        val qId = qObj.optString("id", "q$i")
                        val title = qObj.optString("title", "追加確認")
                        val questionText = qObj.optString("question", "確認事項があります")
                        val optsArray = qObj.optJSONArray("options")
                        val options = mutableListOf<QuestionOption>()
                        if (optsArray != null) {
                            for (j in 0 until optsArray.length()) {
                                val optObj = optsArray.getJSONObject(j)
                                options.add(
                                    QuestionOption(
                                        id = optObj.optString("id", "opt_$j"),
                                        label = optObj.optString("label", "選択肢")
                                    )
                                )
                            }
                        }
                        if (options.isNotEmpty()) {
                            questions.add(ClarificationQuestion(qId, title, questionText, options))
                        }
                    }
                }

                if (questions.isEmpty()) {
                    // Default questions
                    questions.add(
                        ClarificationQuestion(
                            id = "material",
                            title = "材質の確認",
                            questionText = "材質はどちらに近いですか？",
                            options = listOf(
                                QuestionOption("hard_plastic", "硬いプラ"),
                                QuestionOption("soft_plastic", "柔らかいプラ")
                            )
                        )
                    )
                    questions.add(
                        ClarificationQuestion(
                            id = "size",
                            title = "大きさの確認",
                            questionText = "一番長い辺の長さは？",
                            options = listOf(
                                QuestionOption("under_30", "< 30cm"),
                                QuestionOption("30_to_50", "30〜50cm"),
                                QuestionOption("over_50", "> 50cm")
                            )
                        )
                    )
                }

                return AnalysisOutput.NeedsClarification(
                    itemName = itemName,
                    initialNote = root.optString("reason", "材質や大きさによって自治体の分別区分が分かれます。"),
                    questions = questions
                )
            } else {
                val categoryHint = root.optString("categoryHint", "可燃ごみ")
                val advice = root.optString("disposalAdvice", "自治体の指定袋に入れて出してください。")
                val reason = root.optString("reason", "")

                // Parse detected texts from vision AI
                val detectedTexts = mutableListOf<String>()
                val detectedArray = root.optJSONArray("detectedTexts")
                if (detectedArray != null) {
                    for (i in 0 until detectedArray.length()) {
                        val txt = detectedArray.optString(i).trim()
                        if (txt.isNotBlank()) detectedTexts.add(txt)
                    }
                }

                // Parse composite parts breakdown
                val partsBreakdown = mutableListOf<com.example.data.model.WastePartItem>()
                val partsArray = root.optJSONArray("partsBreakdown")
                if (partsArray != null) {
                    for (i in 0 until partsArray.length()) {
                        val partObj = partsArray.optJSONObject(i) ?: continue
                        val pName = partObj.optString("partName")
                        val pMat = partObj.optString("material")
                        val pCat = partObj.optString("categoryName")
                        val pMethod = partObj.optString("disposalMethod")
                        if (pName.isNotBlank() && pMethod.isNotBlank()) {
                            partsBreakdown.add(
                                com.example.data.model.WastePartItem(
                                    partName = pName,
                                    material = pMat,
                                    categoryName = pCat,
                                    disposalMethod = pMethod
                                )
                            )
                        }
                    }
                }

                // Smart Category Resolution with complete synonym mapping (Never fall back to burnable for plastics or metals!)
                val matchedCat = matchCategorySmartly(categoryHint, itemName, reason, advice, municipality)

                val sched = municipality.schedules.firstOrNull { it.categoryId == matchedCat.id }
                val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

                val nextDateText = if (matchedCat.id == "small_appliance") {
                    "市役所・量販店等の回収BOX（開館中随時）"
                } else {
                    next.dateText + next.dayOfWeekText
                }
                val daysRemainingText = if (matchedCat.id == "small_appliance") "常時持込可" else next.daysRemainingText

                val finalAdvice = if (matchedCat.id == "small_appliance" && !advice.contains("回収ボックス")) {
                    "【集積所排出禁止・発火注意】個人情報・データを初期化・消去し、市役所や公民館、家電量販店等の小型家電回収ボックスへ直接投入してください（袋不要・無料）。\n$advice"
                } else {
                    advice
                }

                val score = root.optInt("confidenceScore", 92).coerceIn(50, 99)

                return AnalysisOutput.Resolved(
                    SortingResult(
                        itemName = itemName,
                        categoryName = matchedCat.name,
                        categoryId = matchedCat.id,
                        colorHex = matchedCat.colorHex,
                        municipalityName = municipality.name,
                        nextDateText = nextDateText,
                        daysRemainingText = daysRemainingText,
                        disposalAdvice = finalAdvice,
                        sizeMaterialNotes = reason,
                        requiresReservation = matchedCat.id == "oversized",
                        isConfidenceHigh = score >= 80,
                        confidenceScore = score,
                        partsBreakdown = partsBreakdown,
                        detectedTexts = detectedTexts
                    )
                )
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun matchCategorySmartly(
        categoryHint: String,
        itemName: String,
        reason: String,
        advice: String,
        municipality: Municipality
    ): com.example.data.model.WasteCategory {
        val fullText = "$categoryHint $itemName $reason $advice".lowercase()

        // 1. 小型家電・バッテリー内蔵機器
        val isCircuitBoard = fullText.contains("基板") || fullText.contains("制御基板") ||
                fullText.contains("回路") || fullText.contains("電子部品") || fullText.contains("マザーボード")
        val isSmallAppliance = isCircuitBoard ||
                fullText.contains("小型家電") || fullText.contains("回収ボックス") || fullText.contains("回収box") ||
                itemName.contains("スマホ") || itemName.contains("スマートフォン") ||
                itemName.contains("携帯") || itemName.contains("タブレット") ||
                itemName.contains("充電器") || itemName.contains("スマートウォッチ") ||
                itemName.contains("電子辞書") || itemName.contains("扇風機") ||
                itemName.contains("ドライヤー") || itemName.contains("アイロン")

        if (isSmallAppliance) {
            val cat = municipality.categories.firstOrNull { it.id == "small_appliance" }
            if (cat != null) return cat
        }

        // 2. 有害・危険物（スプレー缶、カセットボンベ、乾電池等）
        val isHazardous = itemName.contains("スプレー缶") || itemName.contains("カセットボンベ") ||
                itemName.contains("エアゾール") || itemName.contains("ガスボンベ") ||
                itemName.contains("乾電池") || itemName.contains("ボタン電池") ||
                itemName.contains("蛍光管") || itemName.contains("ライター") ||
                fullText.contains("危険物") || fullText.contains("有害") || fullText.contains("ガス抜き")

        if (isHazardous) {
            val cat = municipality.categories.firstOrNull { it.id == "hazardous" || it.id == "non_burnable" }
            if (cat != null) return cat
        }

        // 3. 不燃ごみ・金属類（ステンレス水筒、鍋、フライパン、陶器、ガラス、傘、包丁）
        val isPurePlasticWaterBottle = (itemName.contains("水筒") || itemName.contains("ボトル")) &&
                (itemName.contains("プラスチック") || itemName.contains("プラ製")) &&
                !itemName.contains("ステンレス") && !itemName.contains("金属")

        val isMetalOrCeramic = (itemName.contains("ステンレス水筒") || itemName.contains("水筒") ||
                itemName.contains("魔法瓶") || itemName.contains("まほうびん") || itemName.contains("タンブラー") ||
                (itemName.contains("ステンレス") && !itemName.contains("たわし")) ||
                itemName.contains("フライパン") || itemName.contains("やかん") || itemName.contains("鍋") ||
                itemName.contains("金物") || itemName.contains("アルミ") || itemName.contains("スチール") ||
                itemName.contains("傘") || itemName.contains("包丁") || itemName.contains("ハサミ") ||
                categoryHint.contains("不燃") || categoryHint.contains("金属") || categoryHint.contains("金物") ||
                categoryHint.contains("陶器") || categoryHint.contains("ガラス") || categoryHint.contains("燃えない")) &&
                !isPurePlasticWaterBottle

        if (isMetalOrCeramic) {
            val cat = municipality.categories.firstOrNull { it.id == "non_burnable" || it.id == "metal" }
            if (cat != null) return cat
        }

        // 4. ペットボトル（飲料用PETボトル）
        val isPetBottle = (itemName.contains("ペットボトル") || fullText.contains("ペットボトル") || categoryHint.contains("pet")) &&
                !itemName.contains("キャップ") && !itemName.contains("ラベル")
        if (isPetBottle) {
            val cat = municipality.categories.firstOrNull { it.id == "bottle_can" || it.id == "pet" || it.id == "plastic" }
            if (cat != null) return cat
        }

        // 5. 空き缶・空き瓶
        val isCanOrBottle = fullText.contains("空き缶") || fullText.contains("空き瓶") ||
                fullText.contains("空きビン") || fullText.contains("飲料缶") ||
                (fullText.contains("びん") && !fullText.contains("魔法瓶"))
        if (isCanOrBottle) {
            val cat = municipality.categories.firstOrNull { it.id == "bottle_can" || it.id == "non_burnable" }
            if (cat != null) return cat
        }

        // 6. 資源古紙
        val isPaper = fullText.contains("古紙") || fullText.contains("段ボール") ||
                fullText.contains("新聞") || fullText.contains("雑誌") || fullText.contains("紙パック")
        if (isPaper) {
            val cat = municipality.categories.firstOrNull { it.id == "paper" }
            if (cat != null) return cat
        }

        // 7. 粗大ごみ
        val isOversized = fullText.contains("粗大") || fullText.contains("大型ごみ")
        if (isOversized) {
            val cat = municipality.categories.firstOrNull { it.id == "oversized" }
            if (cat != null) return cat
        }

        // 8. ★最重要★ プラスチック資源 / プラスチック類ごみ（シノニム完全網羅）
        val isPlastic = categoryHint.contains("プラ") || categoryHint.contains("プラスチック") ||
                categoryHint.contains("容器包装") || categoryHint.contains("合成樹脂") ||
                itemName.contains("プラスチック") || itemName.contains("プラ製") ||
                itemName.contains("ポリ") || itemName.contains("ビニール") ||
                itemName.contains("タッパー") || itemName.contains("トレイ") ||
                reason.contains("プラスチック") || reason.contains("プラマーク") || reason.contains("容器包装")

        if (isPlastic) {
            val cat = municipality.categories.firstOrNull { it.id == "plastic" }
            if (cat != null) return cat
        }

        // 9. 一般のマッチング（categoryHint との文字列比較）
        val genericMatch = municipality.categories.firstOrNull {
            categoryHint.contains(it.shortName) || it.name.contains(categoryHint) ||
                    categoryHint.contains(it.id)
        }
        if (genericMatch != null) return genericMatch

        // 10. 可燃ごみ（フォールバック）
        return municipality.categories.firstOrNull { it.id == "burnable" } ?: MunicipalityData.CAT_BURNABLE
    }

    /**
     * Create or correct sorting result with user-specified category and item name
     */
    fun createCustomSortingResult(
        itemName: String,
        categoryId: String,
        municipality: Municipality,
        customAdvice: String? = null
    ): SortingResult {
        val category = municipality.categories.firstOrNull { it.id == categoryId }
            ?: municipality.categories.firstOrNull()
            ?: MunicipalityData.CAT_BURNABLE

        val schedule = municipality.schedules.firstOrNull { it.categoryId == category.id }
        val next = schedule?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

        val nextDateText = if (category.id == "small_appliance") {
            "市役所・量販店等の回収BOX（開館中随時）"
        } else {
            next.dateText + next.dayOfWeekText
        }
        val daysRemainingText = if (category.id == "small_appliance") "常時持込可" else next.daysRemainingText

        return SortingResult(
            itemName = itemName.trim().ifBlank { "指定品目" },
            categoryName = category.name,
            categoryId = category.id,
            colorHex = category.colorHex,
            municipalityName = municipality.name,
            nextDateText = nextDateText,
            daysRemainingText = daysRemainingText,
            disposalAdvice = customAdvice ?: "${category.name}として自治体指定の袋または回収場所へ出してください。",
            sizeMaterialNotes = "ユーザーによる訂正・手動指定の分別ルール",
            requiresReservation = category.id == "oversized"
        )
    }
}
