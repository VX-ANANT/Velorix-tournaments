cat app/src/main/java/com/example/data/repository/PlatformRepository.kt | awk '
BEGIN { }
/try \{/ {
    if ($0 ~ /try \{/ && getline next_line) {
        if (next_line ~ /val txSnapshot = firestore.collection\("transactions"\).whereEqualTo\("userId", uid\).get\(\).await\(\)/) {
            print "                    try {"
            print "                        var txSnapshot: com.google.firebase.firestore.QuerySnapshot? = null"
            print "                        var attempt = 0"
            print "                        while (attempt < 3 && txSnapshot == null) {"
            print "                            try {"
            print "                                txSnapshot = firestore.collection(\"transactions\").whereEqualTo(\"userId\", uid).get().await()"
            print "                            } catch (e: Exception) {"
            print "                                attempt++"
            print "                                if (attempt >= 3) throw e"
            print "                                kotlinx.coroutines.delay(1000)"
            print "                            }"
            print "                        }"
            print "                        val fetchedTxs = txSnapshot?.toObjects(Transaction::class.java) ?: emptyList()"
            getline next_line2
            print next_line2
            next
        } else if (next_line ~ /val matchStatsSnapshot = firestore.collection\("match_stats"\).whereEqualTo\("userId", uid\).get\(\).await\(\)/) {
            print "                    try {"
            print "                        var matchStatsSnapshot: com.google.firebase.firestore.QuerySnapshot? = null"
            print "                        var attempt = 0"
            print "                        while (attempt < 3 && matchStatsSnapshot == null) {"
            print "                            try {"
            print "                                matchStatsSnapshot = firestore.collection(\"match_stats\").whereEqualTo(\"userId\", uid).get().await()"
            print "                            } catch (e: Exception) {"
            print "                                attempt++"
            print "                                if (attempt >= 3) throw e"
            print "                                kotlinx.coroutines.delay(1000)"
            print "                            }"
            print "                        }"
            print "                        val fetchedMatchStats = matchStatsSnapshot?.toObjects(com.example.data.model.MatchStat::class.java) ?: emptyList()"
            getline next_line2
            print next_line2
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
