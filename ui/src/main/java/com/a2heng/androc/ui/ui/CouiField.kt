package com.a2heng.androc.ui.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * ColorOS 风格的输入框：小标签在上，内容区 12dp 内边距，1dp 描边。
 * Material 的 OutlinedTextField 内部左右 16dp + 浮动标签，窄屏会挤。
 */
@Composable
fun CouiField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(
            label,
            style = CouiTheme.type.labelSmall,
            color = CouiTheme.colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        val shape = RoundedCornerShape(14.dp)
        val border = if (focused) CouiTheme.colors.accent
        else CouiTheme.colors.outline
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = CouiTheme.type.body.copy(
                color = CouiTheme.colors.onSurface,
            ),
            cursorBrush = SolidColor(CouiTheme.colors.accent),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(CouiTheme.colors.surface, shape)
                .border(1.dp, border, shape)
                .padding(horizontal = 12.dp),
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = CouiTheme.type.body,
                                color = CouiTheme.colors.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                    if (trailing != null) {
                        Spacer(Modifier.width(8.dp))
                        trailing()
                    }
                }
            },
        )
    }
}
