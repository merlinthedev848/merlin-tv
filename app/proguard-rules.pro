# ==============================================================================
# Merlin TV — ProGuard & R8 Optimization and Shrinking Rules
# ==============================================================================

# ── Kotlin Coroutines & Flow ──
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── Hilt & Dagger ──
-dontwarn dagger.hilt.**
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends androidx.lifecycle.ViewModel
-keep class * extends androidx.room.RoomDatabase
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @javax.inject.Inject class * { *; }
-keep @javax.inject.Singleton class * { *; }

# ── Room Database ──
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase {
    <methods>;
}

# ── OkHttp & Okio ──
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ── Media3 & ExoPlayer ──
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.exoplayer.hls.** { *; }
-keep class androidx.media3.ui.** { *; }

# ── Coil Image Loading ──
-keep class coil.** { *; }
-dontwarn coil.**
-keep class coil.compose.** { *; }

# ── Timber Logging ──
-keep class timber.log.** { *; }
-dontwarn timber.log.**

# ── Application Models & Data Classes ──
-keep class com.example.merlinmedia.model.** { *; }
-keep class com.example.merlinmedia.data.local.entity.** { *; }
-keep class com.example.merlinmedia.data.local.dao.** { *; }
-keep class com.example.merlinmedia.data.** { *; }

# ── Annotations & Keep ──
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}
