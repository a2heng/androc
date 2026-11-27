## 0.7.1 - 2026-11-28

- **修复 release 包点击“本机”闪退：** R8 会重命名 zstd-jni 的原生绑定类，导致 release 解压 `pack.tar.zst` 时 JNI 找不到方法。已在 `app/proguard-rules.pro` 保留 `com.github.luben.zstd.**`、`org.apache.commons.compress.compressors.zstandard.**` 和 `org.tukaani.xz.**`。
- **服务器错误自动重试：** 网络异常、HTTP 408、429（限流）和 5xx 最多重试 3 次，指数退避并遵循 `Retry-After`；重试时显示“服务器限流/服务器错误/网络错误，重试 n/3”。
- **压缩结果反馈：** `/compact` 的返回值会解析并提示：`completed`（已压缩）、`skipped`（无需压缩）、`failed`（失败原因）。
- **版本步进：** `0.7.1-androc` / `versionCode 12`。
