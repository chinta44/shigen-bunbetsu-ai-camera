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

        // Under threshold: Municipal differences!
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

                val matchedCat = municipality.categories.firstOrNull {
                    categoryHint.contains(it.shortName) || it.name.contains(categoryHint)
                } ?: municipality.categories.firstOrNull { it.id == "burnable" } ?: MunicipalityData.CAT_BURNABLE

                val sched = municipality.schedules.firstOrNull { it.categoryId == matchedCat.id }
                val next = sched?.getNextCollectionDate() ?: municipality.schedules.first().getNextCollectionDate()

                return AnalysisOutput.Resolved(
                    SortingResult(
                        itemName = itemName,
                        categoryName = matchedCat.name,
                        categoryId = matchedCat.id,
                        colorHex = matchedCat.colorHex,
                        municipalityName = municipality.name,
                        nextDateText = next.dateText + next.dayOfWeekText,
                        daysRemainingText = next.daysRemainingText,
                        disposalAdvice = advice,
                        sizeMaterialNotes = reason,
                        requiresReservation = matchedCat.id == "oversized"
                    )
                )
            }
        } catch (e: Exception) {
            return null
        }
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

        return SortingResult(
            itemName = itemName.trim().ifBlank { "指定品目" },
            categoryName = category.name,
            categoryId = category.id,
            colorHex = category.colorHex,
            municipalityName = municipality.name,
            nextDateText = next.dateText + next.dayOfWeekText,
            daysRemainingText = next.daysRemainingText,
            disposalAdvice = customAdvice ?: "${category.name}として自治体指定の袋または回収場所へ出してください。",
            sizeMaterialNotes = "ユーザーによる訂正・手動指定の分別ルール",
            requiresReservation = category.id == "oversized"
        )
    }
}
