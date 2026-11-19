# Androc 仓库规则（改路径或命令之前先读）

**设计信条：** 复杂度由机制承担，用户/代理保持透明。只有一个运行时（proot）、
每样东西只有一个权威路径、不做命令名白名单，任何失败都必须能自愈或续跑。
如果某个改动让用户必须了解内部细节，那它大概率是错的。

1. **先读 [`docs/handover.md`](docs/handover.md)、
   [`docs/design.md`](docs/design.md)、
   [`docs/paths.md`](docs/paths.md)、[`docs/commands.md`](docs/commands.md)、
   [`docs/series.md`](docs/series.md)、[`docs/plugins.md`](docs/plugins.md)、
   [`docs/model.md`](docs/model.md) 和 [`docs/connections.md`](docs/connections.md)。**
   它们是交接、产品模型、路径、proot 挂载、命令执行、打包、插件、服务端默认模型
   和连接方式的契约。
2. **绝不在 proot 之外执行 Termux ELF。** 唯一例外是
   `CommandResolver.SAFE_NATIVE` 里的叶子工具（它们用 `linker64` +
   `LD_LIBRARY_PATH` 启动）。拿不准就交给 proot。
3. **不要往 `SAFE_NATIVE` 里加命令**，除非它从不 `exec` 其他程序。
   解释器、构建工具、包管理器、git 辅助命令一律不许进。
4. **每个启动必需文件**都必须同时出现在 `LocalToolbox.ESSENTIALS`、
   `ESSENTIAL_ROOTS`（拉取脚本）、`bootstrap.txt` 和 `docs/paths.md` 里。
5. **`bootstrap.txt` 必须包含 `bash` 和 `readline`**，否则 dpkg 一旦失败，
   prefix 会坏成 `library "libreadline.so.8" not found`。
6. **不要在 shell 脚本里硬编码** `com.a2heng.androc`、`com.termux`、
   `nativeLibraryDir` 或 JDK/SDK 路径。用 `$HOME`、`$PREFIX`、`$TMPDIR`；
   `nativeLibraryDir` 路径在运行时生成。
7. **`oc-proot` 必须保持幂等**（`OC_ENGINE=proot` 不会套娃），并且必须把调用者的
   `$PWD` 保留为 `proot -w`；强行改成 `$HOME` 会让 `$PWD/project` 指向错误的目录。
8. **不要在 `$HOME/bin` 里再放一份生成的包装脚本**（toolbox 已经存在时）；
   `LocalRuntime.cleanLegacyHomeBin` 会清掉旧的。
9. **用固定版本的本地工具链构建/验证：**

   ```sh
   source /d/AndroidDevToolchain/env.sh
   cd /d/005_dev/androc
   python scripts/verify-toolbox.py
   ./gradlew :app:assembleDebug
   ```

10. **打发布 tag 之前：** `python scripts/verify-toolbox.py` 必须通过；
    `python scripts/check-opencode-latest.py --strict` 必须通过，且
    `opencode.version` 必须是最新的上游版本。发布 CI 会跑这两项；
    任一失败就不要打 tag。
11. **改行为时同时更新文档和技能。** 文档是给人做仓库评审的；技能和
    `$HOME/.config/*/AGENTS.md` 才是代理启动时真正加载的东西。60 秒验收测试在
    `docs/paths.md` 里，要在真机上跑（`oc-engine status` 必须显示 `proot`、
    `oc-doctor`、`bash -lc 'git --version'`、`python3 --version`、
    `pkg install -y jq`）。
12. **`ui/` 是唯一的 UI 来源。** 旧的 `a2heng/opencode-android-ui` 仓库已不再
    维护；永远不要再引入 submodule 或第二份拷贝。保持
    [`docs/mobile-integration.md`](docs/mobile-integration.md) 里的行为不变量
    （默认模型、已保存连接、保活、路径规范化/补全）。与独立 UI 构建相比，只允许
    品牌、更新仓库地址、Androc 的 HOME 文件预设和宿主注入的 Termux shell 不同。
13. **相对路径不是可选项。** `normPath` 只修分隔符；不以 `/`（或盘符）开头的路径
    必须先经 `AppModel.resolvePath` / `Markdown.resolveFileRef` 补全，才能发给
    `/api/fs/read`。服务端会把自己的 cwd 当作基准去解析裸相对路径，结果要么悄悄
    打开错文件，要么 404。
14. **`RELEASE_NOTES.md` 是增量的。** 只保留当前要发的版本（相对上一个 tag），
    用中文写，不写历史章节，不写英文。CI 会把整个文件当作 GitHub Release 正文，
    堆叠历史会被原样发布出去。
15. **提交信息：ASCII 头之后全部用中文。** 格式为 `type(scope): 中文摘要`，
    后面跟中文正文。头部的 type 和括号里的 scope 保持小写 ASCII；冒号之后和整个
    正文都是中文。`type` 只允许：`feat`、`fix`、`perf`、`refactor`、`docs`、
    `test`、`build`、`ci`、`chore`、`revert`。`scope` 只允许：`ui`、`app`、
    `server`、`proot`、`toolbox`、`plugin`、`skill`、`model`、`conn`、`chat`、
    `fs`、`build`、`release`（`scope` 可省略；不要自创，也不要在 type 或括号里
    写中文）。

Windows 工具链位于 `D:\AndroidDevToolchain`，它的 README 说明了唯一的
JDK/SDK/PATH 配置方式。
