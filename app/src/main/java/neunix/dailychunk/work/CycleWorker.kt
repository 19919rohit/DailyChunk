package neunix.dailychunk.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.download.DownloadService

class CycleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
    }

    override suspend fun doWork(): Result {
        AppContainer.init(applicationContext)
        val id = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        if (id == -1L) return Result.failure()
        DownloadService.start(applicationContext, id)
        return Result.success()
    }
}