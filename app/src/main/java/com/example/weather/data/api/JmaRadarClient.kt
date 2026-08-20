package com.example.weather.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.weather.data.model.RadarFrame
import com.example.weather.data.model.RadarPixelSample
import com.example.weather.data.model.RadarPrecipitation
import com.example.weather.data.model.RadarTargetTime
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.isInJapan
import com.example.weather.data.model.radarIntensityLowerBound
import com.example.weather.data.model.representativeRadarIntensity
import com.example.weather.data.model.toRadarEpochMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
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
            val latest = json.decodeFromString(ListSerializer(RadarTargetTime.serializer()), body)
                .maxByOrNull { it.validtime }
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
        if (!location.isInJapan()) return@withContext null
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

        val samples = mutableListOf<RadarPixelSample>()
        val radius = 1
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val x = pixelX + dx
                val y = pixelY + dy
                if (x !in 0 until bitmap.width || y !in 0 until bitmap.height) continue
                val color = bitmap.getPixel(x, y)
                samples += RadarPixelSample(
                    dx = dx,
                    dy = dy,
                    intensityLowerBoundMmPerHour = radarIntensityLowerBound(
                        alpha = color ushr 24 and 0xFF,
                        red = color ushr 16 and 0xFF,
                        green = color ushr 8 and 0xFF,
                        blue = color and 0xFF,
                    ),
                )
            }
        }
        bitmap.recycle()

        val representativeIntensity = representativeRadarIntensity(samples)
            ?: throw IOException("JMA radar tile color coverage was insufficient")
        RadarPrecipitation(
            intensityLowerBoundMmPerHour = representativeIntensity,
            observedAtMillis = frame.validTime.toRadarEpochMillis(),
        )
    }
}
