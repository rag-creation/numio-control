package com.rr.numio.control.ui.setup

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.control.data.AppSettings
import com.rr.numio.control.data.HandlePosition
import com.rr.numio.control.data.HandleSettings
import com.rr.numio.control.data.HandleSide
import com.rr.numio.control.data.OpenMode
import com.rr.numio.control.data.PanelScene
import com.rr.numio.control.service.ControlService
import com.rr.numio.control.ui.theme.ControlIcons
import com.rr.numio.control.ui.theme.Numio
import com.rr.numio.control.ui.theme.NumioWordmark
import com.rr.numio.control.ui.theme.ThemeMode
import kotlinx.coroutines.launch

private const val SOURCE_URL = "https://github.com/rag-creation/numio-control"

/** The app's only screen for now: setting up the panel, and its settings. */
@Composable
fun SetupScreen(settings: AppSettings, refreshKey: Int) {
    val c = Numio.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val serviceOn by ControlService.isRunning.collectAsState()
    val canWrite = remember(refreshKey) { Settings.System.canWrite(context) }
    val hasDnd = remember(refreshKey) {
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
    }
    val theme by settings.themeMode.collectAsState(initial = ThemeMode.DEFAULT)
    val scene by settings.scene.collectAsState(initial = PanelScene.NIGHT_GARDEN)
    val handle by settings.handle.collectAsState(initial = HandleSettings(HandleSide.RIGHT, HandlePosition.TOP))
    val hasMusic = remember(refreshKey) {
        androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ---- Header ----
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    NumioWordmark(height = 16.dp, textColor = c.text)
                    Text(
                        "Control",
                        style = MaterialTheme.typography.headlineMedium,
                        color = c.text,
                        modifier = Modifier.padding(top = 6.dp).semantics { heading() },
                    )
                }
                Row(
                    Modifier
                        .clip(CircleShape)
                        .border(1.dp, c.divider, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(ControlIcons.NoInternet, contentDescription = null, tint = c.accentInk, modifier = Modifier.size(16.dp))
                    Text("No internet, ever", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                }
            }
            Text(
                "A control panel for your phone. Swipe down from the top right of the screen to open it.",
                color = c.textSoft,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            // ---- Setup ----
            SectionTitle("Set up")
            Card {
                Step(
                    title = "Turn on the panel",
                    body = "Accessibility > Installed apps > Control by Numio panel. It only draws the panel; it can't read your screen.",
                    done = serviceOn,
                    onClick = { context.openSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                )
                if (!serviceOn) {
                    Hint(
                        "Switch greyed out? Android blocks it for apps installed from a file. Open App info, tap ⋮, choose Allow restricted settings, then try again.",
                        action = "Open App info",
                        onClick = {
                            context.openSafely(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)),
                            )
                        },
                    )
                }
                Divider()
                Step(
                    title = "Brightness and auto rotate",
                    body = "Allow Change system settings.",
                    done = canWrite,
                    onClick = {
                        context.openSafely(
                            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.packageName)),
                        )
                    },
                )
                Divider()
                Step(
                    title = "Silent mode",
                    body = "Allow Do Not Disturb access, so the Sound button can switch to silent.",
                    done = hasDnd,
                    onClick = { context.openSafely(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) },
                )
                Divider()
                Step(
                    title = "Music card (optional)",
                    body = "Allow notification access, so the panel can show what's playing and control it. Notifications are never read.",
                    done = hasMusic,
                    onClick = { context.openSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                )
            }
            PrimaryButton(
                text = if (serviceOn) "Show panel now" else "Turn on the panel first",
                enabled = serviceOn,
                onClick = { ControlService.showPanel() },
            )

            // ---- Panel ----
            SectionTitle("Panel")
            Card {
                Choice(
                    label = "Open with",
                    options = listOf(
                        "Top right" to (handle.mode == OpenMode.TOP_RIGHT),
                        "Whole top" to (handle.mode == OpenMode.TOP_FULL),
                        "Side edge" to (handle.mode == OpenMode.EDGE),
                    ),
                    onPick = { i -> scope.launch { settings.setOpenMode(OpenMode.entries[i]) } },
                )
                Text(
                    when (handle.mode) {
                        OpenMode.TOP_RIGHT -> "Swipe down from the right half of the top edge. The left half still opens Samsung's panel."
                        OpenMode.TOP_FULL -> "Swipe down anywhere on the top edge. The bell button in the panel opens your notifications."
                        OpenMode.EDGE -> "Swipe in from the amber handle, or tap it."
                    },
                    color = c.textSoft,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
                if (handle.mode == OpenMode.EDGE) {
                    Divider()
                    Choice(
                        label = "Handle side",
                        options = listOf("Right" to (handle.side == HandleSide.RIGHT), "Left" to (handle.side == HandleSide.LEFT)),
                        onPick = { i -> scope.launch { settings.setHandleSide(if (i == 0) HandleSide.RIGHT else HandleSide.LEFT) } },
                    )
                    Divider()
                    Choice(
                        label = "Handle position",
                        options = listOf("Top" to (handle.position == HandlePosition.TOP), "Middle" to (handle.position == HandlePosition.MIDDLE)),
                        onPick = { i -> scope.launch { settings.setHandlePosition(if (i == 0) HandlePosition.TOP else HandlePosition.MIDDLE) } },
                    )
                }
                Divider()
                Choice(
                    label = "Scene",
                    options = listOf("Night garden" to (scene == PanelScene.NIGHT_GARDEN), "Plain" to (scene == PanelScene.NONE)),
                    onPick = { i -> scope.launch { settings.setScene(if (i == 0) PanelScene.NIGHT_GARDEN else PanelScene.NONE) } },
                )
                Divider()
                Choice(
                    label = "Look",
                    options = ThemeMode.entries.map { it.title() to (theme == it) },
                    onPick = { i -> scope.launch { settings.setThemeMode(ThemeMode.entries[i]) } },
                )
            }
            Text(
                "Rearranging and resizing the buttons comes in the next update.",
                color = c.textSoft,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            // ---- Accessibility ----
            SectionTitle("Accessibility")
            Card {
                Text(
                    "Every button has a spoken label for TalkBack and tells you whether it is on or off. " +
                        "With TalkBack, the top strip (or the edge handle) is a button called \"Open Control panel\". " +
                        "Back or the Close button at the bottom closes the panel.",
                    color = c.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }

            // ---- About ----
            Card {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable(role = Role.Button) { context.openSafely(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL))) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Source code", color = c.text, fontWeight = FontWeight.Bold)
                        Text("Free and open source, GPL-3.0", color = c.textSoft, fontSize = 13.sp)
                    }
                    Icon(ControlIcons.Chevron, contentDescription = null, tint = c.textSoft, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                "Control by Numio $version · Made by RR",
                color = c.textSoft,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private fun ThemeMode.title(): String = when (this) {
    ThemeMode.DARK -> "Dark"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.SYSTEM -> "System"
}

private fun Context.openSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        runCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        color = Numio.colors.textSoft,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp).semantics { heading() },
    )
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Numio.colors.card),
    ) { content() }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Numio.colors.divider))
}

@Composable
private fun Step(title: String, body: String, done: Boolean, onClick: () -> Unit) {
    val c = Numio.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "open settings", onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = if (done) "Allowed" else "Not allowed yet" }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(body, color = c.textSoft, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.size(12.dp))
        if (done) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(ControlIcons.Check, contentDescription = null, tint = c.accentInk, modifier = Modifier.size(18.dp))
                Text("Allowed", color = c.accentInk, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
            }
        } else {
            Box(
                Modifier.clip(CircleShape).background(c.accent).padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text("Allow", color = c.onAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Hint(text: String, action: String, onClick: () -> Unit) {
    val c = Numio.colors
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, c.divider, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Text(text, color = c.textSoft, fontSize = 13.sp)
        Text(
            action,
            color = c.accentInk,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 44.dp)
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun Choice(label: String, options: List<Pair<String, Boolean>>, onPick: (Int) -> Unit) {
    val c = Numio.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, color = c.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Row(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(c.button)
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEachIndexed { i, (name, selected) ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) c.accent else Color.Transparent)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(i) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(name, color = if (selected) c.onAccent else c.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Numio.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) c.accent else c.button)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) c.onAccent else c.textSoft, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
