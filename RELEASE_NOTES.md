## 0.7.2 - 2026-11-28

- **修复 release 包点击“本机”闪退：** R8 会重命名 zstd-jni 的原生绑定类，导致 release 解压 `pack.tar.zst` 时 JNI 找不到方法。已在 `app/proguard-rules.pro` 保留 `com.github.luben.zstd.**`、`org.apache.commons.compress.compressors.zstandard.**` 和 `org.tukaani.xz.**`。
- **服务器错误自动重试：** 网络异常、HTTP 408、429（限流）和 5xx 最多重试 3 次，指数退避并遵循 `Retry-After`；重试状态以对话内联卡片显示（不是顶栏/Toast）。
- **压缩结果反馈：** `/compact` 的返回值会解析并在对话流中显示：`completed`（已压缩）、`skipped`（无需压缩）、`failed`（失败原因）；服务端 compaction 消息的 `summary` 用“上下文压缩”卡片展示（与思考卡片同风格，可展开/收起），失败时卡片内显示错误。
- **失败/限流可见：** 每次失败的请求都会在对话中生成“错误”卡片（含“地区不可用”等）；服务端 `session.retry.scheduled` 会显示“重试中 N”卡片（含错误原因和等待时间），不再静默留空。
- **版本步进：** `0.7.2-androc` / `versionCode 13`。
