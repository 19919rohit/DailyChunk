package neunix.dailychunk

import android.app.Application
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.notification.Notifications
import neunix.dailychunk.ui.theme.ThemePreferences

class DailyChunkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
        ThemePreferences.init(this)
        Notifications.ensureChannel(this)
    }
}