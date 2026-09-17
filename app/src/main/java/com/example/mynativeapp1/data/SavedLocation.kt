package com.example.mynativeapp1.data

import com.example.mynativeapp1.data.remote.dto.GeocodingResult
import kotlinx.serialization.Serializable
import kotlin.math.round

@Serializable
data class SavedLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val country: String?,
    val admin1: String? = null,
) {
    fun subtitle(): String = listOfNotNull(admin1, country)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    fun locationKey(): String =
        "${round(latitude * 100) / 100}:${round(longitude * 100) / 100}"

    companion object {
        val DEFAULT_BEIJING = SavedLocation(
            name = "北京",
            latitude = 39.9075,
            longitude = 116.39723,
            timezone = "Asia/Shanghai",
            country = "中国",
            admin1 = "北京市",
        )
    }
}

fun GeocodingResult.toSavedLocation(): SavedLocation = SavedLocation(
    name = name,
    latitude = latitude,
    longitude = longitude,
    timezone = timezone ?: "auto",
    country = country,
    admin1 = admin1,
)
