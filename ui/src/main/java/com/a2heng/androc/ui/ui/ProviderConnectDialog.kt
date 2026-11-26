package com.a2heng.androc.ui.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
    // 已连接优先；其余按常用程度（市占/上游常用）排序，最后按名字。
    val integrations = remember(model.integrations) {
        orderedIntegrations(model.integrations)
    }

    LaunchedEffect(Unit) { model.loadIntegrations() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("连接 Provider") },
        text = {
            val connected = remember(integrations) { integrations.filter { it.connected } }
            val recommended = remember(integrations) {
                integrations.filter { !it.connected && PROVIDER_RANK.containsKey(it.id) }
            }
            val rest = remember(integrations) {
                integrations.filter { !it.connected && !PROVIDER_RANK.containsKey(it.id) }
            }
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (integrations.isEmpty()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                Modifier.height(16.dp).width(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("正在读取 Provider…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    if (connected.isNotEmpty()) {
                        item(key = "header-connected") { ProviderSection("已连接") }
                        items(connected, key = { it.id }) { info ->
                            ProviderRow(
                                info,
                                busy = model.integrationBusy == info.id,
                                onKey = { keyFor = info },
                                onOAuth = { startProviderOAuth(model, info) { oauth = it } },
                                onDisconnect = { model.disconnectIntegration(info) },
                            )
                        }
                    }
                    if (recommended.isNotEmpty()) {
                        item(key = "header-popular") { ProviderSection("常用") }
                        items(recommended, key = { it.id }) { info ->
                            ProviderRow(
                                info,
                                busy = model.integrationBusy == info.id,
                                onKey = { keyFor = info },
                                onOAuth = { startProviderOAuth(model, info) { oauth = it } },
                                onDisconnect = { model.disconnectIntegration(info) },
                            )
                        }
                    }
                    if (rest.isNotEmpty()) {
                        item(key = "header-all") { ProviderSection("全部") }
                        items(rest, key = { it.id }) { info ->
                            ProviderRow(
                                info,
                                busy = model.integrationBusy == info.id,
                                onKey = { keyFor = info },
                                onOAuth = { startProviderOAuth(model, info) { oauth = it } },
                                onDisconnect = { model.disconnectIntegration(info) },
                            )
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

@Composable
private fun ProviderSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun ProviderRow(
    info: IntegrationInfo,
    busy: Boolean,
    onKey: () -> Unit,
    onOAuth: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(providerLabel(info), style = MaterialTheme.typography.bodyMedium)
            Text(
                methodSummary(info),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        when {
            busy -> CircularProgressIndicator(
                Modifier.height(16.dp).width(16.dp),
                strokeWidth = 2.dp,
            )
            info.connected -> TextButton(onClick = onDisconnect) { Text("断开") }
            info.keyMethod != null -> TextButton(onClick = onKey) { Text("API Key") }
            info.oauthMethod != null -> TextButton(onClick = onOAuth) { Text("OAuth") }
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

private fun startProviderOAuth(
    model: AppModel,
    info: IntegrationInfo,
    onReady: (OAuthAttempt) -> Unit,
) {
    val method = info.oauthMethod ?: return
    model.startOAuthConnect(info, method) { attempt ->
        val id = attempt?.str("attemptID") ?: return@startOAuthConnect
        onReady(OAuthAttempt(info, attempt, id))
        model.pollOAuth(info, id) { ok, _ ->
            if (ok) model.loadIntegrations()
        }
    }
}

private fun methodSummary(info: IntegrationInfo): String {
    if (info.methods.isEmpty()) return "没有可用的连接方式"
    return info.methods.map { method ->
        when (method.type) {
            "key" -> "API Key"
            "oauth" -> "OAuth"
            "env" -> "环境变量 " + method.envNames.joinToString(", ")
            "command" -> "服务器命令"
            else -> method.type
        }
    }.distinct().joinToString(" · ")
}

/**
 * 常用 Provider 顺序（粗略按市占/开发者使用量，Top 10 之后补 OpenCode 生态与国内常用）。
 * 未列出的按名字排；已连接永远在最前。
 */
private val PROVIDER_PRIORITY = listOf(
    "openai",
    "anthropic",
    "google",
    "github-copilot",
    "openrouter",
    "xai",
    "mistral",
    "groq",
    "deepseek",
    "togetherai",
    "cerebras",
    "fireworks-ai",
    "amazon-bedrock",
    "azure",
    "opencode",
    "opencode-go",
    "zhipuai",
    "zai",
    "moonshotai",
    "moonshotai-cn",
    "alibaba",
    "alibaba-cn",
    "minimax",
    "minimax-cn",
    "volcengine",
    "siliconflow",
    "siliconflow-cn",
    "tencent-tokenhub",
    "modelscope",
    "stepfun",
    "sensenova",
    "ollama-cloud",
    "lmstudio",
    "nvidia",
    "cohere",
    "perplexity",
    "huggingface",
)

private val PROVIDER_RANK: Map<String, Int> =
    PROVIDER_PRIORITY.withIndex().associate { (index, id) -> id to index }

/** 国内/常用 Provider 的中文名；完整 i18n 后续再抽资源。 */
private val PROVIDER_ALIAS = mapOf(
    "alibaba" to "阿里云",
    "alibaba-cn" to "阿里云（国内）",
    "zhipuai" to "智谱 AI",
    "zai" to "智谱 Z.AI",
    "moonshotai" to "月之暗面",
    "moonshotai-cn" to "月之暗面（国内）",
    "minimax" to "MiniMax",
    "minimax-cn" to "MiniMax（国内）",
    "volcengine" to "火山引擎",
    "siliconflow" to "硅基流动",
    "siliconflow-cn" to "硅基流动（国内）",
    "modelscope" to "魔搭 ModelScope",
    "tencent-tokenhub" to "腾讯云 TokenHub",
    "stepfun" to "阶跃星辰",
    "sensenova" to "商汤日日新",
)

private fun providerLabel(info: IntegrationInfo): String {
    val alias = PROVIDER_ALIAS[info.id] ?: return info.name
    return if (info.name.equals(alias, ignoreCase = true)) alias else "$alias · ${info.name}"
}

private fun orderedIntegrations(list: List<IntegrationInfo>): List<IntegrationInfo> =
    list.sortedWith(
        compareBy<IntegrationInfo> { if (it.connected) 0 else 1 }
            .thenBy { PROVIDER_RANK[it.id] ?: Int.MAX_VALUE }
            .thenBy { it.name.lowercase() }
    )
