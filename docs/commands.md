# 构建、调试与发布命令

所有命令在仓库根目录执行。路径细节见 [`paths.md`](paths.md)。

---

## 1. 前置

| 项 | 要求 |
|---|---|
| 主机 | Windows 11 + Git Bash；Unix 风格路径（`/d/005_dev/androc`） |
| JDK | 17（Gradle 自身）；`JAVA_HOME` 指向它 |
| Android SDK | 平台 34 / 36 / 37.0，build-tools 34.0.0 / 36.0.0 |
| 设备 | arm64-v8a，Android 8.0+（minSdk 26） |
| 网络 | 首次拉取 opencode 与 rootfs 需要访问 GitHub 和市场索引 |

---

## 2. 脚本一览（`scripts/`）

| 命令 | 作用 | 产物 / 缓存 |
|---|---|---|
| `python scripts/fetch-opencode.py` | 按 `opencode.version` 下载官方 linux-arm64-musl 二进制 | `app/src/bundle/jniLibs/arm64-v8a/libopencode2_bin.so`；缓存 `.cache/opencode` |
| `python scripts/fetch-opencode.py --latest` | 取上游最新稳定版，并改写 `opencode.version` | 同上 |
| `python scripts/fetch-opencode.py --version X.Y.Z` | 指定版本（不改 `opencode.version`） | 同上 |
| `python scripts/fetch-runtime.py` | 下载 `runtime-rootfs-1` 构建输入 | `app/src/bundle/assets/runtime/rootfs.tar.xz` |
| `python scripts/check-opencode-latest.py` | 查询上游最新版并与 `opencode.version` 比较 | 退出码 1 表示落后（仅 `--strict`） |
| `python scripts/check-16kb.py <apk…>` | 检查 APK 内 64 位 ELF 的 16 KB 页对齐 | 退出码 1 表示不合格 |
| `python scripts/check-elf-align.py <file|dir>…` | 逐个 ELF 打印 `min_align` | 纯报告 |
| `node scripts/make-webview-shims.js` | 重新生成 core-js 垫片 | `app/src/bundle/assets/oc-corejs.js` |
| `node scripts/test-webview-shims.js` | 在 Node 里验证垫片覆盖的 API | 自检 |

`fetch-runtime.py` 额外支持离线包：

```sh
RUNTIME_ROOTFS_URL=<url> python scripts/fetch-runtime.py
OPENCODE_PACK_URL=https://… python scripts/fetch-runtime.py
```

两个 `fetch` 脚本都幂等：产物已存在且大小足够时直接复用。

---

## 3. 构建

```sh
python scripts/fetch-opencode.py      # 1) OpenCode 二进制
python scripts/fetch-runtime.py       # 2) bundle 内置 rootfs

./gradlew :app:assembleThinDebug      # 只有 UI
./gradlew :app:assembleBundleDebug    # UI + rootfs + OpenCode
./gradlew :app:assembleDebug          # 两个 flavor 的 debug
./gradlew :app:assembleRelease        # 两个 flavor 的 release（需要签名）
```

产物：

| 任务 | APK |
|---|---|
| `assembleThinDebug` | `app/build/outputs/apk/thin/debug/app-thin-debug.apk` |
| `assembleBundleDebug` | `app/build/outputs/apk/bundle/debug/app-bundle-debug.apk` |
| `assembleThinRelease` | `app/build/outputs/apk/thin/release/app-thin-release.apk` |
| `assembleBundleRelease` | `app/build/outputs/apk/bundle/release/app-bundle-release.apk` |

Gradle 属性：

| 属性 | 默认 | 说明 |
|---|---|---|
| `-PandrocVersionName` | `0.6.2-androc` | 版本名（旧名 `adiaVersionName` 仍接受） |
| `-PandrocVersionCode` | `9` | 版本号 |
| `-PandrocRequireSigning=1` | 关闭 | 没有签名材料时直接失败 |
| `-PandrocUi=mobile|web` | `mobile` | `:ui` 的入口形态 |

签名材料二选一：

- `keystore.properties`（根目录，`storeFile` / `storePassword` / `keyAlias` / `keyPassword`）；
- 环境变量 `ANDROC_KEYSTORE` / `ANDROC_KEY_ALIAS` / `ANDROC_KEYSTORE_PASS` / `ANDROC_KEY_PASS`。

---

## 4. 真机安装与日志

```sh
adb install -r app/build/outputs/apk/thin/debug/app-thin-debug.apk
adb install -r app/build/outputs/apk/bundle/debug/app-bundle-debug.apk

adb logcat -s OpenCodeLocal OpenCodeMusl OpenCodePacks OpenCodeRuntimeAssets ThinRuntime
adb shell run-as com.a2heng.androc.debug cat files/logs/androc.log
```

| 日志标签 | 来源 |
|---|---|
| `OpenCodeLocal` | proot 启动、挂载、配置写入 |
| `OpenCodeMusl` | rootfs 选择、调试通道 |
| `OpenCodeRuntimeAssets` | bundle 内置 runtime 解压 |
| `OpenCodePacks` | 市场目录包与 skills 复制 |
| `ThinRuntime` | thin 启动器 |
| `AutomationServer` | 调试 API |

设备内存日志：`files/logs/androc.log`（512 KB 轮转）、`files/logs/thin-runtime.log`。

---

## 5. 调试 API（仅 debug 构建）

`MobileFrontendActivity` 在 `BuildConfig.DEBUG` 时启动，绑定 `127.0.0.1:42100`，
每个请求都要带 `X-Androc-Token: androc-local-dev`。

```sh
adb forward tcp:42100 tcp:42100
H='X-Androc-Token: androc-local-dev'
B=http://127.0.0.1:42100

curl -s -H "$H" "$B/ping"
curl -s -H "$H" "$B/state"
curl -s -H "$H" "$B/local"
curl -s -H "$H" "$B/logs"
```

| 方法 | 路径 | 作用 |
|---|---|---|
| GET | `/ping` | 存活 |
| GET | `/state` | UI 状态、当前连接、本机组件、已保存连接 |
| GET | `/local` | 已装版本、是否运行、URL |
| GET | `/logs` | `thin-runtime.log` 尾部 |
| POST | `/connect?url=&password=` | 连接并保存一个服务器 |
| POST | `/start-local` | 启动本机组件并自动连接 |
| GET | `/update/check` | 查询 OpenCode 更新 |
| POST | `/update/download` | 下载并安装更新 |
| GET | `/proxy?path=/api/info` | 带 Basic 认证转发到本机 server |
| GET/POST | `/market?url=` | 查询 / 覆盖市场索引地址 |
| GET | `/musl?cmd=` | 在 musl rootfs 里跑一次性命令 |
| GET | `/plugins` | 组件列表（id、版本、skills、是否已装） |
| POST | `/plugin/install?id=` | 安装组件（`android`） |
| POST | `/plugin/uninstall?id=` | 卸载组件 |

示例：

```sh
curl -s -H "$H" "$B/musl?cmd=uname%20-a%20%26%26%20java%20-version"
curl -s -H "$H" -X POST "$B/plugin/install?id=android"
curl -s -H "$H" -X POST "$B/market?url=https://mirror.example/androc-market/main/index.json"
adb forward --remove tcp:42100
```

直接用 curl 连本机 server（密码见 `/state` 或 `SharedPreferences("app")`）：

```sh
curl -s -u opencode:<password> http://127.0.0.1:4096/api/info
adb forward tcp:4096 tcp:4096
curl -s -u opencode:<password> http://127.0.0.1:4096/global/health
```

---

## 6. rootfs 内的命令

agent、本机终端、构建都在同一个 proot 里，命令直接写，不需要包装：

| 类别 | 命令 | 说明 |
|---|---|---|
| shell | `sh`、`bash` | `SHELL=/bin/bash` |
| 包管理 | `apk add <pkg>`、`apk del` | Alpine apk-tools；`/etc/resolv.conf` 已绑定 |
| VCS | `git` | `$HOME/.gitconfig` 已加 `safe.directory = *` |
| 语言 | `python3`、`node`、`npm` | rootfs 自带 |
| JVM | `java`、`javac` | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk` |
| Android 构建 | `gradle`、`aapt2`、`zipalign`、`apksigner`、`adb` | `PATH` 已含 SDK 与 Gradle |
| 设备 | `/system/bin/am`、`pm`、`logcat`、`dumpsys`、`input`、`screencap` | `/system` 已 bind，用绝对路径 |

构建前先加载 Android 环境（尤其是 `LD_PRELOAD` 的 JVM shim）：

```sh
. /opt/androc/env.sh
java -version
gradle -v
aapt2 version
```

端侧构建一个项目：

```sh
cd "$HOME/workspace/<project>"
. /opt/androc/env.sh
gradle assembleDebug
```

`android` 组件安装后，它的 skills 会写到
`$HOME/.config/opencode/skills` 与 `$HOME/.config/opencode-android/skills`。

---

## 7. 发布

```sh
python scripts/check-opencode-latest.py --strict   # 必须通过
python scripts/fetch-opencode.py
python scripts/fetch-runtime.py
git tag v0.6.2
git push origin v0.6.2
```

| 项 | 内容 |
|---|---|
| 触发 | push `v*` tag；或 `workflow_dispatch`（只出签名 APK，不建 Release） |
| 版本 | tag 去掉 `v`；`versionCode = major*10000 + minor*100 + patch` |
| 任务 | `:app:assembleRelease`，thin + bundle 两个 flavor |
| 校验 | `check-opencode-latest.py --strict`、每个 APK 跑 `check-16kb.py`、`apksigner verify` |
| 产物 | `androc-<ver>-thin.apk`、`androc-<ver>-bundle.apk`，各带 `.sha256` |
| 正文 | `RELEASE_NOTES.md` 全文（只写当前版本，中文） |
| 密钥 | `ANDROC_KEYSTORE_BASE64`、`ANDROC_KEY_ALIAS`、`ANDROC_KEYSTORE_PASS`、`ANDROC_KEY_PASS` |

CI（`.github/workflows/ci.yml`）在每次 push/PR 上跑
`assembleThinDebug` + `assembleBundleDebug`，并用
`ANDROC_16KB_STRICT=1` 检查两个 APK。

---

## 8. 校验命令

```sh
python scripts/check-opencode-latest.py --strict
ANDROC_16KB_STRICT=1 python scripts/check-16kb.py \
  app/build/outputs/apk/*/*/*.apk
python scripts/check-elf-align.py app/src/main/jniLibs/arm64-v8a
node scripts/test-webview-shims.js
```

发布前至少确认：两个 APK 都能装、bundle 能离线
起本机 server、`android` 包能在设备内完成一次 `assembleDebug`。

---

## 9. 报错对照

| 现象 | 原因 | 处理 |
|---|---|---|
| `faltan binarios del runtime proot` | `nativeLibraryDir` 缺 proot/loader/musl 库 | 重装 APK；确认 ABI 是 `arm64-v8a` |
| `Android 插件（musl rootfs）未安装` | 没有任何可用 rootfs | 确认 bundle 已解压内置 runtime |
| `OpenCode 组件未安装` | 当前是 thin | thin 只做远程客户端，本机能力用 bundle |
| `installed hash mismatch` | 下载不完整或缓存损坏 | 删 `files/market/<包>` 下对应文件后重试 |
| `HTTP 403/404 …/index.json` | 默认索引不可达 | `POST /market?url=<镜像索引>` |
| `market index signature rejected` | 索引被改或镜像用了别的密钥 | 换回默认索引 |
| `java: cannot execute` / JVM 启动崩 | 没加载 fixexec shim | 先 `. /opt/androc/env.sh` |
| Gradle 找不到 SDK | 没有 `ANDROID_HOME`/`sdk.dir` | 用 `env.sh`；项目里写 `sdk.dir=/opt/android-sdk` |
| 本机 server 起不来 | 看 `files/logs/androc.log` 的 `OpenCodeLocal` 行 | 重启服务；检查 4096/4098 是否被占 |
| 16 KB 检查失败 | 某个 ELF 的 `p_align < 0x4000` | 用 `check-16kb.py` 定位具体 `.so` |

---

## 10. 代码位置

| 功能 | 文件 |
|---|---|
| 构建、flavor、端口、签名 | `app/build.gradle` |
| 拉取脚本 | `scripts/fetch-opencode.py`、`scripts/fetch-runtime.py` |
| 版本校验 | `scripts/check-opencode-latest.py`、`opencode.version` |
| 对齐校验 | `scripts/check-16kb.py`、`scripts/check-elf-align.py` |
| CI / 发布 | `.github/workflows/ci.yml`、`.github/workflows/release.yml` |
| 调试 API | `app/src/main/java/com/a2heng/androc/AutomationServer.java` |
| proot 启动 | `app/src/main/java/com/a2heng/androc/LocalRuntime.java` |
