# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.spendsense.data.** { *; }
-keep class com.kyant.backdrop.** { *; }
-dontwarn com.kyant.backdrop.**

# SpendSenseCore JNI bindings & internal transfer models
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.spendsense.core.** { *; }
