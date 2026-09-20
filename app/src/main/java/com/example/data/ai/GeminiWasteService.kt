package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiWasteService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun getPresetApiKey(): String {
        return try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val field = buildConfigClass.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    suspend fun analyzeWasteImage(
        bitmap: Bitmap,
        municipalityName: String,
        prefectureName: String,
        oversizedThresholdCm: Int,
        customApiKey: String? = null,
        targetItemHint: String? = null
    ): WasteAiAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey.trim() else getPresetApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // No API key provided, signal to fallback
            return@withContext WasteAiAnalysisResult.KeyMissing
        }

        try {
            // Compress bitmap to 1280px dimension to ensure Japanese text and labels are sharp and legible
            val scaledBitmap = scaleBitmapIfNeeded(bitmap, 1280)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val targetFocusInstruction = if (!targetItemHint.isNullOrBlank()) {
                "\n【最重要指示】画面内に複数の物体が写っている場合、ユーザーが指定した対象「$targetItemHint」または指定枠内の物体を特定・分別してください。他の物や背景は無視してください。"
            } else {
                "\n画面内に複数の物体が写っている場合は、中央または最も手前にある代表的な不用品1点を特定してください。"
            }

            val prompt = """
                あなたは日本の自治体ゴミ分別ルールに精通した最高精度の鑑定専門家AIです。
                ユーザーは「$prefectureName $municipalityName」のゴミ分別ルールに従って正確に不用品を分別したいと考えています。
                （この自治体の粗大ごみ基準は一辺 $oversizedThresholdCm cm以上です）

                $targetFocusInstruction

                【絶対遵守：多段階推論（Chain-of-Thought）プロセス】
                画像全体のシルエットだけで早とちりせず、必ず以下の思考手順を踏んで判定してください：

                ■ STEP 1: 画像内の文字・商品ラベル・ロゴの徹底読取（最優先OCR）
                - パッケージや本体に印字された文字（例: 「虫さされ」「かゆみに」「第2類医薬品」「化粧水」「シャンプー」「マヨネーズ」「スプレー缶」「PET」など）を漏らさず書き出してください。
                - 【超重要警告】画像内に文字が存在する場合、ボトルのシルエットだけで「飲料ボトル」等と誤認・早とちりすることは厳禁です。文字情報（商品名・医薬品区分・用途）を最優先の根拠として品目を特定してください。

                ■ STEP 2: 商品ジャンルと用途の確定
                - 医薬品（外用液・軟膏・目薬等）、化粧品、食品、洗剤、日用品、小型家電、文具等のジャンルを正確に特定します。

                ■ STEP 3: 構成パーツ・素材の分解（Parts Breakdown）
                - 複合製品はパーツに分解してください：
                  1. 本体容器（素材: プラスチック、金属、ガラス、陶器等）
                  2. キャップ・フタ（素材: プラスチック、金属等）
                  3. 中栓・ノズル・塗布部（スポンジ、ポンプ、パッキン等）
                  4. 残った中身（薬品、油、飲料、ガス等）

                ■ STEP 4: 自治体の公式分別ルールの適用
                  - 医薬品残液：下水に流さず、不要な布や紙に吸わせて「可燃ごみ」
                  - プラスチック製外用薬・シャンプー容器（プラマーク有）：水洗いして「プラスチック資源」
                  - スポンジ塗布ヘッド：外せれば「可燃ごみ」、外れなければ本体ごと「プラスチック資源」
                  - ステンレス水筒・金属ボトル：本体は「不燃ごみ・金属類」、プラ蓋・パッキンは外して「プラスチック資源」
                  - スプレー缶・カセットボンベ：中身使い切り、火気のない屋外でガス抜きし、穴あけ不要で「有害危険物」または「不燃ごみ」

                もし材質や大きさによって自治体の分別区分が分かれる場合は「isAmbiguous: true」とし、ユーザーに尋ねるべき追加質問を1〜2個生成してください。

                必ず以下のJSONフォーマットのみで回答してください：
                {
                  "detectedTexts": ["画像から読み取った文字1", "文字2（商品名・用途など）"],
                  "itemName": "判定した正確な品名（例：虫さされ・かゆみ止め外用薬ボトル、ステンレス水筒、プラスチックケース）",
                  "confidenceScore": 95,
                  "isAmbiguous": false,
                  "categoryHint": "プラスチック資源 または 不燃ごみ または 可燃ごみ または 粗大ごみ または 有害危険物 または ペットボトル または 空き缶・空き瓶 または 小型家電",
                  "reason": "読み取った文字と主たる材質に基づく判定根拠（例：ラベルの『虫さされ・かゆみに』の印字から外用医薬品容器と特定。主たる素材はプラスチック製容器包装です）",
                  "disposalAdvice": "捨て方の具体的な手順（例: 中身の残液は紙に吸わせて可燃ごみへ。ボトル本体とキャップは軽く水ですすいでプラスチック資源へ。スポンジ栓は外せれば可燃ごみへ）",
                  "partsBreakdown": [
                    {
                      "partName": "ボトル本体・キャップ",
                      "material": "プラスチック",
                      "categoryName": "プラスチック資源",
                      "disposalMethod": "軽く水ですすいで指定プラ袋へ"
                    },
                    {
                      "partName": "残った薬液・中身",
                      "material": "液体（医薬品）",
                      "categoryName": "可燃ごみ",
                      "disposalMethod": "不要な紙や布に吸わせて可燃袋へ（流しに流さない）"
                    },
                    {
                      "partName": "スポンジ塗布部",
                      "material": "ウレタン/プラ",
                      "categoryName": "可燃ごみ（外せる場合）",
                      "disposalMethod": "外せれば可燃ごみへ、外れなければ本体と一緒にプラ資源へ"
                    }
                  ],
                  "questions": [
                    {
                      "id": "material",
                      "title": "材質の確認",
                      "question": "材質・素材はどれに当てはまりますか？",
                      "options": [
                        {"id": "hard_plastic", "label": "硬いプラ（タッパー・容器等）"},
                        {"id": "soft_plastic", "label": "柔らかいプラ（包装用プラマーク等）"},
                        {"id": "metal_or_mix", "label": "金属製（ステンレス等）"}
                      ]
                    }
                  ]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            
            // Primary model: gemini-3.8-flash (with seamless fallback to gemini-2.5-flash if 3.8 is not yet accessible in the key's tier)
            val primaryUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=$apiKey"
            val fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val primaryRequest = Request.Builder()
                .url(primaryUrl)
                .post(requestBody)
                .build()

            var response = client.newCall(primaryRequest).execute()
            if (!response.isSuccessful && (response.code == 404 || response.code == 400)) {
                // If gemini-3.8-flash returns 404 or model not found on user API key tier, gracefully try gemini-2.5-flash
                response.close()
                val fallbackRequest = Request.Builder()
                    .url(fallbackUrl)
                    .post(requestBody)
                    .build()
                response = client.newCall(fallbackRequest).execute()
            }

            if (!response.isSuccessful) {
                return@withContext WasteAiAnalysisResult.Error("APIエラー: ${response.code}")
            }

            val responseBody = response.body?.string().orEmpty()
            val jsonRoot = JSONObject(responseBody)
            val candidates = jsonRoot.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val textContent = firstCandidate?.optJSONObject("content")
                ?.optJSONArray("parts")?.optJSONObject(0)
                ?.optString("text").orEmpty()

            if (textContent.isBlank()) {
                return@withContext WasteAiAnalysisResult.Error("回答を取得できませんでした")
            }

            WasteAiAnalysisResult.Success(textContent)
        } catch (e: Exception) {
            WasteAiAnalysisResult.Error(e.message ?: "通信エラー")
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}

sealed class WasteAiAnalysisResult {
    data class Success(val jsonText: String) : WasteAiAnalysisResult()
    data class Error(val message: String) : WasteAiAnalysisResult()
    object KeyMissing : WasteAiAnalysisResult()
}
