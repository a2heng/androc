# Androc 设计

> 一句话：**一个 UI，一套 proot + Alpine musl 运行时，组件从签名内置组件按需安装。**

Androc 的复杂度集中在宿主：启动、挂载、校验、解压、自愈。用户和 agent 看到的
只有“连接服务器、发消息、开终端、装组件”。

---

## 1. 设计不变量

| # | 不变量 | 机制 |
|---|---|---|
| 1 | **单一运行时** | 所有进程都在 `proot -r <rootfs>` 内：opencode server、agent shell、终端、Gradle。`nativeLibraryDir/libproot.so` + `libproot_loader.so` 是唯一入口；rootfs 是 Alpine musl。 |
| 2 | **单一权威路径** | `$HOME` 只有一个（`files/home` 的 canonical path）；server cwd 是 `$HOME`，会话 cwd 是用户项目；`nativeLibraryDir` 每次启动从 `ApplicationInfo` 读取。proot 把 `$HOME`、`/dev`、`/proc`、`/sys`、`/system`、`hosts`、`resolv.conf` 绑进 rootfs，路径在内外一致。 |
| 3 | **rootfs 单一优先级** | `files/packs/android/<ver>/rootfs` → `files/runtime/<ver>/rootfs` → `files/runtime-core/rootfs`，只由 `MuslRootfs.dir` 实现。 |
| 4 | **组件单一来源** | 内置组件只有 `android`；OpenCode 与 runtime 随 App。`index.json` + `index.json.sig`（Ed25519）；`kind=tar`，`installed.size/sha256` 对应未压缩 tar；全量下载。 |
| 5 | **UI 单一来源** | `ui/` 是唯一 Compose 源码，只依赖 `contracts/` 与 `bundled/`；本机 server、shell、文件根由 `app/` 通过 `LocalRuntimeHost` / `LocalShellSpawner` 注入。行为不变量见 [`mobile-integration.md`](mobile-integration.md)。 |
| 6 | **自愈与续跑** | bundle rootfs 带 `.installed-v2` 标记；下载写 `.part` 并校验 size/sha256；解压进临时目录后原子改名；启动前清理上一轮残留进程；失败的安装保留已安装版本。 |
| 7 | **启动按需** | App 启动只挂 UI；点“本机 → 启动并登录”才启动 opencode，前台服务负责保活；远程连接只走 HTTP/SSE。 |
| 8 | **版本单一来源** | opencode 版本只写在 `opencode.version`；发版 CI 用 `check-opencode-latest.py --strict` 校验；`RELEASE_NOTES.md` 只保留当前版本。 |

这些约束的实现位置：

| 约束 | 代码 |
|---|---|
| proot 启动、bind、环境 | `app/.../LocalRuntime.java` |
| rootfs 选择 | `app/.../MuslRootfs.java` |
| bundle rootfs 解压 | `app/.../RuntimeAssets.java` |
| 内置组件签名、下载、解压 | （已移除；组件内置） |
| 组件安装与 skills | `app/.../AndroidAssets.java`、`AppPluginManager.java` |
| 本机对话框与注入 | `ui/.../LocalRuntimeDialog.kt`、`app/.../MobileFrontendActivity.java` |

---

## 2. 结构

```text
             ┌──────────────────────────────┐
             │ :ui  Compose                 │
             │ 连接 / 会话 / 聊天 / 文件 / 终端 │
             └──────────────┬───────────────┘
                            │ RuntimeConnection / PluginManager
             ┌──────────────▼───────────────┐
             │ :app  宿主                    │
             │ 启动、端口、密码、前台服务、自更新 │
             └──────┬───────────────┬───────┘
                    │               │
        ┌───────────▼──────┐  ┌─────▼─────────────┐
        │ proot + rootfs   │  │ :bundled            │
        │ opencode / shell │  │ 索引验签、下载、解压 │
        └───────────┬──────┘  └─────┬─────────────┘
                    │               │
        ┌───────────▼──────┐  ┌─────▼─────────────┐
        │ kernel-opencode  │  │ android            │
        │ 二进制 + 基础用户态 │  │ 工具链 + skills     │
        └──────────────────┘  └───────────────────┘
```

依赖方向：`:ui → :contracts + :bundled`、`:app → :ui + :contracts + :bundled`；
`:bundled` 不依赖 UI，`:ui` 不依赖运行时实现。

---

## 3. 运行时的边界

| 层 | 内容 | 归属 |
|---|---|---|
| APK native | proot、proot loader、musl loader、opencode 二进制（bundle）、libstdc++/libgcc | 随 APK，`nativeLibraryDir` |
| rootfs | Alpine minirootfs、bash/git/python/node、JDK/SDK/Gradle | 内置组件组件或 bundle 内置 |
| 数据 | `$HOME`：workspace、会话、skills、`.config/opencode` | 应用私有目录，跨组件共享 |

- 组件的可执行文件由 proot 启动；APK native 目录只放“启动器”和随包二进制。
- 终端、agent shell、构建命令共享同一个 rootfs，因此路径、`JAVA_HOME`、
  `ANDROID_HOME` 只有一套。
- Android 包提供带工具链的 Alpine rootfs 与 skills，通过 `/opt/androc/env.sh`
  对上同一套路径约定。

---

## 4. UI 主题：胶囊状态栏与统一卡片

目标：底栏信息紧凑但不挤；卡片头一眼看清「类型 / 摘要 / 命中 / 复制」；详情弹层就是同一张卡片放大。

### 4.1 视觉 token

| 元素 | 值 |
|---|---|
| 卡片底色 | `AndrocColor.Medium`（主题色与背景混合） |
| 卡片圆角 | 10dp（列表卡）/ 12dp（详情弹层） |
| 状态胶囊 | 最小高 32dp，圆角 10dp，左右 8dp；两行：标题 8sp/`lineHeight` 9sp，数值 9sp/11sp |
| 图标胶囊 | 32dp 正方形，圆角 10dp，图标 18dp |
| 操作胶囊（展开/复制） | 圆角 6dp，9sp，水平 7dp、垂直 2dp |
| 卡片头 | 左：类型图标 15dp + 类型名；中：摘要；右：命中% + 复制 |
| 正文预览 | 适中模式统一 4 行 |
| 卡片外边距 | 上下各 2dp；列表项之间不再额外加间距 |
| 表格边框（Markdown） | 浅色 `#9E9E9E`/65%，深色 `#BDBDBD`/45% |

### 4.2 底栏

- 状态胶囊一行居中：ctx、命中、显示、Git、文件、终端、花费；Git/文件/终端是图标胶囊，花费固定最右。
- 不再有 ↑↓ token。
- 已去掉「Excel 表格」分栏；不要恢复纯黑/纯白边框。
- 打开文件/终端/Git：输入行隐藏，最底部状态胶囊保持不动；上方内容切换成子页。
- 点击同一图标回到聊天；系统返回也回聊天。
- 文件子页默认打开当前会话目录；外层「文件」tab 才默认 home。

### 4.3 卡片

- 卡片头统一：`[图标] 类型  摘要（命令首行 / 字数）  ……  命中%  [复制]`；运行中的 shell 在命中前显示「后台」。
- 展开入口只在卡片头（胶囊），正文不再用右下角覆盖式「展开」。
- 完整模式（expanded）不再显示「展开」。
- 思考卡片不显示命中率：它和同一步的 shell 共用同一个请求值，重复显示没有信息量。
- 适中模式正文统一 4 行：思考 4 行；shell 共 4 行（命令最多 2 行，命令只有 1 行时输出 3 行）；其他工具 4 行。
- 适中模式不再单独显示「命中 / in / cache / out」行；只有完整模式在卡片后显示 token 明细。
- 上下文压缩也走同一套卡片头（图标、字数、展开、复制）。
- 对话内图片只显示方形缩略图（消息 104dp、工具 104dp、详情 128dp，圆角 10dp），`Fit` 完整显示整张图、留边不裁切，点一下全屏缩放；精简模式消息缩略图缩到 64dp。多图在同一个全屏查看里左右滑动翻页（缩放时用两侧箭头兜底），输入框附件也一样。
- 输入框上方待发送的附件：点缩略图可以全屏预览，右上角 X 删除。
- 卡片间距统一由卡片自身上下各 2dp 提供；不要按「消息」再加间距（服务端每一步是独立消息，加了会时大时小）。

### 4.4 详情弹层

- 点击卡片打开的弹层 = 同一张卡片的放大版：同样的头（返回 + 图标 + 类型 + 摘要 + 命中 + 复制）、同样的 `AndrocColor.Medium` 底色和 12dp 圆角。
- Git 页（仅本机 Git）分三段：**变更 / 历史 / 分支**。
  - 变更：状态字母以 `git status` 为准（新增 A / 修改 M / 删除 D / 重命名 R），路径省略中段，`+N −N`；点文件弹层看 diff（保留 `-U3` 上下文，新文件没有上下文）。
  - 历史：顶部是分支筛选（全部 / 本地分支），Canvas 车道图 + 提交列表；点提交展开文件列表（每行带 `A/M/D/R`），点文件弹层看该提交对这个文件的 diff；分支/标签用 chip 标在提交上。**只读**，不做切换/提交/暂存（操作留给终端或 AI）。
  - 当前目录是独立空仓库时，历史页会提示并给出「查看上级仓库」，避免 workspace 的嵌套 `.git` 挡住 home 的提交。
  - 远端（OpenCode API 没有 log）仍只有「工作区 / 上一次提交」两段。
- 未检测到 Git 时提供「初始化 Git 仓库 / 重新检测」：初始化走 `POST /api/vcs/init`；重新检测走 `POST /api/location/reload` 重建 location 缓存，用于终端里手动 `git init` 后仍显示空的情况（服务端只在 location 建立时判断有没有 VCS）。
- 子页（文件/终端/Git）顶部不显示返回按钮（用底栏图标或系统返回）。

### 4.5 实现

| 元素 | 代码 |
|---|---|
| 状态栏 | `ChatStatusBar` / `StatusChip2` / `StatusIcon` |
| 卡片头 | `ToolBlock` / `ReasoningBlock` / `ConversationCard` |
| 操作胶囊 | `ActionChip` |
| 详情弹层 | `DetailOverlay` + `ReasoningDetailCard` / `ToolDetailCard` / `GitDiffPopup` |
| 子页 | `ChatScreen.innerPage`（1 文件 / 2 终端 / 3 Git） |
| Shell 行数 | `ShellBlock.onLines` 回传渲染行数，输出拿剩余行 |

### 4.6 反例

- 在消息之间额外加 10dp 间距（卡片头之间应统一 4dp）。
- 正文右下角覆盖「展开」。
- 完整模式还显示「展开」。
- 状态胶囊固定死高度 + 不给 `lineHeight`（第二行会被顶出胶囊）。
- 详情弹层另起一套头/底色。

---

## 5. 文档索引

| 文档 | 内容 |
|---|---|
| [`handover.md`](handover.md) | 现状、启动链路、60 秒验收、已知缺口 |
| [`series.md`](series.md) | 产品分层、三个发行版、安装更新、内置组件包格式 |
| [`paths.md`](paths.md) | 应用目录、proot 挂载、环境变量、端口 |
| [`commands.md`](commands.md) | 构建、调试、发布与日常命令 |
| [`plugins.md`](plugins.md) | `opencode` / `android` 两个插件条目与安装语义 |
| [`model.md`](model.md) | 默认模型解析与写入 |
| [`connections.md`](connections.md) | 多服务器、排序、记忆、保活 |
| [`android-musl.md`](android-musl.md) | musl rootfs 组成、工具链布局、实测体积 |
| [`packs.md`](packs.md) | 内置组件索引、签名、发布节奏 |
| [`storage.md`](storage.md) | 应用私有目录与外部存储边界 |
| [`integration.md`](integration.md) | 官方 App、其他客户端、API 接入 |
| [`mobile-integration.md`](mobile-integration.md) | UI 行为不变量 |
| [`automation-api.md`](automation-api.md) | 回环自动化 API |

---

## 6. 改动检查

- [ ] 新增路径时，先确认它属于 `$HOME`、rootfs 还是 `nativeLibraryDir`，并写进 [`paths.md`](paths.md)。
- [ ] 新增 rootfs 来源时，同时改 `MuslRootfs.dir` 与本文 §1。
- [ ] 新增内置组件时，同时更新 flavor 资产、`AndroidAssets` 与文档。
- [ ] 新增 UI 行为时，保持 [`mobile-integration.md`](mobile-integration.md) 的不变量；底栏与卡片遵循 §4 的胶囊规范。
- [ ] 改启动链路时，跑完 [`handover.md`](handover.md) §5 的六步验收。
- [ ] 发版前跑 `check-opencode-latest.py --strict`，并确认 thin/bundle/full 都能安装启动。
