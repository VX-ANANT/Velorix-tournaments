import os
import re

def replace_isemulator(filepath, context_expr="this"):
    with open(filepath, 'r') as f:
        content = f.read()

    replacement = f"""val isEmulator = try {{
            com.google.android.gms.common.GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable({context_expr}) != com.google.android.gms.common.ConnectionResult.SUCCESS
        }} catch (e: Exception) {{ true }}"""

    pattern = r'val isEmulator = android\.os\.Build\.FINGERPRINT\.contains\("generic"\) \|\| android\.os\.Build\.MODEL\.contains\("Emulator"\) \|\| android\.os\.Build\.HARDWARE\.contains\("goldfish"\) \|\| android\.os\.Build\.HARDWARE\.contains\("ranchu"\)'

    if re.search(pattern, content):
        print(f"Patching {filepath}")
        new_content = re.sub(pattern, replacement, content)
        with open(filepath, 'w') as f:
            f.write(new_content)

replace_isemulator("app/src/main/java/com/example/MyApplication.kt", "this")
replace_isemulator("app/src/main/java/com/example/MainActivity.kt", "this@MainActivity")
replace_isemulator("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "null") # Wait, context is needed for isGooglePlayServicesAvailable
replace_isemulator("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "getApplication()")
