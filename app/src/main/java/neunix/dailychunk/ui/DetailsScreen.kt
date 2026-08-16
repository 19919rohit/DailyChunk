package neunix.dailychunk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.util.Formatters

@Composable
fun DetailsScreen(viewModel: DownloadViewModel, downloadId: Long, onBack: () -> Unit) {
    val download by viewModel.observe(downloadId).collectAsState(initial = null)
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(download?.fileName ?: "Details") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            }
        )
    }) { padding ->
        val d = download
        if (d == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier.padding(padding).padding(20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusChip(d.status)

                val progress = if (d.totalBytes > 0)
                    (d.downloadedBytes.toFloat() / d.totalBytes.toFloat()).coerceIn(0f, 1f)
                else 0f
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                )

                InfoRow("Downloaded", "${Formatters.formatBytes(d.downloadedBytes)} / ${if (d.totalBytes > 0) Formatters.formatBytes(d.totalBytes) else "Unknown"}")
                if (d.status == DownloadStatus.DOWNLOADING) {
                    InfoRow("Speed", Formatters.formatSpeed(d.lastSpeedBps))
                    InfoRow("This cycle", "${Formatters.formatBytes(d.cycleUsedBytes)} / ${Formatters.formatBytes(d.cycleLimitBytes)}")
                }
                InfoRow("Cycle limit", Formatters.formatBytes(d.cycleLimitBytes))
                InfoRow("Cycle interval", Formatters.formatDuration(d.cycleIntervalMillis))
                if (d.status == DownloadStatus.WAITING_NEXT_CYCLE) {
                    InfoRow("Next cycle", Formatters.formatDuration((d.nextCycleAtMillis - now).coerceAtLeast(0)))
                }
                InfoRow("Resume support", if (d.supportsRange) "Supported" else "Not confirmed")
                InfoRow("Destination", d.destinationPath)
                InfoRow("URL", d.url)
                if (d.errorMessage != null) InfoRow("Error", d.errorMessage)

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (d.status) {
                        DownloadStatus.DOWNLOADING -> Button(onClick = { viewModel.pause(d) }) { Text("Pause") }
                        DownloadStatus.PAUSED_MANUAL, DownloadStatus.FAILED -> Button(onClick = { viewModel.resume(d) }) { Text("Resume") }
                        DownloadStatus.WAITING_NEXT_CYCLE -> Button(onClick = { viewModel.resume(d) }) { Text("Start next cycle") }
                        else -> {}
                    }
                    if (d.status != DownloadStatus.COMPLETED && d.status != DownloadStatus.CANCELLED) {
                        OutlinedButton(onClick = { viewModel.cancel(d); onBack() }) { Text("Cancel") }
                    }
                    OutlinedButton(onClick = { viewModel.delete(d); onBack() }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}