package com.example.weather.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.weather.data.model.RadarFrame
import com.example.weather.data.model.RadarPrecipitation
import com.example.weather.data.model.RadarTargetTime
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.radarIntensityLowerBound
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.asinh
import kotlin.math.floor
import kotlin.math.tan

class JmaRadarClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun latestFrame(): RadarFrame = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://www.jma.go.jp/bosai/jmatile/data/nowc/targetTimes_N1.json")
            .header("User-Agent", "PersonalWeather/1.0")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("JMA radar target time request failed")
            val body = response.body?.string().orEmpty()
            val latest = json.decodeFromString(ListSerializer(RadarTargetTime.serializer()), body).firstOrNull()
                ?: throw IOException("JMA radar target time is empty")
            RadarFrame(
                baseTime = latest.basetime,
                validTime = latest.validtime,
                tileTemplate = "https://www.jma.go.jp/bosai/jmatile/data/nowc/${latest.basetime}/none/${latest.validtime}/surf/hrpns/{z}/{x}/{y}.png",
            )
        }
    }

    suspend fun fetchBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "PersonalWeather/1.0")
            .build()
        runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body?.byteStream()?.use(BitmapFactory::decodeStream)
            }
        }.getOrNull()
    }

    suspend fun latestPrecipitation(location: WeatherLocation): RadarPrecipitation? = withContext(Dispatchers.IO) {
        if (location.latitude !in 20.0..48.0 || location.longitude !in 118.0..150.0) {
            return@withContext null
        }
        val frame = latestFrame()
        val zoom = 10
        val tileCount = 1 shl zoom
        val xPosition = (location.longitude + 180.0) / 360.0 * tileCount
        val latitudeRadians = location.latitude * PI / 180.0
        val yPosition = (1.0 - asinh(tan(latitudeRadians)) / PI) / 2.0 * tileCount
        val tileX = floor(xPosition).toInt()
        val tileY = floor(yPosition).toInt()
        val pixelX = floor((xPosition - tileX) * 256.0).toInt()
        val pixelY = floor((yPosition - tileY) * 256.0).toInt()
        val radarUrl = frame.tileTemplate
            .replace("{z}", zoom.toString())
            .replace("{x}", tileX.toString())
            .replace("{y}", tileY.toString())
        val bitmap = fetchBitmap(radarUrl) ?: throw IOException("JMA radar tile request failed")
        val radius = 3
        var strongestIntensity = 0.0
        var recognizedPixel = false
        for (y in (pixelY - radius).coerceAtLeast(0)..(pixelY + radius).coerceAtMost(bitmap.height - 1)) {
            for (x in (pixelX - radius).coerceAtLeast(0)..(pixelX + radius).coerceAtMost(bitmap.width - 1)) {
                val color = bitmap.getPixel(x, y)
                val intensity = radarIntensityLowerBound(
                    alpha = color ushr 24 and 0xFF,
                    red = color ushr 16 and 0xFF,
                    green = color ushr 8 and 0xFF,
                    blue = color and 0xFF,
                )
                if (intensity != null) {
                    recognizedPixel = true
                    strongestIntensity = maxOf(strongestIntensity, intensity)
                }
            }
        }
        bitmap.recycle()
        if (!recognizedPixel) throw IOException("JMA radar tile color was not recognized")
        RadarPrecipitation(
            intensityLowerBoundMmPerHour = strongestIntensity,
            observedAtMillis = frame.validTime.toEpochMillis(),
        )
    }

    private fun String.toEpochMillis(): Long {
        return LocalDateTime.parse(this, RadarTimeFormatter)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
    }

    private companion object {
        val RadarTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }
}
