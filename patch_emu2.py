import re

def replace_emu(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    replacement = """val isEmulator = try {
            android.os.Build.FINGERPRINT.contains("generic") || android.os.Build.MODEL.contains("Emulator") || android.os.Build.HARDWARE.contains("goldfish") || android.os.Build.HARDWARE.contains("ranchu")
        } catch (e: Exception) { true }"""
    
    # We want to replace any block defining val isEmulator = try { ... } catch (e: Exception) { true }
    new_content = re.sub(
        r"val isEmulator = try \{.*?\} catch \(e: Exception\) \{ true \}",
        replacement,
        content,
        flags=re.DOTALL
    )
    with open(filepath, 'w') as f:
        f.write(new_content)

replace_emu("app/src/main/java/com/example/MyApplication.kt")
replace_emu("app/src/main/java/com/example/MainActivity.kt")
replace_emu("app/src/main/java/com/example/data/repository/PlatformRepository.kt")

