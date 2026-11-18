# Androc v0.5.1

第一个可用的 **UI-only + 组件市场** 版本。基础 APK 不再内置 OpenCode，也不内置 Termux 前缀。

## 薄 UI

- 基础 APK 实测 **约 7.1 MB**（arm64-v8a），只含 Compose UI、执行引信（musl loader / proot / 静态 busybox）和市场客户端。
- 登录页与设置页共用「本机 OpenCode」弹框：安装组件后自动启动、自动注入随机密码，并写入已保存连接。
- 已保存连接里本机排在最前；应用重启后会自动启动本机组件再连接。
- 新建会话界面重做：标题一行、HOME 在右、HOME/最近目录胶囊、内联新建文件夹；文件页恢复胶囊快速入口（第一个是 home），去掉项目区平铺。
- Provider 弹框：已连接 → 常用（国内外）→ 全部按名字；列表懒加载，332 个 Provider 不一次性渲染。

## OpenCode 组件（市场首个包）

- 直接消费官方 `anomalyco/opencode` 2.3.2 musl 发布物，不 fork、不改二进制。
- 市场包：xz 压缩 **88.6 MB**，安装后 **247 MB**；纯 Java 解压，不依赖桌面 zstd native。
- 官方 sha256 + 市场 Ed25519 签名双重校验；失败保留旧版本，可回滚。
- 当前用 musl loader 原生启动；本机端口自动选空闲口（默认从 4098 起）。
- 已接 DNS：通过本机 CONNECT 代理走 Android netd，避免 musl 直读 `/etc/resolv.conf` 失败。

## 安全

- 自动化/调试 HTTP API 只在 debug 构建启用；release 不监听 42100。
- 本机 OpenCode 仅绑定 `127.0.0.1`，密码随机生成，不写入日志、不显示在列表。
- 市场索引签名覆盖原始字节；包再次按 sha256 校验后才切换。

## 平台

- 只发 **arm64-v8a**；16 KB 页对齐检查继续保留。
- 基础包和 OpenCode 组件都不要求 root；proot 仅用于后续运行工具/构建链。

## 已知限制

- 安装 OpenCode 后，如果本机还没有 Provider 凭据，发送消息会在服务器端重试/失败；需要在「设置 → 连接 Provider / API」里配置，或从电脑侧同步凭据。
- 增量更新（7 MB 级）暂未启用，当前是全量 xz；下一步做纯 Java 块差量。
- 运行工具/Android 构建组件仍是下一阶段内容。
