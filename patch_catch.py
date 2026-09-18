import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

target = """            } catch (e: Exception) {
                _dbErrorDialog.value = "Google Login Failed: ${e.message}"
            }"""

replacement = """            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                _toastMessage.emit("Google Login Cancelled")
            } catch (e: Exception) {
                _dbErrorDialog.value = "Google Login Failed: ${e.message}"
            }"""

if target in text:
    text = text.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
        f.write(text)
    print("Patched catch block!")
else:
    print("Not found")
