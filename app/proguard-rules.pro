# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Preserve debugging attributes if needed, but rename source files
-keepattributes Signature,AnnotationDefault,EnclosingMethod,InnerClasses
-keepattributes *Annotation*,ElementValuePairs,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Obfuscate all our custom classes heavily
-repackageclasses 'com.example.secure'
-allowaccessmodification

# Keep our main entry points (activities, services, receivers)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# Keep standard Room database components
-keep class * extends androidx.room.RoomDatabase
-keep interface * { @androidx.room.Dao *; }
-keep class * { @androidx.room.Entity *; }

# Keep data models used for serializing/deserializing or parsing
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Keep OkHttp & Retrofit classes
-keepattributes Signature
-keepattributes InnerClasses
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Keep our data classes just in case they are parsed via reflection
-keepclassmembers class com.example.data.models.** { *; }

