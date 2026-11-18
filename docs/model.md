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
