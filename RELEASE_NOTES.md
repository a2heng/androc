# Androc v0.6.0

本版把运行时固定下来：**musl 取代 Termux/bionic**，并拆成两个发行版。

## 两个 APK

- **UI 版（thin）**：只有界面。远程连接 opencode 服务，终端用服务端 PTY；不下载、不启动任何本地 runtime。
- **UI + Runtime + OC 版（bundle）**：内置 musl runtime 和 opencode 二进制，本机开箱可用。

## 本机 OpenCode

- 启动 App 只进界面，**不自动启动**本地 server；只有点「本机 → 启动并登录」才启动，并等健康检查通过再连接，避免后台耗电。
- 首次启动时把内置 runtime 解压到应用私有目录；之后直接复用。
- 终端是 rootfs 里的 bash PTY；远程模式仍用服务端 PTY。

## 运行时

- 删除 Termux/bionic 整套：`LocalToolbox`、`ToolboxServer`、`ToolboxService`、`ToolchainInstaller` 和 dpkg 路径全部移除。
- 唯一运行时：proot + Alpine/musl rootfs（bash、coreutils、git、python3/pip、node/npm、openssh、make、curl、wget）。
- 在 CI 里验证过：JDK 21 + Gradle 9.9 + AGP + aapt2 可在纯 musl 下构建 APK。

## 插件

- OpenCode、Android、以及后续语言/写作组件都走市场。
- `android` 插件 = JDK 21 + SDK 35 + Gradle 9.9 + build-tools + adb + `android-*` skills，按需下载。
- OpenCode 更新只查 Androc 市场，不再查官方上游；本机更新只匹配已安装的 thin/bundle。

## 其它

- `full` flavor 删除，只剩 `thin` / `bundle`。
- 组件包带签名索引，安装后校验 SHA-256。

## 已知限制

- `thin` 想本机跑仍需下载 OpenCode（以及 runtime-core）；`bundle` 不需要。
- 插件增量更新暂未启用，当前为全量 xz。
