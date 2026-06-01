package com.example.weather.data.api

import com.example.weather.data.model.CurrentWeather
import com.example.weather.data.model.DailyWeather
import com.example.weather.data.model.GeocodingResponse
import com.example.weather.data.model.HourlyWeather
import com.example.weather.data.model.OpenMeteoResponse
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.toWeatherLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class OpenMeteoClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun fetchForecast(location: WeatherLocation): WeatherSnapshot = withContext(Dispatchers.IO) {
        val preferred = request(location, useJmaModel = true)
        if (preferred != null) {
            val needsProbabilityFallback =
                preferred.daily.any { it.maxPrecipitationProbability == null } ||
                    preferred.hourly.any { it.precipitationProbability == null }
            val withProbabilities = if (needsProbabilityFallback) {
                request(location, useJmaModel = false)?.let { preferred.withProbabilityFallback(it) } ?: preferred
            } else {
                preferred
            }
            withProbabilities.copy(usedFallbackModel = false)
        } else {
            request(location, useJmaModel = false)?.copy(usedFallbackModel = true)
                ?: throw IOException("Open-Meteo forecast request failed")
        }
    }

    suspend fun searchLocations(query: String): List<WeatherLocation> = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        if (normalized.length < 2) return@withContext emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", normalized)
            .addQueryParameter("count", "10")
            .addQueryParameter("language", "ja")
            .addQueryParameter("format", "json")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PersonalWeather/1.0")
            .build()
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                val body = response.body?.string().orEmpty()
                json.decodeFromString<GeocodingResponse>(body).results.map { it.toWeatherLocation() }
            }
        }.getOrDefault(emptyList())
    }

    private fun request(location: WeatherLocation, useJmaModel: Boolean): WeatherSnapshot? {
        val builder = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", location.latitude.toString())
            .addQueryParameter("longitude", location.longitude.toString())
            .addQueryParameter("current", "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,precipitation,wind_speed_10m,wind_direction_10m,pressure_msl")
            .addQueryParameter("hourly", "temperature_2m,precipitation_probability,weather_code,precipitation")
            .addQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,precipitation_sum,uv_index_max,sunrise,sunset")
            .addQueryParameter("forecast_days", "14")
            .addQueryParameter("timezone", "Asia/Tokyo")
        if (useJmaModel) builder.addQueryParameter("models", "jma_seamless")

        val request = Request.Builder()
            .url(builder.build())
            .header("User-Agent", "PersonalWeather/1.0")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                json.decodeFromString<OpenMeteoResponse>(body).toSnapshot(location)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun OpenMeteoResponse.toSnapshot(location: WeatherLocation): WeatherSnapshot {
        val hourlyItems = hourly?.time.orEmpty().mapIndexed { index, time ->
            HourlyWeather(
                time = time,
                temperatureC = hourly?.temperature?.getOrNull(index),
                precipitationProbability = hourly?.precipitationProbability?.getOrNull(index),
                weatherCode = hourly?.weatherCode?.getOrNull(index),
                precipitationMm = hourly?.precipitation?.getOrNull(index),
            )
        }
        val dailyItems = daily?.time.orEmpty().mapIndexed { index, date ->
            DailyWeather(
                date = date,
                weatherCode = daily?.weatherCode?.getOrNull(index),
                maxTemperatureC = daily?.maxTemperature?.getOrNull(index),
                minTemperatureC = daily?.minTemperature?.getOrNull(index),
                maxPrecipitationProbability = daily?.maxPrecipitationProbability?.getOrNull(index),
                precipitationSumMm = daily?.precipitationSum?.getOrNull(index),
                uvIndexMax = daily?.uvIndexMax?.getOrNull(index),
                sunrise = daily?.sunrise?.getOrNull(index),
                sunset = daily?.sunset?.getOrNull(index),
            )
        }
        return WeatherSnapshot(
            location = location,
            current = CurrentWeather(
                temperatureC = current?.temperature,
                apparentTemperatureC = current?.apparentTemperature,
                humidityPercent = current?.humidity,
                weatherCode = current?.weatherCode,
                precipitationMm = current?.precipitation,
                windSpeedKmh = current?.windSpeed,
                windDirectionDeg = current?.windDirection,
                pressureHpa = current?.pressure,
                time = current?.time,
            ),
            hourly = hourlyItems,
            daily = dailyItems,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun WeatherSnapshot.withProbabilityFallback(fallback: WeatherSnapshot): WeatherSnapshot {
        val fallbackHourly = fallback.hourly.associateBy { it.time }
        val fallbackDaily = fallback.daily.associateBy { it.date }
        return copy(
            hourly = hourly.map { hour ->
                hour.copy(
                    precipitationProbability = hour.precipitationProbability
                        ?: fallbackHourly[hour.time]?.precipitationProbability,
                )
            },
            daily = daily.map { day ->
                day.copy(
                    maxPrecipitationProbability = day.maxPrecipitationProbability
                        ?: fallbackDaily[day.date]?.maxPrecipitationProbability,
                    precipitationSumMm = day.precipitationSumMm
                        ?: fallbackDaily[day.date]?.precipitationSumMm,
                    uvIndexMax = day.uvIndexMax ?: fallbackDaily[day.date]?.uvIndexMax,
                    sunrise = day.sunrise ?: fallbackDaily[day.date]?.sunrise,
                    sunset = day.sunset ?: fallbackDaily[day.date]?.sunset,
                )
            },
        )
    }
}
