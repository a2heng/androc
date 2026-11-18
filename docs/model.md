# 默认模型：只有一个 Server Default

状态：`[进行中]`。代码已开始收束，API 事实如下。

---

## 1. 原则

模型解析顺序（新会话）：

```text
当前会话已选模型
  > 前端最近使用的模型
  > 免费的第一个模型
  > 服务器默认模型
```

- **前端只记住“最近使用的模型”**，不保存“默认模型”；
- “选择模型”页面：
  - 有会话 → 改当前会话模型（`POST /api/session/:id/model`）；
  - 没有会话 → 改“最近使用”，供新会话使用；
- 服务器默认模型是**只读兜底**，不再由模型选择页随意改写；
- 官方 Web 也是把 recent 存在前端（`models.recent`），会话选择优先；我们按
  上面的顺序做产品取舍。

---

## 2. OpenCode 2.3.2 的 API 事实

| 能力 | 接口 | 说明 |
|---|---|---|
| 读默认模型 | `GET /api/model/default` | 返回 `Model.Info` 或 null；支持 `location` |
| 读配置文档 | `GET /api/config` | 返回 `Config.Entry[]`，从低到高优先级；`document` 带 `path` + `info` |
| 更新配置 | `PATCH /api/experimental/config` | payload 是 `Config.Patch`，**2.3.2 只有 `{shell}`**，不能写 `model` |
| 写文件 | `POST /api/experimental/fs/write?path=…` | 原始 body；可写绝对路径；实验性 |
| 读文件 | `GET /api/fs/read/<path>` | 返回原始 bytes |
| 配置热重载 | 事件 `config.updated` | 配置插件监听文件变化，重载 provider/model |

证据（reference/opencode）：

- `packages/protocol/src/groups/model.ts`：只有 `model.default` 的 GET；
- `packages/protocol/src/groups/config.ts`：`config.update` 的 payload 是 `Config.Patch`；
- `packages/schema/src/config.ts`：`Patch = { shell: string | null }`；
- `packages/core/src/config/plugin/provider.ts`：`ConfigEntryObserver.observe` 在
  `config.updated` 后重载 provider，并把 `sources.defaultModel` 写入
  `models.default.set(...)`。

结论：**没有“直接设置默认模型”的 HTTP endpoint，但有“写配置文件并触发重载”
的通用能力。** Web/桌面也是围绕配置文件做配置，只是 2.3.2 的 config patch
还没暴露 model。

### 2.1 Web / 桌面到底怎么“改默认模型”

研究 `reference/opencode/packages/app` 的结论：

| 位置 | 行为 |
|---|---|
| `providers/models/configured.ts` | `useConfiguredModel()` **只读** `/api/config`：取最后一个 `type=document` 且 `info.model` 存在的文档，把 `info.model` 当作默认；没有任何 setter |
| `settings/models/models.tsx` | 只切换模型的可见性（`models.setVisibility`），不写默认模型 |
| `runtime/server/sync.tsx` | `updateConfigMutation` 明确 TODO：“Restore config updates when the V2 client exposes a config API.”；目前只调用 `experimental.config.update({ shell })` |
| `settings/general/controllers.ts` | 只更新 shell |
| `protocol/groups/config.ts` + `schema/config.ts` | `Config.Patch = { shell }`，没有 `model` |

所以：

- Web UI 的“默认模型”= **读配置文件里的 `model` 字段**，不是调用某个
  `setDefaultModel` API；
- 浏览器本身不能改服务器文件；桌面端可以走本地文件系统；
- 真正“设置默认模型”的通用做法就是**改 `opencode.json`**；
- 我们通过 `experimental/fs.write` 改同一个配置文件，再用 `config.updated`
  让 provider 重载——这比等一个不存在的 API 更接近官方机制。

如果服务器关闭了 `experimental/fs.write`，那么 Web 端也改不了远程默认模型，
只能只读；这不是我们前端缺 API，而是服务端没有开放写配置的能力。

### 2.2 验证命令

```sh
# 1. 看配置文档和默认模型
curl -u opencode:$PASS http://127.0.0.1:4096/api/config
curl -u opencode:$PASS http://127.0.0.1:4096/api/model/default

# 2. 读最高优先级的 document path
#    从 /api/config 的 document.path 取最后一个

# 3. 写回（保留其他字段，只改 model）
curl -u opencode:$PASS -X POST \
  "http://127.0.0.1:4096/api/experimental/fs/write?path=<urlencoded-abs-path>" \
  -H 'Content-Type: application/json' \
  --data-binary @opencode.json

# 4. 等 config.updated 后确认
curl -u opencode:$PASS http://127.0.0.1:4096/api/model/default
```

若 `fs.write` 返回 404/501，说明服务端没有开放该实验接口；本机可用配置文件
回退，远程只能只读。

---

## 3. 实现方式

### 3.1 模型选择（主路径）

- 会话内：`setModel(ref)` → `POST /api/session/{id}/model` → 写入 recent；
- 无会话：`setRecentModel(ref)` → 只写前端 recent；
- 新会话：服务端没有指定 model 时，按 §1 的顺序选一个并 `POST .../model`；
- 选择器不再调用 `fs.write` 改服务器默认。

### 3.2 服务器默认（可选工具，不是主路径）

仍然保留“显式设置为服务器默认”的能力，但不在模型选择页自动触发：

1. `GET /api/config` → 取最高优先级、带 `path` 的 `document`；
2. `GET /api/fs/read/<path>` 读原始 JSON；
3. 保留其他字段，写入：
   ```json
   "model": "providerID/modelID"        // variant 时 "providerID/modelID#variant"
   ```
4. `POST /api/experimental/fs/write?path=<abs>` 写回；
5. 轮询 `GET /api/model/default`，等待 `config.updated` 生效；
6. 返回成功/失败。

本机兜底：直接写 `$HOME/.config/opencode*/opencode.json` + 重启。

### 3.3 失败时的 UI

- 远程且不可写：显示“服务器未开放配置写入 API；请在服务器修改”，
  default 仍只读展示；
- 写入失败：保留旧值，显示错误；
- 不要偷偷改成本地 Default。

---

## 4. Provider 连接 / 斜杠 `/model`

官方 Mobile/Web 的“Connect”不是模型 API，而是 **Integration** API；模型页和
`/model` 命令共用同一个入口。

### 4.1 官方接口

| 动作 | 接口 |
|---|---|
| 列出 Provider/集成 | `GET /api/integration` |
| API Key 连接 | `POST /api/integration/:id/connect/key` `{ key, label? }` |
| OAuth 开始 | `POST /api/integration/:id/connect/oauth` `{ methodID }` |
| OAuth 状态 | `GET /api/integration/:id/connect/oauth/:attemptID` |
| OAuth 完成（code/paste） | `POST /api/integration/:id/connect/oauth/:attemptID/complete` `{ code? }` |
| OAuth 取消 | `DELETE /api/integration/:id/connect/oauth/:attemptID` |
| 环境变量方式 | `GET /api/integration` 里 `type=env` 的 `names`，由服务器环境提供 |
| 服务器命令方式 | `POST /api/integration/:id/connect/command` `{ methodID }` + 轮询 status |
| 添加 well-known 源 | `POST /api/experimental/integration/wellknown` `{ url }` |

`GET /api/integration` 返回：

```json
{
  "id": "anthropic",
  "name": "Anthropic",
  "methods": [
    { "type": "key", "label": "API Key" },
    { "type": "oauth", "id": "oauth", "label": "Sign in" }
  ],
  "connections": [{ "id": "…", "label": "…" }]
}
```

### 4.2 我们的 UI

模型选择页（设置和 `/model` 共用）顶部增加：

```text
连接 Provider / API                2/5
```

点开列出所有集成：

- 已连接 → “已连接”；
- `key` → 输入 API Key → `connect/key`；
- `oauth` → 打开浏览器 + 轮询 attempt；`paste`/`code` 模式给输入框 →
  `complete`；
- `env` → 显示需要设置的环境变量名；
- `command` → 显示需要在服务器执行的命令。

连接成功后刷新 `/api/model`、`/api/model/default`，新模型立即出现在列表里。

### 4.3 `/model`

聊天里的 `/model` 已经打开同一个 `ModelPickerPage`；因此“连接 Provider”和
“切换模型”是同一个页面，不需要两套 UI。

---

## 5. UI 行为

### 5.1 多会话

模型有两层，互不混淆：

| 数据 | 存在哪里 | 作用范围 |
|---|---|---|
| `session.model` | 服务器，每个会话独立 | 只影响这个会话 |
| `recent` | 前端，最近一次明确选过的模型 | 新会话的默认来源 |

规则：

- 在会话 A 里换模型：只改 A 的服务端 `session.model`，同时更新前端 `recent`；
- 会话 B 的模型不受影响；
- 新建会话：`recent` → 免费第一个 → 服务器默认；
- 切回 A 显示 A 的模型，切回 B 显示 B 的模型。

例子：

```text
A = A 模型，B = B 模型
新建 C          → C = B（recent=B）
在 A 里改成 A   → A = A，B 仍然 B，recent=A
新建 D          → D = A（recent=A）
切回 B          → B 仍然 B
```

所以两个会话不会互相覆盖；`recent` 只决定“下一个新会话从哪个模型开始”。

- 设置 → 模型：
  - 有会话：显示“当前会话模型”，点击切换 = `POST /api/session/:id/model`；
  - 无会话：显示“最近使用的模型”，点击只更新前端 recent；
  - 没有 recent 时显示免费的第一个模型；再没有才显示服务器默认；
- 聊天左下角“模型”是同一个入口，点击打开同一个选择页；
- 新会话：recent > 免费第一个 > 服务器默认；
- 服务器默认只读展示，不在模型选择页偷偷改写。

---

## 6. 安全

写服务器配置文件是强能力：

- 只有显式的“设为服务器默认”工具才写配置；
- 显示将写入的路径；
- 不把远程配置写入做成静默行为；
- 失败不要留下半个 JSON（先构造完整字符串，再一次性写）。

---

## 7. 状态

| 项 | 状态 |
|---|---|
| 前端记住最近使用模型 | `[已实现]` |
| 当前会话模型可切换 | `[已实现]` |
| 新会话 recent > 免费 > 服务器默认 | `[已实现]` |
| 服务器默认只读展示 | `[已实现]` |
| Provider API Key / OAuth 连接 | `[已实现]`（首版） |
| `/model` 与设置页共用模型选择 | `[已实现]` |
| `env` / `command` 连接方式 | `[进行中]`（env 只展示变量名） |
| 显式“设为服务器默认”工具 | `[计划]`（API 已备） |
| 官方 Mobile 对齐 | `[计划]` |
