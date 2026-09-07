package neunix.dailychunk.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Reusable empty-state visual. Loads its animation purely by asset filename
 * from app/src/main/assets/lottie/ — so refreshing the look of any empty
 * state later is a drop-in file swap (same filename, new export from
 * LottieFiles or After Effects/Bodymovin). No code changes needed.
 *
 * By default, animations play indefinitely to preserve existing behavior.
 *
 * Pass a custom [iterations] value when an animation should only play a
 * specific number of times.
 *
 * For example:
 *     iterations = 1
 *
 * plays the animation once and leaves it on its final frame.
 *
 * Falls back to the caller's own icon if the asset is missing, still
 * loading, or fails to parse, so a bad or absent animation file can never
 * leave a screen blank.
 */
@Composable
fun LottieEmptyState(
    assetName: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iterations: Int = LottieConstants.IterateForever,
    fallback: @Composable () -> Unit = {}
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("lottie/$assetName")
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (composition != null) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(160.dp)
            )
        } else {
            Spacer(Modifier.height(72.dp))
            fallback()
            Spacer(Modifier.height(72.dp))
        }

        Text(
            title,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(4.dp))

        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}