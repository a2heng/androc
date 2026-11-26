# 构建、调试与发布命令

所有命令在仓库根目录执行。路径细节见 [`paths.md`](paths.md)。

## 1. 前置

| 项 | 要求 |
|---|---|
| JDK | 17（Gradle 自身）；`JAVA_HOME` 指向它 |
| Android SDK | 平台 34 / 36 / 37.0，build-tools 34.0.0 / 36.0.0 |
| 设备 | arm64-v8a，Android 8.0+（minSdk 26） |
| 网络 | 首次拉取 opencode/rootfs/Android 包需要访问 GitHub |

## 2. 脚本（`scripts/`）

| 命令 | 作用 | 产物 |
|---|---|---|
| `python scripts/fetch-opencode.py` | 按 `opencode.version` 下载官方 linux-arm64-musl 二进制 | `app/src/oc/jniLibs/arm64-v8a/libopencode2_bin.so` |
| `python scripts/fetch-opencode.py --latest` | 取上游最新稳定版并改写 `opencode.version` | 同上 |
| `python scripts/fetch-runtime.py` | 下载 oc 内置 musl rootfs | `app/src/oc/assets/runtime/rootfs.tar.xz` |
| `python scripts/fetch-android-pack.py` | 按 `android-pack.version`+`android-pack.sha256` 下载 apk 内置 Android 工具链 | `app/src/apk/assets/android/pack.tar.xz` |
| `python scripts/check-opencode-latest.py --strict` | 检查 `opencode.version` 是否最新 | 退出码 1 表示落后 |
| `python scripts/check-16kb.py <apk…>` | 检查 APK 内 64 位 ELF 16 KB 页对齐 | 退出码 1 表示不合格 |

`fetch` 脚本幂等：产物已存在且可用时复用缓存。

## 3. 构建

```sh
python scripts/fetch-opencode.py
python scripts/fetch-runtime.py

./gradlew :app:assembleUiDebug
./gradlew :app:assembleOcDebug
./gradlew :app:assembleApkDebug
```

| 任务 | APK |
|---|---|
| `assembleUiDebug` | `app/build/outputs/apk/ui/debug/app-ui-debug.apk` |
| `assembleOcDebug` | `app/build/outputs/apk/oc/debug/app-oc-debug.apk` |
| `assembleApkDebug` | `app/build/outputs/apk/apk/debug/app-apk-debug.apk` |

Gradle 属性：

| 属性 | 默认 | 说明 |
|---|---|---|
| `-PandrocVersionName` | `0.7.0-androc` | 版本名 |
| `-PandrocVersionCode` | `10` | 版本号 |
| `-PandrocUiSuffix` | `.ui` | ui flavor applicationId 后缀 |
| `-PandrocApkSuffix` | `.apk` | apk flavor applicationId 后缀 |
| `-PandrocRequireSigning=1` | 关闭 | 没有签名材料时失败 |

## 4. 真机安装与日志

```sh
adb install -r app/build/outputs/apk/ui/debug/app-ui-debug.apk
adb install -r app/build/outputs/apk/oc/debug/app-oc-debug.apk
adb install -r app/build/outputs/apk/apk/debug/app-apk-debug.apk

adb logcat -s OpenCodeLocal OpenCodeMusl OpenCodeAndroidAssets OpenCodeRuntimeAssets ThinRuntime
```

| 日志标签 | 来源 |
|---|---|
| `OpenCodeLocal` | proot 启动、挂载、配置写入 |
| `OpenCodeMusl` | rootfs 选择、调试通道 |
| `OpenCodeRuntimeAssets` | oc/apk 内置 runtime 解压 |
| `OpenCodeAndroidAssets` | apk 内置 Android 包解压 |
| `ThinRuntime` | thin 启动器 |

## 5. 调试 API（仅 debug）

绑定 `127.0.0.1:42100`，请求头 `X-Androc-Token: androc-local-dev`。

```sh
adb forward tcp:42100 tcp:42100
H='X-Androc-Token: androc-local-dev'
B=http://127.0.0.1:42100
curl -s -H "$H" "$B/ping"
curl -s -H "$H" "$B/state"
curl -s -H "$H" "$B/local"
```

| 方法 | 路径 | 作用 |
|---|---|---|
| GET | `/ping` | 存活 |
| GET | `/state` | UI 状态、连接、本机组件 |
| GET | `/local` | 已装版本、是否运行、URL |
| GET | `/logs` | `thin-runtime.log` 尾部 |
| POST | `/connect?url=&password=` | 连接并保存一个服务器 |
| POST | `/start-local` | 启动本机组件并自动连接 |
| GET | `/proxy?path=/api/info` | 带认证转发到本机 server |
| GET | `/musl?cmd=` | 在 musl rootfs 里跑一次性命令 |
| GET | `/plugins` | 组件状态 |

## 6. 发布

```sh
python scripts/check-opencode-latest.py --strict
python scripts/fetch-opencode.py
python scripts/fetch-runtime.py
# apk 还需要 assets/android/pack.tar.xz
./gradlew :app:assembleUiRelease :app:assembleOcRelease :app:assembleApkRelease
```

发布前至少确认：三个 APK 都能装；oc/apk 能离线起本机 server；apk 能在设备内完成一次 Android 构建。

## 7. 报错对照

| 现象 | 原因 | 处理 |
|---|---|---|
| `faltan binarios del runtime proot` | `nativeLibraryDir` 缺 proot/loader/musl 库 | 重装 APK；确认 ABI 是 `arm64-v8a` |
| `Android 插件（musl rootfs）未安装` | 没有可用 rootfs | oc/apk 确认内置 runtime 已解压；ui 无本机运行时 |
| `OpenCode 组件未安装` | 当前是 ui | ui 只做远程客户端，本机能力用 oc/apk |
| `java: cannot execute` / JVM 崩溃 | 没加载 fixexec shim | 先 `. /opt/androc/env.sh` |
