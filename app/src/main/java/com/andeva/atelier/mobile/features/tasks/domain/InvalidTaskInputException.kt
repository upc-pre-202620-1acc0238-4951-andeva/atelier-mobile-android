package com.andeva.atelier.mobile.features.tasks.domain

/** Un dato no cumple [TaskValidation]; [field] dice cuál (`actualLaborHours`, `notes`, `missingItemDescription`…). */
class InvalidTaskInputException(val field: String) : Exception("Invalid task input: $field")
