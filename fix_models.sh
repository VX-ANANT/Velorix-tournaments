sed -i 's/import kotlinx.serialization.json.JsonNames/import kotlinx.serialization.json.JsonNames\nimport com.google.firebase.firestore.IgnoreExtraProperties/g' app/src/main/java/com/example/data/model/Models.kt
sed -i 's/data class User(/@IgnoreExtraProperties\ndata class User(/g' app/src/main/java/com/example/data/model/Models.kt
sed -i 's/data class Tournament(/@IgnoreExtraProperties\ndata class Tournament(/g' app/src/main/java/com/example/data/model/Models.kt
sed -i 's/data class Transaction(/@IgnoreExtraProperties\ndata class Transaction(/g' app/src/main/java/com/example/data/model/Models.kt
