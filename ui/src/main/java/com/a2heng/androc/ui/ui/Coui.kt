package com.a2heng.androc.ui.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * COUI 基础组件：全部自绘（Box/Canvas/Popup/BasicText），不使用 Material 组件的
 * 外观。颜色和字阶暂时从 MaterialTheme 读取，后续可切到 CouiTheme。
 */
object Coui {
    val fieldHeight = 48.dp
    val radiusField = 14.dp
    val radiusCard = 18.dp
    val radiusDialog = 24.dp
    val gap = 8.dp
    val pad = 16.dp
    val screenPad = 20.dp

    @Composable
    fun label() = CouiTheme.colors.onSurfaceVariant
    @Composable
    fun surface() = CouiTheme.colors.surface
    @Composable
    fun line() = CouiTheme.colors.outline
    @Composable
    fun accent() = CouiTheme.colors.accent
    @Composable
    fun onAccent() = CouiTheme.colors.onAccent
}

/* ------------------------------------------------------------------- icons */

enum class CouiIcon { ArrowDown, Check, Close, Back, Terminal, Lock, Delete, CheckCircle, More, Server, ChevronRight }

@Composable
fun CouiGlyph(icon: CouiIcon, tint: Color = Coui.label(), size: Int = 18) {
    Canvas(Modifier.size(size.dp)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = w * 0.12f, cap = StrokeCap.Round)
        when (icon) {
            CouiIcon.ArrowDown -> {
                drawLine(tint, Offset(w * 0.22f, h * 0.38f), Offset(w * 0.5f, h * 0.66f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.66f), Offset(w * 0.78f, h * 0.38f), stroke.width, StrokeCap.Round)
            }
            CouiIcon.Check -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.52f), Offset(w * 0.42f, h * 0.74f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.74f), Offset(w * 0.82f, h * 0.28f), stroke.width, StrokeCap.Round)
            }
            CouiIcon.Close -> {
                drawLine(tint, Offset(w * 0.26f, h * 0.26f), Offset(w * 0.74f, h * 0.74f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.74f, h * 0.26f), Offset(w * 0.26f, h * 0.74f), stroke.width, StrokeCap.Round)
            }
            CouiIcon.Back -> {
                drawLine(tint, Offset(w * 0.62f, h * 0.24f), Offset(w * 0.34f, h * 0.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.34f, h * 0.5f), Offset(w * 0.62f, h * 0.76f), stroke.width, StrokeCap.Round)
            }
            CouiIcon.Terminal -> {
                drawRoundRect(
                    tint, topLeft = Offset(w * 0.16f, h * 0.2f),
                    size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.6f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
                    style = Stroke(width = stroke.width),
                )
                drawLine(tint, Offset(w * 0.3f, h * 0.42f), Offset(w * 0.42f, h * 0.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.5f), Offset(w * 0.3f, h * 0.58f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.58f), Offset(w * 0.68f, h * 0.58f), stroke.width, StrokeCap.Round)
            }
            CouiIcon.Lock -> {
                drawRoundRect(
                    tint, topLeft = Offset(w * 0.24f, h * 0.44f),
                    size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.38f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f),
                    style = Stroke(width = stroke.width),
                )
                drawArc(
                    tint, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(w * 0.34f, h * 0.2f),
                    size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.4f),
                    style = Stroke(width = stroke.width, cap = StrokeCap.Round),
                )
            }
            CouiIcon.Delete -> {
                drawLine(tint, Offset(w * 0.26f, h * 0.3f), Offset(w * 0.74f, h * 0.3f), stroke.width, StrokeCap.Round)
                drawRoundRect(
                    tint, topLeft = Offset(w * 0.32f, h * 0.36f),
                    size = androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.46f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.08f),
                    style = Stroke(width = stroke.width),
                )
                drawLine(tint, Offset(w * 0.42f, h * 0.22f), Offset(w * 0.58f, h * 0.22f), stroke.width, StrokeCap.Round)
            }
            CouiIcon.CheckCircle -> {
                drawCircle(tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = stroke.width))
                drawLine(tint, Offset(w * 0.34f, h * 0.5f), Offset(w * 0.46f, h * 0.64f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.46f, h * 0.64f), Offset(w * 0.68f, h * 0.38f), stroke.width, StrokeCap.Round)
            }
            CouiIcon.ChevronRight -> {
                drawLine(tint, Offset(w * 0.4f, h * 0.26f), Offset(w * 0.64f, h * 0.5f), stroke.width, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.64f, h * 0.5f), Offset(w * 0.4f, h * 0.74f), stroke.width, StrokeCap.Round)
            }
            CouiIcon.Server -> {
                val barW = w * 0.56f
                val barH = h * 0.1f
                for (i in 0..2) {
                    drawRoundRect(
                        tint,
                        topLeft = Offset((w - barW) / 2f, h * (0.26f + i * 0.22f)),
                        size = androidx.compose.ui.geometry.Size(barW, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barH / 2f),
                    )
                }
            }
            CouiIcon.More -> {
                drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.26f))
                drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.74f))
            }
        }
    }
}

/* -------------------------------------------------------------------- text */

@Composable
fun CouiText(
    text: String,
    style: TextStyle = CouiTheme.type.body,
    color: Color = CouiTheme.colors.onSurface,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    BasicText(
        text = text,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/* ------------------------------------------------------------------ select */

@Composable
fun CouiSelect(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    label: String? = null,
    modifier: Modifier = Modifier,
    optionLabel: (String) -> String = { it },
) {
    var open by remember { mutableStateOf(false) }
    val heightPx = with(LocalDensity.current) { Coui.fieldHeight.roundToPx() }
    Column(modifier) {
        if (label != null) {
            CouiText(
                label,
                style = CouiTheme.type.labelSmall,
                color = Coui.label(),
            )
            Spacer(Modifier.height(6.dp))
        }
        val shape = RoundedCornerShape(Coui.radiusField)
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(Coui.fieldHeight)
                    .background(Coui.surface(), shape)
                    .border(1.dp, Coui.line(), shape)
                    .clickable { open = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CouiText(
                    optionLabel(value),
                    style = CouiTheme.type.body,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                CouiGlyph(CouiIcon.ArrowDown)
            }
            if (open) {
                Popup(
                    onDismissRequest = { open = false },
                    properties = PopupProperties(focusable = true),
                    offset = IntOffset(0, heightPx + 6),
                ) {
                    CouiCard(Modifier.width(200.dp), onClick = null) {
                        options.forEachIndexed { index, item ->
                            if (index > 0) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                        .height(1.dp)
                                        .background(CouiTheme.colors.outlineVariant),
                                )
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .clickable {
                                        onSelect(item)
                                        open = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CouiText(
                                    optionLabel(item),
                                    style = CouiTheme.type.body,
                                    modifier = Modifier.weight(1f),
                                )
                                if (item == value) CouiGlyph(CouiIcon.Check, tint = Coui.accent())
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ button */

@Composable
fun CouiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(Coui.radiusField)
    Row(
        modifier
            .fillMaxWidth()
            .height(Coui.fieldHeight)
            .clip(shape)
            .background(if (enabled) Coui.accent() else CouiTheme.colors.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(8.dp))
        }
        CouiText(
            text,
            style = CouiTheme.type.label,
            color = if (enabled) Coui.onAccent() else Coui.label(),
        )
    }
}

/* -------------------------------------------------------------------- card */

@Composable
fun CouiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Coui.radiusCard)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(Coui.surface())
            .border(1.dp, CouiTheme.colors.outlineVariant, shape)
            .padding(vertical = 4.dp),
        content = content,
    )
}

@Composable
fun CouiSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        CouiText(
            title,
            style = CouiTheme.type.label,
            color = Coui.label(),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
        CouiCard(content = content)
    }
}

/* -------------------------------------------------------------------- item */

@Composable
fun CouiListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            CouiText(title)
            if (subtitle != null) {
                CouiText(
                    subtitle,
                    style = CouiTheme.type.bodySmall,
                    color = Coui.label(),
                    maxLines = 2,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/* ------------------------------------------------------------------ switch */

@Composable
fun CouiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackW = 46.dp
    val trackH = 28.dp
    val thumb = 22.dp
    val padding = 3.dp
    Box(
        modifier
            .size(trackW, trackH)
            .clip(CircleShape)
            .background(if (checked) Coui.accent() else CouiTheme.colors.surfaceVariant)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(horizontal = padding)
                .size(thumb)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
fun CouiSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    CouiListItem(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        trailing = { CouiSwitch(checked, onCheckedChange) },
    )
}

/* ------------------------------------------------------------ text button */

@Composable
fun CouiTextButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Coui.accent(),
    filled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .clip(shape)
            .then(
                if (filled) Modifier
                    .background(CouiTheme.colors.surfaceVariant)
                    .border(1.dp, CouiTheme.colors.outlineVariant, shape)
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        CouiText(text, style = CouiTheme.type.label, color = color)
    }
}

@Composable
fun CouiIconButton(
    icon: CouiIcon,
    modifier: Modifier = Modifier,
    tint: Color = Coui.label(),
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier
            .size(36.dp)
            .clip(shape)
            .background(CouiTheme.colors.surfaceVariant)
            .border(1.dp, CouiTheme.colors.outlineVariant, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CouiGlyph(icon, tint = tint, size = 18)
    }
}

/* ----------------------------------------------------------------- spinner */

@Composable
fun CouiSpinner(size: Int = 16, color: Color = Coui.accent()) {
    Canvas(Modifier.size(size.dp)) {
        val stroke = this.size.width * 0.12f
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 280f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/* ------------------------------------------------------------------ dialog */

@Composable
fun CouiDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            CouiCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    CouiText(title, style = CouiTheme.type.title)
                    Spacer(Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

/* -------------------------------------------------------------------- chip */

@Composable
fun CouiChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .clip(shape)
            .background(
                if (selected) CouiTheme.colors.accent else CouiTheme.colors.surfaceVariant,
            )
            .border(
                1.dp,
                if (selected) CouiTheme.colors.accent else CouiTheme.colors.outlineVariant,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        CouiText(
            text,
            style = CouiTheme.type.label,
            color = if (selected) CouiTheme.colors.onAccent else CouiTheme.colors.onSurface,
        )
    }
}
