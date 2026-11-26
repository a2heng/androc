# Androc 路径、挂载与环境

Androc 只有一个运行时：**proot + Alpine musl rootfs**。所有目录都在 App 私有目录内，
`files/home` 就是 `$HOME`；proot 把 `$HOME` 按原绝对路径 bind 进 rootfs，宿主和沙箱里看到的是同一路径。

## 1. 三个发行版

| 发行版 | applicationId 后缀 | APK 内容 | 首次启动 |
|---|---|---|---|
| `ui` | `.ui` | 只有 UI | 只连远程；不带 runtime |
| `oc` | （无） | ui 全部 + `assets/runtime/rootfs.tar.xz` + `libopencode2_bin.so` | 装完即可离线起本机 OpenCode |
| `apk` | `.apk` | oc 全部 + `assets/android/pack.tar.xz` | 装完即可离线起本机 OpenCode + Android 工具链 |

三个发行版可并存安装。`applicationId` 为 `com.a2heng.androc`（debug `.debug`，beta `.beta`，ui/apk 再加 flavor 后缀）。

## 2. `files/` 目录

`<files>` = `Context.getFilesDir()`。

| 路径 | 内容 | 写入方 |
|---|---|---|
| `<files>/home` | `$HOME` | `LocalRuntime` |
| `<files>/runtime-core/rootfs/` | oc/apk 内置 rootfs，只解压一次 | `RuntimeAssets.ensure` |
| `<files>/runtime-core/.installed-v2` | 内置 runtime 解压完成标记 | `RuntimeAssets` |
| `<files>/packs/android/bundled/` | apk 内置 Android 包：`skills/` + `rootfs/` | `AndroidAssets.ensure` |
| `<files>/packs/<id>/<ver>/` | 兼容旧安装的目录包 | 历史/兼容 |
| `<files>/logs/androc.log` | 应用与运行时日志，512 KB 轮转 | `AppLog` |
| `<files>/logs/thin-runtime.log` | thin 启动器日志 | `ThinRuntime` |
| `<files>/lib/` | loader 库目录 | `LocalRuntime` / `ThinRuntime` |

缓存目录（`cache/`）：

| 路径 | 作用 |
|---|---|
| `cache/tmp` | `TMPDIR`；沙箱 `/tmp` |
| `cache/tmp/proot` | `PROOT_TMP_DIR` |

`nativeLibraryDir` 是唯一允许 `exec` 的目录，随安装/升级变化，只能运行时读取。

## 3. `$HOME` 布局

`LocalRuntime.homeDir()` 返回 `<files>/home` 的 canonical path。

| 路径 | 作用 |
|---|---|
| `$HOME/workspace` | 默认项目根 |
| `$HOME/.config/opencode` | 用户配置：`opencode.jsonc`、`tools/`、`skills/` |
| `$HOME/.config/opencode-android` | `OPENCODE_CONFIG_DIR`：`opencode.json` + `skills/` |
| `$HOME/.local/share` | `XDG_DATA_HOME` |
| `$HOME/.cache` | `XDG_CACHE_HOME` |
| `$HOME/.local/state` | `XDG_STATE_HOME` |
| `$HOME/.run` | `XDG_RUNTIME_DIR` |
| `$HOME/.oc-etc/hosts` | 每次启动重写，bind 到 `/etc/hosts` |
| `$HOME/.oc-etc/resolv.conf` | 每次启动重写，bind 到 `/etc/resolv.conf` |
| `$HOME/.gitconfig` | 启动时追加 `[safe] directory = *` |

## 4. rootfs 来源与优先级

`MuslRootfs.dir(ctx)` 按顺序取第一个存在的 rootfs：

| 顺序 | 路径 | 来源 |
|---:|---|---|
| 1 | `<files>/packs/android/bundled/rootfs` | apk flavor 内置 Android 包 |
| 2 | `<files>/packs/android/<ver>/rootfs` | 兼容旧安装 |
| 3 | `<files>/runtime/<ver>/rootfs` | 兼容旧运行时目录 |
| 4 | `<files>/runtime-core/rootfs` | oc/apk 内置 runtime |
| 5 | `<files>/packs/runtime-core/<ver>/rootfs` | 兼容旧包 |

## 5. proot 挂载

服务器/终端用 `LocalRuntime.prootCmd`：

| 沙箱路径 | 宿主路径 |
|---|---|
| `/` | rootfs |
| `$HOME` | `$HOME`（绝对路径保持一致） |
| `/opt/androc/lib` | `nativeLibraryDir` |
| `/dev` `/proc` `/sys` `/system` | 同名 |
| `/etc/hosts` | `$HOME/.oc-etc/hosts` |
| `/etc/resolv.conf` | `$HOME/.oc-etc/resolv.conf` |

## 6. 关键环境变量

`PATH` 包含 `/opt/android-sdk/*`、`/opt/gradle/gradle-9.9.0/bin`、常规系统路径。
`JAVA_HOME=/usr/lib/jvm/java-21-openjdk`，`ANDROID_HOME=/opt/android-sdk`。
`OPENCODE_SERVER_USERNAME=opencode`，密码随机生成存 prefs。

## 7. 启动与自愈

`LocalServerService` → `RuntimeAssets.ensure` → `AndroidAssets.ensure`（apk）→ `LocalRuntime.start`：

1. 清理遗留 git/缓存/半成品 rootfs
2. 建 `$HOME` 目录与配置
3. 写 `opencode.jsonc`、`opencode-android/opencode.json`、skills、hosts/resolv
4. 用 proot 启动 `opencode serve --hostname 127.0.0.1 --port <SERVER_PORT>`

## 8. 不变量

1. 一个运行时：proot + musl rootfs。
2. rootfs 优先级唯一：见 §4。
3. `$HOME` 只有一套：`files/home`。
4. 路径用运行时值，不写死包名。
5. 不使用市场；组件随 APK flavor 内置。
6. 端口固定：4096（release）/4098（debug/beta/thin），调试 API 42100。
