package com.example.guestgallery.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.RoundedCorner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.guestgallery.data.GalleryRepository

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8), // Google Photos light blue in dark mode
    secondary = Color(0xFFADC6FF),
    tertiary = Color(0xFFF28B82),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A73E8), // Google Photos blue in light theme
    secondary = Color(0xFF4285F4),
    tertiary = Color(0xFFEA4335),
    background = Color.White,
    surface = Color(0xFFF8F9FA),
    onBackground = Color(0xFF202124),
    onSurface = Color(0xFF202124)
)

// Helper to find Activity context
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Fallback corner radius based on manufacturer aesthetics
private fun getDefaultCornerRadius(manufacturer: String): Int {
    val cleanName = manufacturer.lowercase()
    return when {
        cleanName.contains("google") || cleanName.contains("pixel") -> 28
        cleanName.contains("samsung") -> 16
        cleanName.contains("oneplus") || cleanName.contains("oppo") -> 20
        cleanName.contains("nothing") -> 24
        cleanName.contains("xiaomi") -> 18
        else -> 16
    }
}

@Composable
fun GuestGalleryTheme(
  repository: GalleryRepository,
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val config = LocalConfiguration.current

  // Collect settings-configured UI overrides
  val uiOverrideEnabled by repository.uiOverrideEnabled.collectAsState()
  val uiOverrideColumns by repository.uiOverrideColumns.collectAsState()
  val uiOverrideSpacingDp by repository.uiOverrideSpacingDp.collectAsState()
  val uiOverridePhotoCornersDp by repository.uiOverridePhotoCornersDp.collectAsState()
  val uiOverrideAlbumCornersDp by repository.uiOverrideAlbumCornersDp.collectAsState()
  val uiOverrideThemeBrand by repository.uiOverrideThemeBrand.collectAsState()
  
  // 1. Detect navigation style (Gesture vs 3-Button) based on navigationBar bottom height
  val navigationBars = WindowInsets.navigationBars
  val navBarHeightDp = with(density) { navigationBars.getBottom(this).toDp() }
  val isGesture = navBarHeightDp < 32.dp

  // 2. Query physical corner radius (Android 12+) or fallback to heuristics
  val activity = remember(context) { context.findActivity() }
  val cornerRadiusDp = remember(density, activity) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && activity != null) {
          try {
              val insets = activity.window.decorView.rootWindowInsets
              val corner = insets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
              val radiusPx = corner?.radius ?: 0
              if (radiusPx > 0) {
                  (radiusPx / density.density).toInt()
              } else {
                  getDefaultCornerRadius(Build.MANUFACTURER)
              }
          } catch (e: Exception) {
              getDefaultCornerRadius(Build.MANUFACTURER)
          }
      } else {
          getDefaultCornerRadius(Build.MANUFACTURER)
      }
  }

  // 3. Query developer settings animation duration scale
  val animationScale = remember(context) {
      try {
          android.provider.Settings.Global.getFloat(
              context.contentResolver,
              android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
              1.0f
          )
      } catch (e: Exception) {
          1.0f
      }
  }

  val adaptiveConfig = remember(
      darkTheme, density, config, isGesture, cornerRadiusDp, animationScale,
      uiOverrideEnabled, uiOverrideColumns, uiOverrideSpacingDp,
      uiOverridePhotoCornersDp, uiOverrideAlbumCornersDp, uiOverrideThemeBrand
  ) {
      AdaptiveUiConfig(
          androidVersion = Build.VERSION.SDK_INT,
          manufacturer = if (uiOverrideEnabled) uiOverrideThemeBrand else Build.MANUFACTURER,
          isSystemDark = darkTheme,
          fontScale = density.fontScale,
          density = density.density,
          screenWidthDp = config.screenWidthDp,
          screenHeightDp = config.screenHeightDp,
          isGestureNavigation = isGesture,
          cornerRadiusDp = if (uiOverrideEnabled) uiOverrideAlbumCornersDp else cornerRadiusDp,
          animationScale = animationScale,
          gridColumns = if (uiOverrideEnabled) uiOverrideColumns else 3,
          gridSpacingDp = if (uiOverrideEnabled) uiOverrideSpacingDp else 1.5f,
          photoCornerRadiusDp = if (uiOverrideEnabled) uiOverridePhotoCornersDp else 0,
          albumCornerRadiusDp = if (uiOverrideEnabled) uiOverrideAlbumCornersDp else cornerRadiusDp,
          themeBrand = "google",
          uiOverrideEnabled = uiOverrideEnabled
      )
  }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  CompositionLocalProvider(LocalAdaptiveUiConfig provides adaptiveConfig) {
      MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
