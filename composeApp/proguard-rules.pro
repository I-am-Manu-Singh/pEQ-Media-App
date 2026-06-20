# Jetpack Compose ProGuard Rules
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Media3 / ExoPlayer ProGuard Rules
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.common.** { *; }
-dontwarn androidx.media3.**

# ONNX Runtime Rules
-keep class com.microsoft.onnxruntime.** { *; }
-dontwarn com.microsoft.onnxruntime.**

# jAudioTagger Rules
-keep class net.jthink.jaudiotagger.** { *; }
-dontwarn net.jthink.jaudiotagger.**

# Ktor Client Rules (OkHttp & Core)
-keep class io.ktor.client.** { *; }
-dontwarn io.ktor.**
-dontwarn io.netty.**
-dontwarn org.apache.http.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

