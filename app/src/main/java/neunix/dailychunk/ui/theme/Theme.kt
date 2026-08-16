package neunix.dailychunk.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { LIGHT, DARK, SYSTEM }

object ThemePreferences {
    private const val PREFS = "dailychunk_prefs"
    private const val KEY_THEME = "theme_mode"

    private val _themeMode = MutableStateFlow(ThemeMode.LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_THEME, ThemeMode.LIGHT.name)
        _themeMode.value = try {
            ThemeMode.valueOf(saved ?: "LIGHT")
        } catch (e: Exception) {
            ThemeMode.LIGHT
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        _themeMode.value = mode
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, mode.name).apply()
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D6BFF),
    onPrimary = Color.White,
    secondary = Color(0xFF00B894),
    background = Color(0xFFFBFBFE),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F3F9)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7B96FF),
    onPrimary = Color.Black,
    secondary = Color(0xFF32D9A0),
    background = Color(0xFF14161B),
    surface = Color(0xFF1C1F26),
    surfaceVariant = Color(0xFF262A33)
)

@Composable
fun DailyChunkTheme(content: @Composable () -> Unit) {
    val mode by ThemePreferences.themeMode.collectAsState()
    val darkTheme = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}