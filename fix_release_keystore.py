import re

with open("app/build.gradle.kts", "r") as f:
    text = f.read()

target = """    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }"""
replacement = """    create("release") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }"""
text = text.replace(target, replacement)

with open("app/build.gradle.kts", "w") as f:
    f.write(text)

