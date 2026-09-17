package com.example.mynativeapp1.data.remote

import com.example.mynativeapp1.data.remote.dto.ForecastResponse
import com.example.mynativeapp1.data.remote.dto.GeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

private const val CURRENT_WEATHER_FIELDS =
    "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"

private const val DAILY_WEATHER_FIELDS =
    "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum"

interface GeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "zh",
    ): GeocodingResponse
}

interface ForecastApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_WEATHER_FIELDS,
        @Query("daily") daily: String? = null,
        @Query("forecast_days") forecastDays: Int? = null,
        @Query("timezone") timezone: String,
    ): ForecastResponse
}

class OpenMeteoApi internal constructor(
    private val geocodingApi: GeocodingApi,
    private val forecastApi: ForecastApi,
) {
    suspend fun searchCity(name: String, count: Int = 5): GeocodingResponse =
        geocodingApi.searchCity(name = name, count = count)

    suspend fun getForecast(
        latitude: Double,
        longitude: Double,
        timezone: String,
        includeDailyForecast: Boolean,
    ): ForecastResponse = forecastApi.getForecast(
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
        daily = if (includeDailyForecast) DAILY_WEATHER_FIELDS else null,
        forecastDays = if (includeDailyForecast) 7 else null,
    )
}
