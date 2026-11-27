package com.a2heng.androc.ui.ui

import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.androc.ui.data.AppModel
import com.a2heng.androc.ui.data.FsEntry
import com.a2heng.androc.ui.data.isAbsolutePath
import com.a2heng.androc.ui.data.joinPath
import com.a2heng.androc.ui.data.normPath

@Composable
fun FilesScreen(model: AppModel, wide: Boolean = false) {
    LaunchedEffect(Unit) {
        model.ensureDrives()
        if (model.fsDir.isBlank()) model.loadDir(null)
    }

    val rootBase = model.fsBase()
    BackHandler(enabled = model.openFile != null || parentDir(model.fsDir, rootBase) != null) {
        if (model.openFile != null) {
            model.closeFile()
        } else {
            parentDir(model.fsDir, rootBase)?.let { model.loadDir(it) }
        }
    }

    if (wide) {
        // 横屏：左边目录/文件列表，右边预览，选文件不再把列表顶掉
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(320.dp).fillMaxHeight()) { FsListPane(model) }
            androidx.compose.material3.VerticalDivider(
                Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            )
            Box(Modifier.weight(1f).fillMaxHeight()) {
                val open = model.openFile
                if (open != null) {
                    FileViewer(model, open)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "选一个文件在这里查看",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        return
    }

    val open = model.openFile
    if (open != null) {
        FileViewer(model, open)
        return
    }

    FsListPane(model)
}

@Composable
private fun FsListPane(model: AppModel) {
    val rootBase = model.fsBase()
    var creatingFolder by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { parentDir(model.fsDir, rootBase)?.let { model.loadDir(it) } },
                enabled = parentDir(model.fsDir, rootBase) != null,
            ) {
                Icon(
                    Icons.Rounded.ArrowUpward,
                    contentDescription = "上级目录",
                    modifier = Modifier.size(20.dp),
                )
            }
            Text("文件", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Text(
                pathLabel(rootBase, model.fsDir),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
            if (model.fsLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            IconButton(onClick = { folderName = ""; creatingFolder = true }) {
                Icon(
                    Icons.Rounded.CreateNewFolder,
                    contentDescription = "新建文件夹",
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // 快速入口：第一个永远是 home，权限/路径迷路时可以一键回来。
        // 只放根入口：Windows 放盘符根，POSIX 放 home。
        // 不再依赖 sessions，避免会话轮询把整个文件页重组一遍。
        // 第一个「此电脑」列出所有盘；后面跟各会话的目录。
        val quickDirs = buildList {
            // 只有 Windows 服务端才有「此电脑」。
            val win = model.fsDrives.isNotEmpty() ||
                Regex("^[A-Za-z]:").containsMatchIn(model.homeDir)
            if (win) add("此电脑" to com.a2heng.androc.ui.data.AppModel.DRIVES_PATH)
            val home = model.homeDir.trimEnd('/')
            if (home.isNotBlank() && !Regex("^[A-Za-z]:").containsMatchIn(home)) {
                add("home" to home)
            }
            model.expandSessionDir(model.current?.directory)?.let { add("当前会话" to it) }
            model.sessions.forEach { s ->
                val dir = model.expandSessionDir(s.directory) ?: return@forEach
                if (none { it.second == dir }) {
                    add(dir.trimEnd('/').substringAfterLast('/').ifBlank { dir } to dir)
                }
            }
        }.distinctBy { it.second }.take(12)
        androidx.compose.foundation.layout.FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            quickDirs.forEach { (label, dir) ->
                FsChip(
                    label = label,
                    selected = model.fsDir.trimEnd('/') == dir.trimEnd('/'),
                ) {
                    model.closeFile()
                    model.loadDir(dir)
                }
            }
        }

        model.fsError?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }

        if (model.fsEntries.isEmpty() && !model.fsLoading) {
            EmptyState("目录是空的")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(model.fsEntries, key = { it.path }) { entry ->
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    FsRow(
                        entry = entry,
                        onClick = {
                            val full = fullPath(model.fsDir.ifBlank { rootBase }, entry.path)
                            if (entry.isDir) model.loadDir(full) else model.openFile(full)
                        },
                        onLongClick = {
                            val full = fullPath(model.fsDir.ifBlank { rootBase }, entry.path)
                            scope.launch { shareEntry(context, full, entry.isDir) }
                        },
                    )
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }

    if (creatingFolder) {
        AppDialog(
            onDismissRequest = { creatingFolder = false },
            title = { Text("新建文件夹") },
            text = {
                Column {
                    Text(
                        pathLabel(rootBase, model.fsDir),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        placeholder = { Text("新文件夹名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = folderName.isNotBlank(),
                    onClick = {
                        val name = folderName.trim().trim('/')
                        creatingFolder = false
                        folderName = ""
                        if (name.isNotEmpty()) {
                            model.createFolder(model.fsDir, name) {
                                model.loadDir(model.fsDir)
                            }
                        }
                    },
                ) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = {
                    creatingFolder = false
                    folderName = ""
                }) { Text("取消") }
            },
        )
    }
}

private fun fullPath(root: String, relative: String): String = FsNav.join(root, relative)

private fun isUnder(base: String, path: String): Boolean {
    val b = normPath(base).trimEnd('/')
    val p = normPath(path).trimEnd('/')
    if (b.isEmpty()) return true
    return p == b || p.startsWith("$b/")
}

private fun sameRoot(a: String, b: String): Boolean? {
    val ra = Regex("^[A-Za-z]:").find(a)?.value?.lowercase()
    val rb = Regex("^[A-Za-z]:").find(b)?.value?.lowercase()
    if (ra != null || rb != null) return ra == rb
    return true
}

/** 已经是根（"/" 或 Windows 的 "D:/"）就没有上一级。 */
private fun isRootPath(p: String): Boolean =
    p.isEmpty() || p == "/" || Regex("^[A-Za-z]:$").matches(p)

/** 上一级；规则集中在 FsNav。 */
private fun parentDir(current: String, base: String): String? = FsNav.up(current, base)

/** Ruta visible: relativa a HOME; la propia raiz se muestra como "~". */
private fun pathLabel(base: String, dir: String): String {
    val b = normPath(base).trimEnd('/')
    val d = normPath(dir).trimEnd('/')
    return when {
        d.isBlank() -> "~"
        b.isNotEmpty() && d == b -> "~"
        b.isNotEmpty() && b != "/" && d.startsWith("$b/") -> "~" + d.removePrefix(b)
        else -> d
    }
}


@Composable
private fun FsChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) AndrocColor.MediumStrong
        else AndrocColor.Medium,
        contentColor = if (selected) AndrocColor.Deep
        else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
        ),
        modifier = Modifier.clickableNoRipple(onClick),
    ) {
        Text(
            label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

private fun zipDir(src: File, dest: File) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(dest))).use { zos ->
        src.walkTopDown().forEach { f ->
            if (f == src) return@forEach
            val rel = f.relativeTo(src).path.replace('\\', '/')
            if (rel.isBlank()) return@forEach
            val name = if (f.isDirectory) "$rel/" else rel
            zos.putNextEntry(ZipEntry(name))
            if (f.isFile) f.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }
}

private suspend fun shareEntry(context: Context, path: String, isDir: Boolean) {
    val file = File(path)
    if (!file.exists()) return
    val shareFile = if (isDir) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "正在打包文件夹…", Toast.LENGTH_SHORT).show()
        }
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "share")
            dir.mkdirs()
            val safe = file.name.ifBlank { "folder" }.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val zip = File(dir, "$safe-${System.currentTimeMillis()}.zip")
            zipDir(file, zip)
            zip
        }
    } else file
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
    val mime = if (isDir) "application/zip" else {
        context.contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(shareFile.extension.lowercase())
            ?: "*/*"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    withContext(Dispatchers.Main) {
        context.startActivity(Intent.createChooser(intent, "共享"))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FsRow(entry: FsEntry, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (entry.isDir) Icons.Rounded.Folder else Icons.Rounded.Description,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (entry.isDir) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            entry.path.trimEnd('/').substringAfterLast('/'),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FileViewer(model: AppModel, path: String) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { model.closeFile() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    path.substringAfterLast('/'),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    path.substringBeforeLast('/', ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (model.openFileLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(end = 4.dp), strokeWidth = 2.dp)
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            FilePreviewContent(model, path, Modifier.fillMaxSize())
        }
    }
}
