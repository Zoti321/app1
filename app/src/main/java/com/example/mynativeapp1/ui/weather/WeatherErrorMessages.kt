package com.example.mynativeapp1.ui.weather

import com.example.mynativeapp1.data.WeatherError

object WeatherErrorMessages {

    fun messageFor(error: Throwable): String = when (error) {
        WeatherError.NoNetwork -> "无法连接网络，请检查后重试"
        WeatherError.ApiFailure -> "获取天气失败，请稍后重试"
        WeatherError.CityNotFound -> "未找到该城市天气信息"
        else -> "获取天气失败，请稍后重试"
    }
}
