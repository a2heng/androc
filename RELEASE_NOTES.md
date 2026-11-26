## 0.7.0 - 2026-11-26

- **三个发行版可并存：** `ui`（纯 UI，只连远程）、`oc`（UI + OpenCode + 工具）、`apk`（`oc` + Android 工具链：JDK/SDK/Gradle/aapt2/adb + android skills）。`ui`/`apk` 使用独立 applicationId 后缀，可与 `oc` 并存安装。
- **移除市场与插件安装：** 不再使用市场下载组件；Android 工具链内化到 `apk` APK，不再作为可安装插件。`ui` 不再从市场安装本机 OpenCode。
- **修复 Provider/API 连接：** 连接 Provider 时不再因静默失败导致列表清空或模型不刷新；连接失败会显示具体错误；已连接的 Provider 可在界面中断开（删除凭据），断开后会刷新模型/集成状态。
