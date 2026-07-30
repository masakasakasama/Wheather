package com.example.weather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.forecastDays
import com.example.weather.data.model.freshRadarPrecipitation

@Composable
fun WeeklyScreen(snapshot: WeatherSnapshot?) {
    var selectedDayDate by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("2週間予報", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (snapshot == null) {
            Text("データがありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        Text("日付を押すと1時間ごとの予報を表示します。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(snapshot.forecastDays().take(14), key = { it.date }) { day ->
                WeeklyRow(day = day, dayHours = snapshot.hourly.forDate(day.date), onClick = { selectedDayDate = day.date })
            }
        }
    }
    selectedDayDate
        ?.let { date -> snapshot?.forecastDays()?.firstOrNull { it.date == date } }
        ?.let { day ->
        DayDetailDialog(
            day = day,
            dayHours = snapshot?.hourly?.forDate(day.date).orEmpty(),
            timezone = snapshot?.timezone ?: "Asia/Tokyo",
            radarPrecipitation = snapshot?.freshRadarPrecipitation(),
            onDismiss = { selectedDayDate = null },
        )
    }
}
