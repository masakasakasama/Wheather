package com.example.weather.data.api

import com.example.weather.data.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class AppUpdateClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun checkForUpdate(currentVersionCode: Int): Result<AppUpdateInfo?> = runCatching {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(VERSION_JSON_URL)
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string().orEmpty()
                val info = json.decodeFromString<AppUpdateInfo>(body)
                info.takeIf { it.versionCode > currentVersionCode }
            }
        }
    }

    companion object {
        private const val VERSION_JSON_URL =
            "https://github.com/masakasakasama/Wheather/releases/download/latest-debug/version.json"
    }
}
