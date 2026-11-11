package com.rr.numio.control.panel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.MediaRouter2
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat
import com.rr.numio.control.service.MediaListener

/**
 * The music card: follows whichever app is playing (the phone's media sessions) and
 * sends play, pause, next and previous to it. Needs notification access; without it
 * the card offers to turn it on.
 */
class MediaWatcher(private val context: Context) {
    private val sessions = context.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(context, MediaListener::class.java)
    private val main = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private var listening = false

    var hasAccess by mutableStateOf(false); private set
    var title by mutableStateOf<String?>(null); private set
    var artist by mutableStateOf<String?>(null); private set
    var art by mutableStateOf<ImageBitmap?>(null); private set
    var playing by mutableStateOf(false); private set
    val hasMedia: Boolean get() = title != null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = read()
        override fun onPlaybackStateChanged(state: PlaybackState?) = read()
        override fun onSessionDestroyed() = pick(currentSessions())
    }

    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { pick(it.orEmpty()) }

    /** Called each time the panel opens. */
    fun refresh() {
        hasAccess = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        if (!hasAccess) {
            clear()
            return
        }
        if (!listening) {
            listening = runCatching { sessions.addOnActiveSessionsChangedListener(sessionsChanged, listener, main) }.isSuccess
        }
        pick(currentSessions())
    }

    fun stop() {
        if (listening) runCatching { sessions.removeOnActiveSessionsChangedListener(sessionsChanged) }
        listening = false
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun currentSessions(): List<MediaController> =
        runCatching { sessions.getActiveSessions(listener) }.getOrDefault(emptyList())

    /** Prefers the app that is playing right now, else the most recent one. */
    private fun pick(list: List<MediaController>) {
        val next = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
        if (next?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = next
            next?.registerCallback(callback, main)
        }
        read()
    }

    private fun read() {
        val c = controller
        val meta = c?.metadata
        if (c == null || meta == null) return clear()
        title = meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: meta.getString(MediaMetadata.METADATA_KEY_TITLE)
        artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        val bitmap: Bitmap? = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        art = bitmap?.asImageBitmap()
        playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
    }

    private fun clear() {
        title = null
        artist = null
        art = null
        playing = false
    }

    fun playPause() {
        val t = controller?.transportControls ?: return
        if (playing) t.pause() else t.play()
    }

    fun next() = controller?.transportControls?.skipToNext()
    fun previous() = controller?.transportControls?.skipToPrevious()

    /** The app that is playing, to open it from the card. */
    fun appIntent(): Intent? = controller?.packageName?.let { context.packageManager.getLaunchIntentForPackage(it) }

    /** Android's own "play on" picker (speaker, headphones...) on Android 14+. */
    fun showOutputSwitcher(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching { MediaRouter2.getInstance(context).showSystemOutputSwitcher() }.getOrDefault(false)
        } else {
            false
        }

    fun accessIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener.flattenToString())
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
}
