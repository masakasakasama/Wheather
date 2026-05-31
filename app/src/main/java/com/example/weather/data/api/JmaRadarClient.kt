package com.example.weather.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.weather.data.model.RadarFrame
import com.example.weather.data.model.RadarTargetTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

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
}
