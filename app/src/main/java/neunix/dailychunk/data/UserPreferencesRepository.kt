package neunix.dailychunk.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "dailychunk_settings")

enum class IntervalUnit { MINUTES, HOURS }

data class AppPreferences(
    val themeMode: String = "LIGHT",
    val wifiOnly: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val defaultCycleAmountMb: Float = 100f,
    val defaultIntervalValue: Long = 24L,
    val defaultIntervalUnit: IntervalUnit = IntervalUnit.HOURS,
    val maxConcurrentDownloads: Int = 2
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val CYCLE_MB = floatPreferencesKey("default_cycle_mb")
        val INTERVAL_VALUE = longPreferencesKey("default_interval_value")
        val INTERVAL_UNIT = stringPreferencesKey("default_interval_unit")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            themeMode = prefs[Keys.THEME] ?: "LIGHT",
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            defaultCycleAmountMb = prefs[Keys.CYCLE_MB] ?: 100f,
            defaultIntervalValue = prefs[Keys.INTERVAL_VALUE] ?: 24L,
            defaultIntervalUnit = try {
                IntervalUnit.valueOf(prefs[Keys.INTERVAL_UNIT] ?: "HOURS")
            } catch (e: Exception) {
                IntervalUnit.HOURS
            },
            maxConcurrentDownloads = prefs[Keys.MAX_CONCURRENT] ?: 2
        )
    }

    suspend fun setThemeMode(mode: String) = context.dataStore.edit { it[Keys.THEME] = mode }
    suspend fun setWifiOnly(value: Boolean) = context.dataStore.edit { it[Keys.WIFI_ONLY] = value }
    suspend fun setNotificationsEnabled(value: Boolean) = context.dataStore.edit { it[Keys.NOTIFICATIONS] = value }
    suspend fun setDefaultCycleAmountMb(value: Float) = context.dataStore.edit { it[Keys.CYCLE_MB] = value }
    suspend fun setDefaultInterval(value: Long, unit: IntervalUnit) = context.dataStore.edit {
        it[Keys.INTERVAL_VALUE] = value
        it[Keys.INTERVAL_UNIT] = unit.name
    }
    suspend fun setMaxConcurrentDownloads(value: Int) = context.dataStore.edit { it[Keys.MAX_CONCURRENT] = value }
}