package com.andeva.atelier.mobile.features.tasks.domain

@JvmInline
value class TaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskId must not be blank" }
    }
}
