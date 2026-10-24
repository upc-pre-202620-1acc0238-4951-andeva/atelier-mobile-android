package com.andeva.atelier.mobile.features.reception.domain

/** Restricciones de los requests de docs/api/crm.md. Solo evitan una llamada inútil; el backend vuelve a validar. */
object ReceptionValidation {
    const val NAME_MAX = 100
    const val COMPANY_NAME_MAX = 150
    const val TAX_ID_MAX = 20
    const val PERSON_TAX_ID_MIN = 8
    const val COMPANY_TAX_ID_MIN = 11
    const val EMAIL_MAX = 150
    const val PHONE_MAX = 20
    const val PLATE_MAX = 15
    const val BRAND_MODEL_MAX = 50
    const val VIN_MAX = 17
    const val YEAR_MIN = 1950

    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /** La placa se busca y se guarda en mayúsculas y sin espacios. */
    fun normalizePlate(plate: String) = plate.filterNot { it.isWhitespace() }.uppercase()

    fun isPlate(plate: String) = normalizePlate(plate).let { it.isNotEmpty() && it.length <= PLATE_MAX }

    fun isName(value: String) = value.isNotBlank() && value.trim().length <= NAME_MAX

    fun isCompanyName(value: String) = value.isNotBlank() && value.trim().length <= COMPANY_NAME_MAX

    fun isPersonTaxId(value: String) = value.trim().length in PERSON_TAX_ID_MIN..TAX_ID_MAX

    fun isCompanyTaxId(value: String) = value.trim().length in COMPANY_TAX_ID_MIN..TAX_ID_MAX

    fun isEmail(value: String) = value.trim().let { it.length <= EMAIL_MAX && EMAIL.matches(it) }

    fun isOptionalEmail(value: String?) = value.isNullOrBlank() || isEmail(value)

    fun isOptionalPhone(value: String?) = value.isNullOrBlank() || value.trim().length <= PHONE_MAX

    fun isBrandOrModel(value: String) = value.isNotBlank() && value.trim().length <= BRAND_MODEL_MAX

    fun isOptionalVin(value: String?) = value.isNullOrBlank() || value.trim().length <= VIN_MAX

    fun isOptionalYear(value: Int?) = value == null || value >= YEAR_MIN
}
