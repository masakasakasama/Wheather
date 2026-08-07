# Weather presentation logic research

This app intentionally separates four different meteorological signals instead of forcing them to agree:

1. Weather condition / icon
2. Precipitation probability
3. Precipitation amount
4. Observed precipitation (radar)

Public weather APIs use the same separation. Apple WeatherKit exposes condition/symbol, precipitation type, precipitation chance and precipitation amount as independent fields. AccuWeather exposes icon phrase / precipitation presence / precipitation type and intensity separately. The Weather Company documents precipitation type and QPF as independent concepts. Open-Meteo documents weather_code as an instantaneous WMO condition, precipitation as the preceding-period accumulation, precipitation_probability as the chance of more than 0.1 mm, and the daily weather_code as the most severe condition of the day.

Because those fields have different time semantics, `weather_code = drizzle` and `precipitation = 0.0 mm` are not automatically contradictory. The UI must not rewrite a provider condition solely from rounded accumulation.

## Product rules

- Current condition in Japan: fresh JMA radar observation overrides model rain/no-rain presentation.
- Hourly condition: show the provider hourly weather code as-is.
- Today card: summarize only the remaining hours from now, not the most severe condition that happened earlier today.
- Future day card: use a representative hourly condition instead of blindly using Open-Meteo's "most severe condition of the day" code.
- Brief precipitation: if wet weather appears only briefly, keep the dominant dry/cloud condition and append a brief-precipitation note instead of turning the whole day into a rain icon.
- Probability and amount remain visible as separate metrics.
- Trace precipitation below 0.1 mm is preserved internally and displayed as `<0.1mm`, not rounded to `0.0mm` and then used to erase the weather condition.
- Umbrella/rain advice uses future-period probability + amount + observed radar, not the daily icon.

## References

- Apple WeatherKit: `CurrentWeather.condition`, `symbolName`, `HourWeather.precipitation`, `precipitationChance`, `precipitationAmount`
- AccuWeather API schemas: `IconPhrase`, `HasPrecipitation`, `PrecipitationType`, `PrecipitationIntensity`
- The Weather Company API: precipitation type and QPF are independent; precipitation-event API models onset/offset as events
- Open-Meteo docs: weather code is instantaneous; precipitation is the preceding-period sum; precipitation probability is based on >0.1 mm; daily weather code is the most severe condition of the day
