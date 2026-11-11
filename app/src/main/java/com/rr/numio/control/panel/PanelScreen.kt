package com.rr.numio.control.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.control.R
import com.rr.numio.control.data.PanelScene
import com.rr.numio.control.ui.theme.ControlIcons
import com.rr.numio.control.ui.theme.Numio
import com.rr.numio.control.ui.theme.NumioWordmark

// ---------------------------------------------------------------------------------
// Look: colours for the panel. The night garden has its own warm, translucent set;
// plain uses the Numio dark / light palette.
// ---------------------------------------------------------------------------------

private data class Look(
    val garden: Boolean,
    val card: Color,      // translucent panel surfaces
    val outline: Color,   // thin warm amber edge on every surface
    val off: Color,       // a round button that is off
    val offRing: Color,
    val text: Color,
    val soft: Color,
    val accent: Color,
    val onAccent: Color,
    val track: Color,
    val cream: Color,
)

private val LocalLook = staticCompositionLocalOf {
    Look(true, Color.Black, Color.Black, Color.Black, Color.Black, Color.White, Color.White, Color.Yellow, Color.Black, Color.Gray, Color.White)
}

@Composable
private fun lookFor(scene: PanelScene): Look {
    val c = Numio.colors
    return if (scene == PanelScene.NIGHT_GARDEN) {
        Look(
            garden = true,
            card = Color(0xD6181412),
            outline = Color(0x59FFC21A),
            off = Color(0xFF2C2622),
            offRing = Color(0x26F6EEDD),
            text = Color(0xFFF6EEDD),
            soft = Color(0xFFCFC3AD),
            accent = Color(0xFFFFC21A),
            onAccent = Color(0xFF1A1400),
            track = Color(0xFF3A322A),
            cream = Color(0xFFF6E7C8),
        )
    } else {
        Look(
            garden = false,
            card = c.card,
            outline = c.accent.copy(alpha = 0.28f),
            off = c.button,
            offRing = c.divider,
            text = c.text,
            soft = c.textSoft,
            accent = c.accent,
            onAccent = c.onAccent,
            track = c.track,
            cream = c.thumb,
        )
    }
}

/** A panel surface: translucent card with a thin warm outline (brighter when [lit]). */
@Composable
private fun Modifier.surface(shape: Shape = RoundedCornerShape(30.dp), lit: Boolean = false): Modifier {
    val l = LocalLook.current
    return this
        .clip(shape)
        .background(l.card)
        .border(1.dp, if (lit) l.accent.copy(alpha = 0.9f) else l.outline, shape)
}

// ---------------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------------

/** What a round button does, which decides how TalkBack describes it. */
private enum class TileKind {
    /** Switches something on and off right here (torch, auto rotate). */
    SWITCH,
    /** Can only be read; tapping opens Android's own panel or page (Wi-Fi, NFC...). */
    OPENS,
    /** Does one thing (lock, screenshot, change ringer). */
    ACTION,
}

private data class TileSpec(
    val label: String,
    val icon: ImageVector,
    val on: Boolean,
    val kind: TileKind,
    val clickLabel: String,
    val onClick: () -> Unit,
)

private fun specFor(id: TileId, c: PanelController): TileSpec? = when (id) {
    TileId.SOUND -> when (c.ringer) {
        Ringer.RING -> TileSpec("Sound", ControlIcons.Sound, true, TileKind.ACTION, "switch to vibrate", c::cycleRinger)
        Ringer.VIBRATE -> TileSpec("Vibrate", ControlIcons.Vibrate, true, TileKind.ACTION, "switch to silent", c::cycleRinger)
        Ringer.SILENT -> TileSpec("Silent", ControlIcons.Silent, false, TileKind.ACTION, "switch to sound", c::cycleRinger)
    }
    TileId.FLASHLIGHT -> if (!c.hasTorch) null else
        TileSpec("Flashlight", ControlIcons.Flashlight, c.torchOn, TileKind.SWITCH, "", c::toggleTorch)
    TileId.LOCK -> TileSpec("Lock", ControlIcons.Lock, false, TileKind.ACTION, "lock the screen", c::lockScreen)
    TileId.AIRPLANE -> TileSpec("Airplane mode", ControlIcons.Airplane, c.airplaneOn, TileKind.OPENS, "open airplane mode settings", c::openAirplane)
    TileId.LOCATION -> TileSpec("Location", ControlIcons.Location, c.locationOn, TileKind.OPENS, "open location settings", c::openLocation)
    TileId.MOBILE_DATA -> TileSpec("Mobile data", ControlIcons.MobileData, false, TileKind.OPENS, "open internet panel", c::openMobileData)
    TileId.AUTO_ROTATE -> TileSpec("Auto rotate", ControlIcons.Rotate, c.autoRotateOn, TileKind.SWITCH, "", c::toggleAutoRotate)
    TileId.NFC -> if (!c.hasNfc) null else
        TileSpec("NFC", ControlIcons.Nfc, c.nfcOn, TileKind.OPENS, "open NFC panel", c::openNfc)
    TileId.CAST -> TileSpec("Screen cast", ControlIcons.Cast, false, TileKind.OPENS, "open cast settings", c::openCast)
    TileId.POWER_SAVING -> TileSpec("Power saving", ControlIcons.PowerSaving, c.powerSavingOn, TileKind.OPENS, "open battery saver settings", c::openPowerSaving)
    TileId.SCREENSHOT -> TileSpec("Screenshot", ControlIcons.Screenshot, false, TileKind.ACTION, "take a screenshot", c::screenshot)
}

/** A round button face: amber with a warm glow when on, soft charcoal and cream when off. */
@Composable
private fun Orb(icon: ImageVector, on: Boolean, size: Dp, iconSize: Dp = 26.dp) {
    val l = LocalLook.current
    val base = Modifier.size(size)
    val face = if (on) {
        base.shadow(12.dp, CircleShape, ambientColor = l.accent, spotColor = l.accent).clip(CircleShape).background(l.accent)
    } else {
        base.clip(CircleShape).background(l.off).border(1.dp, l.offRing, CircleShape)
    }
    Box(face, contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = if (on) l.onAccent else l.text, modifier = Modifier.size(iconSize))
    }
}

// ---------------------------------------------------------------------------------
// The panel
// ---------------------------------------------------------------------------------

private val KittenWidth = 160.dp
private val KittenHeight = 102.dp   // the picture is 900 x 576
private val PawOverlap = 9.dp       // paws hang this far over the top of the Wi-Fi bar

/** The panel: drawn in the accessibility overlay window over whatever app is open. */
@Composable
fun PanelScreen(
    controller: PanelController,
    layout: PanelLayout,
    scene: PanelScene,
    visible: Boolean,
    onClose: () -> Unit,
) {
    val look = lookFor(scene)
    CompositionLocalProvider(LocalLook provides look) {
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
                // Behind the panel: the scene, or a plain dim. Tap it to close.
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (look.garden) {
                                Modifier.background(
                                    Brush.verticalGradient(listOf(Color(0xFF0C1328), Color(0xFF15131C), Color(0xFF1C1611))),
                                )
                            } else {
                                Modifier.background(Numio.colors.scrim)
                            },
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = "close panel",
                            onClick = onClose,
                        ),
                ) {
                    if (look.garden) {
                        Image(
                            painter = painterResource(R.drawable.scene_night_room),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = visible,
                // Slide only: no fade on the panel itself, so it is never left see-through
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Column(
                    Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        // Taps on the panel itself must not reach the close-on-tap layer behind it
                        .pointerInput(Unit) { detectTapGestures { } }
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TopRow(controller, look.garden)
                    ConnectionsWithKitten(controller, look.garden)
                    TileGrid(layout.tiles.mapNotNull { specFor(it, controller) })
                    SliderBar(
                        leading = ControlIcons.Sun,
                        name = "Brightness",
                        value = controller.brightness,
                        fill = Brush.horizontalGradient(listOf(Color(0xFFD98A00), look.accent)),
                        knobRing = look.accent,
                        onChange = controller::changeBrightness,
                        trailing = ControlIcons.SunAuto,
                        trailingOn = controller.autoBrightnessOn,
                        trailingName = "Auto brightness",
                        onTrailing = controller::toggleAutoBrightness,
                    )
                    SliderBar(
                        leading = ControlIcons.Volume,
                        name = "Media volume",
                        value = controller.volume,
                        fill = Brush.horizontalGradient(listOf(look.cream.copy(alpha = 0.7f), look.cream)),
                        knobRing = look.cream,
                        onChange = controller::changeVolume,
                        trailing = ControlIcons.VolumeOff,
                        trailingOn = controller.muted,
                        trailingName = "Mute",
                        onTrailing = controller::toggleMute,
                    )
                    MusicCard(controller)
                    CloseBar(onClose)
                }
            }
        }
    }
}

/** Wordmark on the left. In the plain look the header buttons sit here too. */
@Composable
private fun TopRow(controller: PanelController, garden: Boolean) {
    val l = LocalLook.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (garden) Color(0xB30D0B0A) else Color.Transparent)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            NumioWordmark(height = 18.dp, textColor = l.text)
        }
        Spacer(Modifier.weight(1f))
        if (!garden) HeaderButtons(controller)
    }
}

@Composable
private fun HeaderButtons(controller: PanelController) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        HeaderButton(ControlIcons.Bell, "Notifications", controller::systemNotifications)
        HeaderButton(ControlIcons.Edit, "Edit panel", controller::openApp)
        HeaderButton(ControlIcons.Power, "Power menu", controller::powerMenu)
        HeaderButton(ControlIcons.Settings, "Control settings", controller::openApp)
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val l = LocalLook.current
    Box(
        Modifier
            .size(46.dp)
            .surface(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = l.text, modifier = Modifier.size(22.dp))
    }
}

/**
 * Wi-Fi and Bluetooth, big. In the night garden the kitten peeks over the Wi-Fi bar
 * with its paws on the edge (drawn after the bar, so on top of it), the header buttons
 * sit to its right, and a few stars twinkle around it. Nothing covers a button or label.
 */
@Composable
private fun ConnectionsWithKitten(controller: PanelController, garden: Boolean) {
    val kittenTop = if (garden) KittenHeight - PawOverlap else 0.dp
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(top = kittenTop),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ConnectionPill(
                icon = ControlIcons.Wifi, title = "Wi-Fi", on = controller.wifiOn,
                clickLabel = "open Wi-Fi panel", onClick = controller::openWifi,
            )
            ConnectionPill(
                icon = ControlIcons.Bluetooth, title = "Bluetooth", on = controller.bluetoothOn,
                clickLabel = "open Bluetooth settings", onClick = controller::openBluetooth,
            )
        }
        if (garden) {
            Twinkle(Modifier.offset(x = 6.dp, y = 18.dp), 10.dp, 0)
            Twinkle(Modifier.align(Alignment.TopEnd).offset(x = (-30).dp, y = 4.dp), 12.dp, 500)
            Twinkle(Modifier.align(Alignment.TopEnd).offset(x = (-150).dp, y = 14.dp), 8.dp, 1000)
            Image(
                painter = painterResource(R.drawable.scene_night_garden_kitten),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .width(KittenWidth)
                    .height(KittenHeight)
                    .clearAndSetSemantics { },
            )
            Box(Modifier.align(Alignment.TopEnd).padding(top = kittenTop - 56.dp)) {
                HeaderButtons(controller)
            }
        }
    }
}

/** A small four-point star that slowly twinkles. Decoration only. */
@Composable
private fun Twinkle(modifier: Modifier, size: Dp, delayMs: Int) {
    val t = rememberInfiniteTransition(label = "twinkle")
    val alpha by t.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, delayMillis = delayMs), RepeatMode.Reverse),
        label = "alpha",
    )
    Canvas(modifier.size(size).graphicsLayer { this.alpha = alpha }.clearAndSetSemantics { }) {
        val w = this.size.width
        val h = this.size.height
        val c = Offset(w / 2, h / 2)
        val k = w * 0.14f
        val star = Path().apply {
            moveTo(c.x, 0f)
            lineTo(c.x + k, c.y - k)
            lineTo(w, c.y)
            lineTo(c.x + k, c.y + k)
            lineTo(c.x, h)
            lineTo(c.x - k, c.y + k)
            lineTo(0f, c.y)
            lineTo(c.x - k, c.y - k)
            close()
        }
        drawPath(star, Color(0xFFFFE3A0))
    }
}

@Composable
private fun RowScope.ConnectionPill(
    icon: ImageVector,
    title: String,
    on: Boolean,
    clickLabel: String,
    onClick: () -> Unit,
) {
    val l = LocalLook.current
    val state = if (on) "On" else "Off"
    Row(
        Modifier
            .weight(1f)
            .height(84.dp)
            .surface(RoundedCornerShape(42.dp), lit = on)
            .clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = state }
            .padding(start = 12.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Orb(icon, on, 58.dp, 28.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, color = l.text, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(state, color = if (on) l.accent else l.soft, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Icon(ControlIcons.Chevron, contentDescription = null, tint = l.soft, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TileGrid(tiles: List<TileSpec>) {
    Column(
        Modifier
            .fillMaxWidth()
            .surface(RoundedCornerShape(32.dp))
            .padding(horizontal = 4.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        tiles.chunked(PanelLayout.COLUMNS).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { Tile(it) }
                repeat(PanelLayout.COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RowScope.Tile(spec: TileSpec) {
    val l = LocalLook.current
    val state = if (spec.on) "On" else "Off"
    val click = when (spec.kind) {
        TileKind.SWITCH -> Modifier.toggleable(value = spec.on, role = Role.Switch, onValueChange = { spec.onClick() })
        TileKind.OPENS -> Modifier
            .clickable(role = Role.Button, onClickLabel = spec.clickLabel, onClick = spec.onClick)
            .semantics { stateDescription = state }
        TileKind.ACTION -> Modifier.clickable(role = Role.Button, onClickLabel = spec.clickLabel, onClick = spec.onClick)
    }
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(18.dp))
            .then(click)
            .semantics(mergeDescendants = true) { }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Orb(spec.icon, spec.on, 56.dp)
        Text(
            spec.label,
            color = if (spec.on) l.text else l.soft,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = if (spec.on) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp, start = 2.dp, end = 2.dp),
        )
    }
}

/**
 * A slider in its own bar: a thick rounded track with a warm gradient fill and a
 * cream knob ringed in the fill colour. Still a real Material slider underneath,
 * so TalkBack can adjust it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderBar(
    leading: ImageVector,
    name: String,
    value: Float,
    fill: Brush,
    knobRing: Color,
    onChange: (Float) -> Unit,
    trailing: ImageVector,
    trailingOn: Boolean,
    trailingName: String,
    onTrailing: () -> Unit,
) {
    val l = LocalLook.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(68.dp)
            .surface(RoundedCornerShape(34.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(leading, contentDescription = null, tint = l.accent, modifier = Modifier.size(26.dp))
        }
        Slider(
            value = value,
            onValueChange = onChange,
            thumb = {
                Box(
                    Modifier
                        .size(30.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(l.cream)
                        .border(4.dp, knobRing, CircleShape),
                )
            },
            track = { state ->
                val range = state.valueRange.endInclusive - state.valueRange.start
                val fraction = if (range <= 0f) 0f else ((state.value - state.valueRange.start) / range).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(l.track),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(13.dp))
                            .background(fill),
                    )
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .semantics { contentDescription = name },
        )
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (trailingOn) l.accent else Color.Transparent)
                .toggleable(value = trailingOn, role = Role.Switch, onValueChange = { onTrailing() })
                .semantics { contentDescription = trailingName },
            contentAlignment = Alignment.Center,
        ) {
            Icon(trailing, contentDescription = null, tint = if (trailingOn) l.onAccent else l.text, modifier = Modifier.size(24.dp))
        }
    }
}

/** The music card: what's playing, with its own controls. */
@Composable
private fun MusicCard(controller: PanelController) {
    val l = LocalLook.current
    val m = controller.media
    Column(
        Modifier
            .fillMaxWidth()
            .surface(RoundedCornerShape(30.dp), lit = m.playing)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF1B2440), Color(0xFF3A2A18))))
                    .then(if (m.hasMedia) Modifier.clickable(role = Role.Button, onClickLabel = "open the music app", onClick = controller::openMusicApp) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                val art = m.art
                if (art != null) {
                    Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(ControlIcons.Music, contentDescription = null, tint = l.accent, modifier = Modifier.size(28.dp))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                when {
                    !m.hasAccess -> {
                        Text("Your music, here", color = l.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Allow notification access to see and control what's playing.", color = l.soft, fontSize = 12.sp, lineHeight = 15.sp)
                    }
                    !m.hasMedia -> {
                        Text("Nothing playing", color = l.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Start music in any app and it shows here.", color = l.soft, fontSize = 12.sp, lineHeight = 15.sp)
                    }
                    else -> {
                        Text(
                            m.title.orEmpty(),
                            color = l.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(),
                        )
                        Text(m.artist.orEmpty(), color = l.soft, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (!m.hasAccess) {
                Box(
                    Modifier
                        .heightIn(min = 44.dp)
                        .clip(CircleShape)
                        .background(l.accent)
                        .clickable(role = Role.Button, onClickLabel = "open notification access settings", onClick = controller::askMusicAccess)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Allow", color = l.onAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else if (m.hasMedia) {
                Box(
                    Modifier
                        .size(44.dp)
                        .surface(CircleShape)
                        .clickable(role = Role.Button, onClick = controller::mediaOutput)
                        .semantics { contentDescription = "Choose where to play" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(ControlIcons.Output, contentDescription = null, tint = l.text, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (m.hasAccess && m.hasMedia) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaButton(ControlIcons.Previous, "Previous track", big = false) { m.previous() }
                MediaButton(if (m.playing) ControlIcons.Pause else ControlIcons.Play, if (m.playing) "Pause" else "Play", big = true) { m.playPause() }
                MediaButton(ControlIcons.Next, "Next track", big = false) { m.next() }
            }
        }
    }
}

@Composable
private fun MediaButton(icon: ImageVector, description: String, big: Boolean, onClick: () -> Unit) {
    val l = LocalLook.current
    val size = if (big) 56.dp else 48.dp
    Box(
        Modifier
            .size(size)
            .then(
                if (big) {
                    Modifier.shadow(10.dp, CircleShape, ambientColor = l.accent, spotColor = l.accent).clip(CircleShape).background(l.accent)
                } else {
                    Modifier.clip(CircleShape)
                },
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (big) l.onAccent else l.text, modifier = Modifier.size(if (big) 26.dp else 24.dp))
    }
}

@Composable
private fun CloseBar(onClose: () -> Unit) {
    val l = LocalLook.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClose)
            .semantics { contentDescription = "Close panel" },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(48.dp).height(5.dp).clip(CircleShape).background(l.soft))
    }
}
