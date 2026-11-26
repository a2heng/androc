package com.a2heng.androc.ui.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.CheckCircle
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
import com.a2heng.androc.ui.data.AppModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(model: AppModel) {
    var showModelPicker by remember { mutableStateOf(false) }
    var showAgentPicker by remember { mutableStateOf(false) }
    var showServers by remember { mutableStateOf(false) }
    var showProviderConnect by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        model.loadMeta()
    }
    // Go 额度只对 opencode-go 模型显示；选到其它模型时不占位置。
    val goModel = resolvedModel(model)?.providerID == "opencode-go"
    LaunchedEffect(goModel, model.serverUrl) {
        if (goModel) model.loadGoUsage()
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
        CouiSectionTitle("连接")
        Card(
            colors = CardDefaults.cardColors(containerColor = AndrocColor.Medium),
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
                PillAction("管理", Icons.Rounded.Dns) { showServers = true }
            }
        }

        Spacer(Modifier.height(8.dp))
        CouiSectionTitle("模型与模式")
        // 与聊天顶栏同一套卡片、同一份数据；来源就在卡片第一行「模型」后面。
        ModelControlCard(
            model = model,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                model.loadMeta()
                showModelPicker = true
            },
        )
        AgentControlCard(
            model = model,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                model.loadMeta()
                showAgentPicker = true
            },
        )
        if (goModel) {
            Spacer(Modifier.height(4.dp))
            GoUsageCard(model)
        }


        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = AndrocColor.Medium),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickableNoRipple { showProviderConnect = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "连接 Provider / API",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "API Key、OAuth、环境变量；连接后模型才会出现",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        CouiSectionTitle("外观")
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AppearanceSwatch("跟随系统", "system", model.appearance == "system") { model.updateAppearance("system") }
            AppearanceSwatch("浅色", "light", model.appearance == "light") { model.updateAppearance("light") }
            AppearanceSwatch("深色", "dark", model.appearance == "dark") { model.updateAppearance("dark") }
        }

        Spacer(Modifier.height(10.dp))
        CouiSectionTitle("主题色")
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
        CouiSectionTitle("运行中发送消息时")
        TripleOptionRow(
            titles = listOf("steer", "queue", "中断后发送"),
            subtitles = listOf("下一步前插入", "本轮结束后", "先中断再发"),
            selected = when (model.sendMode) {
                "queue" -> 1
                "interrupt" -> 2
                else -> 0
            },
            onSelect = {
                model.updateSendMode(
                    when (it) {
                        1 -> "queue"
                        2 -> "interrupt"
                        else -> "steer"
                    },
                )
            },
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        CouiSectionTitle("聊天显示")
        TripleOptionRow(
            titles = listOf("精简", "适中", "完整"),
            subtitles = listOf("一行 + 气泡", "预览 + 展开", "全部平铺"),
            selected = when (model.detailLevel) {
                "full" -> 1
                "expanded" -> 2
                else -> 0
            },
            onSelect = {
                model.updateDetailLevel(
                    when (it) {
                        1 -> "full"
                        2 -> "expanded"
                        else -> "compact"
                    },
                )
            },
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        CouiSectionTitle("更新")
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
        CouiSectionTitle("关于")
        Text(
            "Androc v${model.appVersion}（内置 Androc UI，MIT）\n" +
                "数据全部来自局域网 opencode 服务，App 只在内存里做聚合。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))
    }

        if (showModelPicker) {
            ModelPickerSheet(
                model = model,
                title = if (model.current != null) "当前会话模型" else "最近使用 / 新会话模型",
                selected = resolvedModel(model, model.current),
                allowNone = false,
                onPick = { ref -> if (ref != null) model.chooseModel(ref) },
                onDismiss = { showModelPicker = false },
            )
        }
        if (showAgentPicker) {
            AgentPickerSheet(
                agents = agentOptions(model.agents),
                selectedId = resolvedAgentId(model, model.current),
                onPick = { id ->
                    model.chooseAgent(id)
                    showAgentPicker = false
                },
                onDismiss = { showAgentPicker = false },
            )
        }
        if (showServers) {
            ServerSwitchDialog(model) { showServers = false }
        }
        if (showProviderConnect) {
            ProviderConnectDialog(model) { showProviderConnect = false }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    CouiCard(onClick = onClick) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                CouiText(title, style = CouiTheme.type.body)
                CouiText(
                    subtitle,
                    style = CouiTheme.type.bodySmall,
                    color = CouiTheme.colors.onSurfaceVariant,
                )
            }
            CouiGlyph(CouiIcon.ChevronRight, tint = CouiTheme.colors.onSurfaceVariant, size = 18)
        }
    }
}


@Composable
private fun PillAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        color = if (danger) MaterialTheme.colorScheme.errorContainer
        else AndrocColor.Medium,
        contentColor = if (danger) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        modifier = Modifier.clickableNoRipple(onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TripleOptionRow(
    titles: List<String>,
    subtitles: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        titles.forEachIndexed { index, title ->
            val active = index == selected
            val bg by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primary
                else AndrocColor.Medium,
                label = "option-bg",
            )
            val fg by animateColorAsState(
                if (active) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "option-fg",
            )
            Surface(
                color = bg,
                contentColor = fg,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = if (active) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .clickableNoRipple { onSelect(index) },
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (active) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Text(
                        subtitles.getOrElse(index) { "" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
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
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    CircleShape,
                )
                .padding(if (selected) 3.dp else 0.dp)
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
    val brush = remember(key, dark) { when (key) {
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
    } }
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
                    if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    CircleShape,
                )
                .padding(if (selected) 3.dp else 0.dp)
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

@Composable
private fun CouiSectionTitle(text: String) {
    CouiText(
        text,
        style = CouiTheme.type.labelSmall,
        color = CouiTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}
