package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MyraaDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = VoidBlack,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = CyanNeon,
    secondary = VioletNeon,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = VioletNeon,
    tertiary = BlueHolo,
    onTertiary = VoidBlack,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderGlow,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to futuristic dark aesthetic
    dynamicColor: Boolean = false, // Keep signature MYRAA branding
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MyraaDarkColorScheme,
        typography = Typography,
        content = content
    )
}

