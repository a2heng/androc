# 前端集成：内置 `:ui` 模块

Androc 的 Compose 前端现在**直接内置在仓库里**：

```groovy
// settings.gradle
include(":ui")
```

`ui/` 是原来的 `client/` 模块改名而来，包含数据层、全部 Compose 界面、终端、
文件和设置页。`app` 通过 `implementation project(':ui')` 使用它。

完整的产品形态（一个 App、三层能力、pack 下载）见
[`series.md`](series.md)。这里只说明前端结构与行为约定。

## 一个 App，一个 UI 源码

- **Androc（本仓库 `:app`）** 是唯一应用；`thin` / `bundle` 只是构建 flavor。
- 原来的 `a2heng/opencode-android-ui` 不再维护，代码并入 `:ui`，不再维护第二份
  源码，也不再用 git submodule。
- `:ui` 只依赖 `:contracts`，不依赖 `:kernel`；它通过 `RuntimeConnection`
  接收服务器地址和凭据，不启动任何进程。
- 需要独立 UI APK 时，从同一个 `:ui` 模块组装，不复制代码。

## 登录和设置合并

下一阶段 `ConnectScreen` 会合并进 `SettingsScreen`，统一为 **Setup**：

- 连接：本机内核 / 局域网 / 远程 / 官方 App 配对；
- 能力包：kernel pack、tools pack 的安装、更新、修复、卸载；
- 存储与诊断。

首次启动和“设置”打开的是同一个 Composable，不再有两套入口。

## UI 行为约定

这些是踩过坑的行为，改 `ui/` 时必须保持（不变量，不是“曾经和另一个仓库同步”）：

| 能力 | 位置 | 说明 |
| --- | --- | --- |
| 默认模型 | `SettingsScreen.kt` + `AppModel.updateDefaultModel` | 选择页必须放在滚动 Column **外面**（用 `Box`），否则 `fillMaxSize` 被压成 0 高度，表现为「点默认模型没反应」；新建会话后调 `POST /api/session/{id}/model` 真正落到服务端 |
| 保存连接 | `ConnectScreen.kt` + `AppModel.savedConnections` | 只有 ping 通过才写入；地址栏右侧箭头下拉、回填密码、可删除；改地址/密码或取消立即 `cancelConnect()` |
| 连接重试 | `AppModel.connect` | 登录页第一次连不上停在原地（`hadConnected=false`），用过之后掉线才退避重连；`ON_RESUME` 补一次 |
| 后台保活 | `ConnectionService.kt` + `OpenCodeRoot.kt` | 连上后前台服务保持 SSE；`ON_RESUME` 调 `ensureKeepAlive()` 补启动（宿主在 `onCreate` 自动连接时可能被系统拒绝 FGS） |
| 路径统一 | `Models.kt` → `normPath` / `isAbsolutePath` / `joinPath` | 服务端返回的 `\` 统一成 `/`；`C:` 的上一级是 `C:/` |
| 相对路径 | `AppModel.resolvePath` / `Markdown.resolveFileRef` | 不以 `/` 开头的路径按「会话目录 → HOME → 文件页根」补全，避免 `/api/fs/read` 按服务端 cwd 解析（404/打开错文件） |
| 文件页上一级 | `FilesScreen.kt` / `NewSessionDialog.kt` / `FileMentionDialog.kt` | 根路径（`/`、`C:/`）不再往上；相对 `fsDir` 先补到基准根再切父目录 |
| Git「上一次提交」 | `AppModel.loadGit` | committed 模式传 `base=HEAD~1`；工作区干净时自动切过去 |
| 会话列表去重 | `SessionsScreen.kt` | 时间视图必须用 `groupedRest`（`recent` 已从 `rest` 取走前 3 条），否则同一会话出现两次 |
| 键盘跟随滚动 | `ChatScreen.kt` | 只处理**往下滚**：手指向下滑动 ≥28dp 时 `hide()` 收起键盘；往上滚不做任何事，不再“滑出键盘”。只看用户手势，流式内容/程序滚动不管；系统 IME 不能逐像素跟手 |
| 流式内容不顶行 | `ChatScreen.kt` | 不在底部时冻结 live 气泡（文本/思考），避免生成中的文字不断改变底部 item 高度、把用户正在看的行顶下去；回到底部自动解冻 |
| PTY | `AppModel.createPty` | 远端终端先让服务端用自己的默认 shell（Windows 上是 Git Bash/cmd），旧服务端再退回 `bash`；**Androc 本机终端不走这里**，由宿主注入本机 proot shell |
| 文件预览只此一份 | `FilePreview.kt` + `ImageViewer.kt` | 聊天里的文件卡片、文件页、Markdown 里的文件引用都调 `FilePreviewContent`；图片统一 `ClickableBitmap` → `ZoomableImageDialog`（手势用 Telephoto），不要在各页面里再写一份 |
| Markdown 渲染 | `Markdown.kt` | 表格网格线只有一套 1dp（每格各画 0.5dp 会叠成粗细不齐的接缝）；代码块右上角有复制；独占一行的 `![alt](src)` 渲染成图片卡片，点击全屏缩放 |
| Go 额度 | `GoUsageCard.kt` + `AppModel.loadGoUsage` | 只在 `providerID == "opencode-go"` 时显示：设置页在模型/模式卡片下、对话 `/usage`、以及命中率右侧的消费胶囊（Go 会话 cost=0 时改用本月额度百分比）；Credential 只在内存里用于读 Console 每日用量，不持久化 |

## 构建

```sh
./gradlew :app:assembleDebug
# 不再需要 git submodule update
```

前端在 `ui/`，宿主在 `app/`；`:ui` 的 namespace 是
`com.a2heng.androc.ui`，Androc 的 applicationId 是
`com.a2heng.androc(.debug/.beta)`。

## 已废弃的旧方案

以前用过 `mobile/opencode-mobile` 子模块、`LocalServerInfoProvider`
ContentProvider 自动握手和 mobile 仓库里的 `:client` 模块。ContentProvider
仍保留为同机客户端的可选便利，但不再是跨产品集成契约；官方 OpenCode
Android 走标准 pairing（见 `series.md` §8）。
