import re
with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "r") as f:
    text = f.read()

target = """    fun loginWithGoogle(context: android.content.Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            try {
                kotlinx.coroutines.withTimeout(300000L) {
                    val webClientId = "27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com"
                    val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    
                    val request = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    
                    val credentialManager = androidx.credentials.CredentialManager.create(context)
                    
                    try {
                        val result = credentialManager.getCredential(request = request, context = context)
                        val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(result.credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        
                        withContext(Dispatchers.IO) {
                            val credential = GoogleAuthProvider.getCredential(idToken, null)
                            auth.signInWithCredential(credential).await()
                        }
                    } catch (e: Exception) {
                        throw Exception("Native Setup Incomplete. Error: ${e.message}")
                    }
                    
                    withContext(Dispatchers.IO) {
                        repository.fetchDataFromServer(force = true)
                    }
                    if (repository.user.firstOrNull() == null) {
                        repository.saveUserProfile("Player_" + (1000..9999).random(), "")
                    }
                }
                val username = repository.user.firstOrNull()?.username ?: "Warrior"
                prefs.edit().putBoolean("is_logged_in", true).apply()
                _isLoggedIn.value = true
                checkLoginStreak()
                _toastMessage.emit("Welcome back, ${username}!")
                onComplete()
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _dbErrorDialog.value = "Google Login Timeout: Please check your internet connection."
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                _toastMessage.emit("Google Login Cancelled")
            } catch (e: Exception) {
                _dbErrorDialog.value = "Google Login Failed: ${e.message}"
            } finally {
                _isAuthLoading.value = false
            }
        }
    }"""

replacement = """    fun loginWithGoogle(context: android.content.Context, onComplete: () -> Unit = {}, onFallback: () -> Unit = {}) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            try {
                kotlinx.coroutines.withTimeout(300000L) {
                    val webClientId = "27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com"
                    val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()
                    
                    val request = androidx.credentials.GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    
                    val credentialManager = androidx.credentials.CredentialManager.create(context)
                    
                    val result = try {
                        credentialManager.getCredential(request = request, context = context)
                    } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                        throw e
                    } catch (e: Exception) {
                        null
                    }
                    
                    if (result == null) {
                        _isAuthLoading.value = false
                        onFallback()
                        return@withTimeout
                    }
                    
                    try {
                        val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(result.credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        
                        withContext(Dispatchers.IO) {
                            val credential = GoogleAuthProvider.getCredential(idToken, null)
                            auth.signInWithCredential(credential).await()
                        }
                    } catch (e: Exception) {
                        _isAuthLoading.value = false
                        onFallback()
                        return@withTimeout
                    }
                    
                    withContext(Dispatchers.IO) {
                        repository.fetchDataFromServer(force = true)
                    }
                    if (repository.user.firstOrNull() == null) {
                        repository.saveUserProfile("Player_" + (1000..9999).random(), "")
                    }
                }
                if (_isAuthLoading.value) { // means fallback was not triggered
                    val username = repository.user.firstOrNull()?.username ?: "Warrior"
                    prefs.edit().putBoolean("is_logged_in", true).apply()
                    _isLoggedIn.value = true
                    checkLoginStreak()
                    _toastMessage.emit("Welcome back, ${username}!")
                    onComplete()
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _dbErrorDialog.value = "Google Login Timeout: Please check your internet connection."
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                // Cancelled
            } catch (e: Exception) {
                _isAuthLoading.value = false
                onFallback()
            } finally {
                if (_isAuthLoading.value) {
                    _isAuthLoading.value = false
                }
            }
        }
    }"""

if target in text:
    text = text.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", "w") as f:
        f.write(text)
    print("Replaced loginWithGoogle")
else:
    print("Not found")
