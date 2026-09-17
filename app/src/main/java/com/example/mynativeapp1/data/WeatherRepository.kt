package com.example.mynativeapp1.data

import com.example.mynativeapp1.data.remote.OpenMeteoApi
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class WeatherRepository(
    private val api: OpenMeteoApi,
) : WeatherDataSource {

    override suspend fun getWeather(city: String): Result<WeatherInfo> {
        return try {
            val geocoding = api.searchCity(name = city)
            val location = geocoding.results?.firstOrNull()
                ?: return Result.failure(WeatherError.CityNotFound)

            val forecast = api.getForecast(
                latitude = location.latitude,
                longitude = location.longitude,
                timezone = location.timezone ?: "auto",
            )
            val current = forecast.current
                ?: return Result.failure(WeatherError.ApiFailure)

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
}
