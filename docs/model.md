# 默认模型：只有一个 Server Default

状态：`[进行中]`。代码已开始收束，API 事实如下。

---

## 1. 原则

- **Default 只有一种：服务器默认模型（Server Default）。**
- App 不保存本地“默认模型”，不再有“本地默认 / 服务器默认”两个概念。
- 新建会话不传 model，服务端自然使用它的 default。
- 显示的是它，修改的也是它。

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

### 3.1 通用（所有内核）

`OcClient.setDefaultModel(ref)`：

1. `GET /api/config` → 取最高优先级、带 `path` 的 `document`；
2. `GET /api/fs/read/<path>` 读原始 JSON；
3. 保留其他字段，写入：
   ```json
   "model": "providerID/modelID"        // variant 时 "providerID/modelID#variant"
   ```
4. `POST /api/experimental/fs/write?path=<abs>` 写回；
5. 轮询 `GET /api/model/default`，等待 `config.updated` 生效；
6. 返回成功/失败。

适用：本机、局域网、远程——只要服务器开放实验性 fs.write 且配置文档可写。

### 3.2 本机回退

如果服务器没开放 `experimental/fs.write`：

- 写 `$HOME/.config/opencode-android/opencode.json` 和
  `$HOME/.config/opencode/opencode.json` 的 `model`；
- `LocalRuntime.stop()` + `LocalServerService.start()` 重启；
- 这是“本机内核”的兜底，不是主路径。

### 3.3 失败时的 UI

- 远程且不可写：显示“服务器未开放配置写入 API；请在服务器修改”，
  default 仍只读展示；
- 写入失败：保留旧值，显示错误；
- 不要偷偷改成本地 Default。

---

## 4. UI 行为

- 设置 → 默认模型：显示 `服务器默认 · provider/model`；
- 选择模型 = 写服务器配置，不是写本地偏好；
- 新建会话不再应用 App 本地默认；
- 连接远程时，仍显示服务器 default；可编辑与否由 API 能力决定；
- 模型选择器不再有“跟随服务器默认”的本地选项（因为只有一个 default）。

---

## 5. 安全

写服务器配置文件是强能力：

- 必须由用户显式点击“设为服务器默认”；
- 显示将写入的路径；
- 不把远程配置写入做成静默行为；
- 失败不要留下半个 JSON（先构造完整字符串，再一次性写）。

---

## 6. 状态

| 项 | 状态 |
|---|---|
| 移除 App 本地 default | `[进行中]` |
| `GET /api/model/default` 展示 | `[已实现]` |
| `fs.write` 通用写入 | `[进行中]` |
| 本机配置文件回退 | `[已实现]`（代码已加） |
| 远程能力探测/只读提示 | `[进行中]` |
| 官方 Mobile 对齐 | `[计划]` |
