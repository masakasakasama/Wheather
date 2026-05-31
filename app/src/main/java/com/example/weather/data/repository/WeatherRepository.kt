package com.example.weather.data.repository

import android.content.Context
import com.example.weather.data.api.OpenMeteoClient
import com.example.weather.data.cache.WeatherCache
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.widget.WeatherWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.Flow

class WeatherRepository(
    private val context: Context,
    private val openMeteoClient: OpenMeteoClient,
    private val cache: WeatherCache,
) {
    val weather: Flow<WeatherSnapshot?> = cache.snapshot
    val selectedLocation: Flow<WeatherLocation> = cache.selectedLocation

    suspend fun refresh(location: WeatherLocation = cache.readLocationOnce()): Result<WeatherSnapshot> {
        return runCatching {
            val snapshot = openMeteoClient.fetchForecast(location)
            cache.saveLocation(location)
            cache.saveSnapshot(snapshot)
            WeatherWidget().updateAll(context)
            snapshot
        }
    }

    suspend fun saveLocation(location: WeatherLocation) {
        cache.saveLocation(location)
    }
}
