package com.andeva.atelier.mobile.features.reception.domain

/** Un dato no cumple [ReceptionValidation]; [field] dice cuál (`taxId`, `plate`, `email`…). */
class InvalidReceptionInputException(val field: String) : Exception("Invalid reception input: $field")
