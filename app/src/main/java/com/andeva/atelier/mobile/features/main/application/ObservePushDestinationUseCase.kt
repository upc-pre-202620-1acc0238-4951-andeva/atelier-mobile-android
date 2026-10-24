package com.andeva.atelier.mobile.features.main.application

import com.andeva.atelier.mobile.core.notifications.PushInbox
import javax.inject.Inject

/** Push tocado pendiente de abrir (docs/04-mobile-integration.md §2). */
class ObservePushDestinationUseCase @Inject constructor(
    private val pushInbox: PushInbox
) {
    operator fun invoke() = pushInbox.pending
}
