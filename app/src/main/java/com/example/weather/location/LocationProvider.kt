package com.example.weather.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.weather.data.model.PresetLocations
import com.example.weather.data.model.WeatherLocation

class LocationProvider(private val context: Context) {
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun currentOrDefault(): WeatherLocation {
        if (!hasLocationPermission()) return PresetLocations.first()
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = manager.getBestLastKnownLocation() ?: return PresetLocations.first()
        return WeatherLocation("現在地", location.latitude, location.longitude)
    }

    private fun LocationManager.getBestLastKnownLocation(): Location? {
        return getProviders(true)
            .mapNotNull { provider -> runCatching { getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }
}
