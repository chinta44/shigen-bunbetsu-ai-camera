package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.util.MunicipalityLinkResolver
import java.net.URLEncoder

/**
 * 自治体公式PDF・早見表・ごみ分別ハンドブック内蔵スマートビューア
 *
 * 【根本的なリンク切れ対策】
 * 1. 万が一PDF URLが404/エラーになった場合、真っ白画面にせず即座に「リンク切れ検知パネル」を表示
 * 2. 自治体公式分別ポータルページへのワンタップ切替
 * 3. 自治体名＋キーワードによるWeb再検索フォールバック
 * 4. 外部ブラウザでのダイレクトオープン
 */
@Composable
fun OfficialHandbookViewerDialog(
    municipalityName: String,
    pdfUrl: String?,
    officialWebUrl: String?,
    title: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentUrl by remember {
        mutableStateOf(
            pdfUrl?.takeIf { it.isNotBlank() }
                ?: officialWebUrl?.takeIf { it.isNotBlank() }
                ?: "https://www.google.com/search?q=${URLEncoder.encode("$municipalityName ごみ 分別 早見表 PDF", "UTF-8")}"
        )
    }

    var isLoading by remember { mutableStateOf(true) }
    var hasLoadError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFD32F2F),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "閉じる",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "$municipalityName 公式ガイド・PDF",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    hasLoadError = false
                                    isLoading = true
                                    webViewInstance?.reload()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "再読み込み",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "ブラウザを開けませんでした", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "外部ブラウザで開く",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                // Loading progress indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFE53935)
                    )
                }

                // Smart Fallback Error Banner (shown if URL is broken / 404)
                if (hasLoadError) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "自治体のPDFリンクが変更または休止中です",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                            Text(
                                text = "自治体のWebサイト更新に伴い、直リンク先のPDFファイルが移動または更新された可能性があります。以下のスマート検索または公式ポータルをご利用ください。",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF5D4037),
                                lineHeight = 18.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        hasLoadError = false
                                        isLoading = true
                                        val query = "$municipalityName ごみ 分別 早見表 PDF"
                                        val googleSearch = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
                                        currentUrl = googleSearch
                                        webViewInstance?.loadUrl(googleSearch)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("最新PDFを検索", fontSize = 12.sp)
                                }

                                if (!officialWebUrl.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            hasLoadError = false
                                            isLoading = true
                                            currentUrl = officialWebUrl
                                            webViewInstance?.loadUrl(officialWebUrl)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("公式Webへ移動", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // In-App WebView
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewInstance = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    setSupportZoom(true)
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            hasLoadError = true
                                            isLoading = false
                                            errorMessage = error?.description?.toString() ?: "接続エラー"
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        return if (url.startsWith("http://") || url.startsWith("https://")) {
                                            false // Load within WebView
                                        } else {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                ctx.startActivity(intent)
                                                true
                                            } catch (e: Exception) {
                                                true
                                            }
                                        }
                                    }
                                }
                                loadUrl(currentUrl)
                            }
                        },
                        update = { webView ->
                            if (webView.url != currentUrl) {
                                webView.loadUrl(currentUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading && !hasLoadError) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFFD32F2F),
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "公式PDF・ページを読み込み中...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
