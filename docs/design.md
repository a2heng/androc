# Androc 设计总览（含未实现项）

本文是产品与架构的**总入口**：记录已经实现、正在做、还没做的决定。代码没跟上
不影响这里先写清楚。

状态标记：`[已实现]` / `[进行中]` / `[计划]` / `[待定]`。

---

## 0. 一句话

> **App 薄到只剩 UI；OpenCode、Termux、Android 工具链、未来的能力全部是
> 可独立安装/更新的无界面插件 APK。**

基础 APK 只负责：Compose 界面、连接管理、插件管理、插件 APK 的发现与安装。

---

## 1. 最终形态

```text
┌─────────────────────────────────────────────┐
│  基础 APK：只有 UI                           │
│  - 会话 / 聊天 / 文件 / 终端 / 设置          │
│  - 多服务器连接（本机 / 局域网 / 远程）       │
│  - 插件管理（发现、安装、更新、卸载、源）      │
└───────────────┬─────────────────────────────┘
                │ HTTP + pairing / AIDL
┌───────────────▼─────────────────────────────┐
│  OpenCode 运行时插件 APK（无界面，内置）      │
│  - proot 引擎、opencode、Linux rootfs（Alpine / Termux bionic）      │
│  - 前台服务、server、密码、pairing            │
│  - 不可卸载，可独立更新                       │
└───────────────┬─────────────────────────────┘
                │
┌───────────────▼─────────────────────────────┐
│  Android 插件 / 社区插件 APK                 │
│  - 每个插件 = Skill + 它需要的工具链          │
│  - 独立安装 / 更新 / 卸载                     │
└─────────────────────────────────────────────┘
```

`proot` 引擎不能再往下拆：它必须位于某个 APK 的 `nativeLibraryDir`。把它放进
运行时插件 APK，基础 UI 就可以完全不含 native 运行时。

---

## 2. 已确定的决策

| # | 决策 | 状态 |
|---|---|---|
| 1 | 一个应用、一个 UI；不再拆三个 APK | `[已实现]` |
| 2 | 插件是**独立的无界面 APK**，不是网址脚本 | `[进行中]` |
| 3 | 插件 = Skill + 工具链；分类：核心 / 运行时 / Android / 其他 | `[进行中]` |
| 4 | OpenCode 运行时插件不可卸载，但可独立检查上游并更新 | `[进行中]` |
| 5 | 插件管理只在连接**本机**时出现；远程只显示连接 | `[已实现]` |
| 6 | 多服务器：本机第一，远程按最近使用；记住上次连接的服务器 | `[已实现]` |
| 7 | 模型：当前会话 > 前端 recent > 免费第一个 > 服务器默认（只读兜底） | `[已实现]` |
| 8 | 前端功能对齐官方 OpenCode Mobile；后端跟随 OpenCode 能力/版本 | `[计划]` |
| 9 | 发版前必须确认内置 opencode 是最新稳定版 | `[已实现]` |
| 10 | 大工具链不强制随 App 下载；可下载或从本地包导入 | `[进行中]` |

---

## 3. 文档地图

| 文档 | 内容 |
|---|---|
| [`handover.md`](handover.md) | **交接文档**：现状、坑、下一步、验收 |
| [`tech-selection.md`](tech-selection.md) | **本次技术选型**：薄 UI、pack 市场、运行工具/语言/构建链/OpenCode 的体积与方案 |
| [`automation-api.md`](automation-api.md) | 开发/测试用回环 HTTP API（状态、连接、启动本机） |
| [`plugins.md`](plugins.md) | 插件 APK 契约、分类、来源/网址管理、安装更新卸载 |
| [`plugin-catalog.md`](plugin-catalog.md) | 远期插件目录；Writing 第一个样例与参考工作流 |
| [`plugin-repos.md`](plugin-repos.md) | 插件仓库、CI、每日自动更新、签名 |
| [`linux-runtime.md`](linux-runtime.md) | 轻量 Linux 运行时：Alpine / Termux bionic / RootfsManager、效率与体积 |
| [`model.md`](model.md) | 服务器默认模型、API 事实、写入方式 |
| [`connections.md`](connections.md) | 多服务器、新建/编辑 API、排序、记忆 |
| [`series.md`](series.md) | 模块分层、阶段路线 |
| [`packs.md`](packs.md) | 插件包/产物仓库、独立发版、签名 manifest |
| [`integration.md`](integration.md) | 官方 App、其他应用、API、Console 接入 |
| [`storage.md`](storage.md) | 应用私有目录、SAF、MediaStore、OBB |
| [`paths.md`](paths.md) / [`commands.md`](commands.md) | proot 路径与命令契约 |

---

## 4. 模块与仓库

当前：

```text
:contracts   稳定 API（连接、插件、pairing）
:kernel      proot/server 代码（未来迁到运行时插件 APK）
:tools       工具链代码/资产（未来迁到 Android 插件 APK）
:ui          Compose UI（薄）
:app         当前完整应用（迁移期）
```

目标：

```text
:app                 基础 APK：只有 UI + 插件宿主（thin）
:ui                  Compose 界面
:runtime-plugin      运行时插件 APK：proot + opencode + Linux rootfs
:kernel              运行时实现（被 :runtime-plugin 依赖）
:plugin-sdk          插件契约/AIDL/PluginHost
:tools               Android 插件：skills + JDK/SDK/Gradle
:contracts           共享 API
```

代码仓库仍建议一个 monorepo；**产物**（插件 APK / payload）可以放
`androc-packs` 独立发版。理由见 [`packs.md`](packs.md)。

---

## 5. 路线图

| Phase | 内容 | 状态 |
|---|---|---|
| 0 | 改名 Androc、模块拆分、UI 合并、opencode 版本锁 | `[已实现]` |
| 1 | 多服务器切换、本机优先、记住上次、插件管理雏形 | `[已实现]` |
| 2 | 默认模型收束到 Server Default（API + 本机回退） | `[进行中]` |
| 3 | 插件 APK 契约 + 插件源/网址管理 + 本地 APK 安装 | `[计划]` |
| 4 | 运行时插件 APK（proot/opencode/Linux rootfs） + 薄 UI 基础包 | `[进行中]` |
| 5 | Android 插件（一个：构建/调试/AI/测试 + 工具链） | `[计划]` |
| 6 | 官方 Mobile 功能对齐（配对、语音、通知、结果摘要） | `[计划]` |
| 7 | thin/bundle 分发、插件包签名 manifest、镜像 | `[计划]` |
| 8 | 数据迁移：旧单 APK → 薄 UI + 运行时插件 | `[待定]` |

---

## 6. 关键约束

1. **proot 引擎必须在某个 APK 内**；不能下载到 `files/` 后直接 exec。
2. 插件 APK 的 native 库由插件自己执行；基础 UI 不 exec 任何插件文件。
3. 插件安装/更新/卸载走系统 `PackageInstaller`；签名/哈希必须校验。
4. 插件只在连接本机时可管理；远程服务器的能力由服务器自己负责。
5. 任何“默认”都指服务器默认；App 不保留第二套 Default。
6. 失败必须可恢复：下载 `.part`、安装回滚、卸载不动项目/会话。
7. 文档先行：没实现的也写在这里，避免再次走偏。

---

## 7. 待定问题

- 运行时插件 APK 的数据（HOME/sessions）如何从旧单 APK 迁移？
- 社区插件用统一签名，还是按作者签名 + 用户信任（TOFU）？
- 插件 APK 体积上限：大工具链放 APK 还是 payload？
- 官方 Mobile 的语音/通话能力是否要复刻，还是只对齐连接与会话？
- 远程服务器没有 `experimental.fs.write` 时，默认模型是否只读？
