package neunix.dailychunk.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import neunix.dailychunk.MainActivity
import neunix.dailychunk.util.Formatters

object Notifications {
    private const val PROGRESS_CHANNEL_ID = "downloads_progress"
    private const val EVENTS_CHANNEL_ID = "downloads_events"
    private const val ID_BASE = 1000

    /**
     * Two channels, matching Android's own guidance: a silent, low-importance
     * channel for the ongoing progress notification (updates constantly,
     * must never make noise), and a separate default-importance channel for
     * one-off events — completed, waiting, failed. Sharing a single LOW
     * channel for everything is why completion/failure alerts were easy to
     * miss: LOW importance never shows a heads-up or plays a sound.
     */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(PROGRESS_CHANNEL_ID, "Download progress", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Ongoing progress for active downloads"
                    setShowBadge(false)
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(EVENTS_CHANNEL_ID, "Download updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Completed, waiting, and failed download alerts"
                }
            )
        }
    }

    fun notificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notificationId(downloadId: Long): Int = ID_BASE + downloadId.toInt()

    fun progressNotification(
        context: Context, fileName: String, downloaded: Long, total: Long, speed: Long, downloadId: Long
    ): Notification {
        ensureChannels(context)
        val percent = if (total > 0) ((downloaded * 100) / total).toInt() else -1
        val builder = NotificationCompat.Builder(context, PROGRESS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(fileName)
            .setContentText(
                if (percent >= 0) "$percent% • ${Formatters.formatSpeed(speed)}"
                else "${Formatters.formatBytes(downloaded)} • ${Formatters.formatSpeed(speed)}"
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(contentIntent(context, downloadId))
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (percent >= 0) builder.setProgress(100, percent, false) else builder.setProgress(0, 0, true)
        return builder.build()
    }

    fun showCompleted(context: Context, fileName: String, downloadId: Long) {
        if (!notificationsEnabled(context)) return
        ensureChannels(context)
        val notif = NotificationCompat.Builder(context, EVENTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText(fileName)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, downloadId))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(downloadId), notif)
    }

    fun showWaiting(context: Context, fileName: String, intervalMillis: Long, downloadId: Long) {
        if (!notificationsEnabled(context)) return
        ensureChannels(context)
        val notif = NotificationCompat.Builder(context, EVENTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Daily limit reached")
            .setContentText("$fileName will resume in ${Formatters.formatDuration(intervalMillis)}")
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, downloadId))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(downloadId), notif)
    }

    fun showFailed(context: Context, fileName: String, error: String, downloadId: Long) {
        if (!notificationsEnabled(context)) return
        ensureChannels(context)
        val notif = NotificationCompat.Builder(context, EVENTS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download failed")
            .setContentText("$fileName — $error")
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, downloadId))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(downloadId), notif)
    }

    private fun contentIntent(context: Context, downloadId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("downloadId", downloadId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, downloadId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}