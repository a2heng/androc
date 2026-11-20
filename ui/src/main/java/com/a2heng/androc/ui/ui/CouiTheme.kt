package com.a2heng.androc.ui.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** COUI 自有色板：不依赖 Material3。 */
data class CouiColors(
    val dark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val accent: Color,
    val onAccent: Color,
    val error: Color,
)

val CouiLight = CouiColors(
    dark = false,
    background = Color(0xFFF5F5F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFEFF2),
    onSurface = Color(0xFF141416),
    onSurfaceVariant = Color(0xFF6B6B73),
    outline = Color(0xFFDCDCE0),
    outlineVariant = Color(0x14000000),
    accent = Color(0xFF16181C),
    onAccent = Color(0xFFFFFFFF),
    error = Color(0xFFC0323D),
)

val CouiDark = CouiColors(
    dark = true,
    background = Color(0xFF0A0A0B),
    surface = Color(0xFF141416),
    surfaceVariant = Color(0xFF1E1E21),
    onSurface = Color(0xFFEDEDEF),
    onSurfaceVariant = Color(0xFFA0A0A8),
    outline = Color(0xFF2C2C31),
    outlineVariant = Color(0x1FFFFFFF),
    accent = Color(0xFFEDEDEF),
    onAccent = Color(0xFF0B0D10),
    error = Color(0xFFF07178),
)

data class CouiType(
    val title: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val labelSmall: TextStyle,
)

val CouiTypes = CouiType(
    title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    headline = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
    label = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp),
    labelSmall = TextStyle(fontSize = 11.5.sp, lineHeight = 15.sp),
)

val LocalCouiColors = staticCompositionLocalOf { CouiLight }
val LocalCouiType = staticCompositionLocalOf { CouiTypes }

@Composable
fun CouiTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCouiColors provides if (dark) CouiDark else CouiLight,
        LocalCouiType provides CouiTypes,
        content = content,
    )
}

object CouiTheme {
    val colors: CouiColors
        @Composable get() = LocalCouiColors.current
    val type: CouiType
        @Composable get() = LocalCouiType.current
}
