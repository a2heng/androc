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
