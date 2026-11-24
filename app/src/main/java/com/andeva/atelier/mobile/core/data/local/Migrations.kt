package com.andeva.atelier.mobile.core.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Versión 2: marca del repuesto en la caché `parts` (US31, H35). Las filas existentes quedan sin marca. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE parts ADD COLUMN brand TEXT")
    }
}

/**
 * Versión 3: fecha de solicitud de suspensión en `tasks` (`INTEGER` epoch ms UTC, US29) y propietario `user_id`
 * en `telemetry_readings` para aislar lecturas en fosa entre usuarios de un terminal compartido (US19).
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE tasks ADD COLUMN pending_suspension_requested_at INTEGER")
        connection.execSQL("ALTER TABLE telemetry_readings ADD COLUMN user_id TEXT")
    }
}

val AppMigrations: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
