package com.example.mynativeapp1.data

object WeatherCodeMapper {

    private val descriptions = mapOf(
        0 to "晴",
        1 to "大部晴朗",
        2 to "局部多云",
        3 to "阴",
        45 to "雾",
        48 to "雾凇",
        51 to "小毛毛雨",
        53 to "中毛毛雨",
        55 to "大毛毛雨",
        56 to "小冻毛毛雨",
        57 to "大冻毛毛雨",
        61 to "小雨",
        63 to "中雨",
        65 to "大雨",
        66 to "小冻雨",
        67 to "大冻雨",
        71 to "小雪",
        73 to "中雪",
        75 to "大雪",
        77 to "雪粒",
        80 to "小阵雨",
        81 to "中阵雨",
        82 to "大阵雨",
        85 to "小阵雪",
        86 to "大阵雪",
        95 to "雷暴",
        96 to "雷暴伴小冰雹",
        99 to "雷暴伴大冰雹",
    )

    fun toChineseDescription(code: Int): String = descriptions[code] ?: "未知天气"
}
