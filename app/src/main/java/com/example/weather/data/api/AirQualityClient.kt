package com.example.weather.data.api

import com.example.weather.data.model.AirQuality
import com.example.weather.data.model.HourlyAirQuality
import com.example.weather.data.model.OpenMeteoAirQualityResponse
import com.example.weather.data.model.WeatherLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class AirQualityClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun fetchAirQuality(location: WeatherLocation): AirQuality? = withContext(Dispatchers.IO) {
        val url = "https://air-quality-api.open-meteo.com/v1/air-quality".toHttpUrl().newBuilder()
            .addQueryParameter("latitude", location.latitude.toString())
            .addQueryParameter("longitude", location.longitude.toString())
            .addQueryParameter("current", "european_aqi,us_aqi,pm10,pm2_5,nitrogen_dioxide,ozone")
            .addQueryParameter("hourly", "european_aqi,pm2_5,pm10,uv_index")
            .addQueryParameter("forecast_days", "2")
            .addQueryParameter("timezone", "Asia/Tokyo")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PersonalWeather/1.0")
            .build()

        var result: AirQuality? = null
        repeat(2) {
            if (result != null) return@repeat
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (body != null) {
                            result = json.decodeFromString<OpenMeteoAirQualityResponse>(body).toAirQuality()
                        }
                    }
                }
            } catch (_: Exception) {
                // Air quality is optional; retry once, then omit it from the snapshot.
            }
        }
        result
    }

    private fun OpenMeteoAirQualityResponse.toAirQuality(): AirQuality {
        val hourlyItems = hourly?.time.orEmpty().mapIndexed { index, time ->
            HourlyAirQuality(
                time = time,
                europeanAqi = hourly?.europeanAqi?.getOrNull(index),
                pm25 = hourly?.pm25?.getOrNull(index),
                pm10 = hourly?.pm10?.getOrNull(index),
                uvIndex = hourly?.uvIndex?.getOrNull(index),
            )
        }
        return AirQuality(
            time = current?.time,
            europeanAqi = current?.europeanAqi,
            usAqi = current?.usAqi,
            pm10 = current?.pm10,
            pm25 = current?.pm25,
            nitrogenDioxide = current?.nitrogenDioxide,
            ozone = current?.ozone,
            hourly = hourlyItems,
        )
    }
}
