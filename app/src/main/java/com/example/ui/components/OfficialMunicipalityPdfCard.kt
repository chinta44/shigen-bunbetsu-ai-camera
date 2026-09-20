package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.data.model.Municipality
import com.example.data.util.MunicipalityLinkResolver
import com.example.ui.screens.OfficialHandbookViewerDialog
import java.net.URLEncoder

/**
 * Official Municipality Waste Sorting PDF & Handbook Link Card
 * Provides robust 3-stage link resolution to completely solve the municipality PDF link rot issue.
 */
@Composable
fun OfficialMunicipalityPdfCard(
    municipality: Municipality?,
    customPdfUrl: String? = null,
    customPdfTitle: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mName = municipality?.name ?: "お住まいの自治体"

    var isViewerDialogOpen by remember { mutableStateOf(false) }

    val pdfUrl = customPdfUrl?.takeIf { it.isNotBlank() }
        ?: municipality?.officialGuidePdfUrl?.takeIf { it.isNotBlank() }
    val pdfTitle = customPdfTitle?.takeIf { it.isNotBlank() }
        ?: municipality?.officialGuidePdfTitle?.takeIf { it.isNotBlank() }
        ?: "${mName} 家庭ごみ分別早見表（公式PDF）"

    val officialWebUrl = municipality?.officialWebUrl?.takeIf { it.isNotBlank() }

    val hasDirectPdf = !pdfUrl.isNullOrBlank()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("official_pdf_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF7F7) // Very gentle warm red-tinted card
        ),
        border = BorderStroke(1.5.dp, Color(0xFFE57373).copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: PDF badge & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFD32F2F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PDF",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "自治体公式 分別PDFで確認",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "原本照合",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "AI判定の確認・詳細ルールの原本照合（リンク切れ自動回避機能付き）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Description / Target Info
            Surface(
                color = Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFFFCDD2))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = pdfTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = if (hasDirectPdf) {
                            "AIの判定結果が正しいか、$mName の公式分別早見表・ハンドブック原本を開いて直接確認できます。リンク切れが起きてもアプリ内自動検索で最新版へ即時フォールバックします。"
                        } else {
                            "$mName の公式ごみ分別ガイドや早見表PDFを検索・確認できます。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF555555),
                        lineHeight = 18.sp
                    )
                }
            }

            // Action Buttons
            // Button 1: Smart In-App Viewer (Safely loads with fallback error handling)
            Button(
                onClick = { isViewerDialogOpen = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_official_pdf_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "📄 公式分別PDF（早見表）を開く",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White.copy(alpha = 0.85f)
                )
            }

            // Button 2: Direct Municipality Portal URL
            if (!officialWebUrl.isNullOrBlank()) {
                OutlinedButton(
                    onClick = {
                        MunicipalityLinkResolver.openOfficialPortal(context, mName, officialWebUrl)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${mName} ごみ分別公式ポータルを開く", fontSize = 13.sp)
                }
            }

            // Button 3: Smart Fallback Search (Always guaranteed to work even if city changes URLs completely)
            OutlinedButton(
                onClick = {
                    MunicipalityLinkResolver.searchOfficialPdf(context, mName)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("🔍 Webで最新の分別PDF・早見表をダイレクト再検索", fontSize = 12.sp)
            }
        }
    }

    // In-App Smart Viewer Dialog
    if (isViewerDialogOpen) {
        OfficialHandbookViewerDialog(
            municipalityName = mName,
            pdfUrl = pdfUrl,
            officialWebUrl = officialWebUrl,
            title = pdfTitle,
            onDismiss = { isViewerDialogOpen = false }
        )
    }
}
