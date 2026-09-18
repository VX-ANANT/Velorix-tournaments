import java.util.Base64
import java.io.File

tasks.register("decodeKeystore") {
    doLast {
        val base64 = File("debug.keystore.base64").readText().trim()
        val decoded = Base64.getDecoder().decode(base64)
        File("debug.keystore").writeBytes(decoded)
        println("Decoded successfully!")
    }
}
