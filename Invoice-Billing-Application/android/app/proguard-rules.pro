# SwiftLab Android ProGuard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @org.jetbrains.annotations.Nullable <fields>;
}
-keep class com.example.swiftlab.data.remote.dto.** { *; }
-keep class com.example.swiftlab.data.local.entity.** { *; }
