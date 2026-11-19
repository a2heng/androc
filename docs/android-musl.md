# Android 插件迁到 musl（去 bionic / Termux）

状态：`[进行中]`。决定：Android 插件不再以 termux-bionic 作为专用 rootfs，
改为 musl/Alpine。Termux 是 MVP 阶段为了尽快跑通 JDK/aapt2 引入的临时方案，
现在深化插件体系，要把这笔技术债还掉。

## 目标

- 只保留一套用户态：**musl/Alpine**。
- Android 插件 = `android-*` skills + musl rootfs（JDK / SDK / Gradle / aapt2 /
  zipalign / apksigner / adb）。
- 通用运行时、语言插件、写作插件也都用同一套 musl 根。
- `runtime-core` 是 musl rootfs；不再有 termux-bionic rootfs。

## 为什么

- 去掉 ~1.1 GB 的 Termux 前缀和重复的 glibc/bionic 库。
- opencode 本身就是 musl，agent、shell、构建工具统一到一个 libc / loader。
- proot 只挂一套路径，启动更快，少一类库路径不匹配的问题。
- 包管理只剩 apk/apk-tools，skills 不用区分 `$PREFIX` 和 `/usr`。
- CI 只验证一套 rootfs，增量包和回滚都更简单。

## 卡点与验证顺序

| # | 组件 | 问题 | 方案 |
|---|---|---|---|
| 1 | `aapt2` | build-tools 里的 glibc/bionic 二进制 | 先试 gcompat；不行就静态版或自编 musl 版 |
| 2 | `zipalign` / `apksigner` | 同上 | 同上 |
| 3 | JDK | bionic 已验证 | Alpine openjdk17/21（musl） |
| 4 | Gradle + AGP | 需要 musl JDK 与 native 组件 | 在 musl 下跑最小 AGP 工程 |
| 5 | NDK clang | glibc 链接 | gcompat 或 NDK 的 musl 变体 |
| 6 | adb | Termux 的 adb 是 bionic | 改用 App 内 `DeviceApi` |

## 探针

`androc-market/.github/workflows/android-musl.yml` 在 Alpine 容器里直接跑
`aapt2 version`、`zipalign`、`apksigner --version`，先确认 build-tools 的
glibc 二进制能否在 `gcompat` 下工作。跑通后再做 JDK + Gradle 的最小构建。

## 回滚

- rootfs 与 toolchain 都按 pack 版本安装，失败保留 previous。
- 过渡期保留 termux-bionic 作为可选 pack；确认 musl 构建稳定后再删。
