package com.rr.numio.control.panel

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rr.numio.control.MainActivity

/**
 * Reads the phone's state for the panel and carries out what each button does.
 * State lives in Compose state, so the panel redraws when it changes.
 *
 * What Android allows a normal app to switch directly: torch, ringer (silent needs
 * Do Not Disturb access), auto rotate and brightness (need "Change system settings"),
 * media volume. Wi-Fi, Bluetooth, airplane mode, location, mobile data, NFC, cast and
 * power saving can only be read, so those buttons open Android's own panel or page.
 */
class PanelController(
    private val context: Context,
    /** Closes the panel. Called after a button opens another screen. */
    private val closePanel: () -> Unit,
    /** Runs an accessibility global action (lock screen, power menu, screenshot). */
    private val globalAction: (Int) -> Unit,
) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val camera = context.getSystemService(CameraManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val power = context.getSystemService(PowerManager::class.java)
    private val vibrator = context.getSystemService(Vibrator::class.java)

    var wifiOn by mutableStateOf(false); private set
    var bluetoothOn by mutableStateOf(false); private set
    var ringer by mutableStateOf(Ringer.RING); private set
    var torchOn by mutableStateOf(false); private set
    var airplaneOn by mutableStateOf(false); private set
    var locationOn by mutableStateOf(false); private set
    var autoRotateOn by mutableStateOf(false); private set
    var nfcOn by mutableStateOf(false); private set
    var hasNfc by mutableStateOf(false); private set
    var powerSavingOn by mutableStateOf(false); private set
    var brightness by mutableFloatStateOf(0.5f); private set
    var autoBrightnessOn by mutableStateOf(false); private set
    var volume by mutableFloatStateOf(0.5f); private set
    var muted by mutableStateOf(false); private set

    /** The music card. */
    val media = MediaWatcher(context)

    private val torchId: String? = runCatching {
        camera.cameraIdList.firstOrNull { id ->
            val c = camera.getCameraCharacteristics(id)
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }.getOrNull()

    val hasTorch: Boolean get() = torchId != null

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchId) torchOn = enabled
        }
    }

    fun start() {
        runCatching { camera.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper())) }
        refresh()
    }

    fun stop() {
        media.stop()
        runCatching { camera.unregisterTorchCallback(torchCallback) }
    }

    /** Reads everything again. Called each time the panel opens. */
    @SuppressLint("MissingPermission") // Bluetooth on/off needs no permission from Android 12
    fun refresh() {
        val cr = context.contentResolver
        wifiOn = runCatching { context.getSystemService(WifiManager::class.java).isWifiEnabled }.getOrDefault(false)
        bluetoothOn = runCatching {
            context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true
        }.getOrDefault(false)
        ringer = when (audio.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> Ringer.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> Ringer.VIBRATE
            else -> Ringer.RING
        }
        airplaneOn = Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        locationOn = runCatching { context.getSystemService(LocationManager::class.java).isLocationEnabled }.getOrDefault(false)
        autoRotateOn = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        val nfc = runCatching { NfcAdapter.getDefaultAdapter(context) }.getOrNull()
        hasNfc = nfc != null
        nfcOn = nfc?.isEnabled == true
        powerSavingOn = power.isPowerSaveMode
        brightness = BrightnessCurve.toPosition(
            Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128),
        )
        autoBrightnessOn = Settings.System.getInt(
            cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
        muted = audio.isStreamMute(AudioManager.STREAM_MUSIC) || volume == 0f
        media.refresh()
    }

    // ---- Permissions ----

    val canChangeSettings: Boolean get() = Settings.System.canWrite(context)
    val hasDndAccess: Boolean get() = notifications.isNotificationPolicyAccessGranted

    fun askChangeSettings() = open(
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.packageName)),
    )

    fun askDndAccess() = open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))

    // ---- Buttons that switch something directly ----

    fun toggleTorch() {
        val id = torchId ?: return
        runCatching { camera.setTorchMode(id, !torchOn) }
    }

    fun cycleRinger() {
        val target = ringer.next(vibrator?.hasVibrator() == true)
        // Android only lets an app turn on silent (or leave it) with Do Not Disturb access
        if ((target == Ringer.SILENT || ringer == Ringer.SILENT) && !hasDndAccess) {
            askDndAccess()
            return
        }
        val mode = when (target) {
            Ringer.RING -> AudioManager.RINGER_MODE_NORMAL
            Ringer.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
            Ringer.SILENT -> AudioManager.RINGER_MODE_SILENT
        }
        try {
            audio.ringerMode = mode
            ringer = target
        } catch (_: SecurityException) {
            askDndAccess()
        }
    }

    fun toggleAutoRotate() {
        if (!canChangeSettings) return askChangeSettings()
        val on = !autoRotateOn
        Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (on) 1 else 0)
        autoRotateOn = on
    }

    /** Brightness slider, 0 to 1. Moving it turns auto brightness off, like the system slider. */
    fun changeBrightness(position: Float) {
        if (!canChangeSettings) return askChangeSettings()
        val cr = context.contentResolver
        if (autoBrightnessOn) {
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            autoBrightnessOn = false
        }
        Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, BrightnessCurve.toSetting(position))
        brightness = position.coerceIn(0f, 1f)
    }

    fun toggleAutoBrightness() {
        if (!canChangeSettings) return askChangeSettings()
        val on = !autoBrightnessOn
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
        )
        autoBrightnessOn = on
    }

    /** Media volume slider, 0 to 1. */
    fun changeVolume(position: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val steps = Math.round(position.coerceIn(0f, 1f) * max)
        runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, steps, 0) }
        volume = position.coerceIn(0f, 1f)
        muted = steps == 0
    }

    fun toggleMute() {
        runCatching { audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0) }
        refresh()
    }

    // ---- Accessibility actions: the panel closes first so it isn't in the way ----

    fun lockScreen() {
        closePanel()
        globalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
    }

    /** Opens Android's (Samsung's) own notification panel. */
    fun systemNotifications() {
        closePanel()
        globalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun powerMenu() {
        closePanel()
        globalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
    }

    /** Waits for the panel to leave the screen, so it isn't in the screenshot. */
    fun screenshot() {
        closePanel()
        Handler(Looper.getMainLooper()).postDelayed(
            { globalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT) },
            450,
        )
    }

    // ---- Buttons that open Android's own panel or settings page ----

    fun openWifi() = open(Intent(Settings.Panel.ACTION_WIFI))
    fun openBluetooth() = open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    fun openAirplane() = open(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS))
    fun openLocation() = open(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    fun openMobileData() = open(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
    fun openNfc() = open(Intent(Settings.Panel.ACTION_NFC), fallback = Intent(Settings.ACTION_NFC_SETTINGS))
    fun openCast() = open(Intent(Settings.ACTION_CAST_SETTINGS))
    fun openPowerSaving() = open(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
    fun openApp() = open(Intent(context, MainActivity::class.java))
    fun askMusicAccess() = open(media.accessIntent())
    fun openMusicApp() { media.appIntent()?.let { open(it) } }

    /** Android's speaker / headphones picker; older Android opens Bluetooth settings. */
    fun mediaOutput() {
        if (media.showOutputSwitcher()) closePanel() else openBluetooth()
    }

    /**
     * Starts the screen while the panel is still showing (Android lets an app with a
     * visible window start activities), then closes the panel.
     */
    private fun open(intent: Intent, fallback: Intent = Intent(Settings.ACTION_SETTINGS)) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            runCatching { context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        } catch (_: SecurityException) {
            runCatching { context.startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        closePanel()
    }
}
