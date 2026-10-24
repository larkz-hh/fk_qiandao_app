package io.github.yesterdaylz.kxqiandao.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 应用配色
 *
 * @param heatmap 热力图 5 个色阶（0 = 没签到）
 */
data class KxColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val muted: Color,
    val divider: Color,
    val primary: Color,
    val onPrimary: Color,
    val success: Color,
    val onSuccess: Color,
    val mutedContainer: Color,
    val onMuted: Color,
    val danger: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val heatmap: List<Color>,
)

val LightKxColors = KxColors(
    background = Color(0xFFF9F9F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurface = Color(0xFF111111),
    muted = Color(0xFF757575),
    divider = Color(0xFFD0D7DE),
    primary = Color(0xFF085594),
    onPrimary = Color(0xFFFFFFFF),
    success = Color(0xFF82B646),
    onSuccess = Color(0xFF111111),
    mutedContainer = Color(0xFF757575),
    onMuted = Color(0xFFFFFFFF),
    danger = Color(0xFFE53935),
    warningContainer = Color(0xFFFFF4CE),
    onWarningContainer = Color(0xFF8A6D3B),
    heatmap = listOf(
        Color(0xFFEBEDF0),
        Color(0xFF9BE9A8),
        Color(0xFF40C463),
        Color(0xFF30A14E),
        Color(0xFF216E39),
    ),
)

val DarkKxColors = KxColors(
    background = Color(0xFF121212),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurface = Color(0xFFEDEDED),
    muted = Color(0xFF9A9A9A),
    divider = Color(0xFF3A3A3C),
    primary = Color(0xFF6FB3E8),
    onPrimary = Color(0xFF0A2A44),
    success = Color(0xFF8FCB5B),
    onSuccess = Color(0xFF0B1A00),
    mutedContainer = Color(0xFF3A3A3C),
    onMuted = Color(0xFFE6E6E6),
    danger = Color(0xFFFF6B6B),
    warningContainer = Color(0xFF3A2E12),
    onWarningContainer = Color(0xFFE8C56B),
    heatmap = listOf(
        Color(0xFF2A2A2C),
        Color(0xFF0E4429),
        Color(0xFF006D32),
        Color(0xFF26A641),
        Color(0xFF39D353),
    ),
)

val LocalKxColors = staticCompositionLocalOf { LightKxColors }

object KxTheme {
    val colors: KxColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKxColors.current
}
