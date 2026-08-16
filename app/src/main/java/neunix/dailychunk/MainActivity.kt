package neunix.dailychunk

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import neunix.dailychunk.data.AppContainer
import neunix.dailychunk.ui.DailyChunkNavHost
import neunix.dailychunk.ui.theme.DailyChunkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContainer.init(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        val initialId = intent?.getLongExtra("downloadId", -1L) ?: -1L

        setContent {
            DailyChunkTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DailyChunkNavHost(initialDownloadId = if (initialId > 0) initialId else null)
                }
            }
        }
    }
}