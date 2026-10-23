package com.andeva.atelier.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.notifications.PushInbox
import com.andeva.atelier.mobile.core.notifications.PushRouter
import com.andeva.atelier.mobile.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var pushInbox: PushInbox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handlePush(intent)
        setContent {
            AtelierWorkshopTheme {
                AppNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePush(intent)
    }

    /** Push tocado: los extras traen `data` del mensaje (`type` + ids). */
    private fun handlePush(intent: Intent?) {
        val extras = intent?.extras ?: return
        val data = extras.keySet().associateWith { extras.getString(it) }
        PushRouter.route(data)?.let(pushInbox::post)
    }
}
