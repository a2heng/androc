# 接入其他应用、API 与 Console

本文说明 Androc 如何被**其他客户端**接入：官方 OpenCode Android、同机 App、
局域网/远程客户端、脚本、浏览器 Console。原则只有一条：

> **公开契约是标准 opencode HTTP + pairing。**
> ContentProvider、分享、二维码只是同机便利，不是协议。

---

## 1. 连接方式矩阵

| 客户端 | 方式 | 需要什么 | 备注 |
|---|---|---|---|
| **Androc 自己的 UI** | 同进程 `RuntimeConnection` | 无 | kernel 直接给 URL + 密码 |
| **官方 OpenCode Android** | 配对链接 / QR | kernel pack 已启动 | 官方 App 不查询我们的 ContentProvider |
| **同机其他 App** | ContentProvider（可选）或分享链接 | 声明权限 / 用户分享 | 默认不暴露密码，见 §7 |
| **局域网客户端** | `http://<手机IP>:4096` + Basic auth | 打开“局域网访问” | 明文 HTTP，必须在可信网络 |
| **远程/电脑** | VPN/Tailscale 或隧道 + Basic auth | 网络可达 | 不默认暴露公网 |
| **浏览器 Console** | `http://…:4096/`，先走 pairing 链接 | 浏览器 | 会话 cookie |
| **脚本 / CI** | `curl -u opencode:$PASS` | URL + 密码 | 标准 HTTP API |

---

## 2. Kernel 暴露什么

`:kernel` 的 `OpenCodeKernel implements RuntimeController` 提供：

```java
RuntimeConnection connection();   // baseUrl + username + password
String pairingLink();             // 一次性 /auth/connect/<code>
boolean running();
void start();
void stop();
```

派生值（不新增协议）：

```text
API base   = connection.baseUrl()
Console    = connection.baseUrl() + "/"
Pairing    = Pairing.link(connection.baseUrl(), code)
QR payload = Pairing.qrPayload(code, reachableUrls)
```

LAN 模式：

```java
KernelConfig.builder()
    .hostname("0.0.0.0")   // 默认 127.0.0.1
    .port(4096)
    .build();
```

切换 hostname 需要重启服务器；设置页负责提示“仅可信网络”。

---

## 3. 设置页的“连接”区块

登录和设置已经合并；连接相关操作都在这里：

| 操作 | 行为 |
|---|---|
| 本机内核 | 启动/停止、状态、端口 |
| 复制 API 地址 | `http://127.0.0.1:4096` |
| 复制密码 | `opencode` + 当前密码 |
| 生成配对链接 | `POST /api/pair` → 复制链接（5 分钟、一次性） |
| 分享配对链接 | `ACTION_SEND` 文本 |
| 显示二维码 | 只用于**另一台设备**；同机扫码无意义 |
| 打开 Console | 先加载 pairing 链接，再进 `/` |
| 局域网访问 | 开关；重启到 `0.0.0.0`；显示手机 IP |
| 其他应用接入 | 展开说明 + 复制/分享 |

同机接入不需要二维码：复制链接，粘贴到官方 App。

---

## 4. 官方 OpenCode Android 的接入流程

1. 安装 kernel pack，启动内核；
2. 设置 → **生成配对链接**；
3. 复制链接，打开官方 App → 粘贴；
4. 官方 App 消费一次性 code，拿到 30 天会话令牌；
5. 看到本机会话列表，新建会话并收到流式输出。

验收脚本（真机）：

```sh
# 1. 健康检查
curl -u opencode:$PASS http://127.0.0.1:4096/global/health

# 2. 申请配对码
curl -u opencode:$PASS -X POST http://127.0.0.1:4096/api/pair
# {"code":"…","expires_in":300}

# 3. 链接（浏览器/官方 App）
# http://127.0.0.1:4096/auth/connect/<code>

# 4. 会话列表
curl -u opencode:$PASS http://127.0.0.1:4096/api/session
```

> 不同 opencode 版本的路径可能是 `/api/pair` 或 `/server/pair`；内核应探测
> 两者，而不是假设其中一个。配对码在服务端 5 分钟过期、只能用一次。

---

## 5. API 接入

任何能发 HTTP 的客户端都能接入，不需要我们的 SDK：

```sh
BASE=http://127.0.0.1:4096
AUTH="-u opencode:$PASS"

curl $AUTH $BASE/global/health
curl $AUTH $BASE/api/session
curl $AUTH -X POST $BASE/api/session -H 'Content-Type: application/json' -d '{}'
```

- Basic auth 用户名固定 `opencode`；
- 配对链接换取的是会话令牌（cookie `opencode_session_<port>`，30 天）；
- 流式输出走服务端原有 SSE/WebSocket；
- 不要新增 `/androc/...` 私有 API；需要扩展时先看上游是否已有。

---

## 6. Console（Web UI / TUI）

### Web Console

opencode server 自带 Web UI，路径是 `/`：

- **Androc 内**：`MainActivity` 的 WebView 先 `webLoad(pairingUrl)` 设置 cookie，
  再进 `/`；Compose 设置页通过 `HostActions.openConsole()` 触发。
- **电脑浏览器**：打开 `http://<手机IP>:4096/`，或先访问 pairing 链接。
- **外部浏览器**：需要 Basic auth；部分浏览器不支持 URL 内嵌密码，建议用
  pairing 链接。

### TUI

`opencode` 的终端 UI 可以在 proot 终端里运行，但手机输入体验有限；作为
高级功能保留，不放进主流程。

---

## 7. 同机 App：ContentProvider 与安全

`LocalServerInfoProvider` 是可选的同机便利：

```text
content://com.a2heng.androc.local-server/info
permission: com.a2heng.androc.permission.LOCAL_SERVER_INFO
```

返回 JSON：`url`、`port`、`username`、`password`、`running`、`pid`、`app`。

**安全规则（必须改）**：

1. 默认不把密码交给任意声明 normal 权限的 App；
2. 设置页提供“允许同机应用自动连接”，默认关闭；
3. 打开后也只允许**前台/近期**查询，或改成 `signature` 权限；
4. 第三方 App 的正路是用户**分享配对链接**，不是读 provider；
5. 永远不要通过 Intent extra / URL query 传递密码。

推荐实现：

```text
provider exported=true, readPermission=<signature|normal>
  + Settings 开关（默认 off）
  + 每次查询校验调用方 package + 用户授权时间窗
```

如果不想承担这个风险，直接把 provider 设为 `exported=false`，只保留
“分享配对链接”这一条路。

---

## 8. 深链 / 分享

- **分享**：`ACTION_SEND`，正文就是配对链接；任何 App 都能接收。
- **深链**：可以注册 `androc://connect?url=…`，但**不要**在链接里放密码；
  打开后必须让用户确认，并把它当作“连接到某个服务器”的普通输入。
- 不实现 `androc://pair?password=…` 这类自动登录。

---

## 9. 实现落点

| 工作 | 模块 | 说明 |
|---|---|---|
| `KernelConfig.hostname`、pairing 探测 | `:kernel` | 从 `LocalRuntime` 迁入 |
| `HostActions.openConsole/share/copy/qr` | `:ui` + `:app` | UI 只发命令，app 执行 |
| 设置页“连接”区块 | `:ui` | 和登录合并 |
| WebView pairing 入口 | `:app` | 复用 `MainActivity.webLoad` |
| provider 安全收紧 | `:kernel` | 默认关闭/签名权限 |
| `docs/integration.md` + 验收脚本 | 文档 | 本页 |

---

## 10. 验收清单

- [ ] 官方 App 用复制的配对链接连上，能看到会话；
- [ ] `curl -u opencode:$PASS /global/health` 返回 200；
- [ ] 浏览器打开 `/` 正常，cookie 生效；
- [ ] 局域网模式能从另一台设备连接，关闭后不能；
- [ ] 同机 provider 在开关关闭时拒绝查询；
- [ ] 配对链接只能用一次，过期后提示重新生成。
