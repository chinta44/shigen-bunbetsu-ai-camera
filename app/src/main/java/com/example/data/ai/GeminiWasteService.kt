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
                "\n【最重要指示：特定被写体のピンポイント指定】\nユーザーが写真内の特定の対象物体（または写真上の位置をタップ指定）を指定しています：「$targetItemHint」。\n写真の中に複数の物体（例：別の場所にあるトイレットペーパー、ケーブル、容器、家電など）が写っている場合であっても、必ずこの指定された位置・物体のみに100%焦点を絞って品名と分別ルールを鑑定してください。指定外の物体や周囲の物は一切無視して回答してください。"
            } else {
                "\n画面内に複数の物体が写っている場合は、中央または最も手前にある代表的な不用品1点を特定してください。"
            }

            val prompt = """
                あなたは日本の自治体ゴミ分別ルールに精通した最高精度の鑑定専門家AIです。
                ユーザーは「$prefectureName $municipalityName」のゴミ分別ルールに従って正確に不用品を分別したいと考えています。
                （この自治体の粗大ごみ基準は一辺 $oversizedThresholdCm cm以上です）

                $targetFocusInstruction

                【絶対遵守：多段階推論（Chain-of-Thought）プロセス】
                特定のジャンル（ボトルや容器など）に偏らず、あらゆる家庭不用品（履物・靴、衣類、日用品、家電、家具、容器包装、危険物等）を客観的・多角的に鑑定してください：

                ■ STEP 1: 被写体の正確な特定（形状・特徴・ロゴ・印字の総合観察）
                - 履物・衣類（サンダル、クロックス、スニーカー、長靴、革靴、スリッパ、衣類、タオル等）
                - 生活雑貨・文具（財布、メガネ、傘、バッグ、時計、おもちゃ、文具等）
                - 小型家電・電子機器（スマホ、充電器、扇風機、ドライヤー、電子基板、バッテリー機器等）
                - 容器包装・ボトル（ペットボトル、空き缶、瓶、トレイ、スプレー缶、シャンプー・薬品容器等）
                - 家具・金物（フライパン、鍋、収納ケース、椅子、布団等）
                ※商品名、ブランドロゴ（例：「crocs」「NIKE」等）、型番、注意書きなどの文字・印字がある場合はOCRで正確に読み取り、品名特定の有力な根拠としてください。

                ■ STEP 2: 主たる素材・構造の判定
                - 素材（EVA樹脂・ゴム、皮革、布、プラスチック、金属、ガラス、陶器等）
                - 複合製品の場合は、分解が必要なパーツ（本体、フタ、中栓、残った中身、バッテリー等）を特定します。

                ■ STEP 3: 自治体公式分別ルールの適用（重要基準）
                - 履物・靴類（サンダル・クロックス・スニーカー・革靴・長靴等）：
                  一般に【可燃ごみ（燃やすごみ）】（指定袋に入るサイズ）。
                  ※【超重要警告】EVA樹脂やプラスチック製サンダルであっても、「容器包装プラスチック」ではないため、原則として資源プラではなく「可燃ごみ」になります。誤ってプラスチック資源に分類しないでください。
                - 衣類・布類：【可燃ごみ】または資源古布（自治体ルールによる）
                - 小型家電・バッテリー内蔵機器：火災防止のため市役所等の【小型家電回収ボックス】または不燃ごみ
                - ステンレス水筒・金属製品：本体は【不燃ごみ・金属類】、プラ蓋やパッキンは【プラスチック資源】
                - スプレー缶・カセットボンベ：中身使い切り、火気のない屋外でガス抜きし【有害危険物】または【不燃ごみ】
                - プラスチック製容器包装（プラマーク有）：水洗いして【プラスチック資源】
                - 医薬品・化粧品容器：残液は紙に吸わせて【可燃ごみ】、ボトル本体は素材に応じて分別

                もし材質や大きさによって自治体の分別区分が分かれる場合は「isAmbiguous: true」とし、ユーザーに尋ねるべき追加質問を1〜2個生成してください。

                必ず以下のJSONフォーマットのみで回答してください：
                {
                  "detectedTexts": ["画像から読み取った文字・ブランド・ロゴ（例: crocs, 虫さされ, 型番等）"],
                  "itemName": "判定した正確な品名（例: クロックス（EVA樹脂製サンダル）、スニーカー、ステンレス水筒、虫さされ外用薬ボトル）",
                  "confidenceScore": 95,
                  "isAmbiguous": false,
                  "categoryHint": "可燃ごみ または プラスチック資源 または 不燃ごみ または 粗大ごみ または 有害危険物 または 小型家電 または ペットボトル または 空き缶・空き瓶",
                  "reason": "読み取った特徴と材質に基づく判定根拠（例: 通気孔とヒールストラップの特徴からクロックス型サンダルと特定。素材はEVA樹脂ですが容器包装ではないため、この自治体では可燃ごみとなります）",
                  "disposalAdvice": "捨て方の具体的な手順",
                  "partsBreakdown": [
                    {
                      "partName": "パーツ名",
                      "material": "素材",
                      "categoryName": "分別区分",
                      "disposalMethod": "具体的な捨て方"
                    }
                  ],
                  "questions": [
                    {
                      "id": "question_id",
                      "title": "確認項目",
                      "question": "質問内容",
                      "options": [
                        {"id": "opt1", "label": "選択肢1"}
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
            
            var response: okhttp3.Response? = null
            var lastErrorCode = 0
            var lastErrorMessage = ""

            val modelList = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-3.8-flash")
            val modelUrls = modelList.map { "https://generativelanguage.googleapis.com/v1beta/models/$it:generateContent?key=$apiKey" }

            for ((idx, url) in modelUrls.withIndex()) {
                val req = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    response = res
                    break
                } else {
                    lastErrorCode = res.code
                    val errorBody = res.body?.string().orEmpty()
                    res.close()
                    try {
                        val errObj = JSONObject(errorBody).optJSONObject("error")
                        lastErrorMessage = errObj?.optString("message") ?: "HTTP $lastErrorCode"
                    } catch (_: Exception) {
                        lastErrorMessage = "HTTP $lastErrorCode: $errorBody"
                    }

                    // If error is 503 (high demand), 429 (rate limit), 404 (model not found), or 500 (transient server error),
                    // continue to next fallback model!
                    if (res.code == 503 || res.code == 429 || res.code == 404 || res.code == 400 || res.code >= 500) {
                        // Sleep briefly (200ms) before trying the next fallback model
                        try { kotlinx.coroutines.delay(200) } catch (_: Exception) {}
                        continue
                    } else {
                        // True unrecoverable error (e.g. 403 invalid API key)
                        break
                    }
                }
            }

            if (response == null || !response.isSuccessful) {
                val msg = if (lastErrorMessage.isNotBlank()) lastErrorMessage else "APIエラー (code: $lastErrorCode)"
                return@withContext WasteAiAnalysisResult.Error(msg)
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
