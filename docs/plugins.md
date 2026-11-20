# 插件与组件

Androc 用组件承载本机能力：UI 只负责连接、会话和展示，OpenCode 与 Android 工具由市场包提供。

## 1. 发行版

| 发行版 | APK 内容 | 本机能力来源 |
|---|---|---|
| thin | 只有 UI | 无市场，只连远程 |
| bundle | UI + OpenCode + musl rootfs | 开箱可用；市场有更高版本时仍可更新 |

两个发行版共用同一套代码、路径和配置目录。

## 2. 组件清单

`AppPluginManager` 只暴露两个条目。

| id | 名称 | 内容 | 市场包 | 安装位置 |
|---|---|---|---|---|
| `opencode` | OpenCode 内核 | opencode 二进制 + Alpine musl rootfs | `kernel-opencode` | `files/runtime/<upstreamVersion>/`，含 `opencode` 与 `rootfs/` |
| `android` | Android 开发 | `android-*` skills + rootfs（JDK 21、Android SDK 36、Gradle 9.9、静态 aarch64 aapt2/adb） | `android` | `files/packs/android/<version>/` |

`android` 安装完成后，skills 额外复制到两个配置目录：

- `$HOME/.config/opencode/skills`
- `$HOME/.config/opencode-android/skills`

rootfs 里的 Android 工具通过 `/opt/androc/env.sh` 加入 `PATH`。

## 3. 状态判定

| 组件 | 已安装 | 版本 | 体积 |
|---|---|---|---|
| `opencode` | bundle 内置二进制 + runtime | `BuildConfig.OPENCODE_VERSION`（随 App） | 当前 opencode 二进制 |
| `android` | rootfs 目录存在，且 `$HOME/.config/opencode/skills` 至少有一个对应 skill | rootfs 所在版本目录名 | rootfs + skills 合计 |

## 4. 安装与更新

| 动作 | `opencode` | `android` |
|---|---|---|
| 安装 | bundle：设置 → 市场 → 安装 `android` | 设置 → 组件与工具链 → 安装 |
| 更新 | 检查更新 → 更新；`RuntimeUpdateService` 从市场下载 | 再次从市场安装同一包 |
| 卸载 | —（随 APK 或市场更新） | 删除 `files/packs/android` 与两个配置目录里的 skills |

### 4.1 `opencode` 更新流程

1. 读取市场索引，取 `kernel-opencode`，比较 `upstreamVersion` 与当前版本。
2. 下载 `tar.xz`，校验 archive 的 `size`/`sha256`。
3. 解压到 `files/runtime/<version>.tmp`，校验未压缩 tar 的 `size`/`sha256` 后替换目标目录。
4. 停止本机服务，用新二进制重启。
5. 30 秒内 `/global/health` 返回目标版本即成功；写入偏好并删除其它 runtime 版本目录。
6. 启动失败则删除新目录、恢复原版本并重启服务。

bundle 内置版本低于市场版本时走同一条更新流程；市场目录优先于内置版本。

### 4.2 `android` 安装流程

1. `PackInstaller` 读取市场 `android` 包。
2. 解压到 `files/packs/android/<version>/`。
3. 把 `<pack>/skills/*` 复制到两个配置目录的 `skills/`。
4. 至少一个 skill 就位才算安装成功；失败保留已安装版本。

## 5. 管理入口

| 入口 | 作用 |
|---|---|
| 登录页 → 本机 | 显示组件是否安装、市场版本与体积；安装、更新、启动并登录 |
| 设置 → 组件与插件 | 两个组件卡片：状态、版本、体积、技能、工具链、安装/检查更新/更新/卸载 |
| 远程连接 | 只管理当前服务器的会话；组件操作在本机连接下进行 |

安装 `opencode` 后，`LocalRuntimeLauncher` 启动本机服务，并把 URL + 密码注入正常登录流程。

## 6. 代码位置

| 责任 | 文件 |
|---|---|
| 组件列表与操作 | `app/src/main/java/com/a2heng/androc/AppPluginManager.java` |
| skill → 组件映射 | `app/src/main/java/com/a2heng/androc/PluginCatalog.java` |
| android 包安装与 skills 复制 | `app/src/main/java/com/a2heng/androc/PackInstaller.java` |
| rootfs 定位与 proot 命令 | `app/src/main/java/com/a2heng/androc/MuslRootfs.java` |
| opencode 更新、健康检查、回滚 | `app/src/main/java/com/a2heng/androc/RuntimeUpdateService.java` |
| 市场索引、下载、校验 | `market/src/main/kotlin/com/a2heng/androc/market/MarketClient.kt` |
| 本机安装/启动对话框 | `ui/src/main/java/com/a2heng/androc/ui/ui/LocalRuntimeDialog.kt` |
