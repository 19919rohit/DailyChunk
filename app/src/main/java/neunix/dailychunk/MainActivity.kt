package neunix.dailychunk

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.ui.AppRoot
import neunix.dailychunk.ui.theme.DailyChunkTheme
import neunix.dailychunk.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled via the Settings banner */ }

    private val sharedUrlState = mutableStateOf<String?>(null)
    private val openedDownloadId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() must run before super.onCreate() — that's
        // what lets the system paint the splash instantly on tap, before any
        // Kotlin/Compose classes have even loaded.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        AppContainer.init(applicationContext)

        // AppContainer.init() above is synchronous (Room + DataStore setup),
        // so by this line the app's real data layer is genuinely ready. The
        // splash is held for exactly that long — never an artificial delay,
        // and never dismissed before the app can actually respond.
        var appReady = false
        splashScreen.setKeepOnScreenCondition { !appReady }
        appReady = true

        // A subtle custom exit — fade + gentle icon pop-out — instead of the
        // default instant cut, matching the animated feel of the branded
        // intro that follows it.
        splashScreen.setOnExitAnimationListener { provider ->
            val fadeOut = ObjectAnimator.ofFloat(provider.view, "alpha", 1f, 0f).apply {
                duration = 220L
                interpolator = AnticipateInterpolator()
            }
            val scaleX = ObjectAnimator.ofFloat(provider.iconView, "scaleX", 1f, 1.15f).apply { duration = 220L }
            val scaleY = ObjectAnimator.ofFloat(provider.iconView, "scaleY", 1f, 1.15f).apply { duration = 220L }
            fadeOut.doOnEnd { provider.remove() }
            scaleX.start()
            scaleY.start()
            fadeOut.start()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        handleIntent(intent)

        setContent {
            val prefs by AppContainer.prefsState.collectAsState()
            val mode = try { ThemeMode.valueOf(prefs.themeMode) } catch (e: Exception) { ThemeMode.LIGHT }
            val sharedUrl by sharedUrlState
            val downloadId by openedDownloadId

            DailyChunkTheme(mode = mode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(
                        initialDownloadId = downloadId,
                        initialSharedUrl = sharedUrl,
                        onSharedUrlConsumed = { sharedUrlState.value = null },
                        onInitialDownloadConsumed = { openedDownloadId.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val id = intent.getLongExtra("downloadId", -1L)
        if (id > 0) openedDownloadId.value = id

        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val url = text?.trim()?.let { candidate -> Regex("https?://\\S+").find(candidate)?.value }
            if (!url.isNullOrBlank()) sharedUrlState.value = url
        }
    }
}