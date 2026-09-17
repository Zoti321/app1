package com.example.mynativeapp1.data

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val country: String?,
    val admin1: String? = null,
    val addedAt: Long,
) {
    fun subtitle(): String = SavedLocation(
        name = name,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
        country = country,
        admin1 = admin1,
    ).subtitle()

    fun locationKey(): String = SavedLocation(
        name = name,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
        country = country,
        admin1 = admin1,
    ).locationKey()

    fun toSavedLocation(): SavedLocation = SavedLocation(
        name = name,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
        country = country,
        admin1 = admin1,
    )
}

fun SavedLocation.toFavoriteLocation(addedAt: Long = System.currentTimeMillis()): FavoriteLocation =
    FavoriteLocation(
        name = name,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
        country = country,
        admin1 = admin1,
        addedAt = addedAt,
    )
