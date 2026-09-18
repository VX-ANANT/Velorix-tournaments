sed -i 's/get(com.google.firebase.firestore.Source.SERVER)/get()/g' app/src/main/java/com/example/data/repository/PlatformRepository.kt
sed -i 's/if (attempt >= 10) throw e/if (attempt >= 3) break/g' app/src/main/java/com/example/data/repository/PlatformRepository.kt
sed -i 's/} catch (e: Exception) { Log.e("PlatformRepository", "Failed to fetch user", e); throw e }/} catch (e: Exception) { Log.e("PlatformRepository", "Failed to fetch user", e) }/g' app/src/main/java/com/example/data/repository/PlatformRepository.kt
sed -i 's/throw e/return false/g' app/src/main/java/com/example/data/repository/PlatformRepository.kt
