# musl 运行时与 Android 构建包

状态：**已落地**。唯一运行时是 proot + Alpine musl；Android 工具链在同一个
rootfs 内运行。

## 组成

| 层 | 内容 |
|---|---|
| proot | `nativeLibraryDir/libproot.so` + `libproot_loader.so` |
| rootfs | Alpine minirootfs（musl）+ bash/git/python/node 等 |
| OpenCode | `kernel-opencode` 包：`opencode` 二进制 + 同一份 rootfs |
| Android | `android` 包：android-* skills + rootfs 增量（JDK 21、SDK 36、Gradle 9.9、静态 aarch64 build-tools） |

`bundle` APK 内置 `assets/runtime/rootfs.tar.xz`，首次启动解压到
`files/runtime-core/rootfs`；`thin` 不带 runtime，也不下载 OpenCode。
Android 包解压到 `files/packs/android/<ver>/`。

## 工具链布局

| 项 | 路径 |
|---|---|
| JDK 21 | `/usr/lib/jvm/java-21-openjdk` |
| SDK | `/opt/android-sdk`（platform 36、build-tools 36.0.0） |
| Gradle | `/opt/gradle/gradle-9.9.0` |
| 环境脚本 | `/opt/androc/env.sh` |
| JVM shim | `/usr/local/lib/fixexec.so`（`LD_PRELOAD`） |

`aapt2`、`aidl`、`adb`、`zipalign` 用静态 aarch64 构建，直接执行；JDK 的
`mprotect(RW→RWX)` 由 `fixexec.so` 在原地址重映射，JVM 正常启动。

## 实测（真机，2026-11-20）

- bundle：本机 opencode 启动、对话、Python、终端正常。
- android 包：`assembleDebug` 在手机内完成，APK 安装并启动。
- 单项验证：`aapt2 version`、`java -version`、Gradle 9.9 启动、`zipalign` 可用。

## 体积

- OpenCode 包 128 MB（压缩），解压 410 MB。
- Android 包 300 MB（压缩），解压 607 MB。
- bundle APK 164 MB（内含 runtime 压缩包 + opencode 二进制）。

## 已知限制

- 4 KB 页设备已验证；16 KB 页设备需要重新确认内置 ELF 的对齐。
- Android 包首次安装耗时取决于网络与存储写入速度。
