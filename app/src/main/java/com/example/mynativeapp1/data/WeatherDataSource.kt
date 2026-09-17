package com.example.mynativeapp1.data

fun interface WeatherDataSource {
    suspend fun getWeather(city: String): Result<WeatherInfo>
}
