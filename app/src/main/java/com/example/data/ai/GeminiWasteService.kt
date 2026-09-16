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
            // Compress bitmap to reasonable size for vision API
            val scaledBitmap = scaleBitmapIfNeeded(bitmap, 800)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val targetFocusInstruction = if (!targetItemHint.isNullOrBlank()) {
                "\n【最重要指示】画面内に複数の物体が写っている場合、ユーザーが指定した対象「$targetItemHint」または指定枠内の物体を特定・分別してください。他の物や背景は無視してください。"
            } else {
                "\n画面内に複数の物体が写っている場合は、中央または最も手前にある代表的な不用品1点を特定してください。"
            }

            val prompt = """
                あなたは日本のゴミ分別専門家AIです。
                ユーザーは「$prefectureName $municipalityName」のゴミ分別ルールに従って分別したいと考えています。
                （この自治体の粗大ごみ基準は一辺 $oversizedThresholdCm cm以上です）

                添付された画像のごみ・不用品を特定し、分別判定を行ってください。
                $targetFocusInstruction
                
                もし、材質（硬質プラスチック、軟質プラ、革、布、金属混在など）や大きさ（${oversizedThresholdCm}cmを超えるか等）によって
                自治体の分別区分が分かれる場合は、必ず「isAmbiguous: true」とし、ユーザーに尋ねるべき追加質問を1〜2個生成してください。
                （例：プラスチックケース、衣装ケース、小型家電、フライパン、傘、クッション、スプレー缶、財布など）

                必ず以下のJSONフォーマットのみで回答してください：
                {
                  "itemName": "判定した品物名（例：プラスチックケース、ペットボトル）",
                  "confidenceScore": 95,
                  "isAmbiguous": true または false,
                  "categoryHint": "推測される分別区分（例：プラスチック資源、可燃ごみ、粗大ごみ等）",
                  "reason": "分別の理由や判断基準の説明",
                  "disposalAdvice": "捨て方のコツや注意点（水洗い、キャップ外し等）",
                  "questions": [
                    {
                      "id": "material",
                      "title": "材質の確認",
                      "question": "材質・素材はどれに当てはまりますか？",
                      "options": [
                        {"id": "hard_plastic", "label": "硬いプラ（タッパー・収納ケース等）"},
                        {"id": "soft_plastic", "label": "柔らかいプラ（包装用プラマーク等）"},
                        {"id": "metal_or_mix", "label": "金属製または金属との複合素材"}
                      ]
                    },
                    {
                      "id": "size",
                      "title": "大きさの確認",
                      "question": "一番長い辺の長さはどのくらいですか？",
                      "options": [
                        {"id": "under_threshold", "label": "${oversizedThresholdCm}cm未満"},
                        {"id": "over_threshold", "label": "${oversizedThresholdCm}cm以上（粗大ごみ基準）"}
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
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
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
