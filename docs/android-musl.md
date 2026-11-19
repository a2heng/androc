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
glibc 二进制能否在 `gcompat` 下工作。

**探针结果（2026-11-19，Alpine 3.20 + `gcompat libgcc libstdc++ zlib`）：**

| 组件 | 结果 |
|---|---|
| `aapt2` | ✅ `Android Asset Packaging Tool (aapt) 2.20-13193326` |
| `zipalign` | ✅ 正常运行 |
| `apksigner` | ✅ `0.9` |
| OpenJDK | ✅ `21.0.8+9-alpine-r0` |
| Gradle | ✅ `8.7` |

结论：build-tools 的 glibc 二进制在 `gcompat + libgcc + libstdc++ + zlib`
下可用，不需要 bionic。

**最小 AGP 构建（同一 workflow 的 `assemble` job）：**

| 项 | 值 |
|---|---|
| 环境 | Alpine 3.20 + OpenJDK 21 + Gradle 9.9 + AGP 9.5.1 |
| SDK | platforms;android-36 + build-tools;36.0.0 |
| 结果 | ✅ `BUILD SUCCESSFUL in 33s` |
| 产物 | `app-debug.apk` 849.8 KB（含 APK Signing Block） |

也就是说：**JDK + Gradle + AGP + aapt2 在纯 musl 下能完整产出 APK**。
下一步把整套 rootfs 打成 `android` 插件的 musl pack，替换 termux-bionic。

## aarch64 工具链

设备是 arm64，而 Google 官方 build-tools 只有 x86_64。现有 `make-toolchain.py`
用的是：

- `lzhiyong/android-sdk-tools` 35.0.2 的 **static aarch64** build-tools
  （aapt / aapt2 / aidl / zipalign / adb 等）；
- `third_party/android-build-tools/aapt2-aarch64-16kb`（16 KB 对齐的 aapt2）。

这些是静态 aarch64 二进制，在 musl 下**不需要 gcompat**；gcompat 只是 CI 里
用 x86_64 build-tools 做可行性验证时需要。

## musl rootfs 组成

```text
packs/android/
├─ pack.json
├─ skills/                 # android-* + termux
├─ tools/                  # 构建输入：aapt2-aarch64-16kb 等（不打包）
└─ rootfs/                 # 产物，不提交
   ├─ bin/sh ...           # Alpine minirootfs
   ├─ usr/lib/jvm/...      # openjdk21 (musl)
   └─ opt/android-sdk/     # static build-tools + platforms/android-35
   └─ opt/gradle/...       # Gradle 9.9
   └─ opt/androc/env.sh
```

`scripts/build-android-rootfs.py`（market 仓库）负责组装；`build-pack.py` 把
`skills + rootfs` 打成 `android` 包。

**Rootfs 实测（CI，Alpine 3.20.9 + openjdk21 + static build-tools 35.0.2 +
platform-35 + Gradle 9.9）：**

| 项 | 值 |
|---|---|
| rootfs 未压缩 | 893 MB |
| tar | 607 MB |
| tar.xz | **299 MB** |
| 组装 + 打包 | 2m16s |

对比旧的 termux-bionic：`toolbox 266 MB + toolchain 479 MB = 745 MB`（还没算
JDK），而且需要两套用户态。musl 单 rootfs 压缩后 299 MB，体积和复杂度都降下来。

## 回滚

- rootfs 与 toolchain 都按 pack 版本安装，失败保留 previous。
- 过渡期保留 termux-bionic 作为可选 pack；确认 musl 构建稳定后再删。
