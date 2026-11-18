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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.a2heng.androc.ui.data.AppModel
import com.a2heng.androc.ui.data.LocalRuntimeHost

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
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("本机 OpenCode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (launcher == null || !launcher.installed) {
                    Text(
                        "组件未安装。安装后由 App 启动本机服务，地址和密码自动注入，不需要手动填写。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "OpenCode 组件 ≈ 98 MB 下载 / 247 MB 安装；从官方 release 检查更新。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "市场接口已预留，组件包发布后可在这里一键安装。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                TextButton(enabled = false, onClick = {}) { Text("安装组件") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}
