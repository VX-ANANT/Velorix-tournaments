package com.example.data.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import com.example.data.model.SystemAppConfig
import com.example.data.model.User

enum class FeatureCategory(val label: String) {
    FINANCIAL("Wallet & Economy"),
    GAMEPLAY("Tournaments & Matches"),
    SOCIAL("Community & Comms"),
    ACCOUNT("Operative & Security"),
    CUSTOM("Custom Modules")
}

data class AppFeature(
    val id: String,
    val title: String,
    val description: String,
    val category: FeatureCategory = FeatureCategory.CUSTOM,
    val defaultEnabled: Boolean = true,
    val adminBypassAllowed: Boolean = true
)

/**
 * Dynamic Feature Registry for VeloRix Platform.
 *
 * HOW TO ADD A NEW FEATURE IN THE FUTURE:
 * Simply call `FeatureRegistry.register(AppFeature("my_feature_id", "Feature Name", "Description..."))`
 * from anywhere in your codebase!
 *
 * It will AUTOMATICALLY:
 * 1. Appear in the Admin Situation Controls panel with a real-time Toggle Switch.
 * 2. Sync state instantly across Firebase Realtime Database and Firestore.
 * 3. Grant automatic Admin testing bypass (admins can use it even when disabled for public).
 * 4. Protect UI using `FeatureRegistry.isFeatureEnabled("my_feature_id", systemConfig, user)`.
 */
object FeatureRegistry {
    private val _registeredFeatures = mutableStateListOf<AppFeature>()
    val registeredFeatures: List<AppFeature> get() = _registeredFeatures

    init {
        // Register core and modular platform features
        register(
            AppFeature(
                id = "token_conversion",
                title = "Token to VT Conversion",
                description = "Converts earned activity tokens into VT balance (Rate: 10 Tokens = 1 VT).",
                category = FeatureCategory.FINANCIAL,
                defaultEnabled = true
            )
        )
        register(
            AppFeature(
                id = "tournament_join",
                title = "Tournament Registrations",
                description = "Enables operatives to register and pay entry fees for esports tournaments.",
                category = FeatureCategory.GAMEPLAY,
                defaultEnabled = true
            )
        )
        register(
            AppFeature(
                id = "room_credentials_delivery",
                title = "Live Room ID & Password Delivery",
                description = "Displays confidential Custom Room credentials 15 mins prior to match start.",
                category = FeatureCategory.GAMEPLAY,
                defaultEnabled = true
            )
        )
        register(
            AppFeature(
                id = "instant_withdrawals",
                title = "Instant UPI/Bank Withdrawals",
                description = "Permits users to request real money wallet withdrawals.",
                category = FeatureCategory.FINANCIAL,
                defaultEnabled = true
            )
        )
        register(
            AppFeature(
                id = "daily_login_rewards",
                title = "Daily Login Streak System",
                description = "Grants consecutive login rewards and streak tracking.",
                category = FeatureCategory.FINANCIAL,
                defaultEnabled = true
            )
        )
        register(
            AppFeature(
                id = "founder_pass_hub",
                title = "Founder Pass Registration",
                description = "Allows operatives to purchase exclusive lifetime Founder Supporter passes.",
                category = FeatureCategory.FINANCIAL,
                defaultEnabled = true
            )
        )
        register(
            AppFeature(
                id = "community_chat",
                title = "Community Global Chat",
                description = "Real-time operative public chatroom and matchmaking discussion.",
                category = FeatureCategory.SOCIAL,
                defaultEnabled = true
            )
        )
        register(
            AppFeature(
                id = "ai_sentinel_support",
                title = "AI Support Assistant",
                description = "Automated 24/7 operative issue resolution and guide answers.",
                category = FeatureCategory.ACCOUNT,
                defaultEnabled = true
            )
        )
    }

    /**
     * Registers a new feature into the dynamic registry.
     */
    fun register(feature: AppFeature) {
        if (_registeredFeatures.none { it.id == feature.id }) {
            _registeredFeatures.add(feature)
        }
    }

    /**
     * Checks if a given feature is enabled based on server-side config and user role.
     */
    fun isFeatureEnabled(
        featureId: String,
        systemConfig: SystemAppConfig,
        user: User? = null
    ): Boolean {
        val isAdmin = user?.role?.contains("admin", ignoreCase = true) == true ||
                user?.phoneOrEmail?.equals("service.veloxyra@gmail.com", ignoreCase = true) == true ||
                user?.phoneOrEmail?.equals("anantisback47@gmail.com", ignoreCase = true) == true

        val feature = _registeredFeatures.find { it.id == featureId }
        val isExplicitlySet = systemConfig.featureFlags.containsKey(featureId)
        val isEnabled = if (isExplicitlySet) {
            systemConfig.featureFlags[featureId] == true
        } else {
            feature?.defaultEnabled ?: true
        }

        // If user is Admin, they can always bypass disabled features for testing
        if (isAdmin && (feature?.adminBypassAllowed != false)) {
            return true
        }

        return isEnabled
    }
}
