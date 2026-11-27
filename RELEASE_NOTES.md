## 0.7.1 - 2026-11-28

- **修复 release 包点击“本机”闪退：** R8 会重命名 zstd-jni 的原生绑定类，导致 release 解压 `pack.tar.zst` 时 JNI 找不到方法。已在 `app/proguard-rules.pro` 保留 `com.github.luben.zstd.**`、`org.apache.commons.compress.compressors.zstandard.**` 和 `org.tukaani.xz.**`。
- **版本步进：** `0.7.1-androc` / `versionCode 12`。
