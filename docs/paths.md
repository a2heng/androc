# Androc 路径、挂载与环境

Androc 只有一个运行时：**proot + Alpine musl rootfs**。所有目录都在 App 私有
目录内，`files/home` 就是 `$HOME`；proot 把 `$HOME` 按原绝对路径 bind 进 rootfs，
所以宿主和沙箱里看到的是同一条路径。

配套：[`commands.md`](commands.md) 说明构建、调试、发布命令；
[`android-musl.md`](android-musl.md) 说明 musl 迁移与真机实测。

---

## 1. 两个发行版

| 发行版 | APK 内容 | release 体积 | 首次启动 |
|---|---|---:|---|
| `thin` | UI + `nativeLibraryDir` 里的 proot / loader / musl loader | 约 7 MB | 只连远程；不带 runtime、不带市场 |
| `bundle` | thin 的全部 + `assets/runtime/rootfs.tar.xz` + `libopencode2_bin.so` | 约 164 MB | 装完即可离线起本机 OpenCode |

| 项 | 值 |
|---|---|
| applicationId | `com.a2heng.androc`（debug `.debug`，beta `.beta`） |
| minSdk / targetSdk / compileSdk | 26 / 34 / 37 |
| ABI | `arm64-v8a` |
| 内置 OpenCode 版本 | 由 `opencode.version` 决定（当前 `2.3.2`），Gradle 写入 `BuildConfig.OPENCODE_VERSION` |

---

## 2. `files/` 目录总表

`<files>` = `Context.getFilesDir()`，即 `/data/data/<pkg>/files`（canonical 形式）。

| 路径 | 内容 | 写入方 |
|---|---|---|
| `<files>/home` | `$HOME`，见 §3 | `LocalRuntime` |
| `<files>/runtime/<upstream>/` | 市场 `kernel-opencode` 包：`opencode` + `rootfs/` | `MarketClient.installOpencode`、`RuntimeUpdateService` |
| `<files>/runtime/<upstream>.tmp/` | 解包临时目录，完成后重命名为 `<upstream>` | 同上 |
| `<files>/runtime-core/rootfs/` | bundle 内置 rootfs，只解压一次 | `RuntimeAssets.ensure`（由 `LocalServerService` 调用） |
| `<files>/runtime-core/.installed-v2` | 内置 runtime 解压完成标记 | `RuntimeAssets` |
| `<files>/packs/<id>/<ver>/` | 市场目录包：`android` = `skills/` + `rootfs/` | `MarketClient.installPack` |
| `<files>/packs/<id>/<id>-<ver>.tar.tmp` | 解压出的 tar（校验后删除） | 同上 |
| `<files>/packs/<id>/<id>-<ver>.tmp/` | 解包临时目录，完成后重命名为 `<ver>` | 同上 |
| `<files>/market/` | 下载缓存：`opencode-<packver>.xz`、`<id>-<ver>.tar.xz`、`.part` | `MarketClient` |
| `<files>/logs/androc.log` | 应用与运行时日志，512 KB 轮转为 `androc.log.1` | `AppLog` |
| `<files>/logs/thin-runtime.log` | thin 启动器拉起的 server 日志 | `ThinRuntime` |
| `<files>/lib/` | loader 库目录；thin 启动器在这里放 `libstdc++.so.6`、`libgcc_s.so.1` | `LocalRuntime` / `ThinRuntime` |

缓存目录（`Context.getCacheDir()` = `/data/data/<pkg>/cache`）：

| 路径 | 作用 |
|---|---|
| `cache/tmp` | `TMPDIR`；沙箱里的 `/tmp` 就是它 |
| `cache/tmp/proot` | `PROOT_TMP_DIR`（`LocalRuntime` 启动的 proot） |
| `cache/tmp/musl` | `PROOT_TMP_DIR`（`MuslRootfs.run` 调试通道） |

`nativeLibraryDir` 是唯一允许 `exec` 的目录，随每次安装/升级变化，只能运行时读取：

| 文件 | 用途 |
|---|---|
| `libproot.so` | proot 本体 |
| `libproot_loader.so` | `PROOT_LOADER` |
| `libld_musl.so` | musl loader（沙箱内 ELF 的解释器） |
| `libtalloc.so`、`libandroid-shmem.so`、`libstdcxx.so`、`libgcc_s.so` | proot / loader 依赖 |
| `libopencode2_bin.so` | bundle 内置的 opencode 二进制 |

---

## 3. `$HOME` 布局

`LocalRuntime.homeDir()` 返回 `<files>/home` 的 **canonical path**（通常是
`/data/data/<pkg>/files/home`，不是 `/data/user/0/...`）。字符串比较必须用
canonical 形式。

| 路径 | 作用 |
|---|---|
| `$HOME/workspace` | 默认项目根；服务器启动时保证存在 |
| `$HOME/.config` | `XDG_CONFIG_HOME` |
| `$HOME/.config/opencode` | 用户配置：`opencode.jsonc`（`autoupdate:false`）、`tools/grep.ts`、`tools/glob.ts`、`skills/` |
| `$HOME/.config/opencode-android` | `OPENCODE_CONFIG_DIR`：`opencode.json` 只含 `autoupdate:false` 与 `"shell":"/bin/bash"`；`skills/` 是同一批 skills 的副本 |
| `$HOME/.local/share` | `XDG_DATA_HOME`：OpenCode DB、会话、日志 |
| `$HOME/.cache` | `XDG_CACHE_HOME` |
| `$HOME/.local/state` | `XDG_STATE_HOME` |
| `$HOME/.run` | `XDG_RUNTIME_DIR` |
| `$HOME/.oc-etc/hosts` | 每次启动重写，bind 到沙箱 `/etc/hosts` |
| `$HOME/.oc-etc/resolv.conf` | 每次启动重写（`1.1.1.1`、`8.8.8.8`），bind 到 `/etc/resolv.conf` |
| `$HOME/.gitconfig` | 启动时追加 `[safe] directory = *` |
| `$HOME/sdcard` | 有 root 时 `mount --bind /storage/emulated/0`；无 root 时保留空目录 |
| `$HOME/lib` | 运行时创建，留给 loader 的库目录 |

skills 的两条来源，都会落到上面两个 `skills/` 目录：

| 来源 | 内容 | 代码 |
|---|---|---|
| APK `assets/skills/` | 核心集（如 `skill-installer`），受插件开关控制 | `LocalRuntime.installSkills` |
| 市场 `android` 包的 `skills/` | android-* 全套 | `PackInstaller.copySkills`（安装插件时） |

---

## 4. rootfs 来源与优先级

`MuslRootfs.dir(ctx)` 按顺序取第一个存在的 rootfs：

| 顺序 | 路径 | 来源 |
|---:|---|---|
| 1 | `<files>/packs/android/<ver>/rootfs` | Android 包（skills + JDK/SDK/Gradle），装了就优先 |
| 2 | `<files>/runtime/<ver>/rootfs` | 市场 `kernel-opencode` 包（同目录还有 `opencode`） |
| 3 | `<files>/runtime-core/rootfs` | bundle 内置 `assets/runtime/rootfs.tar.xz` 解压 |
| 4 | `<files>/packs/runtime-core/<ver>/rootfs` | 兼容旧市场包 |

同一层有多个版本目录时取**字符串最大**的版本名。运行时可用条件：
`prootAvailable()`（§2 的 8 个 `.so` 全部存在）且 `MuslRootfs.dir() != null`；
否则 `LocalRuntime.start` 直接报错，不会退回其它执行方式。

---

## 5. proot 挂载表

### 5.1 服务器 / 终端（`LocalRuntime.prootCmd`）

| 沙箱路径 | 宿主路径 | 说明 |
|---|---|---|
| `/` | rootfs | `proot -r` |
| `$HOME` | `$HOME` | 绝对路径保持一致，会话/项目路径不用换算 |
| `/opt/androc/lib` | `nativeLibraryDir` | proot 依赖库、内置 `opencode` |
| `/opt/androc/bin` | 市场二进制的目录（`files/runtime/<ver>`） | 仅当二进制不在 `nativeLibraryDir` 时挂载 |
| `/dev`、`/proc`、`/sys` | 同名 | 设备、进程、系统信息 |
| `/system` | `/system` | 设备命令（`/system/bin/am`、`logcat`…） |
| `/etc/hosts` | `$HOME/.oc-etc/hosts` | 固定 opencode / models 的 IPv4 |
| `/etc/resolv.conf` | `$HOME/.oc-etc/resolv.conf` | DNS |

工作目录用 `proot -w` 指定：服务器和本机终端是 `$HOME`；agent 的每条命令由
OpenCode 自己切到会话目录，仍然在同一个 proot 命名空间里。

### 5.2 调试通道（`MuslRootfs.command`）

debug API `GET /musl?cmd=` 用最小挂载跑一次性命令：

| 沙箱路径 | 宿主路径 |
|---|---|
| `$HOME` | `$HOME`（不存在时先在 rootfs 里建同名空目录） |
| `/dev`、`/proc`、`/sys`、`/system` | 同名 |
| `/etc/hosts`、`/etc/resolv.conf` | `$HOME/.oc-etc/*`（存在才挂） |

环境只设 `LD_LIBRARY_PATH=nativeLibraryDir`、`PROOT_LOADER`、`PROOT_TMP_DIR`，
入口是 `/bin/sh -lc '<cmd>'`。

### 5.3 rootfs 内的固定路径（Android 包提供）

| 路径 | 内容 |
|---|---|
| `/opt/android-sdk` | `platform-tools`、`build-tools/36.0.0`（静态 aarch64 aapt2/adb） |
| `/opt/gradle/gradle-9.9.0` | Gradle 发行版，`bin/` 在 `PATH` 里 |
| `/usr/lib/jvm/java-21-openjdk` | `JAVA_HOME` |
| `/opt/androc/env.sh` | 导出 `PATH`、`JAVA_HOME`、`LD_PRELOAD=/usr/local/lib/fixexec.so` |
| `/usr/local/lib/fixexec.so` | JVM 的 `mprotect(RW→RWX)` shim |

---

## 6. 沙箱环境变量

`LocalRuntime.buildEnv` + `startProot` 注入，`ProcessBuilder.environment()` 会先
清空再整体写入：

| 变量 | 值 |
|---|---|
| `HOME` | `$HOME`（`<files>/home`） |
| `PWD` | `$HOME` |
| `TMPDIR` | `cache/tmp` |
| `XDG_CONFIG_HOME` / `XDG_DATA_HOME` / `XDG_CACHE_HOME` / `XDG_STATE_HOME` / `XDG_RUNTIME_DIR` | 见 §3 |
| `PATH` | `/opt/android-sdk/build-tools/36.0.0:/opt/android-sdk/platform-tools:/opt/gradle/gradle-9.9.0/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin` |
| `SHELL` | `/bin/bash` |
| `LANG` / `LC_ALL` | `C.UTF-8` |
| `TERM` | `xterm-256color` |
| `USER` / `LOGNAME` | `u0_a<uid % 100000>` |
| `NO_COLOR` / `CI` | `1` / `1` |
| `JAVA_HOME` | `/usr/lib/jvm/java-21-openjdk` |
| `ANDROID_HOME` / `ANDROID_SDK_ROOT` | `/opt/android-sdk` |
| `ANDROID_ROOT` / `ANDROID_DATA` | `/system` / `/data` |
| `OPENCODE_DISABLE_AUTOUPDATE` | `1` |
| `OPENCODE_RUNTIME` / `OC_ENGINE` | `proot` |
| `OPENCODE_NATIVE_LIB_DIR` | `/opt/androc/lib` |
| `LD_LIBRARY_PATH` | `nativeLibraryDir:/opt/androc/lib`（proot 本体在宿主侧需要） |
| `PROOT_LOADER` / `PROOT_TMP_DIR` | `libproot_loader.so` / `cache/tmp/proot` |
| `OPENCODE_SERVER_USERNAME` | `opencode` |
| `OPENCODE_SERVER_PASSWORD` / `OPENCODE_PASSWORD` | 首次启动生成的 32 字节随机串（Base64URL），存 `SharedPreferences("app").local_server_password` |
| `OPENCODE_CONFIG_DIR` | `$HOME/.config/opencode-android` |

`/opt/androc/env.sh` 是 Android 包给**构建**用的补充入口：`source` 之后
`JAVA_HOME`、`ANDROID_HOME`、`LD_PRELOAD` 都指向 §5.3 的路径。

---

## 7. 市场包布局

索引：`https://raw.githubusercontent.com/a2heng/androc-market/main/index.json`
（`MarketClient.DEFAULT_INDEX_URL`），签名是同目录 `index.json.sig`，Ed25519；
只有默认索引强制验签，自定义镜像（`MarketConfig`）跳过验签。包本身按
SHA-256 校验，**全量下载，无增量**（`delta` 字段只解析不使用）。

| 包 | 内容 | 安装结果 |
|---|---|---|
| `kernel-opencode` | `opencode` + `rootfs/`（tar.xz，`kind=tar`） | `<files>/runtime/<upstreamVersion>/` |
| `android` | `skills/` + `rootfs/`（JDK 21 / SDK 36 / Gradle 9.9 / aapt2 / adb） | `<files>/packs/android/<version>/`；skills 复制到两个 config 目录 |

`abi` 只接受 `arm64-v8a`。下载先写 `<name>.part`，长度与 SHA-256 都匹配后
才重命名；已经存在且校验通过的缓存直接复用。

---

## 8. 端口与监听

| 监听 | 端口 | 说明 |
|---|---|---|
| 本机 OpenCode server | `127.0.0.1:4096`（release） | `BuildConfig.SERVER_PORT` |
| 同上的 debug / beta | `127.0.0.1:4098` | 见 `app/build.gradle` 的 `debug` / `beta` |
| thin 启动器 | `127.0.0.1:4098`–`4199` | 从空闲端口里挑一个，存 `SharedPreferences("thin_runtime").port` |
| 调试 API | `127.0.0.1:42100` | 仅回环，头 `X-Androc-Token: androc-local-dev` |
| 出站代理（thin） | `127.0.0.1:<随机>` | `LocalNetworkProxy`，用作 `HTTP(S)_PROXY` |
| 服务器发现的 ContentProvider | `content://<applicationId>.local-server/info` | 返回 `url`、`username`、`password`、`running`、`port` |

服务器认证是 Basic：用户名 `opencode`，密码来自 §6。

---

## 9. 启动顺序与自愈

`LocalServerService` → `RuntimeAssets.ensure`（只有 bundle 有 asset）→
`ensureSdcardMount`（有 root 才成功）→ `LocalRuntime.start`：

1. `cleanManagedGit`：删遗留目录（`$HOME/toolchain`、`files/usr`）里的 `.git`；对
   `$HOME`、`workspace`、`projects`、`.config`、`.local` 只删空仓库或
   `Androc project:` 自动提交生成的仓库；
2. `pruneTransientCaches`：清 `$HOME/.cache/tmp`、opencode 日志、Gradle daemon
   里 7 天前的文件，删遗留的 `toolchain.tmp` 与包缓存；
3. `pruneGeneratedRootfs`：`$HOME/rootfs` 全是符号链接时整目录删除；
4. 建 `$HOME/workspace`、`$HOME/.config/opencode`、`$HOME/.local/*` 等；
5. 写 `opencode.jsonc`、`opencode-android/opencode.json`、`.gitconfig`、插件
   `tools/grep.ts` + `glob.ts`、skills；
6. 重写 `.oc-etc/hosts` 与 `resolv.conf`；
7. 用 `proot ... opencode serve --hostname 127.0.0.1 --port <SERVER_PORT>` 启动。

安装/更新 OpenCode 走 `RuntimeUpdateService`：从市场装到
`<files>/runtime/<ver>/`，写 `opencode_runtime_version` / `opencode_runtime_path`，
重启服务并做健康检查；失败则回滚到旧二进制。

---

## 10. 不变量

1. **一个运行时**：服务器、终端、agent、构建全部在同一个 proot + musl rootfs 里。
2. **rootfs 优先级唯一**：`files/runtime/<ver>/rootfs` → `files/runtime-core/rootfs`
   → `files/packs/*/rootfs`；新增来源要同步 `MuslRootfs.dir` 和本页。
3. **`$HOME` 只有一套**：`files/home`，沙箱里不改名、不映射到 `/home`。
4. **路径用运行时值**：`nativeLibraryDir`、`$HOME`、`$PWD` 都在运行时取，不写死包名。
5. **市场只有两个包**：`kernel-opencode`、`android`；索引必须验签。
6. **下载可续、安装可重试**：`.part` 续传、SHA-256 校验、临时目录改名落盘。
7. **端口固定**：4096（release）/ 4098（debug、beta、thin），调试 API 42100。

---

## 11. 代码对照

| 责任 | 文件 |
|---|---|
| `$HOME`、proot 启动、挂载、环境变量、清理 | `app/src/main/java/com/a2heng/androc/LocalRuntime.java` |
| rootfs 选择 | `app/src/main/java/com/a2heng/androc/MuslRootfs.java` |
| bundle 内置 runtime 解压 | `app/src/main/java/com/a2heng/androc/RuntimeAssets.java` |
| 市场包目录与 skills 复制 | `app/src/main/java/com/a2heng/androc/PackInstaller.java` |
| 索引/验签/下载/解包 | `market/src/main/kotlin/com/a2heng/androc/market/MarketClient.kt` |
| 市场地址覆盖 | `app/src/main/java/com/a2heng/androc/MarketConfig.java` |
| 调试 API | `app/src/main/java/com/a2heng/androc/AutomationServer.java` |
| 日志 | `app/src/main/java/com/a2heng/androc/AppLog.java` |
| 端口、发行版、签名 | `app/build.gradle` |
