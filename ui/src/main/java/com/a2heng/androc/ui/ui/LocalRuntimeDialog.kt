package com.a2heng.androc.ui.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.a2heng.androc.market.MarketClient
import com.a2heng.androc.ui.data.AppModel
import com.a2heng.androc.ui.data.LocalRuntimeHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One dialog shared by the login screen and Settings.
 *
 * Local OpenCode is just another server at 127.0.0.1: the component owns the
 * process and the password, and the UI receives the same URL+password pair it
 * would type for a remote server. The user never sees those values.
 */
@Composable
fun LocalRuntimeDialog(model: AppModel, onDismiss: () -> Unit) {
    val launcher = LocalRuntimeHost.launcher
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var installedVersion by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf(0f) }

    fun install() {
        busy = true
        error = null
        progress = 0f
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    var last = 0f
                    MarketClient().installOpencode(context.filesDir) { done, total ->
                        if (total > 0) {
                            val p = (done.toFloat() / total).coerceIn(0f, 1f)
                            if (p - last > 0.01f || p >= 1f) {
                                last = p
                                scope.launch { progress = p }
                            }
                        }
                    }
                }
                installedVersion = file.parentFile?.name
                progress = 1f
            } catch (e: Exception) {
                error = e.message ?: e.toString()
            } finally {
                busy = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("本机 OpenCode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (launcher == null || !launcher.installed) {
                    if (installedVersion != null) {
                        Text(
                            "OpenCode 组件已安装 · $installedVersion",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "运行时启动器接入后，这里会直接启动本机服务并自动连接。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "组件未安装。安装后由 App 启动本机服务，地址和密码自动注入，不需要手动填写。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "OpenCode 组件 ≈ 89 MB 下载 / 247 MB 安装；从官方 release 检查更新。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (busy) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "下载/校验中 ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        "组件已安装" + (launcher.version?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "启动后会自动连接 127.0.0.1 上的本机服务。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(8.dp))
                        Text("正在启动本机服务…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            if (launcher != null && launcher.installed) {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        error = null
                        launcher.start(
                            { url, password ->
                                busy = false
                                model.connect(url, password, remember = false)
                                onDismiss()
                            },
                            { message ->
                                busy = false
                                error = message
                            },
                        )
                    },
                ) { Text("启动并连接") }
            } else {
                TextButton(
                    enabled = !busy && installedVersion == null,
                    onClick = { install() },
                ) { Text(if (installedVersion == null) "安装组件" else "已安装") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}
