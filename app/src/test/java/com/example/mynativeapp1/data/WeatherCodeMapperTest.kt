package com.example.mynativeapp1.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeMapperTest {

    @Test
    fun toChineseDescription_returnsKnownDescriptions() {
        assertEquals("晴", WeatherCodeMapper.toChineseDescription(0))
        assertEquals("阴", WeatherCodeMapper.toChineseDescription(3))
        assertEquals("雷暴", WeatherCodeMapper.toChineseDescription(95))
    }

    @Test
    fun toChineseDescription_returnsFallback_forUnknownCode() {
        assertEquals("未知天气", WeatherCodeMapper.toChineseDescription(123))
    }
}
