package com.a2heng.androc.ui.ui

import android.graphics.Bitmap
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

// ---------------------------------------------------------------------------
// 图片：统一解码 + 缓存 + 全屏缩放浏览。所有地方（聊天、工具输出、文件页）共用这一套。
//   · data:image/...;base64、http(s)、file://、绝对路径都能读
//   · 命中缓存直接显示，解码最长边压到 maxSide，避免大图 OOM
//   · 全屏浏览用 Telephoto：双指缩放、双击放大、放大后可拖动、边界回弹都是现成的
// ---------------------------------------------------------------------------

private val imageCache: LruCache<String, Bitmap> =
    object : LruCache<String, Bitmap>(16 * 1024) { // 16 MB
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

private val imageHttp by lazy { OkHttpClient() }

/** 已解码的图（只查缓存，不触发加载）。 */
fun cachedImage(src: String): Bitmap? = imageCache.get(src)

/** 把字节流解码成 Bitmap，最长边不超过 maxSide。 */
fun decodeBitmapBytes(bytes: ByteArray, maxSide: Int = 1600): Bitmap? {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
    return runCatching {
        android.graphics.BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            android.graphics.BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }.getOrNull()
}

/** 读一张图：支持 data: / http(s) / file:// / 本地路径。失败返回 null。 */
suspend fun loadImageBitmap(src: String, maxSide: Int = 1600): Bitmap? {
    cachedImage(src)?.let { return it }
    val bitmap = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = when {
                src.startsWith("data:") -> {
                    val comma = src.indexOf(',')
                    if (comma < 0) null else Base64.decode(src.substring(comma + 1), Base64.DEFAULT)
                }
                src.startsWith("http://") || src.startsWith("https://") -> {
                    imageHttp.newCall(Request.Builder().url(src).build()).execute().use { resp ->
                        if (resp.isSuccessful) resp.body?.bytes() else null
                    }
                }
                else -> {
                    val path = if (src.startsWith("file://")) {
                        android.net.Uri.parse(src).path
                    } else {
                        src
                    }
                    path?.let { p -> File(p).takeIf { it.isFile }?.readBytes() }
                }
            }
            bytes?.let { decodeBitmapBytes(it, maxSide) }
        }.getOrNull()
    }
    if (bitmap != null) imageCache.put(src, bitmap)
    return bitmap
}

/** 全屏图片浏览：黑底 + 关闭按钮 + 缩放提示，手势全部交给 Telephoto。 */
@Composable
fun ZoomableImageDialog(
    bitmap: Bitmap,
    title: String? = null,
    onClose: () -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f)),
        ) {
            key(bitmap) {
                val zoomState = rememberZoomableState()
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title ?: "图片",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .zoomable(zoomState),
                )
            }
            Row(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.14f), CircleShape),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "关闭", tint = Color.White)
                }
            }
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                if (!title.isNullOrBlank()) {
                    Text(
                        title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "双指缩放 · 双击放大 · 拖动查看",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}

/** 已经拿到 Bitmap 的预览图：点一下全屏看大图。 */
@Composable
fun ClickableBitmap(
    bitmap: Bitmap,
    title: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    contentDescription: String? = title,
) {
    var open by remember(bitmap) { mutableStateOf(false) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.clickableNoRipple { open = true },
    )
    if (open) ZoomableImageDialog(bitmap, title) { open = false }
}

/**
 * 多图全屏查看：左右滑动翻页，单页仍支持双指缩放。
 * 缩放后横向手势会被图片吃掉，所以左右两侧另给翻页按钮兜底。
 */
@Composable
fun ZoomableImagePagerDialog(
    uris: List<String>,
    initialIndex: Int = 0,
    title: String = "图片",
    onClose: () -> Unit,
) {
    if (uris.isEmpty()) return
    val scope = rememberCoroutineScope()
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f)),
        ) {
            val pager = rememberPagerState(
                initialPage = initialIndex.coerceIn(0, uris.lastIndex),
            ) { uris.size }
            HorizontalPager(
                state = pager,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val uri = uris[page]
                val bitmap = remember(uri) { decodeDataUri(uri, 1600) }
                if (bitmap != null) {
                    key(bitmap) {
                        val zoomState = rememberZoomableState()
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .zoomable(zoomState),
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("图片无法解码", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
            // 顶栏：渐变遮罩 + 关闭 + 标题 + 页码，没有悬浮圆钮
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.72f),
                            0.22f to Color.Transparent,
                        ),
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.Top,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                    )
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(top = 12.dp),
                )
                if (uris.size > 1) {
                    Surface(
                        color = Color(0xFF1F1F22),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(
                            "${pager.currentPage + 1} / ${uris.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            // 底栏：一颗深色胶囊，多图时两端给轻量翻页箭头
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp)
                    .background(Color(0xFF1F1F22), RoundedCornerShape(50))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (uris.size > 1) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0))
                            }
                        },
                        enabled = pager.currentPage > 0,
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "上一张",
                            tint = if (pager.currentPage > 0) Color.White
                            else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    if (uris.size > 1) "左右滑动切换 · 双指缩放"
                    else "双指缩放 · 双击放大",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                if (uris.size > 1) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(uris.lastIndex))
                            }
                        },
                        enabled = pager.currentPage < uris.lastIndex,
                        modifier = Modifier.size(30.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "下一张",
                            tint = if (pager.currentPage < uris.lastIndex) Color.White
                            else Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
