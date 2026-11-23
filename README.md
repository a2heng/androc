# Androc

**Android Dev In Android** — 在 Android 手机上开发 Android，并在同一个 App 里
运行 [opencode](https://github.com/anomalyco/opencode)。

- 应用名：**Androc**
- 包名：`com.a2heng.androc`
- 运行时：**proot + Alpine musl**，自带用户态，不需要 root
- opencode：**2.4.0**（musl arm64）

## 两个发行版

| 发行版 | 内容 | 适用 |
|---|---|---|
| **thin** | 只有 UI（约 7 MB，蓝色图标） | 极简远程客户端：连电脑或远程 opencode，不带市场 |
| **bundle** | UI + musl runtime + OpenCode（约 164 MB，绿色图标） | 本机开箱即用，带市场（Android 工具链） |

两个包是同一个 App、同一个包名，只是内置资源不同；自更新会下载同发行版的 APK。

## 能做什么

- **本机 opencode**：点「本机 → 启动并登录」才启动，不后台常驻。
- **远程 opencode**：填 URL + 密码，保存多套连接，断线自动重连，连接期间前台保活。
- **会话 / 文件 / 终端**：同一个 UI 里切换远程与本机。
- **端侧构建 Android**：装 `android` 组件后，手机里直接 `assembleDebug`，
  工具链是 JDK 21 + SDK 36 + Gradle 9.9 + 静态 aarch64 aapt2/adb。
- **市场**：bundle 可安装 `android` 组件（JDK/SDK/Gradle/skills）；OpenCode 与 runtime 随 App 更新。

## 已验证（真机）

- bundle：本机 opencode 启动，对话、Python 脚本、终端正常。
- android 组件：手机内 `assembleDebug` 成功，产出 APK 安装并启动。
- 应用内自更新：thin 只下 thin APK，下载完成后可交给系统安装器。

## 构建

```sh
# 1) opencode 二进制（仓库不提交）
python scripts/fetch-opencode.py

# 2) bundle 内置的 musl runtime（下载 runtime-rootfs 构建输入，约 40 MB）
python scripts/fetch-runtime.py

# 3) 两个发行版
./gradlew :app:assembleThinDebug      # 只有 UI
./gradlew :app:assembleBundleDebug    # UI + runtime + OC
```

产物：

- `app/build/outputs/apk/thin/debug/app-thin-debug.apk`
- `app/build/outputs/apk/bundle/debug/app-bundle-debug.apk`

release 由 CI 构建，产出 `androc-<ver>-thin.apk` 与 `androc-<ver>-bundle.apk`。

## 目录

全部在 App 私有目录内：

```
files/
├─ home/                    # $HOME：会话目录、workspace、skills
│  ├─ workspace/
│  └─ .config/opencode/skills/
├─ runtime/<ver>/           # 市场 OC 组件：opencode + rootfs
├─ runtime-core/rootfs/     # bundle 首次启动解压的内置 runtime
├─ packs/<id>/<ver>/        # 市场组件（android 的 rootfs + skills）
└─ market/                  # 下载缓存
```

proot 把 `$HOME`、`/dev`、`/proc`、`/sys`、`/system` 绑定进 rootfs，工作目录
保持调用者的 `$PWD`。细节见 [`docs/paths.md`](docs/paths.md)。

## 文档

| 文档 | 内容 |
|---|---|
| [`docs/paths.md`](docs/paths.md) | 目录、挂载、环境变量、端口 |
| [`docs/commands.md`](docs/commands.md) | 构建、调试、发布命令 |
| [`docs/series.md`](docs/series.md) | 产品分层与市场包格式 |
| [`docs/design.md`](docs/design.md) | 设计不变量 |
| [`docs/handover.md`](docs/handover.md) | 架构、启动链路、验收清单 |
| [`docs/plugins.md`](docs/plugins.md) | 插件（OpenCode / Android） |
| [`docs/model.md`](docs/model.md) | 服务端默认模型 |
| [`docs/connections.md`](docs/connections.md) | 连接方式与保活 |
| [`docs/android-musl.md`](docs/android-musl.md) | musl 迁移决策与实测 |
| [`docs/mobile-integration.md`](docs/mobile-integration.md) | UI 行为不变量 |

## 参考项目

- [opencode](https://github.com/anomalyco/opencode)
- [proot](https://proot-me.github.io/)
- [Alpine Linux](https://alpinelinux.org/)
- [lzhiyong/android-sdk-tools](https://github.com/lzhiyong/android-sdk-tools)（静态 aarch64 build-tools）
- [lidanthedev/Acode-fixexec](https://github.com/lidanthedev/Acode-fixexec)（JVM exec-mem shim，GPL-3.0）

## 许可

Androc 自身代码 MIT。第三方组件与许可证见
[`THIRD_PARTY_LICENSES.md`](THIRD_PARTY_LICENSES.md)。
