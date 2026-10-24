package com.andeva.atelier.mobile.features.main.domain

/** Estado de una operación en cola (docs/06-offline-sync.md §3). Las exitosas se borran. */
enum class PendingOperationStatus { PENDING, SYNCING, FAILED }
