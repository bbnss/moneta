# Ktor usa la reflection su alcuni engine: teniamo i nomi delle classi di servizio.
-keepclassmembers class io.ktor.** { *; }
-dontwarn org.slf4j.**
-dontwarn kotlinx.coroutines.debug.**

# kotlinx.serialization: i serializer generati vengono cercati per nome.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class it.bbnss.moneta.**$$serializer { *; }
-keepclassmembers class it.bbnss.moneta.** {
    *** Companion;
}
-keepclasseswithmembers class it.bbnss.moneta.** {
    kotlinx.serialization.KSerializer serializer(...);
}
