package com.andeva.atelier.mobile.features.attendance.domain

@JvmInline
value class AttendanceId(val value: String) {
    init {
        require(value.isNotBlank()) { "AttendanceId must not be blank" }
    }
}
