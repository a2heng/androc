# 自动化 API（开发/测试用）

App 在 `127.0.0.1:42100` 起一个仅回环的 HTTP API，用来不靠 adb 点击读写状态、
触发语义动作。

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
| GET | `/state` | UI、连接、本机组件、已保存连接、更新进度 |
| POST | `/connect?url=&password=` | 连接并记住一个服务器 |
| POST | `/start-local` | 启动已安装的 OpenCode 组件并连接 |
| GET | `/plugins` | 插件列表（opencode / android） |
| POST | `/plugin/install?id=` | 安装组件 |
| POST | `/plugin/uninstall?id=` | 卸载组件 |
| GET | `/market` | 当前市场索引地址 |
| POST | `/market?url=` | 设置市场索引（本地镜像/调试） |
| GET | `/musl?cmd=` | 在 proot rootfs 里执行命令，返回输出 |
| GET | `/proxy?path=` | 代理到本机 opencode |
| GET | `/logs` | 读取本机运行时日志 |
| GET | `/update/check` | 检查自更新，返回发行版与 APK URL |
| POST | `/update/download` | 下载并交给系统安装器 |

`/state` 示例：

```json
{
  "ui": true,
  "connected": true,
  "serverUrl": "http://127.0.0.1:4096",
  "updateMessage": "已是最新版本 v0.6.2",
  "updateProgress": 0,
  "local": {"available": true, "installed": true, "version": "2.3.2", "running": true},
  "saved": [
    {"url": "http://127.0.0.1:4096", "password": "***"},
    {"url": "http://192.168.0.105:42000", "password": "***"}
  ]
}
```

## 设计边界

- 开发/自动化接口，token 固定，只绑定 loopback。
- 只暴露语义动作（连接/启动/安装/更新），不暴露像素坐标。
- 聊天、文件、终端等数据面仍走 opencode 的 HTTP/SSE/PTY。
