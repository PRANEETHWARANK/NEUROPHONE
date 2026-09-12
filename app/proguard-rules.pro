# Proguard rules for NeuroPhone
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
