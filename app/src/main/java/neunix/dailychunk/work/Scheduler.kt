package neunix.dailychunk.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Scheduler {
    private fun workName(id: Long) = "cycle_$id"

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
}