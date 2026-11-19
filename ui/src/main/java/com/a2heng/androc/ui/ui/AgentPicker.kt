package com.a2heng.androc.ui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.a2heng.androc.ui.data.agentDescription
import com.a2heng.androc.ui.data.agentLabel
import com.a2heng.androc.ui.data.str
import kotlinx.serialization.json.JsonObject

// ---------------------------------------------------------------------------
// Agent / 模式选择：弹出的底部卡片（不是全屏页），聊天顶栏和设置页共用。
// 服务端返回 build / plan / compaction / title / summary 这类英文 id，
// 展示时统一走 agentLabel 汉化。
// ---------------------------------------------------------------------------

/** 一条可选的 Agent（模式）。 */
data class AgentOption(
    val id: String,
    val title: String,
    val description: String,
)

/** 服务端 /api/agent 的原始 JSON → 可选项；id 缺失的条目忽略。 */
fun agentOptions(agents: List<JsonObject>): List<AgentOption> = agents.mapNotNull { a ->
    val id = a.str("id") ?: return@mapNotNull null
    val raw = a.str("name") ?: a.str("id") ?: id
    val desc = a.str("description").orEmpty()
    AgentOption(
        id = id,
        title = agentLabel(raw),
        description = agentDescription(id, desc).takeIf { it.isNotBlank() } ?: "",
    )
}

/** 一个 Agent id 的展示名（已汉化）。 */
fun agentTitle(id: String): String = agentLabel(id)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentPickerSheet(
    agents: List<AgentOption>,
    selectedId: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                "选择模式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (agents.isEmpty()) {
                Text(
                    "没有可用的模式",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                ) {
                    items(agents, key = { it.id }) { item ->
                        val selected = item.id == selectedId
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) AndrocColor.MediumStrong else AndrocColor.Medium,
                                    RoundedCornerShape(12.dp),
                                )
                                .clickableNoRipple { onPick(item.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) AndrocColor.Deep
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                                if (item.description.isNotBlank()) {
                                    Text(
                                        item.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "当前",
                                    tint = AndrocColor.Deep,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
