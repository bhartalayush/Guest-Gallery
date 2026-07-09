package com.example.guestgallery.theme

import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf

data class AdaptiveUiConfig(
    val androidVersion: Int = Build.VERSION.SDK_INT,
    val manufacturer: String = Build.MANUFACTURER,
    val isSystemDark: Boolean = false,
    val fontScale: Float = 1.0f,
    val density: Float = 1.0f,
    val screenWidthDp: Int = 360,
    val screenHeightDp: Int = 640,
    val isGestureNavigation: Boolean = true,
    val cornerRadiusDp: Int = 16,
    val animationScale: Float = 1.0f,
    
    // Custom UI Customization Overrides
    val gridColumns: Int = 3,
    val gridSpacingDp: Float = 1.5f,
    val photoCornerRadiusDp: Int = 0,
    val albumCornerRadiusDp: Int = 16,
    val themeBrand: String = "samsung",
    val uiOverrideEnabled: Boolean = false
)

val LocalAdaptiveUiConfig = staticCompositionLocalOf<AdaptiveUiConfig> {
    error("No AdaptiveUiConfig provided")
}
