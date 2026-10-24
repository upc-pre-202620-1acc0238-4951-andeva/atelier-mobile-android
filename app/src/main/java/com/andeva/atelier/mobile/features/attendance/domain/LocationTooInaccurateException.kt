package com.andeva.atelier.mobile.features.attendance.domain

/** Precisión peor que [AttendanceValidation.MAX_ACCURACY_METERS]: pedir salir a cielo abierto. */
class LocationTooInaccurateException(val accuracyMeters: Float) : Exception("Location accuracy is $accuracyMeters m")
