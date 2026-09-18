import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

func = """    fun clearDbError() {
        _dbErrorDialog.value = null
    }

    fun showError(msg: String) {
        _dbErrorDialog.value = msg
    }
"""

text = text.replace("""    fun clearDbError() {
        _dbErrorDialog.value = null
    }""", func)

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)
print("ViewModel Patched!")
