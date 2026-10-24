package com.andeva.atelier.mobile.features.attendance.domain

/** Falta el permiso de ubicación precisa: la UI debe pedirlo. */
class LocationPermissionDeniedException : Exception("Location permission not granted")
