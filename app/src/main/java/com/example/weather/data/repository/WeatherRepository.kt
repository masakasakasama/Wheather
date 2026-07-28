package com.example.weather.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.weather.data.api.AirQualityClient
import com.example.weather.data.api.OpenMeteoClient
import com.example.weather.data.cache.WeatherCache
import com.example.weather.data.model.NotificationSettings
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.canonicalizedSavedLocations
import com.example.weather.data.model.identityKey
import com.example.weather.data.model.isDeviceLocation
import com.example.weather.widget.WeatherWidget
import com.example.weather.widget.WeatherSquareWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WeatherRepository(
    private val context: Context,
    private val openMeteoClient: OpenMeteoClient,
    private val airQualityClient: AirQualityClient,
    private val cache: WeatherCache,
) {
    private val savedLocationsMutex = Mutex()

    val weather: Flow<WeatherSnapshot?> = cache.snapshot
    val selectedLocation: Flow<WeatherLocation> = cache.selectedLocation
    val savedLocations: Flow<List<WeatherLocation>> = cache.savedLocations
    val notificationSettings: Flow<NotificationSettings> = cache.notificationSettings

    suspend fun refresh(): Result<WeatherSnapshot> = refresh(cache.readLocationOnce())

    suspend fun refresh(location: WeatherLocation): Result<WeatherSnapshot> {
        return runCatching {
            val forecast = openMeteoClient.fetchForecast(location)
            val snapshot = forecast.copy(
                airQuality = airQualityClient.fetchAirQuality(location),
            )
            saveLocation(location)
            cache.saveSnapshot(snapshot)
            WeatherWidget().updateAll(context)
            WeatherSquareWidget().updateAll(context)
            snapshot
        }
    }

    suspend fun saveLocation(location: WeatherLocation) {
        cache.saveLocation(location)
        addSavedLocation(location)
    }

    suspend fun addSavedLocation(location: WeatherLocation) = savedLocationsMutex.withLock {
        val current = cache.readSavedLocationsOnce().canonicalizedSavedLocations()
        val existingIndex = current.indexOfFirst { it.identityKey() == location.identityKey() }
        if (existingIndex >= 0) {
            if (location.isDeviceLocation() && current[existingIndex] != location) {
                cache.saveLocations(current.toMutableList().apply { set(existingIndex, location) })
            }
            return@withLock
        }
        cache.saveLocations(current + location)
    }

    suspend fun moveLocation(location: WeatherLocation, direction: Int) = savedLocationsMutex.withLock {
        val current = cache.readSavedLocationsOnce().toMutableList()
        val index = current.indexOfFirst { it.identityKey() == location.identityKey() }
        if (index < 0) return@withLock
        val target = (index + direction).coerceIn(0, current.lastIndex)
        if (index == target) return@withLock
        val item = current.removeAt(index)
        current.add(target, item)
        cache.saveLocations(current)
    }

    suspend fun deleteLocation(location: WeatherLocation) = savedLocationsMutex.withLock {
        val current = cache.readSavedLocationsOnce()
        val next = current.filterNot { it.identityKey() == location.identityKey() }
        cache.saveLocations(next.ifEmpty { current.take(1) })
        if (cache.readLocationOnce().identityKey() == location.identityKey()) {
            cache.saveLocation(next.firstOrNull() ?: current.first())
        }
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings) {
        cache.saveNotificationSettings(
            settings.copy(
                rainLookAheadHours = settings.rainLookAheadHours.coerceIn(1, 12),
                rainProbabilityThreshold = settings.rainProbabilityThreshold.coerceIn(10, 100),
                rainAmountThresholdMm = settings.rainAmountThresholdMm.coerceIn(0.0, 10.0),
            ),
        )
    }

    suspend fun searchLocations(query: String): Result<List<WeatherLocation>> {
        return runCatching { openMeteoClient.searchLocations(query) }
    }
}
