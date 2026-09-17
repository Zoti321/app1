package com.example.mynativeapp1.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object NetworkModule {

    private const val GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/"
    private const val FORECAST_BASE_URL = "https://api.open-meteo.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient = OkHttpClient.Builder().build()

    private fun createRetrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    fun createOpenMeteoApi(
        geocodingBaseUrl: String = GEOCODING_BASE_URL,
        forecastBaseUrl: String = FORECAST_BASE_URL,
    ): OpenMeteoApi {
        val geocodingApi = createRetrofit(geocodingBaseUrl).create(GeocodingApi::class.java)
        val forecastApi = createRetrofit(forecastBaseUrl).create(ForecastApi::class.java)
        return OpenMeteoApi(geocodingApi, forecastApi)
    }

    val openMeteoApi: OpenMeteoApi by lazy { createOpenMeteoApi() }
}
