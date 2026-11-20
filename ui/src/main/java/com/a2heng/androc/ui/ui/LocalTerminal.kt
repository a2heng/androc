package com.a2heng.androc.ui.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.a2heng.androc.ui.data.AppModel

/**
 * 本机终端（proot + bash）。
 */
@Composable
fun LocalTerminalView(model: AppModel) {
    LaunchedEffect(Unit) { model.ensureLocalTerminal() }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CouiIconButton(CouiIcon.Back) { model.closeLocalTerminalView() }
            Spacer(Modifier.padding(horizontal = 6.dp))
            CouiText("本机终端", style = CouiTheme.type.label)
        }
        TerminalPane(
            emulator = model.localTerminalEmulator,
            fontSize = model.ptyFontSize,
            onFontSize = model::updatePtyFontSize,
            onInput = model::sendLocalTerminal,
            placeholder = when {
                model.localTerminalStarting -> "正在打开终端…"
                model.localTerminalError != null -> model.localTerminalError ?: ""
                else -> "正在连接 shell…"
            },
        )
    }
}
