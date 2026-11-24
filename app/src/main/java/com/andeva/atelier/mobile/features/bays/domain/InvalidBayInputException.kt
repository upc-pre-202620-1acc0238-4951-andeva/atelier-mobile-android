package com.andeva.atelier.mobile.features.bays.domain

/** Entrada inválida antes de llamar al backend de bahías; [field] indica cuál. */
class InvalidBayInputException(val field: String) : Exception("Invalid bay input: $field")
