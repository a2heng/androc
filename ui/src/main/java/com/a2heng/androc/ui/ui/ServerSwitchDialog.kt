package com.a2heng.androc.ui.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.a2heng.androc.ui.data.AppModel

/** 多服务器快速切换。 */
@Composable
fun ServerSwitchDialog(model: AppModel, onDismiss: () -> Unit) {
    val colors = CouiTheme.colors
    CouiDialog("切换服务器", onDismiss = onDismiss) {
        if (model.savedConnections.isEmpty()) {
            CouiText(
                "还没有保存的服务器。",
                style = CouiTheme.type.body,
                color = colors.onSurfaceVariant,
            )
        } else {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                model.savedConnectionsSorted().forEach { saved ->
                    val active = saved.url == model.serverUrl
                    CouiCard(
                        Modifier.fillMaxWidth(),
                        onClick = {
                            onDismiss()
                            model.switchTo(saved)
                        },
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                CouiText(hostLabel(saved.url), style = CouiTheme.type.body, maxLines = 1)
                                CouiText(
                                    saved.url,
                                    style = CouiTheme.type.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            if (active) {
                                Spacer(Modifier.width(8.dp))
                                CouiText(
                                    "当前",
                                    style = CouiTheme.type.labelSmall,
                                    color = colors.accent,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            CouiTextButton("关闭") { onDismiss() }
            Spacer(Modifier.width(4.dp))
            CouiTextButton("打开登录页") {
                onDismiss()
                model.disconnect()
            }
        }
    }
}

private fun hostLabel(url: String): String {
    val host = url.removePrefix("https://").removePrefix("http://").trimEnd('/')
    return if (host.startsWith("127.0.0.1") || host.startsWith("localhost")) {
        "本机 · $host"
    } else {
        host
    }
}
