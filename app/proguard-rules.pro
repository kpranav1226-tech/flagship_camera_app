# ClearShot AI ProGuard & R8 Optimization Rules
-keepattributes *Annotation*
-dontwarn okio.**

# Preserve all application classes, ViewModels, and UI composables
-keep class com.clearshot.ai.** { *; }

# OpenCV Java & JNI bridge preservation
-keep class org.opencv.** { *; }
-keepclassmembers class org.opencv.** { *; }
-dontwarn org.opencv.**

# CameraX & Camera2 interop
-keep class androidx.camera.** { *; }
-keepclassmembers class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Jetpack Compose runtime
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**
