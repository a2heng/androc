# Androc R8 rules (thin/full release)
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions

# kotlinx.serialization: generated serializers are looked up reflectively.
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp platform hooks
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
# net.i2p.crypto:eddsa references JDK-only X.509 helpers; Android uses its own.
-dontwarn sun.security.x509.**
-dontwarn sun.security.util.**

# zstd-jni binds native methods by name via JNI; R8 must not rename/strip them.
-keep class com.github.luben.zstd.** { *; }
-dontwarn com.github.luben.zstd.**

# commons-compress zstd wrapper + xz reader are used from the asset extractors.
-keep class org.apache.commons.compress.compressors.zstandard.** { *; }
-keep class org.tukaani.xz.** { *; }
