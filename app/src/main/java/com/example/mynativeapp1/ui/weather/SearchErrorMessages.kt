package com.example.mynativeapp1.ui.weather

import com.example.mynativeapp1.data.WeatherError

object SearchErrorMessages {
    const val EMPTY_RESULTS = "未找到匹配地点，请换个关键词"
    const val NO_NETWORK = "无法连接网络，请检查后重试"
    const val API_FAILURE = "搜索失败，请稍后重试"
    const val PLACEHOLDER = "输入至少 2 个字开始搜索"

    fun messageFor(error: Throwable): String = when (error) {
        WeatherError.NoNetwork -> NO_NETWORK
        WeatherError.CityNotFound -> EMPTY_RESULTS
        else -> API_FAILURE
    }
}
