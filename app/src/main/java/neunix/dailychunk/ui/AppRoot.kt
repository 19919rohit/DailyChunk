package neunix.dailychunk.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun AppRoot(
    initialDownloadId: Long?,
    initialSharedUrl: String?,
    onSharedUrlConsumed: () -> Unit,
    onInitialDownloadConsumed: () -> Unit
) {
    var showIntro by remember { mutableStateOf(true) }

    Crossfade(
        targetState = showIntro,
        animationSpec = tween(260),
        modifier = Modifier.fillMaxSize(),
        label = "splashToApp"
    ) { intro ->
        if (intro) {
            BrandedSplashScreen(onFinished = { showIntro = false })
        } else {
            DailyChunkNavHost(
                initialDownloadId = initialDownloadId,
                initialSharedUrl = initialSharedUrl,
                onSharedUrlConsumed = onSharedUrlConsumed,
                onInitialDownloadConsumed = onInitialDownloadConsumed
            )
        }
    }
}