package com.a2heng.androc.ui.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.androc.ui.data.isAbsolutePath
import com.a2heng.androc.ui.data.joinPath
import com.a2heng.androc.ui.data.normPath
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// 一个够用的 Markdown 渲染器（不引第三方库）：
//   · 围栏代码块 ```lang ... ``` 单独成块，等宽 + 可横向滚动
//   · 行内 `code` / **粗体** / *斜体* / [链接](url) / # 标题
//   · 其余按纯文本渲染，保留换行
// ---------------------------------------------------------------------------

private sealed interface Block {
    data class Text(val text: String) : Block
    data class Code(val lang: String, val code: String) : Block
}

private sealed interface Line {
    data class Heading(val level: Int, val text: String) : Line
    data class Quote(val text: String) : Line
    data class ListItem(val indent: Int, val marker: String, val text: String) : Line
    data class Para(val text: String) : Line
    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>,
        val aligns: List<Int>,
    ) : Line

    /** 独占一行的图片：![alt](src) —— 渲染成可点开缩放的卡片。 */
    data class Image(val alt: String, val src: String) : Line
    data object Rule : Line
    data object Gap : Line
}

private val headingRe = Regex("^(#{1,6})\\s+(.*)$")
private val quoteRe = Regex("^\\s*>\\s?(.*)$")
private val listRe = Regex("^(\\s*)([-*+]|\\d+[.)])\\s+(.*)$")
private val ruleRe = Regex("^\\s*(---+|\\*\\*\\*+|___+)\\s*$")

/** 整行只有一张图（允许 alt 为空、允许 title）。 */
private val imageLineRe = Regex("""^\s*!\[([^\]]*)\]\(\s*([^)\s]+)(?:\s+"[^"]*")?\s*\)\s*$""")

private fun splitRow(line: String): List<String> =
    line.trim().trim('|').split('|').map { it.trim() }

private fun isTableSeparator(line: String): Boolean {
    val t = line.trim()
    if (!t.contains('-')) return false
    val cells = t.trim('|').split('|')
    return cells.isNotEmpty() && cells.all { Regex("^:?-{2,}:?$").matches(it.trim()) }
}

private fun tableAligns(line: String): List<Int> =
    splitRow(line).map { cell ->
        when {
            cell.startsWith(":") && cell.endsWith(":") -> 0
            cell.endsWith(":") -> 1
            else -> -1
        }
    }

private fun parseLines(text: String): List<Line> {
    val lines = text.split('\n')
    val out = mutableListOf<Line>()
    val para = mutableListOf<String>()
    fun flush() {
        if (para.isNotEmpty()) {
            out += Line.Para(para.joinToString("\n"))
            para.clear()
        }
    }
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trimEnd()
        // 竖线表格：| a | b | + | --- | --- |
        if (line.trimStart().startsWith("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
            flush()
            val headers = splitRow(line)
            val aligns = tableAligns(lines[i + 1])
            i += 2
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trimStart().startsWith("|")) {
                rows += splitRow(lines[i])
                i++
            }
            out += Line.Table(headers, rows, aligns)
            continue
        }
        val h = headingRe.find(line)
        if (h != null) {
            flush()
            out += Line.Heading(h.groupValues[1].length, h.groupValues[2].trim())
            i++
            continue
        }
        if (line.isBlank()) {
            flush()
            out += Line.Gap
            i++
            continue
        }
        if (ruleRe.matches(line)) {
            flush()
            out += Line.Rule
            i++
            continue
        }
        val q = quoteRe.find(line)
        if (q != null) {
            flush()
            // 连续的 > 行合成一段，左边那根竖线才是一整条
            val quoted = StringBuilder(q.groupValues[1])
            i++
            while (i < lines.size) {
                val next = quoteRe.find(lines[i].trimEnd()) ?: break
                quoted.append('\n').append(next.groupValues[1])
                i++
            }
            out += Line.Quote(quoted.toString())
            continue
        }
        val l = listRe.find(line)
        if (l != null) {
            flush()
            val indent = l.groupValues[1].length / 2
            val body = l.groupValues[3]
            // 任务列表：- [ ] / - [x] → ☐ / ☑
            val task = when {
                body.startsWith("[ ]") -> "☐" to body.removePrefix("[ ]").trimStart()
                body.startsWith("[x]") || body.startsWith("[X]") ->
                    "☑" to body.substring(3).trimStart()
                else -> null
            }
            out += if (task != null) {
                Line.ListItem(indent, task.first, task.second)
            } else {
                Line.ListItem(indent, l.groupValues[2], body)
            }
            i++
            continue
        }
        val img = imageLineRe.find(line)
        if (img != null) {
            flush()
            out += Line.Image(img.groupValues[1].trim(), img.groupValues[2].trim())
            i++
            continue
        }
        para += line
        i++
    }
    flush()
    return out
}

private fun splitBlocks(source: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val buffer = StringBuilder()
    var code: StringBuilder? = null
    var lang = ""

    fun flushText() {
        val t = buffer.toString().trim('\n')
        if (t.isNotBlank()) blocks += Block.Text(t)
        buffer.clear()
    }

    for (line in source.split('\n')) {
        val fence = line.trimStart()
        if (fence.startsWith("```")) {
            if (code == null) {
                flushText()
                code = StringBuilder()
                lang = fence.removePrefix("```").trim()
            } else {
                blocks += Block.Code(lang, code.toString().trimEnd('\n'))
                code = null
                lang = ""
            }
            continue
        }
        if (code != null) {
            code.append(line).append('\n')
        } else {
            buffer.append(line).append('\n')
        }
    }
    if (code != null) {
        // 没闭合的代码块也按代码显示，总比吞掉好
        blocks += Block.Code(lang, code.toString().trimEnd('\n'))
    } else {
        flushText()
    }
    return blocks
}

private val inlinePattern = Regex(
    "(`[^`\\n]+`)|(\\*\\*[^*\\n]+\\*\\*)|(~~[^~\\n]+~~)|(\\*[^*\\n]+\\*)|(!?\\[[^\\]\\n]*\\]\\([^)\\n]+\\))",
)

private val HTTP_LINK_RE = Regex("""^https?://""", RegexOption.IGNORE_CASE)

private fun isHttpLink(href: String): Boolean = HTTP_LINK_RE.containsMatchIn(href)

private fun isLikelyFileHref(href: String): Boolean {
    if (href.isBlank()) return false
    if (href.startsWith("#") || href.startsWith("mailto:") || href.startsWith("tel:")) return false
    if (isHttpLink(href)) return false
    if (href.startsWith("file://")) return true
    if (href.startsWith("~/") || href.startsWith("/") || href.startsWith("./") || href.startsWith("../")) return true
    // 形如 foo/bar、foo.md、README —— 常见相对文件引用
    return href.contains('/') || href.contains('.') && !href.startsWith("?")
}

private fun inline(
    text: String,
    code: Color,
    link: Color,
    onOpenPath: ((String) -> Unit)? = null,
): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    val linkStyle = SpanStyle(color = link, textDecoration = TextDecoration.Underline)
    for (match in inlinePattern.findAll(text)) {
        if (match.range.first > cursor) append(text.substring(cursor, match.range.first))
        val token = match.value
        when {
            token.startsWith("`") -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = code,
                        background = code.copy(alpha = 0.12f),
                    ),
                ) { append(token.substring(1, token.length - 1)) }
            }
            token.startsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(inlinePlain(token.substring(2, token.length - 2)))
                }
            }
            token.startsWith("~~") -> {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(inlinePlain(token.substring(2, token.length - 2)))
                }
            }
            token.startsWith("*") -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(inlinePlain(token.substring(1, token.length - 1)))
                }
            }
            token.startsWith("![") -> {
                // 图片：显示 [图片: alt]，href 若是文件可点击
                val alt = token.substringAfter("![").substringBefore("]")
                val href = token.substringAfter("](").substringBeforeLast(")")
                val label = if (alt.isBlank()) "[图片]" else "[图片: $alt]"
                appendLinked(label, href, linkStyle, onOpenPath)
            }
            token.startsWith("[") -> {
                val label = token.substringAfter('[').substringBefore(']')
                val href = token.substringAfter("](").substringBeforeLast(")")
                appendLinked(label, href, linkStyle, onOpenPath)
            }
            else -> append(token)
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

private fun AnnotatedString.Builder.appendLinked(
    label: String,
    href: String,
    linkStyle: SpanStyle,
    onOpenPath: ((String) -> Unit)?,
) {
    if (href.isBlank()) {
        append(label)
        return
    }
    when {
        isHttpLink(href) -> {
            val ann = LinkAnnotation.Url(
                href,
                TextLinkStyles(style = linkStyle),
            )
            withLink(ann) { append(label) }
        }
        isLikelyFileHref(href) && onOpenPath != null -> {
            pushStringAnnotation("FILE_PATH", href)
            withStyle(linkStyle) { append(label) }
            pop()
        }
        isLikelyFileHref(href) -> {
            // 没有回调时也保留样式，至少可见
            withStyle(linkStyle) { append(label) }
        }
        else -> withStyle(linkStyle) { append(label) }
    }
}

private fun inlinePlain(text: String): String = text
    .replace("`", "")
    .replace("**", "")
    .replace("*", "")

private fun estimateColWidth(texts: List<String>, compact: Boolean): androidx.compose.ui.unit.Dp {
    // 粗略估算：ASCII 按 1，CJK/全角按 2，避免中文表格被压成细条
    val maxUnits = texts.maxOfOrNull { s ->
        s.sumOf { ch ->
            when {
                ch.code in 0x2E80..0x9FFF || ch.code in 0xFF00..0xFFEF || ch.code in 0x3000..0x303F -> 2
                else -> 1
            } as Int
        }
    } ?: 0
    val unit = if (compact) 5 else 6
    val min = if (compact) 56 else 72
    val max = if (compact) 320 else 420
    return (maxUnits * unit).coerceIn(min, max).dp
}

@Composable
private fun MarkdownTable(
    line: Line.Table,
    codeColor: Color,
    linkColor: Color,
    compact: Boolean,
    onOpenPath: ((String) -> Unit)?,
) {
    val colCount = line.headers.size.coerceAtLeast(1)
    val widths = (0 until colCount).map { c ->
        estimateColWidth(
            listOf(line.headers.getOrElse(c) { "" }) + line.rows.map { it.getOrElse(c) { "" } },
            compact,
        )
    }
    // 网格线统一 1dp：以前每个格子自己画 0.5dp 边框，相邻两边叠成 1dp、
    // 靠外一侧只有 0.5dp，接缝粗细不齐才是表头错位的根源。
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val headerBg = AndrocColor.MediumStrong
    val stripeBg = AndrocColor.Medium
    val padH = if (compact) 6.dp else 8.dp
    val padV = if (compact) 4.dp else 6.dp
    val fontSize = if (compact) 10.5.sp else 12.sp
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, grid, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                TableRow(
                    cells = line.headers,
                    widths = widths,
                    aligns = line.aligns,
                    fontSize = fontSize,
                    padH = padH,
                    padV = padV,
                    header = true,
                    bg = headerBg,
                    grid = grid,
                    codeColor = codeColor,
                    linkColor = linkColor,
                    onOpenPath = onOpenPath,
                    dividerAbove = false,
                )
                if (line.rows.isEmpty()) {
                    TableRow(
                        cells = List(colCount) { "" },
                        widths = widths,
                        aligns = line.aligns,
                        fontSize = fontSize,
                        padH = padH,
                        padV = padV,
                        header = false,
                        bg = Color.Transparent,
                        grid = grid,
                        codeColor = codeColor,
                        linkColor = linkColor,
                        onOpenPath = onOpenPath,
                        dividerAbove = true,
                    )
                }
                line.rows.forEachIndexed { r, row ->
                    TableRow(
                        cells = row,
                        widths = widths,
                        aligns = line.aligns,
                        fontSize = fontSize,
                        padH = padH,
                        padV = padV,
                        header = false,
                        bg = if (r % 2 == 0) Color.Transparent else stripeBg,
                        grid = grid,
                        codeColor = codeColor,
                        linkColor = linkColor,
                        onOpenPath = onOpenPath,
                        dividerAbove = true,
                    )
                }
            }
        }
    }
}

/**
 * 一行表格：所有格子用同一高度（IntrinsicSize.Min + fillMaxHeight），
 * 竖线由格子之间的 1dp Box 画、横线由 drawBehind 画，接缝处处一致。
 */
@Composable
private fun TableRow(
    cells: List<String>,
    widths: List<Dp>,
    aligns: List<Int>,
    fontSize: TextUnit,
    padH: Dp,
    padV: Dp,
    header: Boolean,
    bg: Color,
    grid: Color,
    codeColor: Color,
    linkColor: Color,
    onOpenPath: ((String) -> Unit)?,
    dividerAbove: Boolean,
) {
    Row(
        Modifier
            .background(bg)
            .height(IntrinsicSize.Min)
            .drawBehind {
                if (dividerAbove) {
                    drawLine(grid, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                }
            },
    ) {
        widths.forEachIndexed { c, w ->
            if (c > 0) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(grid))
            }
            Box(Modifier.width(w).fillMaxHeight()) {
                LinkableText(
                    inline(cells.getOrElse(c) { "" }, codeColor, linkColor, onOpenPath),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = fontSize,
                        fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = when (aligns.getOrElse(c) { -1 }) {
                            0 -> TextAlign.Center
                            1 -> TextAlign.End
                            else -> TextAlign.Start
                        },
                    ),
                    modifier = Modifier.padding(horizontal = padH, vertical = padV),
                    onOpenPath = onOpenPath,
                )
            }
        }
    }
}

@Composable
private fun MarkdownLines(
    text: String,
    codeColor: Color,
    linkColor: Color,
    compact: Boolean,
    onOpenPath: ((String) -> Unit)?,
) {
    val lines = remember(text) { parseLines(text) }
    val base = if (compact) {
        MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp)
    } else {
        MaterialTheme.typography.bodyMedium
    }
    Column(Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            when (line) {
                is Line.Heading -> SelectionContainer {
                    LinkableText(
                    inline(line.text, codeColor, linkColor, onOpenPath),
                    style = if (compact) {
                        when (line.level) {
                            1 -> MaterialTheme.typography.titleSmall
                            2 -> MaterialTheme.typography.labelLarge
                            else -> MaterialTheme.typography.labelMedium
                        }
                    } else {
                        when (line.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        }
                    },
                    fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = if (compact) 4.dp else 6.dp, bottom = 2.dp),
                        onOpenPath = onOpenPath,
                    )
                }
                is Line.Quote -> SelectionContainer {
                    Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 2.dp),
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(2.dp)),
                    )
                    LinkableText(
                        inline(line.text, codeColor, linkColor, onOpenPath),
                        style = base,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                        onOpenPath = onOpenPath,
                    )
                    }
                }
                is Line.ListItem -> SelectionContainer {
                    Row(
                    Modifier.padding(start = (line.indent * 12).dp, top = 1.dp, bottom = 1.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        if (line.marker in listOf("-", "*", "+")) "•" else line.marker,
                        style = base,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(if (compact) 16.dp else 18.dp),
                    )
                    LinkableText(
                        inline(line.text, codeColor, linkColor, onOpenPath),
                        style = base,
                        modifier = Modifier.weight(1f),
                        onOpenPath = onOpenPath,
                    )
                    }
                }
                is Line.Table -> SelectionContainer {
                    MarkdownTable(line, codeColor, linkColor, compact, onOpenPath)
                }
                is Line.Image -> MarkdownImageCard(line.alt, line.src, compact, onOpenPath)
                Line.Rule -> HorizontalDivider(Modifier.padding(vertical = if (compact) 4.dp else 6.dp))
                Line.Gap -> Spacer(Modifier.height(if (compact) 4.dp else 6.dp))
                is Line.Para -> SelectionContainer {
                    LinkableText(
                        inline(line.text, codeColor, linkColor, onOpenPath),
                        style = base,
                        modifier = Modifier.padding(vertical = 1.dp),
                        onOpenPath = onOpenPath,
                    )
                }
            }
        }
    }
}

/**
 * Markdown 里的图片：卡片式显示（圆角 + 说明 + 点击放大）。
 * 远程 / data: URI 自己下载解码；本地路径交给文件卡片（onOpenPath）。
 */
@Composable
private fun MarkdownImageCard(
    alt: String,
    src: String,
    compact: Boolean,
    onOpenPath: ((String) -> Unit)?,
) {
    val remote = src.startsWith("data:") ||
        src.startsWith("http://") ||
        src.startsWith("https://")
    var bitmap by remember(src) { mutableStateOf(cachedImage(src)) }
    var failed by remember(src) { mutableStateOf(false) }
    var viewerOpen by remember(src) { mutableStateOf(false) }
    LaunchedEffect(src) {
        if (bitmap == null && remote) {
            val loaded = loadImageBitmap(src)
            if (loaded == null) failed = true else bitmap = loaded
        }
    }
    val title = alt.ifBlank {
        src.substringAfterLast('/').substringBefore('?').ifBlank { "图片" }
    }
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 3.dp else 5.dp)
            .background(AndrocColor.Medium, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f), shape)
            .clip(shape),
    ) {
        val shown = bitmap
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 72.dp else 96.dp, max = if (compact) 220.dp else 320.dp)
                .then(
                    when {
                        shown != null -> Modifier.clickableNoRipple { viewerOpen = true }
                        !remote && onOpenPath != null -> Modifier.clickableNoRipple { onOpenPath.invoke(src) }
                        else -> Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                shown != null -> Image(
                    bitmap = shown.asImageBitmap(),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
                remote && !failed -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                else -> Text(
                    if (failed) "图片加载失败" else "本地图片",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val hint = when {
                shown != null -> "点击放大"
                remote && !failed -> "加载中"
                failed -> "无法加载"
                else -> "点击打开"
            }
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall,
                color = if (shown != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
    val openBitmap = bitmap
    if (viewerOpen && openBitmap != null) {
        ZoomableImageDialog(openBitmap, title) { viewerOpen = false }
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onOpenPath: ((String) -> Unit)? = null,
) {
    val blocks = remember(text) { splitBlocks(text) }
    val codeColor = MaterialTheme.colorScheme.primary
    val linkColor = MaterialTheme.colorScheme.secondary
    val mono = MaterialTheme.colorScheme.onSurface

    Column(modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is Block.Code -> {
                    val context = LocalContext.current
                    var copied by remember(block.code) { mutableStateOf(false) }
                    LaunchedEffect(copied) {
                        if (copied) {
                            delay(1500)
                            copied = false
                        }
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                AndrocColor.Medium,
                                RoundedCornerShape(8.dp),
                            ),
                    ) {
                        // 头部：左边语言，右边复制按钮
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 4.dp, top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                block.lang,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                if (copied) "已复制" else "复制",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (copied) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        copyBlockToClipboard(context, block.code)
                                        copied = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        SelectionContainer {
                            Text(
                                block.code,
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (compact) 10.5.sp else 11.sp,
                                color = mono,
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
                is Block.Text -> MarkdownLines(block.text, codeColor, linkColor, compact, onOpenPath)
            }
        }
    }
}

/** 代码块右上角的复制：把整块代码原样写进系统剪贴板。 */
private fun copyBlockToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("OpenCode code", text))
}


// ---------------------------------------------------------------------------
// 文本里的路径：点一下就打开文件卡片。
// 支持：file:// URI、~/…、/…（绝对）、以及常见的相对路径（含 / 或带扩展名），
// 不要求以 / 开头。解析时按当前会话目录补全相对路径。
// ---------------------------------------------------------------------------

private val FILE_URI_RE = Regex("""file://[^\s"'`<>)\]]+""")

// 绝对/家目录路径：~ 或 / 开头，至少一段
private val ABS_PATH_RE = Regex("""(?:~|/)[\w.@%+\-]+(?:/[\w.@%+\-]+)*""")

// 相对路径：形如 src/foo.kt、./foo、../foo、a/b/c.ts、README.md（带扩展名的单段）
// 规则：由 [\w.@%+-] 和 / 组成，包含 / 或包含“看起来像扩展名”的 .xxx 结尾，
// 且不以 http(s):// 开头（避免误伤链接），前面不是 URL/协议的一部分。
private val REL_PATH_RE = Regex(
    """(?<![A-Za-z0-9_:/])(?:\./|\.\./)?[\w.@%+\-]+(?:/[\w.@%+\-]+)+(?:[?#][^\s"'`<>)\]]*)?""" +
        """|(?<![A-Za-z0-9_:/])(?:\./|\.\./)?[\w@%+\-]+(?:\.[\w@%+\-]+)*\.[A-Za-z0-9]{1,10}(?::\d+(?::\d+)?)?(?![A-Za-z0-9_])""",
)

private val TRAILING_JUNK = charArrayOf('.', ',', ';', ':', ')', ']', '}', '"', '\'', '>', '，', '。', '：', '）', '】', '」')

private fun cleanMatch(raw: String): String {
    var s = raw
    while (s.isNotEmpty() && s.last() in TRAILING_JUNK) s = s.dropLast(1)
    // 去掉 :line 或 :line:col 后缀（编辑器跳转格式），保留路径本体用于打开
    return s.replace(Regex(""":(\d+)(?::\d+)?$"""), "")
}

/** 返回 offset 落在哪个路径上；优先 file://，其次绝对/家目录，其次相对路径。 */
fun pathAt(text: String, offset: Int): String? {
    for (re in listOf(FILE_URI_RE, ABS_PATH_RE, REL_PATH_RE)) {
        for (m in re.findAll(text)) {
            if (offset in m.range) {
                val cleaned = cleanMatch(m.value)
                // 过滤掉明显不是文件路径的东西（纯数字/版本号等）
                if (cleaned == "." || cleaned == ".." || cleaned.all { it == '/' }) continue
                if (re === REL_PATH_RE && !looksLikeFilePath(cleaned)) continue
                return cleaned
            }
        }
    }
    return null
}

private fun looksLikeFilePath(s: String): Boolean {
    if (s.startsWith("./") || s.startsWith("../") || s.contains('/')) return true
    // 单段带扩展名：README.md、build.gradle.kts 等
    val dot = s.lastIndexOf('.')
    if (dot > 0 && dot < s.length - 1) {
        val ext = s.substring(dot + 1).lowercase()
        // 常见扩展名/后缀白名单倾向：避免把 "v1.2" 当路径
        if (ext.any { !it.isLetterOrDigit() }) return false
        if (ext.length in 1..10 && ext.any { it.isLetter() }) return true
    }
    return false
}

/**
 * 把 Markdown/工具里的原始路径解析成可交给文件 API 的绝对路径。
 * - file:// → 去协议并解码
 * - ~/ → 展开 home
 * - / 开头 → 原样
 * - 其余 → 相对 baseDir（会话目录）
 */
fun resolveFileRef(raw: String, baseDir: String, homeDir: String): String {
    var s = raw.trim()
    if (s.startsWith("file://")) {
        s = s.removePrefix("file://")
        // file:///C:/... (Windows 风格)
        if (Regex("""^/[A-Za-z]:""").containsMatchIn(s)) s = s.removePrefix("/")
        s = runCatching { java.net.URLDecoder.decode(s, "UTF-8") }.getOrElse { s }
        s = s.substringBefore('?').substringBefore('#')
    }
    s = normPath(cleanMatch(s))
    val home = normPath(homeDir).trimEnd('/')
    return when {
        s == "~" || s == "~/" -> home.ifBlank { s }
        s.startsWith("~/") -> if (home.isNotBlank()) home + s.drop(1) else s
        isAbsolutePath(s) -> s
        else -> {
            // 相对路径（不以 / 开头）：优先按会话目录补全；没有会话目录就按 HOME，
            // 否则服务端会按自己的工作目录解析，打开错文件或直接 404。
            val base = normPath(baseDir).trimEnd('/')
            when {
                base.isNotBlank() -> joinPath(base, s)
                home.isNotBlank() -> joinPath(home, s)
                else -> s
            }
        }
    }
}

@Composable
private fun LinkableText(
    annotated: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    onOpenPath: ((String) -> Unit)? = null,
) {
    // 不挂自定义 tap 手势：它会吞掉长按，导致 Markdown 文本无法选择/复制。
    Text(
        annotated,
        style = style,
        modifier = modifier,
        color = color,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
    )
}
