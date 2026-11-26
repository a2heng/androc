# Androc

Androc 是 Android 上的 OpenCode 客户端与本机运行时。它把
[OpenCode](https://github.com/anomalyco/opencode) 跑在手机里的 proot + Alpine musl
沙箱中，不依赖 Termux。

## 三个发行版

| 发行版 | applicationId 后缀 | 内容 | 用途 |
|---|---|---|---|
| **ui** | `.ui` | 只有 UI | 远程客户端：连接电脑或远程 opencode |
| **oc** | （无） | UI + musl runtime + OpenCode | 本机开箱即用 |
| **apk** | `.apk` | oc + Android 工具链 | 本机 OpenCode + Android 构建/调试 |

三个发行版可并存安装。

## 构建

```sh
python scripts/fetch-opencode.py
python scripts/fetch-runtime.py
./gradlew :app:assembleUiDebug :app:assembleOcDebug :app:assembleApkDebug
```

## 运行时目录

```text
files/
├─ home/                    # $HOME
├─ runtime-core/rootfs/     # oc/apk 内置运行时
├─ packs/android/bundled/   # apk 内置 Android 工具链（skills + rootfs）
└─ logs/
```

## 文档

| 文档 | 内容 |
|---|---|
| [`docs/handover.md`](docs/handover.md) | 总览与接手 |
| [`docs/paths.md`](docs/paths.md) | 路径、挂载、rootfs 优先级 |
| [`docs/plugins.md`](docs/plugins.md) | 发行版与内置组件 |
| [`docs/commands.md`](docs/commands.md) | 构建、调试、发布 |
| [`docs/connections.md`](docs/connections.md) | 连接管理 |
| [`docs/model.md`](docs/model.md) | 模型与 Provider |

## 许可证

MIT，见 [LICENSE](LICENSE)。第三方声明见 [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)。
