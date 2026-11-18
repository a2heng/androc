# Androc 交接文档

> 给下一个接手的人/agent：先读这一页，再按链接深入。
> 目标不是“介绍项目”，而是让你知道**现在能跑什么、哪里是坑、下一步做什么**。

---

## 0. 一分钟了解

- 仓库：`https://github.com/a2heng/androc`（本地 `D:\005_dev\androc`）
- 应用：Androc，`com.a2heng.androc`（debug `.debug`，beta `.beta`）
- 技术栈：Android + Java/Kotlin + Compose + WebView + Termux/proot + opencode
- 目标：**薄 UI + 无界面插件 APK**；所有能力（运行时、Android、写作…）都是插件
- 当前状态：**完整旧单 APK 仍可用；插件 APK 骨架已能绑定；尚未真正薄 UI**
- 内置 opencode：**2.3.2**，由 `opencode.version` 锁定
- 测试机：OPPO PKB110，Android 17，arm64，4 KB 页

---

## 1. 必读顺序

1. [`design.md`](design.md) — 产品总览、决策、路线图
2. [`handover.md`](handover.md) — 本页
3. [`paths.md`](paths.md) / [`commands.md`](commands.md) — proot 路径与命令契约
4. [`plugins.md`](plugins.md) — 插件 APK 契约、分类、来源/网址
5. [`plugin-catalog.md`](plugin-catalog.md) — 远期插件、Writing 设计
6. [`plugin-repos.md`](plugin-repos.md) — 插件仓库/CI/自动更新
7. [`linux-runtime.md`](linux-runtime.md) — Alpine/Termux/rootfs 方案与实测体积
8. [`model.md`](model.md) / [`connections.md`](connections.md) — 模型与连接
9. [`integration.md`](integration.md) / [`storage.md`](storage.md) — 外部接入与存储
10. [`series.md`](series.md) / [`packs.md`](packs.md) — 模块与产物发布

---

## 2. 现在能跑什么

| 能力 | 状态 | 说明 |
|---|---|---|
| `:app` debug/release | ✅ 可构建 | 当前仍是完整单 APK（约 896 MB debug） |
| 本机 opencode 2.3.2 | ✅ | proot 内运行，WebView + Compose 两套 UI |
| 端侧 Android 构建 | ✅ | JDK 21 + SDK 36 + aapt2 + Gradle |
| Compose UI | ✅ | 会话/聊天/文件/终端/设置 |
| 多服务器 | ✅ | 本机优先、远程按最近使用、记住上次 |
| 模型 | ✅ | 当前会话 > recent > 免费第一个 > 服务器默认 |
| Provider Connect | ✅ 首版 | `/api/integration` 的 key/OAuth；env/command 部分 |
| 插件管理 UI | ⚠️ 过渡 | 3 个进程内插件：opencode / termux / android |
| 插件 APK 骨架 | ⚠️ | `:plugin-sdk` + `:runtime-plugin`，可绑定，start 是占位 |
| 薄 UI / 运行时插件 | ❌ | 尚未迁移，基础 APK 仍含 runtime |
| Writing 插件 | ❌ | 只有设计文档 |
| RootfsManager / Alpine | ❌ | 只有设计文档和实测数据 |

---

## 3. 已确定的架构

### 3.1 产品

```text
基础 APK（只有 UI + 插件宿主）
  ├─ runtime 插件：OpenCode + Linux rootfs（Alpine；Android 用 Termux bionic）
  ├─ android 插件：全部 Android skills + JDK/SDK/Gradle/adb/AI
  ├─ writing 插件：skills + typst/模板
  └─ 社区插件
```

- **Android 是一个插件**，不拆成 build/AI/debug 多个插件；
- 插件 = Skill + 它需要的工具链；
- 插件是**无界面 APK**，独立安装/更新/卸载；
- runtime 不可卸载，但可独立更新；
- 插件只在连接**本机**时可管理；远程只显示连接。

### 3.2 模型

解析顺序：

```text
当前会话模型
  > 前端最近使用（recent）
  > 免费的第一个模型
  > 服务器默认（只读兜底）
```

- “选择模型”在会话内 = 改当前会话；无会话 = 改 recent；
- 不随便改服务器默认；`fs.write` 能力保留为显式工具；
- Provider Connect 走官方 `/api/integration`。

### 3.3 连接

- 多个服务器；本机第一；远程按最近使用；
- 启动时优先恢复上次服务器；
- 切换会清空当前会话/流/PTY，不污染另一个服务器。

### 3.4 Linux 运行时

- 目标：Kernel = OpenCode + git + Python + bash/coreutils（Alpine + proot）；
- Android = JDK/SDK/Gradle/aapt2/adb（Termux bionic + proot）；
- 共享 `$HOME/workspace`、skills；rootfs 隔离；
- 详见 [`linux-runtime.md`](linux-runtime.md)。

### 3.5 官方对齐

- 前端：对齐官方 OpenCode Mobile 的功能/交互；
- 后端：跟 opencode 版本与 API；
- 外部接入：标准 HTTP + pairing；官方 App、其他客户端、API、Console 见
  [`integration.md`](integration.md)。

---

## 4. 代码地图

| 路径 | 作用 |
|---|---|
| `app/` | 当前完整应用：`MainActivity`、`MobileFrontendActivity`、运行时、assets、jniLibs |
| `ui/` | Compose 客户端（`:ui`），唯一 UI 源码 |
| `contracts/` | 稳定 API：`RuntimeConnection`、`PluginInfo/PluginManager`、`Pairing`… |
| `kernel/` | 运行时库骨架（待迁入） |
| `tools/` | Android 工具链骨架（待迁入） |
| `plugin-sdk/` | AIDL `IRuntimePlugin`、`PluginHost`、`PluginContract`、`PluginConnection` |
| `runtime-plugin/` | 无界面运行时插件 APK 骨架；`RuntimePluginService` 可绑定 |
| `scripts/` | fetch/verify/make-toolchain/check-opencode-latest |
| `docs/` | 全部契约与设计 |
| `opencode.version` | 内置 opencode 版本唯一来源 |

关键代码：

- `app/.../LocalRuntime.java` — proot/server/pairing/HOME。
- `app/.../LocalToolbox.java` — 前缀、`.deb`、自愈、essentials。
- `app/.../AppPluginManager.java` — 当前进程内插件列表（过渡）。
- `app/.../PluginCatalog.java` — skill → 插件映射。
- `ui/.../AppModel.kt` — 连接/会话/模型/插件状态。
- `ui/.../OcClient.kt` — HTTP API，含 `setDefaultModel`、integration connect。
- `ui/.../SettingsScreen.kt` — 设置/插件/模型/连接。
- `ui/.../ChatScreen.kt` — 聊天、`/model`、键盘行为。
- `plugin-sdk/.../PluginHost.java` — 绑定插件 APK。

---

## 5. 构建与安装

### 5.1 环境

- JDK 17：`D:\006_Android\jdk-17`（AGENTS 里的 `D:\AndroidDevToolchain` 已过时）
- Android SDK：`D:\006_Android\sdk`
- 设备：PKB110（USB 调试）

```sh
export JAVA_HOME=/d/006_Android/jdk-17
export PATH="$JAVA_HOME/bin:$PATH"
```

### 5.2 构建

```sh
./gradlew :app:assembleDebug
./gradlew :plugin-sdk:assembleDebug :runtime-plugin:assembleDebug
```

产物：

```text
app/build/outputs/apk/debug/app-debug.apk
runtime-plugin/build/outputs/apk/debug/runtime-plugin-debug.apk
```

### 5.3 安装

```sh
export PATH="/d/006_Android/sdk/platform-tools:$PATH"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r runtime-plugin/build/outputs/apk/debug/runtime-plugin-debug.apk
adb shell am start -n com.a2heng.androc.debug/com.a2heng.androc.MobileFrontendActivity
```

### 5.4 opencode 版本

```sh
python scripts/check-opencode-latest.py --strict
python scripts/fetch-opencode.py            # 读 opencode.version
python scripts/fetch-opencode.py --latest   # 升级并改写版本文件
```

Release CI 已包含 `--strict`；发版前必须通过。

### 5.5 工具链/前缀

```sh
python scripts/fetch-toolbox.py
python scripts/verify-toolbox.py
python scripts/make-toolchain.py
```

---

## 6. 当前实测体积

| 路径 | 体积 |
|---|---:|
| `files/usr` 前缀 | 1.1 GB |
| `files/usr/lib/jvm` | 244 MB |
| `files/home/toolchain` | 643 MB |
| `files/usr/bin` | 239 MB |
| `files/usr/share` | 98 MB |
| `files/usr/lib/python3.14` | 43 MB |
| opencode 二进制 | 237 MB |
| debug APK | 约 896 MB |

结论：JDK + SDK/Gradle 是大头，应该归 Android 插件；kernel 换成 Alpine
后基础安装目标 300–400 MB。

---

## 7. 下一步（按优先级）

### P0：运行时插件真正迁移

1. 把 `LocalRuntime`/`LocalToolbox`/`LocalServerService`/`ToolboxService`/
   `CommandResolver`/`ToolboxServer`/`ServerWatch`/`RuntimeUpdateService` 等
   迁到 `:kernel`；
2. `:runtime-plugin` 依赖 `:kernel`，迁入 `assets/toolbox`、`jniLibs`；
3. `RuntimePluginService.start()` 调真实 runtime，返回
   `{url, username, password, running}`；
4. 基础 `:app` 删除 runtime 依赖，`MobileFrontendActivity` 用
   `PluginHost.bind(...).start()`；
5. 数据迁移另做（旧单 APK 的 HOME/sessions）。

### P1：薄 UI + Android 插件

- `:app` 只依赖 `:ui` + `:plugin-sdk` + `:contracts`；
- Android 插件 APK 带全部 Android skills + JDK/SDK/Gradle；
- runtime 去掉 JDK，Android 去掉 git/python（复用 kernel）。

### P2：插件 APK 管理 UI

- `PackageManager` 发现插件；
- `plugin.json`、`plugins.json` 源、安装/更新/卸载；
- 插件网址：主页/更新源可查看，源可新增/编辑/删除。

### P3：RootfsManager

- Alpine kernel rootfs + `apk`；
- Termux bionic Android rootfs；
- 基准：启动/`find`/`grep`/`git`/`python`/Gradle；
- 有 root 可选 chroot。

### P4：Writing 插件

- 按 `plugin-catalog.md` 的 v1 skills；
- Typst payload；
- 参考 Buchbinder/Bookwright 的工作流。

### P5：仓库/CI

- `androc-plugin-runtime`：每日检查 opencode，上游发版自动发；
- `androc-plugin-android`：周检 + 手动发版；
- `androc-packs`：聚合 `plugins.json` + 签名。

---

## 8. 已知问题/坑

| 问题 | 说明 |
|---|---|
| 基础 APK 仍很重 | 还没迁移 runtime；插件 UI 只是进程内过渡 |
| `RuntimePluginService.start()` 是占位 | 返回 `runtime not migrated yet` |
| 插件发现未实现 | 当前 `AppPluginManager` 读的是 bundled 资产，不是已安装 APK |
| `termux` 是过渡插件 | 目标：kernel 用 Alpine，Termux 只作为 Android bionic rootfs |
| `updateServerDefaultModel` 未暴露 | 模型选择已改为 session/recent；服务器默认只读 |
| Provider env/command 未完成 | key/OAuth 已通；env 只展示变量名 |
| 密码明文存 prefs | 计划用 Keystore |
| aapt2/JDK 是 bionic | 不能直接放进 Alpine musl；Android 用 bionic rootfs |
| 旧数据迁移 | 新包名/插件包后 HOME 不共享，需要导出/导入 |
| 插件签名 | 官方插件必须与基础 UI 同签名（signature 权限） |

---

## 9. 不变量 / 禁止

1. **一个 runtime（proot）**；没有 native/auto/fast。
2. **不按命令名分类**；不要重新引入 `PROOT_COMMANDS` / `SAFE_NATIVE` 扩张。
3. **Termux ELF 不能在 proot 外直接 exec**。
4. **不写死包名/路径**；用 `$HOME`、`$PREFIX`、`BuildConfig`。
5. **`ui/` 是唯一 UI 源码**；旧 UI 仓库不再维护；不再用 git submodule。
6. **Android 是一个插件**，不要再拆 build/AI/debug。
7. **JDK/SDK/Gradle 属于 Android 插件**，不要塞进 kernel。
8. **OpenCode 发版前必须检查最新版本**。
9. **Android 插件不自动发版**；CI 只检查/开 PR。
10. **不把代理地址写进文档**。

---

## 10. 验收

### 10.1 构建/安装

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 10.2 60 秒路径验收（真机）

```sh
oc-engine status        # proot
oc-doctor               # 无 MISSING
bash -lc 'git --version'
python3 --version
pkg install -y jq
```

### 10.3 插件绑定

```sh
adb shell dumpsys package com.a2heng.androc.runtime | grep -A2 BIND
```

应看到 `RuntimePluginService`、action `com.a2heng.androc.plugin.BIND`、
permission `com.a2heng.androc.permission.PLUGIN`。

### 10.4 模型/连接

- 会话 A 用 A 模型，会话 B 用 B 模型；改 A 不影响 B；
- 新建会话用最近使用；
- 本机第一、远程按最近使用；
- 重启后恢复上次服务器。

### 10.5 Writing（未来）

安装 writing → 生成文章 → typst 导出 PDF → 卸载不影响 Android。

---

## 11. 关键提交（最近）

```text
c2b6d7c docs: lightweight Linux runtime split
c6a1130 docs: plugin catalog / writing workflow / repo automation
c44d150 refactor: one Android plugin
cbf73d7 docs: plugin split independent updates
dc69799 feat: plugin SDK + runtime plugin APK skeleton
4828d0f feat: remember recent model per frontend
6074d25 feat: provider connect in model picker
```

---

## 12. 交接清单

- [ ] 读完 `design.md` + 本页 + `paths.md`/`commands.md`
- [ ] 配好 JDK/SDK，`./gradlew :app:assembleDebug` 成功
- [ ] 安装 `app-debug.apk` + `runtime-plugin-debug.apk`
- [ ] `dumpsys` 能看到 runtime 插件服务
- [ ] 跑一次 60 秒路径验收
- [ ] 从 §7 选一个 P0/P1 任务开始
- [ ] 改行为时同步 docs + skills
