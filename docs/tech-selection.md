# Androc 技术选型：薄 UI + 组件市场 + 三个能力包

> 状态：`[已确认，按阶段落地]`。本文只做选型，不改代码。
> 调研日期 2026-11-19。所有体积为**实测或官方索引直读**；单位除特别标注外为
> **MB = 10^6 B**；MiB 会单独写明。
> 设备基线：PKB110 / Android 17 / SDK 37 / arm64-v8a / **页大小 4 KB** / 12 GB RAM。
> OpenCode 的官方 Mobile 是 iOS（TestFlight），Android 尚未发布；**Mobile 不是本项目的对标重点**。
> UI 的硬线是“比 OpenCode Web 版更好用”，原生端已实现的能力保留。

---

## 0. 目标（本次确认版）

1. **基础 APK = UI 版**：安装即用，能连远程/本机、能看市场；尽量小。
2. **三个能力组件**，全部可独立安装/更新/卸载：
   - **OpenCode**（最重要）：本机 server + 会话能力；
   - **Android 运行工具**：极小的命令环境，可扩展 Python / Java / Kotlin / C / C++ / Rust；
   - **Android 构建**：最重、按需安装，Java/Kotlin/C/C++/Rust 出 APK。
3. **组件市场**：独立 GitHub 仓库做索引、签名、发布；App 内只读 manifest，像市场一样安装/更新。
4. **UI 功能不低于 OpenCode Web 版**（官方 Mobile 有但不重要）；现有原生能力全部保留。
5. **OpenCode 直接消费官方 release**：不 fork、不自研 server，只检查官方版本并按市场签名分发。
6. **先选型后动手**；能内化的机制自己写，不把一个 900 MB 的 IDE 或完整 Termux 塞进基础包。

**平台范围**：只支持 **arm64-v8a**（覆盖绝大多数在用 Android 手机/平板），不构建 x86/x86_64；
`minSdk 26`；16 KB 对齐见 §4.2.1。模拟器和 x86 设备不在支持范围，避免多 ABI 让所有包翻倍。

---

## 1. 结论摘要

> **推荐：薄 UI（≈15–20 MB）+ 5 MB 执行引信 + 纯 pack 组件 + 签名市场。**
> 组件不再做成三个无界面 APK，而是**三个（或多个）可增量更新的 pack**；
> 只有确实需要 Android 系统权限/界面的扩展才用 APK。

| 决策 | 选择 | 关键理由 |
|---|---|---|
| 基础包 | Compose UI + R8 + **musl loader / proot / 静态 busybox**（合计约 5 MB） | 没有可执行引信，下载的 ELF 无法跑；引信很小，不违背“UI 版” |
| 组件形态 | **pack 优先**，APK 仅用于 UI/系统集成插件 | pack 不占双份存储、支持 7.6 MB 增量、无安装弹窗、无签名权限耦合 |
| OpenCode | 官方 2.3.2 musl 二进制，**zstd pack 98 MB**，装后 247 MB | strip/UPX 无收益；原生 loader 启动，不上 proot |
| 运行工具 | **0.6 MB 静态 busybox + 系统命令包装器 + Alpine 轻 rootfs** | 305 个 applet 原生可跑；系统命令 0 MB；完整 Linux 才 26 MB |
| 语言 | Python 用 28 MB standalone；Node 用 Alpine 17/47；JVM/构建用 Termux bionic | 避免把 100–250 MB 的 bionic 前缀当默认 |
| 构建 | T1 javac（120/200）→ T2 Kotlin（+62）→ T3 C/C++（+44）→ T4 Gradle（+520） | 先用直调出 APK，AGP 项目再拉 Gradle；SDK 受 EULA 限制必须用户同意后下载 |
| 市场 | 独立 repo `androc-market` + `:market` 客户端库 + Ed25519 签名索引 | 静态 JSON + GitHub Release 足够；不引入 F-Droid/数据库 |
| 内化边界 | 自研市场、安装器、执行引信、pack 格式、命令包装；**不自研 OpenCode server / JDK / Gradle** | 自研 server 需 3–6 人月并永久追协议 |

**总体积直觉**：

```text
纯 UI 安装                  ≈ 15–20 MB（APK）/ 30–40 MB（装后）
+ OpenCode 组件             98 MB 下载 / 247 MB 装后（增量更新 7.6 MB）
+ 运行工具核心              ≈ 0（busybox 随基础包）+ 11/26 MB（Alpine 完整 Linux）
+ Python                    ≈ 28 MB 下载（standalone，免 rootfs）
+ Android 构建 T1           120 MB 下载 / 200 MB 装后（+SDK 30–66 MB）
+ Android 构建 T4           +520 MB 下载 / +1 GB 装后（Gradle/AGP + 缓存）
```

---

## 2. 现状基线（实测）

| 项 | 数值 | 出处 |
|---|---:|---|
| 最新 release APK（bundle） | **950 MB** | GitHub `v0.4.3` |
| **thin release（实测 2026-11-19）** | **7.35 MB** | 含 4 MB 执行引信；远程连接/文件/终端已通 |
| bundle debug（不含 opencode assets） | 33.9 MB | 本机构建 |
| 仓库内 `jniLibs` | 18 MB | `app/src/main/jniLibs` |
| 仓库内 assets（不含生成物） | 424 KB（bundle） | `app/src/bundle/assets`；thin 不打包 |
| opencode 2.3.2 解包 | **247.7 MB**（单 ELF，内嵌 Bun 1.4.4） | 实测 |
| opencode zstd -19 | **98.1 MB** | 实测 |
| 2.3.1→2.3.2 差量 | **7.6 MB（3.1%）** | `zstd --patch-from` 实测 |
| Termux `files/usr` | 1.1 GB | 旧文档 |
| UI 代码 | 29 文件 / 14,036 行 | `ui/` |
| 官方 Mobile | iOS 有（TestFlight）；Android 未发布；**不作对标** | `reference/opencode/services/site/.../product-data.ts` |
| 对标基线 | **OpenCode Web UI** + 本仓库现有 `ui/` | |

**结论**：950 MB 不是 UI 造成的。真正的大头是 opencode 二进制 + Termux 前缀 + JDK/SDK/Gradle；
把它们拆成 pack 后，基础包可以回到十几 MB。

---

## 3. 目标架构

```text
┌──────────────────────────────────────────────────────────────┐
│ 基础 APK（UI 版，≈15–20 MB）                                  │
│  Compose UI + AppModel + OcClient + 通知/配对/分享            │
│  :market 客户端（索引、签名、下载、校验、安装、回滚、增量）   │
│  执行引信（nativeLibraryDir，约 5 MB）：                       │
│    ld-musl 0.72 + libstdc++ 2.82 + libgcc 0.13                │
│    proot 0.26 + proot_loader 0.02 + talloc 0.03 + shmem 0.01  │
│    busybox.static 1.06（305 applet，急救 shell）              │
└───────────────┬──────────────────────────────────────────────┘
                │ AIDL：start/stop/status/pair
                │ HTTP + SSE/PTY：会话数据面
┌───────────────▼──────────────────────────────────────────────┐
│ pack 存储（App 私有 files/，原子切换 + 回滚）                 │
│  kernel-opencode   98 MB dl / 247 MB inst / 7.6 MB delta      │
│  runtime-core      alpine 11 / 26（或 0：busybox 已随基础）    │
│  lang-python       28 MB（standalone，免 rootfs）              │
│  build-t1..t4      120→820 MB dl，分层可删                    │
└───────────────┬──────────────────────────────────────────────┘
                │
┌───────────────▼──────────────────────────────────────────────┐
│ androc-market（独立 GitHub 仓库）                             │
│  index.json + Ed25519 签名 + 每包 release + delta + 镜像      │
└──────────────────────────────────────────────────────────────┘
```

---

## 4. 关键决策

### 4.1 组件形态：pack 优先，APK 只做“需要系统集成”的插件

| 维度 | 无界面 APK（旧方案） | **pack（推荐）** |
|---|---|---|
| 存储 | APK 本体 + 解包 = 双份（OpenCode 366 MB） | 单份（247 MB） |
| 更新 | 整包重下（118 MB） | 差量 7.6 MB，或全量 98 MB |
| 安装 | 系统安装器弹窗、签名权限 | App 内解包/原子切换，无弹窗 |
| 独立进程 | 天然隔离 | 需 App 进程/服务托管（proot 子进程） |
| Android 权限 | 可自己申请 | 只能借用基础 App |
| 适合 | UI 插件、Shizuku/传感器等系统能力 | 运行时、语言、构建链、OpenCode |

**决策**：三个目标组件全部 pack；`RuntimePlugin` AIDL 保留，作为**未来的 Android 集成插件**通道，
不作为 OpenCode/运行工具/构建链的默认形态。

### 4.2 基础包的“执行引信”

Android 10+ 禁止 exec 应用私有目录，`nativeLibraryDir` 是唯一稳定可执行位置。基础包必须自带：

| 文件 | 大小 | 用途 |
|---|---:|---|
| `libld_musl.so` | 723 KB | 启动 opencode / musl 静态/动态工具 |
| `libstdcxx.so` + `libgcc_s.so` | 2.95 MB | opencode 运行期依赖 |
| `libproot.so` + loader + talloc + shmem | 0.32 MB | 复杂用户态/构建链 |
| `libbusybox.so`（static-pie） | 1.06 MB | 305 applet，不依赖 rootfs 的急救 shell |
| 合计 | **≈5.1 MB**（APK 内压缩后约 2–3 MB） | |

- 这 5 MB 是“能力开关”，不是运行时；不装任何组件时，它只提供系统 toybox + busybox。
- 16 KB 对齐：以上文件 `p_align` 均 ≥ 0x4000；CI 继续跑 `check-elf-align.py`（见 §4.2.1）。

### 4.2.1 16 KB 对齐：三条线分开看

> 16 KB 对齐是**安装/面向未来的开发要求**，不是“用 opencode 的要求”。
> 本体和 Android 构建插件必须满足；OpenCode 只是被消费的上游产物，单独检查。

| 范围 | 是否硬性 | 负责方 | 落地 |
|---|---|---|---|
| **Androc 本体**（基础 APK 及全部随包 `.so`） | **硬性**（未来设备安装/升级） | 我们 | 所有 native `p_align ≥ 0x4000`；CI `check-elf-align.py`；APK `zipalign -P 16` |
| **Android 构建插件产出的 APK** | **硬性（默认）** | 我们 + 用户项目 | `zipalign -P 16 -f 4`；NDK r28+；构建后 `check-16kb.py` 失败即报错；技能/文档写清 |
| **OpenCode 官方二进制** | 兼容性检查，**不是我们的发布门槛** | 上游 | 打包时检查（当前 64 KB 对齐）；若某版不合规 → 固定旧版/回滚，不改本体策略 |

- 当前测试机 PKB110 是 **4 KB 页**，只能验证功能，不能验证 16 KB；发布前需要 16 KB 设备/模拟器，
  没有时以 ELF 静态检查 + CI 为准。
- 不为迁就 OpenCode 放宽本体检查；也不因为本体要求去改上游二进制。两条线独立。
- 不再把 opencode 放进 `jniLibs`；loader 可直接映射 `files/runtime/<ver>/opencode`。

### 4.3 运行工具：三层，不从 100 MB 起步

| 层 | 内容 | 下载 | 装后 | 说明 |
|---|---|---:|---:|---|
| **T0 系统** | `/system/bin/toybox` 210 applet + `sh` + `awk` + 系统命令包装 | 0 | 0 | 0BSD；`am/pm/input/screencap/...` 在系统分区 |
| **T0+ 急救** | static busybox 305 applet（随基础包） | 0 | 1.06 | musl static，原生执行，不依赖 proot |
| **T1 完整 Linux** | Alpine 3.24.4 minirootfs + bash/coreutils/git/curl/tar/xz/rg/jq/less/nano | **11.0** | **26.2** | 需要 `apk`、GNU 工具、shebang 时；走 proot |
| **T1-b bionic** | Termux 同等集合（可选） | 20.7 | 100.4 | 只有需要与 JDK/NDK 共用 bionic 时才装 |

- 默认 T0+；用户点“完整命令行”才下 T1（26 MB），不再默认铺 1.1 GB 前缀。
- Android 系统命令：`getprop/cmd -l/pm list/logcat` 普通 uid 可用；`input/screencap/settings/am/dumpsys 明细`
  需要 **Shizuku** 或无线 ADB。UI 上明确标注“需 Shizuku”，不假装可用。
- 命令包装器用脚本生成（`unset LD_LIBRARY_PATH; exec /system/bin/...`），不按命令名维护路由表。

### 4.4 语言运行时

| 语言 | 方案 | 下载/装后 | 结论 |
|---|---|---:|---|
| Python | `python-build-standalone` 3.14 aarch64-musl stripped | 29.4 归档 / 解包约 100 | **首选**；自带 loader，不需要 100 MB rootfs；pip 可用 |
| Python（极简） | microPython | 0.23 / 0.50 | 只做脚本/教学，不装 pip |
| Node | Alpine nodejs 26 | 17.2 / 47.0 | 比 Termux 10.2/50.5 装后更小 |
| Lua / QuickJS | Alpine | 0.08/0.25、0.85/1.75 | 嵌入式脚本/插件沙箱 |
| JVM | Termux openjdk-21 + **jlink 自裁** | 106 / 254 → **约 50–70** | 需要 javac/kotlinc/Gradle；属构建链，不属日常工具 |
| Kotlin | kotlin-compiler-embeddable | 59.7 jar | 依赖 JVM；不单独给“运行工具” |
| C/C++（轻） | tcc | 0.01–0.24 / 1.4 | 只适合 C 子集，不能替代 NDK |
| C/C++（完整） | Alpine gcc 或 zig | 58/160、36/197 | 放构建/原生层，按需 |
| **Rust（可选，重）** | Termux rust 1.100 / Alpine rust 1.96 | Termux **128.2/572.3**；Alpine 69.0/191.1 + gcc 52.5/136.6 ≈ **124/339** | 构建向，不放进日常运行工具；单独 Rust pack |
| 沙箱扩展 | Wasmtime / WasmEdge | 9.3/26.6、0.6/4.0 | 可以预留插件沙箱，不做主力运行时 |

- **ABI 规则**：musl 静态工具原生跑；musl 动态工具走 base 的 loader；bionic 工具（JDK/aapt2）只在 proot；
  不把三种启动方式混成“按命令名路由”。
- **Rust**：只做构建能力，不默认装。Termux `rust`（bionic，128/572）最省事；
  Alpine `rust` + gcc（≈124/339）更小，但要额外的 musl rootfs。二者都放 T3 之后的独立 pack。
- **Python 优先 standalone 而不是 Alpine rootfs**：省 ~70 MB 安装体积，且不要求用户先装完整 Linux。

### 4.5 Android 构建：四层，先小后大

| 层 | 内容 | 下载 | 装后 | 能力 |
|---|---|---:|---:|---|
| **T1** | Termux 基础闭包 + openjdk-21（jlink 后 50–70）+ 自建 aapt2 7.6 + zipalign + d8 20.5 + apksigner 1.2 + android.jar 27.8 | **≈120** | **≈200** | Java 单模块 hello world |
| **T2** | + kotlin-compiler 60.2 + stdlib/reflect/coroutines | +62 | +70 | Kotlin/XML |
| **T3** | + clang 30.7 + cmake/ninja + ndk-sysroot 2.0（不要 413 MB 的 ndk-multilib） | +44 | +520 | JNI / 简单 CMake |
| **T3+ Rust**（可选） | + Termux rust（或 Alpine rust+gcc） | +124–128 | +339–572 | Rust → arm64 APK/JNI |
| **T4** | + Gradle 9.9.1 147 + AGP 传递依赖 + 预热 Maven 缓存 | +520 | +1 GB | Compose/多模块/KSP/AAR |

- **引擎混合**：T1–T3 直调（aapt2 → javac/kotlinc → d8 → zipalign → apksigner），检测到 AGP 项目再拉 T4。
- **SDK 许可**：`android.jar`/platform 不能随包再分发；必须在 App 内弹 EULA 后从 `dl.google.com` 下载，
  只保留 `android.jar` + `core-for-system-modules.jar`（65.9 → 约 30 MB）。
- **自建 aarch64 aapt2**（7.62 MB，16 KB 对齐）替代 Termux 的 16.0.0.4 和 Google 的 x86_64 build-tools。
- **JDK 自裁**：CI 预生成 jlink 镜像（`base, compiler, jdk.compiler, logging, xml, naming, unsupported, zipfs`），
  不必在手机上跑 jlink；省 180–200 MB。
- **W^X**：所有 Termux ELF 继续走 `oc-proot`；不把任何工具放到私有目录直接 exec。
- **16 KB 默认**：构建产物 `zipalign -P 16` + `check-16kb.py` 门禁；这是开发插件的要求，与 OpenCode 的兼容性检查分开（§4.2.1）。
- **T4 缓存**：不预热全量 Maven；空 Activity 模板约 90 MB，Compose 首次联网拉，默认先 `--offline`。

### 4.6 OpenCode 组件

> **策略：完全外包给官方 release。** 不 fork、不改二进制、不自研兼容 server；
> 我们只做“检查版本 → 下载 → 校验 → 分发 → 回滚”。

- 以官方 release 为准：`opencode-linux-arm64-musl.tar.gz`（120.5 MB）→ 二进制 247.7 MB。
- pack 用 **zstd -19**（98.1 MB）；更新用 **zstd --patch-from 差量**（2.3.1→2.3.2 实测 7.6 MB）。
- 启动：`libld_musl.so --library-path … files/runtime/<ver>/opencode serve --hostname 127.0.0.1`。
- 健康检查失败 → 回滚上一版；保留一份旧包；磁盘不足时退全量。
- 不 strip、不 UPX：收益 0.03%，并可能破坏 Bun 的 `$bunfs` 偏移。
- 不自研 server：OpenAPI 139 path / 170 operation，server 3,544 行，core 498 文件；3–6 人月起。
- 许可：opencode MIT；npm 包不带 LICENSE，必须自己附；Bun MIT、musl MIT、libstdc++ GPLv3+Exception、
  JSC LGPL-2.1（原样分发，不改不重链）。
- 更新源优先级：官方 GitHub immutable release + SHA256SUMS；市场再加一层 Ed25519 签名 manifest。

### 4.7 组件市场

**仓库**：`a2heng/androc-market`（独立），只放索引、schema、构建脚本、release 产物，不放 App 代码。

```text
androc-market/
├─ index.json                  # 当前稳定索引（签名）
├─ schema/index.schema.json
├─ packs/<id>/recipe.json      # 版本、URL、sha256、delta、依赖、许可
├─ .github/workflows/release.yml
└─ dist/…                      # Release 附件：*.pack / *.delta / index.json(.sig)
```

**index.json 草案**：

```json
{
  "schema": 1,
  "channel": "stable",
  "generated": "2026-11-19T00:00:00Z",
  "app": { "minVersionCode": 9, "minContracts": "1.0.0" },
  "packs": [
    {
      "id": "kernel-opencode",
      "name": "OpenCode",
      "category": "kernel",
      "version": "2.3.2-1",
      "abi": "arm64-v8a",
      "size": 98086682,
      "installedSize": 247667816,
      "sha256": "…",
      "url": "https://github.com/a2heng/androc-market/releases/download/kernel-2.3.2-1/kernel-opencode.pack",
      "delta": {
        "from": "2.3.1-1",
        "size": 7625027,
        "sha256": "…",
        "url": "…/kernel-2.3.1-1_to_2.3.2-1.zst"
      },
      "mirrors": ["…"],
      "license": ["MIT", "GPL-3.0-with-GCC-exception", "LGPL-2.1"],
      "minAppVersionCode": 9
    }
  ],
  "signature": "ed25519:…"
}
```

**客户端库 `:market`（自研，不引重依赖）**：

```kotlin
interface Market {
    suspend fun refresh(): Index
    fun installed(): List<InstalledPack>
    fun checkUpdate(id: String): Update?
    suspend fun install(id: String, onProgress: (Long, Long) -> Unit)
    suspend fun update(id: String, delta: Boolean = true)
    suspend fun uninstall(id: String)
    fun verify(pack: File): Boolean
}
```

- 签名：Ed25519；公钥进 `:contracts`；minSdk 26 没有平台 Ed25519，用 ~50 KB 的纯 Java 实现，不引 BouncyCastle。
- 校验：`sha256` + `size` + `signature` + `minAppVersionCode` + `abi`；失败删 `.part`。
- 下载：`.part` 续传；失败退镜像；多线程可选。
- 安装：解到 `staging/` → 校验文件清单 → 原子 rename 到 `packs/<id>/<version>/` → 写 `installed.json` → 健康检查 → 失败回滚。
- 差量：候选 `zstd --patch-from`（已验证 3.1%）、hdiffpatch、bsdiff；需在端侧实现 zstd 字典应用
  （纯 Java aircompressor 或小型 JNI）。**未定**，见 §9。
- 卸载：只删该 pack 目录与 skills；项目/会话/其他包不动。
- 市场 UI：分类、版本、体积、许可、签名、来源、更新日志；只读本机时可用。

### 4.8 UI

- 保留 Compose（不换 View），但做瘦身：
  - R8 full mode + `shrinkResources`；
  - `material-icons-extended`（35.7 MB AAR）换 `material-icons-core` + 少量自绘 vector；
  - termlib 只留 `zh/en` 资源；
  - 去掉 2.1 MB 内置字体，终端用系统 `monospace`（或子集化）；
  - 根布局不引 appcompat/material Views。
- 目标：thin release **10–14 MB**，激进 7–9 MB；**实测 7.1 MB**（2026-11-19）。
- 本机组件入口：登录页与设置共用 `LocalRuntimeDialog`。未安装时显示组件体积/来源；
  安装后一键启动，由宿主注入 `127.0.0.1` 地址与密码，用户不需要手填。
- 设置里的插件列表改为**弹框**（不再平铺卡片）；登录页和设置看到的是同一个组件弹框。
- 功能补齐（硬线 = 比 OpenCode Web 好；Mobile 项标“可选”）：
  | 项 | 现状 | 目标 |
  |---|---|---|
  | 完成/权限通知、未读、静音 | 只有保活通知 | v1 硬线 |
  | 多电脑命名/健康/切换 | URL 列表 | v1 硬线 |
  | 分享预览（多文件/选会话/指令） | 文本+单图直接注入 | v1 硬线 |
  | 文件编辑/上传/下载 | 只读 | v1.1 |
  | Git 写操作 | 只读 | v1.1 |
  | i18n（中/英） | 硬编码中文 | v1 |
  | auto-approve/Usage/日志 | 无 | v1 |
  | 配对/QR/一次性链接 | 无 | **可选**（官方 Mobile 不重要） |
  | 语音输入/会话通话 | 无 | **可选**（v1.1 以后） |
- 对标口径：**OpenCode Web UI 是硬线**——我们已有原生 SSE、diff、缓存看板、文件预览、终端、
  多服务器，正常应明显强于 Web；官方 Mobile 有但不作为验收目标。

### 4.9 内化 vs 依赖

| 自己写 | 用上游 |
|---|---|
| 市场索引/签名/安装器/回滚/差量应用 | opencode server（MIT，不自研） |
| pack 格式与执行引信 | JDK/Gradle/Kotlin/clang（不重编） |
| Android 命令包装器、skills | Python/Node/Alpine/Termux 包 |
| AIDL/HTTP 连接与配对 | Android SDK（用户下载，不再分发） |
| UI + 缓存看板 + diff 渲染 | Compose/OkHttp 等基础库 |

**明确不做**：
- 不 fork CodeOnTheGo/AndroidIDE（GPLv3、1.1 GB、targetSdk 28）；
- 不把完整 Termux 当默认前缀；
- 不把 Gradle/AGP 塞进基础包；
- 不自研 opencode 兼容 server（3–6 人月，且永久追协议）；
- 不为省 20 MB 去重链 Bun/JSC（许可与维护成本反而更高）。

---

## 5. 体积预算汇总

| 状态 | 下载 | 安装 | 说明 |
|---|---:|---:|---|
| UI only | 15–20 | 30–40 | 含 5 MB 执行引信 |
| + OpenCode | +98 | +247 | 增量更新 7.6 MB |
| + 完整 Linux（Alpine T1） | +11 | +26 | 可选；busybox 已随基础包 |
| + Python | +28–30 | +100 | standalone，免 rootfs |
| + Node | +17 | +47 | Alpine |
| + JVM（构建用） | +50–106 | +70–254 | jlink 后约 70 |
| + Android 构建 T1 | +120 | +200 | 另有 SDK 30–66 |
| + T4（Gradle/AGP） | +520 | +1,000 | 建议 ≥4 GB 空闲 |

对照：现状 950 MB APK；Dajeovv v2 175 MB + 200 MB toolbox；CoGo 资产 969 MB。

---

## 6. 分阶段落地

| Phase | 内容 | 验收 |
|---|---|---|
| **0 地基** | 修正文档中的“官方 Android”；建 `androc-market` 仓库；`:market` 库骨架；公钥/索引 schema | 索引可签名，客户端能验证 |
| **1 薄 UI** | `:app` 去掉 runtime/Java/assets/jniLibs → 只留 UI + 引信；R8；删重依赖 | APK ≤20 MB；远程连接/会话/文件/终端可用；无 runtime 进程 |
| **2 OpenCode pack** | 二进制入 pack；zstd 下载；loader 原生启动；健康检查/回滚 | 98 MB 安装成功；本机会话可用；断网重装可续传 |
| **3 运行工具** | busybox 随基础包；Alpine T1 pack；Shizuku 检测与提示；Python pack | `busybox` 305 applet；`apk add git` 可用；`python3` 28 MB 可跑 |
| **4 构建链** | T1 直调出 hello APK；T2 Kotlin；T3 C/C++；T4 Gradle | 真机 20 s 内出 APK；T1 ≤220 MB；T4 构建真实项目 |
| **5 UI 补齐** | 通知/未读、多电脑、分享预览、i18n、auto-approve；配对/语音可选 | §7 硬线通过 |
| **6 增量/镜像/社区** | zstd delta、镜像、社区源、APK 插件通道 | 7.6 MB 更新成功；签名错误被拒 |

---

## 7. “UI 比 Web 好”最小验收

### A. 硬线（v1 必须）

1. thin APK ≤20 MB，`unzip -l` 无 opencode/toolbox/JDK。
2. 冷启动 2 s 内出现入口：连服务器 / 本机安装 OpenCode / 市场；不出现“正在启动”死等。
3. 远程 URL+密码连接、12 条保存、杀进程恢复上次、切服务器不串会话。
4. 会话：搜索/分组/运行中置顶/新建目录/重命名/删除/标签恢复。
5. SSE：文本/推理/工具/step 实时；断网 10 s 重连补齐；上滚不顶行。
6. 工具渲染：read/write/edit/shell/grep/glob/diff 折叠/exit code/大输出不 OOM。
7. steer/queue/interrupt；队列撤回；停止确认；对话/文件双回退。
8. 权限 once/always/reject；表单 5 种类型；自定义与选项互斥。
9. 模型 free/go/console 分组 + variant + session/recent/default 优先级；Provider Key/OAuth。
10. 缓存看板口径与 `/api/session` tokens 一致；40 步趋势/成本正确。
11. 文件图片/音频/视频/Markdown/二进制预览；相对/Windows/UNC 路径不 404。
12. Git 工作区/HEAD~1 diff；终端 PTY resize/Ctrl-C/方向键；旋转不丢。
13. 完成/权限通知可点进会话；静音/未读；后台 10 分钟 SSE 不断。
14. 多电脑命名/健康/一键切换；分享多文件可选会话/指令。
15. i18n 中/英跟随系统；auto-approve、Usage、日志入口可用。
16. 同一会话在 Web 与原生两种 UI 下看到的工具/消息一致，无缺项。

### B. 可选（Mobile 能力，不阻塞 v1）

- 配对链接/QR（官方 Mobile 不重要；保留接口，默认隐藏）。
- 语音输入/会话通话。
- 开机自启/看门狗策略 UI（runtime 层已具备接口）。

---

## 8. 风险

| 风险 | 影响 | 对策 |
|---|---|---|
| `files/` 内 ELF 由 loader 映射在 Android 17/16 KB 设备被拒 | OpenCode 起不来 | 兜底 proot；再不行组件 APK 放 nativeLibraryDir |
| zstd 差量端侧实现复杂 | 更新退全量 | 先全量 98 MB；delta 作为 Phase 6 |
| Android SDK EULA | 不能预置 android.jar | App 内 EULA + 官方下载；只留必要文件 |
| Termux/Alpine GPL | 再分发义务 | 附 LICENSE/源码获取方式；优先静态 busybox（GPL-2.0）与 Alpine 索引字段 |
| 4 KB vs 16 KB | 未来 16 KB 设备安装/运行失败 | **本体与构建产物**强制 `p_align ≥ 0x4000` + `zipalign -P 16` + CI 检查；OpenCode 单独做兼容性检查，不混为一条线（§4.2.1） |
| 国产 ROM 杀后台 | OpenCode/构建中断 | 前台服务 + 唤醒锁 + 引导白名单；构建分阶段落盘 |
| 市场被投毒 | 执行恶意代码 | Ed25519 签名 + sha256 + 包内清单 + 可选沙箱 |
| 自研 server 诱惑 | 3–6 人月 | 明确不做；只做可更新组件 |

---

## 9. 已确认 / 待定

已确认（2026-11-19）：

1. 组件形态：**pack 优先**；APK 只留给需要 Android 系统能力的插件。
2. 基础包：接受约 5 MB 执行引信（loader/proot/静态 busybox）。
3. 运行工具默认 rootfs：**Alpine**；bionic 只给 JDK/构建。
4. 基础包目标：≤20 MB；thin 实测 **7.35 MB**。
5. 先做薄 UI，再接市场和组件。
6. 平台：**只做 arm64-v8a**。

仍待定：

1. **市场仓库名**：`androc-market` 还是沿用 `androc-packs`？
2. **差量格式**：zstd patch（已验证 7.6 MB）/ hdiffpatch / 先不做？
3. **Rust 宿主**：Termux bionic（128/572，最省事）还是 Alpine musl（124/339，更小但多一个 rootfs）？

---

## 10. 主要数据来源

- 本地：`reference/opencode`、`reference/opencode-android`、`reference/CodeOnTheGo`、`reference/vexel`、
  `reference/termux-android-studio`、`reference/android-sdk-tools`、`reference/opencode-termux-native`；
  `ui/`、`app/`、`docs/` 全文；设备 `adb` 实测。
- Termux 索引：TUNA 镜像 `Packages.gz`（2026-11-18）。
- Alpine 索引：TUNA `latest-stable`（3.24.4，2026-11-17/18）；minirootfs 4,029,843 B。
- opencode release：GitHub v2.3.2 API（immutable，27 assets，SHA256SUMS）；npm 包与 sigstore attestation。
- python-build-standalone：`20261110`，3.14.10 aarch64-musl stripped 29,427,563 B。
- CoGo 资产：`dev-assets.appdevforall.org` 实测 369.9/80.7/134.5/383.3 MB。
- Gradle 9.9.1 bin 134.9 MB；R8 9.5.33 21.19 MB；android-sdk-tools 14.4 MB（aapt2 6.48）。
