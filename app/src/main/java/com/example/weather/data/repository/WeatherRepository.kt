package com.example.weather.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.weather.data.api.AirQualityClient
import com.example.weather.data.api.JmaAmedasClient
import com.example.weather.data.api.JmaRadarClient
import com.example.weather.data.api.OpenMeteoClient
import com.example.weather.data.cache.WeatherCache
import com.example.weather.data.model.NotificationSettings
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.canonicalizedSavedLocations
import com.example.weather.data.model.identityKey
import com.example.weather.data.model.isDeviceLocation
import com.example.weather.data.model.isInJapan
import com.example.weather.data.model.sameForecastPlaceAs
import com.example.weather.widget.WeatherWidget
import com.example.weather.widget.WeatherSquareWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WeatherRepository(
    private val context: Context,
    private val openMeteoClient: OpenMeteoClient,
    private val airQualityClient: AirQualityClient,
    private val amedasClient: JmaAmedasClient,
    private val radarClient: JmaRadarClient,
    private val cache: WeatherCache,
    private val temperatureConsensusEngine: TemperatureConsensusEngine = TemperatureConsensusEngine(),
) {
    private val savedLocationsMutex = Mutex()
    private val refreshMutex = Mutex()

    val weather: Flow<WeatherSnapshot?> = cache.snapshot
    val selectedLocation: Flow<WeatherLocation> = cache.selectedLocation
    val savedLocations: Flow<List<WeatherLocation>> = cache.savedLocations
    val notificationSettings: Flow<NotificationSettings> = cache.notificationSettings

    suspend fun selectedLocationOnce(): WeatherLocation = cache.readLocationOnce()

    suspend fun cachedWeatherOnce(): WeatherSnapshot? = cache.readSnapshotOnce()

    suspend fun refresh(): Result<WeatherSnapshot> = refresh(cache.readLocationOnce())

    suspend fun refresh(location: WeatherLocation): Result<WeatherSnapshot> = refreshMutex.withLock {
        runCatching {
            val result = coroutineScope {
                val useJmaServices = location.isInJapan()
                val forecast = async { openMeteoClient.fetchForecast(location) }
                val temperatureModels = async {
                    runCatching { openMeteoClient.fetchTemperatureModels(location) }.getOrNull()
                }
                val observation = async {
                    if (useJmaServices) {
                        runCatching { amedasClient.latestTemperature(location) }.getOrNull()
                    } else {
                        null
                    }
                }
                val airQuality = async {
                    runCatching { airQualityClient.fetchAirQuality(location) }.getOrNull()
                }
                val radar = async {
                    if (useJmaServices) {
                        runCatching { radarClient.latestPrecipitation(location) }.getOrNull()
                    } else {
                        null
                    }
                }
                val accuracy = async { cache.readTemperatureAccuracyOnce() }
                val consensus = temperatureConsensusEngine.apply(
                    base = forecast.await(),
                    modelForecast = temperatureModels.await(),
                    observation = observation.await(),
                    previousAccuracyState = accuracy.await(),
                )
                consensus.copy(
                    snapshot = consensus.snapshot.copy(
                        airQuality = airQuality.await(),
                        radarPrecipitation = radar.await(),
                    ),
                )
            }
            val snapshot = result.snapshot
            val selectedLocation = cache.readLocationOnce()
            check(location.sameForecastPlaceAs(selectedLocation)) {
                "Selected location changed while weather was loading"
            }
            cache.saveSnapshotAndTemperatureAccuracy(snapshot, result.accuracyState)
            updateWidgets()
            snapshot
        }
    }

    suspend fun saveLocation(location: WeatherLocation) {
        cache.saveLocation(location)
        addSavedLocation(location)
        updateWidgets()
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

    suspend fun deleteLocation(location: WeatherLocation): WeatherLocation? = savedLocationsMutex.withLock {
        val current = cache.readSavedLocationsOnce()
        val next = current.filterNot { it.identityKey() == location.identityKey() }
        cache.saveLocations(next.ifEmpty { current.take(1) })
        val selected = cache.readLocationOnce()
        if (selected.identityKey() == location.identityKey()) {
            val replacement = next.firstOrNull() ?: current.first()
            cache.saveLocation(replacement)
            updateWidgets()
            replacement
        } else {
            null
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

    private suspend fun updateWidgets() {
        WeatherWidget().updateAll(context)
        WeatherSquareWidget().updateAll(context)
    }
}
