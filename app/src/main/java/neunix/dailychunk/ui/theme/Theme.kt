package neunix.dailychunk.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { LIGHT, DARK, SYSTEM }

object ThemePreferences {
    private val _themeMode = MutableStateFlow(ThemeMode.LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun init(context: Context) {
        // Actual persisted value is applied from DataStore prefs once loaded (see DailyChunkApp / MainActivity flow).
    }

    fun setFromString(value: String) {
        _themeMode.value = try { ThemeMode.valueOf(value) } catch (e: Exception) { ThemeMode.LIGHT }
    }

    fun localOverride(mode: ThemeMode) {
        _themeMode.value = mode
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF4A5CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E9FF),
    onPrimaryContainer = Color(0xFF1B1F6B),
    secondary = Color(0xFF00BF8F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8FBF0),
    tertiary = Color(0xFFFF9F43),
    background = Color(0xFFF9F9FC),
    onBackground = Color(0xFF1A1B22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1B22),
    surfaceVariant = Color(0xFFF0F1F7),
    onSurfaceVariant = Color(0xFF63667A),
    error = Color(0xFFE0453C),
    outline = Color(0xFFE1E2ED)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8C97FF),
    onPrimary = Color(0xFF0F1247),
    primaryContainer = Color(0xFF2B2F80),
    onPrimaryContainer = Color(0xFFE7E9FF),
    secondary = Color(0xFF3CE0B2),
    onSecondary = Color(0xFF00382A),
    secondaryContainer = Color(0xFF00513C),
    tertiary = Color(0xFFFFB86B),
    background = Color(0xFF121319),
    onBackground = Color(0xFFEAEAF0),
    surface = Color(0xFF1B1D25),
    onSurface = Color(0xFFEAEAF0),
    surfaceVariant = Color(0xFF262833),
    onSurfaceVariant = Color(0xFFA6A8BC),
    error = Color(0xFFFF6B60),
    outline = Color(0xFF33354200 or 0xFF333542)
)

val DailyChunkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val DailyChunkTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.3.sp)
)

@Composable
fun DailyChunkTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = DailyChunkTypography, shapes = DailyChunkShapes, content = content)
}