package com.rr.numio.control.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.rr.numio.control.R

// Inter (SIL OFL 1.1, see licenses/Inter-OFL.txt). Malayalam, Arabic and emoji
// fall back to the phone's system fonts.
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val base = Typography()

val ControlTypography = Typography(
    headlineMedium = base.headlineMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Inter),
    bodyMedium = base.bodyMedium.copy(fontFamily = Inter),
    bodySmall = base.bodySmall.copy(fontFamily = Inter),
    labelLarge = base.labelLarge.copy(fontFamily = Inter, fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.copy(fontFamily = Inter),
    labelSmall = base.labelSmall.copy(fontFamily = Inter),
)

@Composable
fun ControlTheme(dark: Boolean, content: @Composable () -> Unit) {
    val palette = if (dark) Palettes.Dark else Palettes.Light
    val start = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = start.copy(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        background = palette.background,
        onBackground = palette.text,
        surface = palette.background,
        onSurface = palette.text,
        surfaceVariant = palette.card,
        onSurfaceVariant = palette.textSoft,
        outline = palette.divider,
    )
    CompositionLocalProvider(LocalControlPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = ControlTypography, content = content)
    }
}

/** Short way for screens to read the active palette: `Numio.colors.accent`. */
object Numio {
    val colors: ControlPalette
        @Composable @ReadOnlyComposable get() = LocalControlPalette.current
}
