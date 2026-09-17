package com.example.mynativeapp1.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoErrorResponse(
    val error: Boolean,
    val reason: String,
)
