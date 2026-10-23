package com.andeva.atelier.mobile.core.data.remote

import com.google.gson.annotations.SerializedName

/** Formato de error único del backend (docs/api/README.md §5). */
data class ErrorResponseDto(
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("details") val details: List<String>?,
    @SerializedName("timestamp") val timestamp: String?
)
