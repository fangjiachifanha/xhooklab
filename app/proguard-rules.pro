# Xposed entry must be kept
-keep class com.xhooklab.XHookEntry { *; }
-keep class com.xhooklab.** { *; }
-keepclassmembers class * {
    @de.robv.android.xposed.* *;
}
# JNI method names must not be obfuscated
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.xhooklab.NativeHook { *; }
