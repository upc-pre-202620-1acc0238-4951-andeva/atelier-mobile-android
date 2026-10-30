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

val AppMigrations: Array<Migration> = arrayOf(MIGRATION_1_2)
