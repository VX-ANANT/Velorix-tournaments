import re

with open("app/src/main/java/com/example/data/model/Models.kt", "r") as f:
    text = f.read()

new_model = """
@androidx.room.Entity(tableName = "missions")
data class Mission(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val target: Int,
    val progress: Int = 0,
    val rewardCurrency: Double,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)
"""

if "data class Mission(" not in text:
    text += "\n" + new_model
    with open("app/src/main/java/com/example/data/model/Models.kt", "w") as f:
        f.write(text)

