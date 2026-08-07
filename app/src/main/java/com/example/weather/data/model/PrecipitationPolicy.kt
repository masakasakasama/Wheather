package com.example.weather.data.model

const val MEASURABLE_PRECIPITATION_MM = 0.1

enum class PrecipitationState {
    MEASURABLE,
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

object PrecipitationPolicy {
    private val amountBoundWeatherCodes = setOf(
        51, 53, 55, 56, 57,
        61, 63, 65, 66, 67,
        71, 73, 75, 77,
        80, 81, 82,
        85, 86,
    )

    fun normalizeAmount(value: Double?): Double? = when {
        value == null -> null
        !value.isFinite() -> null
        value <= 0.0 -> 0.0
        value < MEASURABLE_PRECIPITATION_MM -> 0.0
        else -> value
    }

    fun normalizeWeatherCode(code: Int?, amountMm: Double?): Int? {
        val normalizedAmount = normalizeAmount(amountMm)
        if (normalizedAmount == null || normalizedAmount >= MEASURABLE_PRECIPITATION_MM) return code
        return if (code in amountBoundWeatherCodes) 3 else code
    }

    fun normalizeSample(code: Int?, amountMm: Double?): NormalizedPrecipitationSample {
        val normalizedAmount = normalizeAmount(amountMm)
        return NormalizedPrecipitationSample(
            weatherCode = normalizeWeatherCode(code, normalizedAmount),
            amountMm = normalizedAmount,
        )
    }

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

    fun weatherCodeRequiresMeasurableAmount(code: Int?): Boolean = code in amountBoundWeatherCodes
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
    fun check(path: String, weatherCode: Int?, amountMm: Double?) {
        if (amountMm != null && (!amountMm.isFinite() || amountMm < 0.0)) {
            add(PrecipitationInvariantViolation(path, "invalid precipitation amount: $amountMm"))
            return
        }
        if (amountMm != null && amountMm > 0.0 && amountMm < MEASURABLE_PRECIPITATION_MM) {
            add(PrecipitationInvariantViolation(path, "sub-threshold precipitation amount survived normalization: $amountMm"))
        }
        if (
            amountMm == 0.0 &&
            PrecipitationPolicy.weatherCodeRequiresMeasurableAmount(weatherCode)
        ) {
            add(PrecipitationInvariantViolation(path, "precipitation weather code $weatherCode contradicts 0.0mm"))
        }
    }

    check("current", current.weatherCode, current.precipitationMm)
    minutely15.forEachIndexed { index, minute ->
        check("minutely15[$index]", minute.weatherCode, minute.precipitationMm)
    }
    hourly.forEachIndexed { index, hour ->
        check("hourly[$index]", hour.weatherCode, hour.precipitationMm)
    }
    daily.forEachIndexed { index, day ->
        check("daily[$index]", day.weatherCode, day.precipitationSumMm)
    }
}
