package neunix.dailychunk.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import neunix.dailychunk.notification.Notifications
import neunix.dailychunk.ui.theme.ThemeMode
import neunix.dailychunk.util.AppLinks
import neunix.dailychunk.util.BatteryOptimizationHelper

@Composable
fun SettingsScreen(viewModel: DownloadViewModel) {
    val prefs by viewModel.preferences.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showCustomDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    var batteryExempt by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }
    var notificationsAllowed by remember { mutableStateOf(Notifications.notificationsEnabled(context)) }

    // Re-check whenever the user comes back from system Settings — there's
    // no callback for those external screens otherwise.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryExempt = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                notificationsAllowed = Notifications.notificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val presetOptions = listOf(1, 2, 3, 4)
    val isCustomActive = prefs.maxConcurrentDownloads !in presetOptions

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            actions = {
                IconButton(onClick = { showAboutDialog = true }) {
                    Icon(Icons.Outlined.Info, contentDescription = "About DailyChunk")
                }
            }
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).padding(20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (!notificationsAllowed) {
                SettingsSection(title = "Notifications blocked") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Notifications are turned off at the system level, so DailyChunk can't alert you when a download finishes, waits, or fails.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            } else {
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
                            }
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        }) { Text("Open notification settings") }
                    }
                }
            }

            SettingsSection(title = "Appearance") {
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    listOf(
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.DARK to "Dark",
                        ThemeMode.SYSTEM to "System default"
                    ).forEach { (mode, label) ->
                        val selected = prefs.themeMode == mode.name
                        Row(
                            Modifier.fillMaxWidth().selectable(selected = selected, onClick = { viewModel.setThemeMode(mode.name) }).padding(vertical = 6.dp),
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
                        Button(onClick = { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context) }) {
                            Text("Allow background activity")
                        }
                    }
                }
            }

            SettingsSection(title = "Network") {
                SettingsSwitchRow("Wi-Fi only", "Wait for Wi-Fi before every cycle, including resumes", prefs.wifiOnly) { viewModel.setWifiOnly(it) }
            }

            SettingsSection(title = "Notifications") {
                SettingsSwitchRow("Enable notifications", "Progress, completion, and error alerts", prefs.notificationsEnabled) { viewModel.setNotificationsEnabled(it) }
            }

            SettingsSection(title = "Downloads") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Max simultaneous downloads: ${prefs.maxConcurrentDownloads}", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        presetOptions.forEach { n ->
                            val selected = prefs.maxConcurrentDownloads == n
                            FilterChip(selected = selected, onClick = { viewModel.setMaxConcurrentDownloads(n) }, label = { Text(n.toString(), maxLines = 1) })
                        }
                        FilterChip(
                            selected = isCustomActive,
                            onClick = { customText = if (isCustomActive) prefs.maxConcurrentDownloads.toString() else ""; showCustomDialog = true },
                            label = { Text(if (isCustomActive) "Custom (${prefs.maxConcurrentDownloads})" else "Custom", maxLines = 1) }
                        )
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom limit") },
            text = {
                Column {
                    Text(
                        "Choose how many downloads can run at the same time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { input -> customText = input.filter { it.isDigit() }.take(5) },
                        label = { Text("Number of downloads") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = customText.toIntOrNull()
                    if (value != null && value > 0) viewModel.setMaxConcurrentDownloads(value)
                    showCustomDialog = false
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About DailyChunk") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "DailyChunk is free and open source software. There are no trackers, no analytics, and no proprietary dependencies anywhere in this app.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Anyone can read the source, verify how it works, or build it themselves.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(AppLinks.REPO_URL, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = { AppLinks.openRepo(context); showAboutDialog = false }) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View source code")
                }
            },
            dismissButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}