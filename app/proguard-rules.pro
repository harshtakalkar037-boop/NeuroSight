# Add project specific ProGuard rules here.
# Keep TFLite classes since they're accessed via reflection/JNI in places.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
