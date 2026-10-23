package com.andeva.atelier.mobile.core.domain

/**
 * Error del backend con el formato único `{ code, message, details }` (docs/api/README.md §5).
 * `message` ya viene traducido (`Accept-Language: es`) y se muestra tal cual.
 * El flujo se decide por [httpStatus]; [code] solo donde el backend lo garantiza.
 */
class ApiException(
    val httpStatus: Int,
    val code: String,
    override val message: String,
    val details: List<String> = emptyList()
) : Exception(message)
