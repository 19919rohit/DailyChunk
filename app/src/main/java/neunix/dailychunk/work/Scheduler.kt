package neunix.dailychunk.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.download.DownloadWorker

object Scheduler {
    private fun workName(id: Long) = "download_$id"
    private const val RECOVERY_WORK_NAME = "recovery_check"

    /**
     * NetworkType.UNMETERED is the practical WorkManager equivalent of
     * "Wi-Fi only": it only unblocks on networks the system doesn't bill
     * per-byte for. NetworkType.CONNECTED means "any network" and, crucially,
     * will sit dormant — not fail — until connectivity returns. This is what
     * makes "cycle time arrives with no internet" self-heal automatically:
     * the job is already enqueued and simply waits for the constraint.
     */
    private fun networkConstraint(): Constraints {
        val wifiOnly = AppContainer.prefsState.value.wifiOnly
        val type = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        return Constraints.Builder().setRequiredNetworkType(type).build()
    }

    /**
     * User-initiated, right-now start (Start Now, Resume, a queue slot
     * freeing up). Expedited jobs run near-instantly instead of waiting in
     * the normal job queue — this is what fixes the "takes a minute to
     * start" problem. Expedited work cannot carry an initial delay, which is
     * fine here since this path is always "start immediately."
     */
    fun startNow(context: Context, id: Long) {
        val data = Data.Builder().putLong(DownloadWorker.KEY_DOWNLOAD_ID, id).build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .setConstraints(networkConstraint())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WHEN_QUOTA_EXCEEDED)
            .addTag(workName(id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(id), ExistingWorkPolicy.REPLACE, request)
    }

    /**
     * Cycle waits and retry backoffs. Not expedited (expedited jobs can't be
     * scheduled for later), but the network constraint still applies, so a
     * cycle due while offline simply waits for connectivity rather than
     * failing.
     */
    fun scheduleDelayed(context: Context, id: Long, delayMillis: Long) {
        val data = Data.Builder().putLong(DownloadWorker.KEY_DOWNLOAD_ID, id).build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setConstraints(networkConstraint())
            .addTag(workName(id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(id), ExistingWorkPolicy.REPLACE, request)
    }
    
    fun scheduleCycle(context: Context, id: Long, delayMillis: Long) {
    scheduleDelayed(context, id, delayMillis)
}

    fun cancel(context: Context, id: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
    }

    fun schedulePeriodicRecovery(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecoveryWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(RECOVERY_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun runImmediateRecoveryCheck(context: Context) {
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<RecoveryWorker>().build())
    }
}