# Androc v0.5.0

一个 App、三层能力：UI → kernel pack → tools pack。本版是改名和结构版本。

## 改名

- **Adia 改名为 Androc**，applicationId 为 `com.a2heng.androc`
  （debug `.debug`，beta `.beta`）。
- 这是一次全新安装，旧 Adia 的数据不会自动迁移；需要保留项目的用户先导出
  `workspace/`。
- 内置 opencode 升到 **2.3.2**，新增 `android-ai` skill（ONNX/TFLite/llama.cpp/MNN）。

## 结构

- 仓库拆成 `:contracts` / `:kernel` / `:tools` / `:ui` / `:app`。
- 原来的独立 UI 仓库 `a2heng/opencode-android-ui` 不再维护，代码并入 `:ui`，
  不再维护第二份源码。
- `:contracts` 定义 `RuntimeConnection`、`RuntimeState`、`RuntimeController`、
  `ToolsController`、`Pairing`、`DiscoveryContract`。
- `:kernel` / `:tools` 仍是骨架，运行时和工具链按 `docs/series.md` 的
  phase 2/3 迁入。

## 打包

- 本版仍是 **full** 构建：toolbox/toolchain 内置于 APK，离线可用。
- 下一阶段出 `thin`（基础 APK + 可下载 kernel/tools pack）和 `full`
  （同样的 pack 放进 assets）。`proot` 引擎始终随 APK，其余都能下载。

## 接入

- 官方 OpenCode Android、其他 App、API、Console 的接入方式统一走标准
  opencode HTTP + pairing，详见 `docs/integration.md`。
- 设置页将合并登录与设置，统一管理连接、能力包、存储和诊断。

## 存储

- 运行时/toolbox/toolchain 继续留在应用私有目录。
- 项目/导入导出/大文件缓存的 SAF、MediaStore、OBB 方案见 `docs/storage.md`。
