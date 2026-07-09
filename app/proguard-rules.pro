# ============================================================
# GuestGallery ProGuard / R8 Rules
# ============================================================

# --- Kotlin ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { *; }

# Kotlin Serialization — keep all @Serializable data classes
-keep @kotlinx.serialization.Serializable class ** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static ** INSTANCE;
    static ** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Compose ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- Coil ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- Biometric ---
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**

# --- Media3 / ExoPlayer ---
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.google.android.exoplayer2.** { *; }
-dontwarn com.google.android.exoplayer2.**

# --- AndroidX Navigation ---
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# --- App classes (keep all activity/fragment/provider entry points) ---
-keep class com.example.guestgallery.MainActivity { *; }
-keep class com.example.guestgallery.data.** { *; }

# --- Remove debug/logging code from release ---
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# --- General ---
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
