package com.a2heng.androc.ui.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ---------------------------------------------------------------------------
// 默认是黑白灰（不抢内容），主题色可在设置里换成 Material You 或预设色。
// 注意：命中率/状态的颜色是语义色（绿=好、红=差），不随主题变。
// ---------------------------------------------------------------------------

val Accent = Color(0xFF8FA3B8)
val Sky = Color(0xFF7DCFFF)
val Warn = Color(0xFFE0AF68)
val Danger = Color(0xFFF07178)

/**
 * 全局只有两档强调色，所有页面（顶栏、底栏、设置、卡片）都用它们，不再各写 alpha：
 *  - [Deep]  深主题色：实心圆按钮、发送键、选中指示
 *  - [Medium] 中强调色：卡片 / 胶囊 / 代码块的浅底（主色与背景按 85% 混合）
 *  - [MediumStrong] 中强调色的深一档：选中项、表头
 */
object AndrocColor {
    val Deep: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val Medium: Color
        @Composable get() = lerp(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.background,
            0.85f,
        )

    val MediumStrong: Color
        @Composable get() = lerp(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.background,
            0.74f,
        )

    /** Medium 底上的正文色，保证对比度。 */
    val OnMedium: Color
        @Composable get() = lerp(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onSurface,
            0.12f,
        )
}

private val MonoDark = darkColorScheme(
    primary = Color(0xFFE8EBEF),
    onPrimary = Color(0xFF0B0D10),
    primaryContainer = Color(0xFF2B323B),
    onPrimaryContainer = Color(0xFFE6E8EC),
    secondary = Color(0xFFB6BCC6),
    onSecondary = Color(0xFF14181D),
    secondaryContainer = Color(0xFF27303A),
    onSecondaryContainer = Color(0xFFDCE0E6),
    tertiary = Color(0xFF9DA5B1),
    onTertiary = Color(0xFF14181D),
    background = Color(0xFF0A0A0B),
    onBackground = Color(0xFFEDEDEF),
    surface = Color(0xFF141416),
    onSurface = Color(0xFFEDEDEF),
    surfaceVariant = Color(0xFF1E1E21),
    onSurfaceVariant = Color(0xFFA0A0A8),
    outline = Color(0xFF2C2C31),
    outlineVariant = Color(0x1FFFFFFF),
    error = Danger,
)

private val MonoLight = lightColorScheme(
    primary = Color(0xFF23272E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3EC),
    onPrimaryContainer = Color(0xFF1A1D21),
    secondary = Color(0xFF5B6068),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE2EA),
    onSecondaryContainer = Color(0xFF23272E),
    tertiary = Color(0xFF7A8089),
    onTertiary = Color.White,
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF141416),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141416),
    surfaceVariant = Color(0xFFEFEFF2),
    onSurfaceVariant = Color(0xFF6B6B73),
    outline = Color(0xFFDCDCE0),
    outlineVariant = Color(0x14000000),
    error = Color(0xFFC0323D),
)

private val DarkAccents = mapOf(
    "blue" to Color(0xFF7AA2F7),
    "purple" to Color(0xFFBB9AF7),
    "orange" to Color(0xFFE0AF68),
    "cyan" to Color(0xFF7DCFFF),
    "green" to Color(0xFF9ECE6A),
    "pink" to Color(0xFFF7768E),
    "red" to Color(0xFFF07178),
    "teal" to Color(0xFF73DACA),
)
private val LightAccents = mapOf(
    "blue" to Color(0xFF2E5AAC),
    "purple" to Color(0xFF6E4BB8),
    "orange" to Color(0xFFA15C00),
    "cyan" to Color(0xFF1B6E84),
    "green" to Color(0xFF3A7D2C),
    "pink" to Color(0xFFB03060),
    "red" to Color(0xFFB3363F),
    "teal" to Color(0xFF0F6F6B),
)

fun themePresetKeys(): List<String> =
    listOf("mono", "dynamic", "blue", "purple", "cyan", "green", "orange", "pink", "red", "teal")

fun themePresetLabel(key: String): String = when (key) {
    "mono" -> "黑白灰（默认）"
    "dynamic" -> "跟随系统取色（Android 12+）"
    "blue" -> "蓝色"
    "purple" -> "紫色"
    "orange" -> "橙色"
    "cyan" -> "青色"
    "green" -> "绿色"
    "pink" -> "粉色"
    "red" -> "红色"
    "teal" -> "青绿"
    else -> key
}

/** 设置页色卡用：某个主题色在当前深浅模式下的实际 accent。 */
fun themeAccentColor(key: String, dark: Boolean): Color = when (key) {
    "mono" -> if (dark) Color(0xFFE8EBEF) else Color(0xFF23272E)
    "dynamic" -> if (dark) Color(0xFF8AB4F8) else Color(0xFF1A73E8)
    else -> (if (dark) DarkAccents else LightAccents)[key] ?: (if (dark) Color(0xFF8FA3B8) else Color(0xFF5B6068))
}

private fun darkPreset(accent: Color): ColorScheme = MonoDark.copy(
    primary = accent,
    onPrimary = Color(0xFF0B0D10),
    primaryContainer = lerp(Color(0xFF14181D), accent, 0.24f),
    onPrimaryContainer = lerp(accent, Color.White, 0.55f),
    secondary = lerp(accent, Color.White, 0.35f),
)

private fun lightPreset(accent: Color): ColorScheme = MonoLight.copy(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = lerp(Color.White, accent, 0.16f),
    onPrimaryContainer = lerp(accent, Color.Black, 0.35f),
    secondary = lerp(accent, Color.Black, 0.25f),
)

fun rateColor(rate: Double, dark: Boolean): Color = when {
    rate >= 95.0 -> if (dark) Color(0xFF4ADE80) else Color(0xFF17864A)
    rate >= 90.0 -> if (dark) Color(0xFFA3E635) else Color(0xFF4D7C0F)
    rate >= 80.0 -> if (dark) Color(0xFFFACC15) else Color(0xFF9A6700)
    rate >= 70.0 -> if (dark) Color(0xFFFB923C) else Color(0xFFB45309)
    rate >= 50.0 -> if (dark) Color(0xFFF87171) else Color(0xFFC0323D)
    else -> if (dark) Color(0xFFEF4444) else Color(0xFF991B1B)
}

/** 上下文占用：越低越好，绿 → 黄 → 红，多档渐变。 */
fun contextUsageColor(pct: Double, dark: Boolean): Color = when {
    pct < 25 -> if (dark) Color(0xFF4ADE80) else Color(0xFF17864A)
    pct < 40 -> if (dark) Color(0xFFA3E635) else Color(0xFF4D7C0F)
    pct < 55 -> if (dark) Color(0xFFFACC15) else Color(0xFF9A6700)
    pct < 68 -> if (dark) Color(0xFFFBBF24) else Color(0xFFB45309)
    pct < 80 -> if (dark) Color(0xFFFB923C) else Color(0xFFC2410C)
    pct < 90 -> if (dark) Color(0xFFF87171) else Color(0xFFC0323D)
    else -> if (dark) Color(0xFFEF4444) else Color(0xFF991B1B)
}

@Composable
fun OcTheme(
    themeMode: String = "mono",
    appearance: String = "system",
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (appearance) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val context = LocalContext.current
    // 主题色只随模式/预设变化重算，避免每次重组都新建 ColorScheme。
    val colors: ColorScheme = androidx.compose.runtime.remember(dark, themeMode) {
        when {
            themeMode == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            dark -> DarkAccents[themeMode]?.let(::darkPreset) ?: MonoDark
            else -> LightAccents[themeMode]?.let(::lightPreset) ?: MonoLight
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CouiTheme(dark = dark) {
        MaterialTheme(
            colorScheme = colors,
            typography = CouiTypography,
            shapes = CouiShapes,
            content = content,
        )
    }
}

/** ColorOS 风格：更紧的字距、更克制的字重层级。 */
private val CouiTypography = Typography(
    headlineSmall = Typography().headlineSmall.copy(fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    titleLarge = Typography().titleLarge.copy(fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    titleSmall = Typography().titleSmall.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 15.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 14.sp),
    bodySmall = Typography().bodySmall.copy(fontSize = 12.5.sp),
    labelLarge = Typography().labelLarge.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    labelMedium = Typography().labelMedium.copy(fontSize = 12.5.sp),
    labelSmall = Typography().labelSmall.copy(fontSize = 11.5.sp),
)

/** ColorOS 的圆角比 Material 默认更收敛。 */
private val CouiShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)
