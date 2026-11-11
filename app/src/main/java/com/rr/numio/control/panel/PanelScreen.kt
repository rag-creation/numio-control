package com.rr.numio.control.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.control.R
import com.rr.numio.control.data.PanelScene
import com.rr.numio.control.ui.theme.ControlIcons
import com.rr.numio.control.ui.theme.Numio
import com.rr.numio.control.ui.theme.NumioWordmark

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

/** The panel: drawn in the accessibility overlay window over whatever app is open. */
@Composable
fun PanelScreen(
    controller: PanelController,
    layout: PanelLayout,
    scene: PanelScene,
    visible: Boolean,
    onClose: () -> Unit,
) {
    val c = Numio.colors
    val room = scene == PanelScene.NIGHT_ROOM
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            // Tap outside the panel to close it. TalkBack users get the Close button below.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (room) Color(0xFF14120F) else c.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "close panel",
                        onClick = onClose,
                    ),
            ) {
                if (room) {
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Night room: start below the window so the moon and lamp stay in view
                if (room) Spacer(Modifier.height(56.dp))
                Header(controller)
                Box(Modifier.padding(top = if (room) 58.dp else 0.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ConnectionPill(
                        icon = ControlIcons.Wifi, title = "Wi-Fi", on = controller.wifiOn,
                        clickLabel = "open Wi-Fi panel", onClick = controller::openWifi,
                    )
                    ConnectionPill(
                        icon = ControlIcons.Bluetooth, title = "Bluetooth", on = controller.bluetoothOn,
                        clickLabel = "open Bluetooth settings", onClick = controller::openBluetooth,
                    )
                }
                if (room) {
                    // The kitten sleeps on top of the bar, paws over its edge
                    Image(
                        painter = painterResource(R.drawable.scene_night_room_cat),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = 80.dp, y = (-64).dp)
                            .width(200.dp)
                            .height(78.dp),
                    )
                }
                }
                TileGrid(layout.tiles.mapNotNull { specFor(it, controller) })
                SliderCard(controller)
                CloseBar(onClose)
            }
        }
    }
}

@Composable
private fun Header(controller: PanelController) {
    val c = Numio.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NumioWordmark(height = 20.dp, textColor = c.text)
        Spacer(Modifier.weight(1f))
        HeaderButton(ControlIcons.Bell, "Notifications", controller::systemNotifications)
        HeaderButton(ControlIcons.Edit, "Edit panel", controller::openApp)
        HeaderButton(ControlIcons.Power, "Power menu", controller::powerMenu)
        HeaderButton(ControlIcons.Settings, "Control settings", controller::openApp)
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = Numio.colors
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.text, modifier = Modifier.size(24.dp))
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
    val c = Numio.colors
    val state = if (on) "On" else "Off"
    Row(
        Modifier
            .weight(1f)
            .height(76.dp)
            .clip(RoundedCornerShape(38.dp))
            .background(c.card)
            .clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = state }
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(54.dp).clip(CircleShape).background(if (on) c.accent else c.button),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (on) c.onAccent else c.text, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(state, color = c.textSoft, fontSize = 13.sp, maxLines = 1)
        }
        Icon(ControlIcons.Chevron, contentDescription = null, tint = c.textSoft, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TileGrid(tiles: List<TileSpec>) {
    val c = Numio.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(c.card)
            .padding(horizontal = 6.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
    val c = Numio.colors
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
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(if (spec.on) c.accent else c.button),
            contentAlignment = Alignment.Center,
        ) {
            Icon(spec.icon, contentDescription = null, tint = if (spec.on) c.onAccent else c.text, modifier = Modifier.size(26.dp))
        }
        Text(
            spec.label,
            color = c.text,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
        )
    }
}

@Composable
private fun SliderCard(controller: PanelController) {
    val c = Numio.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(c.card)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        SliderRow(
            leading = ControlIcons.Sun,
            name = "Brightness",
            value = controller.brightness,
            fill = c.accent,
            onChange = controller::changeBrightness,
            trailing = ControlIcons.SunAuto,
            trailingOn = controller.autoBrightnessOn,
            trailingName = "Auto brightness",
            onTrailing = controller::toggleAutoBrightness,
        )
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(1.dp).background(c.divider))
        SliderRow(
            leading = ControlIcons.Volume,
            name = "Media volume",
            value = controller.volume,
            fill = c.thumb,
            onChange = controller::changeVolume,
            trailing = ControlIcons.VolumeOff,
            trailingOn = controller.muted,
            trailingName = "Mute",
            onTrailing = controller::toggleMute,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderRow(
    leading: ImageVector,
    name: String,
    value: Float,
    fill: Color,
    onChange: (Float) -> Unit,
    trailing: ImageVector,
    trailingOn: Boolean,
    trailingName: String,
    onTrailing: () -> Unit,
) {
    val c = Numio.colors
    val colors = SliderDefaults.colors(
        thumbColor = c.thumb,
        activeTrackColor = fill,
        inactiveTrackColor = c.track,
    )
    Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(leading, contentDescription = null, tint = c.accentInk, modifier = Modifier.size(26.dp))
        }
        Slider(
            value = value,
            onValueChange = onChange,
            colors = colors,
            // A round cream knob and a thick track with no gap or end dot, like the pictures
            thumb = {
                Box(Modifier.size(28.dp).clip(CircleShape).background(c.thumb))
            },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    colors = colors,
                    drawStopIndicator = null,
                    thumbTrackGapSize = 0.dp,
                    modifier = Modifier.height(14.dp),
                )
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
                .background(if (trailingOn) c.accent else Color.Transparent)
                .toggleable(value = trailingOn, role = Role.Switch, onValueChange = { onTrailing() })
                .semantics { contentDescription = trailingName },
            contentAlignment = Alignment.Center,
        ) {
            Icon(trailing, contentDescription = null, tint = if (trailingOn) c.onAccent else c.text, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun CloseBar(onClose: () -> Unit) {
    val c = Numio.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClose)
            .semantics { contentDescription = "Close panel" },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(44.dp).height(5.dp).clip(CircleShape).background(c.textSoft))
    }
}
