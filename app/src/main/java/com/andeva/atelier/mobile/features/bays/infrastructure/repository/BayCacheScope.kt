package com.andeva.atelier.mobile.features.bays.infrastructure.repository

/** Alcance de `cache_metadata` para las bahías de una sede; `bays` y `tasks` lo llenan y lo leen por igual. */
object BayCacheScope {
    fun of(branchId: String) = "bays:$branchId"
}
