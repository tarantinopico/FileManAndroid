package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UiDensity

data class AppDimens(
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp,
    val iconSize: Dp,
    val listItemHeight: Dp
)

val LocalAppDimens = staticCompositionLocalOf {
    AppDimens(4.dp, 8.dp, 16.dp, 24.dp, 56.dp)
}

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    surfaceContainer = md_theme_light_surface,
    surfaceContainerHigh = md_theme_light_surface,
    surfaceContainerHighest = md_theme_light_surface,
    surfaceContainerLow = md_theme_light_background,
    surfaceContainerLowest = md_theme_light_background
)

private val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    surfaceContainer = md_theme_dark_surface,
    surfaceContainerHigh = md_theme_dark_surface,
    surfaceContainerHighest = md_theme_dark_surface,
    surfaceContainerLow = md_theme_dark_background,
    surfaceContainerLowest = md_theme_dark_background
)

@Composable
fun FileManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    density: UiDensity = UiDensity.NORMAL,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColors
        else -> LightColors
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    
    val appDimens = when (density) {
        UiDensity.COMPACT -> AppDimens(paddingSmall = 2.dp, paddingMedium = 4.dp, paddingLarge = 8.dp, iconSize = 20.dp, listItemHeight = 40.dp)
        UiDensity.NORMAL -> AppDimens(paddingSmall = 4.dp, paddingMedium = 8.dp, paddingLarge = 16.dp, iconSize = 24.dp, listItemHeight = 56.dp)
        UiDensity.LARGE -> AppDimens(paddingSmall = 6.dp, paddingMedium = 12.dp, paddingLarge = 24.dp, iconSize = 32.dp, listItemHeight = 64.dp)
        UiDensity.EXTRA_LARGE -> AppDimens(paddingSmall = 8.dp, paddingMedium = 16.dp, paddingLarge = 32.dp, iconSize = 40.dp, listItemHeight = 72.dp)
    }

    val baseTypography = androidx.compose.material3.Typography()
    val textScale = when (density) {
        UiDensity.COMPACT -> 0.85f
        UiDensity.NORMAL -> 1.0f
        UiDensity.LARGE -> 1.25f
        UiDensity.EXTRA_LARGE -> 1.5f
    }
    val typography = androidx.compose.material3.Typography(
        displayLarge = baseTypography.displayLarge.copy(fontSize = baseTypography.displayLarge.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, letterSpacing = (-0.5).dp.value.sp),
        displayMedium = baseTypography.displayMedium.copy(fontSize = baseTypography.displayMedium.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, letterSpacing = (-0.5).dp.value.sp),
        displaySmall = baseTypography.displaySmall.copy(fontSize = baseTypography.displaySmall.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, letterSpacing = (-0.25).dp.value.sp),
        headlineLarge = baseTypography.headlineLarge.copy(fontSize = baseTypography.headlineLarge.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        headlineMedium = baseTypography.headlineMedium.copy(fontSize = baseTypography.headlineMedium.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        headlineSmall = baseTypography.headlineSmall.copy(fontSize = baseTypography.headlineSmall.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        titleLarge = baseTypography.titleLarge.copy(fontSize = baseTypography.titleLarge.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
        titleMedium = baseTypography.titleMedium.copy(fontSize = baseTypography.titleMedium.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
        titleSmall = baseTypography.titleSmall.copy(fontSize = baseTypography.titleSmall.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
        bodyLarge = baseTypography.bodyLarge.copy(fontSize = baseTypography.bodyLarge.fontSize * textScale),
        bodyMedium = baseTypography.bodyMedium.copy(fontSize = baseTypography.bodyMedium.fontSize * textScale),
        bodySmall = baseTypography.bodySmall.copy(fontSize = baseTypography.bodySmall.fontSize * textScale),
        labelLarge = baseTypography.labelLarge.copy(fontSize = baseTypography.labelLarge.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
        labelMedium = baseTypography.labelMedium.copy(fontSize = baseTypography.labelMedium.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
        labelSmall = baseTypography.labelSmall.copy(fontSize = baseTypography.labelSmall.fontSize * textScale, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    )

    val shapes = androidx.compose.material3.Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    )

    CompositionLocalProvider(LocalAppDimens provides appDimens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
