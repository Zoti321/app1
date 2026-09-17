package com.example.mynativeapp1.data

data class WeatherInfo(
    val cityName: String,
    val country: String?,
    val temperatureCelsius: Double,
    val humidityPercent: Int?,
    val weatherDescription: String,
    val weatherCode: Int,
    val windSpeedKmh: Double?,
    val observedAt: String,
    val dailyForecasts: List<DailyForecast> = emptyList(),
)
