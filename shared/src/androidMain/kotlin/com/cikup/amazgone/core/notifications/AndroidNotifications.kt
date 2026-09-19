package com.cikup.amazgone.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.cikup.amazgone.notifications.domain.repository.SystemNotifications
import java.util.concurrent.TimeUnit

const val EXTRA_LINK = "amazgone_link"
private const val CHANNEL_ID = "amazgone_updates"
private const val KEY_ID = "id"
private const val KEY_TITLE = "title"
private const val KEY_BODY = "body"
private const val KEY_LINK = "link"

/** Android: WorkManager fires at the due time (survives app death and reboots) and posts the notification. */
class AndroidSystemNotifications(private val context: Context) : SystemNotifications {
    override fun schedule(id: String, title: String, body: String, atMillis: Long, link: String) {
        val delay = (atMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_ID to id, KEY_TITLE to title, KEY_BODY to body, KEY_LINK to link))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(id), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(id: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
    }

    private fun workName(id: String) = "notification-$id"
}

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!canNotify(context)) return Result.success()
        ensureChannel(context)
        val link = inputData.getString(KEY_LINK).orEmpty()
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply { putExtra(EXTRA_LINK, link) }
        val id = inputData.getString(KEY_ID).orEmpty()
        val tap = launch?.let { PendingIntent.getActivity(context, id.hashCode(), it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(inputData.getString(KEY_TITLE))
            .setContentText(inputData.getString(KEY_BODY))
            .setStyle(NotificationCompat.BigTextStyle().bigText(inputData.getString(KEY_BODY)))
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        @Suppress("MissingPermission") // checked in canNotify
        NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
        return Result.success()
    }
}

private fun canNotify(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java)
    if (manager.getNotificationChannel(CHANNEL_ID) == null) {
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Deliveries & rewards", NotificationManager.IMPORTANCE_DEFAULT))
    }
}

@Composable
actual fun rememberNotificationPermission(): NotificationPermission {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(canNotify(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    return remember(granted) {
        object : NotificationPermission {
            override val granted = granted
            override fun request() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
