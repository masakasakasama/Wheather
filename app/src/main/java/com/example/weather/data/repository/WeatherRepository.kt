package com.example.weather.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.weather.data.api.OpenMeteoClient
import com.example.weather.data.cache.WeatherCache
import com.example.weather.data.cache.identityKey
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
    val savedLocations: Flow<List<WeatherLocation>> = cache.savedLocations

    suspend fun refresh(): Result<WeatherSnapshot> = refresh(cache.readLocationOnce())

    suspend fun refresh(location: WeatherLocation): Result<WeatherSnapshot> {
        return runCatching {
            val snapshot = openMeteoClient.fetchForecast(location)
            saveLocation(location)
            cache.saveSnapshot(snapshot)
            WeatherWidget().updateAll(context)
            snapshot
        }
    }

    suspend fun saveLocation(location: WeatherLocation) {
        cache.saveLocation(location)
        addSavedLocation(location)
    }

    suspend fun addSavedLocation(location: WeatherLocation) {
        val current = cache.readSavedLocationsOnce()
        if (current.any { it.identityKey() == location.identityKey() }) return
        cache.saveLocations(current + location)
    }

    suspend fun moveLocation(location: WeatherLocation, direction: Int) {
        val current = cache.readSavedLocationsOnce().toMutableList()
        val index = current.indexOfFirst { it.identityKey() == location.identityKey() }
        val target = (index + direction).coerceIn(0, current.lastIndex)
        if (index < 0 || index == target) return
        val item = current.removeAt(index)
        current.add(target, item)
        cache.saveLocations(current)
    }

    suspend fun deleteLocation(location: WeatherLocation) {
        val current = cache.readSavedLocationsOnce()
        val next = current.filterNot { it.identityKey() == location.identityKey() }
        cache.saveLocations(next.ifEmpty { current.take(1) })
        if (cache.readLocationOnce().identityKey() == location.identityKey()) {
            cache.saveLocation(next.firstOrNull() ?: current.first())
        }
    }

    suspend fun searchLocations(query: String): Result<List<WeatherLocation>> {
        return runCatching { openMeteoClient.searchLocations(query) }
    }
}
