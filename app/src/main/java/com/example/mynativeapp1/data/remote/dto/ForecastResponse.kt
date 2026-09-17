package com.example.mynativeapp1.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("generationtime_ms") val generationTimeMs: Double,
    @SerialName("utc_offset_seconds") val utcOffsetSeconds: Int,
    val timezone: String,
    @SerialName("timezone_abbreviation") val timezoneAbbreviation: String,
    val elevation: Double,
    @SerialName("current_units") val currentUnits: CurrentUnits? = null,
    val current: CurrentWeather? = null,
    @SerialName("daily_units") val dailyUnits: DailyUnits? = null,
    val daily: DailyWeather? = null,
)

@Serializable
data class CurrentUnits(
    val time: String? = null,
    val interval: String? = null,
    @SerialName("temperature_2m") val temperature2m: String? = null,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: String? = null,
    @SerialName("weather_code") val weatherCode: String? = null,
    @SerialName("wind_speed_10m") val windSpeed10m: String? = null,
)

@Serializable
data class CurrentWeather(
    val time: String,
    val interval: Int,
    @SerialName("temperature_2m") val temperature2m: Double,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("wind_speed_10m") val windSpeed10m: Double? = null,
)

@Serializable
data class DailyUnits(
    val time: String? = null,
    @SerialName("weather_code") val weatherCode: String? = null,
    @SerialName("temperature_2m_max") val temperature2mMax: String? = null,
    @SerialName("temperature_2m_min") val temperature2mMin: String? = null,
    @SerialName("precipitation_sum") val precipitationSum: String? = null,
)

@Serializable
data class DailyWeather(
    val time: List<String>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double>,
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double>,
    @SerialName("precipitation_sum") val precipitationSum: List<Double>,
)
