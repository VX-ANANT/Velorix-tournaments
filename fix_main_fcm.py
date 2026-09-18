import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    text = f.read()

text = text.replace('val token = task.result\n                    android.util.Log.d("FCM_TOKEN", "FCM Registration Token: $token")', 'val token = task.result\n                    android.util.Log.d("FCM_TOKEN", "FCM Registration Token: $token")\n                    viewModel.updateFcmToken(token)')

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(text)

