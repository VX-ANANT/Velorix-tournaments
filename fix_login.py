import re
with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "r") as f:
    text = f.read()

# Make fetchDataFromServer return Boolean
text = text.replace("suspend fun fetchDataFromServer(force: Boolean = false) {", "suspend fun fetchDataFromServer(force: Boolean = false): Boolean {")
text = text.replace('Log.d("PlatformRepository", "Fetch debounced (using cached data).")\n            return', 'Log.d("PlatformRepository", "Fetch debounced (using cached data).")\n            return true')
text = text.replace("db.leaderboardDao().insertAll(fetchedLeaderboard)\n                }\n            }\n        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {\n            Log.w(\"PlatformRepository\", \"Firestore fetch timed out.\")\n        } catch (e: Exception) {\n            Log.e(\"PlatformRepository\", \"Failed to fetch real data from Server.\", e)\n            // dbErrorCallback?.invoke(\"Network error: Unable to connect to Firebase backend API.\")\n        }", "db.leaderboardDao().insertAll(fetchedLeaderboard)\n                }\n            }\n            return true\n        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {\n            Log.w(\"PlatformRepository\", \"Firestore fetch timed out.\")\n            return false\n        } catch (e: Exception) {\n            Log.e(\"PlatformRepository\", \"Failed to fetch real data from Server.\", e)\n            return false\n        }")

# Fix insert/update bug in fetchDataFromServer
target_fetch_user = """                    if (fetchedUser != null) {
                        val currentUser = user.firstOrNull()
                        if (currentUser != null) {
                            db.userDao().update(fetchedUser)
                        } else {
                            db.userDao().insert(fetchedUser)
                        }
                    }"""
replacement_fetch_user = """                    if (fetchedUser != null) {
                        db.userDao().clearAll()
                        db.userDao().insert(fetchedUser)
                    }"""
text = text.replace(target_fetch_user, replacement_fetch_user)

# Fix saveUserProfile to clear users before insert if ID mismatch
target_save_user = """                if (currentUser != null) {
                    db.userDao().update(newUser)
                } else {
                    db.userDao().insert(newUser)
                }"""
replacement_save_user = """                db.userDao().clearAll()
                db.userDao().insert(newUser)"""
text = text.replace(target_save_user, replacement_save_user)

with open("app/src/main/java/com/example/data/repository/PlatformRepository.kt", "w") as f:
    f.write(text)

# Now fix PlatformViewModel
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

# For login
target_login = """                        auth.signInWithEmailAndPassword(firebaseIdentifier, passwordHashT).await()
                        repository.fetchDataFromServer(force = true)
                    }
                }
                var userItem = repository.getUserSync()
                if (userItem == null) {
                    repository.saveUserProfile("Player_" + (1000..9999).random(), phoneOrEmail)
                    userItem = repository.getUserSync()
                }"""
replacement_login = """                        auth.signInWithEmailAndPassword(firebaseIdentifier, passwordHashT).await()
                        val success = repository.fetchDataFromServer(force = true)
                        if (!success) throw Exception("Failed to sync data from server")
                    }
                }
                val userItem = repository.getUserSync()
                if (userItem == null) {
                    throw Exception("Account not found. Please register first.")
                }"""
text = text.replace(target_login, replacement_login)

# For loginWithGoogle
target_google = """                    withContext(Dispatchers.IO) {
                        repository.fetchDataFromServer(force = true)
                    }
                    var u = repository.getUserSync()
                    if (u == null) {
                        repository.saveUserProfile("Player_" + (1000..9999).random(), "")
                        u = repository.getUserSync()
                    }"""
replacement_google = """                    withContext(Dispatchers.IO) {
                        val success = repository.fetchDataFromServer(force = true)
                        if (!success) throw Exception("Failed to sync data from server")
                        var u = repository.getUserSync()
                        if (u == null) {
                            repository.saveUserProfile("Player_" + (1000..9999).random(), "")
                        }
                    }"""
text = text.replace(target_google, replacement_google)

# For loginWithGoogleToken
target_google_token = """                    auth.signInWithCredential(credential).await()
                    repository.fetchDataFromServer(force = true)
                }
                var userItem = repository.getUserSync()
                if (userItem == null) {
                    repository.saveUserProfile("Player_" + (1000..9999).random(), "")
                    userItem = repository.getUserSync()
                }"""
replacement_google_token = """                    auth.signInWithCredential(credential).await()
                    val success = repository.fetchDataFromServer(force = true)
                    if (!success) throw Exception("Failed to sync data from server")
                    var u = repository.getUserSync()
                    if (u == null) {
                        repository.saveUserProfile("Player_" + (1000..9999).random(), "")
                    }
                }
                val userItem = repository.getUserSync()"""
text = text.replace(target_google_token, replacement_google_token)

# Fix logout
target_logout = """            prefs.edit().putBoolean("is_logged_in", false).apply()
            _isLoggedIn.value = false"""
replacement_logout = """            withContext(Dispatchers.IO) {
                repository.db.userDao().clearAll()
            }
            prefs.edit().putBoolean("is_logged_in", false).apply()
            _isLoggedIn.value = false"""
text = text.replace(target_logout, replacement_logout)

with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
    f.write(text)
print("Login fixes applied")
