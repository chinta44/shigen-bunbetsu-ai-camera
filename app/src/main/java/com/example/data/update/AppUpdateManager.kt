package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val apkDownloadUrl: String?,
    val apkFileName: String?,
    val apkSizeBytes: Long,
    val publishedAt: String,
    val htmlUrl: String,
    val isNewer: Boolean
)

sealed class AppUpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : AppUpdateCheckResult()
    data class UpToDate(val latestVersion: String) : AppUpdateCheckResult()
    data class NoReleaseFound(val message: String) : AppUpdateCheckResult()
    data class Error(val message: String) : AppUpdateCheckResult()
}

object AppUpdateManager {

    const val GITHUB_OWNER = "chinta44"
    const val GITHUB_REPO = "shigen-bunbetsu-ai-camera"
    const val GITHUB_API_LATEST_RELEASE = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val GITHUB_RELEASES_WEB_URL = "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Check GitHub Releases API for the latest version
     */
    suspend fun checkForUpdate(currentVersionName: String): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API_LATEST_RELEASE)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "ShigenBunbetsuApp-Android")
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.code == 404) {
                return@withContext AppUpdateCheckResult.NoReleaseFound(
                    "GitHubリポジトリ（$GITHUB_OWNER/$GITHUB_REPO）にまだReleaseが作成されていません。"
                )
            }

            if (!response.isSuccessful) {
                return@withContext AppUpdateCheckResult.Error(
                    "GitHub API接続エラー (コード: ${response.code})"
                )
            }

            val bodyString = response.body?.string() ?: return@withContext AppUpdateCheckResult.Error("空のレスポンスを受信しました")
            val json = JSONObject(bodyString)

            val tagName = json.optString("tag_name", "")
            val title = json.optString("name", tagName)
            val releaseNotes = json.optString("body", "更新内容の詳細はありません。")
            val htmlUrl = json.optString("html_url", GITHUB_RELEASES_WEB_URL)
            val publishedAt = json.optString("published_at", "")

            // Parse clean version name (e.g. "v1.3.7" -> "1.3.7")
            val cleanVersion = tagName.trim().removePrefix("v").removePrefix("V")

            // Find APK asset in assets array
            var apkUrl: String? = null
            var apkName: String? = null
            var apkSize: Long = 0L

            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    val contentType = asset.optString("content_type", "")
                    val downloadUrl = asset.optString("browser_download_url", "")

                    if (name.endsWith(".apk", ignoreCase = true) ||
                        contentType.contains("android.package-archive", ignoreCase = true)
                    ) {
                        apkUrl = downloadUrl
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            val isNewer = isVersionNewer(cleanVersion, currentVersionName)

            val updateInfo = AppUpdateInfo(
                tagName = tagName,
                versionName = cleanVersion,
                title = title,
                releaseNotes = releaseNotes,
                apkDownloadUrl = apkUrl,
                apkFileName = apkName ?: "shigen-bunbetsu-v$cleanVersion.apk",
                apkSizeBytes = apkSize,
                publishedAt = publishedAt,
                htmlUrl = htmlUrl,
                isNewer = isNewer
            )

            if (isNewer) {
                AppUpdateCheckResult.UpdateAvailable(updateInfo)
            } else {
                AppUpdateCheckResult.UpToDate(cleanVersion)
            }
        } catch (e: Exception) {
            AppUpdateCheckResult.Error(e.localizedMessage ?: "更新確認中に予期しないエラーが発生しました")
        }
    }

    /**
     * Download the APK file with progress reporting
     * @return Result with the downloaded File
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates")
            if (!updatesDir.exists()) {
                updatesDir.mkdirs()
            }

            val destinationFile = File(updatesDir, fileName)
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "ShigenBunbetsuApp-Android")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("ダウンロードに失敗しました (HTTP ${response.code})"))
            }

            val responseBody = response.body ?: return@withContext Result.failure(Exception("レスポンスボディが空です"))
            val totalBytes = responseBody.contentLength()

            responseBody.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastPercent = -1

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val percent = if (totalBytes > 0) {
                            ((totalRead * 100) / totalBytes).toInt()
                        } else {
                            0
                        }

                        if (percent != lastPercent) {
                            lastPercent = percent
                            withContext(Dispatchers.Main) {
                                onProgress(percent, totalRead, totalBytes)
                            }
                        }
                    }
                    output.flush()
                }
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Launch the system package installer for the downloaded APK
     */
    fun launchInstaller(context: Context, apkFile: File): Boolean {
        try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return false
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Check if the app has permission to install packages (Android 8.0+)
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Open settings screen to grant "Install unknown apps" permission
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Semantic version comparison (e.g. "1.3.7" > "1.3.6")
     */
    fun isVersionNewer(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }

        val maxParts = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxParts) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
