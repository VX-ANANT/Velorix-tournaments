import re
with open("app/build.gradle.kts", "r") as f:
    text = f.read()

target = """    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }"""
replacement = """    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }"""
text = text.replace(target, replacement)

with open("app/build.gradle.kts", "w") as f:
    f.write(text)
