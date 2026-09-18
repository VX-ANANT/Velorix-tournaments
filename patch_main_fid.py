import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

replacement = """                if (!isEmulator) {
                    com.google.firebase.installations.FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            android.util.Log.d("VelorixTest", "Firebase Installation ID (FID): ${task.result}")
                        } else {
                            android.util.Log.w("VelorixTest", "Unable to get Firebase Installation ID", task.exception)
                        }
                    }
                }"""

content = re.sub(
    r"                com\.google\.firebase\.installations\.FirebaseInstallations\.getInstance\(\)\.id\.addOnCompleteListener.*?\}\s*\}",
    replacement,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
