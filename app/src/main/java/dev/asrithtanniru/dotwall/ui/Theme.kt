package dev.asrithtanniru.dotwall.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.asrithtanniru.dotwall.R

/** Google Sans Flex, the Pixel look. Same family the wallpaper text uses. */
private val GoogleSans = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_semibold, FontWeight.Bold),
)

private fun Typography.withFamily(family: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

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
    MaterialTheme(colorScheme = scheme, typography = Typography().withFamily(GoogleSans), content = content)
}
