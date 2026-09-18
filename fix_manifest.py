import re
with open("app/src/main/AndroidManifest.xml", "r") as f:
    text = f.read()

target = """        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.MyApplication">"""

replacement = """        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
            android:windowSoftInputMode="adjustResize"
            android:theme="@style/Theme.MyApplication">"""

text = text.replace(target, replacement)
with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(text)
