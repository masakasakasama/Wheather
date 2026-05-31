package com.example.weather.data.api

import com.example.weather.data.model.CurrentWeather
import com.example.weather.data.model.DailyWeather
import com.example.weather.data.model.HourlyWeather
import com.example.weather.data.model.OpenMeteoResponse
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
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
            preferred.copy(usedFallbackModel = false)
        } else {
            request(location, useJmaModel = false)?.copy(usedFallbackModel = true)
                ?: throw IOException("Open-Meteo forecast request failed")
        }
    }

    private fun request(location: WeatherLocation, useJmaModel: Boolean): WeatherSnapshot? {
        val builder = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", location.latitude.toString())
            .addQueryParameter("longitude", location.longitude.toString())
            .addQueryParameter("current", "temperature_2m,weather_code,precipitation")
            .addQueryParameter("hourly", "temperature_2m,precipitation_probability,weather_code,precipitation")
            .addQueryParameter("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
            .addQueryParameter("forecast_days", "7")
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
            )
        }
        return WeatherSnapshot(
            location = location,
            current = CurrentWeather(
                temperatureC = current?.temperature,
                weatherCode = current?.weatherCode,
                precipitationMm = current?.precipitation,
                time = current?.time,
            ),
            hourly = hourlyItems,
            daily = dailyItems,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }
}
