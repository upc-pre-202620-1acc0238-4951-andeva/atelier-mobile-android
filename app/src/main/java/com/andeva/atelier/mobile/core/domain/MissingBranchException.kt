package com.andeva.atelier.mobile.core.domain

/**
 * La sesión no tiene sede asignada (sin perfil laboral, docs/07-roadmap.md paso 2) y la acción la necesita:
 * agenda de citas, órdenes de la sede, mecánicos de la sede. La UI pide al administrador que le asigne una.
 */
class MissingBranchException : Exception("Session without branch")
