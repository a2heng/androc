# Androc 仓库规则

**设计信条：** 复杂度由机制承担，用户/代理保持透明。只有一个运行时（proot + musl），
每样东西只有一个权威路径，任何失败都能自愈或续跑。如果某个改动让用户必须了解
内部细节，它大概率是错的。

1. **先读文档：** [`docs/handover.md`](docs/handover.md)、
   [`docs/design.md`](docs/design.md)、[`docs/paths.md`](docs/paths.md)、
   [`docs/commands.md`](docs/commands.md)、[`docs/series.md`](docs/series.md)、
   [`docs/plugins.md`](docs/plugins.md)、[`docs/model.md`](docs/model.md)、
   [`docs/connections.md`](docs/connections.md)、
   [`docs/android-musl.md`](docs/android-musl.md)。
2. **运行时只有一套：** proot + Alpine musl rootfs。Android 工具在同一个
   rootfs 里通过 `/opt/androc/env.sh` 使用。
3. **rootfs 优先级唯一：** `files/packs/android/<ver>/rootfs`（装了 Android 组件时
   优先，自带工具链）→ `files/runtime/<ver>/rootfs`（市场 OC 包）→
   `files/runtime-core/rootfs`（bundle 内置）。
   新增来源时同步 `MuslRootfs.dir` 与 `docs/paths.md`。
4. **路径用运行时值：** `$HOME`（`files/home`）、`$PWD`、`nativeLibraryDir` 在运行时
   生成；脚本里写死包名或 JDK/SDK 路径会让 App 换签名后失效。
5. **市场只有一个包：** `android`（工具链 + skills），`index.json` + `index.json.sig`，
   全量下载、无增量；OpenCode 与 runtime 随 App 更新。
6. **用固定工具链构建/验证：**

   ```sh
   python scripts/fetch-opencode.py
   python scripts/fetch-runtime.py
   ./gradlew :app:assembleThinDebug :app:assembleBundleDebug
   python scripts/check-opencode-latest.py --strict
   ```

7. **发版前：** `check-opencode-latest.py --strict` 通过，`opencode.version` 是最新
   上游版本，thin/bundle 两个 APK 都能装能启动。
8. **改行为时同步文档和技能。** 文档给仓库评审；技能和
   `$HOME/.config/*/AGENTS.md` 才是代理启动时加载的内容。
9. **`ui/` 是唯一的 UI 来源。** 不引入第二份 UI 拷贝；保持
   [`docs/mobile-integration.md`](docs/mobile-integration.md) 的行为不变量。
10. **相对路径先补全。** `normPath` 只修分隔符；不以 `/` 开头的路径必须先经
    `AppModel.resolvePath` / `Markdown.resolveFileRef`，再发给 `/api/fs/read`。
11. **`RELEASE_NOTES.md` 是增量的。** 只保留当前要发的版本，用中文写，不写历史
    章节。CI 会把整个文件当作 Release 正文。
12. **提交信息：ASCII 头之后全部用中文。** 格式 `type(scope): 中文摘要`，正文中文。
    `type` 只用：`feat`、`fix`、`perf`、`refactor`、`docs`、`test`、`build`、
    `ci`、`chore`、`revert`。`scope` 只用：`ui`、`app`、`server`、`proot`、
    `toolbox`、`plugin`、`skill`、`model`、`conn`、`chat`、`fs`、`build`、
    `release`（可省略）。
