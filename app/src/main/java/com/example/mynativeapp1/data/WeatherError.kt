package com.example.mynativeapp1.data

/**
 * 三档错误来源，供 ViewModel 映射中文文案。
 */
sealed class WeatherError : Exception() {
    data object NoNetwork : WeatherError()

    data object ApiFailure : WeatherError()

    data object CityNotFound : WeatherError()
}
