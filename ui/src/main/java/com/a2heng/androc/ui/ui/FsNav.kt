package com.a2heng.androc.ui.ui

import com.a2heng.androc.ui.data.isAbsolutePath
import com.a2heng.androc.ui.data.joinPath
import com.a2heng.androc.ui.data.normPath

/**
 * 文件浏览器唯一的路径规则：文件页和新建会话选择器共用。
 *
 * - Windows：顶是「此电脑」，盘符根再上一级进入此电脑；
 * - POSIX：顶是 "/"，home 只是快捷入口。
 */
object FsNav {
    const val DRIVES = "drives:/"

    private val drive = Regex("^[A-Za-z]:")
    private val driveRoot = Regex("^[A-Za-z]:$")

    fun isWindows(home: String): Boolean = drive.containsMatchIn(home)

    /** 上一级；返回 null 表示已到顶，按钮应禁用。 */
    fun up(current: String, home: String): String? {
        val win = isWindows(home)
        var cur = normPath(current).trimEnd('/')
        if (cur.isEmpty()) cur = if (win) DRIVES.trimEnd('/') else "/"
        if (cur == DRIVES.trimEnd('/')) return null
        // 相对路径补到 home。
        if (!isAbsolutePath(cur) && cur != DRIVES.trimEnd('/')) {
            val h = home.trimEnd('/')
            cur = if (h.isNotBlank()) joinPath(h, cur).trimEnd('/') else "/" + cur.trimStart('/')
        }
        if (driveRoot.matches(cur)) return if (win) DRIVES else null
        if (cur == "/") return null
        val parent = cur.substringBeforeLast('/', "")
        return when {
            parent.isEmpty() -> "/"
            driveRoot.matches(parent) -> "$parent/"
            else -> parent
        }
    }

    /** 当前目录下的条目路径；「此电脑」里的条目本身已经是绝对路径。 */
    fun join(root: String, entry: String): String =
        if (root.trimEnd('/') == DRIVES.trimEnd('/')) entry else joinPath(root, entry)

    fun isDrives(path: String): Boolean = path.trimEnd('/') == DRIVES.trimEnd('/')
}
