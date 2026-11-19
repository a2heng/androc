# `:contracts` — 整个系列唯一共享的 API

一个极小的 Android 库（`com.a2heng.androc.contracts`），不含 Android 框架类型，
也没有任何依赖。

| 类型 | 用途 |
|---|---|
| `RuntimeConnection` | 标准的 opencode 端点：base URL + `opencode` + 密码 |
| `RuntimeState` | `STOPPED` / `STARTING` / `READY` / `FAILED` |
| `RuntimeController` | UI 可以让 kernel 做的事 |
| `ToolsController` | UI 可以让可选工具包做的事 |
| `Pairing` | `/auth/connect/<code>` 链接和二维码 JSON |
| `DiscoveryContract` | 可选的本地 ContentProvider 名称 |

规则：

1. UI 不导入 kernel；kernel 不导入 UI 或 tools。
2. 官方 opencode Android App 通过 `Pairing` + HTTP API 集成，绝不经过这个模块。
3. 只有破坏性变更才升主版本号；产品各自固定版本。
