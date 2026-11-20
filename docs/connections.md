# 连接管理

## 1. 连接模型

| 字段 | 说明 |
|---|---|
| `url` | `http://host:port`；规范化后作为唯一键 |
| `password` | Basic auth 密码，可为空 |
| `username` | 固定 `opencode` |
| `lastUsedAt` | 用列表顺序表示，最近成功连接在最前 |

保存的连接只有 URL + 密码，最多 12 条。本机连接始终排在最前，其余按最近使用排序。

## 2. 本机连接

| 项 | 值 |
|---|---|
| 地址 | `http://127.0.0.1:<port>`；bundle 默认 4096，thin 在 4098–4199 选可用端口并记住 |
| 密码 | 首次使用本机服务时随机生成 32 字节（base64url），存本机偏好 |
| 启动 | `LocalRuntimeLauncher` 启动 proot + `opencode serve`；健康检查通过后回调 URL + 密码 |
| 登录 | 与远程相同，走 URL + Basic auth；凭据由 App 注入 |

## 3. 启动与恢复

| 上次连接 | 启动行为 |
|---|---|
| 远程 | 直接恢复该地址，不被宿主注入的本机 URL 覆盖 |
| 本机 | 使用宿主启动的本机 URL + 密码 |
| 没有记录 | 显示登录页；可手填地址或点“本机” |

## 4. 新建、切换、删除

### 新建

在登录页填地址和密码，点连接；成功后写入保存列表。输入地址时若命中已保存项，自动回填密码。

| 输入 | 处理 |
|---|---|
| 没有协议 | 自动补 `http://` |
| 末尾 `/` | 去掉 |
| `https://` 且对方是纯 HTTP、地址在私有网段 | 自动降级为 `http://` 并提示 |
| 密码 | 只从密码框读取 |

### 切换

| 步骤 | 行为 |
|---|---|
| 1 | `disconnect()`：取消连接与事件流，停止保活服务 |
| 2 | 清空当前会话、消息、会话列表、缓存、PTY |
| 3 | 保留保存的连接、项目文件、主题与设置 |
| 4 | 用新地址连接；成功后更新当前地址与按服务器隔离的 `recent` |

### 删除

- 登录页地址栏下拉列表右侧的删除按钮移除单条记录。
- 删除只影响保存列表；服务器数据与当前已建立的连接保持不变。

## 5. 自动重连与保活

| 场景 | 行为 |
|---|---|
| 已连接过、之后掉线 | 5 秒后重试，间隔翻倍，上限 60 秒 |
| 首次连接失败 | 停在登录页显示错误，不自动重试 |
| 回到前台 | 已连接则补启保活；掉线则立即重试一次 |
| 事件流（SSE）断开 | 1 秒后重连，间隔翻倍，上限 10 秒 |
| 后台保活 | `ConnectionService` 前台服务，常驻通知，`START_STICKY` |

通知栏提供“断开”操作，点击通知回到 App。

## 6. 安全

| 项 | 处理 |
|---|---|
| 保存的密码 | Base64 存在 `SharedPreferences`，不放日志、不放分享内容 |
| 本机密码 | 随机生成，仅 App 与本机进程使用 |
| 日志 | 不输出密码 |
| 远程地址 | 只连接用户填写的地址；自动降级仅限私有网段 |
| 保活通知 | 只显示连接状态，不显示密码与会话内容 |

## 7. 代码位置

| 责任 | 文件 |
|---|---|
| 连接状态、保存列表、重连 | `ui/src/main/java/com/a2heng/androc/ui/data/AppModel.kt` |
| URL 规范化、HTTP 客户端 | `ui/src/main/java/com/a2heng/androc/ui/data/OcClient.kt` |
| 登录页与已保存连接下拉 | `ui/src/main/java/com/a2heng/androc/ui/ui/ConnectScreen.kt` |
| 本机组件启动与凭据注入 | `ui/src/main/java/com/a2heng/androc/ui/ui/LocalRuntimeDialog.kt`、`app/src/main/java/com/a2heng/androc/ThinRuntimeLauncher.java`、`app/src/main/java/com/a2heng/androc/MobileFrontendActivity.java` |
| 前台保活 | `ui/src/main/java/com/a2heng/androc/ui/ConnectionService.kt` |
| 本机密码 | `app/src/main/java/com/a2heng/androc/LocalRuntime.java` |
