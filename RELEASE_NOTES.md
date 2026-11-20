# Androc v0.6.2

修复应用内自更新：下载完成后无法打开系统安装器，以及 thin 版误下 bundle 包。

## 修复

- **自更新按发行版取包**：thin 只下载 `androc-<ver>-thin.apk`，bundle 只下载
  `androc-<ver>-bundle.apk`；thin 不再误下 160MB 的 bundle。
- **安装器可打开缓存里的 APK**：FileProvider 配置补上 `cache/update/`，修复
  下载完成后报 `Failed to find configured root that contains .../cache/update/*.apk`。
- **下载更稳**：更新走 HTTP/1.1，失败自动重试 3 次，并用 `Range` 断点续传，
  网络中断不再从 0 开始。
- 删除未使用的 `ui` 模块 `file_paths.xml`，只保留 `app` 的 `@xml/filepaths`。

## 调试

- 新增 `/update/check`、`/update/download` 调试端点，`/state` 增加
  `updateMessage`、`updateProgress`，便于在真机上验证自更新。
