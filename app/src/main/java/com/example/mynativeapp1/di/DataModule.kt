package com.example.mynativeapp1.di

import com.example.mynativeapp1.data.AppPreferencesStore
import com.example.mynativeapp1.data.PreferencesStore
import com.example.mynativeapp1.data.WeatherDataSource
import com.example.mynativeapp1.data.WeatherRepository
import com.example.mynativeapp1.data.location.AndroidPlaceNameResolver
import com.example.mynativeapp1.data.location.DeviceLocationProvider
import com.example.mynativeapp1.data.location.FusedDeviceLocationProvider
import com.example.mynativeapp1.data.location.PlaceNameResolver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindWeatherDataSource(impl: WeatherRepository): WeatherDataSource

    @Binds
    @Singleton
    abstract fun bindPreferencesStore(impl: AppPreferencesStore): PreferencesStore

    @Binds
    @Singleton
    abstract fun bindDeviceLocationProvider(impl: FusedDeviceLocationProvider): DeviceLocationProvider

    @Binds
    @Singleton
    abstract fun bindPlaceNameResolver(impl: AndroidPlaceNameResolver): PlaceNameResolver
}
