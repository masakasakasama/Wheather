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
    private val savedLocationsKey = stringPreferencesKey("saved_locations")

    val snapshot: Flow<WeatherSnapshot?> = context.weatherDataStore.data.map { preferences ->
        preferences[snapshotKey]?.let { runCatching { json.decodeFromString<WeatherSnapshot>(it) }.getOrNull() }
    }

    val selectedLocation: Flow<WeatherLocation> = context.weatherDataStore.data.map { preferences ->
        preferences[locationKey]?.let { runCatching { json.decodeFromString<WeatherLocation>(it) }.getOrNull() }
            ?: PresetLocations.first()
    }

    val savedLocations: Flow<List<WeatherLocation>> = context.weatherDataStore.data.map { preferences ->
        preferences[savedLocationsKey]
            ?.let { runCatching { json.decodeFromString<List<WeatherLocation>>(it) }.getOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: PresetLocations
    }

    suspend fun readSnapshotOnce(): WeatherSnapshot? = snapshot.first()

    suspend fun readLocationOnce(): WeatherLocation = selectedLocation.first()

    suspend fun readSavedLocationsOnce(): List<WeatherLocation> = savedLocations.first()

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

    suspend fun saveLocations(locations: List<WeatherLocation>) {
        context.weatherDataStore.edit { preferences ->
            preferences[savedLocationsKey] = json.encodeToString(locations.distinctBy { it.identityKey() })
        }
    }
}

fun WeatherLocation.identityKey(): String = "${latitude.formatKey()},${longitude.formatKey()}"

private fun Double.formatKey(): String = "%.4f".format(this)
