sed -i '/Firebase.initialize(context = this)/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/Firebase.appCheck.installAppCheckProviderFactory/d' app/src/main/java/com/example/MainActivity.kt
sed -i '/DebugAppCheckProviderFactory.getInstance(),/d' app/src/main/java/com/example/MainActivity.kt
sed -i 's/super.onCreate()/super.onCreate()\n        try { com.google.firebase.FirebaseApp.initializeApp(this) } catch (e: Exception) { android.util.Log.e("Firebase", "Failed to init", e) }/g' app/src/main/java/com/example/MyApplication.kt
