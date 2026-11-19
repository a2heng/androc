# Androc v0.6.1

0.6.0 之后的修正：OpenCode 与 Runtime 合成一个包，并修复内置 runtime 的解压与判定。

## OpenCode 组件

- OpenCode 现在是一个自包含包：`opencode` 二进制 + musl runtime（bash/git/python/node），安装在 `files/runtime/<version>/`。
- 安装/更新 OpenCode 不再需要单独安装 runtime。
- 更新检查与下载只走 Androc 市场；不做增量，始终下载全量包。

## 本机启动

- bundle 自带 runtime + OC，弹窗显示「已安装」；只有点「启动并登录」才启动本地 server，不后台常驻。
- 首次启动把内置 runtime 解压到 `files/runtime-core/`，路径修正为 `runtime-core/rootfs`。

## 修复

- tar 解压正确还原符号链接（`/bin/sh`、busybox applets 等）。
- proot 启动时 `LD_LIBRARY_PATH` 包含 App native lib 目录，rootfs 内预建 `/opt/androc/lib`。
- OpenCode 更新只查市场，本机更新只匹配已安装的 thin/bundle。

## 清理

- 删除 0.6.0 之前的全部 Release 与 tag。
