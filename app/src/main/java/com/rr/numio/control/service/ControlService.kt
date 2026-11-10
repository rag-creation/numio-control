package com.rr.numio.control.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import com.rr.numio.control.data.AppSettings
import com.rr.numio.control.data.HandleSettings
import com.rr.numio.control.data.HandlePosition
import com.rr.numio.control.data.HandleSide
import com.rr.numio.control.panel.PanelController
import com.rr.numio.control.panel.PanelLayout
import com.rr.numio.control.panel.PanelScreen
import com.rr.numio.control.ui.theme.ControlTheme
import com.rr.numio.control.ui.theme.ThemeMode
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The accessibility service. Android keeps it running once the user turns it on in
 * Settings > Accessibility. It draws two windows of its own over other apps:
 * the edge handle, and the panel when it is open. It receives no accessibility
 * events and cannot read other apps (see res/xml/control_service.xml).
 */
class ControlService : AccessibilityService() {

    companion object {
        private val running = MutableStateFlow(false)

        /** True while the service is on, for the setup screen. */
        val isRunning: StateFlow<Boolean> = running

        @Volatile
        private var instance: ControlService? = null

        /** "Show panel now" on the setup screen. */
        fun showPanel() {
            instance?.openPanel()
        }
    }

    private lateinit var windows: WindowManager
    private lateinit var settings: AppSettings
    private lateinit var controller: PanelController
    private val owner = OverlayOwner()
    private val scope = MainScope()

    private var handle: View? = null
    private var panel: View? = null
    private var closing = false
    private var handleSettings = HandleSettings(HandleSide.RIGHT, HandlePosition.TOP)

    // Compose state read by the panel
    private var panelVisible by mutableStateOf(false)
    private var theme by mutableStateOf(ThemeMode.DEFAULT)
    private var layout by mutableStateOf(PanelLayout.DEFAULT)

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = removePanelNow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windows = getSystemService(WindowManager::class.java)
        settings = AppSettings(this)
        controller = PanelController(this, ::closePanel) { performGlobalAction(it) }
        controller.start()
        owner.start()
        instance = this
        running.value = true

        scope.launch { settings.themeMode.collect { theme = it } }
        scope.launch { settings.layout.collect { layout = it } }
        scope.launch {
            settings.handle.collect {
                handleSettings = it
                if (panel == null) showHandle()
            }
        }

        ContextCompat.registerReceiver(
            this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit // asks for no events
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        instance = null
        running.value = false
        runCatching { unregisterReceiver(screenOff) }
        removePanelNow()
        removeHandle()
        if (::controller.isInitialized) controller.stop()
        owner.stop()
        scope.cancel()
        super.onDestroy()
    }

    // ---- Edge handle ----

    private fun showHandle() {
        removeHandle()
        val density = resources.displayMetrics.density
        val onRight = handleSettings.side == HandleSide.RIGHT
        val view = HandleView(this, onRight) { openPanel() }
        val params = overlayParams((22 * density).toInt(), (128 * density).toInt()).apply {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            gravity = Gravity.TOP or (if (onRight) Gravity.END else Gravity.START)
            y = (screenHeight() * handleSettings.position.fraction).toInt()
        }
        runCatching { windows.addView(view, params) }.onSuccess { handle = view }
    }

    private fun removeHandle() {
        handle?.let { runCatching { windows.removeView(it) } }
        handle = null
    }

    private fun screenHeight(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windows.currentWindowMetrics.bounds.height()
        } else {
            resources.displayMetrics.heightPixels
        }

    // ---- Panel ----

    fun openPanel() {
        if (panel != null) return
        controller.refresh()
        closing = false
        panelVisible = false

        val root = object : FrameLayout(this@ControlService) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action == KeyEvent.ACTION_UP) closePanel()
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }
        val compose = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                ControlTheme(dark = theme.isDark(isSystemInDarkTheme())) {
                    PanelScreen(controller, layout, panelVisible, ::closePanel)
                }
                LaunchedEffect(Unit) { panelVisible = true }
            }
        }
        root.addView(compose, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        owner.attachTo(root)

        val params = overlayParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT).apply {
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            // Frosted glass behind the panel, on phones that support it (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windows.isCrossWindowBlurEnabled) {
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                blurBehindRadius = (36 * resources.displayMetrics.density).toInt()
            }
        }
        runCatching { windows.addView(root, params) }.onSuccess {
            panel = root
            removeHandle()
        }
    }

    /** Slides the panel away, then removes its window. */
    fun closePanel() {
        if (panel == null || closing) return
        closing = true
        panelVisible = false
        scope.launch {
            delay(260)
            removePanelNow()
        }
    }

    private fun removePanelNow() {
        val p = panel ?: return
        runCatching { windows.removeView(p) }
        panel = null
        closing = false
        panelVisible = false
        showHandle()
    }

    private fun overlayParams(width: Int, height: Int) = WindowManager.LayoutParams(
        width,
        height,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    )
}
