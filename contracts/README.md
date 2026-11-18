# `:contracts` — the series' only shared API

Tiny Android library (`com.a2heng.androc.contracts`) with no Android
framework types and no dependencies.

| Type | Purpose |
|---|---|
| `RuntimeConnection` | Standard opencode endpoint: base URL + `opencode` + password |
| `RuntimeState` | `STOPPED` / `STARTING` / `READY` / `FAILED` |
| `RuntimeController` | What the UI may ask a kernel to do |
| `ToolsController` | What the UI may ask the optional tools bundle to do |
| `Pairing` | `/auth/connect/<code>` link and QR JSON |
| `DiscoveryContract` | Optional local ContentProvider names |

Rules:

1. UI never imports the kernel; kernel never imports the UI or tools.
2. The official opencode Android app is integrated through `Pairing` + the
   HTTP API, never through this module.
3. Bump the major version only for breaking changes; products pin a version.
