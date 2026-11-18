package com.a2heng.androc.ui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.androc.contracts.PluginInfo
import com.a2heng.androc.ui.data.AppModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(model: AppModel) {
    var showModelPicker by remember { mutableStateOf(false) }
    var showServers by remember { mutableStateOf(false) }
    var removeId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        model.loadMeta()
        model.refreshPlugins()
    }
    // 选择页必须放在滚动 Column 外面：verticalScroll 给子项无限高度，
    // fillMaxSize 的选择页会被压成 0 高度，表现就是「点默认模型没反应」。
    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("连接", style = MaterialTheme.typography.titleSmall)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LabelValue("服务器", model.serverUrl)
                    LabelValue("服务端版本", if (model.serverVersion.isBlank()) "未知" else "v${model.serverVersion}")
                    LabelValue("认证", if (model.password.isBlank()) "无密码" else "Basic · opencode")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showServers = true }) {
                    Icon(
                        Icons.Rounded.Dns,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("切换")
                }
                TextButton(onClick = { model.disconnect() }) {
                    Icon(
                        Icons.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("断开连接", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("插件", style = MaterialTheme.typography.titleSmall)
        if (!model.isLocalServer()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "当前连接的是远程服务器。插件只装在本机，切回 127.0.0.1 后才能安装、更新或卸载。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (model.plugins.isEmpty()) {
                        Text(
                            "还没有插件信息。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        model.plugins.forEach { plugin ->
                            PluginCard(model, plugin) { removeId = it }
                        }
                    }
                    Text(
                        "插件 = Skill + 它需要的工具链。卸载只删插件本身，不动项目、会话和内核。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("默认模型", style = MaterialTheme.typography.titleSmall)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickableNoRipple {
                        model.loadMeta()
                        showModelPicker = true
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("选择模型", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        model.serverDefaultModel?.let {
                            "服务器默认 · " + modelDisplay(it, model.modelGroupById) +
                                " · " + variantLabel(it.variant)
                        } ?: "服务器默认（未配置）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("外观", style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AppearanceSwatch("跟随系统", "system", model.appearance == "system") { model.updateAppearance("system") }
            AppearanceSwatch("浅色", "light", model.appearance == "light") { model.updateAppearance("light") }
            AppearanceSwatch("深色", "dark", model.appearance == "dark") { model.updateAppearance("dark") }
        }

        Spacer(Modifier.height(10.dp))
        Text("主题色", style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            themePresetKeys().forEach { key ->
                ThemeSwatch(key, selected = model.themeMode == key) { model.updateThemeMode(key) }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("运行中发送消息时", style = MaterialTheme.typography.titleSmall)
        OptionRow(
            title = "steer（默认）",
            subtitle = "在下一个步骤前插入，引导当前这轮回答；opencode 默认行为",
            selected = model.sendMode == "steer",
        ) { model.updateSendMode("steer") }
        OptionRow(
            title = "queue",
            subtitle = "不打断当前回答；这轮结束后作为下一轮输入",
            selected = model.sendMode == "queue",
        ) { model.updateSendMode("queue") }
        OptionRow(
            title = "中断后发送",
            subtitle = "先中断当前回答再发送；本 App 的客户端行为，不是 opencode delivery",
            selected = model.sendMode == "interrupt",
        ) { model.updateSendMode("interrupt") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("聊天显示", style = MaterialTheme.typography.titleSmall)
        OptionRow(
            title = "精简",
            subtitle = "工具压成一行，点一下用气泡看完整内容",
            selected = model.detailLevel == "compact",
        ) { model.updateDetailLevel("compact") }
        OptionRow(
            title = "适中",
            subtitle = "工具/思考先预览一部分，超出点「展开」看完整",
            selected = model.detailLevel == "full",
        ) { model.updateDetailLevel("full") }
        OptionRow(
            title = "完整",
            subtitle = "全部平铺，不再折叠或展开",
            selected = model.detailLevel == "expanded",
        ) { model.updateDetailLevel("expanded") }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("更新", style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier
                .fillMaxWidth()
                .clickableNoRipple { model.checkUpdate() }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("检查更新", style = MaterialTheme.typography.bodyMedium)
                Text(
                    model.updateMessage ?: "当前 v${model.appVersion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (model.checkingUpdate) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        val info = model.updateInfo
        if (info != null && com.a2heng.androc.ui.data.UpdateClient.isNewer(model.appVersion, info.tag)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickableNoRipple { model.downloadUpdate() }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("下载并安装 ${info.tag}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (model.downloadingUpdate) "下载中 ${model.updateProgress}%"
                        else "APK 来自 GitHub Release（${info.tag}）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (model.downloadingUpdate) {
                    androidx.compose.material3.CircularProgressIndicator(
                        progress = { model.updateProgress / 100f },
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("关于", style = MaterialTheme.typography.titleSmall)
        Text(
            "Androc v${model.appVersion}（内置 Androc UI，MIT）\n" +
                "数据全部来自局域网 opencode 服务，App 只在内存里做聚合。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
    }

        PickerPageHost(visible = showModelPicker, onBack = { showModelPicker = false }) {
            ModelPickerPage(
                model = model,
                title = "服务器默认模型",
                selected = model.serverDefaultModel,
                allowNone = false,
                onPick = { ref -> model.updateServerDefaultModel(ref) },
                onDismiss = { showModelPicker = false },
            )
        }
        if (showServers) {
            ServerSwitchDialog(model) { showServers = false }
        }
        removeId?.let { id ->
            val plugin = model.plugins.firstOrNull { it.id() == id }
            AlertDialog(
                onDismissRequest = { removeId = null },
                title = { Text("卸载${plugin?.name() ?: "插件"}？") },
                text = {
                    Text("会删除这个插件的 skills 和工具链；项目、会话和内核不受影响。")
                },
                confirmButton = {
                    TextButton(onClick = {
                        removeId = null
                        model.uninstallPlugin(id)
                    }) {
                        Text("卸载", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { removeId = null }) { Text("取消") }
                },
            )
        }
    }
}

@Composable
private fun PluginCard(
    model: AppModel,
    plugin: PluginInfo,
    onUninstall: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        if (plugin.installed()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        CircleShape,
                    ),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    plugin.name(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    plugin.summary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                when {
                    plugin.updateAvailable() -> "可更新"
                    !plugin.installed() -> "未安装"
                    else -> "已安装"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (plugin.installed()) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val meta = buildList {
            if (plugin.version().isNotBlank()) add("v${plugin.version()}")
            if (plugin.sizeText().isNotBlank()) add(plugin.sizeText())
            if (plugin.bundled()) add("内置") else if (!plugin.installed()) add("需下载")
            if (plugin.updateAvailable() && plugin.latestVersion().isNotBlank()) {
                add("→ ${plugin.latestVersion()}")
            }
        }.joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(
                meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (plugin.skills().isNotEmpty()) {
            Text(
                "技能: " + plugin.skills().joinToString(", "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (plugin.toolchain().isNotEmpty()) {
            Text(
                "工具链: " + plugin.toolchain().joinToString(", "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                model.pluginBusy == plugin.id() -> {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                }
                !plugin.installed() -> {
                    TextButton(onClick = { model.installPlugin(plugin.id()) }) {
                        Text("安装")
                    }
                }
                else -> {
                    TextButton(onClick = { model.checkPluginUpdates(plugin.id()) }) {
                        Text("检查更新")
                    }
                    if (plugin.updateAvailable()) {
                        TextButton(onClick = { model.updatePlugin(plugin.id()) }) {
                            Text("更新")
                        }
                    }
                    if (plugin.removable()) {
                        TextButton(onClick = { onUninstall(plugin.id()) }) {
                            Text("卸载", color = MaterialTheme.colorScheme.error)
                        }
                    } else if (plugin.id() != PluginInfo.OPENCODE) {
                        Text(
                            "随内核",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginRow(name: String, status: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    if (ok) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    CircleShape,
                ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            status,
            style = MaterialTheme.typography.labelSmall,
            color = if (ok) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick)
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppearanceSwatch(
    label: String,
    mode: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val brush = when (mode) {
        "light" -> Brush.linearGradient(listOf(Color.White, Color.White))
        "dark" -> Brush.linearGradient(listOf(Color(0xFF151B23), Color(0xFF151B23)))
        else -> Brush.linearGradient(listOf(Color.White, Color(0xFF151B23)))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(brush)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape,
                )
                .clickableNoRipple(onClick),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun ThemeSwatch(
    key: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val brush = when (key) {
        "dynamic" -> Brush.sweepGradient(
            listOf(
                Color(0xFF7AA2F7),
                Color(0xFFBB9AF7),
                Color(0xFFE0AF68),
                Color(0xFF7DCFFF),
                Color(0xFF7AA2F7),
            ),
        )
        "mono" -> Brush.linearGradient(listOf(Color.White, Color(0xFF151B23)))
        else -> Brush.linearGradient(listOf(themeAccentColor(key, dark), themeAccentColor(key, dark)))
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(50.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(brush)
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    CircleShape,
                )
                .clickableNoRipple(onClick),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            when (key) {
                "mono" -> "黑白"
                "dynamic" -> "系统"
                else -> themePresetLabel(key)
            },
            fontSize = 9.sp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
