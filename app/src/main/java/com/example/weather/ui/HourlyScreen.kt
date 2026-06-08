package com.example.weather.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weather.data.model.WeatherSnapshot

@Composable
fun HourlyScreen(snapshot: WeatherSnapshot?) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("時間ごとの予報", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (snapshot == null) {
            Text("データがありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        val hours = remember(snapshot) { snapshot.hourly.nextHours(48) }
        Text(
            "現在時刻以降の48時間。日付ごとに「今日／明日」でまとめています。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
        )

        val grouped = remember(hours) { groupHoursByDate(hours) }
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            grouped.forEach { (date, dayHours) ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DayBadge(date)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MiniHourlyGraph(dayHours)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                dayHours.forEach { hour -> HourCompactCard(hour) }
                            }
                        }
                    }
                }
            }
        }
    }
}
