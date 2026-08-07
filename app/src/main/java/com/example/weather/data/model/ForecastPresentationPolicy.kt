package com.example.weather.data.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * User-facing summary built from hourly conditions.
 *
 * Open-Meteo's daily weather_code is the most severe condition of the full day.
 * That is useful meteorologically but can be misleading in a consumer UI late in
 * the day (for example, a drizzle that happened in the morning can keep the entire
 * "today" card rainy at 18:00). This presentation model uses remaining hours for
 * today and a representative hourly condition for future days.
 */
data class DayForecastPresentation(
    val weatherCode: Int?,
    val weatherLabel: String,
    val precipitationProbability: Int?,
    val precipitationAmountMm: Double?,
    val isRemainingToday: Boolean,
    val hasBriefPrecipitation: Boolean,
)

private enum class ConditionFamily {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    SNOW,
    SHOWERS,
    THUNDERSTORM,
    UNKNOWN,
}

private val WetFamilies = setOf(
    ConditionFamily.DRIZZLE,
    ConditionFamily.RAIN,
    ConditionFamily.SNOW,
    ConditionFamily.SHOWERS,
    ConditionFamily.THUNDERSTORM,
)

fun WeatherSnapshot.presentationForDay(
    day: DailyWeather,
    now: LocalDateTime = LocalDateTime.now(forecastZoneId()),
): DayForecastPresentation {
    val targetDate = runCatching { LocalDate.parse(day.date) }.getOrNull()
    val allHours = hourly
        .mapNotNull { hour ->
            runCatching { LocalDateTime.parse(hour.time) }.getOrNull()?.let { it to hour }
        }
        .filter { (time) -> targetDate == null || time.toLocalDate() == targetDate }
        .sortedBy { (time) -> time }

    val isToday = targetDate != null && targetDate == now.toLocalDate()
    val currentHour = now.withMinute(0).withSecond(0).withNano(0)
    val relevantPairs = if (isToday) {
        allHours.filter { (time) -> !time.isBefore(currentHour) }
            .ifEmpty { allHours.takeLast(1) }
    } else {
        allHours
    }
    val relevantHours = relevantPairs.map { it.second }

    val condition = representativeCondition(
        hours = relevantHours,
        fallbackCode = day.weatherCode,
    )
    val hourlyProbability = relevantHours.mapNotNull { it.precipitationProbability }.maxOrNull()
    val probability = if (isToday) {
        hourlyProbability
    } else {
        listOfNotNull(day.maxPrecipitationProbability, hourlyProbability).maxOrNull()
    }
    val hourlyAmounts = relevantHours.mapNotNull { PrecipitationPolicy.normalizeAmount(it.precipitationMm) }
    val hourlyAmount = hourlyAmounts.takeIf { it.isNotEmpty() }?.sum()
    val amount = if (isToday) {
        hourlyAmount
    } else {
        day.precipitationSumMm ?: hourlyAmount
    }

    return DayForecastPresentation(
        weatherCode = condition.primaryCode,
        weatherLabel = condition.label,
        precipitationProbability = probability,
        precipitationAmountMm = amount,
        isRemainingToday = isToday,
        hasBriefPrecipitation = condition.hasBriefPrecipitation,
    )
}

private data class RepresentativeCondition(
    val primaryCode: Int?,
    val label: String,
    val hasBriefPrecipitation: Boolean,
)

private fun representativeCondition(
    hours: List<HourlyWeather>,
    fallbackCode: Int?,
): RepresentativeCondition {
    if (hours.isEmpty()) {
        return RepresentativeCondition(fallbackCode, weatherLabel(fallbackCode), false)
    }

    val codedHours = hours.filter { it.weatherCode != null }
    if (codedHours.isEmpty()) {
        return RepresentativeCondition(fallbackCode, weatherLabel(fallbackCode), false)
    }

    val familyCounts = codedHours
        .groupingBy { conditionFamily(it.weatherCode) }
        .eachCount()
    val wetHours = codedHours.filter { conditionFamily(it.weatherCode) in WetFamilies }
    val dryHours = codedHours.filterNot { conditionFamily(it.weatherCode) in WetFamilies }
    val maxProbability = hours.mapNotNull { it.precipitationProbability }.maxOrNull() ?: 0
    val totalAmount = hours.mapNotNull { PrecipitationPolicy.normalizeAmount(it.precipitationMm) }.sum()
    val hasThunder = wetHours.any { conditionFamily(it.weatherCode) == ConditionFamily.THUNDERSTORM }

    // A single short drizzle/shower should not turn a whole-day consumer card into
    // a rain card. Sustained precipitation, meaningful accumulation, high PoP, and
    // any thunderstorm remain prominent.
    val significantWet = hasThunder ||
        wetHours.size >= 2 ||
        totalAmount >= 0.5 ||
        (wetHours.isNotEmpty() && maxProbability >= 60)

    val selectedFamily = when {
        significantWet -> selectFamily(wetHours)
        dryHours.isNotEmpty() -> selectFamily(dryHours)
        else -> selectFamily(codedHours)
    }
    val primaryCode = preferredCode(selectedFamily)
        ?: codedHours.firstOrNull { conditionFamily(it.weatherCode) == selectedFamily }?.weatherCode
        ?: fallbackCode

    val briefWetFamily = if (!significantWet && wetHours.isNotEmpty()) selectFamily(wetHours) else null
    val briefCode = preferredCode(briefWetFamily)
    val baseLabel = weatherLabel(primaryCode)
    val label = if (briefWetFamily != null && briefCode != null) {
        "$baseLabel、一時${weatherLabel(briefCode)}"
    } else {
        baseLabel
    }

    return RepresentativeCondition(
        primaryCode = primaryCode,
        label = label,
        hasBriefPrecipitation = briefWetFamily != null,
    )
}

private fun selectFamily(hours: List<HourlyWeather>): ConditionFamily {
    val counts = hours.groupingBy { conditionFamily(it.weatherCode) }.eachCount()
    return counts.entries
        .sortedWith(
            compareByDescending<Map.Entry<ConditionFamily, Int>> { it.value }
                .thenByDescending { familyPriority(it.key) },
        )
        .firstOrNull()
        ?.key
        ?: ConditionFamily.UNKNOWN
}

private fun familyPriority(family: ConditionFamily): Int = when (family) {
    ConditionFamily.THUNDERSTORM -> 90
    ConditionFamily.SNOW -> 80
    ConditionFamily.RAIN -> 70
    ConditionFamily.SHOWERS -> 65
    ConditionFamily.DRIZZLE -> 60
    ConditionFamily.FOG -> 50
    ConditionFamily.CLOUDY -> 40
    ConditionFamily.PARTLY_CLOUDY -> 30
    ConditionFamily.CLEAR -> 20
    ConditionFamily.UNKNOWN -> 0
}

private fun conditionFamily(code: Int?): ConditionFamily = when (code) {
    0 -> ConditionFamily.CLEAR
    1, 2 -> ConditionFamily.PARTLY_CLOUDY
    3 -> ConditionFamily.CLOUDY
    45, 48 -> ConditionFamily.FOG
    51, 53, 55, 56, 57 -> ConditionFamily.DRIZZLE
    61, 63, 65, 66, 67 -> ConditionFamily.RAIN
    71, 73, 75, 77, 85, 86 -> ConditionFamily.SNOW
    80, 81, 82 -> ConditionFamily.SHOWERS
    95, 96, 99 -> ConditionFamily.THUNDERSTORM
    else -> ConditionFamily.UNKNOWN
}

private fun preferredCode(family: ConditionFamily?): Int? = when (family) {
    ConditionFamily.CLEAR -> 0
    ConditionFamily.PARTLY_CLOUDY -> 2
    ConditionFamily.CLOUDY -> 3
    ConditionFamily.FOG -> 45
    ConditionFamily.DRIZZLE -> 53
    ConditionFamily.RAIN -> 63
    ConditionFamily.SNOW -> 73
    ConditionFamily.SHOWERS -> 81
    ConditionFamily.THUNDERSTORM -> 95
    ConditionFamily.UNKNOWN, null -> null
}
