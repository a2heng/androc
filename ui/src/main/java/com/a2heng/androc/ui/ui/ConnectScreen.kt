package com.a2heng.androc.ui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.a2heng.androc.ui.data.AppBuild
import com.a2heng.androc.ui.data.AppModel

@Composable
fun ConnectScreen(model: AppModel) {
    val initial = remember { splitServerUrl(model.serverUrl) }
    var scheme by rememberSaveable { mutableStateOf(initial.first) }
    var host by rememberSaveable { mutableStateOf(initial.second) }
    var port by rememberSaveable { mutableStateOf(initial.third) }
    var password by rememberSaveable { mutableStateOf(model.password) }
    var showPassword by remember { mutableStateOf(false) }
    var showSavedDialog by remember { mutableStateOf(false) }
    var showLocalRuntime by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val url = composeServerUrl(scheme, host, port)
    val colors = CouiTheme.colors
    val type = CouiTheme.type

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                CouiGlyph(CouiIcon.Terminal, tint = colors.onAccent, size = 30)
            }
            Spacer(Modifier.height(18.dp))
            CouiText("Androc", style = type.headline)
            Spacer(Modifier.height(6.dp))
            CouiText(
                "连接局域网里的 opencode 服务：会话、文件与终端。",
                style = type.body,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            if (!AppBuild.thin) {
                CouiCard {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            CouiText("在这台手机上运行 OpenCode", style = type.body)
                            CouiText(
                                "内置 OpenCode 与运行时",
                                style = type.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        CouiTextButton("本机") { showLocalRuntime = true }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CouiText(
                    "服务器地址",
                    style = type.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (model.savedConnections.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CouiText(
                            "${model.savedConnections.size}",
                            style = type.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        CouiIconButton(CouiIcon.Server) { showSavedDialog = true }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                CouiSelect(
                    value = scheme,
                    options = listOf("http", "https"),
                    onSelect = {
                        scheme = it
                        if (model.connecting || model.connectionError != null) model.cancelConnect()
                    },
                    label = "协议",
                    modifier = Modifier.width(84.dp),
                )
                Spacer(Modifier.width(8.dp))
                CouiField(
                    value = host,
                    onValueChange = {
                        if (model.connecting || model.connectionError != null) model.cancelConnect()
                        host = it
                        model.savedConnectionFor(composeServerUrl(scheme, it, port))
                            ?.let { saved -> password = saved.password }
                    },
                    label = "主机 / IP",
                    placeholder = "192.168.1.100",
                    keyboardType = KeyboardType.Uri,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                CouiField(
                    value = port,
                    onValueChange = {
                        if (model.connecting || model.connectionError != null) model.cancelConnect()
                        port = it.filter { ch -> ch.isDigit() }.take(5)
                    },
                    label = "端口",
                    placeholder = "4096",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.width(76.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            CouiField(
                value = password,
                onValueChange = {
                    if (model.connecting || model.connectionError != null) model.cancelConnect()
                    password = it
                },
                label = "密码",
                placeholder = "服务器密码",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailing = {
                    CouiTextButton(if (showPassword) "隐藏" else "显示") {
                        showPassword = !showPassword
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            if (model.connectionError != null) {
                Spacer(Modifier.height(10.dp))
                CouiText(
                    model.connectionError ?: "",
                    style = type.bodySmall,
                    color = colors.error,
                )
            }

            Spacer(Modifier.height(18.dp))
            CouiPrimaryButton(
                text = if (model.connecting) "取消连接" else "连接",
                onClick = {
                    if (model.connecting) model.cancelConnect() else model.connectSmart(url, password)
                },
                icon = {
                    if (model.connecting) {
                        CouiSpinner(size = 16, color = colors.onAccent)
                    } else {
                        CouiGlyph(CouiIcon.CheckCircle, tint = colors.onAccent)
                    }
                },
            )
            Spacer(Modifier.height(24.dp))
            CouiText(
                "手机与电脑在同一局域网；服务端用 --hostname 0.0.0.0 启动。opencode serve 默认 http://，TLS 才选 https。",
                style = type.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }

    if (showSavedDialog) {
        CouiDialog("已保存的连接", onDismiss = { showSavedDialog = false }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                model.savedConnectionsSorted().forEach { saved ->
                    CouiCard(
                        onClick = {
                            val parts = splitServerUrl(saved.url)
                            scheme = parts.first
                            host = parts.second
                            port = parts.third
                            password = saved.password
                            showSavedDialog = false
                            if (model.connecting || model.connectionError != null) {
                                model.cancelConnect()
                            }
                        },
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                CouiText(prettyServer(saved.url), style = type.body, maxLines = 1)
                                CouiText(
                                    if (saved.password.isBlank()) "无密码" else "已保存密码",
                                    style = type.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            CouiIconButton(
                                icon = CouiIcon.Delete,
                                tint = colors.error,
                            ) { pendingDelete = saved.url }
                        }
                    }
                }
            }
        }
    }
    if (pendingDelete != null) {
        CouiDialog("删除这个连接？", onDismiss = { pendingDelete = null }) {
            CouiText(
                prettyServer(pendingDelete ?: ""),
                style = type.body,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CouiTextButton("取消") { pendingDelete = null }
                Spacer(Modifier.width(8.dp))
                CouiTextButton("删除", color = colors.error) {
                    pendingDelete?.let { model.forgetConnection(it) }
                    pendingDelete = null
                }
            }
        }
    }
    if (showLocalRuntime) {
        LocalRuntimeDialog(model) { showLocalRuntime = false }
    }
}

/** "http://host:port" → (scheme, host, port)。 */
private fun splitServerUrl(raw: String): Triple<String, String, String> {
    val text = raw.trim()
    val scheme = if (text.startsWith("https://", ignoreCase = true)) "https" else "http"
    val rest = text.removePrefix("https://").removePrefix("http://").trimEnd('/')
    val host = rest.substringBefore(':')
    val port = rest.substringAfter(':', "").takeWhile { it.isDigit() }
    return Triple(scheme, host, port)
}

/** 三个输入合回一个 URL；主机为空时返回空串。 */
private fun composeServerUrl(scheme: String, host: String, port: String): String {
    val h = host.trim()
    if (h.isEmpty()) return ""
    return if (port.isBlank()) "$scheme://$h" else "$scheme://$h:${port.trim()}"
}

private fun prettyServer(url: String): String =
    url.removePrefix("http://").removePrefix("https://").trimEnd('/')
