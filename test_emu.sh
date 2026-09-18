cat << 'INNER_EOF' > app/src/main/java/com/example/TestEmu.kt
package com.example

object TestEmu {
    val isEmu = android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("Emulator") || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu")
}
INNER_EOF
