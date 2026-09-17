package com.example.mynativeapp1.data

interface WeatherDataSource {
    suspend fun getWeather(
        location: SavedLocation,
        includeDailyForecast: Boolean = true,
    ): Result<WeatherInfo>

    suspend fun searchLocations(query: String): Result<List<SavedLocation>>
}
