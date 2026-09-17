package com.example.mynativeapp1.data

data class DailyForecast(
    val date: String,
    val weatherCode: Int,
    val weatherDescription: String,
    val temperatureMaxCelsius: Double,
    val temperatureMinCelsius: Double,
    val precipitationSumMm: Double,
)
