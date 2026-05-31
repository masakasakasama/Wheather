package com.example.weather

import android.app.Application
import com.example.weather.worker.WeatherRefreshWorker

class WeatherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppServices.init(this)
        WeatherRefreshWorker.enqueue(this)
    }
}
