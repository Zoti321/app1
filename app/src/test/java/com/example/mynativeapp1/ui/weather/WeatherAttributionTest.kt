package com.example.mynativeapp1.ui.weather

import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherAttributionTest {

    @Test
    fun text_mentionsOpenMeteoAndCcByLicense() {
        assertTrue(WeatherAttribution.TEXT.contains("Open-Meteo.com"))
        assertTrue(WeatherAttribution.TEXT.contains("CC BY 4.0"))
    }

    @Test
    fun contentDescription_mentionsOpenMeteoAndCcByLicense() {
        assertTrue(WeatherAttribution.CONTENT_DESCRIPTION.contains("Open-Meteo.com"))
        assertTrue(WeatherAttribution.CONTENT_DESCRIPTION.contains("CC BY 4.0"))
    }
}
