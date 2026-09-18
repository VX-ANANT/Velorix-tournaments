import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

replacement = """        <!--
        <service
            android:name=".service.MyFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
        -->"""

content = re.sub(
    r"\s*<service\s*android:name=\"\.service\.MyFirebaseMessagingService\".*?</service>",
    replacement,
    content,
    flags=re.DOTALL
)

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
