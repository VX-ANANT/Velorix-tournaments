-keepattributes *Annotation*, Signature, Exception, InnerClasses, EnclosingMethod
-keep class io.ktor.** { *; }
-keep class kotlinx.serialization.** { *; }

-keep class com.example.** { *; }
-dontwarn java.lang.management.**
-dontwarn org.slf4j.**

# Retrofit & Moshi
-dontwarn retrofit2.**
-dontwarn com.squareup.moshi.**
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}

# ZXing QR Code
-keep class com.google.zxing.** { *; }

# Haze Glass Effect
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**

# Lottie
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# Entrig SDK
-keep class com.entrig.** { *; }
-dontwarn com.entrig.**

# AndroidX Credentials & Play Services
-keep class androidx.credentials.** { *; }
-dontwarn androidx.credentials.**
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**
-dontwarn com.google.android.gms.**

# Coroutines, OkHttp, Okio
-dontwarn kotlinx.coroutines.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Firebase
-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }
-dontwarn android.os.ProfilingTrigger**

# Vico Charts
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
