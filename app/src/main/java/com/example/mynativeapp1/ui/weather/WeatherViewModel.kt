package com.example.mynativeapp1.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mynativeapp1.data.WeatherDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val dataSource: WeatherDataSource,
    private val defaultCity: String = DEFAULT_CITY,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    init {
        loadWeather(defaultCity)
    }

    fun loadWeather(city: String) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            _uiState.value = dataSource.getWeather(city).fold(
                onSuccess = { WeatherUiState.Success(it) },
                onFailure = { WeatherUiState.Error(WeatherErrorMessages.messageFor(it)) },
            )
        }
    }

    fun retry() {
        loadWeather(defaultCity)
    }

    private companion object {
        const val DEFAULT_CITY = "北京"
    }
}
