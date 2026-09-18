// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.hilt) apply false
  alias(libs.plugins.google.services) apply false
}

tasks.register("listFiles") {
    doLast {
        fileTree(".").forEach { file ->
            if (!file.absolutePath.contains("/build/") && !file.absolutePath.contains("/.gradle/")) {
                println("FILE: ${file.absolutePath}")
            }
        }
    }
}

tasks.register("decodeKeystore") {
    doLast {
        val base64 = java.io.File("debug.keystore.base64").readText().trim()
        val decoded = java.util.Base64.getDecoder().decode(base64)
        java.io.File("debug.keystore").writeBytes(decoded)
        println("Decoded successfully!")
    }
}
