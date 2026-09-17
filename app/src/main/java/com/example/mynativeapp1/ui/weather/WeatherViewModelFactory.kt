package com.example.mynativeapp1.ui.weather

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mynativeapp1.data.AppPreferencesStore
import com.example.mynativeapp1.data.WeatherRepository
import com.example.mynativeapp1.data.location.AndroidPlaceNameResolver
import com.example.mynativeapp1.data.location.FusedDeviceLocationProvider
import com.example.mynativeapp1.data.remote.NetworkModule

class WeatherViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            val appContext = context.applicationContext
            return WeatherViewModel(
                dataSource = WeatherRepository(NetworkModule.openMeteoApi),
                preferencesStore = AppPreferencesStore(appContext),
                locationProvider = FusedDeviceLocationProvider(appContext),
                placeNameResolver = AndroidPlaceNameResolver(appContext),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
