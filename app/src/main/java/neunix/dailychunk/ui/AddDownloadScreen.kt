package neunix.dailychunk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import neunix.dailychunk.download.AnalyzeResult
import neunix.dailychunk.util.FileUtils
import neunix.dailychunk.util.Formatters

@Composable
fun AddDownloadScreen(
    viewModel: DownloadViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var cycleLimitMb by remember { mutableStateOf("100") }
    var cycleIntervalHours by remember { mutableStateOf("24") }
    var customFileName by remember { mutableStateOf("") }

    val analyzeState by viewModel.analyzeState.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Add download") },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            }
        )
    }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; viewModel.resetAnalyze() },
                label = { Text("Download URL") },
                placeholder = { Text("https://example.com/file.zip") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { viewModel.analyze(url.trim()) },
                enabled = url.isNotBlank() && analyzeState !is AnalyzeUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (analyzeState is AnalyzeUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Analyze")
                }
            }

            when (val state = analyzeState) {
                is AnalyzeUiState.Success -> {
                    val r = state.result
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Filename: ${r.fileName}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Size: ${if (r.totalBytes > 0) Formatters.formatBytes(r.totalBytes) else "Unknown"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text("Type: ${r.contentType ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (r.supportsRange) "Resumable downloading supported"
                                else "Resume not confirmed — cycle limit may not apply if interrupted",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                is AnalyzeUiState.Error -> {
                    Text(
                        "Could not analyze URL: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {}
            }

            OutlinedTextField(
                value = customFileName,
                onValueChange = { customFileName = it },
                label = { Text("Filename (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cycleLimitMb,
                    onValueChange = { cycleLimitMb = it.filter { c -> c.isDigit() } },
                    label = { Text("MB per cycle") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cycleIntervalHours,
                    onValueChange = { cycleIntervalHours = it.filter { c -> c.isDigit() } },
                    label = { Text("Hours between cycles") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            val successState = analyzeState as? AnalyzeUiState.Success
            Button(
                onClick = {
                    val r: AnalyzeResult.Success? = successState?.result
                    val fileName = customFileName.ifBlank { r?.fileName ?: FileUtils.extractFileNameFromUrl(url) }
                    viewModel.addDownload(
                        url = url.trim(),
                        fileName = fileName,
                        totalBytes = r?.totalBytes ?: -1L,
                        supportsRange = r?.supportsRange ?: false,
                        mimeType = r?.contentType,
                        cycleLimitBytes = (cycleLimitMb.toLongOrNull() ?: 100L) * 1024L * 1024L,
                        cycleIntervalMillis = (cycleIntervalHours.toLongOrNull() ?: 24L) * 3600L * 1000L
                    )
                    onDone()
                },
                enabled = url.isNotBlank() && successState != null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Start download") }
        }
    }
}