package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CollectionSchedule
import com.example.data.model.Municipality
import com.example.data.model.ScheduleRecurrence
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MunicipalityEditDialog(
    municipality: Municipality,
    onSave: (Municipality) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(municipality.name) }
    var district by remember { mutableStateOf(municipality.district) }
    var thresholdCm by remember { mutableIntStateOf(municipality.oversizedThresholdCm) }
    var plasticNotes by remember { mutableStateOf(municipality.plasticRuleNotes) }

    // Find current days from existing schedules
    val burnableSchedules = municipality.schedules.filter { it.categoryId == "burnable" }
    var burnableDay1 by remember {
        mutableStateOf(burnableSchedules.getOrNull(0)?.dayOfWeek ?: DayOfWeek.MONDAY)
    }
    var burnableDay2 by remember {
        mutableStateOf(burnableSchedules.getOrNull(1)?.dayOfWeek ?: DayOfWeek.THURSDAY)
    }
    var plasticDay by remember {
        mutableStateOf(
            municipality.schedules.firstOrNull { it.categoryId == "plastic" }?.dayOfWeek ?: DayOfWeek.WEDNESDAY
        )
    }
    var bottleCanDay by remember {
        mutableStateOf(
            municipality.schedules.firstOrNull { it.categoryId == "bottle_can" }?.dayOfWeek ?: DayOfWeek.THURSDAY
        )
    }

    val daysOfWeek = listOf(
        DayOfWeek.MONDAY to "月",
        DayOfWeek.TUESDAY to "火",
        DayOfWeek.WEDNESDAY to "水",
        DayOfWeek.THURSDAY to "木",
        DayOfWeek.FRIDAY to "金",
        DayOfWeek.SATURDAY to "土",
        DayOfWeek.SUNDAY to "日"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("municipality_edit_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "自治体ルール・カレンダーの微調整",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "AIが自動設定した内容をお住まいの地区に合わせて自由に微調整できます。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Name & District
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("自治体名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("地区名・収集エリア（例: 鼎地区, 本町）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Oversized Threshold
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "粗大ごみ（大型ごみ）の基準サイズ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(30, 45, 50, 60).forEach { size ->
                                FilterChip(
                                    selected = thresholdCm == size,
                                    onClick = { thresholdCm = size },
                                    label = { Text("${size}cm以上") },
                                    leadingIcon = if (thresholdCm == size) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                        Text(
                            text = "※ 最長辺が${thresholdCm}cmを超える家具や家電は粗大ごみ扱いとなります",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Collection Days
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📅 定期収集曜日の設定",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "可燃ごみ（週2回）",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            daysOfWeek.forEach { (day, label) ->
                                val isSelected = burnableDay1 == day || burnableDay2 == day
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (burnableDay1 == day) {
                                            // do nothing or keep
                                        } else if (burnableDay2 == day) {
                                            // keep
                                        } else {
                                            burnableDay1 = burnableDay2
                                            burnableDay2 = day
                                        }
                                    },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "プラスチック資源（週1回）",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            daysOfWeek.forEach { (day, label) ->
                                FilterChip(
                                    selected = plasticDay == day,
                                    onClick = { plasticDay = day },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ビン・缶・ペットボトル（資源ごみ）",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            daysOfWeek.forEach { (day, label) ->
                                FilterChip(
                                    selected = bottleCanDay == day,
                                    onClick = { bottleCanDay = day },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                // Plastic rule memo
                OutlinedTextField(
                    value = plasticNotes,
                    onValueChange = { plasticNotes = it },
                    label = { Text("プラごみ分別ルール・指定袋メモ") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedSchedules = listOf(
                        CollectionSchedule("burnable", burnableDay1, ScheduleRecurrence.Weekly, "毎週 ${dayToJp(burnableDay1)}・${dayToJp(burnableDay2)}曜日"),
                        CollectionSchedule("burnable", burnableDay2, ScheduleRecurrence.Weekly, "毎週 ${dayToJp(burnableDay1)}・${dayToJp(burnableDay2)}曜日"),
                        CollectionSchedule("plastic", plasticDay, ScheduleRecurrence.Weekly, "毎週 ${dayToJp(plasticDay)}曜日"),
                        CollectionSchedule("bottle_can", bottleCanDay, ScheduleRecurrence.MonthlyWeeks(listOf(1, 3)), "第1・第3 ${dayToJp(bottleCanDay)}曜日"),
                        CollectionSchedule("non_burnable", DayOfWeek.THURSDAY, ScheduleRecurrence.MonthlyWeeks(listOf(2, 4)), "第2・第4 木曜日"),
                        CollectionSchedule("paper", DayOfWeek.SATURDAY, ScheduleRecurrence.MonthlyWeeks(listOf(1, 3)), "第1・第3 土曜日"),
                        CollectionSchedule("oversized", null, ScheduleRecurrence.OnDemandReservation, "事前予約制（${name}窓口）")
                    )

                    val updated = municipality.copy(
                        name = name.trim(),
                        district = district.trim(),
                        oversizedThresholdCm = thresholdCm,
                        plasticRuleNotes = plasticNotes.trim(),
                        schedules = updatedSchedules
                    )
                    onSave(updated)
                }
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("保存して適用")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

private fun dayToJp(day: DayOfWeek): String {
    return when (day) {
        DayOfWeek.MONDAY -> "月"
        DayOfWeek.TUESDAY -> "火"
        DayOfWeek.WEDNESDAY -> "水"
        DayOfWeek.THURSDAY -> "木"
        DayOfWeek.FRIDAY -> "金"
        DayOfWeek.SATURDAY -> "土"
        DayOfWeek.SUNDAY -> "日"
    }
}
