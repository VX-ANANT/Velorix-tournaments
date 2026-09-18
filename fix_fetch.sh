cat app/src/main/java/com/example/data/repository/PlatformRepository.kt | awk '
BEGIN { in_fetch_user = 0 }
/try \{/ {
    if ($0 ~ /try \{/ && getline next_line) {
        if (next_line ~ /val userSnapshot = firestore.collection\("users"\).document\(uid\).get\(\).await\(\)/) {
            print "                    try {"
            print "                        var userSnapshot: com.google.firebase.firestore.DocumentSnapshot? = null"
            print "                        var attempt = 0"
            print "                        while (attempt < 3 && userSnapshot == null) {"
            print "                            try {"
            print "                                userSnapshot = firestore.collection(\"users\").document(uid).get().await()"
            print "                            } catch (e: Exception) {"
            print "                                attempt++"
            print "                                if (attempt >= 3) throw e"
            print "                                kotlinx.coroutines.delay(1000)"
            print "                            }"
            print "                        }"
            print "                        val fetchedUser = userSnapshot?.toObject(User::class.java)"
            getline next_line2
            print next_line2
            in_fetch_user = 1
            next
        } else {
            print $0
            print next_line
            next
        }
    }
}
{ print }
' > tmp.kt && mv tmp.kt app/src/main/java/com/example/data/repository/PlatformRepository.kt
