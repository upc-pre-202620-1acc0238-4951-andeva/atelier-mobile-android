package com.andeva.atelier.mobile.features.bays.infrastructure.remote

import com.google.gson.annotations.SerializedName

/** `WorkBayResource` de docs/api/operations.md. */
data class WorkBayDto(
    @SerializedName("id") val id: String,
    @SerializedName("branchId") val branchId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("bayType") val bayType: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("currentWorkOrderId") val currentWorkOrderId: String?
)

/** `AssignWorkBayRequest`. */
data class AssignWorkBayRequestDto(@SerializedName("bayId") val bayId: String)

/** `AssignTaskMechanicRequest`. */
data class AssignTaskMechanicRequestDto(@SerializedName("mechanicId") val mechanicId: String)
