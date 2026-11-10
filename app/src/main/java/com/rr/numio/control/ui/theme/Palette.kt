package com.rr.numio.control.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Official Numio palette (getnumio.org). Never swap Numio amber for a generic mustard.
// Only the background, text and accent values below are brand colours; the in-between
// greys are tints of them for cards, buttons, lines and secondary text.
data class ControlPalette(
    val background: Color,   // app screens
    val scrim: Color,        // behind the panel, over other apps
    val card: Color,         // panel cards and setting rows
    val button: Color,       // a round button that is off
    val divider: Color,
    val track: Color,        // empty part of a slider
    val text: Color,
    val textSoft: Color,     // secondary text
    val accent: Color,       // amber fills: buttons that are on, slider fill
    val accentInk: Color,    // amber used as text or a thin line
    val onAccent: Color,     // icons and text on an amber fill
    val thumb: Color,        // slider knob, warm cream
    val isLight: Boolean,
)

object Palettes {
    val Dark = ControlPalette(
        background = Color(0xFF0F0F0E),
        scrim = Color(0x8C0F0F0E),
        card = Color(0xF01B1A18),
        button = Color(0xFF2C2A26),
        divider = Color(0xFF34312C),
        track = Color(0xFF3A3731),
        text = Color(0xFFF1EEE6),
        textSoft = Color(0xFFB3AD9F),
        accent = Color(0xFFFFC21A),
        accentInk = Color(0xFFFFC21A),
        onAccent = Color(0xFF1A1400),
        thumb = Color(0xFFF6E7C8),
        isLight = false,
    )

    val Light = ControlPalette(
        background = Color(0xFFF6F4EE),
        scrim = Color(0x66191815),
        card = Color(0xF5FBFAF6),
        button = Color(0xFFE8E4DA),
        divider = Color(0xFFDEDAD0),
        track = Color(0xFFDDD8CC),
        text = Color(0xFF191815),
        textSoft = Color(0xFF5F5A4E),
        accent = Color(0xFFE9A800),
        accentInk = Color(0xFF7A5600),
        onAccent = Color(0xFF1A1400),
        thumb = Color(0xFFFFFFFF),
        isLight = true,
    )
}

val LocalControlPalette = staticCompositionLocalOf { Palettes.Dark }
