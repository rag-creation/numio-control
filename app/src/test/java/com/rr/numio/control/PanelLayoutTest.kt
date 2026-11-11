package com.rr.numio.control

import com.rr.numio.control.panel.BrightnessCurve
import com.rr.numio.control.panel.PanelLayout
import com.rr.numio.control.panel.Ringer
import com.rr.numio.control.panel.TileId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelLayoutTest {

    @Test
    fun defaultLayoutIsTwoFullRows() {
        assertEquals(10, PanelLayout.DEFAULT.tiles.size)
        assertEquals(0, PanelLayout.DEFAULT.tiles.size % PanelLayout.COLUMNS)
    }

    @Test
    fun layoutSurvivesSaving() {
        val layout = PanelLayout(listOf(TileId.LOCK, TileId.SCREENSHOT, TileId.SOUND))
        assertEquals(layout, PanelLayout.decode(layout.encode()))
    }

    @Test
    fun nothingSavedGivesDefault() {
        assertEquals(PanelLayout.DEFAULT, PanelLayout.decode(null))
        assertEquals(PanelLayout.DEFAULT, PanelLayout.decode(""))
        assertEquals(PanelLayout.DEFAULT, PanelLayout.decode("not_a_tile,also_not"))
    }

    @Test
    fun unknownAndRepeatedTilesAreDropped() {
        val layout = PanelLayout.decode("lock, from_the_future ,lock,nfc")
        assertEquals(listOf(TileId.LOCK, TileId.NFC), layout.tiles)
    }

    @Test
    fun tileKeysAreUnique() {
        assertEquals(TileId.entries.size, TileId.entries.map { it.key }.toSet().size)
    }

    @Test
    fun ringerCycles() {
        assertEquals(Ringer.VIBRATE, Ringer.RING.next(hasVibrator = true))
        assertEquals(Ringer.SILENT, Ringer.VIBRATE.next(hasVibrator = true))
        assertEquals(Ringer.RING, Ringer.SILENT.next(hasVibrator = true))
    }

    @Test
    fun ringerSkipsVibrateWithoutMotor() {
        assertEquals(Ringer.SILENT, Ringer.RING.next(hasVibrator = false))
    }

    @Test
    fun brightnessEnds() {
        assertEquals(BrightnessCurve.MIN, BrightnessCurve.toSetting(0f))
        assertEquals(BrightnessCurve.MAX, BrightnessCurve.toSetting(1f))
        assertEquals(BrightnessCurve.MIN, BrightnessCurve.toSetting(-3f))
        assertEquals(BrightnessCurve.MAX, BrightnessCurve.toSetting(7f))
    }

    @Test
    fun brightnessRoundTrip() {
        for (setting in BrightnessCurve.MIN..BrightnessCurve.MAX) {
            val back = BrightnessCurve.toSetting(BrightnessCurve.toPosition(setting))
            assertEquals(setting, back)
        }
    }

    @Test
    fun middleOfSliderIsDimmerThanHalfSetting() {
        // Square curve: half way on the slider is about a quarter of the raw range
        assertTrue(BrightnessCurve.toSetting(0.5f) < 128)
    }
}
