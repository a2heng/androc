package com.a2heng.androc.ui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a2heng.androc.ui.data.AppModel
import com.a2heng.androc.ui.data.ModelRef
import com.a2heng.androc.ui.data.SessionInfo
import com.a2heng.androc.ui.data.str

// ---------------------------------------------------------------------------
// 模型 / 模式卡片：聊天顶栏和设置页共用同一份数据和同一套样式。
// 两处都读 model.current（没有会话时读新会话默认），点开的是同一个选择页，
// 所以任一边改完，另一边立即同步。
// ---------------------------------------------------------------------------

/** 当前该显示的模型：会话自己的 > 最后一条 assistant 消息 > 新会话兜底。 */
fun resolvedModel(model: AppModel, session: SessionInfo? = model.current): ModelRef? =
    session?.model
        ?: model.messages.lastOrNull { it.model != null }?.model
        ?: model.fallbackModel()

/** 模型的展示名：优先服务端给的 name，其次原始 id；不再带 free/go 前缀。 */
fun resolvedModelLabel(model: AppModel, session: SessionInfo? = model.current): String {
    val ref = resolvedModel(model, session) ?: return "服务端默认"
    val name = model.models
        .firstOrNull { it.str("id") == ref.id && it.str("providerID") == ref.providerID }
        ?.str("name")
        ?.takeIf { it.isNotBlank() }
    return name ?: ref.label
}

/** 当前该显示的模式：会话选过的 > 新会话默认 > 服务端第一个 primary。 */
fun resolvedAgentId(model: AppModel, session: SessionInfo? = model.current): String =
    session?.agent?.ifBlank { null }
        ?: model.defaultAgent.ifBlank { null }
        ?: model.agents.firstOrNull { it.str("mode") == "primary" }?.str("name")
        ?: model.agents.firstOrNull()?.str("name")
        ?: "build"

/** 当前模型的来源短名：opencode 免费 / opencode go / opencode console / provider。 */
fun resolvedModelSource(model: AppModel, session: SessionInfo? = model.current): String {
    val ref = resolvedModel(model, session) ?: return "其它"
    return sourceLabel(model.modelGroupById["${ref.providerID}/${ref.id}"], ref.providerID)
}

@Composable
fun ModelControlCard(
    model: AppModel,
    modifier: Modifier = Modifier,
    session: SessionInfo? = model.current,
    hint: String? = null,
    fill: Boolean = true,
    onClick: () -> Unit,
) {
    // 第一行：「模型」后面跟来源（opencode 免费 / opencode go / opencode console）
    val source = resolvedModelSource(model, session)
    ControlCard("模型", resolvedModelLabel(model, session), modifier, hint ?: source, fill, onClick)
}

@Composable
fun AgentControlCard(
    model: AppModel,
    modifier: Modifier = Modifier,
    session: SessionInfo? = model.current,
    hint: String? = null,
    fill: Boolean = true,
    onClick: () -> Unit,
) {
    ControlCard("模式", agentTitle(resolvedAgentId(model, session)), modifier, hint, fill, onClick)
}

/** 两张卡片共用的外观：中强调底 + 深主题色文字 + 右箭头。 */
@Composable
private fun ControlCard(
    label: String,
    value: String,
    modifier: Modifier,
    hint: String?,
    fill: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AndrocColor.Medium)
            .clickableNoRipple(onClick)
            .padding(start = 10.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            // fill=false 时按内容宽度收拢（聊天顶栏的模式卡）；fill=true 时吃满剩余（模型卡 / 设置页）
            if (fill) Modifier.weight(1f) else Modifier,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                if (!hint.isNullOrBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        hint,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
            Text(
                value,
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AndrocColor.Deep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = AndrocColor.Deep.copy(alpha = 0.65f),
            modifier = Modifier.size(16.dp),
        )
    }
}
