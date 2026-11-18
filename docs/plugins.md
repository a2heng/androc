# 插件系统：无界面 APK + Skill + 工具链

状态：`[计划]` 总体；`[进行中]` 当前仅有一版进程内插件管理雏形。

---

## 1. 插件是什么

> **插件 = 一个无界面的独立 APK。**
> 每个插件带一组 Skill，以及这些 Skill 需要的工具链；安装在系统里，
> 用 `PackageInstaller` 安装/更新/卸载。

不是插件的东西：基础 UI、proot 引擎（在运行时插件里）。

插件 APK：

- **没有 Activity**，不显示界面；
- 导出一个 `Service`（AIDL）或 `ContentProvider` 给基础 App；
- 用签名级权限保护；
- 在 `assets/plugin.json`（或 `res/xml/plugin.xml`）里声明元数据；
- 可以有 native 库（例如运行时插件里的 proot/opencode），由插件进程自己执行。

```json
{
  "schema": 1,
  "id": "android-build",
  "name": "Android 构建",
  "category": "android",
  "version": "36.0-1",
  "versionCode": 3601,
  "minAppVersion": 8,
  "skills": ["android-build", "android-project", "android-gradle"],
  "toolchain": ["jdk21", "sdk36", "aapt2", "gradle"],
  "homepage": "https://github.com/a2heng/androc-plugins",
  "updateSource": "https://github.com/a2heng/androc-packs/releases/latest/download/plugins.json"
}
```

基础 App 通过 `PackageManager.getResourcesForApplication(pkg)` 读取
`assets/plugin.json`；通过 `queryIntentServices` 发现插件。

---

## 2. 分类与粒度

四类：

| 分类 | 插件 | 说明 |
|---|---|---|
| **核心** | 基础 UI、插件宿主 | 不可卸载；随 App |
| **运行时** | OpenCode 内核、Termux | 内核不可卸载但可更新；Termux 可更新 |
| **Android** | 每个 Skill 一个插件 | android-build、android-project、android-ui、android-test、android-debug、android-ai、android-ndk… |
| **其他** | 社区/工具插件 | 独立作者，按签名信任 |

粒度规则：

- **一个插件 = 一个 Skill（或强相关的一组 Skill） + 它需要的工具链**；
- 共享工具链放在“提供者”插件里，其他插件声明 `depends`；
  - `android-build` 提供 JDK/SDK/Gradle/aapt2；
  - `android-debug` 只带 adb；
  - `android-ai` 只带 ONNX/TFLite/llama.cpp/MNN；
  - 纯 Skill 插件（android-ui、android-test…）只声明依赖，不重复带工具链。
- 卸载提供者前，必须先卸载/禁用依赖它的插件；UI 要提示。

当前代码里的 5 个插件（opencode、termux、android-build、android-ai、
android-debug）是过渡形态；目标是把 Android 拆细。

---

## 3. 插件源（仓库 URL）

基础 App 维护一个“插件源”列表，每个源是一个 `plugins.json`：

```json
{
  "schema": 1,
  "name": "Androc 官方",
  "url": "https://github.com/a2heng/androc-packs/releases/latest/download/plugins.json",
  "plugins": [
    {
      "id": "android-build",
      "version": "36.0-1",
      "apk": "https://…/android-build-36.0-1.apk",
      "sha256": "…",
      "minAppVersion": 8,
      "size": 734003200
    }
  ]
}
```

规则：

- 一个源可以有很多插件；一个插件也可以被多个源声明（按 `versionCode` 取高）；
- 安装前校验 `sha256`、`versionCode`、`minAppVersion`、签名；
- 源可以禁用/删除；已安装插件不受影响；
- 支持本地 APK 安装（不走源）。

---

## 4. 添加 / 查看 / 修改网址

### 添加插件

1. **从源安装**：插件 → 源 → 新增源（粘贴 `plugins.json` URL）→ 刷新 → 安装；
2. **从文件安装**：插件 → 从文件安装 → 选 APK；
3. **内置插件**：随 App 或首次启动安装，不经过源。

### 查看网址

插件详情显示：

- `主页`：插件作者/项目地址（来自 APK 的 `plugin.json`，只读）；
- `更新源`：这条插件从哪个 `plugins.json` / APK URL 更新；
- `签名`：签名指纹（社区插件显示，官方插件可隐藏）；
- `路径/包名/版本/大小`。

### 修改网址

- **源地址**：插件 → 源 → 编辑 URL；
- **单个插件的更新源**：插件详情 → 编辑更新源（存基础 App 的偏好，覆盖源里的值）；
- APK 自带的 `homepage` 只读；不能改别人的 APK 元数据，只能改“我从哪里更新它”。

---

## 5. 安装 / 更新 / 卸载

| 动作 | 行为 |
|---|---|
| 安装 | 下载 APK → 校验 → `PackageInstaller` 会话 → 用户确认 |
| 更新 | 同包名、更高 `versionCode`；保留插件数据 |
| 卸载 | `PackageInstaller.uninstall`；只删插件自己的数据 |
| 内置运行时 | 更新走同一流程；**卸载按钮禁用** |
| 失败 | 删除临时文件；已安装版本保持不变 |

进度、失败原因、签名错误都要在插件详情里可见。

---

## 6. 通信契约

推荐 AIDL（后续也可用 `ContentProvider.call`）：

```aidl
interface IRuntimePlugin {
  String start();                 // 返回 http://127.0.0.1:<port>
  void stop();
  String connectionJson();        // url / username / password / running
  String pairingLink();
  String pluginInfoJson();
}
```

- 基础 App `bindService`；
- 权限：`com.a2heng.androc.permission.PLUGIN`；
- 官方插件用 `signature`；社区插件用用户信任 + 指纹确认；
- 运行时插件启动后，基础 UI 用标准 HTTP + pairing 连接，和远程服务器同一条路。

---

## 7. 安全

- 插件 APK 是代码，安全等级等同 App；
- 官方源 + 签名校验；社区源明确提示“未验证”；
- 基础 App 不把本机密码交给未信任插件；
- 插件只能访问自己的数据目录；需要共享数据时走明确的 Provider/AIDL；
- 安装前展示包名、版本、签名、权限、来源。

---

## 8. 体积与 payload

- 小插件（skills/scripts）直接放 APK；
- 大工具链（JDK/SDK/Gradle）可以：
  - 放插件 APK（简单，但更新要重下大包）；
  - 或 APK 只带 manifest/安装器，payload 下载/缓存（推荐）；
- 无论哪种，**管理单位仍是插件 APK**；payload 是插件内部实现。

---

## 8. 独立更新：为什么必须切碎

一个 APK 只有一个 `versionCode`，更新就是整包替换。要让每层独立更新，必须把
“更新节奏相同”的东西放在一个 APK 里，不同节奏的拆开：

| 插件 APK | 内容 | 更新节奏 |
|---|---|---|
| `runtime` | proot、opencode、Termux userland | 上游 opencode/Termux 变时；opencode 二进制可在插件内部单独更新 |
| `android-build` | JDK、SDK、aapt2、Gradle、构建 skills | SDK/Gradle 变时 |
| `android-ai` | ONNX/TFLite/llama.cpp/MNN + android-ai skill | AI runtime 变时 |
| `android-debug` | adb/platform-tools + debug/install skills | platform-tools 变时 |
| `android-skills` | ui/test/architecture/networking… 纯 skill | 跟 App/文档变时 |
| 社区插件 | 作者自己的 skill + 工具链 | 作者决定 |

不建议“一个 skill 一个 APK”：20+ 个安装包管理成本高；纯 skill 用
`android-skills` 一个包，或后续改成 skill zip/payload，仍可独立更新。

### 8.1 APK 与 payload 分开

- APK 带：契约、manifest、安装器、少量脚本；
- 大头（JDK/SDK/Gradle、AI 模型）作为 payload 下载/缓存；
- 更新 payload 不需要发新 APK；只有契约/安装器变了才更新 APK。

### 8.2 跨 APK 的数据问题（关键）

不同 APK 的数据目录互不相通；而 proot/构建必须在 **runtime 插件**的
`$HOME/$PREFIX` 里执行。所以不能“android-build APK 自己解压 JDK 就完事”。

正确做法：

- runtime 插件拥有 `$HOME`、`$PREFIX`、skills 目录；
- 其他插件通过 AIDL 把**已校验**的 payload/skill 交给 runtime 安装：
  ```aidl
  void installPayload(String id, String url, String sha256, long size);
  void installSkill(String id, byte[] zip, String sha256);
  void removeCapability(String id);
  ```
- runtime 负责下载/校验/解压/回滚；插件 APK 只是“清单 + 安装请求”；
- 这样每个插件仍可独立更新，执行和存储仍只有一套。

### 8.3 版本

- 每个插件 APK：`versionCode` / `versionName`；
- 每个 payload：独立版本 + `sha256`；
- 基础 UI 只关心“插件是否安装、payload 是否可更新”，不关心内部实现。

## 9. 当前实现（MVP）

已经落地的骨架：

| 模块/文件 | 内容 |
|---|---|
| `:plugin-sdk` | AIDL `IRuntimePlugin` / `IRuntimePluginListener`、`PluginHost`、`PluginContract`、`PluginConnection` |
| `:runtime-plugin` | 无 Activity、无 launcher 的运行时插件 APK；`RuntimePluginService` 已可绑定，带 `assets/plugin.json` |
| 权限 | `com.a2heng.androc.permission.PLUGIN`（signature），只有同一签名的插件能绑定 |
| 服务 | action `com.a2heng.androc.plugin.BIND`，已安装后可用 `dumpsys package` 看到 |

当前 `RuntimePluginService.start()` 还是占位，返回 `runtime not migrated yet`；
运行时代码/资产还在旧 `:app`。下一步把它们迁到 `:runtime-plugin`，再把基础
UI 改成 `PluginHost.bind(...).start()`。

## 10. 迁移顺序（薄 UI）

1. `:plugin-sdk` 骨架（已完成）；
2. 把 `LocalRuntime`/`LocalToolbox`/`LocalServerService`/`ToolboxService`/
   `CommandResolver`/`ToolboxServer`/`ServerWatch`/`RuntimeUpdateService` 等
   迁到 `:kernel`，`:runtime-plugin` 依赖 `:kernel`；
3. `assets/toolbox`、`assets/skills`、`jniLibs` 运行时库迁到 `:runtime-plugin`；
4. `RuntimePluginService.start()` 调用真实 runtime，返回 URL/密码；
5. 基础 `:app` 删除运行时依赖，改为 `PluginHost` 绑定；
6. 运行时插件不可卸载，但可单独更新；UI 只保留 provider/服务器/插件管理；
7. 旧单 APK 的 HOME/sessions 迁移另做（导出/导入或一次性迁移）。

## 11. 当前状态与下一步

| 项 | 状态 |
|---|---|
| 进程内 PluginManager + 插件卡片 | `[已实现]`（过渡） |
| 本机优先、独立 OpenCode 更新 | `[已实现]` |
| 插件 APK 契约 + SDK | `[已实现]`（MVP） |
| 插件源/网址管理 UI | `[计划]` |
| 运行时插件 APK | `[进行中]`（可绑定骨架） |
| Android 细粒度插件 | `[计划]` |
| 官方/社区签名策略 | `[待定]` |
