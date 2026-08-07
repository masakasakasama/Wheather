package com.example.weather.data.model

const val MEASURABLE_PRECIPITATION_MM = 0.1

enum class PrecipitationState {
    MEASURABLE,
    TRACE,
    PROBABILITY_ONLY,
    NO_PRECIPITATION,
    AMOUNT_UNKNOWN,
}

data class PrecipitationAssessment(
    val state: PrecipitationState,
    val probabilityPercent: Int?,
    val amountMm: Double?,
    val weatherCode: Int?,
) {
    val isMeasurable: Boolean
        get() = state == PrecipitationState.MEASURABLE
}

data class NormalizedPrecipitationSample(
    val weatherCode: Int?,
    val amountMm: Double?,
)

data class PrecipitationInvariantViolation(
    val path: String,
    val message: String,
)

/**
 * Normalizes bad numeric input without inventing a relationship between fields.
 *
 * Weather providers intentionally expose condition/type, probability and amount as
 * separate signals with different valid-time semantics. For example, Open-Meteo's
 * weather_code is instantaneous while hourly precipitation is an accumulation for
 * the preceding hour. A rain/drizzle code with 0.0 mm therefore must not be rewritten
 * to cloudy just because the accumulation rounds to zero.
 */
object PrecipitationPolicy {
    fun normalizeAmount(value: Double?): Double? = when {
        value == null -> null
        !value.isFinite() -> null
        value < 0.0 -> null
        else -> value
    }

    /** Kept as a compatibility API. Provider weather condition is preserved. */
    fun normalizeWeatherCode(code: Int?, amountMm: Double?): Int? = code

    fun normalizeSample(code: Int?, amountMm: Double?): NormalizedPrecipitationSample =
        NormalizedPrecipitationSample(
            weatherCode = code,
            amountMm = normalizeAmount(amountMm),
        )

    fun isMeasurable(amountMm: Double?, thresholdMm: Double = MEASURABLE_PRECIPITATION_MM): Boolean {
        val normalizedAmount = normalizeAmount(amountMm) ?: return false
        val effectiveThreshold = thresholdMm.coerceAtLeast(MEASURABLE_PRECIPITATION_MM)
        return normalizedAmount >= effectiveThreshold
    }

    fun assess(
        probabilityPercent: Int?,
        amountMm: Double?,
        weatherCode: Int?,
    ): PrecipitationAssessment {
        val sample = normalizeSample(weatherCode, amountMm)
        val probability = probabilityPercent?.coerceIn(0, 100)
        val state = when {
            sample.amountMm == null -> PrecipitationState.AMOUNT_UNKNOWN
            sample.amountMm >= MEASURABLE_PRECIPITATION_MM -> PrecipitationState.MEASURABLE
            sample.amountMm > 0.0 -> PrecipitationState.TRACE
            (probability ?: 0) > 0 -> PrecipitationState.PROBABILITY_ONLY
            else -> PrecipitationState.NO_PRECIPITATION
        }
        return PrecipitationAssessment(
            state = state,
            probabilityPercent = probability,
            amountMm = sample.amountMm,
            weatherCode = sample.weatherCode,
        )
    }
}

fun WeatherSnapshot.enforcePrecipitationConsistency(): WeatherSnapshot {
    val currentSample = PrecipitationPolicy.normalizeSample(current.weatherCode, current.precipitationMm)
    return copy(
        current = current.copy(
            weatherCode = currentSample.weatherCode,
            precipitationMm = currentSample.amountMm,
        ),
        minutely15 = minutely15.map { minute ->
            val sample = PrecipitationPolicy.normalizeSample(minute.weatherCode, minute.precipitationMm)
            minute.copy(weatherCode = sample.weatherCode, precipitationMm = sample.amountMm)
        },
        hourly = hourly.map { hour ->
            val sample = PrecipitationPolicy.normalizeSample(hour.weatherCode, hour.precipitationMm)
            hour.copy(weatherCode = sample.weatherCode, precipitationMm = sample.amountMm)
        },
        daily = daily.map { day ->
            val sample = PrecipitationPolicy.normalizeSample(day.weatherCode, day.precipitationSumMm)
            day.copy(weatherCode = sample.weatherCode, precipitationSumMm = sample.amountMm)
        },
    )
}

fun WeatherSnapshot.precipitationInvariantViolations(): List<PrecipitationInvariantViolation> = buildList {
    fun check(path: String, amountMm: Double?) {
        if (amountMm != null && (!amountMm.isFinite() || amountMm < 0.0)) {
            add(PrecipitationInvariantViolation(path, "invalid precipitation amount: $amountMm"))
        }
    }

    check("current", current.precipitationMm)
    minutely15.forEachIndexed { index, minute -> check("minutely15[$index]", minute.precipitationMm) }
    hourly.forEachIndexed { index, hour -> check("hourly[$index]", hour.precipitationMm) }
    daily.forEachIndexed { index, day -> check("daily[$index]", day.precipitationSumMm) }
}
