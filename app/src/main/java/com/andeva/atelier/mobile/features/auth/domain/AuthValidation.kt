package com.andeva.atelier.mobile.features.auth.domain

/**
 * Restricciones de los modelos de request de docs/api/iam.md. Solo evitan una llamada inútil;
 * el backend vuelve a validar todo.
 */
object AuthValidation {
    const val PASSWORD_MIN = 8
    const val PASSWORD_MAX = 64
    const val NAME_MAX = 100
    const val OTP_LENGTH = 6

    private val phonePattern = Regex("^\\+?[0-9]{9,15}$")
    private val otpPattern = Regex("^[0-9]{$OTP_LENGTH}$")

    fun isEmail(value: String) = value.contains('@') && value.substringAfter('@').contains('.')

    fun isPassword(value: String) = value.length in PASSWORD_MIN..PASSWORD_MAX

    fun isName(value: String) = value.isNotBlank() && value.length <= NAME_MAX

    fun isPhone(value: String) = phonePattern.matches(value)

    fun isOtp(value: String) = otpPattern.matches(value)
}
