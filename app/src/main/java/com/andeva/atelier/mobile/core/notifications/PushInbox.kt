package com.andeva.atelier.mobile.core.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Push tocado por el usuario, pendiente de abrir. MainActivity lo publica y la navegación
 * de cada feature lo consume. El push es solo un aviso: la pantalla recarga desde la API.
 */
@Singleton
class PushInbox @Inject constructor() {
    private val _pending = MutableStateFlow<PushDestination?>(null)
    val pending: StateFlow<PushDestination?> = _pending.asStateFlow()

    fun post(destination: PushDestination) {
        _pending.value = destination
    }

    fun consume(): PushDestination? = _pending.value.also { _pending.value = null }
}
