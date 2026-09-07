package neunix.dailychunk.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.util.Formatters

@Composable
fun DetailsScreen(viewModel: DownloadViewModel, downloadId: Long, onBack: () -> Unit) {
    val download by viewModel.observe(downloadId).collectAsState(initial = null)
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(download?.fileName ?: "Details", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { padding ->
        val d = download
        if (d == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                Modifier.padding(padding).padding(20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusChip(d.status)

                val progress = if (d.totalBytes > 0) (d.downloadedBytes.toFloat() / d.totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)))

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
                if (d.totalBytes > 0 && d.status != DownloadStatus.COMPLETED && d.status != DownloadStatus.CANCELLED && d.status != DownloadStatus.FAILED) {
                    val remaining = (d.totalBytes - d.downloadedBytes).coerceAtLeast(0)
                    val cyclesLeft = kotlin.math.ceil(remaining.toDouble() / d.cycleLimitBytes.toDouble()).toLong().coerceAtLeast(0)
                    if (cyclesLeft > 0) {
                        val etaMillis = (cyclesLeft - 1).coerceAtLeast(0) * d.cycleIntervalMillis +
                            (if (d.status == DownloadStatus.WAITING_NEXT_CYCLE) (d.nextCycleAtMillis - now).coerceAtLeast(0) else 0L)
                        InfoRow("Estimated completion", "~${formatEta(etaMillis)}, in $cyclesLeft more cycle${if (cyclesLeft == 1L) "" else "s"}")
                    }
                }
                InfoRow("Resume support", if (d.supportsRange) "Supported" else "Not confirmed")
                InfoRow("Saved to", "Download / Daily Chunk / ${d.fileName}")
                InfoRow("URL", d.url)
                if (d.errorMessage != null) InfoRow("Status detail", d.errorMessage)

                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                ) {
                    when (d.status) {
                        DownloadStatus.DOWNLOADING -> Button(onClick = { viewModel.pause(d) }) { Text("Pause", maxLines = 1) }
                        DownloadStatus.PAUSED_MANUAL, DownloadStatus.FAILED -> Button(onClick = { viewModel.resume(d) }) { Text("Resume", maxLines = 1) }
                        DownloadStatus.WAITING_NEXT_CYCLE -> Button(onClick = { viewModel.resume(d) }) { Text("Start next cycle", maxLines = 1) }
                        else -> {}
                    }
                    if (d.status != DownloadStatus.COMPLETED && d.status != DownloadStatus.CANCELLED) {
                        OutlinedButton(onClick = { viewModel.cancel(d); onBack() }) { Text("Cancel", maxLines = 1) }
                    }
                    OutlinedButton(onClick = { showDeleteDialog = true }) { Text("Delete", maxLines = 1) }
                }
            }
        }

        if (showDeleteDialog && d != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete download") },
                text = { Text("Remove \"${d.fileName}\" from DailyChunk? You can also delete the saved file.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.delete(d, deleteFile = true); showDeleteDialog = false; onBack() }) {
                        Text("Delete file too", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.delete(d, deleteFile = false); showDeleteDialog = false; onBack() }) { Text("Keep file") }
                }
            )
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

private fun formatEta(millis: Long): String {
    if (millis <= 0) return "soon"
    val days = millis / (24 * 3600_000L)
    val hours = (millis % (24 * 3600_000L)) / 3600_000L
    return if (days > 0) "${days}d ${hours}h" else Formatters.formatDuration(millis)
}