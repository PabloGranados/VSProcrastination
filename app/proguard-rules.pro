# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line number info for debugging crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# === Room ===
# Keep Room entities and DAOs
-keep class com.example.vsprocrastination.data.model.** { *; }
-keep class com.example.vsprocrastination.data.dao.** { *; }
-keep class com.example.vsprocrastination.data.database.** { *; }

# === Firebase ===
# Keep Firebase model classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# === Kotlin Coroutines ===
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# === DataStore ===
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# === Enums (Difficulty, Priority) ===
-keepclassmembers enum com.example.vsprocrastination.data.model.** {
    **[] $VALUES;
    public *;
}