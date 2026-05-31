package com.example.weather

import android.content.Context
import com.example.weather.data.api.AppUpdateClient
import com.example.weather.data.api.JmaRadarClient
import com.example.weather.data.api.OpenMeteoClient
import com.example.weather.data.cache.WeatherCache
import com.example.weather.data.repository.WeatherRepository
import com.example.weather.update.AppUpdateInstaller
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object AppServices {
    lateinit var cache: WeatherCache
        private set
    lateinit var repository: WeatherRepository
        private set
    lateinit var radarClient: JmaRadarClient
        private set
    lateinit var updateClient: AppUpdateClient
        private set
    lateinit var updateInstaller: AppUpdateInstaller
        private set

    fun init(context: Context) {
        if (::repository.isInitialized) return
        val appContext = context.applicationContext
        val json = Json {
            ignoreUnknownKeys = true
        }
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .build()
        cache = WeatherCache(appContext, json)
        val openMeteoClient = OpenMeteoClient(httpClient, json)
        repository = WeatherRepository(appContext, openMeteoClient, cache)
        radarClient = JmaRadarClient(httpClient, json)
        updateClient = AppUpdateClient(httpClient, json)
        updateInstaller = AppUpdateInstaller(appContext, httpClient)
    }
}
