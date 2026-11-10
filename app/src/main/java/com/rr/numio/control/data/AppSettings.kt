package com.rr.numio.control.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rr.numio.control.panel.PanelLayout
import com.rr.numio.control.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Which screen edge the panel handle sits on. */
enum class HandleSide(val key: String) {
    RIGHT("right"), LEFT("left");

    companion object {
        fun fromKey(key: String?): HandleSide = entries.firstOrNull { it.key == key } ?: RIGHT
    }
}

/** How far down the edge the handle sits. */
enum class HandlePosition(val key: String, val fraction: Float) {
    TOP("top", 0.18f), MIDDLE("middle", 0.42f);

    companion object {
        fun fromKey(key: String?): HandlePosition = entries.firstOrNull { it.key == key } ?: TOP
    }
}

data class HandleSettings(val side: HandleSide, val position: HandlePosition)

/** App settings, stored only on the phone with DataStore. */
class AppSettings(context: Context) {
    private val store = context.applicationContext.settingsStore
    private val themeKey = stringPreferencesKey("theme_mode")
    private val sideKey = stringPreferencesKey("handle_side")
    private val positionKey = stringPreferencesKey("handle_position")
    private val layoutKey = stringPreferencesKey("panel_layout")

    val themeMode: Flow<ThemeMode> = store.data.map { ThemeMode.fromKey(it[themeKey]) }

    val handle: Flow<HandleSettings> = store.data.map {
        HandleSettings(HandleSide.fromKey(it[sideKey]), HandlePosition.fromKey(it[positionKey]))
    }

    val layout: Flow<PanelLayout> = store.data.map { PanelLayout.decode(it[layoutKey]) }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[themeKey] = mode.key }
    suspend fun setHandleSide(side: HandleSide) = store.edit { it[sideKey] = side.key }
    suspend fun setHandlePosition(p: HandlePosition) = store.edit { it[positionKey] = p.key }
    suspend fun setLayout(layout: PanelLayout) = store.edit { it[layoutKey] = layout.encode() }
}
