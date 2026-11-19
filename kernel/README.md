# `:kernel` — 手机上的 opencode

负责让 `opencode` 能在 Android App 里跑起来的一切。这里不画 UI，也不构建 Android 工程。

## 职责

- `opencode` 的 musl 二进制 + musl loader（来自 `jniLibs`）
- proot、bind 表、`PREFIX`、`HOME`、`TMPDIR`、`/etc/hosts`
- toolbox 必需文件（`bash`、`readline` 等）、离线修复、`pkg/apt`
- 服务进程、端口、密码、前台服务、健康检查/看门狗
- 标准配对（`POST /api/pair` → `/auth/connect/<code>`）
- 工作区、日志、临时缓存清理、运行时更新
- 可选的本地发现 Provider（`DiscoveryContract`）

## 公开 API（目标）

```java
OpenCodeKernel kernel = OpenCodeKernel.create(context, KernelConfig.builder()
        .port(4096)
        .opencodeVersion(BuildConfig.OPENCODE_VERSION)
        .build());
kernel.start();
RuntimeConnection c = kernel.connection();   // READY 之前为 null
String link = kernel.pairingLink();           // 官方 App / 其他客户端
```

`OpenCodeKernel` 实现 `:contracts` 里的 `RuntimeController`。

## 迁移对照表（第 2 阶段）

从 `app/src/main/java/com/opencode/launcher/` 迁出：

| 文件 | 说明 |
|---|---|
| `LocalRuntime.java` | 服务启停、HOME、配对；把 `BuildConfig` 换成 `KernelConfig` |
| `LocalToolbox.java` | prefix 修复、`.deb` 合并、必需文件 |
| `CommandResolver.java` | 生成的 `oc-*` 辅助命令 |
| `LocalServerService.java` | 前台服务 |
| `ToolboxService.java` | 首次安装 / 自检 |
| `ToolboxServer.java` | `/doctor`、`/repair` 端点 |
| `ServerWatch.java` | 健康看门狗 |
| `RuntimeUpdateService.java` | opencode 二进制更新 |
| `LocalNetworkProxy.java` | DNS/代理垫片 |
| `LocalServerInfoProvider.java` | 可选发现（`DiscoveryContract`） |
| `NativeRuntimeConfig.java` | proot 环境变量 |
| `BootReceiver.java` | 开机后重启 |
| `AppLog.java` | 共享日志（App 可以继续用） |

清单里的组件跟着一起搬：`LocalServerService`、`ToolboxService`、
`RuntimeUpdateService`、`BootReceiver`、`LocalServerInfoProvider`、看门狗。

资源可以留在 full app 模块：Android 会把所有模块的 assets 合并进同一个
`AssetManager`，所以不管哪个模块打包，kernel 代码都能读到 `assets/toolbox/...`。

## 不能依赖

`:ui`、`:tools`、Compose、Android SDK/Gradle、`com.a2heng.androc` 品牌信息。

## 验收

- `oc-engine status` → `proot`
- `oc-doctor` 没有 `MISSING`
- `bash -lc 'git --version'`、`python3 --version`、`pkg install -y jq`
- 官方 opencode Android App 能用复制出的配对链接连上
- 同一个 kernel AAR 支撑简单版和完整版产品，不需要分叉代码
