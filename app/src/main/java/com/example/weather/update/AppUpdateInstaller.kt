package com.example.weather.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.weather.data.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class AppUpdateInstaller(
    private val context: Context,
    private val httpClient: OkHttpClient,
) {
    suspend fun downloadAndOpenInstaller(info: AppUpdateInfo) {
        val apkFile = withContext(Dispatchers.IO) {
            val updatesDir = File(context.cacheDir, "updates").also { it.mkdirs() }
            val output = File(updatesDir, "PersonalWeather-${info.versionCode}.apk")
            val request = Request.Builder().url(info.apkUrl).build()
            httpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "APK download failed: ${response.code}" }
                response.body?.byteStream()?.use { input ->
                    output.outputStream().use { fileOutput -> input.copyTo(fileOutput) }
                } ?: error("APK body is empty")
            }
            output
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
