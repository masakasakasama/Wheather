package com.example.weather.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.glance.appwidget.updateAll
import com.example.weather.AppServices
import com.example.weather.widget.WeatherWidget
import java.util.concurrent.TimeUnit

class WeatherRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        AppServices.init(applicationContext)
        val result = AppServices.repository.refresh()
        val snapshot = result.getOrNull()
        val disasterSummary = snapshot?.let {
            AppServices.disasterClient.fetchSummary(it.location).getOrNull()
        }
        if (snapshot != null) {
            val settings = AppServices.cache.readNotificationSettingsOnce()
            AppServices.notificationCenter.notifyWeatherEvents(snapshot, disasterSummary, settings)
        }
        WeatherWidget().updateAll(applicationContext)
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_NAME = "weather_refresh"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
