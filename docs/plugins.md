# 组件与发行版

Androc 不再使用市场。组件随 APK flavor 内置：UI 只负责连接、会话和展示，OpenCode 与 Android 工具由对应 APK 提供。

## 1. 发行版

| 发行版 | applicationId 后缀 | APK 内容 | 本机能力 |
|---|---|---|---|
| `ui` | `.ui` | 只有 UI | 只连远程 |
| `oc` | （无） | UI + OpenCode + musl rootfs | 本机 OpenCode |
| `apk` | `.apk` | `oc` + Android 工具链 | 本机 OpenCode + Android 构建/调试 |

三个发行版可并存安装，共用同一套代码结构，但数据目录按 applicationId 隔离。

## 2. 组件

| 组件 | 来源 | 说明 |
|---|---|---|
| OpenCode 运行时 | `oc`/`apk` 内置 | `libopencode2_bin.so` + `assets/runtime/rootfs.tar.xz`，首次启动解压到 `files/runtime-core/rootfs` |
| Android 工具链 | `apk` 内置 | `assets/android/pack.tar.xz`，首次启动解压到 `files/packs/android/bundled/`；包含 `skills/` + rootfs（JDK 21、Android SDK 36、Gradle 9.9、静态 aarch64 aapt2/adb） |

`apk` 解压 Android 包后，skills 复制到两个配置目录：

- `$HOME/.config/opencode/skills`
- `$HOME/.config/opencode-android/skills`

rootfs 里的 Android 工具通过 `/opt/androc/env.sh` 加入 `PATH`。

## 3. 状态判定

| 组件 | 已就绪 | 版本 |
|---|---|---|
| OpenCode | `oc`/`apk` 内置 runtime 可用 | `BuildConfig.OPENCODE_VERSION` |
| Android | `apk` 且 rootfs/skills 就绪 | rootfs 所在版本目录名（bundled） |

## 4. 管理入口

| 入口 | 作用 |
|---|---|
| 登录页 → 本机 | `oc`/`apk` 可启动本机 OpenCode 并登录 |
| 设置 → 组件与工具链 | 只读展示内置组件状态（`apk` 显示 Android 工具链） |
| 远程连接 | 只管理当前服务器的会话 |

## 5. 代码位置

| 责任 | 文件 |
|---|---|
| 组件状态 | `app/src/main/java/com/a2heng/androc/AppPluginManager.java` |
| skill → 组件映射 | `app/src/main/java/com/a2heng/androc/PluginCatalog.java` |
| Android 包解压与 skills 复制 | `app/src/main/java/com/a2heng/androc/AndroidAssets.java` |
| runtime rootfs 解压 | `app/src/main/java/com/a2heng/androc/RuntimeAssets.java` |
| rootfs 定位与 proot 命令 | `app/src/main/java/com/a2heng/androc/MuslRootfs.java` |
| 本机启动 | `app/src/main/java/com/a2heng/androc/LocalRuntime.java`、`ui/src/main/java/com/a2heng/androc/ui/data/LocalRuntimeLauncher.kt` |
