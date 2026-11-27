## 0.7.1 - 2026-11-28

- **修复 release 包点击“本机”闪退：** R8 会重命名 zstd-jni 的原生绑定类，导致 release 解压 `pack.tar.zst` 时 JNI 找不到方法。已在 `app/proguard-rules.pro` 保留 `com.github.luben.zstd.**`、`org.apache.commons.compress.compressors.zstandard.**` 和 `org.tukaani.xz.**`。
- **服务器错误自动重试：** 网络异常、HTTP 408、429（限流）和 5xx 最多重试 3 次，指数退避并遵循 `Retry-After`；重试状态显示在聊天界面顶部，不是 Toast。
- **压缩结果反馈：** `/compact` 的返回值会解析并在聊天界面顶部显示：`completed`（已压缩）、`skipped`（无需压缩）、`failed`（失败原因）；同时把服务端 compaction 消息的 `summary` 渲染到聊天正文（“上下文压缩”卡片），失败时显示错误。
- **服务端错误可见：** 包括“地区不可用”一类的服务端提示，会在聊天界面顶部错误条显示，并兼容 `data.error.message` / `data.message` 等嵌套格式。
- **版本步进：** `0.7.1-androc` / `versionCode 12`。
