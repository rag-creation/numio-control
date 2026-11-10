package com.rr.numio.control.panel

// Plain Kotlin (no Android), so it can be unit tested on the computer.

/** Every round button the panel can show. The key is what gets saved, so never rename one. */
enum class TileId(val key: String) {
    SOUND("sound"),
    FLASHLIGHT("flashlight"),
    LOCK("lock"),
    AIRPLANE("airplane"),
    LOCATION("location"),
    MOBILE_DATA("mobile_data"),
    AUTO_ROTATE("auto_rotate"),
    NFC("nfc"),
    CAST("cast"),
    POWER_SAVING("power_saving"),
    SCREENSHOT("screenshot");

    companion object {
        fun fromKey(key: String): TileId? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Which round buttons the panel shows, in order. The editor (milestone 2) changes this;
 * for now it is the default layout. Saved as a short comma list, e.g. "sound,flashlight".
 */
data class PanelLayout(val tiles: List<TileId>) {

    fun encode(): String = tiles.joinToString(",") { it.key }

    companion object {
        val DEFAULT = PanelLayout(
            listOf(
                TileId.SOUND, TileId.FLASHLIGHT, TileId.LOCK, TileId.AIRPLANE, TileId.LOCATION,
                TileId.MOBILE_DATA, TileId.AUTO_ROTATE, TileId.NFC, TileId.CAST, TileId.POWER_SAVING,
            ),
        )

        /** Buttons per row in the grid. */
        const val COLUMNS = 5

        /**
         * Reads a saved layout. Unknown keys (from a newer version) are skipped and
         * repeats are dropped. Nothing saved, or nothing usable, gives [DEFAULT].
         */
        fun decode(saved: String?): PanelLayout {
            if (saved.isNullOrBlank()) return DEFAULT
            val tiles = saved.split(",")
                .mapNotNull { TileId.fromKey(it.trim()) }
                .distinct()
            return if (tiles.isEmpty()) DEFAULT else PanelLayout(tiles)
        }
    }
}

/** Sound button: ring, then vibrate, then silent, then back to ring. */
enum class Ringer {
    RING, VIBRATE, SILENT;

    /** Phones without a vibration motor skip vibrate. */
    fun next(hasVibrator: Boolean): Ringer = when (this) {
        RING -> if (hasVibrator) VIBRATE else SILENT
        VIBRATE -> SILENT
        SILENT -> RING
    }
}

/**
 * Android stores screen brightness as a number from 1 to 255, but eyes see it
 * curved: the bottom of the range changes a lot more than the top. The slider uses
 * a square curve so the middle of the slider looks like the middle brightness.
 */
object BrightnessCurve {
    const val MIN = 1
    const val MAX = 255

    /** Slider position (0 to 1) to Android's brightness setting. */
    fun toSetting(position: Float): Int {
        val p = position.coerceIn(0f, 1f)
        return (MIN + p * p * (MAX - MIN)).let { Math.round(it) }.coerceIn(MIN, MAX)
    }

    /** Android's brightness setting to a slider position (0 to 1). */
    fun toPosition(setting: Int): Float {
        val s = setting.coerceIn(MIN, MAX)
        return kotlin.math.sqrt((s - MIN).toFloat() / (MAX - MIN))
    }
}
