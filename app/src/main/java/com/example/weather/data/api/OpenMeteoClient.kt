package com.example.weather.data.api

import com.example.weather.data.model.CurrentWeather
import com.example.weather.data.model.DailyWeather
import com.example.weather.data.model.GeocodingResponse
import com.example.weather.data.model.HourlyWeather
import com.example.weather.data.model.MinutelyWeather
import com.example.weather.data.model.ModelTemperaturePoint
import com.example.weather.data.model.ModelTemperatureSeries
import com.example.weather.data.model.MultiModelTemperatureForecast
import com.example.weather.data.model.OpenMeteoResponse
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import com.example.weather.data.model.enforcePrecipitationConsistency
import com.example.weather.data.model.identityKey
import com.example.weather.data.model.isInJapan
import com.example.weather.data.model.toWeatherLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class OpenMeteoClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun fetchForecast(location: WeatherLocation): WeatherSnapshot = withContext(Dispatchers.IO) {
        if (location.isInJapan()) {
            val bestMatch = request(
                location = location,
                modelId = null,
                forecastDays = 14,
                includeMinutely15 = true,
                includePrecipitationProbability = true,
                includeUv = true,
            )
            val jmaPrimary = request(
                location = location,
                modelId = "jma_seamless",
                forecastDays = 11,
                includeMinutely15 = false,
                includePrecipitationProbability = false,
                includeUv = false,
            )

            when {
                jmaPrimary != null -> mergeJapanForecast(jmaPrimary, bestMatch).copy(
                    usedFallbackModel = false,
                    forecastSource = "JMA Seamless（MSM/GSM）主軸 + Best Match（15分・長期補完）",
                )
                bestMatch != null -> bestMatch.copy(
                    usedFallbackModel = true,
                    forecastSource = "Open-Meteo Best Match（JMA取得失敗時の予備）",
                )
                else -> throw IOException("Open-Meteo forecast request failed")
            }
        } else {
            request(
                location = location,
                modelId = null,
                forecastDays = 14,
                includeMinutely15 = true,
                includePrecipitationProbability = true,
                includeUv = true,
            )?.copy(
                usedFallbackModel = false,
                forecastSource = "Open-Meteo Best Match",
            ) ?: throw IOException("Open-Meteo forecast request failed")
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
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Open-Meteo geocoding request failed: HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Open-Meteo geocoding response was empty")
            json.decodeFromString<GeocodingResponse>(body)
                .results
                .map { it.toWeatherLocation() }
                .distinctBy { it.identityKey() }
        }
    }

    suspend fun fetchTemperatureModels(location: WeatherLocation): MultiModelTemperatureForecast =
        withContext(Dispatchers.IO) {
            val url = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
                .addQueryParameter("latitude", location.latitude.toString())
                .addQueryParameter("longitude", location.longitude.toString())
                .addQueryParameter("hourly", "temperature_2m")
                .addQueryParameter("models", TEMPERATURE_MODELS.joinToString(",") { it.apiId })
                .addQueryParameter("forecast_days", "14")
                .addQueryParameter("past_days", "1")
                .addQueryParameter("timezone", "auto")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PersonalWeather/1.0")
                .build()

            var failure: Exception? = null
            repeat(2) {
                try {
                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            failure = IOException("Open-Meteo multi-model request failed: HTTP ${response.code}")
                        } else {
                            val body = response.body?.string()
                                ?: throw IOException("Open-Meteo multi-model response was empty")
                            return@withContext parseTemperatureModels(body)
                        }
                    }
                } catch (error: Exception) {
                    failure = error
                }
            }
            throw failure ?: IOException("Open-Meteo multi-model request failed")
        }

    private fun request(
        location: WeatherLocation,
        modelId: String?,
        forecastDays: Int,
        includeMinutely15: Boolean,
        includePrecipitationProbability: Boolean,
        includeUv: Boolean,
    ): WeatherSnapshot? {
        val hourlyVariables = buildList {
            add("temperature_2m")
            if (includePrecipitationProbability) add("precipitation_probability")
            add("weather_code")
            add("precipitation")
            add("relative_humidity_2m")
            add("wind_speed_10m")
            add("wind_direction_10m")
        }.joinToString(",")
        val dailyVariables = buildList {
            add("weather_code")
            add("temperature_2m_max")
            add("temperature_2m_min")
            if (includePrecipitationProbability) add("precipitation_probability_max")
            add("precipitation_sum")
            if (includeUv) add("uv_index_max")
            add("sunrise")
            add("sunset")
        }.joinToString(",")

        val builder = "https://api.open-meteo.com/v1/forecast".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", location.latitude.toString())
            .addQueryParameter("longitude", location.longitude.toString())
            .addQueryParameter("current", "temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,precipitation,wind_speed_10m,wind_direction_10m,pressure_msl")
            .addQueryParameter("hourly", hourlyVariables)
            .addQueryParameter("daily", dailyVariables)
            .addQueryParameter("forecast_days", forecastDays.toString())
            .addQueryParameter("past_days", "1")
            .addQueryParameter("timezone", "auto")
        if (includeMinutely15) {
            builder
                .addQueryParameter("minutely_15", "temperature_2m,precipitation_probability,weather_code,precipitation")
                .addQueryParameter("forecast_minutely_15", "16")
        }
        if (modelId != null) builder.addQueryParameter("models", modelId)

        val request = Request.Builder()
            .url(builder.build())
            .header("User-Agent", "PersonalWeather/1.0")
            .build()

        repeat(2) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) return json.decodeFromString<OpenMeteoResponse>(body).toSnapshot(location)
                    }
                }
            } catch (_: Exception) {
                // Retry once; final failure falls back to the other provider/cached snapshot.
            }
        }
        return null
    }

    private fun mergeJapanForecast(
        jma: WeatherSnapshot,
        bestMatch: WeatherSnapshot?,
    ): WeatherSnapshot {
        val fallback = bestMatch ?: return jma
        val fallbackHourly = fallback.hourly.associateBy { it.time }
        val jmaHourly = jma.hourly.associateBy { it.time }
        val mergedHourly = (jmaHourly.keys + fallbackHourly.keys)
            .sorted()
            .mapNotNull { time ->
                val primary = jmaHourly[time]
                val secondary = fallbackHourly[time]
                when {
                    primary != null -> primary.copy(
                        temperatureC = primary.temperatureC ?: secondary?.temperatureC,
                        precipitationProbability = primary.precipitationProbability
                            ?: secondary?.precipitationProbability,
                        weatherCode = primary.weatherCode ?: secondary?.weatherCode,
                        precipitationMm = primary.precipitationMm ?: secondary?.precipitationMm,
                        humidityPercent = primary.humidityPercent ?: secondary?.humidityPercent,
                        windSpeedKmh = primary.windSpeedKmh ?: secondary?.windSpeedKmh,
                        windDirectionDeg = primary.windDirectionDeg ?: secondary?.windDirectionDeg,
                    )
                    secondary != null -> secondary
                    else -> null
                }
            }

        val fallbackDaily = fallback.daily.associateBy { it.date }
        val jmaDaily = jma.daily.associateBy { it.date }
        val mergedDaily = (jmaDaily.keys + fallbackDaily.keys)
            .sorted()
            .mapNotNull { date ->
                val primary = jmaDaily[date]
                val secondary = fallbackDaily[date]
                when {
                    primary != null -> primary.copy(
                        weatherCode = primary.weatherCode ?: secondary?.weatherCode,
                        maxTemperatureC = primary.maxTemperatureC ?: secondary?.maxTemperatureC,
                        minTemperatureC = primary.minTemperatureC ?: secondary?.minTemperatureC,
                        maxPrecipitationProbability = primary.maxPrecipitationProbability
                            ?: secondary?.maxPrecipitationProbability,
                        precipitationSumMm = primary.precipitationSumMm ?: secondary?.precipitationSumMm,
                        uvIndexMax = primary.uvIndexMax ?: secondary?.uvIndexMax,
                        sunrise = primary.sunrise ?: secondary?.sunrise,
                        sunset = primary.sunset ?: secondary?.sunset,
                    )
                    secondary != null -> secondary
                    else -> null
                }
            }

        val primaryCurrent = jma.current
        val secondaryCurrent = fallback.current
        val mergedCurrent = primaryCurrent.copy(
            temperatureC = primaryCurrent.temperatureC ?: secondaryCurrent.temperatureC,
            apparentTemperatureC = primaryCurrent.apparentTemperatureC ?: secondaryCurrent.apparentTemperatureC,
            humidityPercent = primaryCurrent.humidityPercent ?: secondaryCurrent.humidityPercent,
            weatherCode = primaryCurrent.weatherCode ?: secondaryCurrent.weatherCode,
            precipitationMm = primaryCurrent.precipitationMm ?: secondaryCurrent.precipitationMm,
            windSpeedKmh = primaryCurrent.windSpeedKmh ?: secondaryCurrent.windSpeedKmh,
            windDirectionDeg = primaryCurrent.windDirectionDeg ?: secondaryCurrent.windDirectionDeg,
            pressureHpa = primaryCurrent.pressureHpa ?: secondaryCurrent.pressureHpa,
            time = primaryCurrent.time ?: secondaryCurrent.time,
            modelTemperatureC = primaryCurrent.modelTemperatureC ?: secondaryCurrent.modelTemperatureC,
        )

        return jma.copy(
            current = mergedCurrent,
            minutely15 = fallback.minutely15.ifEmpty { jma.minutely15 },
            hourly = mergedHourly,
            daily = mergedDaily,
            updatedAtMillis = maxOf(jma.updatedAtMillis, fallback.updatedAtMillis),
            timezone = jma.timezone.ifBlank { fallback.timezone },
        ).enforcePrecipitationConsistency()
    }

    private fun OpenMeteoResponse.toSnapshot(location: WeatherLocation): WeatherSnapshot {
        val minutelyItems = minutely15?.time.orEmpty().mapIndexed { index, time ->
            MinutelyWeather(
                time = time,
                temperatureC = minutely15?.temperature?.getOrNull(index),
                precipitationProbability = minutely15?.precipitationProbability?.getOrNull(index),
                weatherCode = minutely15?.weatherCode?.getOrNull(index),
                precipitationMm = minutely15?.precipitation?.getOrNull(index),
            )
        }
        val hourlyItems = hourly?.time.orEmpty().mapIndexed { index, time ->
            HourlyWeather(
                time = time,
                temperatureC = hourly?.temperature?.getOrNull(index),
                precipitationProbability = hourly?.precipitationProbability?.getOrNull(index),
                weatherCode = hourly?.weatherCode?.getOrNull(index),
                precipitationMm = hourly?.precipitation?.getOrNull(index),
                humidityPercent = hourly?.humidity?.getOrNull(index),
                windSpeedKmh = hourly?.windSpeed?.getOrNull(index),
                windDirectionDeg = hourly?.windDirection?.getOrNull(index),
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
                modelTemperatureC = current?.temperature,
            ),
            minutely15 = minutelyItems,
            hourly = hourlyItems,
            daily = dailyItems,
            updatedAtMillis = System.currentTimeMillis(),
            timezone = timezone ?: "Asia/Tokyo",
        ).enforcePrecipitationConsistency()
    }

    private fun parseTemperatureModels(body: String): MultiModelTemperatureForecast {
        val hourly = json.parseToJsonElement(body).jsonObject["hourly"]?.jsonObject
            ?: throw IOException("Open-Meteo multi-model hourly data was missing")
        val times = hourly["time"]?.jsonArray?.map { it.jsonPrimitive.content }
            ?: throw IOException("Open-Meteo multi-model times were missing")
        val models = TEMPERATURE_MODELS.mapNotNull { model ->
            val values = hourly["temperature_2m_${model.apiId}"]
                ?.jsonArray
                ?.map { element -> element.jsonPrimitive.doubleOrNull }
                ?: return@mapNotNull null
            val points = times.mapIndexed { index, time ->
                ModelTemperaturePoint(time = time, temperatureC = values.getOrNull(index))
            }
            points.takeIf { items -> items.any { it.temperatureC != null } }?.let {
                ModelTemperatureSeries(model.apiId, model.displayName, it)
            }
        }
        if (models.isEmpty()) throw IOException("Open-Meteo returned no temperature models")
        return MultiModelTemperatureForecast(models, System.currentTimeMillis())
    }

    private data class TemperatureModelDefinition(
        val apiId: String,
        val displayName: String,
    )

    private companion object {
        val TEMPERATURE_MODELS = listOf(
            TemperatureModelDefinition("jma_seamless", "JMA"),
            TemperatureModelDefinition("ecmwf_ifs025", "ECMWF"),
            TemperatureModelDefinition("gfs_seamless", "GFS"),
        )
    }
}
