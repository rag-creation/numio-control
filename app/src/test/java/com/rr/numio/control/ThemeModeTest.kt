package com.rr.numio.control

import com.rr.numio.control.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun followsSystemOnlyInSystemMode() {
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
    }

    @Test
    fun unknownSavedValueFallsBack() {
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromKey("purple"))
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromKey(null))
    }
}
