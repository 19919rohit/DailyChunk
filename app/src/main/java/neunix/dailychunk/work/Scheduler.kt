package neunix.dailychunk.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Scheduler {
    private fun workName(id: Long) = "cycle_$id"
    private const val RECOVERY_WORK_NAME = "recovery_check"

    /**
     * Every scheduled cycle requires NetworkType.CONNECTED. This means a
     * retry due to lost connectivity doesn't burn attempts polling — the
     * job simply waits, dormant, until the network returns, then fires.
     */
    fun scheduleCycle(context: Context, id: Long, delayMillis: Long) {
        val data = Data.Builder().putLong(CycleWorker.KEY_DOWNLOAD_ID, id).build()
        val request = OneTimeWorkRequestBuilder<CycleWorker>()
            .setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(data)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(workName(id))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(id), ExistingWorkPolicy.REPLACE, request)
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
        val request = OneTimeWorkRequestBuilder<RecoveryWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}