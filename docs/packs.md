# 内置包规范

Androc 不再使用市场。运行时和 Android 工具链随 APK flavor 内置。本文定义内置包的格式、放置、解压和版本固定规则，供未来新增 bundle 复用。

## 1. 总原则

- **大包单文件**：大型工具链/rootfs 打成一个包放进 `assets/`，不要把成千上万小文件直接散在 assets。
- **已压缩不再二次压缩**：``*.tar.zst`/`*.zst`/`*.tar.xz`/`*.xz` 在 `app/build.gradle` 的 `androidResources.noCompress` 中声明，避免 APK 再压一遍。
- **小文件交给 APK**：小型配置、文本、少量脚本可以直接放 assets/res，由 APK 正常打包。
- **版本固定**：每个外部输入包在仓库根有固定版本/校验文件，构建前由脚本拉取并校验。
- **首次使用解压**：安装 APK 不解压大包；首次启动/首次使用时解压到 `files/`，写 marker，幂等。

## 2. 当前包

| 组件 | flavor | APK 内位置 | 解压位置 | 版本固定 | 运行时入口 |
|---|---|---|---|---|---|
| OpenCode musl rootfs | `oc`,`apk` | `assets/runtime/rootfs.tar.xz` | `files/runtime-core/rootfs` | `opencode.version` + `scripts/fetch-runtime.py` | `RuntimeAssets.ensure()` |
| Android 工具链 | `apk` | `assets/android/pack.tar.xz` | `files/packs/android/bundled/` | `android-pack.version` + `android-pack.sha256` + `scripts/fetch-android-pack.py` | `AndroidAssets.ensure()` |

## 3. 包格式

### 3.1 推荐格式

| 场景 | 格式 | 原因 |
|---|---|---|
| rootfs/工具链（含 symlink、权限、可执行位） | `tar.zst`（优先）/`tar.xz`（兼容） | 保留 Unix 权限/symlink；zstd 解压快，适合手机端首次安装 |
| 纯数据、无需 symlink/权限 | `zip` 或直接 assets | Android/APK 工具链原生支持；但不要用于 rootfs |
| 单个原生二进制 | 直接放 `jniLibs/` 或已签名可执行位置 | 由 APK 安装管理；不要再包一层 |

当前统一优先使用 **`tar.zst`**，解压代码兼容 `.tar.xz`。

### 3.2 tar.xz 规范

- 压缩：`tar -cJf <name>.tar.xz -C <dir> <entries...>`
- 顶层结构固定，解压目标明确：
  - runtime：顶层 `rootfs/...`
  - android：顶层 `skills/` + `rootfs/...`
- 必须保留：普通文件、目录、相对 symlink、可执行位。
- 不要包含：绝对路径、`..` 越界路径、设备文件、硬链接到包外。
- 包内路径使用 `/`，解压时必须做 canonical path 安全检查。

### 3.3 何时不要压缩

| 内容 | 处理 | 原因 |
|---|---|---|
| `*.so`/native libs | `jniLibs/` | 安装器/加载器管理，16K 对齐检查 |
| 很小的文本、JSON、脚本 | 普通 assets | APK zip 已处理，额外 xz 无收益 |
| 已经是压缩格式且不需要解包 | 视情况直接 assets + `noCompress` | 避免双重压缩 |
| 需要随机读取的单文件大资源 | 优先不包进 tar.xz | tar.xz 不利于随机读取 |

## 4. APK 内放置

```text
app/src/oc/assets/runtime/rootfs.tar.xz      # oc/apk 共用
app/src/apk/assets/android/pack.tar.xz       # apk 专用
```

规则：

- 包文件名稳定，不带动态版本号；版本由仓库根版本文件和 marker 管理。
- flavor 专属包放 flavor 的 `assets/`；共用包放 `oc` 并由 `apk` sourceSet 复用。
- `build.gradle` 必须声明：

```gradle
androidResources {
    noCompress += ['tar.xz', 'xz']
}
```

## 5. 解压与安装

### 5.1 目录

| 组件 | 目标目录 | marker |
|---|---|---|
| runtime | `files/runtime-core/`，rootfs 在 `files/runtime-core/rootfs/` | `files/runtime-core/.installed-v2` |
| android | `files/packs/android/bundled/` | `files/packs/android/bundled/.installed-v1` |

### 5.2 时机

- App 启动/进入本机运行时前，由 `LocalServerService` 调用 `RuntimeAssets.ensure()` / `AndroidAssets.ensure()`。
- 解压必须在后台线程，不阻塞 UI 主线程。
- 有 `LOCK` 串行化，防止并发解压互相删除半成品。

### 5.3 流程

1. 检查 marker + 目标目录是否完整；完整则直接返回。
2. 检查 asset 是否存在；不存在则返回 null/不可用。
3. 删除旧的半成品目标目录。
4. 流式解压 `assets/...tar.xz` 到目标目录：`BufferedInputStream -> XZInputStream -> TarArchiveInputStream`。
5. 对每个 entry 做 canonical path 校验；支持 dir/file/symlink；恢复可执行位。
6. 成功后写 marker；失败删除半成品并抛错。

### 5.4 失败策略

- 解压失败不留半成品：删除目标目录。
- 下次启动可重试。
- 日志标签：`OpenCodeRuntimeAssets`、`OpenCodeAndroidAssets`。

## 6. 版本固定

| 文件 | 作用 |
|---|---|
| `opencode.version` | OpenCode 上游版本 |
| `android-pack.version` | Android 工具链包版本 |
| `android-pack.sha256` | Android 工具链包 sha256 |
| `scripts/fetch-opencode.py` | 拉取 OpenCode 二进制 |
| `scripts/fetch-runtime.py` | 拉取 runtime rootfs |
| `scripts/fetch-android-pack.py` | 拉取 Android 工具链包并校验 sha256 |

规则：

- 仓库不提交大型 tar.xz 产物，只提交版本/校验文件和 fetch 脚本。
- CI/发版前先 fetch，再 assemble。
- 更新包时同时更新版本文件、marker 版本（如格式变化）、文档和 RELEASE_NOTES。

## 7. 未来新增 bundle 清单

新增一个内置包时，至少补齐：

1. 版本固定文件（版本 + sha256/签名）。
2. `scripts/fetch-*.py`，支持缓存、校验、幂等。
3. `app/src/<flavor>/assets/<component>/...tar.xz` 放置约定。
4. `*Assets.java`：`ensure()/rootfs()/hasBundled()/marker/LOCK/安全解压`。
5. `build.gradle` 的 `noCompress` 覆盖后缀。
6. `docs/packs.md`、`docs/paths.md`、`docs/commands.md`、`AGENTS.md`。
7. 如需用户可见，更新设置页/README/RELEASE_NOTES。
