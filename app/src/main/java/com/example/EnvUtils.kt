package com.example

import android.os.Build

object EnvUtils {
    fun isEmu(): Boolean {
        return try {
            val finger = Build.FINGERPRINT
            val model = Build.MODEL
            val hardware = Build.HARDWARE
            val product = Build.PRODUCT
            val brand = Build.BRAND
            val device = Build.DEVICE
            val manufacturer = Build.MANUFACTURER

            finger.startsWith("generic") ||
            finger.startsWith("unknown") ||
            finger.contains("generic") ||
            finger.contains("sdk_gphone") ||
            model.contains("google_sdk") ||
            model.contains("Emulator") ||
            model.contains("Android SDK built for x86") ||
            model.contains("sdk_gphone") ||
            manufacturer.contains("Genymotion") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            hardware.contains("vbox86") ||
            product.contains("sdk_gphone") ||
            product.contains("google_sdk") ||
            product.contains("sdk") ||
            product.contains("vbox86p") ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            "google_sdk" == product
        } catch (e: Exception) {
            true
        }
    }
}
