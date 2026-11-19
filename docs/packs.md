# Pack 仓库与发布：能力包怎么跟版本走

本文回答：kernel/tools 这些**能力包**应该跟 App 一起发，还是单独维护？
结论：

> **代码仍在一个仓库；大体积的“能力包产物”单独放一个仓库发布。**
> 能力包有自己的 release 节奏，App 通过签名的 `manifest.json` 拉取。

---

## 1. 为什么不把 pack 塞进 App 的 release

| 做法 | 问题 |
|---|---|
| 每个 App release 都带 700 MB tools | App release 变成 GB 级；用户每次更新都重下；CI 慢 |
| pack 跟 App 版本绑死 | JDK/SDK/Gradle 几个月不变，却被迫跟 App 一起发版 |
| pack 放主仓库 release assets | 主仓库 release 被大文件淹没；镜像/限速难做 |
| pack 放独立仓库 | 需要签名/兼容性约束，但换来独立节奏 |

App 和 pack 的变更频率完全不同：

| 东西 | 变更频率 | 适合 |
|---|---|---|
| UI / 连接 / 设置 | 每周 | App release |
| opencode 二进制 | 上游发版时 | kernel pack |
| Termux userland | 偶尔 | kernel pack |
| JDK / SDK / Gradle | 几个月 | tools payload |
| skills / build.sh | 跟 App | tools bootstrap（随 App） |

---

## 2. 仓库怎么分

```text
a2heng/androc                 # 主仓库：App + UI + kernel 代码 + bootstrap
├─ app/ ui/ kernel/ tools/ contracts/
├─ docs/
├─ scripts/
└─ packs/                     # 只有 manifest schema、构建脚本、测试
   ├─ schema/manifest.schema.json
   ├─ build-kernel-pack.py
   └─ build-tools-pack.py

a2heng/androc-packs           # 新仓库：只放 pack 的构建配方和发布产物
├─ kernel/
│  ├─ recipe.json             # opencode 版本、.deb 列表、校验
│  └─ scripts/
├─ tools/
│  ├─ jdk.json                # 版本、下载 URL、sha256
│  ├─ sdk.json
│  └─ gradle.json
├─ manifest.json              # 当前兼容矩阵（签名的发布产物）
└─ .github/workflows/release.yml
```

**为什么 bootstrap 不单独拆**：

- `tools.bootstrap`（skills + build.sh + setup-toolchain.sh）只有几 MB；
- 它和 App 的 UI/命令/路径强耦合，应该跟 App 同一个版本；
- 把它放 App 里，thin 构建也能“开箱有脚本”，只是没有 JDK/SDK/Gradle。

**为什么 kernel pack 也放 packs 仓库**：

- opencode 二进制 ~240 MB，Termux userland ~100–260 MB；
- 跟上游 opencode 版本走，不跟 App 走；
- 但 kernel 的**代码**（proot 引擎、启动逻辑）仍在主仓库。

---

## 3. 发布节奏

```text
App release:      androc v0.5.0        → thin/bundle APK + bootstrap
Kernel release:   kernel 2.3.2-1       → opencode + userland
Tools release:    tools 36.0-1         → JDK/SDK/Gradle/Maven
Manifest release: packs-2026.11.1      → 指向上面三个的兼容组合
```

- **thin APK**：启动后读 `manifest.json`，按需下载/更新；
- **bundle APK**：内置某一版 pack 快照，离线可用；`manifest.json` 仍可用于后续更新；
- 用户可以在设置里看到“已安装 / 可更新”，不必换 APK。

---

## 4. manifest 与签名

稳定的下载入口（建议）：

```text
https://github.com/a2heng/androc-packs/releases/latest/download/manifest.json
```

`manifest.json` 至少包含：

```json
{
  "schema": 1,
  "channel": "stable",
  "generated": "2026-11-18T00:00:00Z",
  "packs": [
    {
      "id": "kernel",
      "version": "2.3.2-1",
      "abi": "arm64-v8a",
      "minAppVersion": 8,
      "minContracts": "1.0.0",
      "size": 247667816,
      "sha256": "…",
      "url": "https://github.com/a2heng/androc-packs/releases/download/kernel-2.3.2-1/kernel.pack"
    },
    {
      "id": "tools.payload",
      "version": "36.0-1",
      "components": ["jdk", "sdk", "gradle", "maven"],
      "minAppVersion": 8,
      "size": 734003200,
      "sha256": "…",
      "url": "…/tools-36.0-1.pack"
    }
  ],
  "signature": "ed25519:…"
}
```

规则：

1. **签名**：manifest 用 Ed25519 签；公钥硬编码在 App（`BuildConfig` 或
   `:contracts`）。没有有效签名的 manifest 一律拒绝。
2. **双重校验**：下载后校验 `sha256` + `size`；不匹配直接删 `.part`。
3. **兼容性**：`minAppVersion` / `minContracts` / `abi` 不匹配就提示升级，
   不尝试“也许能跑”。
4. **回滚**：保留上一版 pack 的 manifest 与文件，安装失败自动回滚。
5. **镜像**：manifest 里可以给 `mirrors` 数组；下载失败按顺序尝试。
6. **离线**：设置页“从文件导入”读取本地 zip，校验同一套 manifest/签名。

> pack 里是可执行文件（opencode、aapt2、Gradle），安全等级等同 APK。
> 不要因为“只是数据”就跳过签名。

---

## 5. 兼容矩阵

| App | contracts | kernel | tools | 说明 |
|---|---|---|---|---|
| 0.5.x | 1.0 | 2.3.x-1 | 36.0-1 | 当前 |
| 0.6.x | 1.1 | 2.3.x-1 | 36.0-1 | 增加 pack 更新 UI |

App 只接受 `minAppVersion <= 自身 versionCode` 且 `minContracts <= 自身` 的 pack。
pack 可以比 App 旧；新 pack 不能强行装到旧 App。

---

## 6. CI 流程

### 主仓库 `androc`

```text
push/tag → verify-toolbox → check-opencode-latest --strict
        → assembleThin + assembleBundle
        → 上传 APK + bootstrap（不含大 payload）
```

### packs 仓库 `androc-packs`

```text
手动/定时 → 检查 opencode/JDK/SDK/Gradle 最新
        → 构建 kernel.pack / tools.pack
        → 计算 sha256、生成 manifest.json、签名
        → GitHub Release: kernel-x.y.z-N / tools-x.y-N / manifest
```

- 大文件只在 packs 仓库的 release，主仓库 release 保持轻量；
- packs 仓库的 CI 只跑 Python/zip/签名，不需要 Android SDK；
- 镜像可以是 R2/S3/自建 CDN，manifest 里换 `url` 即可。

---

## 7. 现在怎么落地

| 阶段 | 做什么 |
|---|---|
| **现在** | 主仓库保留 bundle 构建；`tools.bootstrap` 随 App；`tools.payload` 可删除/重装（已接好 UI） |
| **下一步** | 建 `androc-packs`；先发布 `tools-36.0-1` 和 `kernel-2.3.2-1`；App 读取签名 manifest |
| **之后** | thin APK 默认按需下载；bundle APK 内置快照；设置页显示更新 |
| **最后** | 需要时再把 kernel/tools **代码**拆 repo；现在不做，避免版本漂移 |

**不要**现在就把 `:kernel` / `:ui` 代码拆成独立仓库。先拆**产物**，代码边界
已经在 `:contracts` 里固定；等 pack manifest 稳定后再拆代码仓库。
