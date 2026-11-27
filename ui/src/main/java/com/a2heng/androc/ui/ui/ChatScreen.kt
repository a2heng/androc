package com.a2heng.androc.ui.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UTurnLeft
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.a2heng.androc.ui.data.AppModel
import com.a2heng.androc.ui.data.Attachment
import com.a2heng.androc.ui.data.CommandInfo
import com.a2heng.androc.ui.data.FormField
import com.a2heng.androc.ui.data.FormInfo
import com.a2heng.androc.ui.data.Msg
import com.a2heng.androc.ui.data.Part
import com.a2heng.androc.ui.data.PermissionReq
import com.a2heng.androc.ui.data.SessionInfo
import com.a2heng.androc.ui.data.Tokens
import com.a2heng.androc.ui.data.formatCost
import com.a2heng.androc.ui.data.formatTokens
import com.a2heng.androc.ui.data.rateLabel
import com.a2heng.androc.ui.data.str
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(model: AppModel, session: SessionInfo, wide: Boolean = false) {
    var input by remember(session.id) { mutableStateOf(model.draft(session.id)) }
    var pickModel by remember { mutableStateOf(false) }
    var pickAgent by remember { mutableStateOf(false) }
    var revertTarget by remember { mutableStateOf<Msg?>(null) }
    var detailToolID by remember { mutableStateOf<String?>(null) }
    // 思考详情：live 表示点的是底部流式区，内容要继续跟着 model.liveReasoning 更新
    var reasoningView by remember { mutableStateOf<ReasoningView?>(null) }
    var fileTarget by remember { mutableStateOf<String?>(null) }
    // 文本里的路径 → 文件预览卡。支持 file://、~/、绝对路径，以及相对当前会话目录的路径。
    val openPath: (String) -> Unit = { raw ->
        val abs = resolveFileRef(raw, session.directory, model.homeDir)
        fileTarget = abs
        model.openFile(abs)
    }

    var showCommands by remember { mutableStateOf(false) }
    var showUsage by remember { mutableStateOf(false) }
    var showFilePicker by remember { mutableStateOf(false) }
    var openMenu by remember { mutableStateOf(false) }
    var confirmStop by remember { mutableStateOf(false) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val enterProgress by animateFloatAsState(
        targetValue = if (entered) 0f else 1f,
        animationSpec = tween(durationMillis = 260, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "chat-enter",
    )
    val motion = maxOf(backProgress, enterProgress)
    val deviceCorner = rememberDeviceCornerRadius()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // 键盘只跟随「用户手势」：列表一离开底部就收起；只有在底部再往下拉才弹出。
    // 内容流式增长、程序滚动都不触发，避免正文/工具输出把键盘顶来顶去。
    // Android 的 IME 高度由系统控制，只能 show()/hide()，不能逐像素跟手。
    val keyboard = LocalSoftwareKeyboardController.current
    val composerFocus = remember { FocusRequester() }
    val atBottom by remember { derivedStateOf { !listState.canScrollBackward } }

    // 不在底部时冻结 live 内容：否则新的文字会不断改变底部 item 的高度，
    // 把用户正在看的那一行顶下去。回到/滑到底部后自动解冻。
    var frozenLive by remember(session.id) { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(atBottom, model.sending) {
        when {
            !atBottom && frozenLive == null &&
                (model.sending || model.liveText.isNotBlank() || model.liveReasoning.isNotBlank()) ->
                frozenLive = model.liveText to model.liveReasoning
            atBottom || !model.sending -> frozenLive = null
        }
    }
    val context = LocalContext.current
    val attachments = remember { mutableStateListOf<Attachment>() }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val att = withContext(Dispatchers.IO) { loadImageAttachment(context, uri) }
                if (att != null) attachments.add(att)
            }
        }
    }

    // 其它 App 分享进来的文本 / 图片
    LaunchedEffect(model.pendingDraft, model.pendingAttachment) {
        if (model.pendingDraft != null || model.pendingAttachment != null) {
            val (text, image) = model.consumeDraft()
            if (!text.isNullOrBlank()) input = text
            if (image != null) attachments.add(image)
        }
    }

    LaunchedEffect(session.id) { model.loadMeta(session.directory) }
    LaunchedEffect(input, session.id) { model.saveDraft(session.id, input) }

    LaunchedEffect(session.id) {
        while (true) {
            model.refreshShells()
            kotlinx.coroutines.delay(3000)
        }
    }

    // 发出 pending / 新消息后回到最底部（reverseLayout 的 0 就是底部）
    // 只在队列变多时滚到底，队列被取走/撤回时不要来回跳
    var lastQueueSize by remember(session.id) { mutableStateOf(0) }
    LaunchedEffect(model.queue.size) {
        // 只有本来就在底部才跟随队列；正在看历史时不要被新的 pending 拽回去。
        if (model.queue.size > lastQueueSize && model.queue.isNotEmpty() && !listState.canScrollBackward) {
            listState.animateScrollToItem(0)
        }
        lastQueueSize = model.queue.size
    }

    // 滚动到最上面（reverseLayout 的末尾）时，继续往更早翻一页
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 4) model.loadOlderMessages()
        }
    }

    val pendingPermissions = model.permissions.filter { it.sessionID == session.id }
    val pendingForms = model.forms.filter { it.sessionID == session.id }
    // 通过 API 新建的会话在服务端没有 model 字段，从最后一条 assistant 消息里补
    val effectiveModel = session.model ?: model.messages.lastOrNull { it.model != null }?.model
    val isGoModel = effectiveModel?.providerID == "opencode-go"
    val goMonthly = if (isGoModel) model.goLimits?.go?.monthly else null
    LaunchedEffect(isGoModel, session.id) {
        if (isGoModel) model.loadGoUsage(session.directory)
    }
    val lastTokens = model.messages.lastOrNull { it.isAssistant && it.tokens != null }?.tokens
    // 命中率用整个会话的累计口径（服务端 session.tokens 就是全会话汇总），
    // 上下文占用仍然只看最近一步（context 是单次请求的窗口占用）。
    val aggregateTokens = session.tokens
        ?: model.messages.mapNotNull { it.tokens }.reduceOrNull { a, b -> a.plus(b) }
    val contextLimit = effectiveModel?.let { model.contextLimits[it.id] } ?: 0L
    val contextUsed = (lastTokens?.input ?: 0L) + (lastTokens?.cacheRead ?: 0L) + (lastTokens?.cacheWrite ?: 0L)
    val contextPct = if (contextLimit > 0 && contextUsed > 0) {
        (contextUsed * 100.0 / contextLimit).coerceIn(0.0, 100.0)
    } else {
        null
    }
    val contextColor = contextPct?.let {
        contextUsageColor(it, MaterialTheme.colorScheme.background.luminanceIsDark())
    } ?: MaterialTheme.colorScheme.onSurfaceVariant
    val backOwner = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current
    if (backOwner != null) {
        PredictiveBackHandler { events ->
            try {
                events.collect { event -> backProgress = event.progress }
                model.closeSession()
                model.refreshSessions()
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                backProgress = 0f
                throw cancel
            }
        }
    } else {
        BackHandler {
            model.closeSession()
            model.refreshSessions()
        }
    }

    Scaffold(
        modifier = Modifier
            .clip(RoundedCornerShape(deviceCorner * motion))
            .graphicsLayer {
                // 子页返回：不移动，只淡出；干净利落
                alpha = 1f - motion
            },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (wide) Modifier
                            else Modifier.windowInsetsPadding(WindowInsets.statusBars),
                        )
                        .padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .clickableNoRipple {
                                model.closeSession()
                                model.refreshSessions()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // 两个圆按钮之间只有两行：上面标题，下面是「模型」「模式」两张卡片。
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            session.shortTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 和设置页完全同一套卡片、同一份数据
                            ModelControlCard(
                                model = model,
                                session = session,
                                onClick = {
                                    model.loadMeta(session.directory)
                                    pickModel = true
                                },
                                modifier = Modifier.weight(1f),
                            )
                            // 模式名短，别再和模型平分宽度，只占文本自身
                            AgentControlCard(
                                model = model,
                                session = session,
                                fill = false,
                                onClick = {
                                    model.loadMeta(session.directory)
                                    pickAgent = true
                                },
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    // 和左下角「+」同一种实心主色圆：右侧切会话
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .clickableNoRipple { openMenu = !openMenu },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "切换会话（${model.openSessions.size}）",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        },
        bottomBar = {
            ChatComposer(
                model = model,
                busy = model.sending,
                value = input,
                onChange = { input = it },
                attachments = attachments,
                onPickImage = { imagePicker.launch("image/*") },
                onPickFile = { showFilePicker = true },
                onPickCommand = { showCommands = true; model.loadMeta(session.directory) },
                onRemoveAttachment = { attachments.removeAt(it) },
                onStop = { confirmStop = true },
                focusRequester = composerFocus,
                onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty() || attachments.isNotEmpty()) {
                        input = ""
                        // 用「服务端命令 + 客户端内建命令」合并后的列表判断：
                        // compact 这类命令不在 /api/command 里，只看 model.commands 会被当成普通文本发出去。
                        // 命令必须带斜杠：裸写的 compact / interrupt 等一律当普通文本发出去，避免误触发。
                        val cmd = allCommands(model).firstOrNull {
                            text == "/" + it.name || text.startsWith("/" + it.name + " ")
                        }
                        if (cmd != null) {
                            when (cmd.name) {
                                "compact" -> model.compactSession()
                                "interrupt" -> model.interrupt()
                                "model" -> pickModel = true
                                "agent" -> pickAgent = true
                                "reload" -> model.reloadConfig()
                                "usage" -> {
                                    if (resolvedModel(model, session)?.providerID == "opencode-go") {
                                        model.loadGoUsage(session.directory, force = true)
                                        showUsage = true
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "当前模型不是 OpenCode Go，没有 Go 额度",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                                else -> model.runCommand(
                                    cmd.name,
                                    text.removePrefix("/" + cmd.name).trim(),
                                    attachments.toList(),
                                )
                            }
                        } else {
                            model.sendPrompt(text, attachments.toList())
                        }
                        attachments.clear()
                        scope.launch { listState.animateScrollToItem(0) }
                    }
                },
            ) {
                ChatStatusBar(
                    model = model,
                    tokens = aggregateTokens,
                    hitLabel = aggregateTokens?.let { rateLabel(it.hitRate) } ?: "—",
                    hitColor = aggregateTokens?.let {
                        rateColor(it.hitRate, MaterialTheme.colorScheme.background.luminanceIsDark())
                    } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    costLabel = session.cost.takeIf { it > 0 }?.let { formatCost(it) },
                    goLabel = goMonthly?.let { goQuotaPercent(it.percent) },
                    goColor = goMonthly?.let {
                        goQuotaColor(it.percent, MaterialTheme.colorScheme.background.luminanceIsDark())
                    } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    onUsage = { showUsage = true },
                    contextPct = contextPct,
                    contextColor = contextColor,
                    onToggleDetail = {
                        model.updateDetailLevel(nextDetailLevel(model.detailLevel))
                    },
                    onGit = { model.openGit(session.directory) },
                    onCache = { model.openCache(session) },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            if (pendingPermissions.isNotEmpty()) {
                // 权限请求用弹窗，避免被长对话挤到屏幕外
                val req = pendingPermissions.first()
                AppDialog(
                    onDismissRequest = { },
                    title = { Text("需要你的确认") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(req.action, fontWeight = FontWeight.SemiBold)
                            if (req.resources.isNotEmpty()) {
                                Text(
                                    req.resources.joinToString("\n").take(1200),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (req.message.isNotBlank()) {
                                Text(req.message, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { model.replyPermission(req, "once") }) { Text("允许一次") }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = { model.replyPermission(req, "reject") }) {
                                Text("拒绝", color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(onClick = { model.replyPermission(req, "always") }) { Text("总是允许") }
                        }
                    },
                )
            }

            if (model.messagesLoading && model.messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            } else {
                // 瀑布流和 Pending 分开：Pending 固定在输入框上方，不参与消息列表滚动
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .pointerInput(Unit) {
                                // 只观察、不消费：Initial pass 读手指轨迹，列表滚动仍然由 LazyColumn 自己处理。
                                val threshold = 28.dp.toPx()
                                awaitEachGesture {
                                    val down = awaitFirstDown(
                                        requireUnconsumed = false,
                                        pass = PointerEventPass.Initial,
                                    )
                                    var lastY = down.position.y
                                    var totalY = 0f
                                    var hidden = false
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        totalY += change.position.y - lastY
                                        lastY = change.position.y
                                        // 只处理往下滚：收起键盘。往上滚不做任何事。
                                        if (!hidden && totalY > threshold) {
                                            hidden = true
                                            keyboard?.hide()
                                        }
                                        if (change.changedToUpIgnoreConsumed() || !change.pressed) break
                                    }
                                }
                            },
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            reverseLayout = true,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                if (model.detailLevel == "compact") 8.dp else 12.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(
                                if (model.detailLevel == "compact") 4.dp else 10.dp,
                            ),
                        ) {
                            val liveText = frozenLive?.first ?: model.liveText
                            val liveReasoning = frozenLive?.second ?: model.liveReasoning
                            if (frozenLive != null || liveReasoning.isNotBlank() || liveText.isNotBlank() || model.sending) {
                                item(key = "live") {
                                    LiveBubble(
                                        model,
                                        model.detailLevel,
                                        text = liveText,
                                        reasoning = liveReasoning,
                                        sending = frozenLive == null && model.sending,
                                        onOpenReasoning = { reasoningView = ReasoningView(it, live = true) },
                                        onOpenPath = openPath,
                                    )
                                }
                            }
                            model.statusNotice?.let { notice ->
                                item(key = "status-notice") {
                                    SelectionContainer {
                                    ConversationCard(
                                        title = if (notice.contains("重试")) "重试中" else "状态",
                                        body = notice,
                                        detail = model.detailLevel,
                                        icon = Icons.Rounded.Sync,
                                        onClose = { model.clearNotice() },
                                    )
                                    }
                                }
                            }
                            (model.lastError ?: model.connectionError)?.let { error ->
                                item(key = "error-notice") {
                                    SelectionContainer {
                                    ConversationCard(
                                        title = "错误",
                                        body = error,
                                        detail = model.detailLevel,
                                        icon = Icons.Rounded.ErrorOutline,
                                        isError = true,
                                        onClose = { model.clearError() },
                                    )
                                    }
                                }
                            }
                            items(model.messages.asReversed(), key = { it.id }) { msg ->
                                // 卡片出现/消失统一用 CardMotion（淡入淡出，不位移）
                                Box(
                                    Modifier
                                        .animateItem(
                                            fadeInSpec = CardMotion.FadeIn,
                                            placementSpec = null,
                                            fadeOutSpec = CardMotion.FadeOut,
                                        )
                                        .clipToBounds(),
                                ) {
                                    MessageItem(
                                        msg = msg,
                                        detail = model.detailLevel,
                                        baseDir = session.directory,
                                        onBackground = { model.backgroundCurrent() },
                                        onOpenReasoning = { reasoningView = ReasoningView(it, live = false) },
                                        onLongPressUser = { revertTarget = it },
                                        onOpenTool = { detailToolID = it.toolID },
                                        onOpenPath = openPath,
                                    )
                                }
                            }
                            if (model.loadingOlderMessages) {
                                item(key = "older-loading") {
                                    Box(
                                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }
                        }
                        if (!atBottom) {
                            SmallScrollToBottom(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp),
                            ) {
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                        }
                        if (model.shells.any { it.sessionID == session.id }) {
                            // 悬浮球放到整屏层（Scaffold 外面），展开的卡片才能和
                            // 工具/思考/文件详情一样居中、背景渐暗。
                        }
                    }
                    if (model.queue.isNotEmpty()) {
                        PendingRows(model) { restored -> input = restored }
                    }
                }
            }
        }

        // 打开的会话：卷帘卡片，占满可视宽度，向下展开；点外面或返回键收起，背景渐暗
        if (openMenu) {
            BackHandler { openMenu = false }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickableNoRipple { openMenu = false },
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = openMenu,
                modifier = Modifier.align(Alignment.TopStart),
                enter = androidx.compose.animation.expandVertically(
                    animationSpec = tween(220),
                    expandFrom = Alignment.Top,
                ) + androidx.compose.animation.fadeIn(tween(140)),
                exit = androidx.compose.animation.shrinkVertically(
                    animationSpec = tween(180),
                    shrinkTowards = Alignment.Top,
                ) + androidx.compose.animation.fadeOut(tween(120)),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shape = RoundedCornerShape(0.dp, 0.dp, 18.dp, 18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "打开的会话",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${model.openSessions.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            IconButton(
                                onClick = {
                                    openMenu = false
                                    model.closeAllOpenSessions()
                                },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "退出多会话",
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(bottom = 4.dp))
                        if (model.openSessions.isEmpty()) {
                            Text(
                                "还没有打开的会话",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        }
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(model.openSessions, key = { it.id }) { item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickableNoRipple {
                                            openMenu = false
                                            if (item.id != session.id) model.openSession(item)
                                        }
                                        .padding(start = 8.dp, top = 7.dp, end = 2.dp, bottom = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        Modifier
                                            .size(7.dp)
                                            .background(
                                                if (item.id == session.id) MaterialTheme.colorScheme.primary
                                                else if (model.isActive(item.id)) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                CircleShape,
                                            ),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            item.shortTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (item.id == session.id) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            item.directory.substringAfterLast('/').ifBlank { item.directory },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (item.id == session.id) {
                                        Text(
                                            "当前",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    IconButton(
                                        onClick = { model.closeOpenSession(item.id) },
                                        modifier = Modifier.size(30.dp),
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "关闭这个会话",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
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

    // 后台 shell 悬浮球 + 展开卡片：放在 Scaffold 外面，和详情卡共用 DetailOverlay。
    if (model.shells.any { it.sessionID == session.id }) {
        FloatingShells(model = model, sessionID = session.id)
    }

    if (pickModel) {
        ModelPickerSheet(
            model = model,
            title = "选择模型",
            selected = resolvedModel(model, session),
            onPick = { ref -> if (ref != null) model.chooseModel(ref) },
            onDismiss = { pickModel = false },
        )
    }

    if (pickAgent) {
        AgentPickerSheet(
            agents = agentOptions(model.agents),
            selectedId = resolvedAgentId(model, session),
            onPick = { id ->
                model.chooseAgent(id)
                pickAgent = false
            },
            onDismiss = { pickAgent = false },
        )
    }

    if (showUsage) {
        GoUsageSheet(model = model, onDismiss = { showUsage = false })
    }

    pendingForms.firstOrNull()?.let { form ->
        FormDialog(
            form = form,
            onDismiss = { model.dismissForm(form.id) },
            onSubmit = { answer -> model.replyForm(form, answer) },
        )
    }

    revertTarget?.let { msg ->
        AppDialog(
            onDismissRequest = { revertTarget = null },
            title = { Text("回退到这条消息？") },
            text = {
                Text(
                    "会用 opencode 的 revert 撤销这条消息及其之后的内容。" +
                        "「回退并还原文件」会把这一段的文件改动一起还原。",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    model.revertTo(msg.id, false)
                    revertTarget = null
                }) { Text("回退对话") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            copyToClipboard(context, msg.text)
                            revertTarget = null
                        },
                        enabled = msg.text.isNotBlank(),
                    ) { Text("复制") }
                    TextButton(onClick = { revertTarget = null }) { Text("取消") }
                    TextButton(onClick = {
                        model.revertTo(msg.id, true)
                        revertTarget = null
                    }) {
                        Text("回退并还原文件", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
    }

    if (confirmStop) {
        AppDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("停止当前任务？") },
            text = {
                Text(
                    "会中断这个会话正在进行的回答（等同于把运行中的任务停下来）。",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    model.interrupt()
                }) { Text("停止", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) { Text("继续") }
            },
        )
    }

    fileTarget?.let { path ->
        DetailOverlay(onDismiss = { fileTarget = null; model.closeFile() }) { close ->
            FilePreviewCard(model, path, close)
        }
    }

    // 详情卡每次重组都从最新消息里取，运行中的工具/思考在弹窗里也能继续更新
    val detailTool = detailToolID?.let { id ->
        model.messages.asSequence()
            .flatMap { it.parts.asSequence() }
            .filterIsInstance<Part.Tool>()
            .firstOrNull { it.toolID == id }
    }
    // 流式思考：把最新内容持续写进状态，否则流式结束后会退回“点开那一刻”的旧内容
    LaunchedEffect(model.liveReasoning, reasoningView?.live) {
        val view = reasoningView ?: return@LaunchedEffect
        if (view.live && model.liveReasoning.isNotBlank()) {
            reasoningView = view.copy(text = model.liveReasoning)
        }
    }
    val reasoningShown = reasoningView

    if (wide) {
        // 宽屏：详情做成右侧面板（聊天仍然可见），点另一张卡直接换内容
        detailTool?.let { tool ->
            SideDetailPane(onDismiss = { detailToolID = null }) { close ->
                ToolDetailCard(tool, session.directory, close, openPath)
            }
        }
        reasoningShown?.let { view ->
            SideDetailPane(onDismiss = { reasoningView = null }) { close ->
                ReasoningDetailCard(view.text, view.live, close)
            }
        }
    } else {
        detailTool?.let { tool ->
            DetailOverlay(onDismiss = { detailToolID = null }) { close ->
                ToolDetailCard(tool, session.directory, close, openPath)
            }
        }
        reasoningShown?.let { view ->
            DetailOverlay(onDismiss = { reasoningView = null }) { close ->
                ReasoningDetailCard(view.text, view.live, close)
            }
        }
    }

    PickerPageHost(visible = showFilePicker, onBack = { showFilePicker = false }) {
        FileMentionPage(
            model = model,
            startDir = session.directory.ifBlank { model.homeDir },
            onDismiss = { showFilePicker = false },
        ) { absolute ->
            val base = session.directory.ifBlank { model.homeDir }
            val rel = if (base.isNotBlank() && absolute.startsWith("$base/")) {
                absolute.removePrefix("$base/")
            } else {
                absolute
            }
            input = (input.trimEnd() + " @" + rel + " ").trimStart()
        }
    }

    PickerPageHost(visible = showCommands, onBack = { showCommands = false }) {
        PickerPage(title = "命令", onBack = { showCommands = false }) {
            val commandList = allCommands(model)
            if (commandList.isEmpty()) {
                EmptyState("没有可用命令")
            } else {
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(commandList, key = { it.name }) { cmd ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickableNoRipple {
                                    showCommands = false
                                    input = "/" + cmd.name + " " + input.trim()
                                }
                                .padding(vertical = 10.dp),
                        ) {
                            Column {
                                Text("/${cmd.name}", style = MaterialTheme.typography.bodyLarge)
                                if (cmd.description.isNotBlank()) {
                                    Text(
                                        cmd.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
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
private fun ChatComposer(
    model: AppModel,
    busy: Boolean,
    value: String,
    onChange: (String) -> Unit,
    attachments: List<Attachment>,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onPickCommand: () -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    focusRequester: FocusRequester,
    footer: @Composable () -> Unit = {},
) {
    var actionsOpen by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (actionsOpen) 45f else 0f, label = "plus-rotate")
    val slashQuery = if (value.startsWith("/") && !value.contains(' ')) value.substring(1) else null
    val suggestions = if (slashQuery == null) {
        emptyList()
    } else {
        allCommands(model)
            .filter { it.name.contains(slashQuery, ignoreCase = true) }
            .take(6)
    }
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
        ) {
            if (suggestions.isNotEmpty()) {
                Surface(
                    color = AndrocColor.Medium,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Column {
                        suggestions.forEach { cmd ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickableNoRipple { onChange("/" + cmd.name + " ") }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "/" + cmd.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    cmd.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            if (attachments.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    attachments.forEachIndexed { index, att ->
                        Box {
                            val bmp = remember(att.base64) {
                                decodeDataUri("data:${att.mime};base64,${att.base64}", 240)
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            }
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(18.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                    .clickableNoRipple { onRemoveAttachment(index) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "移除",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickableNoRipple { actionsOpen = !actionsOpen },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = if (actionsOpen) "收起" else "添加",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp).rotate(rotation),
                    )
                }
                AnimatedVisibility(
                    visible = actionsOpen,
                    enter = androidx.compose.animation.fadeIn(
                        animationSpec = tween(durationMillis = 110),
                    ) + androidx.compose.animation.scaleIn(
                        initialScale = 0.92f,
                        animationSpec = tween(
                            durationMillis = 140,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        ),
                    ),
                    exit = androidx.compose.animation.fadeOut(
                        animationSpec = tween(durationMillis = 90),
                    ) + androidx.compose.animation.scaleOut(
                        targetScale = 0.92f,
                        animationSpec = tween(
                            durationMillis = 100,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing,
                        ),
                    ),
                ) {
                    Row(
                        Modifier.padding(start = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ComposerAction(Icons.Rounded.AddPhotoAlternate, "图片", 26, MaterialTheme.colorScheme.primary, onPickImage)
                        ComposerAction(Icons.Rounded.NoteAdd, "文件", 24, MaterialTheme.colorScheme.primary, onPickFile)
                        ComposerAction(Icons.Rounded.Terminal, "命令", 23, MaterialTheme.colorScheme.primary, onPickCommand)
                    }
                }
                Spacer(Modifier.width(6.dp))
                val hint = when {
                    !busy -> "发消息给 opencode…"
                    model.sendMode == "steer" -> "运行中 · 引导当前回答"
                    model.sendMode == "interrupt" -> "运行中 · 中断后发送"
                    else -> "运行中 · 结束后再发"
                }
                // 自绘输入框：比 OutlinedTextField 的默认内边距紧凑，清空按钮能贴右上角
                Surface(
                    color = AndrocColor.Medium,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(Modifier.padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 6.dp)) {
                        BasicTextField(
                            value = value,
                            onValueChange = onChange,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            maxLines = 5,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .padding(end = if (value.isEmpty()) 0.dp else 24.dp),
                            decorationBox = { inner ->
                                Box {
                                    if (value.isEmpty() && !actionsOpen) {
                                        Text(
                                            hint,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    inner()
                                }
                            },
                        )
                        // 清空按钮贴在输入框右上角；点了先弹确认，避免误删刚打的字
                        if (value.isNotEmpty()) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickableNoRipple { confirmClear = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "清空输入",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                val hasContent = value.isNotBlank() || attachments.isNotEmpty()
                val stopMode = !hasContent && busy
                FloatingActionButton(
                    onClick = {
                        when {
                            hasContent -> onSend()
                            stopMode -> onStop()
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .alpha(if (hasContent || stopMode) 1f else 0.45f),
                    containerColor = when {
                        hasContent -> MaterialTheme.colorScheme.primary
                        stopMode -> MaterialTheme.colorScheme.error
                        else -> AndrocColor.Medium
                    },
                    contentColor = when {
                        hasContent -> MaterialTheme.colorScheme.onPrimary
                        stopMode -> MaterialTheme.colorScheme.onError
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = CircleShape,
                ) {
                    Icon(
                        if (stopMode) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.Send,
                        contentDescription = if (stopMode) "停止" else "发送",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            footer()
        }
    }

    if (confirmClear) {
        AppDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空输入？") },
            text = {
                Text(
                    "只会清空输入框里还没发出去的文字，已附加的图片/文件不受影响。",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onChange("")
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun DialogBackButton(onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = AndrocColor.MediumStrong,
            contentColor = AndrocColor.OnMedium,
        ),
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "返回",
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .height(34.dp)
            .background(AndrocColor.Medium, shape)
            .then(if (onClick != null) Modifier.clickableNoRipple(onClick) else Modifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 输入区下方的一排状态底栏：模型 / 模式 / 命中 / 花费。
 * 不横向滚动；其余信息（上下文、tokens、显示细节、Git、打开的会话）在顶栏「⋮」里。
 */
@Composable
private fun ChatStatusBar(
    model: AppModel,
    tokens: Tokens?,
    hitLabel: String,
    hitColor: Color,
    costLabel: String?,
    goLabel: String? = null,
    goColor: Color = Color.Unspecified,
    onUsage: () -> Unit = {},
    contextPct: Double?,
    contextColor: Color,
    onToggleDetail: () -> Unit,
    onGit: () -> Unit,
    onCache: () -> Unit,
) {
    // 左右各留 12dp：圆角屏幕不会切到胶囊的圆角。
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(
                "ctx",
                contextPct?.let { "${it.roundToInt()}%" } ?: "—",
                valueColor = contextColor,
            )
            StatusChip(
                "",
                detailLevelLabel(model.detailLevel),
                onClick = onToggleDetail,
            )
            StatusChip(
                "Git",
                "",
                onClick = onGit,
            )
            StatusChip(
                "命中",
                hitLabel,
                valueColor = hitColor,
                onClick = onCache,
            )
            // Go 订阅没有会话花费（服务端 cost 为 0），改用本月额度占用当消费显示。
            if (costLabel != null) {
                StatusChip("", costLabel)
            } else if (goLabel != null) {
                StatusChip("Go", goLabel, valueColor = goColor, onClick = onUsage)
            } else {
                StatusChip("", "—")
            }
            Spacer(Modifier.weight(1f))
            // ↑/↓ 放最末尾：这里位置最宽裕，顶栏就不用再挤一行
            TokenStack(tokens)
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)
    // 全部胶囊共用同一个中强调底；强调项只把文字换成深主题色。
    val bg = AndrocColor.Medium
    val fg = if (emphasized) AndrocColor.Deep else valueColor
    Row(
        modifier
            .height(24.dp)
            .background(bg, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (label.isNotBlank()) {
            Text(
                label,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (value.isNotBlank()) Text(
            value,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 顶栏右侧：tokens 上下两行（上是输入、下是输出），单位自动 k / M。 */
@Composable
private fun TokenStack(tokens: Tokens?) {
    // 和左边胶囊同一套中强调底，左对齐，上下两行
    Column(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AndrocColor.Medium)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            tokens?.let { "↑ ${formatTokens(it.input)}" } ?: "↑ —",
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            tokens?.let { "↓ ${formatTokens(it.output)}" } ?: "↓ —",
            fontSize = 9.sp,
            lineHeight = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun MetaText(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
) {
    Text(
        text,
        fontSize = 9.5.sp,
        lineHeight = 11.sp,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = if (onClick != null) Modifier.clickableNoRipple(onClick) else Modifier,
    )
}

@Composable
private fun MetaDot() {
    Text(
        "·",
        fontSize = 9.5.sp,
        lineHeight = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.padding(horizontal = 3.dp),
    )
}

private fun detailLevelLabel(level: String): String = when (level) {
    "compact" -> "精简"
    "full" -> "适中"
    else -> "完整"
}

private fun nextDetailLevel(level: String): String = when (level) {
    "compact" -> "full"
    "full" -> "expanded"
    else -> "compact"
}

@Composable
private fun ComposerAction(
    icon: ImageVector,
    label: String,
    size: Int,
    tint: Color,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size.dp))
    }
}

/** 思考详情打开时记住来源：live 表示底部流式区，内容要继续跟着 model.liveReasoning 长。 */
private data class ReasoningView(val text: String, val live: Boolean)

@Composable
private fun ConversationCard(
    title: String,
    body: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isError: Boolean = false,
    markdown: Boolean = false,
    onClose: (() -> Unit)? = null,
) {
    if (body.isBlank()) return
    val context = LocalContext.current
    val previewLimit = if (detail == "compact") 120 else 220
    val long = body.length > previewLimit
    var expanded by rememberSaveable(body) { mutableStateOf(!long) }
    val collapsed = long && !expanded
    Surface(
        color = AndrocColor.Medium,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickableNoRipple { if (long) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                if (long) {
                    Text(
                        if (expanded) "收起" else "展开",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (onClose != null) {
                    TextButton(onClick = onClose) {
                        Text("关闭", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (markdown && !collapsed) {
                AssistantText(body, null)
            } else {
                Text(
                    if (collapsed) body.take(previewLimit) + "…" else body,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error
                    else AndrocColor.OnMedium,
                )
            }
        }
    }
}

@Composable
private fun MessageItem(
    msg: Msg,
    detail: String,
    baseDir: String = "",
    onBackground: () -> Unit = {},
    onOpenReasoning: (String) -> Unit = {},
    onLongPressUser: (Msg) -> Unit = {},
    onOpenTool: (Part.Tool) -> Unit = {},
    onOpenPath: ((String) -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    if (msg.isError) {
        ConversationCard(
            title = "错误",
            body = msg.error ?: msg.text.ifBlank { "请求失败" },
            detail = detail,
            icon = Icons.Rounded.ErrorOutline,
            isError = true,
        )
        return
    }
    if (msg.isCompaction) {
        if (msg.error != null) {
            ConversationCard(
                title = "上下文压缩失败",
                body = msg.error,
                detail = detail,
                icon = Icons.Rounded.Compress,
                isError = true,
            )
        } else {
            ConversationCard(
                title = "上下文压缩",
                body = msg.text,
                detail = detail,
                icon = Icons.Rounded.Compress,
                markdown = true,
            )
        }
        return
    }
    if (msg.isUser) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                color = AndrocColor.MediumStrong,
                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                modifier = Modifier
                    .widthIn(max = 330.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPressUser(msg)
                        },
                    ),
            ) {
                Column(Modifier.padding(horizontal = if (detail == "compact") 10.dp else 13.dp, vertical = if (detail == "compact") 6.dp else 9.dp)) {
                    msg.images.forEach { DataUriImage(it) }
                    Text(
                        msg.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AndrocColor.OnMedium,
                    )
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth()) {
        if (msg.retryAttempt != null) {
            val wait = msg.retryAt?.let {
                ((it - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
            }
            ConversationCard(
                title = "重试中 ${msg.retryAttempt}",
                body = buildString {
                    msg.retryError?.let { append(it) }
                    if (wait != null) {
                        if (isNotEmpty()) append("\n")
                        append("约 ${wait}s 后重试")
                    }
                },
                detail = detail,
                icon = Icons.Rounded.Sync,
            )
            Spacer(Modifier.height(4.dp))
        }
        msg.parts.forEach { part ->
            when (part) {
                is Part.Reasoning -> ReasoningBlock(part.text, detail, onOpenReasoning = onOpenReasoning)
                is Part.Tool -> ToolBlock(part, detail, onOpenTool, msg.tokens?.hitRate, baseDir, onBackground, onOpenPath)
                is Part.Text -> AssistantText(part.text, onOpenPath)
            }
        }
        if (msg.parts.isEmpty() && msg.text.isNotBlank()) AssistantText(msg.text, onOpenPath)
        msg.error?.let {
            ConversationCard(
                title = "错误",
                body = it,
                detail = detail,
                icon = Icons.Rounded.ErrorOutline,
                isError = true,
            )
        }
        if (detail != "compact") Footer(msg)
    }
}

@Composable
private fun AssistantText(text: String, onOpenPath: ((String) -> Unit)? = null) {
    if (text.isBlank()) return
    MarkdownText(
        text,
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        onOpenPath = onOpenPath,
    )
}

/** 工具卡片正文最多预览的行数：超过就显示「… 展开」，所有工具共用这一个阈值。 */
private const val TOOL_PREVIEW_LINES = 20

@Composable
private fun LeftAccent(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent, RoundedCornerShape(2.dp)),
        )
        Box(Modifier.weight(1f).padding(start = 6.dp)) { content() }
    }
}

/**
 * 多行文本的「省略 + 展开」：到固定行数后加省略号，末尾给一个可点的高亮「展开」，
 * 完整内容交给工具详情/思考详情弹窗去看，不在正文里塞滚动条。
 */
@Composable
private fun ClampedText(
    text: String,
    maxLines: Int,
    onExpand: () -> Unit,
    maskColor: Color,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize: androidx.compose.ui.unit.TextUnit = 10.5.sp,
) {
    // 注意：不要用 text 作为 remember 的 key。流式文本每来一段都会把状态重置成 false，
    // 按钮就会「出现→消失→出现」地闪；只从 false 变 true，绝不回退。
    var overflowed by remember { mutableStateOf(false) }
    Box(modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = fontSize,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = {
                if (it.hasVisualOverflow) overflowed = true
            },
        )
        if (overflowed) {
            // 嵌在右下角的小按钮：按钮自己的右下圆角就是卡片圆角，左上做成切角
            Surface(
                color = maskColor,
                shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clickableNoRipple(onExpand),
            ) {
                Text(
                    "展开",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReasoningBlock(
    text: String,
    detail: String,
    live: Boolean = false,
    onOpenReasoning: (String) -> Unit = {},
) {
    if (text.isBlank()) return
    val context = LocalContext.current
    if (detail == "compact") {
        // 精简模式也给一行思考，点开看完整内容
        Row(
            Modifier
                .fillMaxWidth()
                .clickableNoRipple { onOpenReasoning(text) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "思考",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "${text.length} 字",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "展开",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    } else {
    Surface(
        color = AndrocColor.Medium,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clickableNoRipple { onOpenReasoning(text) },
    ) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "思考",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (live) "进行中" else "完成",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (live) Sky else Accent,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${text.length} 字",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // 卡片本身就是唯一容器：不再套一层深色背景 + padding
            LeftAccent(
                accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                if (detail == "expanded") {
                    MarkdownText(
                        text,
                        Modifier.fillMaxWidth(),
                        compact = true,
                    )
                } else {
                    ClampedText(
                        text,
                        maxLines = 16,
                        onExpand = { onOpenReasoning(text) },
                        maskColor = AndrocColor.Medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun ReasoningDetailCard(text: String, live: Boolean, onClose: () -> Unit) {
    val (contentMin, contentMax) = rememberDialogContentHeight()
    val scroll = rememberScrollState()
    // 流式思考：内容还在长，跟着滚到底部（用户往上翻过就不要再抢）
    LaunchedEffect(text.length, live) {
        if (live && scroll.maxValue - scroll.value < 120) scroll.scrollTo(scroll.maxValue)
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 6.dp,
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .widthIn(max = 520.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DialogBackButton(onClick = onClose)
                Spacer(Modifier.width(4.dp))
                Text(
                    "思考过程 · ${text.length} 字",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                Modifier
                    .padding(top = 10.dp)
                    .heightIn(min = contentMin, max = contentMax)
                    .verticalScroll(scroll)
                    .padding(vertical = 5.dp),
            ) {
                MarkdownText(text, Modifier.fillMaxWidth(), compact = true)
            }
        }
    }
}

@Composable
private fun ShellBlock(text: String, command: Boolean, maxLines: Int) {
    val accent = if (command) Sky else MaterialTheme.colorScheme.outline
    val bg = if (command) Sky.copy(alpha = 0.10f) else AndrocColor.Medium
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .background(bg, RoundedCornerShape(6.dp)),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolBlock(
    part: Part.Tool,
    detail: String,
    onOpen: (Part.Tool) -> Unit,
    rate: Double? = null,
    baseDir: String = "",
    onBackground: () -> Unit = {},
    onOpenFile: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val hasDiff = part.diff.isNotBlank()
    val cleanPatch = if (hasDiff) cleanDiff(part.diff) else ""
    val hasOutput = part.output.isNotBlank()
    val hasImages = part.images.isNotEmpty()
    val hasError = part.error.isNotBlank()
    val statusColor = when (part.status) {
        "completed", "success" -> Accent
        "error", "failed" -> MaterialTheme.colorScheme.error
        "running" -> Sky
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val runningShell = part.name.lowercase() in setOf("shell", "bash", "sh") && part.status == "running"
    var backgrounded by remember(part.toolID) { mutableStateOf(false) }

    if (detail == "compact") {
        val summary = part.input.lineSequence().firstOrNull()?.take(140).orEmpty()
        Row(
            Modifier
                .fillMaxWidth()
                .clickableNoRipple { onOpen(part) }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(toolIcon(part.name), contentDescription = null, modifier = Modifier.size(12.dp), tint = statusColor)
            Spacer(Modifier.width(5.dp))
            Text(
                part.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    summary,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            val extra = when {
                hasImages -> "图片"
                hasDiff -> "diff"
                part.payload.isNotBlank() -> "内容"
                hasOutput -> "输出"
                hasError -> "错误"
                else -> ""
            }
            if (runningShell && !backgrounded) {
                Spacer(Modifier.width(6.dp))
                Text(
                    "后台",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickableNoRipple {
                            backgrounded = true
                            onBackground()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
            if (rate != null) {
                Spacer(Modifier.width(6.dp))
                val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
                Text(
                    rateLabel(rate),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = rateColor(rate, dark),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(48.dp),
                )
            } else if (extra.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    extra,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor.copy(alpha = 0.9f),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(48.dp),
                )
            }
        }
        return
    }

    val isShellTool = part.name.lowercase() in setOf("shell", "bash", "sh")
    val isEditTool = part.name in setOf("edit", "patch", "multiedit")
    val isWriteTool = part.name == "write"
    val isReadTool = part.name.lowercase() == "read"
    val isExpanded = detail == "expanded"
    val displayName = toolDisplayName(part.name)
    val inputPath = if (isReadTool || isEditTool || isWriteTool) firstLineText(part.input) else ""
    val relPath = shortToolPath(inputPath, baseDir)
    val readRange = if (isReadTool) {
        Regex("""lines? (\d+)[-–](\d+)""").find(part.output)?.let { "L${it.groupValues[1]}–${it.groupValues[2]}" }
    } else {
        null
    }
    val writeLineCount = if (isWriteTool) part.payload.lineSequence().count() else 0
    val addLines = if (isEditTool) cleanPatch.lineSequence().count { it.startsWith("+") } else 0
    val delLines = if (isEditTool) cleanPatch.lineSequence().count { it.startsWith("-") } else 0
    val subtitle = when {
        isReadTool -> listOfNotNull(relPath.ifBlank { null }, readRange).joinToString(" · ")
        isWriteTool -> listOfNotNull(relPath.ifBlank { null }, "${writeLineCount} 行").joinToString(" · ")
        isEditTool -> listOfNotNull(
            relPath.ifBlank { null },
            if (addLines + delLines > 0) "+$addLines −$delLines" else null,
        ).joinToString(" · ")
        else -> ""
    }
    val readBody = if (isReadTool) part.output.substringAfter('\n', part.output) else ""
    Surface(
        color = AndrocColor.Medium,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clickableNoRipple { onOpen(part) },
    ) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(toolIcon(part.name), contentDescription = null, modifier = Modifier.size(15.dp), tint = statusColor)
                Spacer(Modifier.width(6.dp))
                Text(displayName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                Text(
                    toolStatusLabel(part),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                )
                Spacer(Modifier.weight(1f))
                if (runningShell && !backgrounded) {
                    Text(
                        "后台",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickableNoRipple {
                                backgrounded = true
                                onBackground()
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                if (part.truncated) {
                    Text("已截断", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (subtitle.isNotBlank()) {
                val canOpenFile = (isReadTool || isEditTool || isWriteTool) &&
                    inputPath.isNotBlank() && onOpenFile != null
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = if (canOpenFile) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .then(
                            if (canOpenFile) Modifier.clickableNoRipple { onOpenFile!!(inputPath) }
                            else Modifier,
                        ),
                )
            }

            if (isShellTool) {
                if (part.input.isNotBlank()) {
                    ShellBlock(part.input.take(4000), command = true, maxLines = 6)
                }
                if (hasOutput) {
                    LeftAccent(
                        accent = Sky,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        if (isExpanded) {
                            Text(
                                part.output.takeLast(30000),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            ClampedText(
                                part.output.takeLast(12000),
                                maxLines = TOOL_PREVIEW_LINES,
                                onExpand = { onOpen(part) },
                                maskColor = AndrocColor.Medium,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            } else {
                if (!isReadTool && !isEditTool && !isWriteTool && part.input.isNotBlank()) {
                    Text(
                        part.input.lineSequence().firstOrNull()?.take(220) ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                if (isReadTool && readBody.isNotBlank()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .background(
                                AndrocColor.Medium,
                                RoundedCornerShape(6.dp),
                            )
                            .padding(6.dp),
                    ) {
                        if (isExpanded) {
                            Text(
                                readBody.take(30000),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            ClampedText(
                                readBody.take(20000),
                                maxLines = TOOL_PREVIEW_LINES,
                                onExpand = { onOpen(part) },
                                maskColor = AndrocColor.Medium,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (hasImages) {
                    part.images.forEach { DataUriImage(it) }
                }
                if (hasDiff) {
                    if (isExpanded) {
                        DiffView(cleanPatch)
                    } else {
                        val longDiff = cleanPatch.lineSequence().count() > TOOL_PREVIEW_LINES
                        Box(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 340.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                            ) {
                                DiffView(cleanPatch)
                            }
                            if (longDiff) {
                                Surface(
                                    color = AndrocColor.Medium,
                                    shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 6.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .clickableNoRipple { onOpen(part) },
                                ) {
                                    Text(
                                        "展开",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                if (isWriteTool && part.payload.isNotBlank()) {
                    if (isExpanded) {
                        Text(
                            part.payload.take(30000),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp)
                                .background(
                                    AndrocColor.Medium,
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(6.dp),
                        )
                    } else {
                        ClampedText(
                            part.payload.take(12000),
                            maxLines = TOOL_PREVIEW_LINES,
                            onExpand = { onOpen(part) },
                            maskColor = AndrocColor.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp)
                                .background(
                                    AndrocColor.Medium,
                                    RoundedCornerShape(6.dp),
                                )
                                .padding(8.dp),
                        )
                    }
                }
                if (hasOutput && !isWriteTool && !isReadTool && (!isEditTool || !hasDiff)) {
                    if (isExpanded) {
                        Text(
                            part.output.takeLast(30000),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                        )
                    } else {
                        ClampedText(
                            part.output.takeLast(12000),
                            maxLines = TOOL_PREVIEW_LINES,
                            onExpand = { onOpen(part) },
                            maskColor = AndrocColor.Medium,
                            modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                        )
                    }
                }
            }
            if (hasError) {
                Text(
                    part.error,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun ToolDetailCard(
    part: Part.Tool,
    baseDir: String = "",
    onClose: () -> Unit,
    onOpenFile: ((String) -> Unit)? = null,
) {
    val (contentMin, contentMax) = rememberDialogContentHeight()
    val isShellTool = part.name.lowercase() in setOf("shell", "bash", "sh")
    val isWriteTool = part.name == "write"
    val isEditTool = part.name in setOf("edit", "patch", "multiedit")
    val isReadTool = part.name.lowercase() == "read"
    val displayName = toolDisplayName(part.name)
    val inputPath = if (isReadTool || isEditTool || isWriteTool) firstLineText(part.input) else ""
    val relPath = shortToolPath(inputPath, baseDir)
    val readRange = if (isReadTool) {
        Regex("""lines? (\d+)[-–](\d+)""").find(part.output)?.let { "L${it.groupValues[1]}–${it.groupValues[2]}" }
    } else {
        null
    }
    val subtitle = when {
        isReadTool -> listOfNotNull(relPath.ifBlank { null }, readRange).joinToString(" · ")
        isWriteTool -> relPath
        isEditTool -> relPath
        else -> ""
    }
    val readBody = if (isReadTool) part.output.substringAfter('\n', part.output) else ""
    val statusColor = when (part.status) {
        "completed", "success" -> Accent
        "error", "failed" -> MaterialTheme.colorScheme.error
        "running" -> Sky
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 8.dp)
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
            ) {
                Column(Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DialogBackButton(onClick = onClose)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            toolIcon(part.name),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = statusColor,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            toolStatusLabel(part),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                        )
                    }
                    if (subtitle.isNotBlank()) {
                        val canOpenFile = (isReadTool || isEditTool || isWriteTool) &&
                            inputPath.isNotBlank() && onOpenFile != null
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = if (canOpenFile) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .then(
                                    if (canOpenFile) Modifier.clickableNoRipple {
                                        onClose()
                                        onOpenFile!!(inputPath)
                                    } else Modifier,
                                ),
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Column(
                        Modifier
                            .heightIn(min = contentMin, max = contentMax)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 5.dp),
                    ) {
                        if (isReadTool) {
                            if (readBody.isNotBlank()) {
                                Text(
                                    readBody.take(30000),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        } else if (isWriteTool) {
                            if (part.payload.isNotBlank()) {
                                Text(
                                    part.payload.take(20000),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            } else {
                                Text(
                                    "（没有写入内容）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            if (part.input.isNotBlank()) {
                                if (isShellTool) {
                                    ShellBlock(part.input, command = true, maxLines = Int.MAX_VALUE)
                                } else {
                                    Text(
                                        part.input,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (part.images.isNotEmpty()) {
                                part.images.forEach { DataUriImage(it) }
                            }
                            if (part.diff.isNotBlank()) {
                                DiffView(cleanDiff(part.diff))
                            }
                            if (part.output.isNotBlank() && (isShellTool || !isEditTool)) {
                                Spacer(Modifier.height(6.dp))
                                if (isShellTool) {
                                    ShellBlock(
                                        part.output.takeLast(30000),
                                        command = false,
                                        maxLines = Int.MAX_VALUE,
                                    )
                                } else {
                                    Text(
                                        part.output,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        if (part.error.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                part.error,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
}

@Composable
private fun FilePreviewCard(model: AppModel, path: String, onClose: () -> Unit) {
    val (contentMin, contentMax) = rememberDialogContentHeight()
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 6.dp,
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .widthIn(max = 720.dp),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DialogBackButton(onClick = onClose)
                Spacer(Modifier.width(4.dp))
                Text(
                    path.substringAfterLast('/'),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                path.substringBeforeLast('/'),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Box(
                Modifier
                    .heightIn(min = contentMin, max = contentMax)
                    .padding(vertical = 4.dp),
            ) {
                FilePreviewContent(model, path, Modifier.fillMaxSize())
            }
        }
    }
}

private fun toolStatusLabel(part: Part.Tool): String = when {
    part.exitCode != null && part.exitCode != 0L -> "exit ${part.exitCode}"
    part.status == "completed" || part.status == "success" -> "完成"
    part.status == "running" -> "运行中"
    part.status == "error" || part.status == "failed" -> "失败"
    else -> part.status.ifBlank { "—" }
}

/** 复制用户消息到系统剪贴板。 */
private fun copyToClipboard(context: Context, text: String) {
    if (text.isBlank()) return
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("OpenCode message", text))
    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
}

private fun toolDisplayName(name: String): String = when (name.lowercase()) {
    "read" -> "读取"
    "write" -> "写入"
    "edit", "patch", "multiedit" -> "编辑"
    "shell", "bash", "sh" -> "命令"
    "grep", "search" -> "搜索"
    "glob" -> "查找文件"
    "webfetch", "fetch" -> "抓取网页"
    "websearch" -> "网页搜索"
    "task", "subagent" -> "子任务"
    else -> name
}

/** 去掉 patch 自带的文件头（Index/====/---/+++），路径已经在卡片副标题里显示一次了。 */
private fun cleanDiff(diff: String): String =
    diff.lineSequence()
        .filterNot { line ->
            line.startsWith("Index: ") ||
                line.startsWith("diff --git ") ||
                line.startsWith("--- ") ||
                line.startsWith("+++ ") ||
                (line.isNotEmpty() && line.all { it == '=' })
        }
        .joinToString("\n")
        .trim()

private fun firstLineText(text: String): String =
    text.lineSequence().firstOrNull()?.trim().orEmpty()

private fun shortToolPath(path: String, baseDir: String): String {
    val p = path.trim()
    if (p.isEmpty()) return ""
    val base = baseDir.trimEnd('/')
    if (base.isNotEmpty() && p.startsWith("$base/")) return p.removePrefix("$base/")
    val parts = p.trim('/').split('/').filter { it.isNotEmpty() }
    return when {
        parts.size <= 2 -> p
        else -> "…/" + parts.takeLast(2).joinToString("/")
    }
}

@Composable
private fun Footer(msg: Msg) {
    val tokens = msg.tokens ?: return
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    val color = rateColor(tokens.hitRate, dark)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(color, RoundedCornerShape(4.dp)),
        )
        Text(
            "命中 ${rateLabel(tokens.hitRate)}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "in ${formatTokens(tokens.input)} · cache ${formatTokens(tokens.cacheRead)} · out ${formatTokens(tokens.outputAll)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (msg.cost > 0) {
            Text(
                formatCost(msg.cost),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveBubble(
    model: AppModel,
    detail: String,
    text: String = model.liveText,
    reasoning: String = model.liveReasoning,
    sending: Boolean = model.sending,
    onOpenReasoning: (String) -> Unit = {},
    onOpenPath: ((String) -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        if (reasoning.isNotBlank()) {
            ReasoningBlock(reasoning, detail, live = true, onOpenReasoning = onOpenReasoning)
        }
        if (text.isNotBlank()) {
            AssistantText(text, onOpenPath)
        } else if (sending) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    model.statusLine.ifBlank { "正在生成…" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PendingRows(model: AppModel, onWithdraw: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 150.dp)
            .animateContentSize(tween(durationMillis = 100))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        model.queue.forEach { item ->
            Surface(
                color = AndrocColor.Medium,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { model.withdrawQueued(item.id) { restored -> onWithdraw(restored) } },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            androidx.compose.ui.res.painterResource(com.a2heng.androc.ui.R.drawable.ic_return_horizontal),
                            contentDescription = "撤回编辑",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        item.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { model.cancelQueued(item.id) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "移除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallScrollToBottom(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        containerColor = AndrocColor.Medium,
        contentColor = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
    ) {
        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "回到底部", modifier = Modifier.size(22.dp))
    }
}

/** 选一张图 -> 最长边压到 1600px -> JPEG 85 -> inline base64（opencode 的 files[].uri）。 */
fun loadImageAttachment(context: Context, uri: Uri): Attachment? = runCatching {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: return@runCatching null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    var sample = 1
    while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
    val bitmap = BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample },
    ) ?: return@runCatching null
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    Attachment(
        name = "image_${System.currentTimeMillis()}.jpg",
        mime = "image/jpeg",
        base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP),
    )
}.getOrNull()

/** opencode 原生命令 + 本 App 的客户端命令（去重）。 */
private fun builtinClientCommands(): List<CommandInfo> = listOf(
    CommandInfo("compact", "压缩当前会话的上下文"),
    CommandInfo("interrupt", "中断当前回答"),
    CommandInfo("model", "切换模型"),
    CommandInfo("agent", "切换 Agent"),
    CommandInfo("usage", "查看 OpenCode Go 额度"),
    CommandInfo("reload", "重新加载服务端配置"),
)

private fun allCommands(model: AppModel): List<CommandInfo> =
    model.commands + builtinClientCommands().filter { extra ->
        model.commands.none { it.name == extra.name }
    }

@Composable
private fun SendModeItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "当前",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        onClick = onClick,
    )
}

// ---------------------------------------------------------------------------
// 表单（opencode 的 question / 结构化输入）
// ---------------------------------------------------------------------------

@Composable
private fun FormDialog(form: FormInfo, onDismiss: () -> Unit, onSubmit: (JsonObject) -> Unit) {
    val values = remember(form.id) { mutableStateMapOf<String, String>() }
    val bools = remember(form.id) { mutableStateMapOf<String, Boolean>() }
    val multis = remember(form.id) { mutableStateMapOf<String, Set<String>>() }

    AppDialog(
        onDismissRequest = onDismiss,
        title = { Text(form.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                form.fields.forEach { field ->
                    FieldEditor(field, values, bools, multis)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val answer = buildJsonObject {
                    form.fields.forEach { f ->
                        when (f.type) {
                            "boolean" -> put(f.key, JsonPrimitive(bools[f.key] ?: f.defaultBool ?: false))
                            "multiselect" -> put(
                                f.key,
                                buildJsonArray {
                                    (multis[f.key] ?: emptySet()).forEach { add(JsonPrimitive(it)) }
                                },
                            )
                            "integer" -> values[f.key]?.toLongOrNull()?.let { put(f.key, JsonPrimitive(it)) }
                            "number" -> values[f.key]?.toDoubleOrNull()?.let { put(f.key, JsonPrimitive(it)) }
                            else -> {
                                // 选了选项就用选项，否则用自定义输入
                                val chosen = multis[f.key]?.firstOrNull()
                                put(f.key, JsonPrimitive(chosen ?: values[f.key] ?: ""))
                            }
                        }
                    }
                }
                onSubmit(answer)
            }) { Text("提交") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun FieldEditor(
    field: FormField,
    values: MutableMap<String, String>,
    bools: MutableMap<String, Boolean>,
    multis: MutableMap<String, Set<String>>,
) {
    Column {
        Text(
            field.title + if (field.required) " *" else "",
            style = MaterialTheme.typography.labelLarge,
        )
        if (field.description.isNotBlank()) {
            Text(
                field.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (field.type) {
            "boolean" -> Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    bools[field.key] = !(bools[field.key] ?: field.defaultBool ?: false)
                }) {
                    Text(if (bools[field.key] ?: field.defaultBool ?: false) "是" else "否")
                }
            }
            "string" -> {
                val focusManager = LocalFocusManager.current
                // 点击输入框就用 press 事件取消单选：光靠 onFocusChanged 时，输入框已经聚焦就不会再触发
                val interaction = remember(field.key) { MutableInteractionSource() }
                LaunchedEffect(interaction, field.key) {
                    interaction.interactions.collect { event ->
                        if (event is PressInteraction.Press && field.type != "multiselect") {
                            if (!multis[field.key].isNullOrEmpty()) multis[field.key] = emptySet()
                        }
                    }
                }
                // 选项在上面，自定义输入在最下面
                if (field.options.isNotEmpty()) {
                    OptionChips(field, multis, values) {
                        // 选选项 = 关掉自定义回答：清空文字并收起焦点，两块互相排斥
                        values[field.key] = ""
                        focusManager.clearFocus()
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "或自定义回答",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val singleChosen = field.options.isNotEmpty() &&
                    field.type != "multiselect" &&
                    !multis[field.key].isNullOrEmpty()
                OutlinedTextField(
                    value = values[field.key] ?: field.defaultText ?: "",
                    onValueChange = {
                        values[field.key] = it
                        // 自己输入时，上面的单选选项自动取消
                        if (field.type != "multiselect") multis[field.key] = emptySet()
                    },
                    placeholder = {
                        Text(
                            field.placeholder.ifBlank {
                                when {
                                    singleChosen -> "点这里输入自定义回答（会取消上面的选择）"
                                    field.options.isNotEmpty() -> "或者直接输入…"
                                    else -> ""
                                }
                            },
                        )
                    },
                    interactionSource = interaction,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                )
            }
            "multiselect" -> OptionChips(field, multis, values)
            else -> OutlinedTextField(
                value = values[field.key] ?: field.defaultText ?: "",
                onValueChange = { values[field.key] = it },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun OptionChips(
    field: FormField,
    multis: MutableMap<String, Set<String>>,
    values: MutableMap<String, String>,
    onPicked: () -> Unit = {},
) {
    // 用不可变 Set 放在 state map 里：每次点击整体替换，避免 list 变异不触发重组 / 状态错乱
    val selected = multis[field.key] ?: emptySet()
    val primary = MaterialTheme.colorScheme.primary
    val isMulti = field.type == "multiselect"
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
        field.options.forEach { option ->
            val isSelected = option.value in selected
            Surface(
                color = if (isSelected) AndrocColor.MediumStrong
                else AndrocColor.Medium,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickableNoRipple {
                        // 与官方前端一致：单选再点同一项不会取消；多选是切换；
                        // 选选项 = 关掉自定义回答（清空文字并收键盘），两块互斥。
                        if (isMulti) {
                            multis[field.key] =
                                if (isSelected) selected - option.value else selected + option.value
                        } else {
                            multis[field.key] = setOf(option.value)
                            onPicked()
                        }
                    },
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 选中状态一定要看得出来：单选是圆点，多选是打勾方块
                    val markShape = if (isMulti) RoundedCornerShape(4.dp) else CircleShape
                    Box(
                        Modifier
                            .size(16.dp)
                            .border(1.5.dp, if (isSelected) primary else MaterialTheme.colorScheme.outline, markShape)
                            .background(if (isSelected) primary else Color.Transparent, markShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                            )
                        }
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) AndrocColor.OnMedium
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        if (option.description.isNotBlank()) {
                            Text(
                                option.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
