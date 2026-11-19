# 内置技能

启动器会把这些技能复制到：

```text
~/.config/opencode/skills/
~/.config/opencode-android/skills/
```

更新 APK 后请新建会话（或重启服务端），让 OpenCode
重新发现它们。

## 核心工作流

| 技能 | 用途 |
|---|---|
| `android-build` | 用 JDK 21/Gradle 9.9.0/SDK 36 离线构建 APK，含 16 KB 审核 |
| `android-project` | 搭建或修复 Gradle 项目 |
| `android-install` | 通过内置 adb 安装/启动/卸载 |
| `android-debug` | logcat、崩溃/ANR、安装并运行 |
| `android-16kb` | 16 KB ELF 对齐：升级预编译 `.so`，而不是拨一个开关 |

## 代码质量

| 技能 | 用途 |
|---|---|
| `android-ui` | Material 3、Compose/XML、响应式布局、无障碍 |
| `android-architecture` | MVVM、ViewModel/StateFlow、仓库层、导航 |
| `android-test` | JVM 单元测试与真机仪器化测试 |
| `android-i18n` | 中/英/西语资源、复数、RTL |
| `android-security` | 权限、导出组件、密钥、WebView |

## 平台

| 技能 | 用途 |
|---|---|
| `android-gradle` | AGP/Gradle/SDK 兼容性与构建错误 |
| `android-deps` | 依赖解析、离线缓存、冲突 |
| `android-lib-cache` | 只下载一次，把缺失的库缓存进共享的 Gradle/Maven 缓存 |
| `android-ndk` | CMake/NDK、ABI 过滤、16 KB 原生构建 |
| `android-database` | Room/SQLite/DataStore、迁移 |
| `android-networking` | OkHttp/Retrofit、超时、离线行为 |
| `android-background` | WorkManager、前台服务、Doze |
| `android-performance` | 启动、卡顿、内存、Baseline Profiles |
| `android-ai` | 端侧推理：ONNX Runtime、TFLite、llama.cpp/GGUF、模型存储、CPU/NPU 限制（MNN 现状） |
| `termux` | 内置 Termux 用户态环境：bash/git/python/node/clang、pkg/apt、adb、路径陷阱 |

## 发布

| 技能 | 用途 |
|---|---|
| `android-release` | 密钥库、签名、R8、release APK |
| `android-publish` | GitHub Releases / F-Droid / 直接分发 APK |

## 元技能

| 技能 | 用途 |
|---|---|
| `skill-installer` | 从 git、zip 或本地目录安装/列出/移除技能 |

## 添加技能

让代理使用 `skill-installer`，或直接运行：

```sh
bash ~/.config/opencode/skills/skill-installer/scripts/install.sh <source> [id]
```

只从你信任的来源安装技能：技能就是代理会照做的
指令。
