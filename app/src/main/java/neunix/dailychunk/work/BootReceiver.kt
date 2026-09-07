package neunix.dailychunk.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.data.QueueManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppContainer.init(context.applicationContext)
                val downloads = AppContainer.repository.observeAll().first()

                downloads.filter { it.status == DownloadStatus.WAITING_NEXT_CYCLE }.forEach {
                    val delay = (it.nextCycleAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
                    Scheduler.scheduleDelayed(context.applicationContext, it.id, delay)
                }

                // A download mid-retry-backoff when the device rebooted has no
                // persisted "next attempt" timestamp to recompute — safest is
                // to requeue it and let the normal queue pick it up shortly.
                val hadStaleRetries = downloads.any { it.status == DownloadStatus.RETRYING }
                if (hadStaleRetries) {
                    downloads.filter { it.status == DownloadStatus.RETRYING }.forEach {
                        AppContainer.repository.update(
                            it.copy(status = DownloadStatus.QUEUED, updatedAt = System.currentTimeMillis())
                        )
                    }
                    QueueManager.tryStartNext(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}