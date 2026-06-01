package com.example.weather.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.weather.MainActivity
import com.example.weather.R
import com.example.weather.data.model.DisasterSummary
import com.example.weather.data.model.HourlyWeather
import com.example.weather.data.model.NotificationSettings
import com.example.weather.data.model.WeatherSnapshot
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WeatherNotificationCenter(
    private val context: Context,
) {
    private val prefs = context.getSharedPreferences("weather_notifications", Context.MODE_PRIVATE)

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_WEATHER,
            "天気通知",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "雨の接近、警報・注意報、台風などを通知します。"
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyWeatherEvents(
        snapshot: WeatherSnapshot,
        disasterSummary: DisasterSummary?,
        settings: NotificationSettings,
    ) {
        if (!canNotify()) return
        ensureChannels()
        if (settings.rainNotificationsEnabled) notifyRainIfNeeded(snapshot, settings)
        if (settings.disasterNotificationsEnabled) notifyDisasterIfNeeded(disasterSummary)
    }

    private fun notifyRainIfNeeded(snapshot: WeatherSnapshot, settings: NotificationSettings) {
        val rainHour = snapshot.hourly.nextNotificationHours(settings.rainLookAheadHours).firstOrNull {
            (it.precipitationProbability ?: 0) >= settings.rainProbabilityThreshold ||
                (it.precipitationMm ?: 0.0) >= settings.rainAmountThresholdMm
        } ?: return
        val signature = "${rainHour.time}:${rainHour.precipitationProbability}:${rainHour.precipitationMm}"
        if (!shouldNotify("rain_signature", signature)) return

        val probability = rainHour.precipitationProbability?.let { "$it%" } ?: "--%"
        val precipitation = rainHour.precipitationMm?.let { "%.1fmm".format(it) } ?: "--mm"
        show(
            id = NOTIFICATION_RAIN,
            title = "雨が近いです",
            text = "${formatDateHourLabel(rainHour.time)}ごろ 降水確率 $probability / $precipitation",
        )
    }

    private fun notifyDisasterIfNeeded(summary: DisasterSummary?) {
        if (summary?.hasImportantInfo != true) return
        val signature = listOf(
            summary.officeName.orEmpty(),
            summary.warningHeadline.orEmpty(),
            summary.activeWarnings.joinToString("|"),
            summary.typhoons.joinToString("|") { "${it.number}:${it.category}" },
        ).joinToString("#")
        if (!shouldNotify("disaster_signature", signature)) return

        val warningText = summary.activeWarnings.take(3).joinToString(" / ")
        val typhoonText = summary.typhoons.take(2).joinToString(" / ") { "台風${it.number}号 ${it.category}" }
        val text = listOf(warningText, typhoonText, summary.warningHeadline)
            .filterNot { it.isNullOrBlank() }
            .joinToString(" / ")
            .ifBlank { "気象庁の重要情報があります。" }
        show(
            id = NOTIFICATION_DISASTER,
            title = "重要な気象情報",
            text = "${summary.officeName ?: "現在地周辺"}: $text",
        )
    }

    private fun shouldNotify(key: String, signature: String): Boolean {
        if (prefs.getString(key, null) == signature) return false
        prefs.edit().putString(key, signature).apply()
        return true
    }

    private fun show(id: Int, title: String, text: String) {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_WEATHER)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun canNotify(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun List<HourlyWeather>.nextNotificationHours(count: Int): List<HourlyWeather> {
        val now = LocalDateTime.now(ZoneId.of("Asia/Tokyo")).withMinute(0).withSecond(0).withNano(0)
        return filter { hour ->
            runCatching { !LocalDateTime.parse(hour.time).isBefore(now) }.getOrDefault(false)
        }.take(count)
    }

    private fun formatDateHourLabel(time: String): String {
        val parsed = runCatching { LocalDateTime.parse(time) }.getOrNull() ?: return "--"
        val hour = parsed.hour
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val period = if (hour < 12) "AM" else "PM"
        return "${parsed.format(DateTimeFormatter.ofPattern("M/d"))} $period ${displayHour}時"
    }

    companion object {
        private const val CHANNEL_WEATHER = "weather_alerts"
        private const val NOTIFICATION_RAIN = 1001
        private const val NOTIFICATION_DISASTER = 1002
    }
}
