package dev.asrithtanniru.dotwall.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object DotsColors {
    val Background = Color(0xFF0D0D0C)
    val Surface = Color(0xFF1A1A19)
    val Text = Color(0xFFECEAE4)
    val Muted = Color(0xFF7C7A74)
}

@Composable
fun DotsTheme(accent: Color = Color(0xFFD97757), content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = DotsColors.Background,
        background = DotsColors.Background,
        onBackground = DotsColors.Text,
        surface = DotsColors.Surface,
        onSurface = DotsColors.Text,
        surfaceVariant = DotsColors.Surface,
        onSurfaceVariant = DotsColors.Muted,
        secondaryContainer = DotsColors.Surface,
        onSecondaryContainer = DotsColors.Text,
        outline = DotsColors.Muted,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
