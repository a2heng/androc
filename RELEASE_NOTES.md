## 0.7.5 - 2026-11-28

- **本机 DNS 修复：** proot 的 `/etc/resolv.conf` 改为「当前网络 DNS 优先 + `223.5.5.5` / `119.29.29.29` 兜底」，本地 DNS 失败 1 秒即切换；不再写死 `1.1.1.1` / `8.8.8.8`。
- **错误文案：** DNS 解析失败、连接超时、401（Key 无效）、402/429（余额/限流）分别给出明确提示，不再一律显示「检查网络连接」。
- **手机场景提示词：** 启动时写入 `~/.config/opencode-android/AGENTS.md` 和 `~/.config/opencode/AGENTS.md`，内容是本机 OpenCode 的能力清单（proot、HOME、文件/终端/网络、apk 版额外工具），不要求用户修环境。
- **skill-installer：** `description` 明确「只在安装/列出/删除技能时使用」，避免普通对话误加载。
- **版本步进：** `0.7.5-androc` / `versionCode 16`。
