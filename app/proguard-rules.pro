# OpuntiaPad R8 rules.
# Sora Editor / tm4e parse grammars & themes with Gson and reflection: keep their models.
-keep class org.eclipse.tm4e.** { *; }
-keep class io.github.rosemoe.sora.langs.textmate.** { *; }
-keep class org.joni.** { *; }
-keep class org.jcodings.** { *; }
-dontwarn org.joni.**
-dontwarn org.jcodings.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.jspecify.annotations.**

# Gson
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
