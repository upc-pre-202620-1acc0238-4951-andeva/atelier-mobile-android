package com.andeva.atelier.mobile.features.main.application

import com.andeva.atelier.mobile.core.notifications.PushInbox
import javax.inject.Inject

/** Devuelve el push pendiente y lo borra, para navegar una sola vez. La pantalla recarga desde la API. */
class ConsumePushDestinationUseCase @Inject constructor(
    private val pushInbox: PushInbox
) {
    operator fun invoke() = pushInbox.consume()
}
