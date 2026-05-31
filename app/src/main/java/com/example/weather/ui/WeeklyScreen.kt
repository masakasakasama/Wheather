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
import com.example.weather.data.model.DailyWeather
import com.example.weather.data.model.WeatherSnapshot

@Composable
fun WeeklyScreen(snapshot: WeatherSnapshot?) {
    var selectedDay by remember { mutableStateOf<DailyWeather?>(null) }
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
        Text("AM / PMの概況を表示します。カードを押すと詳細を表示します。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(snapshot.daily.take(14)) { day ->
                WeeklyRow(day = day, dayHours = snapshot.hourly.forDate(day.date), onClick = { selectedDay = day })
            }
        }
    }
    selectedDay?.let { day ->
        DayDetailDialog(day = day, dayHours = snapshot?.hourly?.forDate(day.date).orEmpty(), onDismiss = { selectedDay = null })
    }
}
