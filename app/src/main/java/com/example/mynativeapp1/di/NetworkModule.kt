package com.example.mynativeapp1.di

import com.example.mynativeapp1.data.remote.OpenMeteoApi
import com.example.mynativeapp1.data.remote.OpenMeteoApiFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = OpenMeteoApiFactory.createJson()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideOpenMeteoApi(
        okHttpClient: OkHttpClient,
        json: Json,
    ): OpenMeteoApi = OpenMeteoApiFactory.createOpenMeteoApi(
        okHttpClient = okHttpClient,
        json = json,
    )
}
