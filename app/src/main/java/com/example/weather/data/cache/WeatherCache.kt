package com.example.weather.data.cache

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.weather.data.model.PresetLocations
import com.example.weather.data.model.WeatherLocation
import com.example.weather.data.model.WeatherSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.weatherDataStore by preferencesDataStore(name = "weather")

class WeatherCache(
    private val context: Context,
    private val json: Json,
) {
    private val snapshotKey = stringPreferencesKey("weather_snapshot")
    private val locationKey = stringPreferencesKey("selected_location")

    val snapshot: Flow<WeatherSnapshot?> = context.weatherDataStore.data.map { preferences ->
        preferences[snapshotKey]?.let { runCatching { json.decodeFromString<WeatherSnapshot>(it) }.getOrNull() }
    }

    val selectedLocation: Flow<WeatherLocation> = context.weatherDataStore.data.map { preferences ->
        preferences[locationKey]?.let { runCatching { json.decodeFromString<WeatherLocation>(it) }.getOrNull() }
            ?: PresetLocations.first()
    }

    suspend fun readSnapshotOnce(): WeatherSnapshot? = snapshot.first()

    suspend fun readLocationOnce(): WeatherLocation = selectedLocation.first()

    suspend fun saveSnapshot(snapshot: WeatherSnapshot) {
        context.weatherDataStore.edit { preferences ->
            preferences[snapshotKey] = json.encodeToString(snapshot)
        }
    }

    suspend fun saveLocation(location: WeatherLocation) {
        context.weatherDataStore.edit { preferences ->
            preferences[locationKey] = json.encodeToString(location)
        }
    }
}
