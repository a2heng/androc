package com.a2heng.androc.ui.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.a2heng.androc.ui.data.AppModel
import com.a2heng.androc.ui.data.IntegrationInfo
import com.a2heng.androc.ui.data.bool
import com.a2heng.androc.ui.data.str
import kotlinx.serialization.json.JsonObject

/**
 * 官方 /api/integration 的“连接 Provider”流程：
 * key 直接提交；oauth 打开浏览器并轮询 attempt；env 提示设置环境变量。
 * 和 /model、设置页的模型选择共用同一个入口。
 */
@Composable
fun ProviderConnectDialog(model: AppModel, onDismiss: () -> Unit) {
    var keyFor by remember { mutableStateOf<IntegrationInfo?>(null) }
    var oauth by remember { mutableStateOf<OAuthAttempt?>(null) }

    LaunchedEffect(Unit) { model.loadIntegrations() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("连接 Provider") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (model.integrations.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.height(16.dp).width(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("正在读取 Provider…", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    model.integrations.forEach { info ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(info.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    methodSummary(info),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            when {
                                info.connected -> Text(
                                    "已连接",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                info.keyMethod != null -> TextButton(onClick = { keyFor = info }) {
                                    Text("API Key")
                                }
                                info.oauthMethod != null -> TextButton(onClick = {
                                    model.startOAuthConnect(info, info.oauthMethod!!) { attempt ->
                                        val id = attempt?.str("attemptID") ?: return@startOAuthConnect
                                        oauth = OAuthAttempt(info, attempt, id)
                                        model.pollOAuth(info, id) { ok, _ ->
                                            if (ok) model.loadIntegrations()
                                        }
                                    }
                                }) { Text("OAuth") }
                                info.envMethod != null -> Text(
                                    "环境变量",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                else -> Text(
                                    "服务器端",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { model.loadIntegrations() }) { Text("刷新") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )

    keyFor?.let { info ->
        var key by remember(info.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { keyFor = null },
            title = { Text("${info.name} · API Key") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Key 只提交给当前连接的服务器，由服务器保存。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        singleLine = true,
                        label = { Text("API Key") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = key.isNotBlank() && model.integrationBusy == null,
                    onClick = {
                        model.connectIntegrationKey(info, key.trim())
                        keyFor = null
                    },
                ) { Text("连接") }
            },
            dismissButton = {
                TextButton(onClick = { keyFor = null }) { Text("取消") }
            },
        )
    }

    oauth?.let { state ->
        val context = LocalContext.current
        val url = state.attempt.str("url").orEmpty()
        val instructions = state.attempt.str("instructions").orEmpty()
        val paste = state.attempt.bool("paste") || state.attempt.str("mode") == "code"
        var code by remember(state.attemptID) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                model.cancelOAuth(state.info, state.attemptID)
                oauth = null
            },
            title = { Text("${state.info.name} · OAuth") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        instructions.ifBlank { "在浏览器完成授权，然后回到这里。" },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (url.isNotBlank()) {
                        Text(
                            url,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 3,
                        )
                    }
                    if (paste) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            singleLine = true,
                            label = { Text("授权码 / 回调 URL") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                Row {
                    if (paste) {
                        TextButton(
                            enabled = code.isNotBlank(),
                            onClick = {
                                model.completeOAuth(state.info, state.attemptID, code.trim())
                                oauth = null
                            },
                        ) { Text("提交") }
                    }
                    TextButton(onClick = {
                        if (url.isNotBlank()) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                    }) { Text("打开浏览器") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    model.cancelOAuth(state.info, state.attemptID)
                    oauth = null
                }) { Text("取消") }
            },
        )
    }
}

private data class OAuthAttempt(
    val info: IntegrationInfo,
    val attempt: JsonObject,
    val attemptID: String,
)

private fun methodSummary(info: IntegrationInfo): String {
    if (info.methods.isEmpty()) return "没有可用的连接方式"
    return info.methods.joinToString(" · ") { method ->
        when (method.type) {
            "key" -> "API Key"
            "oauth" -> "OAuth"
            "env" -> "环境变量 " + method.envNames.joinToString(", ")
            "command" -> "服务器命令"
            else -> method.type
        }
    }
}
