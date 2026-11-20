# Androc 设计

> 一句话：**一个 UI，一套 proot + Alpine musl 运行时，组件从签名市场按需安装。**

Androc 的复杂度集中在宿主：启动、挂载、校验、解压、自愈。用户和 agent 看到的
只有“连接服务器、发消息、开终端、装组件”。

---

## 1. 设计不变量

| # | 不变量 | 机制 |
|---|---|---|
| 1 | **单一运行时** | 所有进程都在 `proot -r <rootfs>` 内：opencode server、agent shell、终端、Gradle。`nativeLibraryDir/libproot.so` + `libproot_loader.so` 是唯一入口；rootfs 是 Alpine musl。 |
| 2 | **单一权威路径** | `$HOME` 只有一个（`files/home` 的 canonical path）；server cwd 是 `$HOME`，会话 cwd 是用户项目；`nativeLibraryDir` 每次启动从 `ApplicationInfo` 读取。proot 把 `$HOME`、`/dev`、`/proc`、`/sys`、`/system`、`hosts`、`resolv.conf` 绑进 rootfs，路径在内外一致。 |
| 3 | **rootfs 单一优先级** | `files/packs/android/<ver>/rootfs` → `files/runtime/<ver>/rootfs` → `files/runtime-core/rootfs`，只由 `MuslRootfs.dir` 实现。 |
| 4 | **组件单一来源** | 市场只有 `kernel-opencode` 与 `android`；`index.json` + `index.json.sig`（Ed25519）；`kind=tar`，`installed.size/sha256` 对应未压缩 tar；全量下载。 |
| 5 | **UI 单一来源** | `ui/` 是唯一 Compose 源码，只依赖 `contracts/` 与 `market/`；本机 server、shell、文件根由 `app/` 通过 `LocalRuntimeHost` / `LocalShellSpawner` 注入。行为不变量见 [`mobile-integration.md`](mobile-integration.md)。 |
| 6 | **自愈与续跑** | bundle rootfs 带 `.installed-v2` 标记；下载写 `.part` 并校验 size/sha256；解压进临时目录后原子改名；启动前清理上一轮残留进程；失败的安装保留已安装版本。 |
| 7 | **启动按需** | App 启动只挂 UI；点“本机 → 启动并登录”才启动 opencode，前台服务负责保活；远程连接只走 HTTP/SSE。 |
| 8 | **版本单一来源** | opencode 版本只写在 `opencode.version`；发版 CI 用 `check-opencode-latest.py --strict` 校验；`RELEASE_NOTES.md` 只保留当前版本。 |

这些约束的实现位置：

| 约束 | 代码 |
|---|---|
| proot 启动、bind、环境 | `app/.../LocalRuntime.java` |
| rootfs 选择 | `app/.../MuslRootfs.java` |
| bundle rootfs 解压 | `app/.../RuntimeAssets.java` |
| 市场签名、下载、解压 | `market/.../MarketClient.kt` |
| 组件安装与 skills | `app/.../PackInstaller.java`、`AppPluginManager.java` |
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
        │ proot + rootfs   │  │ :market            │
        │ opencode / shell │  │ 索引验签、下载、解压 │
        └───────────┬──────┘  └─────┬─────────────┘
                    │               │
        ┌───────────▼──────┐  ┌─────▼─────────────┐
        │ kernel-opencode  │  │ android            │
        │ 二进制 + 基础用户态 │  │ 工具链 + skills     │
        └──────────────────┘  └───────────────────┘
```

依赖方向：`:ui → :contracts + :market`、`:app → :ui + :contracts + :market`；
`:market` 不依赖 UI，`:ui` 不依赖运行时实现。

---

## 3. 运行时的边界

| 层 | 内容 | 归属 |
|---|---|---|
| APK native | proot、proot loader、musl loader、opencode 二进制（bundle）、libstdc++/libgcc | 随 APK，`nativeLibraryDir` |
| rootfs | Alpine minirootfs、bash/git/python/node、JDK/SDK/Gradle | 市场组件或 bundle 内置 |
| 数据 | `$HOME`：workspace、会话、skills、`.config/opencode` | 应用私有目录，跨组件共享 |

- 组件的可执行文件由 proot 启动；APK native 目录只放“启动器”和随包二进制。
- 终端、agent shell、构建命令共享同一个 rootfs，因此路径、`JAVA_HOME`、
  `ANDROID_HOME` 只有一套。
- Android 包提供带工具链的 Alpine rootfs 与 skills，通过 `/opt/androc/env.sh`
  对上同一套路径约定。

---

## 4. 文档索引

| 文档 | 内容 |
|---|---|
| [`handover.md`](handover.md) | 现状、启动链路、60 秒验收、已知缺口 |
| [`series.md`](series.md) | 产品分层、两个发行版、安装更新、市场包格式 |
| [`paths.md`](paths.md) | 应用目录、proot 挂载、环境变量、端口 |
| [`commands.md`](commands.md) | 构建、调试、发布与日常命令 |
| [`plugins.md`](plugins.md) | `opencode` / `android` 两个插件条目与安装语义 |
| [`model.md`](model.md) | 默认模型解析与写入 |
| [`connections.md`](connections.md) | 多服务器、排序、记忆、保活 |
| [`android-musl.md`](android-musl.md) | musl rootfs 组成、工具链布局、实测体积 |
| [`packs.md`](packs.md) | 市场索引、签名、发布节奏 |
| [`storage.md`](storage.md) | 应用私有目录与外部存储边界 |
| [`integration.md`](integration.md) | 官方 App、其他客户端、API 接入 |
| [`mobile-integration.md`](mobile-integration.md) | UI 行为不变量 |
| [`automation-api.md`](automation-api.md) | 回环自动化 API |

---

## 5. 改动检查

- [ ] 新增路径时，先确认它属于 `$HOME`、rootfs 还是 `nativeLibraryDir`，并写进 [`paths.md`](paths.md)。
- [ ] 新增 rootfs 来源时，同时改 `MuslRootfs.dir` 与本文 §1。
- [ ] 新增市场包时，同时更新索引 schema、`PackInstaller` 与 [`series.md`](series.md)。
- [ ] 新增 UI 行为时，保持 [`mobile-integration.md`](mobile-integration.md) 的不变量。
- [ ] 改启动链路时，跑完 [`handover.md`](handover.md) §5 的六步验收。
- [ ] 发版前跑 `check-opencode-latest.py --strict`，并确认 thin/bundle 都能安装启动。
