package com.example.mynativeapp1.ui.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class ForecastDayLabelTest {

    @Test
    fun labelFor_firstDay_isToday() {
        assertEquals("今天", ForecastDayLabel.labelFor("2026-09-17", 0))
    }

    @Test
    fun labelFor_secondDay_isTomorrow() {
        assertEquals("明天", ForecastDayLabel.labelFor("2026-09-18", 1))
    }

    @Test
    fun labelFor_thirdDay_isWeekdayName() {
        assertEquals("周六", ForecastDayLabel.labelFor("2026-09-19", 2))
    }
}
