package com.rr.numio.control

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.rr.numio.control.data.AppSettings
import com.rr.numio.control.ui.setup.SetupScreen
import com.rr.numio.control.ui.theme.ControlTheme
import com.rr.numio.control.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    // Goes up each time the app comes back from Settings, so permission ticks refresh
    private var resumes by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = AppSettings(this)
        setContent {
            val mode by settings.themeMode.collectAsState(initial = ThemeMode.DEFAULT)
            ControlTheme(dark = mode.isDark(isSystemInDarkTheme())) {
                SetupScreen(settings = settings, refreshKey = resumes)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumes++
    }
}
