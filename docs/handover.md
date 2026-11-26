# Androc 交接

> 给接手的人/agent：这一页说明现在是什么、怎么启动、怎么验收、哪里还欠。
> 产品分层与内置组件格式见 [`series.md`](series.md)，设计约束见 [`design.md`](design.md)，
> 路径与挂载见 [`paths.md`](paths.md)。

---

## 1. 当前产品

| 项 | 值 |
|---|---|
| 应用 | Androc，`com.a2heng.androc`（debug `.debug`，beta `.beta`） |
| 发行版 | `thin` / `bundle`，同一份代码、同一个 `applicationId` |
| 运行时 | proot + Alpine musl rootfs |
| opencode | `2.4.0`，唯一来源 `opencode.version` |
| 内置组件 | `a2heng/androc-market`：`index.json` + `index.json.sig`（Ed25519） |
| 内置包 | 只有 `android`（工具链 + skills），全量 tar.xz，无增量 |
| 测试机 | OPPO PKB110，Android 17，arm64，4 KB 页 |

| 发行版 | 内容 | 体积 | 用途 |
|---|---|---:|---|
| **thin** | 只有 UI | 约 7 MB | 远程客户端：连接电脑或远程 opencode，不带内置组件 |
| **bundle** | UI + 内置 musl rootfs + opencode | 约 164 MB | 本机开箱可用，首次启动解压内置 rootfs |

两个包只在内置资源上不同：`bundle` 带 `assets/runtime/rootfs.tar.xz` 和
opencode 二进制，`thin` 两者都不带。OpenCode 与 runtime 随 App 更新。

### 组件

| 组件 | 内容 | 安装位置 |
|---|---|---|
| OpenCode | 二进制（jniLibs）+ 内置 rootfs（Alpine；bash/git/python/node） | `nativeLibraryDir`、`files/runtime-core/` |
| `android` | `android-*` skills + musl rootfs（JDK 21、SDK 36、Gradle 9.9、静态 aarch64 aapt2/adb） | `files/packs/android/<version>/` |

rootfs 选择顺序（`MuslRootfs.dir`）：

```text
files/runtime/<ver>/rootfs          # 内置组件安装的 OpenCode 组件
→ files/packs/android/<ver>/rootfs  # 装了 android 组件时优先
→ files/runtime-core/rootfs         # bundle 首次启动解压
```

### 组件

| id | 名称 | 来源 |
|---|---|---|
| `opencode` | OpenCode 内核 | bundle 内置，随 App 更新 |
| `android` | Android 开发 | 内置组件 `android` 包 |

---

## 2. 仓库与代码地图

```text
app/         宿主：启动、运行时、内置组件安装、自动化 API
ui/          Compose 前端：连接/设置、会话、聊天、文件、终端
（market 已移除；组件内置到 APK flavor）
contracts/   共享契约：PluginInfo/PluginManager、RuntimeConnection、Pairing
scripts/     fetch-opencode / fetch-runtime / check-opencode-latest / check-16kb
docs/        文档
third_party/ 静态 aarch64 build-tools（aapt2 等）
reference/   只读参考项目
kernel/ tools/ plugin-sdk/ runtime-plugin/   预留模块，不参与当前启动链路
```

| 路径 | 作用 |
|---|---|
| `app/.../MobileFrontendActivity.java` | launcher；挂载 `:ui`，注入本机 launcher/shell |
| `app/.../LocalRuntime.java` | proot 启动、bind、server、终端、密码 |
| `app/.../MuslRootfs.java` | rootfs 选择与 proot 命令 |
| `app/.../RuntimeAssets.java` | 解压 bundle 内置 `assets/runtime/rootfs.tar.xz` |
| `app/.../AndroidAssets.java` | 内置组件目录包安装、skills 复制 |
| `app/.../AppPluginManager.java` | `opencode` / `android` 两个插件条目 |
| `app/.../MarketConfig.java` | 内置组件索引地址覆盖（调试/镜像） |
| `app/.../RuntimeAssets.java` | 前台更新 OpenCode 组件 |
| `app/.../AutomationServer.java` | 回环自动化 API（`:42100`） |
| `app/.../AndroidAssets.java` | full 内置 Android 包解压与 skills 复制 |
| `ui/.../LocalRuntimeDialog.kt` | “本机 OpenCode”：安装 / 更新 / 启动并登录 |
| `ui/.../SettingsScreen.kt` | 设置、插件、模型、连接 |
| `ui/.../AppModel.kt` | 连接、会话、模型、路径补全 |
| `scripts/fetch-opencode.py` | 按 `opencode.version` 拉 opencode 到 jniLibs |
| `scripts/fetch-runtime.py` | 下载 runtime-rootfs 构建输入给 bundle |

---

## 3. 启动链路

1. **只进 UI。** `MobileFrontendActivity` 是 launcher；启动时挂载 Compose，
   不启动 opencode、不启动前台服务。
2. **用户点“本机”。** `LocalRuntimeDialog`（只有 bundle 有）显示“启动并登录”。
3. **启动本机 server。**
   - `bundle`：`LocalServerService` 先 `RuntimeAssets.ensure()`，把
     `assets/runtime/rootfs.tar.xz` 解到 `files/runtime-core/rootfs`；
   - `LocalRuntime.start(port)` 用 `MuslRootfs.dir` 选 rootfs；
   - `proot -r <rootfs>`，绑定 `$HOME`、`nativeLibraryDir → /opt/androc/lib`、
     组件目录 → `/opt/androc/bin`、`/dev`、`/proc`、`/sys`、`/system`、
     `hosts`/`resolv.conf`，cwd 为 `$HOME`；
   - 执行 `opencode serve --hostname 127.0.0.1 --port <SERVER_PORT>`。
5. **健康检查与登录。** 轮询 `/api/info`（Basic `opencode:<password>`）；
   通过后把 `http://127.0.0.1:<port>` 和密码注入普通 `connect`，
   本机就是一套普通服务器。密码随机生成，存 `app` 偏好
   `local_server_password`。
6. **终端。** `LocalRuntime.localShell` 在同一 rootfs 内启动
   `/bin/bash -i -l`，由宿主注入 `:ui`；终端与 server 共享同一套路径。
7. **远程连接。** 只发 HTTP/SSE，不启动本机进程；连接期间由
   `ConnectionService` 前台保活。

端口：release `4096`，debug/beta `4098`（`BuildConfig.SERVER_PORT`）。
自动化 API 固定 `127.0.0.1:42100`，见 [`automation-api.md`](automation-api.md)。

---

## 4. 构建与安装

```sh
python scripts/fetch-opencode.py     # 按 opencode.version 取二进制
python scripts/fetch-runtime.py      # 从内置组件 OC 包取 rootfs 给 bundle
./gradlew :app:assembleThinDebug :app:assembleBundleDebug
python scripts/check-opencode-latest.py --strict
```

产物：

```text
app/build/outputs/apk/thin/debug/app-thin-debug.apk
app/build/outputs/apk/bundle/debug/app-bundle-debug.apk
```

release CI 出 `androc-<ver>-thin.apk` / `androc-<ver>-bundle.apk`，并跑
16 KB 静态检查与 apksigner 校验。发布说明只保留当前版本，写在
`RELEASE_NOTES.md`。

```sh
adb install -r app/build/outputs/apk/bundle/debug/app-bundle-debug.apk
adb shell am start -n com.a2heng.androc.debug/com.a2heng.androc.MobileFrontendActivity
```

---

## 5. 真机 60 秒验收

用 `bundle` debug 包，按顺序做：

| # | 操作 | 期望 |
|---|---|---|
| 1 | 安装并打开 App | 直接进 UI（连接页或上次会话）；本机 server 不自动启动 |
| 2 | “本机” → “启动并登录” | 前台通知出现，随后提示 OpenCode 已在本机运行 |
| 3 | 新建会话，发一句话 | 流式回复；模型选择遵循 [`model.md`](model.md) |
| 4 | 打开终端 | `git --version`、`python3 --version`、`node --version` 均有输出 |
| 5 | 设置 → 插件 → Android → 安装 | 下载/校验/解压完成，显示已安装与体积 |
| 6 | 终端构建 | `bash ~/.config/opencode/skills/android-build/scripts/build.sh <项目> debug` 输出 `BUILD SUCCESSFUL` |

辅助检查：

```sh
adb forward tcp:42100 tcp:42100
curl -s -H 'X-Androc-Token: androc-local-dev' http://127.0.0.1:42100/state
```

`/state.local` 应为 `installed=true, running=true`，`serverUrl` 指向本机端口。
构建细节见 [`commands.md`](commands.md)，路径见 [`paths.md`](paths.md)。

---

## 6. 已知缺口

| 缺口 | 现状 | 下一步 |
|---|---|---|
| **16 KB 页设备** | 真机只在 4 KB 页（PKB110）验证；`jniLibs` 与 android rootfs 的 ELF 只做静态检查 | 用 16 KB 页 Android 15/16 真机跑完整启动、终端、`assembleDebug`；保留 `ANDROC_16KB_STRICT=1` 的 CI 检查 |
| **android 包体积/下载** | 压缩约 300 MB，解压约 607 MB；全量下载，无增量；旧版本目录按 `<version>` 保留 | 首次安装前提示空间；下载失败续跑；评估版本清理与更小的 rootfs 分层 |

---

## 7. 交接清单

- [ ] 读完本页、[`series.md`](series.md)、[`design.md`](design.md)、[`paths.md`](paths.md)
- [ ] 配好 JDK/SDK，`assembleThinDebug` 与 `assembleBundleDebug` 都成功
- [ ] 真机跑完 §5 六步
- [ ] 确认 `opencode.version` 与内置组件 `kernel-opencode` 对得上
- [ ] 改行为时同步 docs、skills 与 `RELEASE_NOTES.md`
