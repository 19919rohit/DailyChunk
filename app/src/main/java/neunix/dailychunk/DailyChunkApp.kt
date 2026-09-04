package neunix.dailychunk

import android.app.Application
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.notification.Notifications
import neunix.dailychunk.work.Scheduler

class DailyChunkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        Notifications.ensureChannels(this)
        Scheduler.schedulePeriodicRecovery(this)
        Scheduler.runImmediateRecoveryCheck(this)
    }
}