# Keep line numbers for Crashlytics stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keep public class * extends java.lang.Exception

# Firebase / Crashlytics
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Play services (transitive)
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# OkHttp / Okio (Ktor OkHttp engine)
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.example.f1_kmp.**$$serializer { *; }
-keepclassmembers class com.example.f1_kmp.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.f1_kmp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# App models deserialized from network
-keep class com.example.f1_kmp.data.model.** { *; }
-keep class com.example.f1_kmp.domain.model.** { *; }

# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# OSMDroid / OSMBonusPack
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
-keep class org.osmdroid.bonuspack.** { *; }
-dontwarn org.osmdroid.bonuspack.**

# AppMetrica
-keep class io.appmetrica.** { *; }
-dontwarn io.appmetrica.**

# Coil 3
-dontwarn coil3.**
