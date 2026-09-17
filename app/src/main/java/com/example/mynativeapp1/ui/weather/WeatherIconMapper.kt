package com.example.mynativeapp1.ui.weather

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

object WeatherIconMapper {

    fun iconFor(weatherCode: Int): ImageVector = when (weatherCode) {
        0, 1 -> Icons.Outlined.WbSunny
        2 -> Icons.Outlined.WbCloudy
        3 -> Icons.Outlined.Cloud
        45, 48 -> Icons.Outlined.Cloud
        51, 53, 55, 56, 57 -> Icons.Outlined.Grain
        61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Outlined.Umbrella
        71, 73, 75, 77, 85, 86 -> Icons.Outlined.AcUnit
        95, 96, 99 -> Icons.Outlined.Thunderstorm
        else -> Icons.Outlined.Cloud
    }
}
