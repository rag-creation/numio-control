package com.rr.numio.control.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Line icons on a 24 x 24 grid, 2 px stroke. Drawn for Control by Numio; a few
// shapes (Wi-Fi arcs, Bluetooth, lock shackle) follow Lucide, ISC licence
// (licenses/Lucide-ISC.txt). Icon() tints them, so the stroke colour here doesn't matter.
private fun lineIcon(name: String, path: String, filled: String? = null): ImageVector {
    val b = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(path),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
    if (filled != null) b.addPath(pathData = addPathNodes(filled), fill = SolidColor(Color.Black))
    return b.build()
}

object ControlIcons {
    val Wifi = lineIcon("wifi", "M2 8.5a15 15 0 0 1 20 0M5 12a10 10 0 0 1 14 0M8.5 15.5a5 5 0 0 1 7 0", "M12 18a1.3 1.3 0 1 1 0 2.6a1.3 1.3 0 1 1 0-2.6z")
    val Bluetooth = lineIcon("bluetooth", "M7 7l10 10-5 5V2l5 5L7 17")
    val Sound = lineIcon("sound", "M4 9h4l5-4v14l-5-4H4zM16.5 8.5a5 5 0 0 1 0 7M19.5 6a9 9 0 0 1 0 12")
    val Vibrate = lineIcon("vibrate", "M9 4h6a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H9a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1zM4.5 8v8M19.5 8v8")
    val Silent = lineIcon("silent", "M8.7 4.6A6 6 0 0 1 18 10v4M6 9.5V16l-2 2h12M10 21h4M3 3l18 18")
    val Flashlight = lineIcon("flashlight", "M7 3h10v3l-2 3v12H9V9L7 6zM12 13v2")
    val Lock = lineIcon("lock", "M7 11V7a5 5 0 0 1 10 0v4M5 11h14v10H5z")
    val Airplane = lineIcon("airplane", "M10.5 3.5a1.5 1.5 0 0 1 3 0V9l7 4v2l-7-2v4.5l2 1.5v1.5L12 19.5l-3.5 1V19l2-1.5V13l-7 2v-2l7-4z")
    val Location = lineIcon("location", "M12 21s-7-6.5-7-12a7 7 0 0 1 14 0c0 5.5-7 12-7 12zM12 6.5a2.5 2.5 0 1 1 0 5a2.5 2.5 0 1 1 0-5z")
    val MobileData = lineIcon("mobile_data", "M7 20V4M3 8l4-4 4 4M17 4v16M13 16l4 4 4-4")
    val Rotate = lineIcon("rotate", "M20 12a8 8 0 1 1-2.3-5.7M20 3.5V7h-3.5M10 11V9.5a2 2 0 0 1 4 0V11M9 11h6v5H9z")
    val Nfc = lineIcon("nfc", "M5 4h14a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1zM9 16V8l6 8V8")
    val Cast = lineIcon("cast", "M3 16a5 5 0 0 1 5 5M3 12a9 9 0 0 1 9 9M3 8V6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-5", "M3 19.5a1.2 1.2 0 1 1 0 2.4a1.2 1.2 0 1 1 0-2.4z")
    val PowerSaving = lineIcon("power_saving", "M11 20a7 7 0 0 1-7-7c0-6 6-9 16-9 0 10-3 16-9 16zM4 20c3-5 6-8 10-10")
    val Screenshot = lineIcon("screenshot", "M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3")
    val Sun = lineIcon("sun", "M12 8a4 4 0 1 1 0 8a4 4 0 1 1 0-8zM12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4")
    val SunAuto = lineIcon("sun_auto", "M12 7a5 5 0 1 1 0 10a5 5 0 1 1 0-10zM12 1.5v2M12 20.5v2M1.5 12h2M20.5 12h2M4.6 4.6l1.4 1.4M18 18l1.4 1.4M4.6 19.4L6 18M18 6l1.4-1.4M10.3 14.5L12 9.5l1.7 5M10.9 13h2.2")
    val Volume = lineIcon("volume", "M4 9h4l5-4v14l-5-4H4zM16.5 8.5a5 5 0 0 1 0 7")
    val VolumeOff = lineIcon("volume_off", "M4 9h4l5-4v14l-5-4H4zM16 9.5l5 5M21 9.5l-5 5")
    val Edit = lineIcon("edit", "M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4")
    val Power = lineIcon("power", "M12 3v8M6.3 6.3a8 8 0 1 0 11.4 0")
    val Settings = lineIcon("settings", "M12 9a3 3 0 1 1 0 6a3 3 0 1 1 0-6zM12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9L7 7M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1")
    val Chevron = lineIcon("chevron", "M9 6l6 6-6 6")
    val Check = lineIcon("check", "M5 12.5l4.5 4.5L19 7.5")
    val NoInternet = lineIcon("no_internet", "M2 8.5a15 15 0 0 1 6-3.6M22 8.5a15 15 0 0 0-9-3.4M5 12a10 10 0 0 1 3.5-2.2M19 12a10 10 0 0 0-4-2.5M8.5 15.5a5 5 0 0 1 7 0M3 3l18 18")
}
