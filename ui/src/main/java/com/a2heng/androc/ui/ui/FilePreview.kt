package com.a2heng.androc.ui.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.androc.ui.data.AppModel

// ---------------------------------------------------------------------------
// 文件预览的唯一实现：聊天里的文件卡片、文件页、Markdown 里的文件引用都走这里。
// 一次改（图片缩放、Markdown、媒体播放、二进制提示）到处生效。
// 状态来自 AppModel.openFile()：先 openFile(path)，再渲染 FilePreviewContent()。
// ---------------------------------------------------------------------------

/** 文件内容预览骨架：加载中 / 媒体 / 图片 / 二进制 / Markdown / 纯文本。 */
@Composable
fun FilePreviewContent(
    model: AppModel,
    path: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val mediaUri = model.openFileMediaUri
    val mediaKind = model.openFileMediaKind
    when {
        model.openFileLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }

        mediaUri != null && mediaKind != null -> Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MediaPreview(
                uri = mediaUri,
                kind = mediaKind,
                title = path.substringAfterLast('/'),
                modifier = Modifier.fillMaxWidth(),
            )
            FileMeta(model, mediaKind)
        }

        model.openFileImage != null -> ClickableBitmap(
            bitmap = model.openFileImage!!,
            title = path.substringAfterLast('/'),
            modifier = modifier.fillMaxSize(),
        )

        model.openFileBinary -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                model.openFileText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        model.openFileMarkdown -> Box(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            val baseDir = path.substringBeforeLast('/', "")
            MarkdownText(
                model.openFileText,
                Modifier.fillMaxWidth(),
                compact = compact,
                onOpenPath = { ref -> model.openFile(resolveFileRef(ref, baseDir, model.homeDir)) },
            )
        }

        else -> Box(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FileMeta(model, "text")
                Text(
                    model.openFileText.take(120_000),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** 统一的一行元信息：MIME · 大小。 */
@Composable
private fun FileMeta(model: AppModel, fallbackKind: String) {
    val meta = listOfNotNull(
        model.openFileMime.ifBlank { fallbackKind },
        if (model.openFileBytesSize > 0L) "${model.openFileBytesSize / 1024} KB" else null,
    ).joinToString(" · ")
    if (meta.isBlank()) return
    Text(
        meta,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
