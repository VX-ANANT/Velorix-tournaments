import re
with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "r") as f:
    text = f.read()

def insert_after(text, func_name):
    match = re.search(r"fun " + func_name + r".*?\{", text, re.DOTALL)
    if match:
        idx = match.end()
        return text[:idx] + "\n    val isAuthLoading by viewModel.isAuthLoading.collectAsState(initial = false)\n" + text[idx:]
    return text

text = insert_after(text, "LoginScreen")
text = insert_after(text, "RegistrationScreen")

with open("app/src/main/java/com/example/ui/screens/AuthScreen.kt", "w") as f:
    f.write(text)
print("AuthScreen Fixed isAuthLoading!")
