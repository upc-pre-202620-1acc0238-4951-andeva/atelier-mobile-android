package com.andeva.atelier.mobile.features.attendance.domain

/** Motivo vacío o de más de [AttendanceValidation.JUSTIFICATION_MAX] caracteres. */
class InvalidJustificationException : Exception("Invalid justification reason")
