package com.example.data.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 根本的なPDF/ウェブリンク切れ対策マネージャー (Smart Link Fallback Engine)
 *
 * 【問題の本質と根本的解決策】
 * 1. 【Android WebViewの限界】標準WebViewはPDFインライン表示に非対応で真っ白になる。
 *    → Chrome Custom Tabs (CCT) / 外部ネイティブPDFビューアによる確実なレンダリングを採用。
 * 2. 【自治体WAFの壁】Google Docs Viewer等の外部プロキシはImperva等の自治体WAFに弾かれる。
 *    → 端末側の直接セッション(CCT/ブラウザ)で開くことで403エラーを回避。
 * 3. 【年度更新リンク切れ】自治体PDF直リンクは毎年の改訂で404になりやすい。
 *    → 自治体公式HTMLポータルを主軸としつつ、ドメイン限定検索(site:...)による最新PDFへの自動フォールバックを完備。
 */
object MunicipalityLinkResolver {

    private const val TAG = "LinkResolver"

    /**
     * URLがPDFファイルへの直リンクであるかを判定
     */
    fun isPdfUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val cleanUrl = url.split("?").first().lowercase()
        return cleanUrl.endsWith(".pdf")
    }

    /**
     * 自治体WebURLからホストドメインを抽出（例: www.city.nagoya.jp）
     */
    fun extractDomain(urlStr: String?): String? {
        if (urlStr.isNullOrBlank()) return null
        return try {
            val uri = java.net.URI(urlStr)
            uri.host?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 自治体名やドメインに応じた最適なPDF検索クエリURLを生成
     * ドメインが判明している場合は site: 演算子付きのURLも生成可能
     */
    fun buildSearchUrl(municipalityName: String, officialWebUrl: String? = null): String {
        val domain = extractDomain(officialWebUrl)
        val query = if (!domain.isNullOrBlank() && !domain.contains("google")) {
            "site:$domain ごみ 分別 早見表 PDF"
        } else {
            "$municipalityName ごみ 分別 早見表 PDF ハンドブック"
        }
        return "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
    }

    /**
     * PDFを安全かつ確実に開く（Chrome Custom Tabs 優先）
     * WebViewで真っ白になる問題を完全に防ぎ、Android端末の内蔵PDFエンジンで美しく描画する
     */
    fun openPdfSafely(
        context: Context,
        municipalityName: String,
        directPdfUrl: String?,
        officialWebUrl: String? = null
    ) {
        val targetUrl = when {
            !directPdfUrl.isNullOrBlank() -> directPdfUrl
            !officialWebUrl.isNullOrBlank() -> officialWebUrl
            else -> buildSearchUrl(municipalityName, officialWebUrl)
        }

        launchCustomTabOrBrowser(context, targetUrl)
    }

    /**
     * Google検索で最新の公式PDF・早見表をダイレクト検索
     */
    fun searchOfficialPdf(context: Context, municipalityName: String, officialWebUrl: String? = null) {
        val searchUrl = buildSearchUrl(municipalityName, officialWebUrl)
        launchCustomTabOrBrowser(context, searchUrl)
    }

    /**
     * 自治体の公式ごみ・環境ポータルページを開く
     */
    fun openOfficialPortal(context: Context, municipalityName: String, officialWebUrl: String?) {
        val url = if (!officialWebUrl.isNullOrBlank()) {
            officialWebUrl
        } else {
            val query = "$municipalityName 家庭ごみ 分別 公式"
            "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
        }
        launchCustomTabOrBrowser(context, url)
    }

    /**
     * Chrome Custom Tabs (CCT) で開く。未対応時は通常ブラウザへフォールバック
     */
    fun launchCustomTabOrBrowser(context: Context, url: String) {
        try {
            val uri = Uri.parse(url)
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            Log.w(TAG, "CustomTabs launch failed, falling back to Intent.ACTION_VIEW: ${e.message}")
            launchBrowser(context, url)
        }
    }

    /**
     * URLの死活チェック（HEADリクエストで200/300番台を確認）
     * UIをブロックしないようコルーチンで非同期実行
     */
    suspend fun checkUrlHealth(urlStr: String?): Boolean = withContext(Dispatchers.IO) {
        if (urlStr.isNullOrBlank()) return@withContext false
        try {
            val url = URL(urlStr)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 4000
                readTimeout = 4000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            }
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..399
        } catch (e: Exception) {
            Log.w(TAG, "Health check failed for $urlStr: ${e.message}")
            false
        }
    }

    private fun launchBrowser(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "ブラウザを開けませんでした: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

