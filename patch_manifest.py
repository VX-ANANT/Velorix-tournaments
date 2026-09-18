import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

replacement = """        <meta-data android:name="firebase_messaging_auto_init_enabled" android:value="false" />
        <meta-data android:name="firebase_analytics_collection_enabled" android:value="false" />
        <activity"""

content = content.replace("<activity", replacement, 1)

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
