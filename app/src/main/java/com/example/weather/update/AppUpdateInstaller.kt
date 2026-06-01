package com.example.weather.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            openInstallPermissionSettings()
            error("APK更新には「この提供元のアプリを許可」をオンにしてください。オンにした後、もう一度更新してください。")
        }

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
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        }
        context.startActivity(intent)
    }

    private fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    companion object {
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
