package neunix.dailychunk.data

import android.content.Context
import kotlinx.coroutines.flow.first
import neunix.dailychunk.download.DownloadService

/**
 * A deliberately simple queue: at most `maxConcurrentDownloads` downloads may be
 * DOWNLOADING at once. Anything else waits in QUEUED until a slot frees up.
 */
object QueueManager {

    suspend fun tryStartNext(context: Context) {
        val prefs = AppContainer.prefsState.value
        val all = AppContainer.repository.observeAll().first()
        val activeCount = all.count { it.status == DownloadStatus.DOWNLOADING }
        if (activeCount >= prefs.maxConcurrentDownloads) return

        val next = all.filter { it.status == DownloadStatus.QUEUED }
            .minByOrNull { it.createdAt } ?: return

        DownloadService.start(context, next.id)
    }

    suspend fun hasFreeSlot(context: Context): Boolean {
        val prefs = AppContainer.prefsState.value
        val activeCount = AppContainer.repository.observeAll().first()
            .count { it.status == DownloadStatus.DOWNLOADING }
        return activeCount < prefs.maxConcurrentDownloads
    }
}