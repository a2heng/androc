# `:tools` — 给 opencode 代理用的 Android 工具

可选的第三层。它把一个能跑的 opencode kernel 变成手机上的 Android IDE：
代理可以装包、构建 APK、调用手机 API。

kernel 必须能在**零工具**的情况下运行：缺工具只会少掉构建技能，绝不能让服务起不来。

## 职责

- 内置技能（`android-build`、`android-project`、`termux` 等）
- 暴露给代理的设备 API（剪贴板、Toast、震动、Wi-Fi 等）
- JDK 21、Android SDK/platform、aarch64 `aapt2`、Gradle、Maven 种子缓存
- `setup-toolchain.sh`、`build.sh`、`check-16kb.py`、缓存清理脚本
- toolbox/包清单与离线拉取/校验脚本
- `ToolsController` 实现

## 迁移对照表（第 3 阶段）

| 现在 | 去向 |
|---|---|
| `app/src/main/assets/skills/**` | `tools/src/main/assets/skills/**` |
| `app/src/main/assets/tools/**` | `tools/src/main/assets/tools/**` |
| `app/src/main/assets/toolchain/**` | `tools/src/main/assets/toolchain/**` |
| `app/src/main/java/.../DeviceApi.java` | `tools/.../DeviceApi.java` |
| `scripts/fetch-toolbox.py`（工具链部分） | 留在 tools |
| `scripts/make-toolchain.py`、`verify-toolbox.py` | 留在 tools |

`toolbox/*.deb`（bash、readline、JDK？）在 shell 必需时留在 kernel；只有构建链
相关的包属于 tools。这一阶段要把清单拆清楚：`bootstrap.txt` → kernel，
SDK/Gradle → tools。

## 不能依赖

`:ui`、Compose、服务进程。工具是被 kernel/代理调用的，不是反过来。

## 验收

- 完整版产品：`bash ~/.config/opencode/skills/android-build/scripts/build.sh /path debug` 成功
- 简单版产品（只有 kernel）：服务照常启动；构建技能报「缺少工具链」而不是崩溃
- `oc-doctor` 能区分 kernel 必需项和工具链附加项
