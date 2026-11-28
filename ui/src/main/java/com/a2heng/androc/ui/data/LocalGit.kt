package com.a2heng.androc.ui.data

/**
 * 本机 Git：直接执行 proot rootfs 里的 `/usr/bin/git`。
 *
 * 只有「连接的是本机服务」时才启用；远端仍然走 OpenCode 的 `/api/vcs`。
 * 这样本机 Git 不依赖服务端的 location/VCS 缓存，`git init`、单提交、
 * 工作区文件列表都以磁盘上的仓库为准。
 *
 * 宿主（app 模块）在启动时通过 [LocalGitHost.runner] 注入
 * `LocalRuntime.runInSandbox`。命令输出末尾带 `__ANDROC_EXIT:<code>`。
 */
object LocalGitHost {
    /** (shell 命令, 超时毫秒) -> 命令输出（含退出码标记行）。未接入时为 null。 */
    var runner: ((String, Int) -> String)? = null
}

data class LocalGitSnapshot(
    val info: VcsInfo,
    val status: List<VcsFileStatus>,
    val diff: List<VcsDiffFile>,
)

object LocalGit {
    private const val EXIT_MARK = "__ANDROC_EXIT:"
    private const val SEP = "@@ANDROC@@"
    // 注意：这里必须用普通字符串，\\n 才会被 printf 解释成换行；
    // 用 raw string 会把 "\\n" 原样输出，导致分段全是字面量 \\n。
    private const val SEP_CMD = "; printf '\\n@@ANDROC@@\\n'; "

    /** 最近一次命令的原始输出；本机 Git 异常时用于页面诊断。 */
    var lastOutput: String? = null
        private set

    fun available(): Boolean = LocalGitHost.runner != null

    private fun quote(path: String): String = "'" + path.replace("'", "'\\''") + "'"

    private fun exec(dir: String, body: String, timeoutMs: Int = 60_000): String {
        val runner = LocalGitHost.runner ?: error("本机 Git 不可用")
        val script = "cd -- ${quote(dir)} 2>/dev/null || " +
            "{ printf '\\n${EXIT_MARK}97\\n'; exit 97; }; $body; " +
            "printf '\\n$EXIT_MARK%s\\n' \$?"
        val raw = runner(script, timeoutMs)
        val idx = raw.lastIndexOf(EXIT_MARK)
        if (idx < 0) lastOutput = raw.take(1200)
        val out = if (idx >= 0) raw.substring(0, idx) else raw
        val code = if (idx >= 0) {
            raw.substring(idx + EXIT_MARK.length)
                .lineSequence().firstOrNull()?.trim()?.toIntOrNull()
        } else {
            null
        }
        if (code == 97) error("目录不存在：$dir")
        return out
    }

    private fun git(dir: String, args: String): String =
        "git -c safe.directory='*' -C ${quote(dir)} $args 2>/dev/null"

    private fun snapshotScript(dir: String, mode: String): String = buildString {
        append(git(dir, "rev-parse --is-inside-work-tree"))
        append(SEP_CMD)
        append(git(dir, "symbolic-ref --short HEAD"))
        append(SEP_CMD)
        append(git(dir, "status --porcelain=v1 -z") + " | head -c 262144")
        append(SEP_CMD)
        append(git(dir, "diff HEAD --numstat") + " | head -c 262144")
        append(SEP_CMD)
        if (mode == "committed") {
            append(git(dir, "diff HEAD~1 HEAD --no-color -U3 --") + " | head -c 1000000")
        } else {
            append("if ${git(dir, "rev-parse --verify --quiet HEAD")} >/dev/null 2>&1; then ")
            append(git(dir, "diff HEAD --no-color -U3 --") + " | head -c 1000000; ")
            append("else " + git(dir, "diff --no-color -U3 --") + " | head -c 1000000; fi")
            append(SEP_CMD)
            // 未跟踪文件也给出补丁；最多 40 个，避免 HOME 下的大目录拖死
            append("n=0; ${git(dir, "ls-files --others --exclude-standard")} | ")
            append("while IFS= read -r f; do ")
            append("n=\$((n+1)); [ \"\$n\" -gt 40 ] && break; ")
            append(git(dir, "diff --no-index --no-color -U3 -- /dev/null \"\$f\"") + "; ")
            append("done")
        }
    }

    fun snapshot(dir: String, mode: String): LocalGitSnapshot {
        val raw = exec(dir, snapshotScript(dir, mode))
        val parts = raw.split(SEP)
        fun part(index: Int): String =
            parts.getOrElse(index) { "" }.trim('\n')

        // proot / shell 可能先在 stderr 打提示；取最后一行有效输出
        val inside = part(0).lineSequence().lastOrNull { it.isNotBlank() }?.trim().orEmpty()
        if (inside != "true") {
            lastOutput = diagnose(dir).take(1600)
            return LocalGitSnapshot(VcsInfo(), emptyList(), emptyList())
        }
        lastOutput = null
        val branch = part(1).lineSequence().lastOrNull { it.isNotBlank() }?.trim().orEmpty()
        val stats = parseNumstat(part(3))
        val status = parseStatus(part(2)).map { item ->
            val (add, del) = stats[item.file] ?: (0 to 0)
            item.copy(additions = add, deletions = del)
        }
        val diff = if (mode == "committed") {
            parseDiff(part(4), "modified")
        } else {
            parseDiff(part(4), "modified") + parseDiff(part(5), "added")
        }
        return LocalGitSnapshot(
            info = VcsInfo(provider = "git", branch = VcsBranch(current = branch)),
            status = status,
            diff = diff,
        )
    }

    /** 检测失败时跑一条诊断命令：git 是否存在、当前目录、.git 是否存在。 */
    private fun diagnose(dir: String): String = runCatching {
        exec(
            dir,
            "git -c safe.directory='*' -C ${quote(dir)} rev-parse --is-inside-work-tree 2>&1; " +
                "echo rev_exit=\$?; " +
                "git -c safe.directory='*' -C ${quote(dir)} rev-parse --show-toplevel 2>&1; " +
                "git -c safe.directory='*' -C ${quote(dir)} rev-parse --git-dir 2>&1; " +
                "ls -ld . .git 2>&1; cat .git/HEAD 2>&1; " +
                "head -20 .git/config 2>&1; pwd; id; git --version",
            15_000,
        ).trim()
    }.getOrElse { it.message ?: it.toString() }

    // ---- 历史 / 分支 ---------------------------------------------------------

    data class Commit(
        val hash: String,
        val parents: List<String>,
        val author: String,
        val time: Long,
        val subject: String,
        val refs: List<String>,
    ) {
        val short: String get() = hash.take(7)
    }

    data class CommitFile(
        val path: String,
        val status: String,
        val additions: Int,
        val deletions: Int,
    )

    data class BranchInfo(
        val name: String,
        val current: Boolean,
        val remote: Boolean,
        val upstream: String,
    )

    fun log(dir: String, limit: Int = 150, ref: String? = null): List<Commit> {
        val target = if (ref.isNullOrBlank()) "--all" else quote(ref)
        val out = exec(
            dir,
            git(
                dir,
                "log $target --topo-order --date-order --max-count=$limit " +
                    "--pretty=format:%H%x1f%P%x1f%an%x1f%at%x1f%D%x1f%s%x1e",
            ),
            45_000,
        )
        return out.split('\u001e').mapNotNull { record ->
            val text = record.trim('\n')
            if (text.isBlank()) return@mapNotNull null
            val f = text.split('\u001f')
            if (f.size < 6) return@mapNotNull null
            Commit(
                hash = f[0].trim(),
                parents = f[1].split(' ').map { it.trim() }.filter { it.isNotBlank() },
                author = f[2].trim(),
                time = f[3].trim().toLongOrNull()?.times(1000L) ?: 0L,
                refs = f[4].split(',').map { it.trim() }.filter { it.isNotBlank() },
                subject = f[5].trim(),
            )
        }
    }

    fun commitFiles(dir: String, hash: String): List<CommitFile> {
        val names = exec(
            dir,
            git(dir, "show --name-status --format= --find-renames ${quote(hash)}"),
            30_000,
        )
        val stats = HashMap<String, Pair<Int, Int>>()
        exec(
            dir,
            git(dir, "show --numstat --format= ${quote(hash)}"),
            30_000,
        ).lineSequence().forEach { line ->
            val cols = line.split('\t')
            if (cols.size >= 3) {
                val add = cols[0].toIntOrNull() ?: 0
                val del = cols[1].toIntOrNull() ?: 0
                stats[cols[2]] = add to del
            }
        }
        val out = ArrayList<CommitFile>()
        names.lineSequence().forEach { line ->
            val cols = line.split('\t')
            if (cols.size >= 2) {
                val code = cols[0].trim()
                val path = cols.last().trim()
                if (path.isNotBlank()) {
                    val status = when {
                        code.startsWith("R") || code.startsWith("C") -> "renamed"
                        code.startsWith("A") -> "added"
                        code.startsWith("D") -> "deleted"
                        else -> "modified"
                    }
                    val (add, del) = stats[path] ?: (0 to 0)
                    out += CommitFile(path, status, add, del)
                }
            }
        }
        return out
    }

    fun commitDiff(dir: String, hash: String, file: String): String = exec(
        dir,
        git(dir, "show --no-color -U3 ${quote(hash)} -- ${quote(file)}") + " | head -c 1000000",
        45_000,
    )

    /** 上级目录里的仓库根（用于 workspace 是独立空仓库、历史在 home 的情况）。 */
    fun parentRepoRoot(dir: String): String? {
        val parent = dir.trimEnd('/').substringBeforeLast('/')
        if (parent.isBlank() || parent == dir) return null
        val out = exec(parent, git(parent, "rev-parse --show-toplevel"), 10_000)
        val root = out.lineSequence().map { it.trim() }.lastOrNull { it.isNotBlank() } ?: return null
        return root.takeIf { it != dir }
    }

    fun branches(dir: String): List<BranchInfo> {
        val out = exec(
            dir,
            git(
                dir,
                "for-each-ref --format='%(refname:short)\t%(HEAD)\t%(upstream:short)' " +
                    "refs/heads refs/remotes",
            ),
        )
        return out.lineSequence().mapNotNull { line ->
            val text = line.trim('\n')
            if (text.isBlank()) return@mapNotNull null
            val f = text.split('\t')
            val name = f.getOrNull(0)?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            BranchInfo(
                name = name,
                current = f.getOrNull(1)?.trim() == "*",
                remote = name.startsWith("origin/") || name.contains("/"),
                upstream = f.getOrNull(2)?.trim().orEmpty(),
            )
        }.toList()
    }

    fun tags(dir: String): List<String> = exec(dir, git(dir, "tag --list"))
        .lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()


    /** 在当前目录初始化仓库；幂等。 */
    fun init(dir: String) {
        val out = exec(
            dir,
            "git -c safe.directory='*' init >/dev/null 2>&1; " +
                "git -c safe.directory='*' -C ${quote(dir)} rev-parse --is-inside-work-tree 2>/dev/null",
        )
        if (!out.contains("true")) {
            error("git init 失败：" + (lastOutput ?: out.take(300)))
        }
    }

    private fun parseStatus(raw: String): List<VcsFileStatus> {
        if (raw.isBlank()) return emptyList()
        val tokens = raw.split('\u0000')
        val out = ArrayList<VcsFileStatus>()
        var i = 0
        while (i < tokens.size) {
            val entry = tokens[i]
            if (entry.length < 4) {
                i++
                continue
            }
            val xy = entry.substring(0, 2)
            val path = entry.substring(3)
            if (path.isNotBlank()) {
                val status = when {
                    xy == "??" -> "added"
                    xy.contains('R') || xy.contains('C') -> "renamed"
                    xy.contains('D') -> "deleted"
                    xy.contains('A') -> "added"
                    else -> "modified"
                }
                out += VcsFileStatus(file = path, additions = 0, deletions = 0, status = status)
            }
            i++
            // rename/copy 的下一段是旧路径
            if (xy.contains('R') || xy.contains('C')) i++
        }
        return out
    }

    private fun parseNumstat(raw: String): Map<String, Pair<Int, Int>> {
        if (raw.isBlank()) return emptyMap()
        val map = HashMap<String, Pair<Int, Int>>()
        raw.lineSequence().forEach { line ->
            val cols = line.split('\t')
            if (cols.size >= 3) {
                val add = cols[0].toIntOrNull() ?: 0
                val del = cols[1].toIntOrNull() ?: 0
                var file = cols[2]
                if (file.contains(" => ")) file = file.substringAfterLast(" => ")
                map[file] = add to del
            }
        }
        return map
    }

    private val plusHeader = Regex("""^\+\+\+ b/(.+)$""", RegexOption.MULTILINE)
    private val minusHeader = Regex("""^--- a/(.+)$""", RegexOption.MULTILINE)

    private fun parseDiff(raw: String, status: String): List<VcsDiffFile> {
        if (raw.isBlank()) return emptyList()
        val chunks = ArrayList<String>()
        var current: StringBuilder? = null
        raw.lineSequence().forEach { line ->
            if (line.startsWith("diff --git ")) {
                current?.let { chunks += it.toString() }
                current = StringBuilder()
            }
            current?.let { it.append(line).append('\n') }
        }
        current?.let { chunks += it.toString() }
        return chunks.mapNotNull { chunk ->
            val plus = plusHeader.find(chunk)?.groupValues?.get(1)
            val minus = minusHeader.find(chunk)?.groupValues?.get(1)
            val file = when {
                !plus.isNullOrBlank() && plus != "/dev/null" -> plus
                !minus.isNullOrBlank() && minus != "/dev/null" -> minus
                else -> return@mapNotNull null
            }
            val additions = chunk.lineSequence()
                .count { it.startsWith("+") && !it.startsWith("+++") }
            val deletions = chunk.lineSequence()
                .count { it.startsWith("-") && !it.startsWith("---") }
            VcsDiffFile(
                file = file,
                patch = chunk,
                additions = additions,
                deletions = deletions,
                status = status,
            )
        }
    }
}
