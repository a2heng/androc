# 模型：默认、会话与 Provider

## 1. 解析顺序

已有会话始终使用服务端的 `session.model`。新建会话按以下顺序取：

```text
前端最近使用（recent）
  > 免费的第一个模型
  > 服务器默认模型
```

| 层 | 存放位置 | 作用范围 | 写入方 |
|---|---|---|---|
| `session.model` | 服务端，每个会话独立 | 当前会话 | 选择模型 |
| `recent` | 前端偏好，按服务器地址隔离 | 新会话 | 无会话时选择模型 |
| 服务器默认 | 服务端配置文件 | 整个服务器 | 显式设置 |
| 免费模型 | `GET /api/model` | 兜底 | 只读 |

`recent` 指向的模型不在当前服务器列表时忽略，继续往下取；没有免费模型时取列表第一个。

## 2. 选择模型

| 场景 | 行为 | 接口 |
|---|---|---|
| 已有会话 | 改当前会话模型，同时更新 `recent` | `POST /api/session/{id}/model`，body `{ "model": { id, providerID, variant? } }` |
| 没有会话 | 只更新 `recent` | 本地偏好 |
| 新会话创建后 | 用 `fallbackModel()` 选中并应用 | `POST /api/session/{id}/model` |

`/model` 命令、聊天左下角、设置 → 模型打开同一个选择页；有会话时改会话，没有会话时改 `recent`。

## 3. 服务器默认模型

- 展示：`GET /api/model/default`；模型选择页只读显示，不自动改写。
- 显式设置：调用下面的流程（模型选择页不自动触发），成功后刷新 `serverDefaultModel`。

| 步骤 | 动作 |
|---|---|
| 1 | `GET /api/config`，取最后一个带 `path` 的 `document` |
| 2 | `GET /api/fs/read/<path>` 读原始 JSON |
| 3 | 保留其它字段，写入 `"model": "providerID/modelID"`；有 variant 时 `providerID/modelID#variant` |
| 4 | `POST /api/experimental/fs/write?path=<urlencoded-abs-path>` 写回完整 JSON |
| 5 | 轮询 `GET /api/model/default`，等待服务端 `config.updated` 重载 |
| 6 | 5 秒内未生效返回失败，由调用方提示或走本机文件兜底 |

本机兜底：服务端没有开放 `experimental/fs.write` 时，直接写 `$HOME/.config/opencode/opencode.json` 和 `$HOME/.config/opencode-android/opencode.json`，再重启本机服务。

### 3.1 OpenCode 2.4.0 的 API 边界

| 能力 | 接口 | 说明 |
|---|---|---|
| 读默认模型 | `GET /api/model/default` | 返回 `Model.Info` 或 null |
| 读配置 | `GET /api/config` | `Config.Entry[]`，从低到高优先级；`document` 带 `path` 与 `info` |
| 改配置 | `PATCH /api/experimental/config` | `Config.Patch` 只有 `{ shell }`，不能写 `model` |
| 读文件 | `GET /api/fs/read/<path>` | 原始 bytes |
| 写文件 | `POST /api/experimental/fs/write?path=…` | 原始 body，可写绝对路径，实验性 |
| 热重载 | 事件 `config.updated` | 服务端重载 provider/model |

默认模型的写入路径是配置文件：写回后由 `config.updated` 触发重载。

## 4. Provider 连接

模型来自 provider，先连接再选模型。

| 动作 | 接口 |
|---|---|
| 列出 provider/集成 | `GET /api/integration` |
| API Key 连接 | `POST /api/integration/{id}/connect/key`，body `{ key, label? }` |
| OAuth 开始 | `POST /api/integration/{id}/connect/oauth`，body `{ methodID }` |
| OAuth 状态 | `GET /api/integration/{id}/connect/oauth/{attemptID}` |
| OAuth 完成 | `POST /api/integration/{id}/connect/oauth/{attemptID}/complete`，body `{ code? }` |
| OAuth 取消 | `DELETE /api/integration/{id}/connect/oauth/{attemptID}` |

UI 按 `methods[].type` 渲染：

| type | UI 行为 |
|---|---|
| `key` | 输入 API Key，提交 `connect/key` |
| `oauth` | 打开授权链接并每秒轮询；`paste`/`code` 模式给输入框，提交 `complete` |
| `env` | 显示需要设置的环境变量名 |
| `command` | 显示“服务器命令”，连接在服务器侧完成 |
| 已连接 | 显示“已连接”，连接按钮隐藏 |

连接成功后刷新 `/api/integration`、`/api/model`、`/api/model/default`，新模型立即进入选择列表。

## 4.1 OpenCode Go 额度

只有当前模型来自 `opencode-go` 时才显示额度；其它模型不占位置。

| 数据 | 接口 |
|---|---|
| 5 小时 / 本周 / 本月百分比 | `GET /api/experimental/usage/limits` 的 `data.go`（`rolling` / `weekly` / `monthly`） |
| 每日累计曲线 | `GET /api/credential` 找到激活的 `opencode` 凭据，再请求 `{metadata.server}/api/activity/go`（`Authorization: Bearer` + `x-org-id`） |

- 设置页在「模型与模式」两张卡片下面显示额度卡片（三窗口 + 本月累计曲线 + 月底虚线预测）；
- 对话底部命中率右边的胶囊：Go 会话没有 `session.cost`，改为显示本月额度占用，点开同一个额度卡片；
- `/usage` 是客户端斜杠命令，打开额度卡片；当前模型不是 Go 时只提示，不显示空卡片；
- 曲线按官方口径把每日花费折算成百分比：`当月百分比 × 当日累计花费 / 本周期总花费`，月底预测按当前日均线性外推；
- `/api/credential` 会返回密钥；只在内存里用于读额度，不写 prefs、不打印日志，曲线失败时仍显示三个百分比。

## 5. 多会话

| 操作 | 结果 |
|---|---|
| A 用 A 模型，B 用 B 模型 | 两个会话各自保存 |
| 在 A 里选 A2 | A = A2，B 不变，`recent` = A2 |
| 新建 C | C = A2（取 `recent`） |
| 切回 B | B 仍是 B |

`recent` 只决定下一个新会话的起点，已有会话保持各自的 `session.model`。

## 6. 写入安全

- 只在显式“设为服务器默认”时写配置，并显示将写入的路径。
- 先构造完整 JSON 字符串，再一次性写入，避免留下半个文件。
- 远程不可写时保留旧值并提示在服务器修改；本机才走文件兜底。

## 7. 代码位置

| 责任 | 文件 |
|---|---|
| 解析顺序、会话/最近模型 | `ui/src/main/java/com/a2heng/androc/ui/data/AppModel.kt` |
| 模型与集成 API | `ui/src/main/java/com/a2heng/androc/ui/data/OcClient.kt` |
| Provider 连接 UI | `ui/src/main/java/com/a2heng/androc/ui/ui/ProviderConnectDialog.kt` |
| 本机配置写入与重启 | `app/src/main/java/com/a2heng/androc/AppServerModelWriter.java` |
