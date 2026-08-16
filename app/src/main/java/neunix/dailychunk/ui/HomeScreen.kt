package neunix.dailychunk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import neunix.dailychunk.data.DownloadEntity
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.ui.theme.ThemeMode
import neunix.dailychunk.ui.theme.ThemePreferences
import neunix.dailychunk.util.Formatters

@Composable
fun HomeScreen(
    viewModel: DownloadViewModel,
    onAddClick: () -> Unit,
    onOpenDetails: (Long) -> Unit
) {
    val downloads by viewModel.downloads.collectAsState()
    val themeMode by ThemePreferences.themeMode.collectAsState()
    val context = LocalContext.current

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DailyChunk", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Big downloads. Small daily chunks.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val next = when (themeMode) {
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.SYSTEM
                            ThemeMode.SYSTEM -> ThemeMode.LIGHT
                        }
                        ThemePreferences.setThemeMode(context, next)
                    }) {
                        Icon(
                            imageVector = when (themeMode) {
                                ThemeMode.LIGHT -> Icons.Outlined.LightMode
                                ThemeMode.DARK -> Icons.Outlined.DarkMode
                                ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                            },
                            contentDescription = "Toggle theme"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add download") }
            )
        }
    ) { padding ->
        if (downloads.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
fun EmptyState(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(
            Icons.Outlined.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text("No downloads yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Add a download to get started", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                StatusChip(download.status)
            }
            Spacer(Modifier.height(10.dp))

            val progress = if (download.totalBytes > 0)
                (download.downloadedBytes.toFloat() / download.totalBytes.toFloat()).coerceIn(0f, 1f)
            else 0f
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${Formatters.formatBytes(download.downloadedBytes)} / " +
                        (if (download.totalBytes > 0) Formatters.formatBytes(download.totalBytes) else "?"),
                    style = MaterialTheme.typography.bodySmall
                )
                if (download.totalBytes > 0) {
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                }
            }

            when (download.status) {
                DownloadStatus.DOWNLOADING -> {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("This cycle", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${Formatters.formatBytes(download.cycleUsedBytes)} / ${Formatters.formatBytes(download.cycleLimitBytes)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(Formatters.formatSpeed(download.lastSpeedBps), style = MaterialTheme.typography.bodySmall)
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
                else -> {}
            }

            Spacer(Modifier.height(12.dp))
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