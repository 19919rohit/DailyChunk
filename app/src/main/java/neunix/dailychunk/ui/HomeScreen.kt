package neunix.dailychunk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.CloudDownload
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import neunix.dailychunk.data.DownloadEntity
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.util.Formatters

@Composable
fun HomeScreen(
    viewModel: DownloadViewModel,
    onAddClick: () -> Unit,
    onOpenDetails: (Long) -> Unit
) {
    val allDownloads by viewModel.downloads.collectAsState()
    val downloads = allDownloads.filter {
        it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.CANCELLED
    }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = { HomeHeader(activeCount = downloads.count { it.status == DownloadStatus.DOWNLOADING }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add download") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (downloads.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(downloads, key = { it.id }) { d ->
                    DownloadCard(
                        download = d,
                        now = now,
                        onClick = { onOpenDetails(d.id) },
                        onPause = { viewModel.pause(d) },
                        onResume = { viewModel.resume(d) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(activeCount: Int) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
        Text("DailyChunk", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            if (activeCount > 0) "$activeCount download${if (activeCount == 1) "" else "s"} in progress"
            else "Big downloads. Small daily chunks.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(88.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(
                    Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("No active downloads", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap \"Add download\" to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusChip(status: DownloadStatus) {
    val (label, color) = when (status) {
        DownloadStatus.QUEUED -> "Queued" to MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.DOWNLOADING -> "Downloading" to MaterialTheme.colorScheme.primary
        DownloadStatus.PAUSED_MANUAL -> "Paused" to MaterialTheme.colorScheme.onSurfaceVariant
        DownloadStatus.WAITING_NEXT_CYCLE -> "Waiting" to MaterialTheme.colorScheme.tertiary
        DownloadStatus.RETRYING -> "Retrying" to MaterialTheme.colorScheme.tertiary
        DownloadStatus.COMPLETED -> "Completed" to MaterialTheme.colorScheme.secondary
        DownloadStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
        DownloadStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun DownloadCard(
    download: DownloadEntity,
    now: Long,
    onClick: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    download.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(download.status)
            }
            Spacer(Modifier.height(12.dp))

            val progress = if (download.totalBytes > 0)
                (download.downloadedBytes.toFloat() / download.totalBytes.toFloat()).coerceIn(0f, 1f)
            else 0f
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${Formatters.formatBytes(download.downloadedBytes)} / " +
                        (if (download.totalBytes > 0) Formatters.formatBytes(download.totalBytes) else "?"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (download.totalBytes > 0) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            when (download.status) {
                DownloadStatus.DOWNLOADING -> {
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text("This cycle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${Formatters.formatBytes(download.cycleUsedBytes)} / ${Formatters.formatBytes(download.cycleLimitBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                Formatters.formatSpeed(download.lastSpeedBps),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                DownloadStatus.WAITING_NEXT_CYCLE -> {
                    Spacer(Modifier.height(10.dp))
                    val remaining = (download.nextCycleAtMillis - now).coerceAtLeast(0)
                    Text(
                        "Daily limit reached — resumes in ${Formatters.formatDuration(remaining)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DownloadStatus.FAILED -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        download.errorMessage ?: "Failed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                DownloadStatus.RETRYING -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        download.errorMessage ?: "Retrying…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                else -> {}
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                when (download.status) {
                    DownloadStatus.DOWNLOADING -> TextButton(onClick = onPause) { Text("Pause") }
                    DownloadStatus.PAUSED_MANUAL, DownloadStatus.FAILED -> TextButton(onClick = onResume) { Text("Resume") }
                    DownloadStatus.WAITING_NEXT_CYCLE -> TextButton(onClick = onResume) { Text("Start now") }
                    else -> {}
                }
            }
        }
    }
}