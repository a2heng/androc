package com.a2heng.androc.ui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.androc.ui.data.AppModel
import com.a2heng.androc.ui.data.LocalGit
import com.a2heng.androc.ui.data.VcsDiffFile
import com.a2heng.androc.ui.data.VcsFileStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GitScreen(
    model: AppModel,
    directory: String,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val info = model.gitInfo
    val selected = model.gitSelectedFile
    val files = mergeGitFiles(model.gitStatus, model.gitDiffFiles)
    var diffPath by remember { mutableStateOf<String?>(null) }
    val localGit = model.localGitActive
    var section by remember { mutableStateOf("changes") } // changes | history
    var paneNonce by remember { mutableIntStateOf(0) }
    var repoDir by remember(directory) { mutableStateOf(directory) }
    LaunchedEffect(directory) { repoDir = directory }
    LaunchedEffect(localGit) { if (!localGit) section = "changes" }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
            GitHeader(
                directory = repoDir,
                branch = info.label,
                provider = info.provider,
                baseLabel = model.gitBaseLabel,
                mode = model.gitMode,
                loading = model.gitLoading,
                files = if (localGit && section != "changes") emptyList() else files,
                onBack = onBack,
                localGit = localGit,
                section = section,
                onSection = {
                    section = it
                    if (it == "changes") model.loadGit("working")
                },
                onRefresh = {
                    if (localGit && section != "changes") paneNonce++
                    else model.loadGit(if (localGit) "working" else model.gitMode)
                },
                onMode = { model.loadGit(it) },
            )

            model.gitError?.let { err ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(onClick = { model.loadGit(model.gitMode) }) { Text("刷新") }
                    }
                }
            }

            val hasVcs = info.provider.isNotBlank() || model.gitStatus.isNotEmpty() || model.gitDiffFiles.isNotEmpty()
            if (model.gitLoading && !hasVcs && model.gitError == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
                return@Column
            }

            if (!hasVcs && !model.gitLoading && model.gitError == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        EmptyState(
                            "未检测到 Git 信息",
                            "当前目录：${directory.ifBlank { "—" }}\n" +
                                "刚在终端里 git init 过，点「重新检测」；" +
                                "目录还不是仓库，点「初始化 Git 仓库」。\n" +
                                "仓库在更深的子目录时，需要把会话目录切到那里。",
                        )
                        Row(
                            Modifier.padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextButton(onClick = { model.initGit() }) { Text("初始化 Git 仓库") }
                            TextButton(onClick = { model.reloadGit() }) { Text("重新检测") }
                        }
                        model.gitDiagnostic?.let { diagnostic ->
                            Text(
                                diagnostic,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .background(
                                        AndrocColor.Medium,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .heightIn(max = 260.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(8.dp),
                            )
                        }
                    }
                }
                return@Column
            }

            if (localGit && section == "history") {
                HistoryPane(
                    directory = repoDir,
                    nonce = paneNonce,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onRepoChange = { repoDir = it },
                )
                return@Column
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            if (files.isEmpty() && !model.gitLoading) {
                val msg = when (model.gitMode) {
                    "committed" -> "上一次提交没有可显示的差异。"
                    "branch" -> "与基线相比没有差异。"
                    else -> "没有检测到未提交的变更。"
                }
                EmptyState(
                    msg,
                    when (model.gitMode) {
                        "committed" ->
                            "「上一次提交」对比的是 HEAD~1 → HEAD。仓库只有 1 个提交、" +
                                "最近一次提交本身为空、或没有可用的 review base 时，这里都会为空；" +
                                "再提交一次后就能看到上一次的改动。"
                        "branch" ->
                            "「与基线相比」需要服务端能确定 review base（远端默认分支或 merge-base）。"
                        else ->
                            "「工作区」显示未提交的改动；工作区干净时这里就是空的。"
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(files, key = { "${model.gitMode}:${it.path}" }) { item ->
                        GitFileRow(
                            item = item,
                            selected = selected == item.path,
                            onClick = {
                                model.selectGitFile(item.path)
                                diffPath = item.path
                            },
                        )
                    }
                }
            }
    }
    // 文件 diff 用弹层展示，列表本身保持全屏
    diffPath?.let { path ->
        val item = files.firstOrNull { it.path == path }
        val diff = model.gitDiffFiles.firstOrNull { it.file == path }
        DetailOverlay(onDismiss = { diffPath = null }) { close ->
            GitDiffPopup(path, item, diff, close)
        }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GitHeader(
    directory: String,
    branch: String,
    provider: String,
    baseLabel: String?,
    mode: String,
    loading: Boolean,
    files: List<GitRowItem>,
    onBack: () -> Unit,
    localGit: Boolean = false,
    section: String = "changes",
    onSection: (String) -> Unit = {},
    onRefresh: () -> Unit,
    onMode: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.AccountTree,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Git", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (branch.isNotBlank() && branch != "—") {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            branch,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (provider.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(provider, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val sub = directory.ifBlank { "—" }
                Text(
                    sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "刷新")
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (localGit) {
                FilterChip(
                    selected = section == "changes",
                    onClick = { onSection("changes") },
                    label = { Text("变更", maxLines = 1) },
                )
                FilterChip(
                    selected = section == "history",
                    onClick = { onSection("history") },
                    label = { Text("历史", maxLines = 1) },
                )
            } else {
                FilterChip(
                    selected = mode == "working",
                    onClick = { if (!loading) onMode("working") },
                    label = { Text("工作区", maxLines = 1) },
                )
                FilterChip(
                    selected = mode == "committed",
                    onClick = { if (!loading) onMode("committed") },
                    label = { Text("上一次提交", maxLines = 1) },
                )
            }
            Spacer(Modifier.weight(1f))
            GitSummary(files)
        }
    }
}

private data class GitRowItem(val path: String, val status: String, val additions: Int, val deletions: Int)

// 与 ToolViews.DiffView 保持一致的主题感知配色
private data class DiffColors(val plus: Color, val minus: Color, val hunk: Color)

@Composable
private fun rememberDiffColors(): DiffColors {
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    return DiffColors(
        plus = if (dark) Color(0xFF4ADE80) else Color(0xFF17864A),
        minus = if (dark) Color(0xFFF87171) else Color(0xFFC0323D),
        hunk = if (dark) Color(0xFF7DD3FC) else Color(0xFF1D7FA6),
    )
}

@Composable
private fun GitSummary(files: List<GitRowItem>) {
    val add = files.sumOf { it.additions }
    val del = files.sumOf { it.deletions }
    if (files.isEmpty() && add == 0 && del == 0) return
    val colors = rememberDiffColors()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${files.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        if (add > 0) Text(
            "+$add",
            style = MaterialTheme.typography.labelSmall,
            color = colors.plus,
            maxLines = 1,
            softWrap = false,
        )
        if (del > 0) Text(
            "-$del",
            style = MaterialTheme.typography.labelSmall,
            color = colors.minus,
            maxLines = 1,
            softWrap = false,
        )
    }
}

private fun mergeGitFiles(status: List<VcsFileStatus>, diffs: List<VcsDiffFile>): List<GitRowItem> {
    val byPath = linkedMapOf<String, GitRowItem>()
    status.forEach {
        byPath[it.file] = GitRowItem(it.file, it.status, it.additions, it.deletions)
    }
    diffs.forEach { d ->
        val prev = byPath[d.file]
        byPath[d.file] = GitRowItem(
            d.file,
            prev?.status?.takeIf { it.isNotBlank() } ?: d.status,
            if (d.additions > 0 || prev == null) d.additions else prev.additions,
            if (d.deletions > 0 || prev == null) d.deletions else prev.deletions,
        )
    }
    return byPath.values.sortedBy { it.path.lowercase() }
}

@Composable
private fun GitDiffPopup(
    path: String,
    item: GitRowItem?,
    diff: VcsDiffFile?,
    onClose: () -> Unit,
) {
    val (contentMin, contentMax) = rememberDialogContentHeight()
    val colors = rememberDiffColors()
    Surface(
        color = AndrocColor.Medium,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .widthIn(max = 720.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(18.dp),
                    )
                }
                Icon(
                    Icons.Rounded.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                val badge = statusBadge(item?.status ?: diff?.status.orEmpty(), colors)
                Text(
                    badge.first,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = badge.second,
                    modifier = Modifier.width(16.dp),
                )
                Text(
                    path,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (item != null && item.additions > 0) {
                    Text(
                        "+${item.additions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.plus,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(6.dp))
                }
                if (item != null && item.deletions > 0) {
                    Text(
                        "−${item.deletions}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.minus,
                        maxLines = 1,
                    )
                }
            }
            Column(
                Modifier
                    .padding(top = 6.dp)
                    .heightIn(min = contentMin, max = contentMax)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
            ) {
                if (diff != null) {
                    GitDiffPanel(diff)
                } else {
                    Text(
                        "该文件没有可展开的补丁内容。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun GitFileRow(item: GitRowItem, selected: Boolean, onClick: () -> Unit) {
    val colors = rememberDiffColors()
    val (statusLabel, statusColor) = statusBadge(item.status, colors)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) AndrocColor.MediumStrong else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            statusLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            modifier = Modifier.width(18.dp),
        )
        val segments = item.path.split('/').filter { it.isNotEmpty() }
        val pathLabel = if (segments.size <= 3) item.path
        else "…/" + segments.takeLast(2).joinToString("/")
        Text(
            pathLabel,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (item.additions > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                "+${item.additions}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.plus,
                maxLines = 1,
                softWrap = false,
            )
        }
        if (item.deletions > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                "-${item.deletions}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.minus,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun GitDiffPanel(diff: VcsDiffFile) {
    val patch = diff.patch.ifBlank { "（无可显示的补丁内容）" }
    val colors = rememberDiffColors()
    val metaFg = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, bottom = 8.dp)
            .background(AndrocColor.Medium, RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
    ) {
        if (diff.more) {
            Text(
                "内容较多，已截断部分显示",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        // 保留 diff 的上下文行（-U3）；只去掉补丁头噪声
        val filtered = patch.lineSequence().filter { line ->
            !line.startsWith("diff --git ") &&
                !line.startsWith("index ") &&
                !line.startsWith("--- ") &&
                !line.startsWith("+++ ") &&
                !line.startsWith('\\') &&
                !line.startsWith("Binary ")
        }
        val lines = filtered.take(220).toList()
        val truncated = filtered.drop(220).any()
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                lines.forEach { line ->
                    val (fg, bg) = when {
                        line.startsWith("+++") || line.startsWith("---") ->
                            metaFg to Color.Transparent
                        line.startsWith("@@") ->
                            colors.hunk to colors.hunk.copy(alpha = 0.10f)
                        line.startsWith("+") ->
                            colors.plus to colors.plus.copy(alpha = 0.10f)
                        line.startsWith("-") ->
                            colors.minus to colors.minus.copy(alpha = 0.10f)
                        line.startsWith("new file mode") ||
                            line.startsWith("deleted file mode") ||
                            line.startsWith("similarity index") ||
                            line.startsWith("rename from") ||
                            line.startsWith("rename to") ||
                            line.startsWith("copy from") ||
                            line.startsWith("copy to") ->
                            metaFg to Color.Transparent
                        else -> MaterialTheme.colorScheme.onSurfaceVariant to Color.Transparent
                    }
                    Text(
                        line.ifEmpty { " " },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        color = fg,
                        softWrap = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg)
                            .padding(horizontal = 8.dp, vertical = 0.5.dp),
                    )
                }
                if (truncated) {
                    Text(
                        "…（已截断显示）",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}


@Composable
private fun statusBadge(status: String, colors: DiffColors): Pair<String, Color> = when (status) {
    "added" -> "A" to colors.plus
    "deleted" -> "D" to colors.minus
    "renamed" -> "R" to MaterialTheme.colorScheme.tertiary
    "modified" -> "M" to MaterialTheme.colorScheme.primary
    else -> "•" to MaterialTheme.colorScheme.onSurfaceVariant
}

private data class CommitDiffView(val title: String, val diff: VcsDiffFile)

@Composable
private fun HistoryPane(
    directory: String,
    nonce: Int,
    modifier: Modifier = Modifier,
    onRepoChange: (String) -> Unit = {},
) {
    var commits by remember(directory) { mutableStateOf<List<LocalGit.Commit>>(emptyList()) }
    var branches by remember(directory) { mutableStateOf<List<LocalGit.BranchInfo>>(emptyList()) }
    var loading by remember(directory) { mutableStateOf(true) }
    var ref by remember(directory) { mutableStateOf<String?>(null) }
    var expanded by remember(directory) { mutableStateOf<String?>(null) }
    var filesByCommit by remember(directory) {
        mutableStateOf<Map<String, List<LocalGit.CommitFile>>>(emptyMap())
    }
    var popup by remember { mutableStateOf<CommitDiffView?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(directory, nonce, ref) {
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching {
                LocalGit.log(directory, ref = ref) to LocalGit.branches(directory)
            }.getOrDefault(emptyList<LocalGit.Commit>() to emptyList<LocalGit.BranchInfo>())
        }
        commits = result.first
        branches = result.second
        loading = false
    }

    val parentRepo = remember(directory, commits.isEmpty()) {
        if (commits.isEmpty()) runCatching { LocalGit.parentRepoRoot(directory) }.getOrNull() else null
    }

    Box(modifier) {
        Column(Modifier.fillMaxSize()) {
            if (branches.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    BranchChip("全部", ref == null) { ref = null }
                    branches.filter { !it.remote }.forEach { branch ->
                        BranchChip(
                            if (branch.current) "● ${branch.name}" else branch.name,
                            ref == branch.name,
                        ) { ref = branch.name }
                    }
                }
            }
            when {
                loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                }
                commits.isEmpty() -> Column(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    EmptyState(
                        "这个仓库还没有提交",
                        if (parentRepo != null) {
                            "当前目录是独立仓库；你之前提交的可能在上级仓库：\n$parentRepo"
                        } else {
                            "提交历史为空，或本机 Git 暂不可用。"
                        },
                    )
                    if (parentRepo != null) {
                        TextButton(onClick = { onRepoChange(parentRepo) }) {
                            Text("查看上级仓库")
                        }
                    }
                }
                else -> {
                    val rows = remember(commits) { layoutGraph(commits) }
                    LazyColumn(
                        Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        items(rows, key = { it.commit.hash }) { row ->
                            val hash = row.commit.hash
                            CommitRow(row, expanded == hash) {
                                expanded = if (expanded == hash) null else hash
                                if (expanded == hash && filesByCommit[hash] == null) {
                                    scope.launch {
                                        val list = withContext(Dispatchers.IO) {
                                            runCatching { LocalGit.commitFiles(directory, hash) }
                                                .getOrDefault(emptyList())
                                        }
                                        filesByCommit = filesByCommit + (hash to list)
                                    }
                                }
                            }
                            if (expanded == hash) {
                                val files = filesByCommit[hash]
                                when {
                                    files == null -> Box(
                                        Modifier.fillMaxWidth()
                                            .padding(start = 46.dp, top = 4.dp, bottom = 8.dp),
                                    ) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    }
                                    files.isEmpty() -> Text(
                                        "该提交没有文件差异",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 46.dp, top = 4.dp, bottom = 8.dp),
                                    )
                                    else -> files.forEach { file ->
                                        CommitFileRow(file) {
                                            scope.launch {
                                                val patch = withContext(Dispatchers.IO) {
                                                    runCatching {
                                                        LocalGit.commitDiff(directory, hash, file.path)
                                                    }.getOrDefault("")
                                                }
                                                if (patch.isNotBlank()) {
                                                    popup = CommitDiffView(
                                                        "${row.commit.short} · ${file.path}",
                                                        VcsDiffFile(
                                                            file = file.path,
                                                            patch = patch,
                                                            additions = file.additions,
                                                            deletions = file.deletions,
                                                            status = file.status,
                                                        ),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        popup?.let { view ->
            DetailOverlay(onDismiss = { popup = null }) { close ->
                GitDiffPopup(view.title, null, view.diff, close)
            }
        }
    }
}

@Composable
private fun BranchChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else AndrocColor.Medium,
        contentColor = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun CommitRow(row: GraphRow, expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GraphCell(row)
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.commit.short,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    row.commit.subject.ifBlank { "(无提交信息)" },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (expanded) "收起" else "展开",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.commit.refs.take(3).forEach { RefChip(it) }
                Text(
                    row.commit.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    fmtCommitTime(row.commit.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun RefChip(raw: String) {
    val isTag = raw.startsWith("tag: ")
    val label = raw.removePrefix("HEAD -> ").removePrefix("tag: ")
    if (label.isBlank()) return
    val color = if (isTag) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = color,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun CommitFileRow(file: LocalGit.CommitFile, onClick: () -> Unit) {
    val colors = rememberDiffColors()
    val badge = statusBadge(file.status, colors)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 46.dp, end = 14.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            badge.first,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = badge.second,
            modifier = Modifier.width(18.dp),
        )
        Text(
            file.path,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (file.additions > 0) {
            Spacer(Modifier.width(6.dp))
            Text("+${file.additions}", style = MaterialTheme.typography.labelSmall, color = colors.plus)
        }
        if (file.deletions > 0) {
            Spacer(Modifier.width(6.dp))
            Text("-${file.deletions}", style = MaterialTheme.typography.labelSmall, color = colors.minus)
        }
    }
}

@Composable
private fun BranchesPane(
    directory: String,
    nonce: Int,
    modifier: Modifier = Modifier,
) {
    var branches by remember(directory) { mutableStateOf<List<LocalGit.BranchInfo>>(emptyList()) }
    var tags by remember(directory) { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember(directory) { mutableStateOf(true) }

    LaunchedEffect(directory, nonce) {
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching { LocalGit.branches(directory) to LocalGit.tags(directory) }
                .getOrDefault(emptyList<LocalGit.BranchInfo>() to emptyList<String>())
        }
        branches = result.first
        tags = result.second
        loading = false
    }

    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 24.dp)) {
        item { PaneLabel("本地分支") }
        val locals = branches.filter { !it.remote }
        if (locals.isEmpty() && !loading) {
            item {
                Text(
                    "没有本地分支（仓库可能还没有提交）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        items(locals, key = { "l:${it.name}" }) { branch ->
            BranchRow(branch)
        }
        val remotes = branches.filter { it.remote }
        if (remotes.isNotEmpty()) {
            item { PaneLabel("远程分支") }
            items(remotes, key = { "r:${it.name}" }) { branch ->
                BranchRow(branch)
            }
        }
        if (tags.isNotEmpty()) {
            item { PaneLabel("标签") }
            items(tags, key = { "t:$it" }) { tag ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tag, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PaneLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun BranchRow(branch: LocalGit.BranchInfo) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.AccountTree,
            contentDescription = null,
            tint = if (branch.current) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            branch.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (branch.current) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (branch.current) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    "当前",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        } else if (branch.upstream.isNotBlank()) {
            Text(
                "→ ${branch.upstream}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

// ---- 提交图 ---------------------------------------------------------------

private data class GraphRow(
    val commit: LocalGit.Commit,
    val lane: Int,
    val activeBefore: BooleanArray,
    val parentLanes: List<Int>,
)

private fun layoutGraph(commits: List<LocalGit.Commit>): List<GraphRow> {
    val lanes = ArrayList<String?>()
    val out = ArrayList<GraphRow>()
    for (commit in commits) {
        var lane = lanes.indexOf(commit.hash)
        if (lane < 0) {
            lane = lanes.indexOfFirst { it == null }
            if (lane < 0) {
                lane = lanes.size
                lanes.add(null)
            }
            lanes[lane] = commit.hash
        }
        val active = BooleanArray(lanes.size) { lanes[it] != null }
        val parentLanes = ArrayList<Int>()
        if (commit.parents.isEmpty()) {
            lanes[lane] = null
        } else {
            lanes[lane] = commit.parents[0]
            parentLanes.add(lane)
            for (parent in commit.parents.drop(1)) {
                var pl = lanes.indexOfFirst { it == parent }
                if (pl < 0) {
                    pl = lanes.indexOfFirst { it == null }
                    if (pl < 0) {
                        pl = lanes.size
                        lanes.add(parent)
                    } else {
                        lanes[pl] = parent
                    }
                }
                parentLanes.add(pl)
            }
        }
        while (lanes.isNotEmpty() && lanes.last() == null) lanes.removeAt(lanes.lastIndex)
        out.add(GraphRow(commit, lane, active, parentLanes))
    }
    return out
}

@Composable
private fun GraphCell(row: GraphRow) {
    val palette = listOf(
        Color(0xFF4C8DFF),
        Color(0xFF34A853),
        Color(0xFFE8A33D),
        Color(0xFFA162F7),
        Color(0xFFE56767),
        Color(0xFF2AA6A6),
        Color(0xFFB5739D),
        Color(0xFF7A8B99),
    )
    val lanes = row.activeBefore.size.coerceAtLeast(row.lane + 1).coerceAtMost(8)
    Canvas(Modifier.width((lanes * 12).dp).height(56.dp)) {
        val laneW = 12.dp.toPx()
        val cx = (row.lane.coerceAtMost(lanes - 1) + 0.5f) * laneW
        val cy = size.height / 2f
        for (i in 0 until lanes) {
            if (!row.activeBefore.getOrElse(i) { false }) continue
            val x = (i + 0.5f) * laneW
            drawLine(
                palette[i % palette.size].copy(alpha = 0.55f),
                Offset(x, 0f),
                Offset(x, size.height),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
        for (pl in row.parentLanes) {
            val x2 = (pl.coerceAtMost(lanes - 1) + 0.5f) * laneW
            drawLine(
                palette[row.lane % palette.size],
                Offset(cx, cy),
                Offset(x2, size.height),
                strokeWidth = 2.dp.toPx(),
            )
        }
        drawCircle(
            palette[row.lane % palette.size],
            radius = 4.dp.toPx(),
            center = Offset(cx, cy),
        )
    }
}

private fun fmtCommitTime(millis: Long): String {
    if (millis <= 0L) return ""
    return java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(millis))
}
