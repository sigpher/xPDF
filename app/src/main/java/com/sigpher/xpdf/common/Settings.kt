package com.sigpher.xpdf.common

import com.blankj.utilcode.util.SPStaticUtils

/**
 * 全局设置。
 * <p>
 * 首次访问时一次性从 SharedPreferences 读入内存，之后读写只走内存，
 * 避免翻页、绘制等高频路径反复触达 SP。
 *
 * @author sigpher
 * @since 2020/6/13
 */
object Settings {

    private const val SP_LOCK_LANDSCAPE = "SP_LOCK_LANDSCAPE"
    private const val SP_MAX_RECENT_COUNT = "SP_MAX_RECENT_COUNT"
    private const val SP_SWIPE_HORIZONTAL = "SP_SWIPE_HORIZONTAL"
    private const val SP_NIGHT_MODE = "SP_NIGHT_MODE"
    private const val SP_VOLUME_CONTROL = "SP_VOLUME_CONTROL"
    private const val SP_SHOW_STATUS_BAR = "SP_SHOW_STATUS_BAR"
    private const val SP_CLICK_FLIP_PAGE = "SP_CLICK_FLIP_PAGE"
    private const val SP_KEEP_SCREEN_ON = "SP_KEEP_SCREEN_ON"
    private const val SP_LINEAR_LAYOUT = "SP_LINEAR_LAYOUT"
    private const val SP_SCROLL_SHORTCUT = "SP_SCROLL_SHORTCUT"
    private const val SP_FIRST_CREATE_SHORTCUT = "SP_FIRST_CREATE_SHORTCUT"

    @Volatile
    private var loaded = false

    @Volatile
    private var lockLandscapeValue = false

    @Volatile
    private var maxRecentCountValue = "9"

    @Volatile
    private var swipeHorizontalValue = true

    @Volatile
    private var nightModeValue = false

    @Volatile
    private var volumeControlValue = true

    @Volatile
    private var showStatusBarValue = false

    @Volatile
    private var clickFlipPageValue = true

    @Volatile
    private var keepScreenOnValue = false

    @Volatile
    private var linearLayoutValue = false

    @Volatile
    private var scrollShortCutValue = false

    @Volatile
    private var firstCreateShortcutValue = true

    private fun ensureLoaded() {
        if (loaded) {
            return
        }
        synchronized(this) {
            if (loaded) {
                return
            }
            lockLandscapeValue = SPStaticUtils.getBoolean(SP_LOCK_LANDSCAPE, false)
            maxRecentCountValue = SPStaticUtils.getString(SP_MAX_RECENT_COUNT, "9")
            swipeHorizontalValue = SPStaticUtils.getBoolean(SP_SWIPE_HORIZONTAL, true)
            nightModeValue = SPStaticUtils.getBoolean(SP_NIGHT_MODE, false)
            volumeControlValue = SPStaticUtils.getBoolean(SP_VOLUME_CONTROL, true)
            showStatusBarValue = SPStaticUtils.getBoolean(SP_SHOW_STATUS_BAR, false)
            clickFlipPageValue = SPStaticUtils.getBoolean(SP_CLICK_FLIP_PAGE, true)
            keepScreenOnValue = SPStaticUtils.getBoolean(SP_KEEP_SCREEN_ON, false)
            linearLayoutValue = SPStaticUtils.getBoolean(SP_LINEAR_LAYOUT, false)
            scrollShortCutValue = SPStaticUtils.getBoolean(SP_SCROLL_SHORTCUT, false)
            firstCreateShortcutValue = SPStaticUtils.getBoolean(SP_FIRST_CREATE_SHORTCUT, true)
            loaded = true
        }
    }

    var lockLandscape: Boolean
        get() {
            ensureLoaded()
            return lockLandscapeValue
        }
        set(value) {
            ensureLoaded()
            lockLandscapeValue = value
            SPStaticUtils.put(SP_LOCK_LANDSCAPE, value)
        }

    var maxRecentCount: String
        get() {
            ensureLoaded()
            return maxRecentCountValue
        }
        set(value) {
            ensureLoaded()
            maxRecentCountValue = value
            SPStaticUtils.put(SP_MAX_RECENT_COUNT, value)
        }

    var swipeHorizontal: Boolean
        get() {
            ensureLoaded()
            return swipeHorizontalValue
        }
        set(value) {
            ensureLoaded()
            swipeHorizontalValue = value
            SPStaticUtils.put(SP_SWIPE_HORIZONTAL, value)
        }

    var nightMode: Boolean
        get() {
            ensureLoaded()
            return nightModeValue
        }
        set(value) {
            ensureLoaded()
            nightModeValue = value
            SPStaticUtils.put(SP_NIGHT_MODE, value)
        }

    var volumeControl: Boolean
        get() {
            ensureLoaded()
            return volumeControlValue
        }
        set(value) {
            ensureLoaded()
            volumeControlValue = value
            SPStaticUtils.put(SP_VOLUME_CONTROL, value)
        }

    var showStatusBar: Boolean
        get() {
            ensureLoaded()
            return showStatusBarValue
        }
        set(value) {
            ensureLoaded()
            showStatusBarValue = value
            SPStaticUtils.put(SP_SHOW_STATUS_BAR, value)
        }

    var clickFlipPage: Boolean
        get() {
            ensureLoaded()
            return clickFlipPageValue
        }
        set(value) {
            ensureLoaded()
            clickFlipPageValue = value
            SPStaticUtils.put(SP_CLICK_FLIP_PAGE, value)
        }

    var keepScreenOn: Boolean
        get() {
            ensureLoaded()
            return keepScreenOnValue
        }
        set(value) {
            ensureLoaded()
            keepScreenOnValue = value
            SPStaticUtils.put(SP_KEEP_SCREEN_ON, value)
        }

    var linearLayout: Boolean
        get() {
            ensureLoaded()
            return linearLayoutValue
        }
        set(value) {
            ensureLoaded()
            linearLayoutValue = value
            SPStaticUtils.put(SP_LINEAR_LAYOUT, value)
        }

    var scrollShortCut: Boolean
        get() {
            ensureLoaded()
            return scrollShortCutValue
        }
        set(value) {
            ensureLoaded()
            scrollShortCutValue = value
            SPStaticUtils.put(SP_SCROLL_SHORTCUT, value)
        }

    /**
     * 用于第一次创建桌面快捷方式
     */
    var firstCreateShortcut: Boolean
        get() {
            ensureLoaded()
            return firstCreateShortcutValue
        }
        set(value) {
            ensureLoaded()
            firstCreateShortcutValue = value
            SPStaticUtils.put(SP_FIRST_CREATE_SHORTCUT, value)
        }
}
