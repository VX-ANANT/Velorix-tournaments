import re

with open("app/src/main/java/com/example/data/model/Models.kt", "r") as f:
    text = f.read()

text = text.replace('    @JsonNames("totalWins", "total_wins") val totalWins: Int = 0\n)', '    @JsonNames("totalWins", "total_wins") val totalWins: Int = 0,\n    @JsonNames("fcmToken", "fcm_token") val fcmToken: String = ""\n)')

with open("app/src/main/java/com/example/data/model/Models.kt", "w") as f:
    f.write(text)

