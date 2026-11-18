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
