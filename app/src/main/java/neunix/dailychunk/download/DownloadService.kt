package neunix.dailychunk.download

import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlinx.coroutines.launch
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.data.QueueManager
import neunix.dailychunk.notification.Notifications
import neunix.dailychunk.util.FilePublisher
import neunix.dailychunk.util.NetworkUtils
import neunix.dailychunk.work.Scheduler
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

class DownloadService : LifecycleService() {

    companion object {
        const val ACTION_START = "neunix.dailychunk.action.START"
        const val EXTRA_ID = "extra_download_id"
        private const val MAX_TRANSIENT_RETRIES = 8
        private val activeEngines = ConcurrentHashMap<Long, DownloadEngine>()

        fun start(context: Context, id: Long) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ID, id)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun requestPause(id: Long) {
            activeEngines[id]?.requestPause()
        }

        /** Exponential backoff with a cap and small jitter to avoid thundering-herd retries. */
        private fun computeBackoffMillis(retryCount: Int): Long {
            val shift = (retryCount - 1).coerceIn(0, 10)
            val base = 15_000L * (1L shl shift)
            val capped = base.coerceAtMost(20 * 60_000L)
            return capped + Random.nextLong(0, 5000)
        }
    }

    private val serviceClient by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 12
            maxRequestsPerHost = 8
        }
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val runningJobs = AtomicInteger(0)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val id = intent?.getLongExtra(EXTRA_ID, -1L) ?: -1L
        if (intent?.action == ACTION_START && id != -1L) {
            startForeground(
                Notifications.notificationId(id),
                Notifications.progressNotification(this, "Starting…", 0, 0, 0, id)
            )
            runningJobs.incrementAndGet()
            lifecycleScope.launch {
                try {
                    runDownload(id)
                } finally {
                    val remaining = runningJobs.decrementAndGet()
                    QueueManager.tryStartNext(applicationContext)
                    if (remaining <= 0) {
                        stopForeground(Service.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun runDownload(id: Long) {
        AppContainer.init(applicationContext)
        val repo = AppContainer.repository
        var download = repo.getById(id) ?: return
        if (download.status == DownloadStatus.COMPLETED || download.status == DownloadStatus.CANCELLED) return

        val prefs = AppContainer.prefsState.value

        if (prefs.wifiOnly && !NetworkUtils.isWifiConnected(applicationContext)) {
            repo.update(
                download.copy(
                    status = DownloadStatus.RETRYING,
                    errorMessage = "Waiting for Wi-Fi",
                    updatedAt = System.currentTimeMillis()
                )
            )
            Scheduler.scheduleCycle(applicationContext, id, 5 * 60_000L)
            return
        }

        val engine = DownloadEngine(serviceClient)
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
                NotificationManagerCompat.from(this@DownloadService).notify(
                    Notifications.notificationId(id),
                    Notifications.progressNotification(
                        this@DownloadService, download.fileName, totalDownloaded, download.totalBytes, speed, id
                    )
                )
            }
        } finally {
            activeEngines.remove(id)
            PowerHolder.release()
        }

        val latest = repo.getById(id) ?: return
        when (result) {
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
                    if (prefs.notificationsEnabled) Notifications.showCompleted(this, latest.fileName, id)
                } else {
                    repo.update(
                        latest.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = "Could not save file to Downloads. Check storage permission and free space.",
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    if (prefs.notificationsEnabled) Notifications.showFailed(this, latest.fileName, "Could not save file", id)
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
                Scheduler.scheduleCycle(applicationContext, id, latest.cycleIntervalMillis)
                if (prefs.notificationsEnabled) Notifications.showWaiting(this, latest.fileName, latest.cycleIntervalMillis, id)
            }
            is DownloadEngine.CycleResult.ManuallyPaused -> {
                repo.update(latest.copy(status = DownloadStatus.PAUSED_MANUAL, updatedAt = System.currentTimeMillis()))
            }
            is DownloadEngine.CycleResult.Error -> {
                if (result.isPermanent) {
                    repo.update(
                        latest.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = result.message,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    if (prefs.notificationsEnabled) Notifications.showFailed(this, latest.fileName, result.message, id)
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
                        Scheduler.scheduleCycle(applicationContext, id, delayMs)
                    } else {
                        repo.update(
                            latest.copy(
                                status = DownloadStatus.FAILED,
                                errorMessage = result.message,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        if (prefs.notificationsEnabled) Notifications.showFailed(this, latest.fileName, result.message, id)
                    }
                }
            }
        }
    }
}