import os

def add_file_header(path, header):
    with open(path, "r") as f:
        content = f.read()
    if not content.startswith("/**"):
        with open(path, "w") as f:
            f.write(header + "\n" + content)

models_header = """/**
 * Models.kt
 * 
 * Defines the core data structures (Entities) for the application.
 * These classes represent the local SQLite database tables using Room annotations.
 * 
 * Responsibilities:
 * - Define User, Tournament, Match, Participant, and Transaction models.
 * - Provide JSON serialization support via kotlinx.serialization.
 */"""

db_header = """/**
 * AppDatabase.kt
 * 
 * Room Database configuration and initialization.
 * 
 * Responsibilities:
 * - Define the database schema and version.
 * - Provide DAO (Data Access Object) instances for database operations.
 * - Manage a singleton instance of the Room database to prevent memory leaks.
 */"""

repo_header = """/**
 * PlatformRepository.kt
 * 
 * The Single Source of Truth for the application's data.
 * 
 * Responsibilities:
 * - Mediate between local SQLite (Room) and remote Firebase (Firestore/Auth).
 * - Handle data fetching, caching, and synchronization.
 * - Provide Flow streams to the ViewModel for reactive UI updates.
 */"""

vm_header = """/**
 * PlatformViewModel.kt
 * 
 * The central UI state manager, following the MVI/MVVM pattern.
 * 
 * Responsibilities:
 * - Hold UI state (e.g., user details, tournaments, loading states).
 * - Handle user intents (e.g., login, register, join tournament).
 * - Communicate with the PlatformRepository to fetch/update data.
 * - Expose state to Compose UI using StateFlow.
 */"""

service_header = """/**
 * MyFirebaseMessagingService.kt
 * 
 * Background service to handle Firebase Cloud Messaging (FCM).
 * 
 * Responsibilities:
 * - Receive push notifications from the backend (admin panel).
 * - Display system tray notifications for tournament reminders and match results.
 * - Update the FCM registration token when it changes.
 */"""

add_file_header("app/src/main/java/com/example/data/model/Models.kt", models_header)
add_file_header("app/src/main/java/com/example/data/db/AppDatabase.kt", db_header)
add_file_header("app/src/main/java/com/example/data/repository/PlatformRepository.kt", repo_header)
add_file_header("app/src/main/java/com/example/ui/viewmodel/PlatformViewModel.kt", vm_header)
add_file_header("app/src/main/java/com/example/service/MyFirebaseMessagingService.kt", service_header)

print("Headers added successfully.")
