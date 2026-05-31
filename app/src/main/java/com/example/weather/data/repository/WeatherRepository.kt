package com.example.weather.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.weather.data.api.OpenMeteoClient
import com.example.weather.data.cache.WeatherCache
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.widget.WeatherWidget
import kotlinx.coroutines.flow.Flow

class WeatherRepository(
    private val context: Context,
    private val openMeteoClient: OpenMeteoClient,
    private val cache: WeatherCache,
) {
    val weather: Flow<WeatherSnapshot?> = cache.snapshot
    val selectedLocation: Flow<WeatherLocation> = cache.selectedLocation

    suspend fun refresh(): Result<WeatherSnapshot> = refresh(cache.readLocationOnce())

    suspend fun refresh(location: WeatherLocation): Result<WeatherSnapshot> {
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

    suspend fun searchLocations(query: String): Result<List<WeatherLocation>> {
        return runCatching { openMeteoClient.searchLocations(query) }
    }
}
