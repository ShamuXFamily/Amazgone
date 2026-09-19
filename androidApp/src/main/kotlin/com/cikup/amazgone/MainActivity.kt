package com.cikup.amazgone

import android.content.Intent
import android.os.Bundle
import com.cikup.amazgone.core.notifications.DeepLinks
import com.cikup.amazgone.core.notifications.EXTRA_LINK
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openNotificationLink(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openNotificationLink(intent)

        setContent {
            App()
        }
    }
}

/** A tapped notification launches the activity with its deep link. */
private fun openNotificationLink(intent: Intent?) {
    intent?.getStringExtra(EXTRA_LINK)?.let(DeepLinks::open)
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}