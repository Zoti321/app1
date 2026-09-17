package com.example.mynativeapp1.ui.weather

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherIconMapperTest {

    @Test
    fun iconFor_mapsClearSkyToWbSunny() {
        assertEquals(Icons.Outlined.WbSunny, WeatherIconMapper.iconFor(0))
    }

    @Test
    fun iconFor_mapsOvercastToCloud() {
        assertEquals(Icons.Outlined.Cloud, WeatherIconMapper.iconFor(3))
    }

    @Test
    fun iconFor_mapsPartlyCloudyToWbCloudy() {
        assertEquals(Icons.Outlined.WbCloudy, WeatherIconMapper.iconFor(2))
    }

    @Test
    fun iconFor_mapsRainCodesToUmbrella() {
        assertEquals(Icons.Outlined.Umbrella, WeatherIconMapper.iconFor(63))
    }

    @Test
    fun iconFor_mapsDrizzleCodesToGrain() {
        assertEquals(Icons.Outlined.Grain, WeatherIconMapper.iconFor(51))
    }

    @Test
    fun iconFor_mapsSnowCodesToAcUnit() {
        assertEquals(Icons.Outlined.AcUnit, WeatherIconMapper.iconFor(71))
    }

    @Test
    fun iconFor_mapsThunderstormToThunderstormIcon() {
        assertEquals(Icons.Outlined.Thunderstorm, WeatherIconMapper.iconFor(95))
    }

    @Test
    fun iconFor_returnsCloud_forUnknownCode() {
        assertEquals(Icons.Outlined.Cloud, WeatherIconMapper.iconFor(999))
    }
}
