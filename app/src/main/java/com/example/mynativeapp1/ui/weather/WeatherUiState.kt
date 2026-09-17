package com.example.mynativeapp1.ui.weather

import com.example.mynativeapp1.data.WeatherInfo

sealed interface WeatherUiState {
    data object Loading : WeatherUiState

    data class Success(val weather: WeatherInfo) : WeatherUiState

    data class Error(val message: String) : WeatherUiState
}
