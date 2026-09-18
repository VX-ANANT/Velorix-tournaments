import re
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

target_auth = """                    composable("auth") {
                        AuthScreen(
                            viewModel = viewModel,
                            onAuthSuccess = {
                                navController.navigate("main") {
                                    popUpTo(0)
                                }
                            }
                        )
                    }"""

replacement_auth = """                    composable("auth") {
                        AuthScreen(
                            viewModel = viewModel,
                            onAuthSuccess = {
                                if (viewModel.hasCompletedOnboarding.value) {
                                    navController.navigate("main") {
                                        popUpTo(0)
                                    }
                                } else {
                                    navController.navigate("onboarding") {
                                        popUpTo(0)
                                    }
                                }
                            }
                        )
                    }
                    composable("onboarding") {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onComplete = {
                                navController.navigate("main") {
                                    popUpTo(0)
                                }
                            }
                        )
                    }"""

text = text.replace(target_auth, replacement_auth)
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)
