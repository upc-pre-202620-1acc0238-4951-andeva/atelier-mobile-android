package com.andeva.atelier.mobile.features.bays.domain

/** La bahía destino no está libre (ocupada o en mantenimiento); no se llega a llamar al backend. */
class BayUnavailableException : Exception("Bay cannot receive a work order")
