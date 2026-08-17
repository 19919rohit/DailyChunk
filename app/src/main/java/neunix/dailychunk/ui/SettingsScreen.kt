package neunix.dailychunk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import neunix.dailychunk.ui.theme.ThemeMode
import neunix.dailychunk.util.BatteryOptimizationHelper

@Composable
fun SettingsScreen(viewModel: DownloadViewModel) {
    val prefs by viewModel.preferences.collectAsState()
    val context = LocalContext.current

    var showCustomDialog by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var batteryExempt by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }

    val presetOptions = listOf(1, 2, 3, 4)
    val isCustomActive = prefs.maxConcurrentDownloads !in presetOptions

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection(title = "Appearance") {
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    listOf(
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.DARK to "Dark",
                        ThemeMode.SYSTEM to "System default"
                    ).forEach { (mode, label) ->
                        val selected = prefs.themeMode == mode.name
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = selected, onClick = { viewModel.setThemeMode(mode.name) })
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = { viewModel.setThemeMode(mode.name) })
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            SettingsSection(title = "Reliability") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (batteryExempt) "Background downloads are unrestricted on this device."
                        else "Some devices pause background downloads to save battery. Exempting DailyChunk keeps cycles running on schedule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!batteryExempt) {
                        Button(onClick = {
                            BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                            batteryExempt = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                        }) {
                            Text("Allow background activity")
                        }
                    }
                }
            }

            SettingsSection(title = "Network") {
                SettingsSwitchRow(
                    title = "Wi-Fi only",
                    subtitle = "Pause downloads when off Wi-Fi",
                    checked = prefs.wifiOnly,
                    onCheckedChange = { viewModel.setWifiOnly(it) }
                )
            }

            SettingsSection(title = "Notifications") {
                SettingsSwitchRow(
                    title = "Enable notifications",
                    subtitle = "Progress, completion, and error alerts",
                    checked = prefs.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
            }

            SettingsSection(title = "Downloads") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Max simultaneous downloads: ${prefs.maxConcurrentDownloads}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presetOptions.forEach { n ->
                            val selected = prefs.maxConcurrentDownloads == n
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setMaxConcurrentDownloads(n) },
                                label = { Text(n.toString()) }
                            )
                        }
                        FilterChip(
                            selected = isCustomActive,
                            onClick = {
                                customText = if (isCustomActive) prefs.maxConcurrentDownloads.toString() else ""
                                showCustomDialog = true
                            },
                            label = { Text(if (isCustomActive) "Custom (${prefs.maxConcurrentDownloads})" else "Custom") }
                        )
                    }
                }
            }

            Text(
                "DailyChunk v1.0 • Files are saved to Download / Daily Chunk",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom limit") },
            text = {
                Column {
                    Text(
                        "Choose how many downloads can run at the same time (1–10).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { input -> customText = input.filter { it.isDigit() }.take(2) },
                        label = { Text("Number of downloads") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = customText.toIntOrNull()?.coerceIn(1, 10)
                    if (value != null) viewModel.setMaxConcurrentDownloads(value)
                    showCustomDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun Spacer(modifier: Modifier) = androidx.compose.foundation.layout.Spacer(modifier)