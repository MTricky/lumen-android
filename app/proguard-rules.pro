# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlinx Serialization ───────────────────────────────────────────
# Keep all @Serializable classes and their generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all @Serializable data classes in the app
-keep,includedescriptorclasses class com.app.lumen.**$$serializer { *; }
-keepclassmembers class com.app.lumen.** {
    *** Companion;
}
-keepclasseswithmembers class com.app.lumen.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable annotated classes
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static ** INSTANCE;
}

# ── Firebase ────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ── Mixpanel ────────────────────────────────────────────────────────
-dontwarn com.mixpanel.android.**
-keep class com.mixpanel.android.** { *; }

# ── RevenueCat ──────────────────────────────────────────────────────
-keep class com.revenuecat.purchases.** { *; }

# ── Google Play In-App Review ───────────────────────────────────────
-keep class com.google.android.play.core.** { *; }

# ── Facebook SDK ────────────────────────────────────────────────────
-keep class com.facebook.** { *; }

# ── Google Generative AI ────────────────────────────────────────────
-keep class com.google.ai.client.generativeai.** { *; }

# ── Google Play Services ─────────────────────────────────────────────
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# ── Kotlin ──────────────────────────────────────────────────────────
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
