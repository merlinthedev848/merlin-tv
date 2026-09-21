# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil rules
-keep class coil.** { *; }
-dontwarn coil.**

# Application Models and Data Classes
-keep class com.example.merlinmedia.model.** { *; }
-keep class com.example.merlinmedia.data.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}
