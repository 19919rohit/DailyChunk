package neunix.dailychunk.data

import android.content.Context
import kotlinx.coroutines.flow.first
import neunix.dailychunk.work.Scheduler

/**
 * At most maxConcurrentDownloads may be DOWNLOADING at once (no upper cap —
 * whatever the user sets in Settings, including arbitrarily large custom
 * values). Anything else waits in QUEUED until a slot frees up.
 */
object QueueManager {

    @Synchronized
    suspend fun tryStartNext(context: Context) {
        val prefs = AppContainer.prefsState.value
        val all = AppContainer.repository.observeAll().first()
        val activeCount = all.count { it.status == DownloadStatus.DOWNLOADING }
        if (activeCount >= prefs.maxConcurrentDownloads) return

        val next = all.filter { it.status == DownloadStatus.QUEUED }
            .minByOrNull { it.createdAt } ?: return

        // Optimistically claim the slot before the worker actually starts,
        // so a burst of calls to tryStartNext (e.g. several downloads
        // finishing at once) can't all see the same free slot and over-enqueue.
        AppContainer.repository.update(
            next.copy(status = DownloadStatus.DOWNLOADING, updatedAt = System.currentTimeMillis())
        )
        Scheduler.startNow(context, next.id)
    }
}