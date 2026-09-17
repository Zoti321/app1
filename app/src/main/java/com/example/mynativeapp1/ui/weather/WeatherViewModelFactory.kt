package com.example.mynativeapp1.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mynativeapp1.data.WeatherRepository
import com.example.mynativeapp1.data.remote.NetworkModule

class WeatherViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            val repository = WeatherRepository(NetworkModule.openMeteoApi)
            return WeatherViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
