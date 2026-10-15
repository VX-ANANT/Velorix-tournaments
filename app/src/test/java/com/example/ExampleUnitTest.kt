
package com.example

import android.graphics.Path
import android.graphics.PathMeasure
import org.junit.Test
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Person
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ExampleUnitTest {
    @Test
    fun testShadersCompile() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            try {
                val s1 = android.graphics.RuntimeShader(com.example.ui.components.backdrop.internal.RoundedRectRefractionShaderString)
                println("s1 compiled: $s1")
            } catch (e: Throwable) {
                println("s1 error: ${e.message}")
                e.printStackTrace()
                throw e
            }
            try {
                val s2 = android.graphics.RuntimeShader(com.example.ui.components.backdrop.internal.RoundedRectRefractionWithDispersionShaderString)
                println("s2 compiled: $s2")
            } catch (e: Throwable) {
                println("s2 error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    @get:org.junit.Rule
    val composeTestRule = androidx.compose.ui.test.junit4.createComposeRule()

    @Test
    fun testFloatingBottomBarWithGlassRender() {
        composeTestRule.setContent {
            val appBackdrop = com.example.ui.components.backdrop.backdrops.rememberLayerBackdrop()
            androidx.compose.runtime.CompositionLocalProvider(
                com.example.ui.components.LocalLiquidGlassEnabled provides true,
                com.example.ui.components.LocalAppBackdrop provides appBackdrop,
            ) {
                val tabs = listOf(
                    com.example.ui.components.BottomTab("home", "Home", androidx.compose.material.icons.Icons.Default.Home),
                    com.example.ui.components.BottomTab("matches", "Matches", androidx.compose.material.icons.Icons.Default.Star),
                    com.example.ui.components.BottomTab("leaderboard", "Ranks", androidx.compose.material.icons.Icons.Default.Place),
                    com.example.ui.components.BottomTab("wallet", "Wallet", androidx.compose.material.icons.Icons.Default.ShoppingCart),
                )
                val profile = com.example.ui.components.BottomTab("profile", "Profile", androidx.compose.material.icons.Icons.Default.Person)

                com.example.ui.components.FloatingBottomBar(
                    tabs = tabs,
                    currentRoute = "home",
                    onTabSelected = {},
                    profileTab = profile
                )
            }
        }
        composeTestRule.waitForIdle()
    }
}
