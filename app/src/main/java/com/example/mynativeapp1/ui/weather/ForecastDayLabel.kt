package com.example.mynativeapp1.ui.weather

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ForecastDayLabel {
    private val weekdayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    fun labelFor(date: String, index: Int): String = when (index) {
        0 -> "今天"
        1 -> "明天"
        else -> {
            val dayOfWeek = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).dayOfWeek
            weekdayNames[dayOfWeek.value - 1]
        }
    }

    fun contentDescription(
        date: String,
        index: Int,
        maxTemp: Int,
        minTemp: Int,
        description: String,
    ): String = "${labelFor(date, index)}，最高 $maxTemp 度，最低 $minTemp 度，$description"
}
