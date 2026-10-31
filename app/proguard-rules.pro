# Proguard / R8 optimization rules for Focus Reminder

# Keep Firestore DTO models from field obfuscation
-keepclassmembers class com.example.focusreminder.platform.squad.** {
    <fields>;
    <init>();
}

-keepclassmembers class com.example.focusreminder.domain.model.** {
    <fields>;
    <init>();
}

# Keep Kotlin reflection metadata if needed
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod

# Suppress harmless warnings from bundled Google/Android dependencies
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
