package com.example.weather.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeatherLocationTest {
    @Test
    fun deviceLocationsAreTheSameSavedPlaceDespiteGpsMovement() {
        val first = WeatherLocation("現在地", 35.4000, 139.3000)
        val moved = WeatherLocation("現在地", 35.7000, 139.8000)

        assertTrue(first.sameSavedPlaceAs(moved))
    }

    @Test
    fun canonicalizationKeepsOneDeviceLocationWithNewestCoordinates() {
        val tokyo = WeatherLocation("東京", 35.6764, 139.6500)
        val first = WeatherLocation("現在地", 35.4000, 139.3000)
        val latest = WeatherLocation("現在地", 35.7000, 139.8000)

        val result = listOf(tokyo, first, latest).canonicalizedSavedLocations()

        assertEquals(2, result.size)
        assertEquals(tokyo, result[0])
        assertEquals(latest, result[1])
    }

    @Test
    fun namedLocationsAtDifferentCoordinatesRemainSeparate() {
        val station = WeatherLocation("東京", 35.6812, 139.7671)
        val city = WeatherLocation("東京", 35.6764, 139.6500)

        assertFalse(station.sameSavedPlaceAs(city))
        assertEquals(2, listOf(station, city).canonicalizedSavedLocations().size)
    }

    @Test
    fun exactNamedLocationDuplicatesAreRemoved() {
        val berlin = WeatherLocation("ベルリン", 52.5244, 13.4105)

        assertEquals(1, listOf(berlin, berlin).canonicalizedSavedLocations().size)
    }

    @Test
    fun notificationAreasIgnoreGpsJitterButDistinguishCities() {
        val first = WeatherLocation("現在地", 35.6812, 139.7671)
        val jittered = WeatherLocation("現在地", 35.6813, 139.7672)
        val osaka = WeatherLocation("現在地", 34.6937, 135.5023)

        assertEquals(first.forecastAreaKey(), jittered.forecastAreaKey())
        assertFalse(first.forecastAreaKey() == osaka.forecastAreaKey())
    }

    @Test
    fun widgetForecastMustMatchTheConfiguredLocation() {
        val berlin = WeatherLocation("ベルリン", 52.5244, 13.4105)
        val osaka = WeatherLocation("大阪", 34.6937, 135.5023)

        assertTrue(berlin.sameForecastPlaceAs(berlin.copy()))
        assertFalse(berlin.sameForecastPlaceAs(osaka))
    }

    @Test
    fun deviceForecastAllowsGpsJitterButNotAChangedArea() {
        val first = WeatherLocation("現在地", 35.6812, 139.7671)
        val jittered = WeatherLocation("現在地", 35.6813, 139.7672)
        val moved = WeatherLocation("現在地", 34.6937, 135.5023)

        assertTrue(first.sameForecastPlaceAs(jittered))
        assertFalse(first.sameForecastPlaceAs(moved))
    }
}
