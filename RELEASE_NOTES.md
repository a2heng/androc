# Androc v0.6.4

升级内置 OpenCode 到 2.4.0，并补上对话里的用量入口与 OpenCode Go 额度显示。

## OpenCode

- 内置 OpenCode 从 2.3.2 升到 2.4.0（`opencode.version`）。
- 对话新增 `/usage`：查看当前模型的 OpenCode Go 额度。
- 跟随 2.4 的服务端命令与接口：`/automation`、`/init`、`/review`、`/goal` 等由 `/api/command` 直接提供；额度读取使用 `/api/experimental/usage/limits`，每日曲线使用 Console 的 `/api/activity/go`。

## OpenCode Go 额度

- 设置页「模型与模式」下面显示 Go 额度卡片，仅当当前模型来自 `opencode-go`。
- 5 小时、本周、本月三个窗口的百分比与重置时间。
- 本月每日累计曲线（官方 Console 数据），并画出月底预测虚线和 100% 额度线。
- 对话底部命中率右边的消费胶囊：Go 会话花费为 0 时显示本月额度占用，点开额度卡片。

## 修复

- 内置 runtime 解压遇到上次中断留下的断链 symlink（如 `libbrotlienc.so.1`）时会反复失败；现在先按路径删除旧链接、串行解压，失败时清理半成品，下次启动可自愈。

## 文档

- 更新交接文档、版本表、模型/接口说明与前端行为约定。
