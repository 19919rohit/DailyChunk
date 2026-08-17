package neunix.dailychunk

import android.app.Application
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.notification.Notifications

class DailyChunkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        Notifications.ensureChannel(this)
    }
}