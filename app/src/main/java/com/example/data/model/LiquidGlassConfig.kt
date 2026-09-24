package com.example.data.model

import androidx.compose.runtime.Immutable

/**
 * LiquidGlassConfig
 *
 * Real-time runtime configuration for Apple Liquid Glass material,
 * lens refraction, specular lighting, opacity, blur, and dock styling.
 */
@Immutable
data class LiquidGlassConfig(
    val floatingNavBar: Boolean = true,
    val enableLiquidGlass: Boolean = true,
    val vibrancy: Float = 1.0f,               // 0.0f to 2.0f
    val blurRadius: Float = 14f,              // 0f to 30f dp (soft, non-cloudy blur)
    val lensRefractionHeight: Float = 0.55f,  // 0.1f to 1.0f (upper curvature height)
    val lensRefractionAmount: Float = 0.16f,  // 0.0f to 0.45f (subtle specular highlights, non-harsh white)
    val chromaticAberration: Boolean = true,
    val depthEffect: Boolean = true,
    val surfaceTint: String = "obsidian",      // "obsidian", "crimson", "midnight", "clear"
    val surfaceOpacity: Float = 0.28f,        // 0.10f to 0.80f (high transparency, clear backdrop visibility)
    val glassTextColor: String = "white",     // "white", "adaptive", "platinum"
    val glassPlayer: Boolean = true,
    val glassMiniPlayer: Boolean = true,
    val glassNavBar: Boolean = true,
    val glassCards: Boolean = true
)
