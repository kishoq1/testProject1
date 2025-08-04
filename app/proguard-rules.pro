# Rules for NewPipeExtractor
-keep class org.schabi.newpipe.extractor.** { *; }
-keep public class org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeCommentsExtractor { *; }
-dontwarn org.schabi.newpipe.extractor.**

# Rules for nanojson
-keep class com.grack.nanojson.** { *; }
-dontwarn com.grack.nanojson.**

# Rules for OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**