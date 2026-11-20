# 产品分层与市场

本文说明 Androc 的产品分层、两个发行版、安装更新流程和市场包格式。
运行时与路径见 [`paths.md`](paths.md)，插件条目见 [`plugins.md`](plugins.md)，
musl rootfs 的组成与实测见 [`android-musl.md`](android-musl.md)。

---

## 1. 分层

```text
┌────────────────────────────────────────────┐
│ :ui        Compose：连接/设置、会话、聊天、文件、终端 │
├────────────────────────────────────────────┤
│ :app       宿主：启动链路、proot、市场安装、自更新     │
├────────────────────────────────────────────┤
│ proot + Alpine musl rootfs                 │
│   ├─ kernel-opencode：opencode + 基础用户态  │
│   └─ android：JDK/SDK/Gradle/aapt2 + skills │
└────────────────────────────────────────────┘
```

- `:ui` 只依赖 `:contracts`，不启动进程；本机 server、终端、文件根都由宿主注入。
- `:app` 是唯一应用，负责 proot、端口、密码、组件安装和前台服务。
- `:market` 只做签名索引、下载、校验和解压，被 `:app` 与 `:ui` 共用。
- rootfs 只有一套用户态：Alpine musl；Android 工具链装在自己的 Alpine rootfs 里，
  通过 `/opt/androc/env.sh` 暴露 `JAVA_HOME`、`ANDROID_HOME` 和 `PATH`。

---

## 2. 两个发行版

| 发行版 | 内容 | 体积 | 适用 |
|---|---|---:|---|
| **thin** | UI only | 约 7 MB | 连接电脑或远程 opencode；需要本机时从市场装 `kernel-opencode` |
| **bundle** | UI + `assets/runtime/rootfs.tar.xz` + opencode | 约 164 MB | 本机开箱即用，不依赖网络装内核 |

同源规则：

- 同一个 `applicationId`、同一套 `versionCode` / `versionName`；
- 只有资源和 `BuildConfig.THIN` 不同，行为代码不复制；
- 自更新按当前发行版取包：thin 只下 thin APK，bundle 只下 bundle APK；
- bundle 内置的 rootfs 解压一次后，后续以市场组件为准。

---

## 3. 能力与安装位置

| 能力 | 来源 | 安装位置 |
|---|---|---|
| UI、proot 引擎、启动链路 | 随 APK（`nativeLibraryDir`） | APK 本体 |
| OpenCode server | `kernel-opencode` 或 bundle 内置 | `files/runtime/<ver>/`、`files/runtime-core/rootfs` |
| Android 构建链 | `android` 包 | `files/packs/android/<ver>/` |
| skills | 对应包的 `skills/` | 安装后复制到 `$HOME/.config/opencode/skills` 与 `$HOME/.config/opencode-android/skills` |

rootfs 优先级固定为：

```text
files/runtime/<ver>/rootfs
→ files/packs/android/<ver>/rootfs
→ files/runtime-core/rootfs
```

`<ver>` 取目录名字典序最大的一版；`MuslRootfs.dir` 是唯一实现。

---

## 4. 安装与更新

### OpenCode 组件

入口：连接页/设置里的“本机 OpenCode”对话框。

1. 拉取市场 `index.json`，用内置公钥验证 `index.json.sig`；
2. 取 `kernel-opencode`，下载 `archive.url` 到 `files/market`；
3. 校验压缩包 size/sha256，解压成 tar，再校验 `installed` 的 size/sha256；
4. 解到 `files/runtime/<upstreamVersion>.tmp`，成功后改名为正式目录；
5. 已经安装时可直接“启动并登录”；市场版本更新时对话框显示“更新”。

bundle 第一次启动本机时走 `RuntimeAssets.ensure()`：把内置 rootfs 解到
`files/runtime-core/rootfs`，写 `.installed-v2` 标记；已经解压则跳过。

### Android 组件

入口：设置 → 插件 → Android。

1. 同一个签名索引里取 `android`；
2. 下载、校验、解压到 `files/packs/android/<version>/`；
3. 把包内 `skills/` 复制到两个 opencode 配置目录；
4. 卸载只删 `files/packs/android` 与对应 skills，不动会话、项目、其他组件。

### App 本体

- 检查 GitHub Releases 中同发行版的 APK；
- 下载到 `cache/update/`，HTTP/1.1、失败重试、`Range` 续传；
- 交给系统安装器；安装完成后旧 APK 由系统替换。

### 失败处理

- 下载写 `.part`，校验通过才改名；
- 解压先进临时目录，成功后原子改名；
- 校验失败删除临时文件，保留已安装版本；
- 服务启动失败在对话框和日志里给出尾部输出。

---

## 5. 市场包格式

市场仓库只放索引、签名、构建脚本和发布产物：

```text
androc-market/
├─ index.json
├─ index.json.sig
├─ keys/
├─ packs/
│  └─ android/
│     ├─ pack.json
│     ├─ skills/
│     └─ tools/
└─ scripts/
```

索引条目：

```json
{
  "id": "kernel-opencode",
  "name": "OpenCode",
  "category": "kernel",
  "kind": "tar",
  "version": "2.3.2-2",
  "upstreamVersion": "2.3.2",
  "abi": "arm64-v8a",
  "minAppVersionCode": 9,
  "summary": "opencode binary + musl runtime (bash/git/python/node)",
  "installed": {
    "size": 429854720,
    "sha256": "…"
  },
  "archive": {
    "url": "https://…/kernel-opencode-2.3.2-2.tar.xz",
    "size": 128788404,
    "sha256": "…",
    "format": "xz"
  },
  "delta": null
}
```

字段约定：

| 字段 | 含义 |
|---|---|
| `kind` | 固定 `tar`；目录包按 tar 安装 |
| `version` | 包版本；安装目录用它 |
| `upstreamVersion` | 上游 opencode 版本；`files/runtime/<ver>` 用它 |
| `abi` | 当前只有 `arm64-v8a`，不匹配直接拒绝 |
| `installed.size` / `installed.sha256` | **未压缩 tar** 的字节数与哈希 |
| `archive.size` / `archive.sha256` | 下载到的 `.tar.xz` 的字节数与哈希 |
| `delta` | 固定 `null`：每次全量下载 |

签名规则：

- `index.json.sig` 是 Ed25519 对 `index.json` **原始字节**的签名；
- 公钥编译进 App（`MarketClient.DEFAULT_PUBLIC_KEY`）；
- 默认索引强制验签；调试/镜像地址可经 `MarketConfig` 覆盖，覆盖时按调试路径处理。

安装布局：

```text
files/
├─ market/                      # 下载缓存
├─ runtime/<upstreamVersion>/   # kernel-opencode：opencode + rootfs
├─ runtime-core/rootfs/         # bundle 内置解压
└─ packs/android/<version>/     # android：skills + rootfs
```

版本与兼容：

- opencode 版本唯一来源是仓库根目录 `opencode.version`；
- 发版前 `python scripts/check-opencode-latest.py --strict` 必须通过；
- `minAppVersionCode` 声明该包要求的最低 App `versionCode`；
- 组件可以独立更新，不需要重新发 APK。

---

## 6. 卸载与占用

- 卸载 `android`：删 `files/packs/android` 和对应 skills；OpenCode、会话、
  项目文件保留。
- `opencode` 是运行中的内核，卸载按钮不可用；停止本机 server 后由更新流程替换。
- 下载缓存 `files/market` 可随时清理；已安装版本不受影响。
- 旧版本目录按版本号保留，排查回滚时可直接看 `files/runtime` 与
  `files/packs/android`。

---

## 7. 版本

| 对象 | 例子 | 规则 |
|---|---|---|
| App | `0.6.2` | 一个 `versionCode`，thin/bundle 共用 |
| opencode | `2.3.2` | 上游稳定版，写在 `opencode.version` |
| `kernel-opencode` | `2.3.2-2` | `upstreamVersion` + 包修订号 |
| `android` | `2-3` | 工具链批次 + 包修订号 |

构建与发布命令见 [`commands.md`](commands.md)，发布说明只保留当前版本。
