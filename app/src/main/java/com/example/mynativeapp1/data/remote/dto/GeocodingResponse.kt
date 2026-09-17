package com.example.mynativeapp1.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null,
    @SerialName("generationtime_ms") val generationTimeMs: Double? = null,
)

@Serializable
data class GeocodingResult(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    @SerialName("feature_code") val featureCode: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    val timezone: String? = null,
    val population: Int? = null,
    val country: String? = null,
    val admin1: String? = null,
    val admin2: String? = null,
)
