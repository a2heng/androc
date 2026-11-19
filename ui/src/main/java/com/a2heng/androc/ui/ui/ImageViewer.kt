package com.a2heng.androc.ui.ui

import android.graphics.Bitmap
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
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
