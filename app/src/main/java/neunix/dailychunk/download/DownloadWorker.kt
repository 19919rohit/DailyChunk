package neunix.dailychunk.download

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.data.QueueManager
import neunix.dailychunk.notification.Notifications
import neunix.dailychunk.util.FilePublisher
import neunix.dailychunk.work.Scheduler

/**
 * The single place actual bytes get transferred. Runs as a WorkManager
 * CoroutineWorker that promotes itself to a foreground job via
 * setForeground() at the very start of doWork() — this is the currently
 * recommended pattern for long-running, user-relevant background transfers
 * (uploads/downloads/sync), and unlike calling startForegroundService()
 * from a background-triggered job, it isn't subject to the Android 12+
 * background-FGS-start restriction that was silently breaking cycles before.
 */
class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        private const val MAX_TRANSIENT_RETRIES = 10
        private val activeEngines = ConcurrentHashMap<Long, DownloadEngine>()

        fun requestPause(id: Long) {
            activeEngines[id]?.requestPause()
        }

        /** Exponential backoff with a cap and jitter, to avoid thundering-herd retries. */
        private fun computeBackoffMillis(retryCount: Int): Long {
            val shift = (retryCount - 1).coerceIn(0, 10)
            val base = 15_000L * (1L shl shift)
            val capped = base.coerceAtMost(20 * 60_000L)
            return capped + Random.nextLong(0, 5000)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        // Called by WorkManager if it needs foreground info before doWork()
        // finishes setting it explicitly (e.g. for expedited work fallback).
        val placeholder = Notifications.progressNotification(applicationContext, "Preparing…", 0, 0, 0, idOrZero())
        return buildForegroundInfo(idOrZero(), placeholder)
    }

    private fun idOrZero() = inputData.getLong(KEY_DOWNLOAD_ID, 0L)

    private fun buildForegroundInfo(id: Long, notification: Notification): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(Notifications.notificationId(id), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(Notifications.notificationId(id), notification)
        }
    }

    override suspend fun doWork(): Result {
        AppContainer.init(applicationContext)
        val repo = AppContainer.repository
        val id = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        if (id == -1L) return Result.failure()

        var download = repo.getById(id) ?: return Result.failure()
        if (download.status == DownloadStatus.COMPLETED || download.status == DownloadStatus.CANCELLED) {
            return Result.success()
        }

        setForeground(
            buildForegroundInfo(
                id,
                Notifications.progressNotification(applicationContext, download.fileName, download.downloadedBytes, download.totalBytes, 0L, id)
            )
        )

        val prefs = AppContainer.prefsState.value
        val engine = DownloadEngine(NetworkClientProvider.client)
        activeEngines[id] = engine

        download = download.copy(
            status = DownloadStatus.DOWNLOADING,
            cycleUsedBytes = 0L,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )
        repo.update(download)

        PowerHolder.acquire(applicationContext)
        val result = try {
            engine.runCycle(download) { totalDownloaded, cycleUsed, speed ->
                val current = repo.getById(id) ?: return@runCycle
                repo.update(
                    current.copy(
                        downloadedBytes = totalDownloaded,
                        cycleUsedBytes = cycleUsed,
                        lastSpeedBps = speed,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                NotificationManagerCompat.from(applicationContext).notify(
                    Notifications.notificationId(id),
                    Notifications.progressNotification(applicationContext, download.fileName, totalDownloaded, download.totalBytes, speed, id)
                )
            }
        } finally {
            activeEngines.remove(id)
            PowerHolder.release()
        }

        val outcome = handleResult(id, result, prefs.notificationsEnabled)
        QueueManager.tryStartNext(applicationContext)
        return outcome
    }

    private suspend fun handleResult(id: Long, result: DownloadEngine.CycleResult, notificationsEnabled: Boolean): Result {
        val repo = AppContainer.repository
        val latest = repo.getById(id) ?: return Result.success()

        return when (result) {
            is DownloadEngine.CycleResult.Completed -> {
                val workFile = File(latest.destinationPath)
                val finalSize = if (workFile.exists()) workFile.length() else latest.downloadedBytes
                val published = FilePublisher.publish(applicationContext, workFile, latest.fileName, latest.mimeType)
                if (published) {
                    workFile.delete()
                    repo.update(
                        latest.copy(
                            status = DownloadStatus.COMPLETED,
                            downloadedBytes = finalSize,
                            totalBytes = if (latest.totalBytes > 0) latest.totalBytes else finalSize,
                            nextCycleAtMillis = 0L,
                            retryCount = 0,
                            errorMessage = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    if (notificationsEnabled) Notifications.showCompleted(applicationContext, latest.fileName, id)
                    Result.success()
                } else {
                    repo.update(
                        latest.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = "Could not save file to Downloads. Check storage permission and free space.",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    if (notificationsEnabled) Notifications.showFailed(applicationContext, latest.fileName, "Could not save file", id)
                    Result.failure()
                }
            }
            is DownloadEngine.CycleResult.CycleLimitReached -> {
                val nextAt = System.currentTimeMillis() + latest.cycleIntervalMillis
                repo.update(
                    latest.copy(
                        status = DownloadStatus.WAITING_NEXT_CYCLE,
                        nextCycleAtMillis = nextAt,
                        retryCount = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                Scheduler.scheduleDelayed(applicationContext, id, latest.cycleIntervalMillis)
                if (notificationsEnabled) Notifications.showWaiting(applicationContext, latest.fileName, latest.cycleIntervalMillis, id)
                Result.success()
            }
            is DownloadEngine.CycleResult.ManuallyPaused -> {
                repo.update(latest.copy(status = DownloadStatus.PAUSED_MANUAL, updatedAt = System.currentTimeMillis()))
                Result.success()
            }
            is DownloadEngine.CycleResult.Error -> {
                if (result.isPermanent) {
                    repo.update(
                        latest.copy(status = DownloadStatus.FAILED, errorMessage = result.message, updatedAt = System.currentTimeMillis())
                    )
                    if (notificationsEnabled) Notifications.showFailed(applicationContext, latest.fileName, result.message, id)
                    Result.failure()
                } else {
                    val nextRetryCount = latest.retryCount + 1
                    if (nextRetryCount <= MAX_TRANSIENT_RETRIES) {
                        val delayMs = result.retryAfterMillis ?: computeBackoffMillis(nextRetryCount)
                        repo.update(
                            latest.copy(
                                status = DownloadStatus.RETRYING,
                                retryCount = nextRetryCount,
                                errorMessage = result.message,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        Scheduler.scheduleDelayed(applicationContext, id, delayMs)
                    } else {
                        repo.update(
                            latest.copy(status = DownloadStatus.FAILED, errorMessage = result.message, updatedAt = System.currentTimeMillis())
                        )
                        if (notificationsEnabled) Notifications.showFailed(applicationContext, latest.fileName, result.message, id)
                    }
                    Result.success()
                }
            }
        }
    }
}