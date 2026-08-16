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
import kotlinx.coroutines.launch
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.notification.Notifications
import neunix.dailychunk.work.Scheduler
import okhttp3.OkHttpClient

class DownloadService : LifecycleService() {

    companion object {
        const val ACTION_START = "neunix.dailychunk.action.START"
        const val EXTRA_ID = "extra_download_id"
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
    }

    private val serviceClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private var runningJobs = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val id = intent?.getLongExtra(EXTRA_ID, -1L) ?: -1L
        if (intent?.action == ACTION_START && id != -1L) {
            startForeground(
                Notifications.notificationId(id),
                Notifications.progressNotification(this, "Starting…", 0, 0, 0, id)
            )
            runningJobs++
            lifecycleScope.launch {
                try {
                    runDownload(id)
                } finally {
                    runningJobs--
                    if (runningJobs <= 0) {
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

        val engine = DownloadEngine(serviceClient)
        activeEngines[id] = engine

        download = download.copy(
            status = DownloadStatus.DOWNLOADING,
            cycleUsedBytes = 0L,
            errorMessage = null,
            updatedAt = System.currentTimeMillis()
        )
        repo.update(download)

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
        }

        val latest = repo.getById(id) ?: return
        when (result) {
            is DownloadEngine.CycleResult.Completed -> {
                val finalSize = File(latest.destinationPath).let { if (it.exists()) it.length() else latest.downloadedBytes }
                repo.update(
                    latest.copy(
                        status = DownloadStatus.COMPLETED,
                        downloadedBytes = finalSize,
                        totalBytes = if (latest.totalBytes > 0) latest.totalBytes else finalSize,
                        nextCycleAtMillis = 0L,
                        retryCount = 0,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                Notifications.showCompleted(this, latest.fileName, id)
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
                Notifications.showWaiting(this, latest.fileName, latest.cycleIntervalMillis, id)
            }
            is DownloadEngine.CycleResult.ManuallyPaused -> {
                repo.update(latest.copy(status = DownloadStatus.PAUSED_MANUAL, updatedAt = System.currentTimeMillis()))
            }
            is DownloadEngine.CycleResult.Error -> {
                val nextRetryCount = latest.retryCount + 1
                if (nextRetryCount <= 3) {
                    val delayMs = 10_000L * nextRetryCount
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
                    Notifications.showFailed(this, latest.fileName, result.message, id)
                }
            }
        }
    }
}