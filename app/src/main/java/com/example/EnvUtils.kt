package com.example

import android.os.Build

object EnvUtils {
    fun isEmu(): Boolean {
        return try {
            val finger = Build.FINGERPRINT.lowercase()
            val model = Build.MODEL.lowercase()
            val hardware = Build.HARDWARE.lowercase()
            val product = Build.PRODUCT.lowercase()
            val brand = Build.BRAND.lowercase()
            val device = Build.DEVICE.lowercase()
            val manufacturer = Build.MANUFACTURER.lowercase()
            val board = Build.BOARD.lowercase()

            finger.startsWith("generic") ||
            finger.startsWith("unknown") ||
            finger.contains("generic") ||
            finger.contains("sdk_gphone") ||
            finger.contains("vbox") ||
            finger.contains("test-keys") ||
            finger.contains("cuttlefish") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for x86") ||
            model.contains("sdk_gphone") ||
            model.contains("cuttlefish") ||
            manufacturer.contains("genymotion") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            hardware.contains("vbox86") ||
            hardware.contains("cutf") ||
            product.contains("sdk_gphone") ||
            product.contains("google_sdk") ||
            product.contains("sdk") ||
            product.contains("vbox86p") ||
            product.contains("cuttlefish") ||
            board.contains("goldfish") ||
            board.contains("ranchu") ||
            board.contains("cutf") ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            "google_sdk" == product
        } catch (e: Exception) {
            true
        }
    }
}
