# 插件仓库与 CI 自动化

状态：`[计划]`。当前仍在 monorepo 里；下面是把插件拆成独立“库”的目标结构。

---

## 1. 结论

> **UI/契约/SDK 留在主仓库；每个插件是一个独立仓库 + 独立 CI + 独立发版。**
> OpenCode 运行时插件每天检查上游，上游发版就自动发插件版本；
> Android 插件也独立仓库，但**不自动发版**，只自动检查/开 PR。

---

## 2. 仓库矩阵

| 仓库 | 内容 | 发版方式 |
|---|---|---|
| `androc` | 薄 UI、`:contracts`、`:plugin-sdk`、docs、thin/bundle APK | 手动 tag |
| `androc-plugin-runtime` | 运行时插件 APK：proot + opencode + Linux rootfs | **上游 opencode 发版后自动** |
| `androc-plugin-android` | Android 插件 APK：skills + JDK/SDK/Gradle/adb/AI | 手动；CI 只检查/开 PR |
| `androc-plugin-writing` | 写作插件：skills + pandoc/typst/模板 | 手动 tag |
| `androc-packs` | `plugins.json` 聚合、payload、镜像 | 定时/被插件触发 |

每个插件仓库内部：

```text
androc-plugin-runtime/
├─ plugin/          # Service + AIDL 实现 + manifest
├─ runtime-core/    # proot/opencode/rootfs 代码
├─ scripts/         # check/build/release
└─ .github/workflows/release.yml
```

---

## 3. 共享 SDK

`:plugin-sdk` 发布 AAR：

```text
com.a2heng.opencode:plugin-sdk:<sdkVersion>
```

- `IRuntimePlugin` / `IRuntimePluginListener` AIDL；
- `PluginHost`、`PluginContract`、`PluginConnection`；
- 所有插件仓库依赖同一个 SDK 版本；
- SDK 破坏性改动升 major，插件 manifest 写 `minSdkVersion`。

---

## 4. 签名

官方插件与基础 UI **必须同签名**：`com.a2heng.androc.permission.PLUGIN` 是
`signature` 权限。

- 所有官方插件仓库共用同一个 release keystore（CI secret）；
- 社区插件不能拿这个 key：用「普通权限 + 用户确认 + 签名指纹白名单」；
- 基础 UI 安装前显示签名指纹；不信任的源标红。

---

## 5. OpenCode 运行时：每日自动更新

```text
每天 03:17 UTC
  → 查 anomalyco/opencode 最新稳定版
  → 与本仓库 opencode.version 比较
  → 有新版：
       fetch-opencode.py --latest
       build :plugin:assembleRelease
       versionName = <opencode>-<packRevision>
       tag runtime-v<version>
       GitHub Release: APK + plugin.json + sha256
       repository_dispatch → androc-packs 更新 plugins.json
  → 无新版：结束
```

要求：

- 构建、`verify-toolbox.py`、16 KB 检查通过才发；
- 失败只开 issue，不发半成品；
- 自动发版只针对**运行时插件**；基础 UI 不自动发；
- 上游是 pre-release 时默认不跟，除非手动 `workflow_dispatch`。

---

## 6. Android 插件：只检查，不自动发

```text
每周一 04:00 UTC
  → 查 JDK / SDK / build-tools / Gradle 最新
  → 有新版：开 issue/PR，附差异和构建结果
  → 人工 review 后 workflow_dispatch 发版
```

原因：Android 工具链变更会影响构建正确性，自动发版风险高；但仓库、CI、
版本号、payload 仍然独立。

---

## 7. 聚合 manifest

`androc-packs` 维护：

```json
{
  "schema": 1,
  "generated": "…",
  "plugins": [
    { "id": "opencode", "version": "2.3.2-1", "apk": "…", "sha256": "…" },
    { "id": "android", "version": "36.0-1", "apk": "…", "sha256": "…" },
    { "id": "writing", "version": "1.0.0", "apk": "…", "sha256": "…" }
  ]
}
```

- 基础 UI 只读这个 manifest；
- 插件仓库发版后通过 `repository_dispatch` 触发聚合更新；
- 也可以每天定时重建，防止漏事件；
- manifest 用 Ed25519 签名，公钥在基础 UI。

---

## 8. 版本

| 对象 | 规则 | 例子 |
|---|---|---|
| 运行时插件 | 跟 opencode + pack 修订 | `2.3.2-1` |
| Android 插件 | 跟 SDK/build-tools | `36.0-1` |
| 写作插件 | 自己 semver | `1.0.0` |
| plugin-sdk | semver | `1.0.0` |
| 聚合 manifest | 日期/序号 | `2026.11.1` |

`versionCode` 每个插件独立递增；`minAppVersion` 阻止旧 UI 装新插件。

---

## 9. 当前状态

| 项 | 状态 |
|---|---|
| 插件 SDK 骨架 | `[已实现]`（monorepo） |
| 运行时插件 APK 骨架 | `[已实现]`（可绑定，未迁移 runtime） |
| OpenCode 每日检查 workflow | `[进行中]`（`opencode-daily.yml`） |
| 运行时插件自动发版 | `[计划]`（等 runtime 迁移完成） |
| Android 插件仓库 + 周检 | `[计划]` |
| 聚合 manifest + 签名 | `[计划]` |
| 拆仓库 | `[计划]`（等插件代码稳定） |
