package neunix.dailychunk.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.data.DownloadStatus

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppContainer.init(context.applicationContext)
                val downloads = AppContainer.repository.observeAll().first()
                downloads.filter { it.status == DownloadStatus.WAITING_NEXT_CYCLE }.forEach {
                    val delay = (it.nextCycleAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
                    Scheduler.scheduleCycle(context.applicationContext, it.id, delay)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}