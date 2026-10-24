package com.andeva.atelier.mobile.features.attendance.domain

/** El colaborador no tiene perfil laboral, sede o turno asignado: no puede marcar. */
class MissingAssignmentException : Exception("No branch or shift assigned")
