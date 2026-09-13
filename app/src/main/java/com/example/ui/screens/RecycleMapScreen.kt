package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DropoffCategory
import com.example.data.model.FacilityType
import com.example.data.model.Municipality
import com.example.data.model.RecycleSpot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleMapScreen(
    currentMunicipality: Municipality,
    spots: List<RecycleSpot>,
    selectedCategory: DropoffCategory?,
    onCategorySelected: (DropoffCategory?) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var highlightedSpotId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("recycle_map_screen")
    ) {
        // Header Info Bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "拠点回収・回収ボックス案内",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = currentMunicipality.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "乾電池・蛍光灯・小型家電など、ゴミ集積所に出せない品目の回収拠点を案内します。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("recycle_search_field"),
            placeholder = { Text("施設名・店舗名・住所で検索（例: 役所, ヤマダ, イオン）", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "検索") },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "クリア")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("すべて (${spots.size})") }
            )

            DropoffCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(if (isSelected) null else cat) },
                    label = { Text("${cat.iconEmoji} ${cat.title}") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(cat.colorHex).copy(alpha = 0.2f),
                        selectedLabelColor = Color(cat.colorHex)
                    )
                )
            }
        }

        // Radar Visualizer Map
        RecycleRadarView(
            spots = spots,
            highlightedSpotId = highlightedSpotId,
            onSpotClicked = { spot ->
                highlightedSpotId = spot.id
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Spots List
        Text(
            text = "近隣の回収拠点 (${spots.size}件)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (spots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Recycling,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "条件に一致する回収拠点が見つかりませんでした",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(onClick = {
                        onCategorySelected(null)
                        onSearchQueryChanged("")
                    }) {
                        Text("絞り込みをリセット")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(spots) { spot ->
                    val isHighlighted = spot.id == highlightedSpotId
                    RecycleSpotCard(
                        spot = spot,
                        isHighlighted = isHighlighted,
                        onOpenMap = {
                            openGoogleMaps(context, spot)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun RecycleSpotCard(
    spot: RecycleSpot,
    isHighlighted: Boolean,
    onOpenMap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spot_card_${spot.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isHighlighted) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title & Facility Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = spot.facilityType.iconEmoji,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = spot.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = spot.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = spot.facilityType.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Accepted Categories Tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                spot.acceptedCategories.forEach { cat ->
                    Surface(
                        color = Color(cat.colorHex).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.iconEmoji, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = cat.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(cat.colorHex),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Opening Hours & Box Location
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "🕒 営業時間: ${spot.openingHours}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "📦 設置場所: ${spot.boxLocationNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (spot.cautionNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "⚠️ 注意: ${spot.cautionNotes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Button
            Button(
                onClick = onOpenMap,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Googleマップで案内・経路を見る", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RecycleRadarView(
    spots: List<RecycleSpot>,
    highlightedSpotId: String?,
    onSpotClicked: (RecycleSpot) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outlineVariant

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.height * 0.45f

                // Distance concentric circles
                drawCircle(color = outlineColor.copy(alpha = 0.3f), radius = maxRadius, center = center, style = Stroke(1.5f))
                drawCircle(color = outlineColor.copy(alpha = 0.2f), radius = maxRadius * 0.65f, center = center, style = Stroke(1.5f))
                drawCircle(color = outlineColor.copy(alpha = 0.15f), radius = maxRadius * 0.35f, center = center, style = Stroke(1.5f))

                // Center (You are here)
                drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = center)
                drawCircle(color = Color.White, radius = 2.dp.toPx(), center = center)

                // Draw markers for spots
                spots.take(6).forEachIndexed { index, spot ->
                    val angle = (index * 55.0 + 30.0) * (Math.PI / 180.0)
                    val distFraction = 0.35f + (index % 3) * 0.25f
                    val spotX = center.x + (maxRadius * distFraction * Math.cos(angle)).toFloat()
                    val spotY = center.y + (maxRadius * distFraction * Math.sin(angle)).toFloat()

                    val isTarget = spot.id == highlightedSpotId
                    val spotColor = if (isTarget) Color(0xFFE53935) else Color(0xFF1976D2)

                    drawCircle(color = spotColor, radius = if (isTarget) 7.dp.toPx() else 5.dp.toPx(), center = Offset(spotX, spotY))
                }
            }

            // Legend labels
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(modifier = Modifier.width(4.dp))
                Text("現在地", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)

                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = Color(0xFF1976D2),
                    shape = CircleShape,
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(modifier = Modifier.width(4.dp))
                Text("回収拠点ピン", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
            }
        }
    }
}

private fun openGoogleMaps(context: Context, spot: RecycleSpot) {
    try {
        val query = Uri.encode("${spot.name} ${spot.address}")
        val uri = Uri.parse("geo:0,0?q=$query")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            // Fallback to web browser or generic geo handler
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            context.startActivity(webIntent)
        }
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(spot.name)}")
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        context.startActivity(webIntent)
    }
}
