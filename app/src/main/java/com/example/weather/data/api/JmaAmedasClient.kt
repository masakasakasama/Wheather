package com.example.weather.data.api

import com.example.weather.data.model.AmedasObservationValue
import com.example.weather.data.model.AmedasStation
import com.example.weather.data.model.TemperatureObservation
import com.example.weather.data.model.WeatherLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class JmaAmedasClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val stationMutex = Mutex()

    @Volatile
    private var stationCache: Map<String, AmedasStation>? = null

    suspend fun latestTemperature(location: WeatherLocation): TemperatureObservation? = withContext(Dispatchers.IO) {
        if (!location.isInAmedasCoverage()) return@withContext null

        val latestText = getText(LATEST_TIME_URL).trim()
        val observedAt = runCatching { OffsetDateTime.parse(latestText) }.getOrNull() ?: return@withContext null
        val observedAtMillis = observedAt.toInstant().toEpochMilli()
        val ageMillis = nowMillis() - observedAtMillis
        if (ageMillis !in -MAX_CLOCK_SKEW_MILLIS..MAX_OBSERVATION_AGE_MILLIS) return@withContext null

        val jst = observedAt.atZoneSameInstant(JAPAN_ZONE)
        val mapUrl = "$MAP_BASE_URL/${jst.format(MAP_TIME_FORMAT)}.json"
        val observations = json.decodeFromString<Map<String, AmedasObservationValue>>(getText(mapUrl))
        val stations = stationTable()

        selectNearestTemperature(
            location = location,
            stations = stations,
            observations = observations,
            observedAtMillis = observedAtMillis,
        )
    }

    internal fun selectNearestTemperature(
        location: WeatherLocation,
        stations: Map<String, AmedasStation>,
        observations: Map<String, AmedasObservationValue>,
        observedAtMillis: Long,
    ): TemperatureObservation? {
        return observations.mapNotNull { (stationId, value) ->
            val temperatureValues = value.temp ?: return@mapNotNull null
            val temperature = temperatureValues.getOrNull(0) ?: return@mapNotNull null
            val quality = temperatureValues.getOrNull(1)?.toInt() ?: return@mapNotNull null
            if (quality != NORMAL_QUALITY_CODE) return@mapNotNull null
            val station = stations[stationId] ?: return@mapNotNull null
            val latitude = station.lat.toCoordinate() ?: return@mapNotNull null
            val longitude = station.lon.toCoordinate() ?: return@mapNotNull null
            val distanceKm = haversineKm(location.latitude, location.longitude, latitude, longitude)
            if (distanceKm > MAX_STATION_DISTANCE_KM) return@mapNotNull null
            TemperatureObservation(
                temperatureC = temperature,
                stationId = stationId,
                stationName = station.name.ifBlank { "アメダス $stationId" },
                distanceKm = distanceKm,
                observedAtMillis = observedAtMillis,
            )
        }.minByOrNull { it.distanceKm }
    }

    private suspend fun stationTable(): Map<String, AmedasStation> {
        stationCache?.let { return it }
        return stationMutex.withLock {
            stationCache ?: json.decodeFromString<Map<String, AmedasStation>>(getText(STATION_TABLE_URL))
                .also { stationCache = it }
        }
    }

    private fun getText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PersonalWeather/1.0")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("JMA AMeDAS request failed: HTTP ${response.code}")
            return response.body?.string() ?: throw IOException("JMA AMeDAS response was empty")
        }
    }

    companion object {
        internal const val MAX_STATION_DISTANCE_KM = 30.0
        internal const val MAX_OBSERVATION_AGE_MILLIS = 20 * 60 * 1000L
        private const val MAX_CLOCK_SKEW_MILLIS = 5 * 60 * 1000L
        private const val NORMAL_QUALITY_CODE = 0
        private const val LATEST_TIME_URL = "https://www.jma.go.jp/bosai/amedas/data/latest_time.txt"
        private const val STATION_TABLE_URL = "https://www.jma.go.jp/bosai/amedas/const/amedastable.json"
        private const val MAP_BASE_URL = "https://www.jma.go.jp/bosai/amedas/data/map"
        private val JAPAN_ZONE = ZoneId.of("Asia/Tokyo")
        private val MAP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }
}

private fun WeatherLocation.isInAmedasCoverage(): Boolean =
    latitude in 20.0..46.5 && longitude in 122.0..154.0

private fun List<Double>.toCoordinate(): Double? {
    val degrees = getOrNull(0) ?: return null
    val minutes = getOrNull(1) ?: return null
    return degrees + minutes / 60.0
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val latitudeDelta = Math.toRadians(lat2 - lat1)
    val longitudeDelta = Math.toRadians(lon2 - lon1)
    val a = sin(latitudeDelta / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(longitudeDelta / 2).pow(2)
    return 2 * 6_371.0 * asin(sqrt(a))
}
