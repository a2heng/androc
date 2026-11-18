# Androc：一个 App，三层能力

本文是**产品形态、打包方式和模块边界**的唯一说明。路径/命令仍以
[`paths.md`](paths.md)、[`commands.md`](commands.md) 为准。

---

## 0. 结论

> **不拆三个 APK。** 一个 `com.a2heng.androc`，三层能力按需安装：
> UI（基础）→ kernel pack（本机 opencode）→ tools pack（Android 构建链）。

三个“产物”变成同一个 App 的三种状态：

| 状态 | 装了什么 | 能做什么 |
|---|---|---|
| **UI only** | 基础 APK | 连接电脑 / 远程 opencode、官方 App 配对 |
| **UI + kernel** | + kernel pack | 本机跑 opencode、会话、终端 |
| **UI + kernel + tools** | + tools pack | 端侧构建 Android APK |

发布两种构建，代码同源：

- **thin**（默认）：基础 APK，能力包按需下载；
- **full**（离线）：同样一个 App，把能力包打进 assets，给不能联网的设备。

---

## 1. 为什么不是三个 App

| 三个 APK | 一个 App + 能力包 |
|---|---|
| 三个 applicationId、三条升级链 | 一个 App、一条升级链 |
| 用户安装前就要选“简单/复杂” | 装完再按需开启 |
| UI/kernel/tools 三份代码容易漂移 | 同一份源码、同一套测试 |
| CI 要出三个包 | 出 thin/full 两个 APK + 两个 pack |
| 跨 App 共享服务器要额外发现协议 | 同进程，`RuntimeController` 直连 |

独立 UI APK 仍可作为“连接别人服务器”的轻量入口，但它不是另一个代码库：
以后真需要，只是同一个 `:ui` 的另一个 application 模块。

---

## 2. 三层能力

| 层 | 内容 | 安装方式 | 体积（当前） |
|---|---|---|---|
| **基础** | Compose UI、proot 引擎（proot + loader + musl）、连接/设置 | 随 APK | 约 10–20 MB |
| **kernel pack** | `opencode` musl 二进制、Termux toolbox（bash/readline/git/python/pkg…） | 下载或 full 内置 | 约 100–350 MB |
| **tools pack** | JDK 21、Android SDK、aarch64 `aapt2`、Gradle、Maven 缓存、skills | 下载或 full 内置 | 约 700 MB |

基础 APK 只保留**必须**的 native 部分；其余都能下载后在 proot 里执行。

---

## 3. Android 的硬约束：哪些能下载

这是“一个基础 + 下载两个阶段”能成立的关键：

| 文件 | 能不能下载 | 原因 |
|---|---|---|
| `proot`、`proot_loader`、`ld-musl` | **不能** | 它们在 proot 之外由 Android 直接 `exec`，必须在 `nativeLibraryDir` |
| `opencode` 二进制 | **能** | 它由 proot 启动（`proot … opencode serve`），在沙箱内执行 |
| Termux toolbox（bash/git/python） | **能** | 同样在 proot 内执行 |
| JDK/SDK/Gradle/aapt2 | **能** | 数据文件，在 proot 内运行 |

所以：

1. 基础 APK 永远带一个约 2–3 MB 的 proot 引擎；它是“内核”的引信，不是完整内核。
2. kernel pack 下载的是 `opencode` + toolbox，装到 `$HOME`/`$PREFIX` 的私有目录。
3. 任何下载的 ELF **不得**在 proot 外直接 `exec`；否则 Android 10+ 会
   `Permission denied`，这是 `docs/paths.md` 的同一类问题。
4. 如果哪天要把基础 APK 做到几 MB，只有 proot 引擎仍需随包；其余都可以做成 pack。

---

## 4. Pack 格式

每个 pack 是一个目录 + 清单，下载后解到 staging，校验通过再原子切换：

```text
kernel.pack/
├─ manifest.json
├─ bin/opencode          # musl，arm64
├─ prefix/               # Termux toolbox 文件（或 .deb + 安装脚本）
└─ scripts/              # 可选的修复/自检脚本

tools.pack/
├─ manifest.json
├─ toolchain/            # JDK、SDK、Gradle、Maven
└─ skills/               # android-build 等
```

`manifest.json` 至少包含：

```json
{
  "id": "kernel",
  "version": "2.3.2-1",
  "abi": "arm64-v8a",
  "minAppVersion": 8,
  "size": 123456789,
  "sha256": "…",
  "files": 1234
}
```

规则：

- 下载用 `.part` 续传；校验 `sha256`；空间不足直接拒绝，不留半成品；
- 安装失败自动回滚到上一个版本；`repair` 只补缺失文件；
- 支持从系统文件选择器导入本地 pack（离线安装）；
- 只信任 App 内置的公钥/哈希清单；pack 是代码，等同安装 APK；
- 托管优先 GitHub Releases，镜像可配置。

---

## 5. UI：登录和设置合并成“设置”

不再有独立的登录页。入口只有一个 **设置 / Setup**：

| 区块 | 内容 |
|---|---|
| **连接** | 本机内核（启动/停止/状态）、局域网/远程（URL + 密码）、官方 App 配对（链接/二维码） |
| **能力包** | kernel pack / tools pack：版本、体积、已安装/可更新、安装/修复/卸载、进度、剩余空间 |
| **存储** | 各层占用、清理缓存、导入离线 pack |
| **诊断** | `oc-doctor`、日志、重置 |

行为：

- 首次启动 → 设置页；选“在这台手机上运行”就安装 kernel pack，选“连接已有服务器”
  就只填 URL/密码，选“配对官方 App”就显示配对链接。
- 只有连接成功（本机或远程）才进入会话列表；断线时回到设置页。
- 设置页后续随时打开，和首次启动是同一个 Composable，不做两份。
- 原来的 `ConnectScreen` 合并进 `SettingsScreen`；不再维护单独的登录界面。

---

## 6. 模块图

```text
:contracts   稳定 API（RuntimeConnection / RuntimeController / PackManager…）
:kernel      proot 引擎、server 生命周期、pack 安装与自愈、pairing
:tools       tools pack 的内容与清单、skills、device API
:ui          Setup（登录+设置）、会话、聊天、文件、终端
:app         唯一应用；thin / full 两个 flavor
```

依赖规则不变：

```
:ui      -> :contracts
:kernel  -> :contracts
:tools   -> :contracts
:app     -> :ui + :kernel + :tools
```

`ui ↛ kernel`、`kernel ↛ tools`、`tools ↛ ui`。

---

## 7. 能力包 API（phase 2 落到 `:contracts`）

```java
interface PackManager {
    PackState state(String id);          // NOT_INSTALLED / DOWNLOADING / INSTALLING / READY / FAILED
    void install(String id, PackCallback cb);
    void repair(String id, PackCallback cb);
    void remove(String id, PackCallback cb);
    PackInfo info(String id);            // version, size, installedSize, updateAvailable
}
```

`RuntimeController` 保持现有的启动/停止/连接/配对接口；`ToolsController` 被
`PackManager` 取代或包一层。

---

## 8. 官方 opencode 客户端

官方 OpenCode Android（`ai.opencode.mobilecode`）仍然只走标准契约：

| 项 | 值 |
|---|---|
| 地址 | `http://127.0.0.1:<port>` |
| Basic auth | `opencode` + 服务器密码 |
| 配对 | `POST /api/pair`（新版 `/server/pair`）→ `{code, expires_in}` |
| 链接 | `<baseUrl>/auth/connect/<code>`，一次性，5 分钟 |
| 二维码 | `{"code":"…","urls":["http://…"]}` |

设置页负责显示/复制配对链接；kernel pack 没装或没启动时，明确提示先安装。

验收：安装 kernel pack → 启动 → 复制链接 → 官方 App 粘贴 → 看到本机会话。

---

## 9. 版本

| 版本 | 例子 | 规则 |
|---|---|---|
| App | `0.5.0` | 一个 versionCode |
| contracts | `1.0.0` | 破坏性改动升 major |
| kernel pack | `2.3.2-1` | 跟 opencode 版本，后缀是 pack 修订 |
| tools pack | `36.0.0-1` | 跟 SDK/build-tools 版本 |
| opencode | `2.3.2` | 上游二进制；唯一来源是 `opencode.version` |

**发版硬规则**：每次发版前，`opencode.version` 必须等于上游最新 release。

```sh
python scripts/check-opencode-latest.py --strict   # 落后就失败
python scripts/fetch-opencode.py --latest          # 升级并改写版本文件
```

release CI 已包含 `--strict` 检查；不要绕过它打 tag。

App 拒绝 `minAppVersion` 高于自身的 pack；pack 可以独立更新，不需要发新 APK。

---

## 10. 迁移路线

| Phase | 内容 | 完成标志 |
|---|---|---|
| **0（本次）** | Androc 改名、`:contracts`、`:kernel`/`:tools` 骨架、合并 UI 仓库 | 五个模块可配置 |
| **1** | Setup 页合并登录/设置；连接本机/远程 | 一个页面完成连接与状态 |
| **2** | PackManager + kernel pack（opencode + toolbox）；thin 基础包 | 不装 pack 只能连远程；装完能本机会话 |
| **3** | tools pack（JDK/SDK/Gradle/skills） | 端侧构建成功 |
| **4** | thin / full 两个 flavor + CI 产物（APK + packs） | 一次 tag 出 2 APK + 2 pack |
| **5** | 按需拆 repo / 发布 AAR | 外部只依赖 Maven 坐标 |

---

## 11. 不变量

1. **一个应用、一个 applicationId、一个运行时（proot）、一个前缀。**
2. **下载的 ELF 只能在 proot 内执行**；proot 引擎必须在 APK 的 `nativeLibraryDir`。
3. **pack 先校验后安装**；失败回滚，不留“半完成但标记完成”。
4. **UI 不启动进程**；kernel 拥有 server 和 pack 安装。
5. **tools 可选**：不装 tools pack，会话/终端仍然工作。
6. **官方 App 只走 HTTP + pairing**，不依赖我们的 ContentProvider。
7. **thin / full 同源**：full 只是把同样的 pack 放进 assets，不允许两份代码。
8. 文档、skills、验收清单同步更新。
