cat << 'INNER_EOF' > app/src/main/java/com/example/EnvUtils.kt
package com.example

object EnvUtils {
    fun isEmu(): Boolean {
        val f = android.os.Build.FINGERPRINT
        val m = android.os.Build.MODEL
        val h = android.os.Build.HARDWARE
        android.util.Log.d("EmuCheck", "F: $f M: $m H: $h")
        return f.contains("generic") || m.contains("Emulator") || h.contains("goldfish") || h.contains("ranchu")
    }
}
INNER_EOF
