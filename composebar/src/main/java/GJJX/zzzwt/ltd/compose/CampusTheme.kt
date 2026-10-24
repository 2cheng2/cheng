package GJJX.zzzwt.ltd.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.defaultTextStyles
import top.yukonga.miuix.kmp.theme.lightColorScheme

private fun campusLightColors(): Colors = lightColorScheme(
    primary = Color(0xFF1A5FB4),
    onPrimary = Color.White,
    onSurface = Color(0xFF6B7A90),
    surface = Color(0xFFFCFCFD),
    surfaceContainer = Color(0xFFFCFCFD),
    surfaceVariant = Color(0xFFF5F7FA),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF22303F),
)

private fun campusDarkColors(): Colors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0D1B2A),
    onSurface = Color(0xFF94A3B8),
    surface = Color(0xFF1E293B),
    surfaceContainer = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF0F172A),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE2E8F0),
)

@Composable
fun CampusMiuixTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (dark) campusDarkColors() else campusLightColors()
    MiuixTheme(colors, defaultTextStyles(), false, content)
}