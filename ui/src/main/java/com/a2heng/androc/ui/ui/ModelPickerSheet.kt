package com.a2heng.androc.ui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.a2heng.androc.ui.data.AppModel
import com.a2heng.androc.ui.data.ModelRef

/**
 * 模型选择：弹出的底部卡片（不是全屏页）。
 * 分组折叠：free → go → console；选中带档位的模型后就地展开「思考强度」。
 * 聊天页和设置页共用同一个卡片。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    model: AppModel,
    title: String,
    selected: ModelRef?,
    allowNone: Boolean = false,
    noneLabel: String = "不指定（跟随服务端）",
    onPick: (ModelRef?) -> Unit,
    onDismiss: () -> Unit,
) {
    val entries = remember(model.models) { modelEntries(model.models) }
    val selectedGroup = remember(entries, selected) {
        entries.firstOrNull {
            it.ref.id == selected?.id && it.ref.providerID == selected?.providerID
        }?.group
    }
    var expanded by remember(selectedGroup) { mutableStateOf(selectedGroup ?: 0) }
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
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (allowNone) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .background(AndrocColor.Medium, RoundedCornerShape(12.dp))
                        .clickable {
                            onPick(null)
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(noneLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }

            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                entries.groupBy { it.group }.forEach { (group, groupEntries) ->
                    item(key = "group-$group") {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 6.dp)
                                .background(AndrocColor.MediumStrong, RoundedCornerShape(10.dp))
                                .clickable { expanded = if (expanded == group) -1 else group }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modelGroupTitle(group),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = AndrocColor.Deep,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                groupEntries.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                if (expanded == group) Icons.Rounded.KeyboardArrowUp
                                else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    if (expanded == group) {
                        items(groupEntries, key = { "${it.providerID}/${it.ref.id}" }) { entry ->
                            val isCurrent = selected != null &&
                                entry.ref.id == selected.id &&
                                entry.ref.providerID == selected.providerID
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .background(
                                        if (isCurrent) AndrocColor.MediumStrong else AndrocColor.Medium,
                                        RoundedCornerShape(12.dp),
                                    ),
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!isCurrent) {
                                                // 选中带档位的模型后保持面板打开，底下会常驻它的思考强度
                                                onPick(entry.ref.copy(variant = null))
                                                if (entry.variants.isEmpty()) onDismiss()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            entry.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isCurrent) FontWeight.SemiBold
                                            else FontWeight.Normal,
                                            color = if (isCurrent) AndrocColor.Deep
                                            else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            buildString {
                                                append(entry.providerID).append(" · ").append(entry.ref.id)
                                                if (isCurrent) append(" · ").append(variantLabel(selected?.variant))
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (isCurrent) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "当前",
                                            tint = AndrocColor.Deep,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                // 只有被选中的模型才常驻展示它的思考强度档位
                                if (isCurrent && entry.variants.isNotEmpty()) {
                                    Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                                        Text(
                                            "思考强度 · ${variantLabel(selected?.variant)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            VariantChip(
                                                "默认",
                                                selected = selected?.variant == null,
                                            ) {
                                                onPick(entry.ref.copy(variant = null))
                                            }
                                            entry.variants.forEach { v ->
                                                VariantChip(
                                                    variantLabel(v),
                                                    selected = selected?.variant == v,
                                                ) {
                                                    onPick(entry.ref.copy(variant = v))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) AndrocColor.Deep else AndrocColor.Medium,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
