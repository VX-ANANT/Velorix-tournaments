import re

with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "r") as f:
    content = f.read()

replacement = """        val isEmulator = try {
            val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
            com.google.android.gms.common.GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) { true }"""

content = re.sub(
    r"        val isEmulator = try \{\s*com\.google\.android\.gms\.common\.GoogleApiAvailability\.getInstance\(\)\.isGooglePlayServicesAvailable\(null\) \!\= com\.google\.android\.gms\.common\.ConnectionResult\.SUCCESS\s*\} catch \(e: Exception\) \{ true \}",
    replacement,
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "w") as f:
    f.write(content)
