package com.a2heng.androc.ui.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.a2heng.androc.ui.data.AppModel
import com.a2heng.androc.ui.data.GoActivity
import com.a2heng.androc.ui.data.GoLimits
import com.a2heng.androc.ui.data.GoMeter
import com.a2heng.androc.ui.data.formatCost
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * OpenCode Go 额度：三个窗口（5 小时 / 本周 / 本月）+ 本月每日累计曲线和月底预测。
 * 只在当前模型来自 opencode-go 时显示（由调用方判断）。
 */
@Composable
fun GoUsageCard(
    model: AppModel,
    modifier: Modifier = Modifier,
    loading: Boolean = model.goUsageLoading,
    error: String? = model.goUsageError,
) {
    val go = model.goLimits?.go
    val activity = model.goActivity
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    Surface(
        color = AndrocColor.Medium,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "OpenCode Go 额度",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AndrocColor.Deep,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    model.goLimits?.workspaceName?.takeIf { it.isNotBlank() } ?: "OpenCode Console",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (go == null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        loading -> "正在读取额度…"
                        !error.isNullOrBlank() -> error
                        else -> "额度暂不可用"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                go.rolling?.let { MeterTile("5 小时", it, dark, Modifier.weight(1f)) }
                go.weekly?.let { MeterTile("本周", it, dark, Modifier.weight(1f)) }
                go.monthly?.let { MeterTile("本月", it, dark, Modifier.weight(1f)) }
            }

            val monthly = go.monthly
            if (monthly != null) {
                Spacer(Modifier.height(10.dp))
                if (activity != null && activity.daily.isNotEmpty()) {
                    GoUsageChart(monthly, activity, dark)
                } else {
                    Text(
                        if (loading) "正在读取每日用量…" else "每日曲线暂不可用（Console 未返回数据）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 对话里 `/usage` 打开的额度卡片。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoUsageSheet(model: AppModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                "OpenCode Go",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            GoUsageCard(model)
        }
    }
}

@Composable
private fun MeterTile(label: String, meter: GoMeter, dark: Boolean, modifier: Modifier = Modifier) {
    val color = goQuotaColor(meter.percent, dark)
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                goQuotaPercent(meter.percent),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
        Spacer(Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { (meter.percent / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            if (meter.limited) "已用尽 · ${resetText(meter.resetsAt)}" else resetText(meter.resetsAt),
            style = MaterialTheme.typography.labelSmall,
            color = if (meter.limited) goQuotaColor(100.0, dark)
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GoUsageChart(monthly: GoMeter, activity: GoActivity, dark: Boolean) {
    val now = remember(monthly, activity) { System.currentTimeMillis() }
    val start = activity.start
    val end = remember(monthly) {
        Instant.ofEpochMilli(monthly.resetsAt).atZone(ZoneOffset.UTC).toLocalDate()
    }
    val startMillis = remember(start) {
        start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val nowDate = Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate()
    val today = if (nowDate.isAfter(end)) end else nowDate
    val totalDays = ChronoUnit.DAYS.between(start, end).toFloat().coerceAtLeast(1f)
    val elapsed = ((now - startMillis).toFloat() / 86_400_000f)
        .coerceIn(0.05f, totalDays)
    val dailyCost = remember(activity) { activity.daily.associate { it.date to it.costMicroCents } }
    val totalCost = remember(activity) { activity.daily.sumOf { it.costMicroCents }.coerceAtLeast(1L) }
    val current = monthly.percent

    val points = remember(monthly, activity, now) {
        val out = mutableListOf(0f to 0f)
        var cumulative = 0L
        var day = start
        while (day.isBefore(today)) {
            cumulative += dailyCost[day] ?: 0L
            out += (ChronoUnit.DAYS.between(start, day).toFloat() + 1f) to
                (current * cumulative / totalCost).toFloat()
            day = day.plusDays(1)
        }
        out += elapsed to current.toFloat()
        out
    }
    val forecast = (current * totalDays / elapsed).coerceAtLeast(current)
    val maxY = ((maxOf(100.0, current, forecast) * 1.08 / 10.0).let { kotlin.math.ceil(it) } * 10.0)
        .coerceAtLeast(10.0)
    val line = goQuotaColor(current, dark)
    val forecastColor = if (forecast > 100.0) goQuotaColor(100.0, dark)
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "本月累计 ${formatCost(totalCost / 100_000_000.0)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "月底约 ${goQuotaPercent(forecast)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = forecastColor,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(4.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(104.dp),
            ) {
                val left = 4.dp.toPx()
                val right = size.width - 4.dp.toPx()
                val top = 6.dp.toPx()
                val bottom = size.height - 4.dp.toPx()
                val w = (right - left).coerceAtLeast(1f)
                val h = (bottom - top).coerceAtLeast(1f)
                fun x(days: Float) = left + w * (days / totalDays).coerceIn(0f, 1f)
                fun y(percent: Double) = bottom - h * (percent / maxY).toFloat().coerceIn(0f, 1f)

                // 100% 额度线
                val yLimit = y(100.0)
                drawLine(
                    color = goQuotaColor(100.0, dark).copy(alpha = 0.55f),
                    start = Offset(left, yLimit),
                    end = Offset(right, yLimit),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "100%",
                    left,
                    (yLimit - 2.dp.toPx()).coerceAtLeast(10.dp.toPx()),
                    android.graphics.Paint().apply {
                        color = goQuotaColor(100.0, dark).copy(alpha = 0.8f).toArgb()
                        textSize = 9.dp.toPx()
                        isAntiAlias = true
                    },
                )

                // 已发生：实线 + 轻填充
                val solid = Path()
                val fill = Path()
                points.forEachIndexed { index, (dx, value) ->
                    val px = x(dx)
                    val py = y(value.toDouble())
                    if (index == 0) {
                        solid.moveTo(px, py)
                        fill.moveTo(px, bottom)
                        fill.lineTo(px, py)
                    } else {
                        solid.lineTo(px, py)
                        fill.lineTo(px, py)
                    }
                }
                fill.lineTo(x(points.last().first), bottom)
                fill.close()
                drawPath(fill, color = line.copy(alpha = 0.13f))
                drawPath(solid, color = line, style = Stroke(width = 2.dp.toPx()))

                // 预测：从今天到月底的虚线
                val last = points.last()
                drawLine(
                    color = forecastColor.copy(alpha = 0.8f),
                    start = Offset(x(last.first), y(last.second.toDouble())),
                    end = Offset(right, y(forecast)),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
                )
                drawCircle(line, radius = 2.5.dp.toPx(), center = Offset(x(last.first), y(last.second.toDouble())))
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                Text(
                    "${start.monthValue}/${start.dayOfMonth}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${end.monthValue}/${end.dayOfMonth} 重置",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                if (forecast > 100.0) "按当前速度，月底会超过额度（${goQuotaPercent(forecast)}）"
                else "按当前速度，月底用到 ${goQuotaPercent(forecast)}，在额度内",
                style = MaterialTheme.typography.labelSmall,
                color = if (forecast > 100.0) goQuotaColor(100.0, dark)
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun goQuotaPercent(value: Double): String = String.format("%.1f%%", value)

fun goQuotaColor(percent: Double, dark: Boolean): Color = when {
    percent >= 100 -> if (dark) Color(0xFFFF6B6B) else Color(0xFFD32F2F)
    percent >= 85 -> if (dark) Color(0xFFFFB020) else Color(0xFFE08A00)
    percent >= 70 -> if (dark) Color(0xFFFFD166) else Color(0xFFB7791F)
    else -> if (dark) Color(0xFF6BD48F) else Color(0xFF2E7D32)
}

private fun resetText(at: Long): String {
    val now = System.currentTimeMillis()
    val delta = at - now
    if (delta <= 0L) return "即将重置"
    val hours = delta / 3_600_000L
    if (hours < 24L) return "${hours.coerceAtLeast(1)} 小时后重置"
    val date = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = ChronoUnit.DAYS.between(LocalDate.now(ZoneId.systemDefault()), date)
    if (days <= 7L) return "${days.coerceAtLeast(1)} 天后重置"
    return "${date.monthValue}月${date.dayOfMonth}日重置"
}
