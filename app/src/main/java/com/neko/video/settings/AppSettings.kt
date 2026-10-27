package com.neko.video.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.neko.video.model.AspectRatioMode
import com.neko.video.model.MediaSort
import com.neko.video.model.MediaSortField
import com.neko.video.model.ThemeMode
import com.neko.video.model.UserAgentPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("neko_settings")

data class SettingsState(
    val defaultSpeed: Float = 1.0f,
    val autoResume: Boolean = true,
    val autoPlayNext: Boolean = true,
    val seekSeconds: Int = 10,
    val keepScreenOn: Boolean = true,
    val backgroundPlay: Boolean = true,
    val pipEnabled: Boolean = true,
    val defaultAspectRatio: AspectRatioMode = AspectRatioMode.FIT,
    val completionThresholdPercent: Int = 5,
    val subtitleLanguage: String = "system",
    val subtitleSize: Float = 1.0f,
    val audioLanguage: String = "system",
    val audioFocusEnabled: Boolean = true,
    val userAgentPreset: UserAgentPreset = UserAgentPreset.APP_DEFAULT,
    val customUserAgent: String = "",
    val defaultHeadersText: String = "",
    val networkTimeoutSeconds: Int = 20,
    val mobileDataWarning: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val amoledDark: Boolean = false,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = true,
    val enableFloatingBottomBarBlur: Boolean = true,
    val keyColor: Int = 0,
    val colorStyle: String = "TonalSpot",
    val colorSpec: String = "SPEC_2025",
    val librarySort: MediaSort = MediaSort.DEFAULT,
)

class AppSettings(private val context: Context) {
    val settingsFlow: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        prefs.toSettingsState()
    }

    /** One-shot: switch existing installs to Miuix System blue (KernelSU non-Monet default). */
    suspend fun migrateToSystemBlueIfNeeded() {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.MIGRATED_SYSTEM_BLUE] == true) return@edit
            prefs[Keys.DYNAMIC_COLOR] = false
            prefs[Keys.KEY_COLOR] = 0
            prefs[Keys.MIGRATED_SYSTEM_BLUE] = true
        }
    }

    suspend fun update(transform: (SettingsState) -> SettingsState) {
        context.dataStore.edit { prefs ->
            val next = transform(prefs.toSettingsState())
            prefs[Keys.DEFAULT_SPEED] = next.defaultSpeed
            prefs[Keys.AUTO_RESUME] = next.autoResume
            prefs[Keys.AUTO_PLAY_NEXT] = next.autoPlayNext
            prefs[Keys.SEEK_SECONDS] = next.seekSeconds
            prefs[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
            prefs[Keys.BACKGROUND_PLAY] = next.backgroundPlay
            prefs[Keys.PIP_ENABLED] = next.pipEnabled
            prefs[Keys.DEFAULT_ASPECT] = next.defaultAspectRatio.name
            prefs[Keys.COMPLETION_THRESHOLD] = next.completionThresholdPercent
            prefs[Keys.SUBTITLE_LANGUAGE] = next.subtitleLanguage
            prefs[Keys.SUBTITLE_SIZE] = next.subtitleSize
            prefs[Keys.AUDIO_LANGUAGE] = next.audioLanguage
            prefs[Keys.AUDIO_FOCUS] = next.audioFocusEnabled
            prefs[Keys.UA_PRESET] = next.userAgentPreset.name
            prefs[Keys.UA_CUSTOM] = next.customUserAgent
            prefs[Keys.DEFAULT_HEADERS] = next.defaultHeadersText
            prefs[Keys.NETWORK_TIMEOUT] = next.networkTimeoutSeconds
            prefs[Keys.MOBILE_DATA_WARNING] = next.mobileDataWarning
            prefs[Keys.THEME_MODE] = next.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = next.dynamicColor
            prefs[Keys.AMOLED_DARK] = next.amoledDark
            prefs[Keys.ENABLE_BLUR] = next.enableBlur
            prefs[Keys.ENABLE_FLOATING_BOTTOM_BAR] = next.enableFloatingBottomBar
            prefs[Keys.ENABLE_FLOATING_BOTTOM_BAR_BLUR] = next.enableFloatingBottomBarBlur
            prefs[Keys.KEY_COLOR] = next.keyColor
            prefs[Keys.COLOR_STYLE] = next.colorStyle
            prefs[Keys.COLOR_SPEC] = next.colorSpec
            prefs[Keys.LIBRARY_SORT_FIELD] = next.librarySort.field.name
            prefs[Keys.LIBRARY_SORT_ASCENDING] = next.librarySort.ascending
        }
    }

    private fun Preferences.toSettingsState(): SettingsState = SettingsState(
        defaultSpeed = this[Keys.DEFAULT_SPEED] ?: 1.0f,
        autoResume = this[Keys.AUTO_RESUME] ?: true,
        autoPlayNext = this[Keys.AUTO_PLAY_NEXT] ?: true,
        seekSeconds = this[Keys.SEEK_SECONDS] ?: 10,
        keepScreenOn = this[Keys.KEEP_SCREEN_ON] ?: true,
        backgroundPlay = this[Keys.BACKGROUND_PLAY] ?: true,
        pipEnabled = this[Keys.PIP_ENABLED] ?: true,
        defaultAspectRatio = runCatching {
            AspectRatioMode.valueOf(this[Keys.DEFAULT_ASPECT] ?: AspectRatioMode.FIT.name)
        }.getOrDefault(AspectRatioMode.FIT),
        completionThresholdPercent = this[Keys.COMPLETION_THRESHOLD] ?: 5,
        subtitleLanguage = this[Keys.SUBTITLE_LANGUAGE] ?: "system",
        subtitleSize = this[Keys.SUBTITLE_SIZE] ?: 1.0f,
        audioLanguage = this[Keys.AUDIO_LANGUAGE] ?: "system",
        audioFocusEnabled = this[Keys.AUDIO_FOCUS] ?: true,
        userAgentPreset = runCatching {
            UserAgentPreset.valueOf(this[Keys.UA_PRESET] ?: UserAgentPreset.APP_DEFAULT.name)
        }.getOrDefault(UserAgentPreset.APP_DEFAULT),
        customUserAgent = this[Keys.UA_CUSTOM] ?: "",
        defaultHeadersText = this[Keys.DEFAULT_HEADERS] ?: "",
        networkTimeoutSeconds = this[Keys.NETWORK_TIMEOUT] ?: 20,
        mobileDataWarning = this[Keys.MOBILE_DATA_WARNING] ?: true,
        themeMode = runCatching {
            ThemeMode.valueOf(this[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM),
        dynamicColor = this[Keys.DYNAMIC_COLOR] ?: false,
        amoledDark = this[Keys.AMOLED_DARK] ?: false,
        enableBlur = this[Keys.ENABLE_BLUR] ?: true,
        enableFloatingBottomBar = this[Keys.ENABLE_FLOATING_BOTTOM_BAR] ?: true,
        enableFloatingBottomBarBlur = this[Keys.ENABLE_FLOATING_BOTTOM_BAR_BLUR] ?: true,
        keyColor = this[Keys.KEY_COLOR] ?: 0,
        colorStyle = this[Keys.COLOR_STYLE] ?: "TonalSpot",
        colorSpec = this[Keys.COLOR_SPEC] ?: "SPEC_2025",
        librarySort = MediaSort(
            field = runCatching {
                MediaSortField.valueOf(
                    this[Keys.LIBRARY_SORT_FIELD] ?: MediaSortField.NAME.name,
                )
            }.getOrDefault(MediaSortField.NAME),
            ascending = this[Keys.LIBRARY_SORT_ASCENDING] ?: true,
        ),
    )

    private object Keys {
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val AUTO_RESUME = booleanPreferencesKey("auto_resume")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val SEEK_SECONDS = intPreferencesKey("seek_seconds")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val BACKGROUND_PLAY = booleanPreferencesKey("background_play")
        val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
        val DEFAULT_ASPECT = stringPreferencesKey("default_aspect")
        val COMPLETION_THRESHOLD = intPreferencesKey("completion_threshold")
        val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
        val SUBTITLE_SIZE = floatPreferencesKey("subtitle_size")
        val AUDIO_LANGUAGE = stringPreferencesKey("audio_language")
        val AUDIO_FOCUS = booleanPreferencesKey("audio_focus")
        val UA_PRESET = stringPreferencesKey("ua_preset")
        val UA_CUSTOM = stringPreferencesKey("ua_custom")
        val DEFAULT_HEADERS = stringPreferencesKey("default_headers")
        val NETWORK_TIMEOUT = intPreferencesKey("network_timeout")
        val MOBILE_DATA_WARNING = booleanPreferencesKey("mobile_data_warning")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val MIGRATED_SYSTEM_BLUE = booleanPreferencesKey("migrated_system_blue_v1")
        val AMOLED_DARK = booleanPreferencesKey("amoled_dark")
        val ENABLE_BLUR = booleanPreferencesKey("enable_blur")
        val ENABLE_FLOATING_BOTTOM_BAR = booleanPreferencesKey("enable_floating_bottom_bar")
        val ENABLE_FLOATING_BOTTOM_BAR_BLUR = booleanPreferencesKey("enable_floating_bottom_bar_blur")
        val KEY_COLOR = intPreferencesKey("key_color")
        val COLOR_STYLE = stringPreferencesKey("color_style")
        val COLOR_SPEC = stringPreferencesKey("color_spec")
        val LIBRARY_SORT_FIELD = stringPreferencesKey("library_sort_field")
        val LIBRARY_SORT_ASCENDING = booleanPreferencesKey("library_sort_ascending")
    }
}
