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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.a2heng.androc.ui.data.AppModel

@Composable
fun ConnectScreen(model: AppModel) {
    val initial = remember { splitServerUrl(model.serverUrl) }
    var scheme by rememberSaveable { mutableStateOf(initial.first) }
    var host by rememberSaveable { mutableStateOf(initial.second) }
    var port by rememberSaveable { mutableStateOf(initial.third) }
    var schemeMenu by remember { mutableStateOf(false) }
    val url = composeServerUrl(scheme, host, port)
    var password by rememberSaveable { mutableStateOf(model.password) }
    var showPassword by remember { mutableStateOf(false) }
    var savedMenuOpen by remember { mutableStateOf(false) }
    var showLocalRuntime by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(AndrocColor.MediumStrong),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Androc", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "连接局域网里的 opencode 服务：会话、流式回答、缓存命中率、文件与终端。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(26.dp))

            if (!com.a2heng.androc.ui.data.AppBuild.thin) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AndrocColor.Medium,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("在这台手机上运行 OpenCode", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "内置 OpenCode 与运行时，地址和密码由 App 注入",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { showLocalRuntime = true }) { Text("本机") }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            Text(
                "服务器地址",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Box {
                    OutlinedTextField(
                        value = scheme,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("协议") },
                        trailingIcon = {
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier.width(112.dp),
                        shape = RoundedCornerShape(14.dp),
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .clickableNoRipple { schemeMenu = true },
                    )
                    DropdownMenu(
                        expanded = schemeMenu,
                        onDismissRequest = { schemeMenu = false },
                    ) {
                        listOf("http", "https").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    scheme = item
                                    schemeMenu = false
                                    if (model.connecting || model.connectionError != null) {
                                        model.cancelConnect()
                                    }
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = {
                        if (model.connecting || model.connectionError != null) model.cancelConnect()
                        host = it
                        model.savedConnectionFor(composeServerUrl(scheme, it, port))
                            ?.let { saved -> password = saved.password }
                    },
                    label = { Text("主机 / IP") },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                )
                Text(
                    ":",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        if (model.connecting || model.connectionError != null) model.cancelConnect()
                        port = it.filter { ch -> ch.isDigit() }.take(5)
                    },
                    label = { Text("端口") },
                    placeholder = { Text("4096") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(96.dp),
                    shape = RoundedCornerShape(14.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    if (model.savedConnections.isEmpty()) "协议 + 主机 + 端口，密码在下一行"
                    else "已保存 ${model.savedConnections.size} 个连接",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (model.savedConnections.isNotEmpty()) {
                    Box {
                        TextButton(onClick = { savedMenuOpen = true }) {
                            Text("选择已保存")
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = savedMenuOpen,
                            onDismissRequest = { savedMenuOpen = false },
                        ) {
                            model.savedConnectionsSorted().forEach { saved ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                prettyServer(saved.url),
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 210.dp),
                                            )
                                            Text(
                                                if (saved.password.isBlank()) "无密码" else "已保存密码",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { model.forgetConnection(saved.url) },
                                            modifier = Modifier.size(30.dp),
                                        ) {
                                            Icon(
                                                Icons.Rounded.DeleteOutline,
                                                contentDescription = "删除",
                                                modifier = Modifier.size(17.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = {
                                        val parts = splitServerUrl(saved.url)
                                        scheme = parts.first
                                        host = parts.second
                                        port = parts.third
                                        password = saved.password
                                        savedMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    if (model.connecting || model.connectionError != null) model.cancelConnect()
                    password = it
                },
                label = { Text("密码") },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                trailingIcon = {
                    Text(
                        if (showPassword) "隐藏" else "显示",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clickableNoRipple { showPassword = !showPassword },
                    )
                },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )

            if (model.connectionError != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    model.connectionError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (model.connecting) model.cancelConnect() else model.connectSmart(url, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (model.connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.size(10.dp))
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("取消连接")
                } else {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("连接")
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "提示：手机与电脑需在同一局域网；服务端启动参数 --hostname 0.0.0.0。opencode serve 默认是 http://（不启用 TLS），不要填 https://。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
    if (showLocalRuntime) {
        LocalRuntimeDialog(model) { showLocalRuntime = false }
    }
}

/** 保存列表里去掉协议前缀，只显示 host:port，省地方。 */
private fun prettyServer(url: String): String =
    url.removePrefix("http://").removePrefix("https://").trimEnd('/')

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
