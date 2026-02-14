# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.onlinetts.**$$serializer { *; }
-keepclassmembers class com.example.onlinetts.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.onlinetts.** {
    kotlinx.serialization.KSerializer serializer(...);
}
