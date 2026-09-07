package neunix.dailychunk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import neunix.dailychunk.data.DownloadStatus
import neunix.dailychunk.data.IntervalUnit
import neunix.dailychunk.download.AnalyzeResult
import neunix.dailychunk.util.FileUtils
import neunix.dailychunk.util.Formatters

@Composable
fun AddDownloadScreen(
    viewModel: DownloadViewModel,
    initialUrl: String? = null,
    onUrlConsumed: () -> Unit = {},
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val prefs by viewModel.preferences.collectAsState()
    val existingDownloads by viewModel.downloads.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var url by remember { mutableStateOf("") }
    var cycleAmountText by remember { mutableStateOf("") }
    var intervalText by remember { mutableStateOf("") }
    var intervalUnit by remember { mutableStateOf(IntervalUnit.HOURS) }
    var customFileName by remember { mutableStateOf("") }
    var initializedDefaults by remember { mutableStateOf(false) }
    var clipboardSuggestion by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(prefs) {
        if (!initializedDefaults) {
            cycleAmountText = formatMb(prefs.defaultCycleAmountMb)
            intervalText = prefs.defaultIntervalValue.toString()
            intervalUnit = prefs.defaultIntervalUnit
            initializedDefaults = true
        }
    }

    // Links shared into DailyChunk from a browser or another app — the
    // practical way to add downloads from pages that only resolve their real
    // file URL after JavaScript runs, since a real browser already did that
    // work before the link was shared here.
    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            url = initialUrl
            onUrlConsumed()
            viewModel.analyze(initialUrl.trim())
        }
    }

    LaunchedEffect(Unit) {
        if (initialUrl.isNullOrBlank()) {
            val clipText = clipboardManager.getText()?.text?.trim()
            if (!clipText.isNullOrBlank() && Regex("^https?://\\S+$").matches(clipText)) {
                clipboardSuggestion = clipText
            }
        }
    }

    val analyzeState by viewModel.analyzeState.collectAsState()

    val duplicateExists = url.isNotBlank() && existingDownloads.any {
        it.url == url.trim() && it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.CANCELLED
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Add download") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; clipboardSuggestion = null; viewModel.resetAnalyze() },
                label = { Text("Download URL") },
                placeholder = { Text("https://example.com/file.zip") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            if (clipboardSuggestion != null) {
                AssistChip(
                    onClick = { url = clipboardSuggestion ?: ""; clipboardSuggestion = null },
                    label = { Text("Use link from clipboard", maxLines = 1) }
                )
            }

            if (duplicateExists) {
                Text(
                    "You already have an active download for this URL.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Button(
                onClick = { viewModel.analyze(url.trim()) },
                enabled = url.isNotBlank() && analyzeState !is AnalyzeUiState.Loading,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (analyzeState is AnalyzeUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Analyze")
                }
            }

            when (val state = analyzeState) {
                is AnalyzeUiState.Success -> {
                    val r = state.result
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Filename", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(r.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (r.totalBytes > 0) Formatters.formatBytes(r.totalBytes) else "Unknown", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (r.supportsRange) "✓ Resumable downloading supported" else "⚠ Resume not confirmed by server",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (r.supportsRange) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
                is AnalyzeUiState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Could not analyze URL: ${state.message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "If this link needs a browser to load (a \"click to continue\" or countdown page), open it in your browser first, then share the final link to DailyChunk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {}
            }

            OutlinedTextField(
                value = customFileName,
                onValueChange = { customFileName = it },
                label = { Text("Filename (optional)") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Cycle settings", style = MaterialTheme.typography.titleSmall)

            OutlinedTextField(
                value = cycleAmountText,
                onValueChange = { input -> cycleAmountText = input.filter { it.isDigit() || it == '.' } },
                label = { Text("MB per cycle") },
                placeholder = { Text("e.g. 3.5") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = intervalText,
                onValueChange = { input -> intervalText = input.filter { it.isDigit() } },
                label = { Text(if (intervalUnit == IntervalUnit.MINUTES) "Minutes between cycles" else "Hours between cycles") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IntervalUnitOption("Minutes", intervalUnit == IntervalUnit.MINUTES, { intervalUnit = IntervalUnit.MINUTES }, Modifier.weight(1f))
                IntervalUnitOption("Hours", intervalUnit == IntervalUnit.HOURS, { intervalUnit = IntervalUnit.HOURS }, Modifier.weight(1f))
            }

            val successState = analyzeState as? AnalyzeUiState.Success
            val cycleAmount = cycleAmountText.toFloatOrNull()
            val intervalValue = intervalText.toLongOrNull()
            val formValid = url.isNotBlank() && successState != null &&
                cycleAmount != null && cycleAmount > 0f && intervalValue != null && intervalValue > 0L

            fun startDownload(startImmediately: Boolean) {
                val r: AnalyzeResult.Success? = successState?.result
                val fileName = customFileName.ifBlank { r?.fileName ?: FileUtils.extractFileNameFromUrl(url) }
                viewModel.addDownload(
                    url = url.trim(), fileName = fileName, totalBytes = r?.totalBytes ?: -1L,
                    supportsRange = r?.supportsRange ?: false, mimeType = r?.contentType,
                    cycleAmountMb = cycleAmount ?: 100f, intervalValue = intervalValue ?: 24L,
                    intervalUnit = intervalUnit, startImmediately = startImmediately
                )
                onDone()
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { startDownload(false) }, enabled = formValid, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f)) {
                    Text("Start later", maxLines = 1)
                }
                Button(onClick = { startDownload(true) }, enabled = formValid, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f)) {
                    Text("Start now", maxLines = 1)
                }
            }

            if (formValid) {
                val unitLabel = if (intervalUnit == IntervalUnit.MINUTES) "minute" else "hour"
                val plural = if (intervalValue == 1L) "" else "s"
                Text(
                    "\"Start later\" begins the first cycle in $intervalValue $unitLabel$plural instead of right now",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "Saved to Download / Daily Chunk",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun IntervalUnitOption(label: String, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.selectable(selected = selected, onClick = onSelect).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}

private fun formatMb(value: Float): String =
    if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()