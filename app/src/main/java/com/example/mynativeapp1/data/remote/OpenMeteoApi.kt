package com.example.mynativeapp1.data.remote

import com.example.mynativeapp1.data.remote.dto.ForecastResponse
import com.example.mynativeapp1.data.remote.dto.GeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

private const val CURRENT_WEATHER_FIELDS =
    "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"

interface GeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 1,
        @Query("language") language: String = "zh",
    ): GeocodingResponse
}

interface ForecastApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_WEATHER_FIELDS,
        @Query("timezone") timezone: String,
    ): ForecastResponse
}

class OpenMeteoApi internal constructor(
    private val geocodingApi: GeocodingApi,
    private val forecastApi: ForecastApi,
) {
    suspend fun searchCity(name: String): GeocodingResponse =
        geocodingApi.searchCity(name = name)

    suspend fun getForecast(
        latitude: Double,
        longitude: Double,
        timezone: String,
    ): ForecastResponse = forecastApi.getForecast(
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
    )
}
