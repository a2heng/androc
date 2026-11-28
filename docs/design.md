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

## 4. UI 主题：Excel 表格风格（紧凑分栏）

目标：用“横线 + 竖线”的分栏表达信息密度，像 Excel 表格；不使用胶囊 chip，不堆空白。

### 4.1 视觉 token

| 元素 | 值 |
|---|---|
| 顶部横线 | 1dp，outline 18% |
| 分栏竖线 | 1dp × 12dp，outline 22% |
| 字号 | 9sp（标签和数值同一行） |
| 标签色 | `onSurfaceVariant` |
| 数值色 | `onSurface`；命中率/ctx/错误保留语义色 |
| 横向 padding | 每栏 5dp；栏与栏之间不再加额外 gap |
| 图标 | 15dp，放在 24dp 触摸目标内 |
| 背景 | 透明或所在 Surface 底色；分栏本身不加圆角胶囊 |
| 圆角 | 只给外层容器；分栏内部保持直角 |

### 4.2 布局

- 单行，禁止折行；左边状态（ctx、命中、颗粒度、Git、钱），右边导航（↑↓、文件、终端）。
- **↑↓ 必须一行**：`↑ 4.5k  ↓ 307`，不再上下两行。
- 钱：Go 显示额度并可点开额度；普通花费只读；不绑定终端跳转。
- 文件/终端用图标；点击打开**对话内子页**，不是外层 tab；返回或再点图标回到聊天。
- 没有内容的项直接省略，不显示空的 `—` 胶囊。
- 溢出时横向滚动或省略标签，绝不折成第二行。

### 4.3 交互

- 每栏用 `clickableNoRipple`，点击区域至少 24dp。
- 图标必须有 `contentDescription`。
- 文件/终端子页：只替换内容区，顶部“文件 / 终端”标签，返回键回到聊天；不关闭会话、不切换外层 tab。

### 4.4 实现

| 元素 | 代码 |
|---|---|
| 状态栏 | `ChatStatusBar` |
| 单栏文本 | `BarText` |
| 竖线 | `BarDivider` |
| 单行 token | `TokenInline` |
| 内部子页 | `ChatScreen.innerPage` + `InnerTab` |

### 4.5 反例

- 胶囊 chip + 空白 + 双行 token 混排。
- 用 `TextButton` 放状态栏/卡片头（48dp 最小高度会撑出 gap）。
- 空 `—` 占位。
- 状态栏跳到外层 tab 并关闭会话。

### 4.6 后续

- 把字号/线宽/透明度提成 `CompactBarTheme` 常量，文件列表、工具头复用。
- 窄屏溢出时优先隐藏“命中”标签，只留数值。

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
- [ ] 新增 UI 行为时，保持 [`mobile-integration.md`](mobile-integration.md) 的不变量；密集栏遵循 §4 的 Excel 表格风格。
- [ ] 改启动链路时，跑完 [`handover.md`](handover.md) §5 的六步验收。
- [ ] 发版前跑 `check-opencode-latest.py --strict`，并确认 thin/bundle/full 都能安装启动。
