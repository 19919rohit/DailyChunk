package neunix.dailychunk.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.data.QueueManager

/**
 * Safety net for the one failure mode nothing else covers: the OS silently
 * killing the DownloadService process mid-transfer (rare, but happens on
 * aggressive OEM battery managers). When that happens the DB is left saying
 * DOWNLOADING with nothing scheduled to ever revive it, since normally only
 * WAITING_NEXT_CYCLE downloads get a scheduled worker.
 *
 * During normal operation, updatedAt refreshes at least every ~30s (progress
 * ticks every 1s, and the OkHttp read timeout guarantees an error/retry
 * update within 30s of any stall). So a DOWNLOADING row untouched for 3+
 * minutes reliably means the process died, not that it's just slow.
 *
 * Runs periodically (every 15 minutes, the WorkManager minimum) and once
 * immediately at app cold start.
 */
class RecoveryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val STALE_THRESHOLD_MS = 3 * 60 * 1000L
    }

    override suspend fun doWork(): Result {
        AppContainer.init(applicationContext)
        val repo = AppContainer.repository
        val now = System.currentTimeMillis()
        val all = repo.observeAll().first()

        var requeuedAny = false
        all.filter { it.status == DownloadStatus.DOWNLOADING && now - it.updatedAt > STALE_THRESHOLD_MS }
            .forEach { stale ->
                repo.update(
                    stale.copy(
                        status = DownloadStatus.QUEUED,
                        errorMessage = "Resumed after an interruption",
                        updatedAt = now
                    )
                )
                requeuedAny = true
            }

        if (requeuedAny) {
            QueueManager.tryStartNext(applicationContext)
        }
        return Result.success()
    }
}