# 自动化 API（开发/测试用）

App 在 `127.0.0.1:42100` 起一个仅回环的 HTTP API，用来**不靠 adb 点击**读写状态、
触发语义动作。只绑定 loopback，不上局域网。

## 连接

```sh
adb forward tcp:42100 tcp:42100
TOKEN='X-Androc-Token: androc-local-dev'
curl -s -H "$TOKEN" http://127.0.0.1:42100/state
```

## 端点

| 方法 | 路径 | 作用 |
|---|---|---|
| GET | `/ping` | 存活 |
| GET | `/state` | UI 是否就绪、当前连接、本机组件、已保存连接 |
| POST | `/connect?url=&password=` | 连接并记住一个服务器 |
| POST | `/start-local` | 启动已安装的 OpenCode 组件并自动连接（会写入保存列表） |

`/state` 示例：

```json
{
  "ui": true,
  "connected": true,
  "serverUrl": "http://127.0.0.1:4098",
  "local": {"available": true, "installed": true, "version": "2.3.2", "running": true},
  "saved": [
    {"url": "http://127.0.0.1:4098", "password": "***"},
    {"url": "http://192.168.0.105:42000", "password": "***"}
  ]
}
```

## 设计边界

- 这是**开发/自动化**接口，不是产品远程控制面；token 固定、只在 loopback。
- 只暴露语义动作（连接/启动/状态），不暴露像素坐标；UI 结构变化不需要改测试脚本。
- 聊天、文件、终端等数据面仍走 opencode 的 HTTP/SSE/PTY，API 只做壳层控制。
