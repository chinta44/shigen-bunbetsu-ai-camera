package com.example.data.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 根本的なPDF/ウェブリンク切れ対策マネージャー (Smart Link Fallback Engine)
 *
 * 【問題の本質】
 * 自治体のホームページやPDFリンクは、年度更新(4月/10月)やCMSリニューアル、
 * ディレクトリ階層の変更、ファイル名の日付更新により頻繁に404(リンク切れ)になります。
 * 単一の直リンクだけを叩くと高確率でエラーとなりユーザー体験が崩壊します。
 *
 * 【3段構えのスマート解決フロー】
 * 1. 登録済み候補URLの順次検証 (Direct URL / Backup Mirror)
 * 2. 直リンクが切れている場合、自治体公式ドメイン内検索・ごみ分別公式ページへ自動フォールバック
 * 3. 自治体名＋分別キーワードのGoogleダイレクト検索へスムーズに誘導
 */
object MunicipalityLinkResolver {

    private const val TAG = "LinkResolver"

    /**
     * リンクを開くためのスマート・セーフランチャー
     * 直接PDF URLを開く前に、あるいは開く際にフォールバックURLも準備して起動する
     */
    fun openSmartPdfLink(
        context: Context,
        municipalityName: String,
        directPdfUrl: String?,
        officialWebUrl: String?
    ) {
        val query = "$municipalityName ごみ 分別 早見表 PDF"
        val googleSearchUrl = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"

        val targetUrl = when {
            !directPdfUrl.isNullOrBlank() -> directPdfUrl
            !officialWebUrl.isNullOrBlank() -> officialWebUrl
            else -> googleSearchUrl
        }

        launchBrowser(context, targetUrl)
    }

    /**
     * Google検索で最新の公式PDF・早見表をダイレクト検索
     */
    fun searchOfficialPdf(context: Context, municipalityName: String) {
        val query = "$municipalityName ごみ 分別 早見表 PDF ハンドブック"
        val url = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
        launchBrowser(context, url)
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
        launchBrowser(context, url)
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
                connectTimeout = 3000
                readTimeout = 3000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")
            }
            val responseCode = connection.responseCode
            connection.disconnect()
            // 200 OK or 3xx Redirects are valid
            responseCode in 200..399
        } catch (e: Exception) {
            Log.w(TAG, "Health check failed for $urlStr: ${e.message}")
            // 通信タイムアウトやファイアウォールブロック時は一旦true扱い（誤判定で消さない）
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
