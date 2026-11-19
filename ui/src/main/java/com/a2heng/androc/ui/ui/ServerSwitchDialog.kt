package com.a2heng.androc.ui.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.a2heng.androc.ui.data.AppModel

/**
 * 多服务器快速切换。会话页和设置页共用；点一行就断开当前连接并连到目标，
 * 不会把两个服务器的会话/流混在一起。
 */
@Composable
fun ServerSwitchDialog(model: AppModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("切换服务器") },
        text = {
            if (model.savedConnections.isEmpty()) {
                Text(
                    "还没有保存的服务器。点「打开登录页」输入地址和密码。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    model.savedConnectionsSorted().forEach { saved ->
                        val active = saved.url == model.serverUrl
                        Surface(
                            color = if (active) {
                                AndrocColor.MediumStrong
                            } else {
                                AndrocColor.Medium
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    model.switchTo(saved)
                                },
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        hostLabel(saved.url),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        saved.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (active) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "当前",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                model.disconnect()
            }) { Text("打开登录页") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

private fun hostLabel(url: String): String {
    val host = url.removePrefix("https://").removePrefix("http://").trimEnd('/')
    return if (host.startsWith("127.0.0.1") || host.startsWith("localhost")) {
        "本机 · $host"
    } else {
        host
    }
}
