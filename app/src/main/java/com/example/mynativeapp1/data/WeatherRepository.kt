package com.example.mynativeapp1.data

import com.example.mynativeapp1.data.remote.OpenMeteoApi
import com.example.mynativeapp1.data.remote.dto.DailyWeather
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class WeatherRepository(
    private val api: OpenMeteoApi,
) : WeatherDataSource {

    override suspend fun searchLocations(query: String): Result<List<SavedLocation>> {
        return try {
            val geocoding = api.searchCity(name = query, count = 5)
            val results = geocoding.results.orEmpty().map { it.toSavedLocation() }
            if (results.isEmpty()) {
                Result.failure(WeatherError.CityNotFound)
            } else {
                Result.success(results)
            }
        } catch (_: IOException) {
            Result.failure(WeatherError.NoNetwork)
        } catch (_: HttpException) {
            Result.failure(WeatherError.ApiFailure)
        } catch (_: SerializationException) {
            Result.failure(WeatherError.ApiFailure)
        }
    }

    override suspend fun getWeather(
        location: SavedLocation,
        includeDailyForecast: Boolean,
    ): Result<WeatherInfo> {
        return try {
            val forecast = api.getForecast(
                latitude = location.latitude,
                longitude = location.longitude,
                timezone = location.timezone,
                includeDailyForecast = includeDailyForecast,
            )
            val current = forecast.current
                ?: return Result.failure(WeatherError.ApiFailure)

            val dailyForecasts = if (includeDailyForecast) {
                mapDailyForecasts(forecast.daily)
            } else {
                emptyList()
            }

            Result.success(
                WeatherInfo(
                    cityName = location.name,
                    country = location.country,
                    temperatureCelsius = current.temperature2m,
                    humidityPercent = current.relativeHumidity2m,
                    weatherDescription = WeatherCodeMapper.toChineseDescription(current.weatherCode),
                    weatherCode = current.weatherCode,
                    windSpeedKmh = current.windSpeed10m,
                    observedAt = current.time,
                    dailyForecasts = dailyForecasts,
                ),
            )
        } catch (_: IOException) {
            Result.failure(WeatherError.NoNetwork)
        } catch (_: HttpException) {
            Result.failure(WeatherError.ApiFailure)
        } catch (_: SerializationException) {
            Result.failure(WeatherError.ApiFailure)
        }
    }

    private fun mapDailyForecasts(daily: DailyWeather?): List<DailyForecast> {
        daily ?: return emptyList()
        return daily.time.mapIndexed { index, date ->
            DailyForecast(
                date = date,
                weatherCode = daily.weatherCode[index],
                weatherDescription = WeatherCodeMapper.toChineseDescription(daily.weatherCode[index]),
                temperatureMaxCelsius = daily.temperature2mMax[index],
                temperatureMinCelsius = daily.temperature2mMin[index],
                precipitationSumMm = daily.precipitationSum[index],
            )
        }
    }
}
