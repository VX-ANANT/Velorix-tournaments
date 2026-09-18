import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

target = """    val themeMode: StateFlow<String> = _themeMode.asStateFlow()"""
replacement = """    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean("has_completed_onboarding", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    fun completeOnboarding(theme: String) {
        setThemeMode(theme)
        prefs.edit().putBoolean("has_completed_onboarding", true).apply()
        _hasCompletedOnboarding.value = true
    }"""
text = text.replace(target, replacement)
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)
